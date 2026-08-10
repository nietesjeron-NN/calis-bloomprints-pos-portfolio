package com.calisbloomprints.pos.data.db

import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.calisbloomprints.pos.data.db.dao.PrinterSettingsDao
import com.calisbloomprints.pos.data.db.dao.ProductDao
import com.calisbloomprints.pos.data.db.dao.SaleDao
import com.calisbloomprints.pos.data.db.dao.StockMovementDao
import com.calisbloomprints.pos.data.db.entity.PrinterSettingsEntity
import com.calisbloomprints.pos.data.db.entity.ProductEntity
import com.calisbloomprints.pos.data.db.entity.SaleEntity
import com.calisbloomprints.pos.data.db.entity.SaleItemEntity
import com.calisbloomprints.pos.data.db.entity.StockMovementEntity

@Database(
    entities = [
        ProductEntity::class,
        SaleEntity::class,
        SaleItemEntity::class,
        PrinterSettingsEntity::class,
        StockMovementEntity::class,
    ],
    version = 6,
    exportSchema = false,
)
@TypeConverters(PosTypeConverters::class)
abstract class BloomprintsDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun saleDao(): SaleDao
    abstract fun printerSettingsDao(): PrinterSettingsDao
    abstract fun stockMovementDao(): StockMovementDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE products ADD COLUMN lowStockThreshold INTEGER NOT NULL DEFAULT 3")
                db.execSQL("ALTER TABLE sales ADD COLUMN serviceFeeCents INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE sales ADD COLUMN depositCents INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE sales ADD COLUMN balanceDueCents INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE printer_settings ADD COLUMN shopName TEXT NOT NULL DEFAULT 'CALI''S BLOOMPRINTS'")
                db.execSQL("ALTER TABLE printer_settings ADD COLUMN shopSubtitle TEXT NOT NULL DEFAULT 'AND KEEPSAKES'")
                db.execSQL("ALTER TABLE printer_settings ADD COLUMN receiptFooter TEXT NOT NULL DEFAULT 'Thank you! Please come again.'")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS stock_movements (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        productId INTEGER NOT NULL,
                        productName TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        delta INTEGER NOT NULL,
                        resultingStock INTEGER NOT NULL,
                        reason TEXT NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_stock_movements_productId ON stock_movements(productId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_stock_movements_createdAt ON stock_movements(createdAt)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE printer_settings ADD COLUMN logoUri TEXT")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sales ADD COLUMN pickupAt INTEGER")
                db.execSQL("ALTER TABLE sales ADD COLUMN pickupReminderAt INTEGER")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sales_pickupAt ON sales(pickupAt)")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE printer_settings ADD COLUMN printLogoOnReceipts INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE products ADD COLUMN barcode TEXT")
                db.execSQL("UPDATE products SET barcode = 'CBK-' || printf('%05d', id) WHERE barcode IS NULL")
            }
        }
    }
}
