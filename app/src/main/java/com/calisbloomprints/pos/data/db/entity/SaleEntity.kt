package com.calisbloomprints.pos.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.calisbloomprints.pos.data.model.PaymentMethod

@Entity(
    tableName = "sales",
    indices = [
        Index(value = ["receiptNumber"], unique = true),
        Index(value = ["pickupAt"]),
    ],
)
data class SaleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val receiptNumber: String,
    val createdAt: Long,
    val pickupAt: Long? = null,
    val pickupReminderAt: Long? = null,
    val customerName: String?,
    val customerContact: String?,
    val notes: String?,
    val subtotalCents: Long,
    val discountCents: Long = 0,
    val serviceFeeCents: Long = 0,
    val totalCents: Long,
    val depositCents: Long = 0,
    val balanceDueCents: Long = 0,
    val amountTenderedCents: Long,
    val changeCents: Long,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
)
