package com.calisbloomprints.pos.data.repository

import com.calisbloomprints.pos.data.db.dao.PrinterSettingsDao
import com.calisbloomprints.pos.data.db.entity.PrinterSettingsEntity
import com.calisbloomprints.pos.data.model.ReceiptWidth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PrinterSettingsRepository(
    private val printerSettingsDao: PrinterSettingsDao,
) {
    val settings: Flow<PrinterSettingsEntity> = printerSettingsDao
        .observeSettings()
        .map { it ?: PrinterSettingsEntity() }

    suspend fun getSettings(): PrinterSettingsEntity {
        return printerSettingsDao.getSettings() ?: PrinterSettingsEntity()
    }

    suspend fun saveSelectedPrinter(name: String?, address: String?) {
        val current = getSettings()
        printerSettingsDao.save(
            current.copy(
                deviceName = name,
                deviceAddress = address,
            ),
        )
    }

    suspend fun saveWidth(width: ReceiptWidth) {
        val current = getSettings()
        printerSettingsDao.save(current.copy(width = width))
    }

    suspend fun setAutoPrintReceipts(enabled: Boolean) {
        val current = getSettings()
        printerSettingsDao.save(current.copy(autoPrintReceipts = enabled))
    }

    suspend fun saveReceiptBranding(shopName: String, shopSubtitle: String, receiptFooter: String) {
        val current = getSettings()
        printerSettingsDao.save(
            current.copy(
                shopName = shopName.trim().ifBlank { "CALI'S BLOOMPRINTS" },
                shopSubtitle = shopSubtitle.trim(),
                receiptFooter = receiptFooter.trim().ifBlank { "Thank you! Please come again." },
            ),
        )
    }

    suspend fun saveLogoUri(logoUri: String?) {
        val current = getSettings()
        printerSettingsDao.save(current.copy(logoUri = logoUri))
    }

    suspend fun savePrintLogoOnReceipts(enabled: Boolean) {
        val current = getSettings()
        printerSettingsDao.save(current.copy(printLogoOnReceipts = enabled))
    }
}
