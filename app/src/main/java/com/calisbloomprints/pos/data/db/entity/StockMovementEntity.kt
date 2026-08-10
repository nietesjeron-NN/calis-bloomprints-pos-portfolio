package com.calisbloomprints.pos.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stock_movements",
    indices = [Index("productId"), Index("createdAt")],
)
data class StockMovementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productId: Long,
    val productName: String,
    val createdAt: Long,
    val delta: Int,
    val resultingStock: Int,
    val reason: String,
)
