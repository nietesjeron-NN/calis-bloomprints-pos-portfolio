package com.calisbloomprints.pos.data.model

import com.calisbloomprints.pos.data.db.entity.ProductEntity

data class CartLine(
    val product: ProductEntity,
    val quantity: Int,
    val customUnitPriceCents: Long? = null,
) {
    val unitPriceCents: Long
        get() = customUnitPriceCents ?: product.priceCents

    val lineTotalCents: Long
        get() = unitPriceCents * quantity
}
