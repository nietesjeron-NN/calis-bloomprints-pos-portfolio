package com.calisbloomprints.pos.barcode

import com.calisbloomprints.pos.data.db.entity.ProductEntity

fun ProductEntity.displayBarcode(): String {
    return barcode?.trim()
        ?.takeIf { it.isNotBlank() }
        ?: "CBK-${id.toString().padStart(5, '0')}"
}
