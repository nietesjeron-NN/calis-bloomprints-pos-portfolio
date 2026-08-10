package com.calisbloomprints.pos.data.db

import androidx.room.Embedded
import androidx.room.Relation
import com.calisbloomprints.pos.data.db.entity.SaleEntity
import com.calisbloomprints.pos.data.db.entity.SaleItemEntity

data class SaleWithItems(
    @Embedded
    val sale: SaleEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "saleId",
    )
    val items: List<SaleItemEntity>,
)
