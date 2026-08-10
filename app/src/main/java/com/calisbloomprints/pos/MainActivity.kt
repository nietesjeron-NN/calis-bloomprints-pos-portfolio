package com.calisbloomprints.pos

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import com.calisbloomprints.pos.barcode.BarcodeSheetPrintAdapter
import com.calisbloomprints.pos.data.db.SaleWithItems
import com.calisbloomprints.pos.data.db.entity.ProductEntity
import com.calisbloomprints.pos.report.SalesReportBuilder
import com.calisbloomprints.pos.report.SalesReportExporter
import com.calisbloomprints.pos.ui.PosViewModel
import com.calisbloomprints.pos.ui.PrinterViewModel
import com.calisbloomprints.pos.ui.screen.CaliBloomprintsPosRoot
import com.calisbloomprints.pos.ui.theme.CalisBloomprintsTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private lateinit var printerViewModel: PrinterViewModel
    private var pendingReportExport: ByteArray? = null

    private val logoPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null && ::printerViewModel.isInitialized) {
            runCatching {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            printerViewModel.saveLogoUri(uri.toString())
        }
    }

    private val printerPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        if (::printerViewModel.isInitialized) {
            printerViewModel.refreshPairedPrinters()
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        // Reminder alarms are already scheduled; permission controls whether Android shows them.
    }

    private val pdfReportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf"),
    ) { uri ->
        savePendingReport(uri)
    }

    private val excelReportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        ),
    ) { uri ->
        savePendingReport(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val container = (application as BloomprintsPosApp).container
        val posViewModel = ViewModelProvider(
            this,
            PosViewModel.Factory(
                container.productRepository,
                container.salesRepository,
                container.pickupReminderScheduler,
            ),
        )[PosViewModel::class.java]

        printerViewModel = ViewModelProvider(
            this,
            PrinterViewModel.Factory(container.printerSettingsRepository, container.printerService),
        )[PrinterViewModel::class.java]

        setContent {
            CalisBloomprintsTheme {
                CaliBloomprintsPosRoot(
                    posViewModel = posViewModel,
                    printerViewModel = printerViewModel,
                    onRequestPrinterPermission = {
                        val permissions = container.printerService.requiredPermissions().toTypedArray()
                        if (permissions.isEmpty()) {
                            printerViewModel.refreshPairedPrinters()
                        } else {
                            printerPermissionLauncher.launch(permissions)
                        }
                    },
                    onOpenBluetoothSettings = {
                        startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                    },
                    onPickLogo = {
                        logoPickerLauncher.launch(arrayOf("image/*"))
                    },
                    onRequestPickupNotificationPermission = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    onPrintBarcodeSheet = { title, products, repeatSingleProduct ->
                        printBarcodeSheet(title, products, repeatSingleProduct)
                    },
                    onExportSalesReport = { format, sales ->
                        exportSalesReport(format, sales)
                    },
                )
            }
        }
    }

    private fun printBarcodeSheet(
        title: String,
        products: List<ProductEntity>,
        repeatSingleProduct: Boolean,
    ) {
        if (products.isEmpty()) return

        val printManager = getSystemService(PrintManager::class.java)
        val attributes = PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
            .build()

        printManager.print(
            title,
            BarcodeSheetPrintAdapter(
                context = this,
                jobName = title,
                products = products,
                repeatSingleProduct = repeatSingleProduct,
            ),
            attributes,
        )
    }

    private fun exportSalesReport(format: String, sales: List<SaleWithItems>) {
        val report = SalesReportBuilder.build(sales)
        val stamp = SimpleDateFormat("yyyy-MM-dd-HHmm", Locale.US).format(Date(report.generatedAt))

        when (format) {
            "pdf" -> {
                pendingReportExport = SalesReportExporter.buildPdf(report)
                pdfReportLauncher.launch("calis-sales-report-$stamp.pdf")
            }
            "xlsx" -> {
                pendingReportExport = SalesReportExporter.buildXlsx(report)
                excelReportLauncher.launch("calis-sales-report-$stamp.xlsx")
            }
        }
    }

    private fun savePendingReport(uri: android.net.Uri?) {
        val bytes = pendingReportExport ?: return
        pendingReportExport = null
        if (uri == null) return

        runCatching {
            contentResolver.openOutputStream(uri)?.use { output ->
                output.write(bytes)
            } ?: error("Could not open the selected file.")
        }.fold(
            onSuccess = {
                Toast.makeText(this, "Report exported.", Toast.LENGTH_SHORT).show()
            },
            onFailure = { throwable ->
                Toast.makeText(this, throwable.message ?: "Could not export report.", Toast.LENGTH_LONG).show()
            },
        )
    }
}
