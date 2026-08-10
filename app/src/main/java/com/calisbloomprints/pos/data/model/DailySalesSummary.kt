package com.calisbloomprints.pos.data.model

import com.calisbloomprints.pos.data.db.SaleWithItems

data class DailySalesSummary(
    val saleCount: Int = 0,
    val grossSalesCents: Long = 0,
    val discountCents: Long = 0,
    val serviceFeeCents: Long = 0,
    val netSalesCents: Long = 0,
    val cashReceivedCents: Long = 0,
    val changeGivenCents: Long = 0,
    val balanceDueCents: Long = 0,
) {
    companion object {
        fun fromSales(sales: List<SaleWithItems>): DailySalesSummary {
            return DailySalesSummary(
                saleCount = sales.size,
                grossSalesCents = sales.sumOf { it.sale.subtotalCents },
                discountCents = sales.sumOf { it.sale.discountCents },
                serviceFeeCents = sales.sumOf { it.sale.serviceFeeCents },
                netSalesCents = sales.sumOf { it.sale.totalCents },
                cashReceivedCents = sales.sumOf { it.sale.amountTenderedCents },
                changeGivenCents = sales.sumOf { it.sale.changeCents },
                balanceDueCents = sales.sumOf { it.sale.balanceDueCents },
            )
        }
    }
}
