package com.calisbloomprints.pos.data.repository

import androidx.room.withTransaction
import com.calisbloomprints.pos.data.db.BloomprintsDatabase
import com.calisbloomprints.pos.data.db.SaleWithItems
import com.calisbloomprints.pos.data.db.entity.SaleEntity
import com.calisbloomprints.pos.data.db.entity.SaleItemEntity
import com.calisbloomprints.pos.data.db.entity.StockMovementEntity
import com.calisbloomprints.pos.data.model.CartLine
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SalesRepository(
    private val database: BloomprintsDatabase,
) {
    private val saleDao = database.saleDao()
    private val productDao = database.productDao()
    private val stockMovementDao = database.stockMovementDao()

    val recentSales: Flow<List<SaleWithItems>> = saleDao.observeRecentSales()
    val allSales: Flow<List<SaleWithItems>> = saleDao.observeAllSales()
    val todaySales: Flow<List<SaleWithItems>> = saleDao.observeSalesSince(startOfToday())
    val pickupOrders: Flow<List<SaleWithItems>> = saleDao.observePickupOrders()

    suspend fun recordCashSale(
        lines: List<CartLine>,
        customerName: String,
        customerContact: String,
        notes: String,
        discountCents: Long,
        serviceFeeCents: Long,
        depositCents: Long,
        amountTenderedCents: Long,
        pickupAt: Long?,
        pickupReminderAt: Long?,
    ): Result<SaleWithItems> = runCatching {
        require(lines.isNotEmpty()) { "Add at least one item before checkout." }

        val subtotalCents = lines.sumOf { it.lineTotalCents }
        require(discountCents >= 0) { "Discount cannot be negative." }
        require(serviceFeeCents >= 0) { "Service fee cannot be negative." }
        require(depositCents >= 0) { "Deposit cannot be negative." }
        require(discountCents <= subtotalCents) { "Discount cannot be more than subtotal." }

        val orderTotalCents = (subtotalCents - discountCents + serviceFeeCents).coerceAtLeast(0)
        require(depositCents <= orderTotalCents) { "Deposit cannot be more than the total." }
        pickupAt?.let {
            require(it > System.currentTimeMillis()) { "Pickup time must be in the future." }
        }

        val amountDueNowCents = if (depositCents > 0L) depositCents else orderTotalCents
        val balanceDueCents = (orderTotalCents - amountDueNowCents).coerceAtLeast(0)
        require(amountTenderedCents >= amountDueNowCents) { "Cash received is less than amount due now." }

        database.withTransaction {
            lines.forEach { line ->
                val product = productDao.findById(line.product.id)
                    ?: error("${line.product.name} is no longer in inventory.")
                if (product.trackStock) {
                    val currentStock = product.stockQuantity ?: 0
                    require(currentStock >= line.quantity) {
                        "Not enough stock for ${product.name}."
                    }
                }
            }

            val now = System.currentTimeMillis()
            val saleId = saleDao.insertSale(
                SaleEntity(
                    receiptNumber = buildReceiptNumber(now),
                    createdAt = now,
                    pickupAt = pickupAt,
                    pickupReminderAt = pickupReminderAt,
                    customerName = customerName.trim().ifBlank { null },
                    customerContact = customerContact.trim().ifBlank { null },
                    notes = notes.trim().ifBlank { null },
                    subtotalCents = subtotalCents,
                    discountCents = discountCents,
                    serviceFeeCents = serviceFeeCents,
                    totalCents = orderTotalCents,
                    depositCents = depositCents,
                    balanceDueCents = balanceDueCents,
                    amountTenderedCents = amountTenderedCents,
                    changeCents = amountTenderedCents - amountDueNowCents,
                ),
            )

            saleDao.insertItems(
                lines.map { line ->
                    SaleItemEntity(
                        saleId = saleId,
                        productId = line.product.id,
                        productName = line.product.name,
                        category = line.product.category,
                        quantity = line.quantity,
                        unitPriceCents = line.unitPriceCents,
                        lineTotalCents = line.lineTotalCents,
                    )
                },
            )

            lines.forEach { line ->
                if (line.product.trackStock) {
                    val product = productDao.findById(line.product.id) ?: return@forEach
                    val currentStock = product.stockQuantity ?: 0
                    val nextStock = (currentStock - line.quantity).coerceAtLeast(0)
                    productDao.setStock(line.product.id, nextStock)
                    stockMovementDao.insert(
                        StockMovementEntity(
                            productId = line.product.id,
                            productName = product.name,
                            createdAt = now,
                            delta = nextStock - currentStock,
                            resultingStock = nextStock,
                            reason = "Sale ${buildReceiptNumber(now)}",
                        ),
                    )
                }
            }

            saleDao.findSaleWithItems(saleId) ?: error("Sale was saved but could not be reloaded.")
        }
    }

    suspend fun findSale(saleId: Long): SaleWithItems? = saleDao.findSaleWithItems(saleId)

    private fun buildReceiptNumber(createdAt: Long): String {
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date(createdAt))
        return "CBK-$stamp"
    }

    private fun startOfToday(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
