package id.swtkiptr.keyboardscanner.scanner

import android.graphics.Rect
import kotlin.math.min

/**
 * Utility class untuk mengatur viewport scanner untuk optimasi pemindaian barcode
 */
class ViewportUtil {
    companion object {
        /**
         * Menentukan area pemindaian yang optimal untuk dapat mendeteksi barcode miring
         *
         * @param width Lebar viewport
         * @param height Tinggi viewport
         * @param percentOfScreen Persentase dari layar yang akan digunakan (0.0-1.0)
         * @return Rect yang berisi area pemindaian yang dioptimalkan
         */
        fun getFramingRect(width: Int, height: Int, percentOfScreen: Float): Rect {
            if (width <= 0 || height <= 0) {
                return Rect(0, 0, 1600, 900) // Ukuran default jika dimensi tidak valid
            }

            val screenDimension = min(width, height)
            val resultDimension = (screenDimension * percentOfScreen).toInt()
            
            val leftOffset = (width - resultDimension) / 2
            val topOffset = (height - resultDimension) / 2
            
            return Rect(
                leftOffset,
                topOffset,
                leftOffset + resultDimension,
                topOffset + resultDimension
            )
        }
    }
}