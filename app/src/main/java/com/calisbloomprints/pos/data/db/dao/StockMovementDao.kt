package com.calisbloomprints.pos.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.calisbloomprints.pos.data.db.entity.StockMovementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StockMovementDao {
    @Insert
    suspend fun insert(movement: StockMovementEntity)

    @Query("SELECT * FROM stock_movements ORDER BY createdAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 30): Flow<List<StockMovementEntity>>
}
