package com.calisbloomprints.pos.data.model

enum class ReceiptWidth(
    val label: String,
    val charsPerLine: Int,
    val paperDots: Int,
    val logoMaxDots: Int,
) {
    MM_58("58 mm", 32, 384, 256),
    MM_80("80 mm", 48, 576, 384),
}
