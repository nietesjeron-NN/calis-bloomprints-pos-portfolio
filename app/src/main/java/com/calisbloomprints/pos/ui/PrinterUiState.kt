package com.calisbloomprints.pos.ui

import com.calisbloomprints.pos.data.db.entity.PrinterSettingsEntity
import com.calisbloomprints.pos.printer.PrinterDevice

data class PrinterUiState(
    val settings: PrinterSettingsEntity = PrinterSettingsEntity(),
    val devices: List<PrinterDevice> = emptyList(),
    val hasPermission: Boolean = false,
    val isBusy: Boolean = false,
    val message: String? = null,
)
