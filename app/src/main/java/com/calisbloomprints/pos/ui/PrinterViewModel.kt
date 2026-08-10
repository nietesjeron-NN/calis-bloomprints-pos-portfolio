package com.calisbloomprints.pos.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.calisbloomprints.pos.data.db.SaleWithItems
import com.calisbloomprints.pos.data.model.ReceiptWidth
import com.calisbloomprints.pos.data.repository.PrinterSettingsRepository
import com.calisbloomprints.pos.printer.BluetoothEscPosPrinterService
import com.calisbloomprints.pos.printer.PrinterDevice
import com.calisbloomprints.pos.printer.PrinterResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PrinterViewModel(
    private val printerSettingsRepository: PrinterSettingsRepository,
    private val printerService: BluetoothEscPosPrinterService,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        PrinterUiState(hasPermission = printerService.hasRequiredPermissions()),
    )
    val uiState: StateFlow<PrinterUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            printerSettingsRepository.settings.collect { settings ->
                _uiState.update {
                    it.copy(
                        settings = settings,
                        hasPermission = printerService.hasRequiredPermissions(),
                    )
                }
            }
        }
        refreshPairedPrinters()
    }

    fun refreshPairedPrinters() {
        when (val result = printerService.pairedPrinters()) {
            is PrinterResult.Success -> {
                _uiState.update {
                    it.copy(
                        devices = result.value,
                        hasPermission = true,
                        message = if (result.value.isEmpty()) {
                            "No paired Bluetooth printers found."
                        } else {
                            "Found ${result.value.size} paired Bluetooth device(s)."
                        },
                    )
                }
            }
            PrinterResult.PermissionRequired -> {
                _uiState.update {
                    it.copy(
                        hasPermission = false,
                        devices = emptyList(),
                        message = "Bluetooth permission is needed to use the printer.",
                    )
                }
            }
            is PrinterResult.Error -> {
                _uiState.update {
                    it.copy(
                        devices = emptyList(),
                        message = result.message,
                    )
                }
            }
        }
    }

    fun selectPrinter(device: PrinterDevice) {
        viewModelScope.launch {
            printerSettingsRepository.saveSelectedPrinter(device.name, device.address)
            _uiState.update { it.copy(message = "${device.name} selected.") }
        }
    }

    fun setReceiptWidth(width: ReceiptWidth) {
        viewModelScope.launch {
            printerSettingsRepository.saveWidth(width)
            _uiState.update { it.copy(message = "${width.label} receipt width saved.") }
        }
    }

    fun setPrintLogoOnReceipts(enabled: Boolean) {
        viewModelScope.launch {
            printerSettingsRepository.savePrintLogoOnReceipts(enabled)
            _uiState.update {
                it.copy(
                    message = if (enabled) {
                        "Receipt logo enabled."
                    } else {
                        "Receipt logo disabled."
                    },
                )
            }
        }
    }

    fun saveReceiptBranding(shopName: String, shopSubtitle: String, receiptFooter: String) {
        viewModelScope.launch {
            printerSettingsRepository.saveReceiptBranding(shopName, shopSubtitle, receiptFooter)
            _uiState.update { it.copy(message = "Receipt text saved.") }
        }
    }

    fun saveLogoUri(logoUri: String?) {
        viewModelScope.launch {
            printerSettingsRepository.saveLogoUri(logoUri)
            _uiState.update {
                it.copy(message = if (logoUri == null) "Logo removed." else "Logo saved.")
            }
        }
    }

    fun testPrint() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, message = null) }
            val settings = printerSettingsRepository.getSettings()
            when (val result = printerService.testPrint(settings)) {
                is PrinterResult.Success -> _uiState.update {
                    it.copy(isBusy = false, message = "Test print sent.")
                }
                PrinterResult.PermissionRequired -> _uiState.update {
                    it.copy(
                        isBusy = false,
                        hasPermission = false,
                        message = "Bluetooth permission is needed before printing.",
                    )
                }
                is PrinterResult.Error -> _uiState.update {
                    it.copy(isBusy = false, message = result.message)
                }
            }
        }
    }

    fun printSale(saleWithItems: SaleWithItems) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, message = null) }
            val settings = printerSettingsRepository.getSettings()
            when (val result = printerService.printReceipt(saleWithItems, settings)) {
                is PrinterResult.Success -> _uiState.update {
                    it.copy(isBusy = false, message = "Receipt sent to printer.")
                }
                PrinterResult.PermissionRequired -> _uiState.update {
                    it.copy(
                        isBusy = false,
                        hasPermission = false,
                        message = "Bluetooth permission is needed before printing.",
                    )
                }
                is PrinterResult.Error -> _uiState.update {
                    it.copy(isBusy = false, message = result.message)
                }
            }
        }
    }

    fun printReservationTicket(saleWithItems: SaleWithItems) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, message = null) }
            val settings = printerSettingsRepository.getSettings()
            when (val result = printerService.printReservationTicket(saleWithItems, settings)) {
                is PrinterResult.Success -> _uiState.update {
                    it.copy(isBusy = false, message = "Reservation ticket sent to printer.")
                }
                PrinterResult.PermissionRequired -> _uiState.update {
                    it.copy(
                        isBusy = false,
                        hasPermission = false,
                        message = "Bluetooth permission is needed before printing.",
                    )
                }
                is PrinterResult.Error -> _uiState.update {
                    it.copy(isBusy = false, message = result.message)
                }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    class Factory(
        private val printerSettingsRepository: PrinterSettingsRepository,
        private val printerService: BluetoothEscPosPrinterService,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PrinterViewModel(printerSettingsRepository, printerService) as T
        }
    }
}
