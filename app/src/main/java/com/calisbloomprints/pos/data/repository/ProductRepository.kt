package com.calisbloomprints.pos.data.repository

import com.calisbloomprints.pos.data.db.dao.ProductDao
import com.calisbloomprints.pos.data.db.dao.StockMovementDao
import com.calisbloomprints.pos.data.db.entity.ProductEntity
import com.calisbloomprints.pos.data.db.entity.StockMovementEntity
import kotlinx.coroutines.flow.Flow
import kotlin.math.max

class ProductRepository(
    private val productDao: ProductDao,
    private val stockMovementDao: StockMovementDao,
) {
    val activeProducts: Flow<List<ProductEntity>> = productDao.observeActiveProducts()
    val allProducts: Flow<List<ProductEntity>> = productDao.observeAllProducts()
    val recentStockMovements: Flow<List<StockMovementEntity>> = stockMovementDao.observeRecent()

    suspend fun ensureSeedData() {
        if (productDao.countProducts() == 0) {
            productDao.insertAll(StarterCatalog.products)
        }
    }

    suspend fun adjustStock(productId: Long, delta: Int) {
        val product = productDao.findById(productId) ?: return
        if (!product.trackStock) return

        val currentStock = product.stockQuantity ?: 0
        val nextStock = max(0, currentStock + delta)
        productDao.setStock(productId, nextStock)
        stockMovementDao.insert(
            StockMovementEntity(
                productId = productId,
                productName = product.name,
                createdAt = System.currentTimeMillis(),
                delta = nextStock - currentStock,
                resultingStock = nextStock,
                reason = "Manual adjustment",
            ),
        )
    }

    suspend fun setActive(productId: Long, active: Boolean) {
        productDao.setActive(productId, active)
    }

    suspend fun deleteProduct(productId: Long) {
        val product = productDao.findById(productId) ?: return
        productDao.delete(productId)

        if (product.trackStock) {
            stockMovementDao.insert(
                StockMovementEntity(
                    productId = productId,
                    productName = product.name,
                    createdAt = System.currentTimeMillis(),
                    delta = -(product.stockQuantity ?: 0),
                    resultingStock = 0,
                    reason = "Product deleted",
                ),
            )
        }
    }

    suspend fun saveProduct(product: ProductEntity) {
        val oldProduct = if (product.id == 0L) null else productDao.findById(product.id)
        productDao.upsert(product)

        if (product.id != 0L && product.trackStock) {
            val oldStock = oldProduct?.stockQuantity ?: 0
            val nextStock = product.stockQuantity ?: 0
            if (oldStock != nextStock) {
                stockMovementDao.insert(
                    StockMovementEntity(
                        productId = product.id,
                        productName = product.name,
                        createdAt = System.currentTimeMillis(),
                        delta = nextStock - oldStock,
                        resultingStock = nextStock,
                        reason = "Product edit",
                    ),
                )
            }
        }
    }
}
