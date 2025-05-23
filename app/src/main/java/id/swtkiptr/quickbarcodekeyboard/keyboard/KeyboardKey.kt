package id.swtkiptr.keyboardscanner.keyboard

import id.swtkiptr.keyboardscanner.keyboard.KeyboardLayout.LayoutType

/**
 * Model class for a keyboard key, replacing the deprecated Keyboard.Key
 */
class KeyboardKey(
    val code: Int,
    val label: String,
    var isCaps: Boolean = false,
    val width: Int = 1, // Width multiplier (1 for standard key, 2 for wide keys like space)
    val isSpecial: Boolean = false, // For special keys like shift, delete, etc.
    val targetLayout: LayoutType? = null // Target layout for toggle keys
) {
    companion object {
        // Define special key codes to replace deprecated Keyboard class constants
        const val CODE_SHIFT = -1
        const val CODE_DELETE = -5
        const val CODE_DONE = -4
        const val CODE_TOGGLE = -3
        const val CODE_TAB = -2
        const val CODE_SPACE = 32
        
        // Caps modes
        const val CAPS_OFF = 0
        const val CAPS_SINGLE = 1  // Only first letter (auto-off after typing a letter)
        const val CAPS_LOCK = 2    // All letters capitalized
    }
    
    // Track caps lock state
    var capsState: Int = CAPS_OFF

    /**
     * Returns the display text for this key, considering capitalization
     */
    fun getDisplayLabel(): String {
        return when (code) {
            CODE_SPACE -> "Space" // Display "Space" for clarity
            CODE_DELETE -> "⌫"
            CODE_DONE -> "↵"
            CODE_SHIFT -> when (capsState) {
                CAPS_SINGLE -> "⇧"     // Single letter cap
                CAPS_LOCK -> "⇧⇧"      // Caps lock
                else -> "⇧"            // Off
            }
            CODE_TOGGLE -> label // Use the label set in KeyboardLayout (e.g., "123", "#+=", "ABC")
            else -> {
                if (isCaps && label.length == 1 && label[0].isLetter()) {
                    label.uppercase()
                } else {
                    label
                }
            }
        }
    }

    /**
     * Get the actual character for this key, considering capitalization
     */
    fun getCharacter(): String {
        if (code == CODE_SPACE) return " "
        if (isSpecial) return label
        return if (isCaps && label.length == 1 && label[0].isLetter()) {
            label.uppercase()
        } else {
            label
        }
    }
}