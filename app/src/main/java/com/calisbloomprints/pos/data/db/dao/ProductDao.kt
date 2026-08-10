package com.calisbloomprints.pos.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.calisbloomprints.pos.data.db.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE active = 1 ORDER BY sortOrder ASC, name ASC")
    fun observeActiveProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products ORDER BY active DESC, sortOrder ASC, name ASC")
    fun observeAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :productId LIMIT 1")
    suspend fun findById(productId: Long): ProductEntity?

    @Query("SELECT COUNT(*) FROM products")
    suspend fun countProducts(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(products: List<ProductEntity>)

    @Upsert
    suspend fun upsert(product: ProductEntity)

    @Query("UPDATE products SET stockQuantity = :stockQuantity WHERE id = :productId")
    suspend fun setStock(productId: Long, stockQuantity: Int)

    @Query("UPDATE products SET active = :active WHERE id = :productId")
    suspend fun setActive(productId: Long, active: Boolean)

    @Query("DELETE FROM products WHERE id = :productId")
    suspend fun delete(productId: Long)
}
