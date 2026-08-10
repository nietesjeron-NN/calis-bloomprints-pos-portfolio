package com.calisbloomprints.pos.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.calisbloomprints.pos.data.db.entity.ProductEntity
import com.calisbloomprints.pos.data.model.CartLine
import com.calisbloomprints.pos.data.model.DailySalesSummary
import com.calisbloomprints.pos.data.model.MoneyFormatter
import com.calisbloomprints.pos.data.model.ProductCategory
import com.calisbloomprints.pos.data.repository.ProductRepository
import com.calisbloomprints.pos.data.repository.SalesRepository
import com.calisbloomprints.pos.pickup.PickupReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class PosViewModel(
    private val productRepository: ProductRepository,
    private val salesRepository: SalesRepository,
    private val pickupReminderScheduler: PickupReminderScheduler,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PosUiState())
    val uiState: StateFlow<PosUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            productRepository.ensureSeedData()
        }
        viewModelScope.launch {
            productRepository.activeProducts.collect { products ->
                _uiState.update { state ->
                    state.copy(
                        products = products,
                        cartLines = reconcileCart(state.cartLines, products),
                    )
                }
            }
        }
        viewModelScope.launch {
            productRepository.allProducts.collect { products ->
                _uiState.update { it.copy(inventoryProducts = products) }
            }
        }
        viewModelScope.launch {
            salesRepository.recentSales.collect { sales ->
                _uiState.update { it.copy(recentSales = sales) }
            }
        }
        viewModelScope.launch {
            salesRepository.allSales.collect { sales ->
                _uiState.update { it.copy(allSales = sales) }
            }
        }
        viewModelScope.launch {
            salesRepository.pickupOrders.collect { sales ->
                _uiState.update { it.copy(pickupOrders = sales) }
            }
        }
        viewModelScope.launch {
            salesRepository.todaySales.collect { sales ->
                _uiState.update { it.copy(todaySummary = DailySalesSummary.fromSales(sales)) }
            }
        }
        viewModelScope.launch {
            productRepository.recentStockMovements.collect { movements ->
                _uiState.update { it.copy(stockMovements = movements) }
            }
        }
    }

    fun selectCategory(category: ProductCategory?) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun addProduct(product: ProductEntity) {
        _uiState.update { state ->
            val currentLine = state.cartLines.firstOrNull { it.product.id == product.id }
            val nextQuantity = (currentLine?.quantity ?: 0) + 1
            if (product.trackStock && nextQuantity > (product.stockQuantity ?: 0)) {
                state.copy(message = "Not enough stock for ${product.name}.")
            } else {
                val nextLines = if (currentLine == null) {
                    state.cartLines + CartLine(product, 1)
                } else {
                    state.cartLines.map { line ->
                        if (line.product.id == product.id) line.copy(quantity = nextQuantity) else line
                    }
                }
                state.copy(cartLines = nextLines, message = "${product.name} added.")
            }
        }
    }

    fun changeQuantity(productId: Long, delta: Int) {
        _uiState.update { state ->
            val nextLines = state.cartLines.mapNotNull { line ->
                if (line.product.id != productId) return@mapNotNull line
                val nextQuantity = line.quantity + delta
                when {
                    nextQuantity <= 0 -> null
                    line.product.trackStock && nextQuantity > (line.product.stockQuantity ?: 0) -> line
                    else -> line.copy(quantity = nextQuantity)
                }
            }
            state.copy(cartLines = nextLines)
        }
    }

    fun updateLinePrice(productId: Long, value: String) {
        val priceCents = MoneyFormatter.parseToCents(value)
        if (priceCents == null || priceCents < 0L) {
            _uiState.update { it.copy(message = "Enter a valid custom price.") }
            return
        }

        _uiState.update { state ->
            state.copy(
                cartLines = state.cartLines.map { line ->
                    if (line.product.id == productId) {
                        line.copy(customUnitPriceCents = priceCents)
                    } else {
                        line
                    }
                },
            )
        }
    }

    fun clearCart() {
        _uiState.update { it.copy(cartLines = emptyList(), message = "Cart cleared.") }
    }

    fun updateCustomerName(value: String) {
        _uiState.update { it.copy(customerName = value) }
    }

    fun updateCustomerContact(value: String) {
        _uiState.update { it.copy(customerContact = value) }
    }

    fun updateOrderNotes(value: String) {
        _uiState.update { it.copy(orderNotes = value) }
    }

    fun updatePickupDate(value: String) {
        _uiState.update { it.copy(pickupDate = value) }
    }

    fun updatePickupTime(value: String) {
        _uiState.update { it.copy(pickupTime = value) }
    }

    fun updatePickupReminderMinutes(value: Int) {
        _uiState.update { it.copy(pickupReminderMinutes = value) }
    }

    fun updateDiscount(value: String) {
        _uiState.update { it.copy(discount = value) }
    }

    fun updateServiceFee(value: String) {
        _uiState.update { it.copy(serviceFee = value) }
    }

    fun updateDeposit(value: String) {
        _uiState.update { it.copy(deposit = value) }
    }

    fun updateCashReceived(value: String) {
        _uiState.update { it.copy(cashReceived = value) }
    }

    fun checkout(printAfterSave: Boolean) {
        val state = _uiState.value
        val amountTenderedCents = state.amountTenderedCents

        if (state.cartLines.isEmpty()) {
            _uiState.update { it.copy(message = "Add at least one item before checkout.") }
            return
        }

        if (amountTenderedCents == null) {
            _uiState.update { it.copy(message = "Enter the cash received.") }
            return
        }

        if (state.discountCents > state.cartTotalCents) {
            _uiState.update { it.copy(message = "Discount cannot be more than subtotal.") }
            return
        }

        if (state.depositCents > state.orderTotalCents) {
            _uiState.update { it.copy(message = "Deposit cannot be more than the total.") }
            return
        }

        if (amountTenderedCents < state.amountDueNowCents) {
            _uiState.update { it.copy(message = "Cash received is less than amount due now.") }
            return
        }

        val pickupAt = parsePickupAt(state.pickupDate, state.pickupTime).fold(
            onSuccess = { it },
            onFailure = { throwable ->
                _uiState.update {
                    it.copy(message = throwable.message ?: "Enter a valid pickup date and time.")
                }
                return
            },
        )
        val pickupReminderAt = pickupAt?.let { reminderTime(it, state.pickupReminderMinutes) }

        _uiState.update { it.copy(isCheckingOut = true, message = null) }

        viewModelScope.launch {
            val result = salesRepository.recordCashSale(
                lines = state.cartLines,
                customerName = state.customerName,
                customerContact = state.customerContact,
                notes = state.orderNotes,
                discountCents = state.discountCents,
                serviceFeeCents = state.serviceFeeCents,
                depositCents = state.depositCents,
                amountTenderedCents = amountTenderedCents,
                pickupAt = pickupAt,
                pickupReminderAt = pickupReminderAt,
            )

            result.fold(
                onSuccess = { sale ->
                    val remindersReady = if (sale.sale.pickupAt != null) {
                        pickupReminderScheduler.schedule(sale)
                    } else {
                        true
                    }
                    _uiState.update {
                        it.copy(
                            cartLines = emptyList(),
                            customerName = "",
                            customerContact = "",
                            orderNotes = "",
                            pickupDate = "",
                            pickupTime = "",
                            discount = "",
                            serviceFee = "",
                            deposit = "",
                            cashReceived = "",
                            isCheckingOut = false,
                            message = checkoutMessage(sale.sale.changeCents, sale.sale.pickupAt, remindersReady),
                            pendingPrintSale = if (printAfterSave) sale else null,
                            needsNotificationPermission = sale.sale.pickupAt != null && !remindersReady,
                        )
                    }
                },
                onFailure = { throwable ->
                    _uiState.update {
                        it.copy(
                            isCheckingOut = false,
                            message = throwable.message ?: "Could not save the sale.",
                        )
                    }
                },
            )
        }
    }

    fun adjustInventory(productId: Long, delta: Int) {
        viewModelScope.launch {
            productRepository.adjustStock(productId, delta)
        }
    }

    fun setProductActive(productId: Long, active: Boolean) {
        viewModelScope.launch {
            productRepository.setActive(productId, active)
        }
    }

    fun saveProduct(product: ProductEntity) {
        viewModelScope.launch {
            productRepository.saveProduct(product)
            _uiState.update { it.copy(message = "${product.name} saved.") }
        }
    }

    fun deleteProduct(productId: Long) {
        viewModelScope.launch {
            productRepository.deleteProduct(productId)
            _uiState.update { it.copy(message = "Product deleted.") }
        }
    }

    fun consumePrintRequest() {
        _uiState.update { it.copy(pendingPrintSale = null) }
    }

    fun consumeNotificationPermissionRequest() {
        _uiState.update { it.copy(needsNotificationPermission = false) }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun reconcileCart(
        cartLines: List<CartLine>,
        freshProducts: List<ProductEntity>,
    ): List<CartLine> {
        return cartLines.mapNotNull { line ->
            val freshProduct = freshProducts.firstOrNull { it.id == line.product.id } ?: return@mapNotNull null
            val nextQuantity = if (freshProduct.trackStock) {
                line.quantity.coerceAtMost(freshProduct.stockQuantity ?: 0)
            } else {
                line.quantity
            }
            if (nextQuantity <= 0) null else line.copy(product = freshProduct, quantity = nextQuantity)
        }
    }

    private fun parsePickupAt(dateText: String, timeText: String): Result<Long?> = runCatching {
        val dateValue = dateText.trim()
        val timeValue = timeText.trim()
        if (dateValue.isBlank() && timeValue.isBlank()) return@runCatching null
        require(dateValue.isNotBlank()) { "Enter the pickup date." }
        require(timeValue.isNotBlank()) { "Enter the pickup time." }

        val date = parseWithFormats(dateValue, DATE_FORMATS)
            ?: error("Use pickup date like 08/06/2026.")
        val time = parseWithFormats(timeValue.uppercase(Locale.US), TIME_FORMATS)
            ?: error("Use pickup time like 3:30 PM.")

        val dateCalendar = Calendar.getInstance().apply { this.time = date }
        val timeCalendar = Calendar.getInstance().apply { this.time = time }
        val pickupCalendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, dateCalendar.get(Calendar.YEAR))
            set(Calendar.MONTH, dateCalendar.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, dateCalendar.get(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY))
            set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        require(pickupCalendar.timeInMillis > System.currentTimeMillis()) {
            "Pickup time must be in the future."
        }
        pickupCalendar.timeInMillis
    }

    private fun parseWithFormats(value: String, patterns: List<String>): Date? {
        return patterns.firstNotNullOfOrNull { pattern ->
            runCatching {
                SimpleDateFormat(pattern, Locale.US).apply {
                    isLenient = false
                }.parse(value)
            }.getOrNull()
        }
    }

    private fun reminderTime(pickupAt: Long, reminderMinutes: Int): Long {
        val requestedReminder = pickupAt - reminderMinutes * 60_000L
        return requestedReminder.coerceAtLeast(System.currentTimeMillis() + 10_000L)
    }

    private fun checkoutMessage(changeCents: Long, pickupAt: Long?, remindersReady: Boolean): String {
        val baseMessage = "Sale saved. Change: ${MoneyFormatter.format(changeCents)}"
        if (pickupAt == null) return baseMessage
        return if (remindersReady) {
            "$baseMessage Pickup reminder scheduled."
        } else {
            "$baseMessage Allow notifications to show pickup reminders."
        }
    }

    class Factory(
        private val productRepository: ProductRepository,
        private val salesRepository: SalesRepository,
        private val pickupReminderScheduler: PickupReminderScheduler,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PosViewModel(productRepository, salesRepository, pickupReminderScheduler) as T
        }
    }

    private companion object {
        val DATE_FORMATS = listOf("MM/dd/yyyy", "M/d/yyyy", "yyyy-MM-dd")
        val TIME_FORMATS = listOf("h:mm a", "hh:mm a", "H:mm", "HH:mm")
    }
}
