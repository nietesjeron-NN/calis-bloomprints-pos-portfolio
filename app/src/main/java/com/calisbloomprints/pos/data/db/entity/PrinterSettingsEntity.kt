package com.calisbloomprints.pos.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.calisbloomprints.pos.data.model.ReceiptWidth

@Entity(tableName = "printer_settings")
data class PrinterSettingsEntity(
    @PrimaryKey
    val id: Int = DEFAULT_ID,
    val deviceName: String? = null,
    val deviceAddress: String? = null,
    val width: ReceiptWidth = ReceiptWidth.MM_58,
    val autoPrintReceipts: Boolean = true,
    val shopName: String = "CALI'S BLOOMPRINTS",
    val shopSubtitle: String = "AND KEEPSAKES",
    val receiptFooter: String = "Thank you! Please come again.",
    val logoUri: String? = null,
    val printLogoOnReceipts: Boolean = false,
) {
    companion object {
        const val DEFAULT_ID = 1
    }
}
