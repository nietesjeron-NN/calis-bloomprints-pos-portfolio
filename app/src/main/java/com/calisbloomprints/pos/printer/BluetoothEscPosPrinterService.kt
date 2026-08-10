package com.calisbloomprints.pos.printer

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.calisbloomprints.pos.data.db.SaleWithItems
import com.calisbloomprints.pos.data.db.entity.PrinterSettingsEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

class BluetoothEscPosPrinterService(
    private val context: Context,
) {
    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(BluetoothManager::class.java)

    private val bluetoothAdapter
        get() = bluetoothManager?.adapter

    fun hasRequiredPermissions(): Boolean {
        return requiredPermissions().all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun requiredPermissions(): List<String> {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> listOf(
                Manifest.permission.BLUETOOTH_CONNECT,
            )
            else -> emptyList()
        }
    }

    @SuppressLint("MissingPermission")
    fun pairedPrinters(): PrinterResult<List<PrinterDevice>> {
        val adapter = bluetoothAdapter
            ?: return PrinterResult.Error("Bluetooth is not available on this device.")

        if (!hasRequiredPermissions()) return PrinterResult.PermissionRequired
        if (!adapter.isEnabled) return PrinterResult.Error("Bluetooth is turned off.")

        val devices = adapter.bondedDevices
            .map { device ->
                PrinterDevice(
                    name = device.name ?: "Bluetooth printer",
                    address = device.address,
                )
            }
            .sortedBy { it.name.lowercase() }

        return PrinterResult.Success(devices)
    }

    suspend fun printReceipt(
        saleWithItems: SaleWithItems,
        settings: PrinterSettingsEntity,
    ): PrinterResult<Unit> {
        val address = settings.deviceAddress
            ?: return PrinterResult.Error("Choose a printer before printing receipts.")

        val bytes = withContext(Dispatchers.IO) {
            EscPosReceiptBuilder.buildReceipt(context, saleWithItems, settings)
        }
        return printBytes(address, bytes)
    }

    suspend fun printReservationTicket(
        saleWithItems: SaleWithItems,
        settings: PrinterSettingsEntity,
    ): PrinterResult<Unit> {
        val address = settings.deviceAddress
            ?: return PrinterResult.Error("Choose a printer before printing reservation tickets.")

        val bytes = withContext(Dispatchers.IO) {
            EscPosReceiptBuilder.buildReservationTicket(context, saleWithItems, settings)
        }
        return printBytes(address, bytes)
    }

    suspend fun testPrint(settings: PrinterSettingsEntity): PrinterResult<Unit> {
        val address = settings.deviceAddress
            ?: return PrinterResult.Error("Choose a printer before sending a test print.")

        val bytes = withContext(Dispatchers.IO) {
            EscPosReceiptBuilder.buildTestPrint(context, settings)
        }
        return printBytes(address, bytes)
    }

    @SuppressLint("MissingPermission")
    private suspend fun printBytes(address: String, bytes: ByteArray): PrinterResult<Unit> {
        val adapter = bluetoothAdapter
            ?: return PrinterResult.Error("Bluetooth is not available on this device.")

        if (!hasRequiredPermissions()) return PrinterResult.PermissionRequired
        if (!adapter.isEnabled) return PrinterResult.Error("Bluetooth is turned off.")

        return withContext(Dispatchers.IO) {
            runCatching {
                val device = adapter.getRemoteDevice(address)
                val secureSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                val socket = try {
                    secureSocket.connect()
                    secureSocket
                } catch (firstFailure: Throwable) {
                    try {
                        secureSocket.close()
                    } catch (_: IOException) {
                        // The fallback socket can still work even if closing this one fails.
                    }
                    val fallbackSocket = device.createInsecureRfcommSocketToServiceRecord(SPP_UUID)
                    try {
                        fallbackSocket.connect()
                        fallbackSocket
                    } catch (fallbackFailure: Throwable) {
                        fallbackFailure.addSuppressed(firstFailure)
                        throw fallbackFailure
                    }
                }
                try {
                    socket.outputStream.use { output ->
                        var offset = 0
                        while (offset < bytes.size) {
                            val length = minOf(PRINT_CHUNK_SIZE, bytes.size - offset)
                            output.write(bytes, offset, length)
                            output.flush()
                            offset += length
                            Thread.sleep(PRINT_CHUNK_DELAY_MS)
                        }
                        output.flush()
                    }
                } finally {
                    try {
                        socket.close()
                    } catch (_: IOException) {
                        // Closing a Bluetooth socket can fail after the printer disconnects.
                    }
                }
            }.fold(
                onSuccess = { PrinterResult.Success(Unit) },
                onFailure = { throwable ->
                    PrinterResult.Error(
                        message = throwable.message ?: "Could not print to the selected Bluetooth printer.",
                        throwable = throwable,
                    )
                },
            )
        }
    }

    private companion object {
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        const val PRINT_CHUNK_SIZE = 256
        const val PRINT_CHUNK_DELAY_MS = 30L
    }
}
