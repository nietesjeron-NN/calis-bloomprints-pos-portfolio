package com.calisbloomprints.pos

import android.content.Context
import androidx.room.Room
import com.calisbloomprints.pos.data.db.BloomprintsDatabase
import com.calisbloomprints.pos.data.repository.PrinterSettingsRepository
import com.calisbloomprints.pos.data.repository.ProductRepository
import com.calisbloomprints.pos.data.repository.SalesRepository
import com.calisbloomprints.pos.pickup.PickupReminderScheduler
import com.calisbloomprints.pos.printer.BluetoothEscPosPrinterService

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: BloomprintsDatabase = Room.databaseBuilder(
        appContext,
        BloomprintsDatabase::class.java,
        "calis-bloomprints-pos.db",
    ).addMigrations(
        BloomprintsDatabase.MIGRATION_1_2,
        BloomprintsDatabase.MIGRATION_2_3,
        BloomprintsDatabase.MIGRATION_3_4,
        BloomprintsDatabase.MIGRATION_4_5,
        BloomprintsDatabase.MIGRATION_5_6,
    ).build()

    val productRepository = ProductRepository(database.productDao(), database.stockMovementDao())
    val salesRepository = SalesRepository(database)
    val printerSettingsRepository = PrinterSettingsRepository(database.printerSettingsDao())
    val printerService = BluetoothEscPosPrinterService(appContext)
    val pickupReminderScheduler = PickupReminderScheduler(appContext)
}
