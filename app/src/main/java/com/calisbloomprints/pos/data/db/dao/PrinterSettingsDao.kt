package com.calisbloomprints.pos.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.calisbloomprints.pos.data.db.entity.PrinterSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PrinterSettingsDao {
    @Query("SELECT * FROM printer_settings WHERE id = 1 LIMIT 1")
    fun observeSettings(): Flow<PrinterSettingsEntity?>

    @Query("SELECT * FROM printer_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettings(): PrinterSettingsEntity?

    @Upsert
    suspend fun save(settings: PrinterSettingsEntity)
}
