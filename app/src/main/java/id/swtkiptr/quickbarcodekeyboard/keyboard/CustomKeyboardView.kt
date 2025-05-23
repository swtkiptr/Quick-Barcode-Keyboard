package id.swtkiptr.keyboardscanner.keyboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.ContextCompat
import id.swtkiptr.keyboardscanner.R

/**
 * A custom keyboard view implementation with a clean white design
 */
class CustomKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Interface for handling key presses
    interface OnKeyClickListener {
        fun onKeyClick(key: KeyboardKey)
    }

    // Enhanced color and dimension settings with simpler white scheme
    private val keyBackground = Paint().apply {
        color = ContextCompat.getColor(context, R.color.keyBackground)
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val keyBackgroundPressed = Paint().apply {
        color = ContextCompat.getColor(context, R.color.keyBackgroundPressed)
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val keySpecialPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.keySpecial)
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.keyText)
        textSize = resources.getDimensionPixelSize(R.dimen.key_text_size).toFloat()
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT
        isAntiAlias = true
    }

    private val specialTextPaint = Paint().apply {
        color = ContextCompat.getColor(context, android.R.color.black)
        textSize = resources.getDimensionPixelSize(R.dimen.key_special_text_size).toFloat()
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
    }

    private val keyboardBackground = Paint().apply {
        color = ContextCompat.getColor(context, R.color.keyboardBackground)
        style = Paint.Style.FILL
    }

    // Popup components for key preview
    private var keyPreviewPopup: PopupWindow? = null
    private var keyPreviewText: TextView? = null
    private val KEY_PREVIEW_TEXT_SIZE = resources.getDimensionPixelSize(R.dimen.key_preview_text_size).toFloat()
    private val KEY_PREVIEW_OFFSET = resources.getDimensionPixelSize(R.dimen.key_preview_offset)
    private val KEY_PREVIEW_PADDING = resources.getDimensionPixelSize(R.dimen.key_preview_padding)
    private val KEY_PREVIEW_DELAY = 80L // Reduced delay before hiding popup for faster response

    // Layout settings
    private val keySpacing = resources.getDimensionPixelSize(R.dimen.key_spacing)
    private var keyHeight = resources.getDimensionPixelSize(R.dimen.key_height)
    private var keyWidth = 0 // Will be calculated based on view width
    private val cornerRadius = resources.getDimensionPixelSize(R.dimen.key_corner_radius).toFloat()
    private val columnsPerRow = 10 // Standard 10 keys per row for QWERTY

    // Current keyboard state
    private var keyboardLayout: KeyboardLayout? = null
    private var keyRects = mutableListOf<Rect>()
    private var keyRectFs = mutableListOf<RectF>() // For rounded corners
    private var keyMap = mutableListOf<KeyboardKey>()

    // Listener for key clicks
    var onKeyClickListener: OnKeyClickListener? = null
    var isPreviewEnabled = true

    // Currently pressed key for visual feedback
    private var pressedKeyIndex = -1

    // Handler and runnables
    private val handler = Handler(Looper.getMainLooper())
    private var hidePopupRunnable: Runnable? = null
    private var repeatRunnable: Runnable? = null
    private var isDeleteHeld = false
    private val REPEAT_INITIAL_DELAY = 300L // Reduced for faster initial repeat
    private val REPEAT_INTERVAL = 35L // Faster repeat interval for smoother deletion

    // Vibrator for haptic feedback
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(VibratorManager::class.java)
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION") 
        context.getSystemService(Vibrator::class.java)
    }

    // Constants for better touch handling
    private val TOUCH_TARGET_EXPANSION = 12f // Expand touch area by this many pixels
    private val TOUCH_SLOP = 8f  // Small movement tolerance

    init {
        // Set background color for the whole keyboard
        setBackgroundColor(ContextCompat.getColor(context, R.color.keyboardBackground))

        // Initialize key preview popup
        setupKeyPreviewPopup()
        
        // Enable hardware acceleration for better performance
        setLayerType(LAYER_TYPE_HARDWARE, null)
        
        // Set view importance to high for priority event handling
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES)
    }

    /**
     * Set the keyboard layout to display
     */
    fun setKeyboardLayout(layout: KeyboardLayout) {
        keyboardLayout = layout
        calculateKeyPositions()
        invalidate()
    }

    /**
     * Calculate positions of all keys based on the layout
     */
    private fun calculateKeyPositions() {
        keyRects.clear()
        keyRectFs.clear()
        keyMap.clear()

        keyboardLayout?.let { layout ->
            val rows = when (layout.layoutType) {
                KeyboardLayout.LayoutType.QWERTY -> listOf(10, 9, 9, 5)
                KeyboardLayout.LayoutType.SYMBOLS -> listOf(10, 9, 9, 5)
                KeyboardLayout.LayoutType.SYMBOLS2 -> listOf(10, 9, 9, 5)

            }

            val availableWidth = width - (keySpacing * 2)
            keyWidth = (availableWidth - (columnsPerRow - 1) * keySpacing) / columnsPerRow

            var keyIndex = 0
            var yPosition = keySpacing * 2

            rows.forEach { keysInRow ->
                var rowKeys = mutableListOf<Pair<KeyboardKey, Int>>()
                var totalUnits = 0

                // Preprocess: calculate width units (to account for larger keys)
                for (i in 0 until keysInRow) {
                    if (keyIndex >= layout.keys.size) break
                    val key = layout.keys[keyIndex]
                    val widthUnits = when {
                        key.code == KeyboardKey.CODE_SPACE -> 3
                        key.width > 1 -> key.width
                        else -> 1
                    }
                    rowKeys.add(Pair(key, widthUnits))
                    totalUnits += widthUnits
                    keyIndex++
                }

                // Recalculate actualKeyWidth based on totalUnits
                val spacingTotal = (rowKeys.size - 1) * keySpacing
                val actualKeyWidth = (availableWidth - spacingTotal) / totalUnits

                // Center the row
                val rowPixelWidth = totalUnits * actualKeyWidth + spacingTotal
                var xPosition = (width - rowPixelWidth) / 2

                for ((key, widthUnits) in rowKeys) {
                    val keyPixelWidth = actualKeyWidth * widthUnits

                    val rect = Rect(
                        xPosition,
                        yPosition,
                        xPosition + keyPixelWidth,
                        yPosition + keyHeight
                    )

                    val rectF = RectF(
                        xPosition.toFloat(),
                        yPosition.toFloat(),
                        (xPosition + keyPixelWidth).toFloat(),
                        (yPosition + keyHeight).toFloat()
                    )

                    keyRects.add(rect)
                    keyRectFs.add(rectF)
                    keyMap.add(key)

                    xPosition += keyPixelWidth + keySpacing
                }

                yPosition += keyHeight + keySpacing
            }
        }
    }

    // Update setupKeyPreviewPopup
    private fun setupKeyPreviewPopup() {
        keyPreviewText = TextView(context).apply {
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, KEY_PREVIEW_TEXT_SIZE)
            setTextColor(ContextCompat.getColor(context, android.R.color.black))
            background = ContextCompat.getDrawable(context, R.drawable.key_preview_background)
            gravity = android.view.Gravity.CENTER
            setPadding(KEY_PREVIEW_PADDING, KEY_PREVIEW_PADDING, KEY_PREVIEW_PADDING, KEY_PREVIEW_PADDING)
        }
        keyPreviewPopup = PopupWindow(
            keyPreviewText,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
            false
        ).apply {
            isClippingEnabled = false
            isTouchable = false
        }
    }

    private fun showKeyPreview(index: Int) {
        if (index < 0 || index >= keyMap.size) return

        // Cancel any pending hide operation
        hidePopupRunnable?.let { handler.removeCallbacks(it) }

        val key = keyMap[index]
        // Don't show preview for special keys like Delete, Shift, etc.
        if (key.isSpecial) return

        val rect = keyRects[index]

        keyPreviewText?.text = key.getDisplayLabel()
        keyPreviewPopup?.let { popup ->
            if (!popup.isShowing) {
                popup.width = rect.width() + KEY_PREVIEW_PADDING * 2
                popup.height = rect.height() + KEY_PREVIEW_PADDING * 2
                popup.showAtLocation(this, android.view.Gravity.NO_GRAVITY,
                    rect.left - KEY_PREVIEW_PADDING,
                    rect.top + KEY_PREVIEW_OFFSET)
            } else {
                popup.update(rect.left - KEY_PREVIEW_PADDING,
                            rect.top + KEY_PREVIEW_OFFSET,
                            rect.width() + KEY_PREVIEW_PADDING * 2,
                            rect.height() + KEY_PREVIEW_PADDING * 2)
            }
        }
    }

    private fun hideKeyPreview() {
        // Cancel any pending hide operations first
        hidePopupRunnable?.let { handler.removeCallbacks(it) }

        // Create new runnable for delayed hiding
        hidePopupRunnable = Runnable {
            keyPreviewPopup?.dismiss()
            hidePopupRunnable = null
        }

        // Schedule the hide operation with delay
        handler.postDelayed(hidePopupRunnable!!, KEY_PREVIEW_DELAY)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw keyboard background
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), keyboardBackground)

        // Draw each key (just the text, without borders)
        for (i in keyRects.indices) {
            val rectF = keyRectFs[i]
            val key = keyMap[i]

            // Only draw background for pressed keys
            if (i == pressedKeyIndex) {
                val bgPaint = keyBackgroundPressed
                canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, bgPaint)
            }

            // Choose text paint based on key type
            val paint = if (key.isSpecial) specialTextPaint else textPaint

            // Draw key text - centered
            val textX = rectF.centerX()
            val textY = rectF.centerY() + (paint.textSize / 3)  // Text vertical centering
            canvas.drawText(key.getDisplayLabel(), textX, textY, paint)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Determine view height based on key height and spacing
        val numRows = 4 // Standard keyboard has 5 rows
        val desiredHeight = numRows * keyHeight + (numRows + 2) * keySpacing

        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = resolveSize(desiredHeight, heightMeasureSpec)

        setMeasuredDimension(width, height)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        calculateKeyPositions()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Use action masking for better multi-touch handling
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                val index = getTouchedKeyIndex(event.x, event.y)
                if (index != -1) {
                    pressedKeyIndex = index
                    // Use hardware acceleration for faster updates
                    setLayerType(LAYER_TYPE_HARDWARE, null)
                    invalidate()

                    // Show key preview immediately for better feedback
                    showKeyPreview(index)

                    // Perform haptic feedback
                    performHapticFeedback()

                    val key = keyMap[index]
                    if (key.code == KeyboardKey.CODE_DELETE) {
                        isDeleteHeld = true
                        repeatRunnable = object : Runnable {
                            override fun run() {
                                if (isDeleteHeld) {
                                    onKeyClickListener?.onKeyClick(key)
                                    handler.postDelayed(this, REPEAT_INTERVAL)
                                }
                            }
                        }
                        // Trigger delete immediately
                        onKeyClickListener?.onKeyClick(key)
                        handler.postDelayed(repeatRunnable!!, REPEAT_INITIAL_DELAY)
                    }
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                // Only process if there's significant movement (reduces unnecessary calculations)
                val index = getTouchedKeyIndex(event.x, event.y)
                if (pressedKeyIndex != index) {
                    // If leaving delete key, stop repeat
                    if (isDeleteHeld && (index == -1 || keyMap.getOrNull(index)?.code != KeyboardKey.CODE_DELETE)) {
                        isDeleteHeld = false
                        repeatRunnable?.let { handler.removeCallbacks(it) }
                    }
                    
                    pressedKeyIndex = index
                    invalidate()

                    // Update key preview based on finger position
                    if (index == -1) {
                        hideKeyPreview()
                    } else {
                        showKeyPreview(index)
                    }
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                hideKeyPreview()
                
                // Switch back to software rendering for normal mode
                setLayerType(LAYER_TYPE_NONE, null)

                // Handle key click
                val index = getTouchedKeyIndex(event.x, event.y)
                if (isDeleteHeld) {
                    isDeleteHeld = false
                    repeatRunnable?.let { handler.removeCallbacks(it) }
                }
                
                if (index != -1) {
                    val key = keyMap[index]
                    // Skip for delete which is already handled in ACTION_DOWN
                    if (key.code != KeyboardKey.CODE_DELETE) {
                        onKeyClickListener?.onKeyClick(key)
                    }
                }
                
                pressedKeyIndex = -1
                invalidate()
                return true
            }
            else -> return false
        }
    }

    private fun getTouchedKeyIndex(x: Float, y: Float): Int {
        // First check for exact hits (highest priority)
        for (i in keyRects.indices) {
            if (keyRects[i].contains(x.toInt(), y.toInt())) {
                return i
            }
        }
        
        // If no exact hit, check with expanded touch area
        for (i in keyRects.indices) {
            val rect = keyRects[i]
            val expandedRect = RectF(
                rect.left - TOUCH_TARGET_EXPANSION,
                rect.top - TOUCH_TARGET_EXPANSION,
                rect.right + TOUCH_TARGET_EXPANSION,
                rect.bottom + TOUCH_TARGET_EXPANSION
            )
            if (expandedRect.contains(x, y)) {
                return i
            }
        }
        return -1
    }

    private fun performHapticFeedback() {
        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(50)
            }
        }
    }
}