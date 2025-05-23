package id.swtkiptr.keyboardscanner

import android.Manifest
import android.content.Context
import android.content.Context.VIBRATOR_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.inputmethodservice.InputMethodService
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import id.swtkiptr.keyboardscanner.keyboard.CustomKeyboardView
import id.swtkiptr.keyboardscanner.keyboard.KeyboardKey
import id.swtkiptr.keyboardscanner.keyboard.KeyboardLayout
import id.swtkiptr.keyboardscanner.scanner.ScannerIndicatorView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.graphics.Rect
import android.os.Build
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.annotation.RequiresApi
import android.os.VibratorManager
import com.google.zxing.ResultPoint

class BarcodeInputService : InputMethodService(), DecoratedBarcodeView.TorchListener,
    BarcodeCallback, CustomKeyboardView.OnKeyClickListener {

    companion object {
        private const val TAG = "BarcodeInputService"
        private const val DEBOUNCE_TIME_MS = 300L
        private const val SAME_BARCODE_DELAY_MS = 2000L
        private const val KEYBOARD_SHOW_DELAY_MS = 200L
        private const val FOCUS_MAINTAIN_DELAY_MS = 100L
        private const val INDICATOR_ACTIVE_DURATION_MS = 800L
        private const val TYPING_DELAY_MS = 20L
        private const val BEEP_DURATION_MS = 150
        private const val BEEP_VOLUME = 80 // Volume range: 0-100
    }

    private var scannerView: DecoratedBarcodeView? = null
    private var scannerIndicator: ScannerIndicatorView? = null
    private var permissionButton: Button? = null
    private var flashlightButton: Button? = null
    private var isTorchOn: Boolean = false
    private var lastText: String = ""
    private var lastTime: Long = 0
    private var lastToastTime: Long = 0
    
    // ToneGenerator for beep sound
    private var toneGenerator: ToneGenerator? = null

    private var keyboardView: CustomKeyboardView? = null
    private var qwertyLayout: KeyboardLayout? = null
    private var symbolsLayout: KeyboardLayout? = null
    private var symbols2Layout: KeyboardLayout? = null  // Added symbols2Layout
    private var currentLayout: KeyboardLayout? = null
    private var switchModeButton: Button? = null

    private var isKeyboardMode = false
    private var isCaps = false

    private val handler = Handler(Looper.getMainLooper())
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var batchScanMode = true

    override fun onCreateInputView(): View {
        val v = layoutInflater.inflate(R.layout.input, null)

        // Initialize ToneGenerator for beep sound
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, BEEP_VOLUME)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize ToneGenerator: ${e.message}", e)
        }

        scannerView = v.findViewById(R.id.zxing_scanner)
        scannerIndicator = v.findViewById(R.id.scanner_indicator)
        setupScanner()

        permissionButton = v.findViewById(R.id.button)
        permissionButton?.setOnClickListener {
            launchPermissionActivity()
        }

        keyboardView = v.findViewById(R.id.keyboardView)
        keyboardView?.onKeyClickListener = this

        qwertyLayout = KeyboardLayout.createQwertyLayout()
        symbolsLayout = KeyboardLayout.createSymbolsLayout()
        symbols2Layout = KeyboardLayout.createSymbol2Layout() // Initialize symbols2Layout

        currentLayout = qwertyLayout
        keyboardView?.setKeyboardLayout(currentLayout!!)

        switchModeButton = v.findViewById(R.id.switchModeButton)
        switchModeButton?.setOnClickListener {
            toggleInputMode()
        }

        flashlightButton = v.findViewById(R.id.flashlightButton)
        flashlightButton?.setOnClickListener { _ ->
            toggleTorch()
        }

        styleButtons()

        enforcePermission()
        return v
    }

    private fun styleButtons() {
        switchModeButton?.let { button ->
            button.setTextColor(ContextCompat.getColor(this, android.R.color.black))
        }

        flashlightButton?.let {
            updateFlashlightButtonStyle()
        }
    }

    private fun updateFlashlightButtonStyle() {
        flashlightButton?.let { button ->
            button.text = if (isTorchOn) "🔦" else "📸"
        }
    }

    private fun launchPermissionActivity() {
        Intent(this, PermissionCheckActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(this)
        }
    }

    private fun setupScanner() {
        try {
            scannerView?.apply {
                setTorchListener(this@BarcodeInputService)
                decodeContinuous(this@BarcodeInputService)
                setStatusText("")
                
                // Optimize camera settings
                val settings = cameraSettings
                settings.requestedCameraId = -1 // Use default camera (usually rear)
                settings.isAutoFocusEnabled = true // Continuous autofocus
                settings.isBarcodeSceneModeEnabled = true // Optimize for barcode scanning
                settings.isMeteringEnabled = true // Improve exposure

                // Limit barcode formats for faster decoding
                val formats = listOf(
                    BarcodeFormat.QR_CODE,
                    BarcodeFormat.CODE_128,
                    BarcodeFormat.EAN_13,
                    BarcodeFormat.UPC_A,
                    BarcodeFormat.CODE_39
                )
                setDecoderFactory(DefaultDecoderFactory(formats))
            } ?: run {
                Log.e(TAG, "Scanner view is null")
                handler.post {
                    Toast.makeText(applicationContext, "Scanner unavailable", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            // Catch any initialization exceptions to prevent crash
            Log.e(TAG, "Error setting up scanner: ${e.message}", e)
            handler.post {
                Toast.makeText(applicationContext, "Error initializing scanner: ${e.message}", Toast.LENGTH_LONG).show()
                permissionButton?.visibility = View.VISIBLE
            }
        }
    }

    private fun toggleInputMode() {
        isKeyboardMode = !isKeyboardMode

        if (isKeyboardMode) {
            scannerView?.visibility = View.GONE
            keyboardView?.visibility = View.VISIBLE
            switchModeButton?.text = getString(R.string.switch_to_scanner)
            scannerIndicator?.visibility = View.GONE
            scannerView?.pause()
        } else {
            scannerView?.visibility = View.VISIBLE
            keyboardView?.visibility = View.GONE
            switchModeButton?.text = getString(R.string.switch_to_keyboard)
            scannerIndicator?.visibility = View.VISIBLE
            currentLayout = qwertyLayout
            keyboardView?.setKeyboardLayout(currentLayout!!)
            enforcePermission()
            scannerView?.resume()
        }
    }

    private fun toggleKeyboardMode(targetLayout: KeyboardLayout.LayoutType? = null) {
        if (targetLayout != null) {
            // Switch to the specified target layout
            currentLayout = when (targetLayout) {
                KeyboardLayout.LayoutType.QWERTY -> qwertyLayout
                KeyboardLayout.LayoutType.SYMBOLS -> symbolsLayout
                KeyboardLayout.LayoutType.SYMBOLS2 -> symbols2Layout
            }
        } else {
            // Default cycling behavior if no target layout specified
            currentLayout = when (currentLayout?.layoutType) {
                KeyboardLayout.LayoutType.QWERTY -> symbolsLayout
                KeyboardLayout.LayoutType.SYMBOLS -> symbols2Layout
                KeyboardLayout.LayoutType.SYMBOLS2 -> qwertyLayout
                null -> qwertyLayout
            }
        }
        keyboardView?.setKeyboardLayout(currentLayout!!)
    }

    private fun toggleTorch() {
        scannerView?.let { view ->
            try {
                if (isTorchOn) {
                    Log.d(TAG, "Turning torch OFF")
                    view.setTorchOff()
                } else {
                    Log.d(TAG, "Turning torch ON")
                    view.setTorchOn()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error toggling torch: ${e.message}", e)
                handler.post {
                    Toast.makeText(applicationContext, "Flashlight error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        } ?: run {
            Log.e(TAG, "Scanner view is null, can't toggle torch")
            handler.post {
                Toast.makeText(applicationContext, "Flashlight unavailable", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun suggestFlashlightIfNeeded() {
        scannerView?.apply {
            if (!isTorchOn && cameraSettings.isMeteringEnabled) {
                handler.postDelayed({
                    if (!isTorchOn) {
                        Toast.makeText(
                            applicationContext,
                            "Low light detected. Try turning on the flashlight.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }, 2000) // Suggest after 2 seconds
            }
        }
    }

    fun enforcePermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            launchPermissionActivity()
        } else {
            permissionButton?.visibility = View.GONE
        }
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        if (!isKeyboardMode) {
            Log.d(TAG, "Starting camera and setting result handler")
            enforcePermission()
            scannerView?.resume()
            suggestFlashlightIfNeeded()
        }
    }

    override fun onFinishInput() {
        super.onFinishInput()
        scannerView?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        
        // Release ToneGenerator resources
        toneGenerator?.release()
        toneGenerator = null
    }

    override fun onTorchOn() {
        Log.d(TAG, "Torch turned on")
        isTorchOn = true
        handler.post {
            updateFlashlightButtonStyle()
            Toast.makeText(applicationContext, "Flashlight ON", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onTorchOff() {
        Log.d(TAG, "Torch turned off")
        isTorchOn = false
        handler.post {
            updateFlashlightButtonStyle()
            Toast.makeText(applicationContext, "Flashlight OFF", Toast.LENGTH_SHORT).show()
        }
    }

    override fun barcodeResult(result: BarcodeResult) {
        result.resultPoints?.let { points ->
            if (points.isNotEmpty()) {
                val rect = calculateBarcodeRect(points)
                scannerIndicator?.setBarcodePosition(rect)
            }
        }
        handleResult(result.text)
    }

    private fun calculateBarcodeRect(points: Array<ResultPoint>): Rect {
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = 0f
        var maxY = 0f
        for (point in points) {
            minX = minOf(minX, point.x)
            minY = minOf(minY, point.y)
            maxX = maxOf(maxX, point.x)
            maxY = maxOf(maxY, point.y)
        }
        val padding = 20f
        return Rect(
            (minX - padding).toInt(),
            (minY - padding).toInt(),
            (maxX + padding).toInt(),
            (maxY + padding).toInt()
        )
    }

    fun handleResult(barcodeText: String) {
        Log.d(TAG, "Barcode scanned: $barcodeText")
        val currentTime = System.currentTimeMillis()
        val requiredDelay = if (barcodeText == lastText) SAME_BARCODE_DELAY_MS else DEBOUNCE_TIME_MS
        if (barcodeText == lastText && currentTime - lastTime < requiredDelay) {
            Log.d(TAG, "Skipping duplicate scan (delay: $requiredDelay ms)")
            if (currentTime - lastToastTime > 1000) {
                handler.post {
                    Toast.makeText(applicationContext, "Please wait before scanning same barcode", Toast.LENGTH_SHORT).show()
                }
                lastToastTime = currentTime
            }
            return
        }
        lastText = barcodeText
        lastTime = currentTime
        scannerIndicator?.setActive(true)

        try {
            // Add vibration feedback with proper version checking
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.let { vibrator ->
                    if (vibrator.hasVibrator()) {
                        vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.let {
                    if (it.hasVibrator()) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            it.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                        } else {
                            @Suppress("DEPRECATION")
                            it.vibrate(100)
                        }
                    }
                }
            }

            // Play beep sound
            playBeep()
        } catch (e: Exception) {
            // Catch any vibration or sound exceptions to prevent crash
            Log.e(TAG, "Error in haptic feedback: ${e.message}", e)
        }

        if (!isKeyboardMode && batchScanMode) {
            scannerView?.pause()
        }
        handler.postDelayed({
            scannerIndicator?.setActive(false)
            scannerIndicator?.clearBarcodePosition()
            if (!isKeyboardMode && batchScanMode) {
                scannerView?.resume()
            }
        }, INDICATOR_ACTIVE_DURATION_MS)
        
        currentInputConnection?.also { ic ->
            if (batchScanMode) {
                simulateTyping(ic, barcodeText)
                serviceScope.launch {
                    delay(barcodeText.length * TYPING_DELAY_MS + 50)
                    sendEnterKey(ic)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(applicationContext, "Scanned: $barcodeText", Toast.LENGTH_SHORT).show()
                        lastToastTime = System.currentTimeMillis()
                    }
                    delay(FOCUS_MAINTAIN_DELAY_MS)
                    requestShowSelf(0)
                }
            } else {
                simulateTyping(ic, barcodeText)
                serviceScope.launch {
                    delay(barcodeText.length * TYPING_DELAY_MS + 50)
                    sendEnterKey(ic)
                }
            }
        }
    }

    private fun sendEnterKey(ic: InputConnection) {
        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
    }

    private fun simulateTyping(ic: InputConnection, text: String) {
        serviceScope.launch {
            for (char in text) {
                ic.commitText(char.toString(), 1)
                delay(TYPING_DELAY_MS)
            }
        }
    }

    private fun playBeep() {
        toneGenerator?.let {
            try {
                // Using TONE_PROP_BEEP for a standard notification beep sound
                it.startTone(ToneGenerator.TONE_PROP_BEEP, BEEP_DURATION_MS)
                Log.d(TAG, "Beep sound played")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to play beep sound: ${e.message}", e)
            }
        }
    }

    override fun onKeyClick(key: KeyboardKey) {
        currentInputConnection?.let { ic ->
            when (key.code) {
                KeyboardKey.CODE_DELETE -> {
                    val selectedText = ic.getSelectedText(0)
                    if (selectedText != null && selectedText.isNotEmpty()) {
                        ic.commitText("", 1)
                    } else {
                        ic.deleteSurroundingText(1, 0)
                    }
                }
                KeyboardKey.CODE_SHIFT -> {
                    // Cycle through caps states: OFF -> SINGLE -> LOCK -> OFF
                    val shiftKey = currentLayout?.keys?.find { it.code == KeyboardKey.CODE_SHIFT }
                    
                    shiftKey?.let { shift ->
                        when (shift.capsState) {
                            KeyboardKey.CAPS_OFF -> {
                                shift.capsState = KeyboardKey.CAPS_SINGLE
                                currentLayout?.setCapsState(true)
                            }
                            KeyboardKey.CAPS_SINGLE -> {
                                shift.capsState = KeyboardKey.CAPS_LOCK
                                currentLayout?.setCapsState(true)
                            }
                            KeyboardKey.CAPS_LOCK -> {
                                shift.capsState = KeyboardKey.CAPS_OFF
                                currentLayout?.setCapsState(false)
                            }
                            else -> {
                                // Handle any other potential state
                                shift.capsState = KeyboardKey.CAPS_OFF
                                currentLayout?.setCapsState(false)
                            }
                        }
                    }
                    
                    keyboardView?.invalidate()
                }
                KeyboardKey.CODE_DONE -> sendEnterKey(ic)
                KeyboardKey.CODE_TAB -> {
                    ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_TAB))
                    ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_TAB))
                }
                KeyboardKey.CODE_TOGGLE -> {
                    // Use the targetLayout property to determine which layout to switch to
                    toggleKeyboardMode(key.targetLayout)
                }
                else -> {
                    val char = key.getCharacter()
                    if (char.isNotEmpty()) {
                        if (ic.getSelectedText(0) != null) {
                            ic.commitText(char, 1)
                        } else {
                            ic.commitText(char, 1)
                        }
                        
                        // If in CAPS_SINGLE mode, turn off caps after typing a letter
                        if (char.length == 1 && char[0].isLetter()) {
                            val shiftKey = currentLayout?.keys?.find { it.code == KeyboardKey.CODE_SHIFT }
                            if (shiftKey?.capsState == KeyboardKey.CAPS_SINGLE) {
                                shiftKey.capsState = KeyboardKey.CAPS_OFF
                                currentLayout?.setCapsState(false)
                                keyboardView?.invalidate()
                            } else {
                                // Do nothing for other cap states
                            }
                        } else {
                            // Not a letter, so caps state remains unchanged
                        }
                    } else {
                        Log.d(TAG, "Empty character from key: ${key.code}")
                    }
                }
            }
        }
    }
}