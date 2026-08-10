package com.calisbloomprints.pos.data.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

object MoneyFormatter {
    fun format(cents: Long): String {
        return "PHP " + String.format(Locale.US, "%,.2f", cents / 100.0)
    }

    fun parseToCents(raw: String): Long? {
        val cleaned = raw
            .replace(",", "")
            .replace("PHP", "", ignoreCase = true)
            .trim()

        if (cleaned.isBlank()) return null

        return runCatching {
            BigDecimal(cleaned)
                .setScale(2, RoundingMode.HALF_UP)
                .movePointRight(2)
                .longValueExact()
        }.getOrNull()
    }
}
