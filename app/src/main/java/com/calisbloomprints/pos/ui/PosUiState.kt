package com.calisbloomprints.pos.ui

import com.calisbloomprints.pos.data.db.SaleWithItems
import com.calisbloomprints.pos.data.db.entity.ProductEntity
import com.calisbloomprints.pos.data.db.entity.StockMovementEntity
import com.calisbloomprints.pos.data.model.CartLine
import com.calisbloomprints.pos.data.model.DailySalesSummary
import com.calisbloomprints.pos.data.model.MoneyFormatter
import com.calisbloomprints.pos.data.model.ProductCategory

data class PosUiState(
    val products: List<ProductEntity> = emptyList(),
    val inventoryProducts: List<ProductEntity> = emptyList(),
    val selectedCategory: ProductCategory? = null,
    val cartLines: List<CartLine> = emptyList(),
    val customerName: String = "",
    val customerContact: String = "",
    val orderNotes: String = "",
    val pickupDate: String = "",
    val pickupTime: String = "",
    val pickupReminderMinutes: Int = 60,
    val discount: String = "",
    val serviceFee: String = "",
    val deposit: String = "",
    val cashReceived: String = "",
    val recentSales: List<SaleWithItems> = emptyList(),
    val allSales: List<SaleWithItems> = emptyList(),
    val pickupOrders: List<SaleWithItems> = emptyList(),
    val todaySummary: DailySalesSummary = DailySalesSummary(),
    val stockMovements: List<StockMovementEntity> = emptyList(),
    val isCheckingOut: Boolean = false,
    val message: String? = null,
    val pendingPrintSale: SaleWithItems? = null,
    val needsNotificationPermission: Boolean = false,
) {
    val visibleProducts: List<ProductEntity>
        get() = selectedCategory?.let { category ->
            products.filter { it.category == category }
        } ?: products

    val cartTotalCents: Long
        get() = cartLines.sumOf { it.lineTotalCents }

    val discountCents: Long
        get() = MoneyFormatter.parseToCents(discount) ?: 0L

    val serviceFeeCents: Long
        get() = MoneyFormatter.parseToCents(serviceFee) ?: 0L

    val orderTotalCents: Long
        get() = (cartTotalCents - discountCents + serviceFeeCents).coerceAtLeast(0L)

    val depositCents: Long
        get() = MoneyFormatter.parseToCents(deposit) ?: 0L

    val amountDueNowCents: Long
        get() = if (depositCents > 0L) depositCents else orderTotalCents

    val balanceDueCents: Long
        get() = (orderTotalCents - amountDueNowCents).coerceAtLeast(0L)

    val amountTenderedCents: Long?
        get() = MoneyFormatter.parseToCents(cashReceived)

    val changeCents: Long
        get() = ((amountTenderedCents ?: 0L) - amountDueNowCents).coerceAtLeast(0L)

    val canCheckout: Boolean
        get() = cartLines.isNotEmpty() &&
            !isCheckingOut
}
