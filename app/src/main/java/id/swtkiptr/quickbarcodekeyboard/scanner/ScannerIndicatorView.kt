package id.swtkiptr.keyboardscanner.scanner

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import id.swtkiptr.keyboardscanner.R

/**
 * A view that displays a target box overlay specifically highlighting detected barcodes
 */
class ScannerIndicatorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {



    private val activePaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.scanner_indicator_active)
        style = Paint.Style.STROKE
        strokeWidth = resources.getDimension(R.dimen.scanner_indicator_stroke)
        isAntiAlias = true
    }

    private var isActive = false
    private val guidePaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.scanner_indicator)
        style = Paint.Style.STROKE
        strokeWidth = resources.getDimension(R.dimen.scanner_indicator_stroke) / 2
        isAntiAlias = true
        alpha = 120
    }

    // Default indicator rectangle (centered)
    private val indicatorRect = RectF()

    // Rectangle positioned around actual detected barcode
    private val barcodeRect = RectF()
    private var hasBarcodePosition = false

    fun setActive(active: Boolean) {
        if (isActive != active) {
            isActive = active
            invalidate()
        }
    }

    /**
     * Set the position of the detected barcode in view coordinates
     */
    fun setBarcodePosition(rect: Rect?) {
        hasBarcodePosition = rect != null

        if (rect != null) {
            barcodeRect.set(
                rect.left.toFloat(),
                rect.top.toFloat(),
                rect.right.toFloat(),
                rect.bottom.toFloat()
            )
            invalidate()
        }
    }

    /**
     * Clear the barcode position
     */
    fun clearBarcodePosition() {
        hasBarcodePosition = false
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        // Calculate the guide rectangle to be centered in the view
        val indicatorSize = resources.getDimension(R.dimen.scanner_indicator_width).coerceAtMost(
            resources.getDimension(R.dimen.scanner_indicator_height)
        ).coerceAtMost(
            Math.min(w, h) * 0.7f // Make sure it fits within the view with some margin
        )

        val left = (w - indicatorSize) / 2
        val top = (h - indicatorSize) / 2

        indicatorRect.set(left, top, left + indicatorSize, top + indicatorSize)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // If we're active and have barcode position, draw the highlighted rectangle
        if (isActive && hasBarcodePosition) {
            // Draw rectangle around the detected barcode
            canvas.drawRect(barcodeRect, activePaint)

            // Draw corner brackets on the barcode rectangle for visual appeal
            val cornerLength = Math.min(barcodeRect.width(), barcodeRect.height()) * 0.15f

            // Top-left corner
            canvas.drawLine(
                barcodeRect.left,
                barcodeRect.top + cornerLength,
                barcodeRect.left,
                barcodeRect.top,
                activePaint
            )
            canvas.drawLine(
                barcodeRect.left,
                barcodeRect.top,
                barcodeRect.left + cornerLength,
                barcodeRect.top,
                activePaint
            )

            // Top-right corner
            canvas.drawLine(
                barcodeRect.right - cornerLength,
                barcodeRect.top,
                barcodeRect.right,
                barcodeRect.top,
                activePaint
            )
            canvas.drawLine(
                barcodeRect.right,
                barcodeRect.top,
                barcodeRect.right,
                barcodeRect.top + cornerLength,
                activePaint
            )

            // Bottom-left corner
            canvas.drawLine(
                barcodeRect.left,
                barcodeRect.bottom - cornerLength,
                barcodeRect.left,
                barcodeRect.bottom,
                activePaint
            )
            canvas.drawLine(
                barcodeRect.left,
                barcodeRect.bottom,
                barcodeRect.left + cornerLength,
                barcodeRect.bottom,
                activePaint
            )

            // Bottom-right corner
            canvas.drawLine(
                barcodeRect.right - cornerLength,
                barcodeRect.bottom,
                barcodeRect.right,
                barcodeRect.bottom,
                activePaint
            )
            canvas.drawLine(
                barcodeRect.right,
                barcodeRect.bottom,
                barcodeRect.right,
                barcodeRect.bottom - cornerLength,
                activePaint
            )
        }
    }
}