package com.calisbloomprints.pos.barcode

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF

object Code128Barcode {
    fun draw(canvas: Canvas, rawValue: String, bounds: RectF, paint: Paint) {
        val codes = encode(rawValue)
        val totalModules = codes.sumOf { CODE_PATTERNS[it].sumOf { width -> width.digitToInt() } }
        val moduleWidth = bounds.width() / totalModules.toFloat()
        var x = bounds.left
        paint.style = Paint.Style.FILL

        codes.forEach { code ->
            var black = true
            CODE_PATTERNS[code].forEach { char ->
                val width = char.digitToInt() * moduleWidth
                if (black) {
                    canvas.drawRect(x, bounds.top, x + width, bounds.bottom, paint)
                }
                x += width
                black = !black
            }
        }
    }

    private fun encode(rawValue: String): List<Int> {
        val value = rawValue
            .trim()
            .ifBlank { "00000" }
            .map { char ->
                if (char.code in 32..126) char else '-'
            }
            .joinToString("")

        val dataCodes = value.map { it.code - 32 }
        val checksum = (START_B + dataCodes.mapIndexed { index, code -> code * (index + 1) }.sum()) % 103
        return listOf(START_B) + dataCodes + checksum + STOP
    }

    private const val START_B = 104
    private const val STOP = 106

    private val CODE_PATTERNS = listOf(
        "212222",
        "222122",
        "222221",
        "121223",
        "121322",
        "131222",
        "122213",
        "122312",
        "132212",
        "221213",
        "221312",
        "231212",
        "112232",
        "122132",
        "122231",
        "113222",
        "123122",
        "123221",
        "223211",
        "221132",
        "221231",
        "213212",
        "223112",
        "312131",
        "311222",
        "321122",
        "321221",
        "312212",
        "322112",
        "322211",
        "212123",
        "212321",
        "232121",
        "111323",
        "131123",
        "131321",
        "112313",
        "132113",
        "132311",
        "211313",
        "231113",
        "231311",
        "112133",
        "112331",
        "132131",
        "113123",
        "113321",
        "133121",
        "313121",
        "211331",
        "231131",
        "213113",
        "213311",
        "213131",
        "311123",
        "311321",
        "331121",
        "312113",
        "312311",
        "332111",
        "314111",
        "221411",
        "431111",
        "111224",
        "111422",
        "121124",
        "121421",
        "141122",
        "141221",
        "112214",
        "112412",
        "122114",
        "122411",
        "142112",
        "142211",
        "241211",
        "221114",
        "413111",
        "241112",
        "134111",
        "111242",
        "121142",
        "121241",
        "114212",
        "124112",
        "124211",
        "411212",
        "421112",
        "421211",
        "212141",
        "214121",
        "412121",
        "111143",
        "111341",
        "131141",
        "114113",
        "114311",
        "411113",
        "411311",
        "113141",
        "114131",
        "311141",
        "411131",
        "211412",
        "211214",
        "211232",
        "2331112",
    )
}
