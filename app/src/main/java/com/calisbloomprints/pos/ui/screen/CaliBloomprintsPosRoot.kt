package com.calisbloomprints.pos.ui.screen

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items as listItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calisbloomprints.pos.data.db.SaleWithItems
import com.calisbloomprints.pos.data.db.entity.ProductEntity
import com.calisbloomprints.pos.data.db.entity.StockMovementEntity
import com.calisbloomprints.pos.data.model.CartLine
import com.calisbloomprints.pos.data.model.DailySalesSummary
import com.calisbloomprints.pos.data.model.MoneyFormatter
import com.calisbloomprints.pos.data.model.ProductCategory
import com.calisbloomprints.pos.data.model.ReceiptWidth
import com.calisbloomprints.pos.printer.PrinterDevice
import com.calisbloomprints.pos.R
import com.calisbloomprints.pos.barcode.displayBarcode
import com.calisbloomprints.pos.report.SalesReportBuilder
import com.calisbloomprints.pos.report.SalesReportPeriod
import com.calisbloomprints.pos.ui.PosUiState
import com.calisbloomprints.pos.ui.PosViewModel
import com.calisbloomprints.pos.ui.PrinterUiState
import com.calisbloomprints.pos.ui.PrinterViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private enum class AppTab(val label: String) {
    SELL("Sell"),
    CART("Cart"),
    CALENDAR("Calendar"),
    BARCODES("Barcodes"),
    REPORTS("Reports"),
    SALES("Sales"),
    INVENTORY("Inventory"),
    PRINTER("Printer"),
    SETTINGS("Settings"),
}

private val BloomBlush = Color(0xFFFFEEF4)
private val BlossomPink = Color(0xFFFFCAD8)
private val PetalPink = Color(0xFFFFAFC7)
private val LeafMist = Color(0xFFEFF8EA)
private val GiftLilac = Color(0xFFF4ECFF)
private val WarmCream = Color(0xFFFFFCF6)
private const val MAX_BARCODE_LABELS = 999

private data class BloomUiColors(
    val blush: Color,
    val blossomPink: Color,
    val petalPink: Color,
    val leafMist: Color,
    val giftLilac: Color,
    val warmCream: Color,
    val giftCream: Color,
)

@Composable
private fun bloomUiColors(): BloomUiColors {
    return if (isSystemInDarkTheme()) {
        BloomUiColors(
            blush = Color(0xFF3A202A),
            blossomPink = Color(0xFF744056),
            petalPink = Color(0xFFD987A4),
            leafMist = Color(0xFF203325),
            giftLilac = Color(0xFF31243F),
            warmCream = Color(0xFF2A2225),
            giftCream = Color(0xFF3A2A1F),
        )
    } else {
        BloomUiColors(
            blush = BloomBlush,
            blossomPink = BlossomPink,
            petalPink = PetalPink,
            leafMist = LeafMist,
            giftLilac = GiftLilac,
            warmCream = WarmCream,
            giftCream = Color(0xFFFFF1DE),
        )
    }
}

@Composable
fun CaliBloomprintsPosRoot(
    posViewModel: PosViewModel,
    printerViewModel: PrinterViewModel,
    onRequestPrinterPermission: () -> Unit,
    onOpenBluetoothSettings: () -> Unit,
    onPickLogo: () -> Unit,
    onRequestPickupNotificationPermission: () -> Unit,
    onPrintBarcodeSheet: (String, List<ProductEntity>, Boolean) -> Unit,
    onExportSalesReport: (String, List<SaleWithItems>) -> Unit,
) {
    val posState by posViewModel.uiState.collectAsStateWithLifecycle()
    val printerState by printerViewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableStateOf(AppTab.SELL) }
    val configuration = LocalConfiguration.current
    val useWideLayout = configuration.screenWidthDp >= 720 ||
        (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE && configuration.screenWidthDp >= 700)

    LaunchedEffect(posState.pendingPrintSale?.sale?.id) {
        val sale = posState.pendingPrintSale
        if (sale != null) {
            printerViewModel.printSale(sale)
            posViewModel.consumePrintRequest()
        }
    }

    LaunchedEffect(posState.needsNotificationPermission) {
        if (posState.needsNotificationPermission) {
            onRequestPickupNotificationPermission()
            posViewModel.consumeNotificationPermissionRequest()
        }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding),
        ) {
            Header(posState, printerState)
            MessageStrip(
                message = posState.message ?: printerState.message,
                onDismiss = {
                    posViewModel.clearMessage()
                    printerViewModel.clearMessage()
                },
            )
            PrimaryScrollableTabRow(selectedTabIndex = selectedTab.ordinal) {
                AppTab.entries.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = { Text(tab.label) },
                    )
                }
            }
            if (useWideLayout && (selectedTab == AppTab.SELL || selectedTab == AppTab.CART)) {
                TabletSellCartScreen(
                    state = posState,
                    onSelectCategory = posViewModel::selectCategory,
                    onAddProduct = posViewModel::addProduct,
                    onQuantityChange = posViewModel::changeQuantity,
                    onCustomerNameChange = posViewModel::updateCustomerName,
                    onCustomerContactChange = posViewModel::updateCustomerContact,
                    onNotesChange = posViewModel::updateOrderNotes,
                    onPickupDateChange = posViewModel::updatePickupDate,
                    onPickupTimeChange = posViewModel::updatePickupTime,
                    onPickupReminderMinutesChange = posViewModel::updatePickupReminderMinutes,
                    onDiscountChange = posViewModel::updateDiscount,
                    onServiceFeeChange = posViewModel::updateServiceFee,
                    onDepositChange = posViewModel::updateDeposit,
                    onCashReceivedChange = posViewModel::updateCashReceived,
                    onLinePriceChange = posViewModel::updateLinePrice,
                    onClearCart = posViewModel::clearCart,
                    onCheckout = posViewModel::checkout,
                )
            } else {
                when (selectedTab) {
                    AppTab.SELL -> SellScreen(
                        state = posState,
                        onSelectCategory = posViewModel::selectCategory,
                        onAddProduct = posViewModel::addProduct,
                    )
                    AppTab.CART -> CartScreen(
                        state = posState,
                        onQuantityChange = posViewModel::changeQuantity,
                        onCustomerNameChange = posViewModel::updateCustomerName,
                        onCustomerContactChange = posViewModel::updateCustomerContact,
                        onNotesChange = posViewModel::updateOrderNotes,
                        onPickupDateChange = posViewModel::updatePickupDate,
                        onPickupTimeChange = posViewModel::updatePickupTime,
                        onPickupReminderMinutesChange = posViewModel::updatePickupReminderMinutes,
                        onDiscountChange = posViewModel::updateDiscount,
                        onServiceFeeChange = posViewModel::updateServiceFee,
                        onDepositChange = posViewModel::updateDeposit,
                        onCashReceivedChange = posViewModel::updateCashReceived,
                        onLinePriceChange = posViewModel::updateLinePrice,
                        onClearCart = posViewModel::clearCart,
                        onCheckout = posViewModel::checkout,
                    )
                    AppTab.CALENDAR -> PickupCalendarScreen(
                        orders = posState.pickupOrders,
                        onReprint = printerViewModel::printSale,
                        onPrintReservationTicket = printerViewModel::printReservationTicket,
                    )
                    AppTab.BARCODES -> BarcodesScreen(
                        products = posState.inventoryProducts.filter { it.active },
                        onPrintQuantities = { barcodeProducts ->
                            onPrintBarcodeSheet(
                                "Cali's Bloomprints barcode sheet",
                                barcodeProducts,
                                false,
                            )
                        },
                        onPrintOneFullPage = { product ->
                            onPrintBarcodeSheet(
                                "${product.name} barcodes",
                                listOf(product),
                                true,
                            )
                        },
                    )
                    AppTab.REPORTS -> ReportsScreen(
                        sales = posState.allSales,
                        onExportPdf = { onExportSalesReport("pdf", posState.allSales) },
                        onExportExcel = { onExportSalesReport("xlsx", posState.allSales) },
                    )
                    AppTab.SALES -> SalesScreen(
                        sales = posState.recentSales,
                        summary = posState.todaySummary,
                        onReprint = printerViewModel::printSale,
                    )
                    AppTab.INVENTORY -> InventoryScreen(
                        products = posState.inventoryProducts,
                        stockMovements = posState.stockMovements,
                        onAdjustStock = posViewModel::adjustInventory,
                        onSetActive = posViewModel::setProductActive,
                        onSaveProduct = posViewModel::saveProduct,
                        onDeleteProduct = posViewModel::deleteProduct,
                    )
                    AppTab.PRINTER -> PrinterScreen(
                        state = printerState,
                        onRequestPermission = onRequestPrinterPermission,
                        onOpenBluetoothSettings = onOpenBluetoothSettings,
                    onRefresh = printerViewModel::refreshPairedPrinters,
                    onSelectPrinter = printerViewModel::selectPrinter,
                    onWidthChange = printerViewModel::setReceiptWidth,
                    onPrintLogoOnReceiptsChange = printerViewModel::setPrintLogoOnReceipts,
                    onSaveReceiptBranding = printerViewModel::saveReceiptBranding,
                    onTestPrint = printerViewModel::testPrint,
                )
                    AppTab.SETTINGS -> SettingsScreen(
                        state = printerState,
                        onPickLogo = onPickLogo,
                        onRemoveLogo = { printerViewModel.saveLogoUri(null) },
                        onSaveReceiptBranding = printerViewModel::saveReceiptBranding,
                    )
                }
            }
        }
    }
}

@Composable
private fun Header(posState: PosUiState, printerState: PrinterUiState) {
    val bloom = bloomUiColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    listOf(bloom.blush, bloom.warmCream, bloom.leafMist),
                ),
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LogoMark(logoUri = printerState.settings.logoUri, modifier = Modifier.size(58.dp))
            Column {
                Text(
                    text = shopNameLabel(printerState.settings.shopName),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = printerState.settings.shopSubtitle.ifBlank { "Keepsakes POS" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Column(
            modifier = Modifier
                .padding(start = 10.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.78f))
                .border(BorderStroke(1.dp, bloom.blossomPink), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.End,
        ) {
            Text(
                text = MoneyFormatter.format(posState.cartTotalCents),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = printerState.settings.deviceName ?: "No printer",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun LogoMark(logoUri: String?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bloom = bloomUiColors()
    val bitmap = remember(logoUri) {
        logoUri?.let { rawUri ->
            runCatching {
                context.contentResolver
                    .openInputStream(Uri.parse(rawUri))
                    ?.use(BitmapFactory::decodeStream)
            }.getOrNull()
        }
    }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    listOf(MaterialTheme.colorScheme.surface, bloom.blossomPink, bloom.giftLilac),
                ),
            )
            .border(BorderStroke(2.dp, MaterialTheme.colorScheme.surface), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Shop logo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Image(
                painter = painterResource(R.drawable.calis_bloomprints_logo),
                contentDescription = "Shop logo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Composable
private fun FallbackBloomLogo() {
    val bloom = bloomUiColors()
    Box(
        modifier = Modifier.size(42.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(17.dp)
                .offset(y = (-10).dp)
                .clip(CircleShape)
                .background(bloom.petalPink),
        )
        Box(
            modifier = Modifier
                .size(17.dp)
                .offset(x = 10.dp)
                .clip(CircleShape)
                .background(bloom.blossomPink),
        )
        Box(
            modifier = Modifier
                .size(17.dp)
                .offset(y = 10.dp)
                .clip(CircleShape)
                .background(if (isSystemInDarkTheme()) Color(0xFFC76C8C) else Color(0xFFE899B4)),
        )
        Box(
            modifier = Modifier
                .size(17.dp)
                .offset(x = (-10).dp)
                .clip(CircleShape)
                .background(if (isSystemInDarkTheme()) Color(0xFF9C5269) else Color(0xFFFFD7E2)),
        )
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondary),
        )
    }
}

@Composable
private fun MessageStrip(message: String?, onDismiss: () -> Unit) {
    if (message == null) return
    val bloom = bloomUiColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    listOf(MaterialTheme.colorScheme.primaryContainer, bloom.leafMist),
                ),
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = message,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
        )
        TextButton(onClick = onDismiss) {
            Text("OK")
        }
    }
}

@Composable
private fun TabletSellCartScreen(
    state: PosUiState,
    onSelectCategory: (ProductCategory?) -> Unit,
    onAddProduct: (ProductEntity) -> Unit,
    onQuantityChange: (Long, Int) -> Unit,
    onCustomerNameChange: (String) -> Unit,
    onCustomerContactChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onPickupDateChange: (String) -> Unit,
    onPickupTimeChange: (String) -> Unit,
    onPickupReminderMinutesChange: (Int) -> Unit,
    onDiscountChange: (String) -> Unit,
    onServiceFeeChange: (String) -> Unit,
    onDepositChange: (String) -> Unit,
    onCashReceivedChange: (String) -> Unit,
    onLinePriceChange: (Long, String) -> Unit,
    onClearCart: () -> Unit,
    onCheckout: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(
            modifier = Modifier
                .weight(0.62f)
                .fillMaxSize(),
        ) {
            Text(
                text = "Products",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            )
            Box(modifier = Modifier.weight(1f)) {
                SellScreen(
                    state = state,
                    onSelectCategory = onSelectCategory,
                    onAddProduct = onAddProduct,
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(0.38f)
                .fillMaxSize(),
        ) {
            Text(
                text = "Cart",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            )
            Box(modifier = Modifier.weight(1f)) {
                CartScreen(
                    state = state,
                    onQuantityChange = onQuantityChange,
                    onCustomerNameChange = onCustomerNameChange,
                    onCustomerContactChange = onCustomerContactChange,
                    onNotesChange = onNotesChange,
                    onPickupDateChange = onPickupDateChange,
                    onPickupTimeChange = onPickupTimeChange,
                    onPickupReminderMinutesChange = onPickupReminderMinutesChange,
                    onDiscountChange = onDiscountChange,
                    onServiceFeeChange = onServiceFeeChange,
                    onDepositChange = onDepositChange,
                    onCashReceivedChange = onCashReceivedChange,
                    onLinePriceChange = onLinePriceChange,
                    onClearCart = onClearCart,
                    onCheckout = onCheckout,
                )
            }
        }
    }
}

@Composable
private fun SellScreen(
    state: PosUiState,
    onSelectCategory: (ProductCategory?) -> Unit,
    onAddProduct: (ProductEntity) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        CategoryRow(
            selectedCategory = state.selectedCategory,
            onSelectCategory = onSelectCategory,
        )
        if (state.visibleProducts.isEmpty()) {
            EmptyState("No products in this category.")
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                gridItems(state.visibleProducts, key = { it.id }) { product ->
                    ProductTile(product = product, onAddProduct = { onAddProduct(product) })
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(
    selectedCategory: ProductCategory?,
    onSelectCategory: (ProductCategory?) -> Unit,
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { onSelectCategory(null) },
                label = { Text("All") },
            )
        }
        listItems(ProductCategory.entries) { category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onSelectCategory(category) },
                label = { Text(category.label) },
            )
        }
    }
}

@Composable
private fun ProductTile(product: ProductEntity, onAddProduct: () -> Unit) {
    ElevatedCard(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = categorySurfaceColor(product.category)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = product.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(categoryAccentColor(product.category)),
                )
                Text(
                    text = product.category.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = MoneyFormatter.format(product.priceCents),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stockLabel(product),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = onAddProduct,
                enabled = !product.trackStock || (product.stockQuantity ?: 0) > 0,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Add")
            }
        }
    }
}

@Composable
private fun CartScreen(
    state: PosUiState,
    onQuantityChange: (Long, Int) -> Unit,
    onCustomerNameChange: (String) -> Unit,
    onCustomerContactChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onPickupDateChange: (String) -> Unit,
    onPickupTimeChange: (String) -> Unit,
    onPickupReminderMinutesChange: (Int) -> Unit,
    onDiscountChange: (String) -> Unit,
    onServiceFeeChange: (String) -> Unit,
    onDepositChange: (String) -> Unit,
    onCashReceivedChange: (String) -> Unit,
    onLinePriceChange: (Long, String) -> Unit,
    onClearCart: () -> Unit,
    onCheckout: (Boolean) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (state.cartLines.isEmpty()) {
            item { EmptyState("Cart is empty.") }
        } else {
            listItems(state.cartLines, key = { it.product.id }) { line ->
                CartLineRow(
                    line = line,
                    onQuantityChange = onQuantityChange,
                    onLinePriceChange = onLinePriceChange,
                )
            }
        }

        item {
            HorizontalDivider()
            Spacer(Modifier.height(10.dp))
            CustomerFields(
                state = state,
                onCustomerNameChange = onCustomerNameChange,
                onCustomerContactChange = onCustomerContactChange,
                onNotesChange = onNotesChange,
                onPickupDateChange = onPickupDateChange,
                onPickupTimeChange = onPickupTimeChange,
                onPickupReminderMinutesChange = onPickupReminderMinutesChange,
                onDiscountChange = onDiscountChange,
                onServiceFeeChange = onServiceFeeChange,
                onDepositChange = onDepositChange,
                onCashReceivedChange = onCashReceivedChange,
            )
        }

        item {
            TotalsBlock(state)
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onClearCart,
                    enabled = state.cartLines.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Clear")
                }
                Button(
                    onClick = { onCheckout(false) },
                    enabled = state.canCheckout,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Save")
                }
                Button(
                    onClick = { onCheckout(true) },
                    enabled = state.canCheckout,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Print")
                }
            }
        }
    }
}

@Composable
private fun CartLineRow(
    line: CartLine,
    onQuantityChange: (Long, Int) -> Unit,
    onLinePriceChange: (Long, String) -> Unit,
) {
    var priceText by remember(line.product.id, line.unitPriceCents) {
        mutableStateOf(String.format(Locale.US, "%.2f", line.unitPriceCents / 100.0))
    }

    ElevatedCard(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = bloomUiColors().warmCream),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(line.product.name, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${line.quantity} x ${MoneyFormatter.format(line.unitPriceCents)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(MoneyFormatter.format(line.lineTotalCents), fontWeight = FontWeight.Bold)
                }
                QuantityButton("-", onClick = { onQuantityChange(line.product.id, -1) })
                Box(
                    modifier = Modifier.size(width = 36.dp, height = 36.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(line.quantity.toString(), fontWeight = FontWeight.Bold)
                }
                QuantityButton("+", onClick = { onQuantityChange(line.product.id, 1) })
            }
            if (line.product.category == ProductCategory.CUSTOM) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Custom price") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                    )
                    OutlinedButton(onClick = { onLinePriceChange(line.product.id, priceText) }) {
                        Text("Set")
                    }
                }
            }
        }
    }
}

@Composable
private fun QuantityButton(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.size(44.dp),
        contentPadding = PaddingValues(0.dp),
    ) {
        Text(label)
    }
}

@Composable
private fun CustomerFields(
    state: PosUiState,
    onCustomerNameChange: (String) -> Unit,
    onCustomerContactChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onPickupDateChange: (String) -> Unit,
    onPickupTimeChange: (String) -> Unit,
    onPickupReminderMinutesChange: (Int) -> Unit,
    onDiscountChange: (String) -> Unit,
    onServiceFeeChange: (String) -> Unit,
    onDepositChange: (String) -> Unit,
    onCashReceivedChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = state.customerName,
            onValueChange = onCustomerNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Customer name") },
            singleLine = true,
        )
        OutlinedTextField(
            value = state.customerContact,
            onValueChange = onCustomerContactChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Contact") },
            singleLine = true,
        )
        OutlinedTextField(
            value = state.orderNotes,
            onValueChange = onNotesChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Order notes") },
            minLines = 2,
        )
        PickupFields(
            state = state,
            onPickupDateChange = onPickupDateChange,
            onPickupTimeChange = onPickupTimeChange,
            onPickupReminderMinutesChange = onPickupReminderMinutesChange,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = state.discount,
                onValueChange = onDiscountChange,
                modifier = Modifier.weight(1f),
                label = { Text("Discount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
            )
            OutlinedTextField(
                value = state.serviceFee,
                onValueChange = onServiceFeeChange,
                modifier = Modifier.weight(1f),
                label = { Text("Service fee") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
            )
        }
        OutlinedTextField(
            value = state.deposit,
            onValueChange = onDepositChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Deposit / partial payment") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
        )
        OutlinedTextField(
            value = state.cashReceived,
            onValueChange = onCashReceivedChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Cash received") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
        )
    }
}

@Composable
private fun PickupFields(
    state: PosUiState,
    onPickupDateChange: (String) -> Unit,
    onPickupTimeChange: (String) -> Unit,
    onPickupReminderMinutesChange: (Int) -> Unit,
) {
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Pickup reminder", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = {
                    val calendar = calendarFromDateInput(state.pickupDate) ?: Calendar.getInstance()
                    DatePickerDialog(
                        context,
                        { _, year, month, day ->
                            val selected = Calendar.getInstance().apply {
                                set(Calendar.YEAR, year)
                                set(Calendar.MONTH, month)
                                set(Calendar.DAY_OF_MONTH, day)
                            }
                            onPickupDateChange(formatPickupDateInput(selected))
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH),
                    ).show()
                },
                modifier = Modifier.weight(1f),
            ) {
                Text("Pick Date")
            }
            OutlinedButton(
                onClick = {
                    val calendar = calendarFromTimeInput(state.pickupTime) ?: Calendar.getInstance()
                    TimePickerDialog(
                        context,
                        { _, hour, minute ->
                            val selected = Calendar.getInstance().apply {
                                set(Calendar.HOUR_OF_DAY, hour)
                                set(Calendar.MINUTE, minute)
                            }
                            onPickupTimeChange(formatPickupTimeInput(selected))
                        },
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE),
                        false,
                    ).show()
                },
                modifier = Modifier.weight(1f),
            ) {
                Text("Pick Time")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = state.pickupDate,
                onValueChange = onPickupDateChange,
                modifier = Modifier.weight(1f),
                label = { Text("Pickup date") },
                placeholder = { Text("08/06/2026") },
                singleLine = true,
            )
            OutlinedTextField(
                value = state.pickupTime,
                onValueChange = onPickupTimeChange,
                modifier = Modifier.weight(1f),
                label = { Text("Pickup time") },
                placeholder = { Text("3:30 PM") },
                singleLine = true,
            )
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listItems(
                listOf(
                    30 to "30 min",
                    60 to "1 hr",
                    120 to "2 hr",
                    1440 to "1 day",
                ),
            ) { option ->
                FilterChip(
                    selected = state.pickupReminderMinutes == option.first,
                    onClick = { onPickupReminderMinutesChange(option.first) },
                    label = { Text(option.second) },
                )
            }
        }
    }
}

@Composable
private fun TotalsBlock(state: PosUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TotalLine("Subtotal", state.cartTotalCents)
        if (state.discountCents > 0L) {
            TotalLine("Discount", -state.discountCents)
        }
        if (state.serviceFeeCents > 0L) {
            TotalLine("Service fee", state.serviceFeeCents)
        }
        TotalLine("Order total", state.orderTotalCents, strong = true)
        TotalLine("Due now", state.amountDueNowCents, strong = state.depositCents > 0L)
        if (state.balanceDueCents > 0L) {
            TotalLine("Balance later", state.balanceDueCents)
        }
        TotalLine("Cash", state.amountTenderedCents ?: 0L)
        TotalLine("Change", state.changeCents, strong = true)
    }
}

@Composable
private fun TotalLine(label: String, cents: Long, strong: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, fontWeight = if (strong) FontWeight.Bold else FontWeight.Normal)
        Text(MoneyFormatter.format(cents), fontWeight = if (strong) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun PickupCalendarScreen(
    orders: List<SaleWithItems>,
    onReprint: (SaleWithItems) -> Unit,
    onPrintReservationTicket: (SaleWithItems) -> Unit,
) {
    val now = remember { System.currentTimeMillis() }
    val pickupOrders = orders.filter { it.sale.pickupAt != null }
    val dueSoon = pickupOrders.filter { (it.sale.pickupAt ?: 0L) < now }
    val upcoming = pickupOrders.filter { (it.sale.pickupAt ?: 0L) >= now }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(
                text = "Pickup Calendar",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }

        if (pickupOrders.isEmpty()) {
            item { EmptyState("No pickup orders yet.") }
        }

        if (dueSoon.isNotEmpty()) {
            item {
                Text(
                    "Needs attention",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
            listItems(dueSoon, key = { "due-${it.sale.id}" }) { order ->
                PickupOrderCard(
                    order = order,
                    onReprint = { onReprint(order) },
                    onPrintReservationTicket = { onPrintReservationTicket(order) },
                )
            }
        }

        if (upcoming.isNotEmpty()) {
            item {
                Text("Upcoming", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            listItems(upcoming, key = { "pickup-${it.sale.id}" }) { order ->
                PickupOrderCard(
                    order = order,
                    onReprint = { onReprint(order) },
                    onPrintReservationTicket = { onPrintReservationTicket(order) },
                )
            }
        }
    }
}

@Composable
private fun PickupOrderCard(
    order: SaleWithItems,
    onReprint: () -> Unit,
    onPrintReservationTicket: () -> Unit,
) {
    ElevatedCard(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = bloomUiColors().warmCream),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        order.sale.customerName ?: "Walk-in customer",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    order.sale.customerContact?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        order.sale.pickupAt?.let(::pickupDateLabel).orEmpty(),
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        order.sale.receiptNumber,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                order.items.joinToString { "${it.quantity} ${it.productName}" },
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (order.sale.balanceDueCents > 0L) {
                Text(
                    "Balance due: ${MoneyFormatter.format(order.sale.balanceDueCents)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onReprint, modifier = Modifier.weight(1f)) {
                    Text("Receipt")
                }
                Button(onClick = onPrintReservationTicket, modifier = Modifier.weight(1f)) {
                    Text("Ticket")
                }
            }
        }
    }
}

@Composable
private fun BarcodesScreen(
    products: List<ProductEntity>,
    onPrintQuantities: (List<ProductEntity>) -> Unit,
    onPrintOneFullPage: (ProductEntity) -> Unit,
) {
    val quantities = remember(products.map { it.id }) {
        mutableStateMapOf<Long, String>().apply {
            products.forEach { product -> this[product.id] = "0" }
        }
    }
    val selectedProducts = products.flatMap { product ->
        val count = quantities[product.id].orEmpty().toIntOrNull()?.coerceIn(0, MAX_BARCODE_LABELS) ?: 0
        List(count) { product }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(
                "A4 Barcode Sheets",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { onPrintQuantities(selectedProducts) },
                    enabled = selectedProducts.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Print Selected")
                }
                OutlinedButton(
                    onClick = {
                        products.forEach { product -> quantities[product.id] = "0" }
                    },
                    enabled = selectedProducts.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Clear")
                }
            }
        }
        item {
            Text(
                "${selectedProducts.size} barcode label(s) selected",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { products.forEach { product -> quantities[product.id] = "1" } },
                    enabled = products.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("1 Each")
                }
                OutlinedButton(
                    onClick = { products.forEach { product -> quantities[product.id] = stockPrintCount(product).toString() } },
                    enabled = products.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Use Stock")
                }
            }
        }

        if (products.isEmpty()) {
            item { EmptyState("No active products to print.") }
        } else {
            listItems(products, key = { "barcode-${it.id}" }) { product ->
                BarcodeProductRow(
                    product = product,
                    quantity = quantities[product.id].orEmpty(),
                    onQuantityChange = { value ->
                        quantities[product.id] = value.filter(Char::isDigit).take(3)
                    },
                    onQuantityStep = { delta ->
                        val current = quantities[product.id].orEmpty().toIntOrNull() ?: 0
                        quantities[product.id] = (current + delta).coerceIn(0, MAX_BARCODE_LABELS).toString()
                    },
                    onPrintFullPage = { onPrintOneFullPage(product) },
                )
            }
        }
    }
}

@Composable
private fun BarcodeProductRow(
    product: ProductEntity,
    quantity: String,
    onQuantityChange: (String) -> Unit,
    onQuantityStep: (Int) -> Unit,
    onPrintFullPage: () -> Unit,
) {
    ElevatedCard(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = categorySurfaceColor(product.category)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(product.name, fontWeight = FontWeight.SemiBold)
                    Text(
                        product.displayBarcode(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(MoneyFormatter.format(product.priceCents), fontWeight = FontWeight.Bold)
                }
                Button(onClick = onPrintFullPage) {
                    Text("Full A4")
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                QuantityButton("-", onClick = { onQuantityStep(-1) })
                OutlinedTextField(
                    value = quantity,
                    onValueChange = onQuantityChange,
                    modifier = Modifier.size(width = 88.dp, height = 58.dp),
                    label = { Text("Qty") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
                QuantityButton("+", onClick = { onQuantityStep(1) })
                Text(
                    "label(s) for this product",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ReportsScreen(
    sales: List<SaleWithItems>,
    onExportPdf: () -> Unit,
    onExportExcel: () -> Unit,
) {
    val report = remember(sales) { SalesReportBuilder.build(sales) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(
                "Sales Reports",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onExportExcel,
                    enabled = sales.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Export Excel")
                }
                OutlinedButton(
                    onClick = onExportPdf,
                    enabled = sales.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Export PDF")
                }
            }
        }

        if (sales.isEmpty()) {
            item { EmptyState("No sales to report yet.") }
        } else {
            report.periods.forEach { period ->
                item(key = "report-${period.label}") {
                    ReportPeriodCard(period)
                }
            }
        }
    }
}

@Composable
private fun ReportPeriodCard(period: SalesReportPeriod) {
    ElevatedCard(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = bloomUiColors().warmCream),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(period.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Sales")
                Text(period.summary.saleCount.toString())
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Items sold")
                Text(period.itemCount.toString())
            }
            TotalLine("Gross", period.summary.grossSalesCents)
            TotalLine("Discounts", -period.summary.discountCents)
            TotalLine("Service fees", period.summary.serviceFeeCents)
            TotalLine("Net sales", period.summary.netSalesCents, strong = true)
            TotalLine("Cash received", period.summary.cashReceivedCents)
            TotalLine("Change given", period.summary.changeGivenCents)
            if (period.summary.balanceDueCents > 0L) {
                TotalLine("Balance due", period.summary.balanceDueCents, strong = true)
            }
        }
    }
}

@Composable
private fun SalesScreen(
    sales: List<SaleWithItems>,
    summary: DailySalesSummary,
    onReprint: (SaleWithItems) -> Unit,
) {
    if (sales.isEmpty()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { DailySummaryCard(summary) }
            item { EmptyState("No sales yet.") }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { DailySummaryCard(summary) }
        listItems(sales, key = { it.sale.id }) { sale ->
            SaleCard(saleWithItems = sale, onReprint = { onReprint(sale) })
        }
    }
}

@Composable
private fun DailySummaryCard(summary: DailySalesSummary) {
    ElevatedCard(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Today", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Sales")
                Text(summary.saleCount.toString())
            }
            TotalLine("Gross", summary.grossSalesCents)
            TotalLine("Discounts", -summary.discountCents)
            TotalLine("Service fees", summary.serviceFeeCents)
            TotalLine("Net sales", summary.netSalesCents, strong = true)
            TotalLine("Cash received", summary.cashReceivedCents)
            TotalLine("Change given", summary.changeGivenCents)
            if (summary.balanceDueCents > 0L) {
                TotalLine("Balance due", summary.balanceDueCents, strong = true)
            }
        }
    }
}

@Composable
private fun SaleCard(saleWithItems: SaleWithItems, onReprint: () -> Unit) {
    ElevatedCard(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = bloomUiColors().warmCream),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(saleWithItems.sale.receiptNumber, fontWeight = FontWeight.Bold)
                    Text(
                        dateLabel(saleWithItems.sale.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    saleWithItems.sale.pickupAt?.let { pickupAt ->
                        Text(
                            "Pickup: ${pickupDateLabel(pickupAt)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
                Text(
                    MoneyFormatter.format(saleWithItems.sale.totalCents),
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                saleWithItems.items.joinToString { "${it.quantity} ${it.productName}" },
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (saleWithItems.sale.balanceDueCents > 0L) {
                Text(
                    "Balance due: ${MoneyFormatter.format(saleWithItems.sale.balanceDueCents)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
            OutlinedButton(onClick = onReprint, modifier = Modifier.fillMaxWidth()) {
                Text("Reprint")
            }
        }
    }
}

@Composable
private fun InventoryScreen(
    products: List<ProductEntity>,
    stockMovements: List<StockMovementEntity>,
    onAdjustStock: (Long, Int) -> Unit,
    onSetActive: (Long, Boolean) -> Unit,
    onSaveProduct: (ProductEntity) -> Unit,
    onDeleteProduct: (Long) -> Unit,
) {
    var editingProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var name by remember { mutableStateOf("") }
    var barcode by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(ProductCategory.CUSTOM) }
    var trackStock by remember { mutableStateOf(false) }
    var stock by remember { mutableStateOf("0") }
    var lowStockThreshold by remember { mutableStateOf("3") }
    var active by remember { mutableStateOf(true) }

    fun loadProduct(product: ProductEntity?) {
        editingProduct = product
        name = product?.name.orEmpty()
        barcode = product?.displayBarcode().orEmpty()
        price = product?.let { String.format(Locale.US, "%.2f", it.priceCents / 100.0) }.orEmpty()
        category = product?.category ?: ProductCategory.CUSTOM
        trackStock = product?.trackStock ?: false
        stock = (product?.stockQuantity ?: 0).toString()
        lowStockThreshold = (product?.lowStockThreshold ?: 3).toString()
        active = product?.active ?: true
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            ProductEditorCard(
                title = if (editingProduct == null) "Add product" else "Edit product",
                name = name,
                barcode = barcode,
                price = price,
                category = category,
                trackStock = trackStock,
                stock = stock,
                lowStockThreshold = lowStockThreshold,
                active = active,
                canDelete = editingProduct?.id != null,
                onNameChange = { name = it },
                onBarcodeChange = { barcode = it },
                onPriceChange = { price = it },
                onCategoryChange = { category = it },
                onTrackStockChange = { trackStock = it },
                onStockChange = { stock = it },
                onLowStockThresholdChange = { lowStockThreshold = it },
                onActiveChange = { active = it },
                onNew = { loadProduct(null) },
                onDelete = {
                    val productId = editingProduct?.id
                    if (productId != null) {
                        onDeleteProduct(productId)
                        loadProduct(null)
                    }
                },
                onSave = {
                    val priceCents = MoneyFormatter.parseToCents(price)
                    if (priceCents != null) {
                        onSaveProduct(
                            ProductEntity(
                                id = editingProduct?.id ?: 0,
                                name = name.trim(),
                                barcode = barcode.trim().ifBlank { null },
                                category = category,
                                priceCents = priceCents,
                                trackStock = trackStock,
                                stockQuantity = if (trackStock) stock.toIntOrNull()?.coerceAtLeast(0) ?: 0 else null,
                                lowStockThreshold = lowStockThreshold.toIntOrNull()?.coerceAtLeast(0) ?: 3,
                                active = active,
                                sortOrder = editingProduct?.sortOrder ?: products.size + 1,
                            ),
                        )
                        loadProduct(null)
                    }
                },
            )
        }

        val lowStockProducts = products.filter {
            it.active && it.trackStock && (it.stockQuantity ?: 0) <= it.lowStockThreshold
        }
        if (lowStockProducts.isNotEmpty()) {
            item {
                Text("Low stock", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            listItems(lowStockProducts, key = { "low-${it.id}" }) { product ->
                Text(
                    "${product.name}: ${product.stockQuantity ?: 0} left",
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }

        listItems(products, key = { it.id }) { product ->
            InventoryRow(
                product = product,
                onAdjustStock = onAdjustStock,
                onSetActive = onSetActive,
                onEdit = { loadProduct(product) },
            )
        }

        if (stockMovements.isNotEmpty()) {
            item {
                Text("Stock history", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            listItems(stockMovements, key = { "move-${it.id}" }) { movement ->
                StockMovementRow(movement)
            }
        }
    }
}

@Composable
private fun ProductEditorCard(
    title: String,
    name: String,
    barcode: String,
    price: String,
    category: ProductCategory,
    trackStock: Boolean,
    stock: String,
    lowStockThreshold: String,
    active: Boolean,
    canDelete: Boolean,
    onNameChange: (String) -> Unit,
    onBarcodeChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onCategoryChange: (ProductCategory) -> Unit,
    onTrackStockChange: (Boolean) -> Unit,
    onStockChange: (String) -> Unit,
    onLowStockThresholdChange: (String) -> Unit,
    onActiveChange: (Boolean) -> Unit,
    onNew: () -> Unit,
    onDelete: () -> Unit,
    onSave: () -> Unit,
) {
    ElevatedCard(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = bloomUiColors().warmCream),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = onNew) {
                    Text("New")
                }
            }
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Name") },
                singleLine = true,
            )
            OutlinedTextField(
                value = barcode,
                onValueChange = onBarcodeChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Barcode") },
                singleLine = true,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = price,
                    onValueChange = onPriceChange,
                    modifier = Modifier.weight(1f),
                    label = { Text("Price") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = lowStockThreshold,
                    onValueChange = onLowStockThresholdChange,
                    modifier = Modifier.weight(1f),
                    label = { Text("Low stock") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listItems(ProductCategory.entries) { option ->
                    FilterChip(
                        selected = category == option,
                        onClick = { onCategoryChange(option) },
                        label = { Text(option.label) },
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Track stock")
                Switch(checked = trackStock, onCheckedChange = onTrackStockChange)
            }
            if (trackStock) {
                OutlinedTextField(
                    value = stock,
                    onValueChange = onStockChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Current stock") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Active")
                Switch(checked = active, onCheckedChange = onActiveChange)
            }
            Button(
                onClick = onSave,
                enabled = name.isNotBlank() && MoneyFormatter.parseToCents(price) != null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save Product")
            }
            if (canDelete) {
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Delete Product")
                }
            }
        }
    }
}

@Composable
private fun InventoryRow(
    product: ProductEntity,
    onAdjustStock: (Long, Int) -> Unit,
    onSetActive: (Long, Boolean) -> Unit,
    onEdit: () -> Unit,
) {
    ElevatedCard(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = categorySurfaceColor(product.category)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(product.name, fontWeight = FontWeight.SemiBold)
                Text(
                    "${product.category.label}  ${MoneyFormatter.format(product.priceCents)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Barcode: ${product.displayBarcode()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(stockLabel(product), fontWeight = FontWeight.Bold)
            }
            if (product.trackStock) {
                QuantityButton("-", onClick = { onAdjustStock(product.id, -1) })
                QuantityButton("+", onClick = { onAdjustStock(product.id, 1) })
            }
            OutlinedButton(onClick = onEdit) {
                Text("Edit")
            }
            Switch(
                checked = product.active,
                onCheckedChange = { onSetActive(product.id, it) },
            )
        }
    }
}

@Composable
private fun StockMovementRow(movement: StockMovementEntity) {
    ElevatedCard(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = bloomUiColors().warmCream),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(movement.productName, fontWeight = FontWeight.SemiBold)
                Text(
                    "${movement.reason}  ${dateLabel(movement.createdAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                "${if (movement.delta > 0) "+" else ""}${movement.delta} -> ${movement.resultingStock}",
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun PrinterScreen(
    state: PrinterUiState,
    onRequestPermission: () -> Unit,
    onOpenBluetoothSettings: () -> Unit,
    onRefresh: () -> Unit,
    onSelectPrinter: (PrinterDevice) -> Unit,
    onWidthChange: (ReceiptWidth) -> Unit,
    onPrintLogoOnReceiptsChange: (Boolean) -> Unit,
    onSaveReceiptBranding: (String, String, String) -> Unit,
    onTestPrint: () -> Unit,
) {
    var shopName by remember { mutableStateOf(state.settings.shopName) }
    var shopSubtitle by remember { mutableStateOf(state.settings.shopSubtitle) }
    var receiptFooter by remember { mutableStateOf(state.settings.receiptFooter) }

    LaunchedEffect(state.settings.shopName, state.settings.shopSubtitle, state.settings.receiptFooter) {
        shopName = state.settings.shopName
        shopSubtitle = state.settings.shopSubtitle
        receiptFooter = state.settings.receiptFooter
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onRequestPermission, modifier = Modifier.weight(1f)) {
                    Text("Grant")
                }
                OutlinedButton(onClick = onOpenBluetoothSettings, modifier = Modifier.weight(1f)) {
                    Text("Pair")
                }
                OutlinedButton(onClick = onRefresh, modifier = Modifier.weight(1f)) {
                    Text("Refresh")
                }
            }
        }

        item {
            Text("Receipt width", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReceiptWidth.entries.forEach { width ->
                    FilterChip(
                        selected = state.settings.width == width,
                        onClick = { onWidthChange(width) },
                        label = { Text(width.label) },
                    )
                }
            }
        }

        item {
            ElevatedCard(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = bloomUiColors().warmCream),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Receipt logo", fontWeight = FontWeight.Bold)
                        Text(
                            "Turn on after text test print works.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = state.settings.printLogoOnReceipts,
                        onCheckedChange = onPrintLogoOnReceiptsChange,
                    )
                }
            }
        }

        item {
            Button(
                onClick = onTestPrint,
                enabled = !state.isBusy && state.settings.deviceAddress != null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.isBusy) "Printing" else "Test Print")
            }
        }

        item {
            ElevatedCard(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = bloomUiColors().warmCream),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("Receipt text", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = shopName,
                        onValueChange = { shopName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Shop name") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = shopSubtitle,
                        onValueChange = { shopSubtitle = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Subtitle") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = receiptFooter,
                        onValueChange = { receiptFooter = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Footer") },
                        minLines = 2,
                    )
                    OutlinedButton(
                        onClick = { onSaveReceiptBranding(shopName, shopSubtitle, receiptFooter) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Save Receipt Text")
                    }
                }
            }
        }

        if (state.devices.isEmpty()) {
            item { EmptyState("No paired printers loaded.") }
        } else {
            listItems(state.devices, key = { it.address }) { device ->
                PrinterDeviceRow(
                    device = device,
                    selectedAddress = state.settings.deviceAddress,
                    onSelect = { onSelectPrinter(device) },
                )
            }
        }
    }
}

@Composable
private fun PrinterDeviceRow(
    device: PrinterDevice,
    selectedAddress: String?,
    onSelect: () -> Unit,
) {
    ElevatedCard(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = bloomUiColors().warmCream),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(device.name, fontWeight = FontWeight.SemiBold)
                Text(
                    device.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(onClick = onSelect) {
                Text(if (selectedAddress == device.address) "Selected" else "Select")
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    state: PrinterUiState,
    onPickLogo: () -> Unit,
    onRemoveLogo: () -> Unit,
    onSaveReceiptBranding: (String, String, String) -> Unit,
) {
    var shopName by remember { mutableStateOf(state.settings.shopName) }
    var shopSubtitle by remember { mutableStateOf(state.settings.shopSubtitle) }
    var receiptFooter by remember { mutableStateOf(state.settings.receiptFooter) }

    LaunchedEffect(state.settings.shopName, state.settings.shopSubtitle, state.settings.receiptFooter) {
        shopName = state.settings.shopName
        shopSubtitle = state.settings.shopSubtitle
        receiptFooter = state.settings.receiptFooter
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ElevatedCard(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    LogoMark(logoUri = state.settings.logoUri, modifier = Modifier.size(112.dp))
                    Text(
                        text = "Shop Logo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = onPickLogo, modifier = Modifier.weight(1f)) {
                            Text("Choose Logo")
                        }
                        OutlinedButton(
                            onClick = onRemoveLogo,
                            enabled = state.settings.logoUri != null,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Remove")
                        }
                    }
                }
            }
        }

        item {
            ElevatedCard(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = bloomUiColors().warmCream),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("Shop text", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = shopName,
                        onValueChange = { shopName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Shop name") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = shopSubtitle,
                        onValueChange = { shopSubtitle = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Subtitle") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = receiptFooter,
                        onValueChange = { receiptFooter = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Receipt footer") },
                        minLines = 2,
                    )
                    Button(
                        onClick = { onSaveReceiptBranding(shopName, shopSubtitle, receiptFooter) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Save Settings")
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun shopNameLabel(value: String): String {
    val rawName = value.trim().ifBlank { "Cali's Bloomprints" }
    if (rawName.any { it.isLowerCase() }) return rawName

    return rawName.lowercase(Locale.US)
        .split(" ")
        .joinToString(" ") { word ->
            word.replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase(Locale.US) else char.toString()
            }
        }
}

@Composable
private fun categorySurfaceColor(category: ProductCategory): Color {
    val bloom = bloomUiColors()
    return when (category) {
        ProductCategory.BOUQUET -> bloom.blush
        ProductCategory.FLOWER -> bloom.leafMist
        ProductCategory.GIFT -> bloom.giftCream
        ProductCategory.PRINTING -> bloom.giftLilac
        ProductCategory.CUSTOM -> bloom.warmCream
    }
}

@Composable
private fun categoryAccentColor(category: ProductCategory): Color {
    return if (isSystemInDarkTheme()) {
        when (category) {
            ProductCategory.BOUQUET -> Color(0xFFFFB0C8)
            ProductCategory.FLOWER -> Color(0xFFBFE2B9)
            ProductCategory.GIFT -> Color(0xFFE8B985)
            ProductCategory.PRINTING -> Color(0xFFD9BEFF)
            ProductCategory.CUSTOM -> Color(0xFFD8AEBB)
        }
    } else {
        when (category) {
            ProductCategory.BOUQUET -> Color(0xFFB84F72)
            ProductCategory.FLOWER -> Color(0xFF557D58)
            ProductCategory.GIFT -> Color(0xFFC8783E)
            ProductCategory.PRINTING -> Color(0xFF8C6BB1)
            ProductCategory.CUSTOM -> Color(0xFF8A5D69)
        }
    }
}

private fun stockLabel(product: ProductEntity): String {
    return if (product.trackStock) {
        val stock = product.stockQuantity ?: 0
        if (stock <= product.lowStockThreshold) {
            "Low stock: $stock"
        } else {
            "Stock: $stock"
        }
    } else {
        "Open stock"
    }
}

private fun stockPrintCount(product: ProductEntity): Int {
    return if (product.trackStock) {
        (product.stockQuantity ?: 0).coerceIn(0, MAX_BARCODE_LABELS)
    } else {
        1
    }
}

private fun dateLabel(timestamp: Long): String {
    return SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.US).format(Date(timestamp))
}

private fun pickupDateLabel(timestamp: Long): String {
    return SimpleDateFormat("MMM dd, hh:mm a", Locale.US).format(Date(timestamp))
}

private fun pickupDateInput(daysFromToday: Int): String {
    val calendar = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, daysFromToday)
    }
    return formatPickupDateInput(calendar)
}

private fun formatPickupDateInput(calendar: Calendar): String {
    return SimpleDateFormat("MM/dd/yyyy", Locale.US).format(calendar.time)
}

private fun formatPickupTimeInput(calendar: Calendar): String {
    return SimpleDateFormat("h:mm a", Locale.US).format(calendar.time)
}

private fun calendarFromDateInput(value: String): Calendar? {
    val date = parseWithFormats(value, listOf("MM/dd/yyyy", "M/d/yyyy", "yyyy-MM-dd")) ?: return null
    return Calendar.getInstance().apply { time = date }
}

private fun calendarFromTimeInput(value: String): Calendar? {
    val date = parseWithFormats(value.uppercase(Locale.US), listOf("h:mm a", "hh:mm a", "H:mm", "HH:mm"))
        ?: return null
    return Calendar.getInstance().apply { time = date }
}

private fun parseWithFormats(value: String, patterns: List<String>): Date? {
    val cleanValue = value.trim()
    if (cleanValue.isBlank()) return null

    return patterns.firstNotNullOfOrNull { pattern ->
        runCatching {
            SimpleDateFormat(pattern, Locale.US).apply {
                isLenient = false
            }.parse(cleanValue)
        }.getOrNull()
    }
}
