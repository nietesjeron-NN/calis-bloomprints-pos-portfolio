package com.calisbloomprints.pos.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.calisbloomprints.pos.data.db.SaleWithItems
import com.calisbloomprints.pos.data.db.entity.SaleEntity
import com.calisbloomprints.pos.data.db.entity.SaleItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleDao {
    @Insert
    suspend fun insertSale(sale: SaleEntity): Long

    @Insert
    suspend fun insertItems(items: List<SaleItemEntity>)

    @Transaction
    @Query("SELECT * FROM sales ORDER BY createdAt DESC LIMIT :limit")
    fun observeRecentSales(limit: Int = 50): Flow<List<SaleWithItems>>

    @Transaction
    @Query("SELECT * FROM sales ORDER BY createdAt DESC")
    fun observeAllSales(): Flow<List<SaleWithItems>>

    @Transaction
    @Query("SELECT * FROM sales WHERE createdAt >= :startAt ORDER BY createdAt DESC")
    fun observeSalesSince(startAt: Long): Flow<List<SaleWithItems>>

    @Transaction
    @Query("SELECT * FROM sales WHERE pickupAt IS NOT NULL ORDER BY pickupAt ASC LIMIT :limit")
    fun observePickupOrders(limit: Int = 100): Flow<List<SaleWithItems>>

    @Transaction
    @Query("SELECT * FROM sales WHERE id = :saleId LIMIT 1")
    suspend fun findSaleWithItems(saleId: Long): SaleWithItems?
}
