package id.swtkiptr.keyboardscanner.keyboard

/**
 * Class that represents a keyboard layout, replacing the deprecated Keyboard class
 */
class KeyboardLayout(
    val keys: List<KeyboardKey>,
    val layoutType: LayoutType
) {
    enum class LayoutType {
        QWERTY, SYMBOLS, SYMBOLS2
    }

    companion object {
        /**
         * Create a standard QWERTY layout (10, 9, 9, 5 keys)
         */
        fun createQwertyLayout(): KeyboardLayout {
            val keys = mutableListOf<KeyboardKey>()

            "qwertyuiop".forEach { char ->
                keys.add(KeyboardKey(char.code, char.toString()))
            }

            "asdfghjkl".forEach { char ->
                keys.add(KeyboardKey(char.code, char.toString()))
            }

            keys.add(KeyboardKey(KeyboardKey.CODE_SHIFT, "⇧", isSpecial = true))

            "zxcvbnm".forEach { char ->
                keys.add(KeyboardKey(char.code, char.toString()))
            }

            keys.add(KeyboardKey(KeyboardKey.CODE_DELETE, "⌫", isSpecial = true))

            // Fourth row - space, toggle, punctuation
            keys.add(KeyboardKey(KeyboardKey.CODE_TOGGLE, "123", isSpecial = true, targetLayout = LayoutType.SYMBOLS))
            keys.add(KeyboardKey(','.code, ",", width = 1, isSpecial = true))
            keys.add(KeyboardKey(KeyboardKey.CODE_SPACE, " ", width = 2, isSpecial = true))
            keys.add(KeyboardKey('.'.code, ".", width = 1, isSpecial = true))
            keys.add(KeyboardKey(KeyboardKey.CODE_DONE, "↵", isSpecial = true))

            return KeyboardLayout(keys, LayoutType.QWERTY)
        }

        /**
         * Create a symbols layout (10, 10, 10, 5 keys)
         */
        fun createSymbolsLayout(): KeyboardLayout {
            val keys = mutableListOf<KeyboardKey>()

            // First row - numbers
            "1234567890".forEach { char ->
                keys.add(KeyboardKey(char.code, char.toString()))
            }

            // Second row - symbols
            "@#\$%&-+()".forEach { char ->
                keys.add(KeyboardKey(char.code, char.toString()))
            }

            keys.add(KeyboardKey(KeyboardKey.CODE_TOGGLE, "#+=", isSpecial = true, targetLayout = LayoutType.SYMBOLS2))

            // Third row - more symbols
            "*\"':;!?".forEach { char ->
                keys.add(KeyboardKey(char.code, char.toString()))
            }
            keys.add(KeyboardKey(KeyboardKey.CODE_DELETE, "⌫", isSpecial = true))


            // Fourth row - special keys
            keys.add(KeyboardKey(KeyboardKey.CODE_TOGGLE, "ABC", isSpecial = true, targetLayout = LayoutType.QWERTY))
            keys.add(KeyboardKey(','.code, ",", width = 1, isSpecial = true))
            keys.add(KeyboardKey(KeyboardKey.CODE_SPACE, "   ", width = 2, isSpecial = true))
            keys.add(KeyboardKey('.'.code, ".", width = 1, isSpecial = true))
            keys.add(KeyboardKey(KeyboardKey.CODE_DONE, "↵", isSpecial = true))

            return KeyboardLayout(keys, LayoutType.SYMBOLS)
        }

        /**
         * Create a second symbols layout with extra space (9, 10, 9, 4 keys)
         */
        fun createSymbol2Layout(): KeyboardLayout {
            val keys = mutableListOf<KeyboardKey>()

            // First row - additional symbols (9 keys)
            "~`|•√π÷×¶∆".forEach { char ->
                keys.add(KeyboardKey(char.code, char.toString()))
            }

            // Second row - currency and other symbols (10 keys)
            "£¢€¥^°={}".forEach { char ->
                keys.add(KeyboardKey(char.code, char.toString()))
            }
            keys.add(KeyboardKey(KeyboardKey.CODE_TOGGLE, "123", isSpecial = true, targetLayout = LayoutType.SYMBOLS))

            // Third row - math and misc symbols (9 keys)
            "\\©®™℅[]".forEach { char ->
                keys.add(KeyboardKey(char.code, char.toString()))
            }
            keys.add(KeyboardKey(KeyboardKey.CODE_DELETE, "⌫", isSpecial = true))

            // Fourth row - special keys
            keys.add(KeyboardKey(KeyboardKey.CODE_TOGGLE, "ABC", isSpecial = true, targetLayout = LayoutType.QWERTY))
            keys.add(KeyboardKey(','.code, ",", width = 1, isSpecial = true))
            keys.add(KeyboardKey(KeyboardKey.CODE_SPACE, "   ", width = 2, isSpecial = true))
            keys.add(KeyboardKey('.'.code, ".", width = 1, isSpecial = true))
            keys.add(KeyboardKey(KeyboardKey.CODE_DONE, "↵", isSpecial = true))

            return KeyboardLayout(keys, LayoutType.SYMBOLS2)
        }
    }

    /**
     * Update all alphabetic keys with caps state
     */
    fun setCapsState(isCaps: Boolean) {
        keys.filter { it.label.length == 1 && it.label[0].isLetter() }
            .forEach { it.isCaps = isCaps }
    }
}