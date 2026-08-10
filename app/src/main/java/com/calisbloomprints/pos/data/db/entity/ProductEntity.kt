package com.calisbloomprints.pos.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.calisbloomprints.pos.data.model.ProductCategory

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val barcode: String? = null,
    val category: ProductCategory,
    val priceCents: Long,
    val trackStock: Boolean,
    val stockQuantity: Int?,
    val lowStockThreshold: Int = 3,
    val active: Boolean = true,
    val sortOrder: Int = 0,
)
