package com.example.ui

import android.app.Application
import android.content.ComponentCallbacks2
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.AppDatabase
import com.example.data.ReceiptRepository
import com.example.model.BlackpoolBar
import com.example.model.BlackpoolVenues
import com.example.model.GigType
import com.example.model.PackageTier
import com.example.model.PaymentMethod
import com.example.model.PrinterChannel
import com.example.model.PrinterStatus
import com.example.model.RamInfo
import com.example.model.ReceiptData
import com.example.model.ReceiptIconType
import com.example.model.ReceiptItem
import com.example.model.WirelessOrder
import com.example.network.WirelessFlaskClient
import com.example.printer.PrintResult
import com.example.printer.UnifiedPrinterManager
import com.example.util.MemoryOptimizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class PosTab(val label: String) {
    BUILDER("POS Builder"),
    PREVIEW("58mm Live Paper"),
    PRINTERS("Printer Hardware"),
    HISTORY("Sync Ledger")
}

sealed class UiMessage {
    data class Success(val message: String) : UiMessage()
    data class Error(val message: String) : UiMessage()
    data class Warning(val message: String) : UiMessage()
}

class PosViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ReceiptRepository
    val printerManager: UnifiedPrinterManager

    val printerStatus: StateFlow<PrinterStatus>
    val allReceipts: StateFlow<List<ReceiptData>>
    val unsyncedCount: StateFlow<Int>

    private val _currentReceipt = MutableStateFlow(ReceiptData())
    val currentReceipt: StateFlow<ReceiptData> = _currentReceipt.asStateFlow()

    private val _selectedChannel = MutableStateFlow(PrinterChannel.SUNMI_BUILTIN)
    val selectedChannel: StateFlow<PrinterChannel> = _selectedChannel.asStateFlow()

    private val _activeTab = MutableStateFlow(PosTab.BUILDER)
    val activeTab: StateFlow<PosTab> = _activeTab.asStateFlow()

    private val _isOfflineSimulated = MutableStateFlow(false)
    val isOfflineSimulated: StateFlow<Boolean> = _isOfflineSimulated.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _uiMessages = MutableSharedFlow<UiMessage>()
    val uiMessages: SharedFlow<UiMessage> = _uiMessages.asSharedFlow()

    // RAM Status tracking for <1GB devices
    private val _ramInfo = MutableStateFlow(
        RamInfo(
            totalMb = MemoryOptimizer.getTotalRamMb(application),
            freeMb = MemoryOptimizer.getAvailableRamMb(application),
            isLowRamDevice = MemoryOptimizer.isLowRam(application),
            lowRamEngineActive = true
        )
    )
    val ramInfo: StateFlow<RamInfo> = _ramInfo.asStateFlow()

    // Custom Icon Bitmap
    private val _customIconBitmap = MutableStateFlow<Bitmap?>(null)
    val customIconBitmap: StateFlow<Bitmap?> = _customIconBitmap.asStateFlow()

    private var cachedLogoBitmap: Bitmap? = null

    private val customLogoFile = java.io.File(application.filesDir, "custom_receipt_logo.png")

    // Blackpool Venues Filter & Search State
    private val _blackpoolSearchQuery = MutableStateFlow("")
    val blackpoolSearchQuery: StateFlow<String> = _blackpoolSearchQuery.asStateFlow()

    private val _selectedBlackpoolArea = MutableStateFlow("All Blackpool")
    val selectedBlackpoolArea: StateFlow<String> = _selectedBlackpoolArea.asStateFlow()

    // Wireless Flask Web App Gateway State
    private val wirelessClient = WirelessFlaskClient()
    private val _wirelessServerUrl = MutableStateFlow("http://10.0.2.2:5000")
    val wirelessServerUrl: StateFlow<String> = _wirelessServerUrl.asStateFlow()

    private val _isWirelessOnline = MutableStateFlow(false)
    val isWirelessOnline: StateFlow<Boolean> = _isWirelessOnline.asStateFlow()

    private val _pendingWirelessOrders = MutableStateFlow<List<WirelessOrder>>(emptyList())
    val pendingWirelessOrders: StateFlow<List<WirelessOrder>> = _pendingWirelessOrders.asStateFlow()

    private val _isWirelessSyncing = MutableStateFlow(false)
    val isWirelessSyncing: StateFlow<Boolean> = _isWirelessSyncing.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = ReceiptRepository(database.receiptDao())
        printerManager = UnifiedPrinterManager(application)

        printerStatus = printerManager.status
        allReceipts = repository.allReceipts.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
        unsyncedCount = repository.unsyncedCount.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            0
        )

        // Memory-lean default brand logo decode using RGB_565 and sampled dimensions
        try {
            cachedLogoBitmap = MemoryOptimizer.decodeSampledBitmap(
                application,
                R.drawable.img_naomi_logo,
                maxDimension = 320
            )
        } catch (e: Exception) {
            // fallback gracefully
        }

        // Restore custom uploaded logo if previously saved by the user
        if (customLogoFile.exists()) {
            try {
                val savedBmp = android.graphics.BitmapFactory.decodeFile(customLogoFile.absolutePath)
                if (savedBmp != null) {
                    _customIconBitmap.value = savedBmp
                    _currentReceipt.value = _currentReceipt.value.copy(
                        iconType = ReceiptIconType.CUSTOM,
                        customIconUri = customLogoFile.absolutePath
                    )
                }
            } catch (e: Exception) {
                // Ignore fallback
            }
        }

        // Initialize connection to Wireless Flask Gateway
        checkWirelessConnection()
    }

    fun refreshRamInfo() {
        _ramInfo.value = RamInfo(
            totalMb = MemoryOptimizer.getTotalRamMb(getApplication()),
            freeMb = MemoryOptimizer.getAvailableRamMb(getApplication()),
            isLowRamDevice = MemoryOptimizer.isLowRam(getApplication()),
            lowRamEngineActive = true
        )
    }

    fun clearMemoryBuffers() {
        printerManager.clearBuffers()
        System.gc()
        refreshRamInfo()
        viewModelScope.launch {
            _uiMessages.emit(UiMessage.Success("Low-RAM buffer purged. Garbage collection completed."))
        }
    }

    fun onTrimMemory(level: Int) {
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            printerManager.clearBuffers()
            System.gc()
            refreshRamInfo()
        }
    }

    // Blackpool Bar Selection and Search
    fun setBlackpoolSearch(query: String) {
        _blackpoolSearchQuery.value = query
    }

    fun setBlackpoolArea(area: String) {
        _selectedBlackpoolArea.value = area
    }

    fun selectBlackpoolBar(bar: BlackpoolBar) {
        _currentReceipt.value = _currentReceipt.value.copy(
            venueName = "${bar.name}, ${bar.address}",
            clientName = bar.name,
            clientContact = bar.contact
        )
        viewModelScope.launch {
            _uiMessages.emit(UiMessage.Success("Loaded Blackpool Venue: ${bar.name}"))
        }
    }

    // Icon handling & Custom Logo Upload
    fun selectIconType(type: ReceiptIconType) {
        _currentReceipt.value = _currentReceipt.value.copy(iconType = type)
    }

    fun setCustomIcon(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val bmp = MemoryOptimizer.decodeSampledBitmapFromUri(getApplication(), uri, maxDimension = 320)
            if (bmp != null) {
                _customIconBitmap.value?.recycle()
                _customIconBitmap.value = bmp
                // Save to disk for persistence across restarts
                try {
                    java.io.FileOutputStream(customLogoFile).use { out ->
                        bmp.compress(Bitmap.CompressFormat.PNG, 95, out)
                    }
                } catch (e: Exception) {
                    // Ignore persistence failure
                }
                _currentReceipt.value = _currentReceipt.value.copy(
                    iconType = ReceiptIconType.CUSTOM,
                    customIconUri = uri.toString()
                )
                _uiMessages.emit(UiMessage.Success("Custom logo uploaded & saved for all 58mm thermal receipts!"))
            } else {
                _uiMessages.emit(UiMessage.Error("Failed to decode image. Please pick another photo."))
            }
        }
    }

    fun removeCustomIcon() {
        _customIconBitmap.value?.recycle()
        _customIconBitmap.value = null
        if (customLogoFile.exists()) {
            customLogoFile.delete()
        }
        _currentReceipt.value = _currentReceipt.value.copy(
            iconType = ReceiptIconType.NAOMI_LOGO,
            customIconUri = null
        )
        viewModelScope.launch {
            _uiMessages.emit(UiMessage.Success("Custom logo removed. Default brand restored."))
        }
    }

    private fun getActiveIconBitmap(receipt: ReceiptData): Bitmap? {
        return when (receipt.iconType) {
            ReceiptIconType.NONE -> null
            ReceiptIconType.NAOMI_LOGO -> cachedLogoBitmap
            ReceiptIconType.VINYL -> MemoryOptimizer.vectorToBitmap(getApplication(), R.drawable.ic_vinyl_record, 256, 256)
            ReceiptIconType.HEADPHONES -> MemoryOptimizer.vectorToBitmap(getApplication(), R.drawable.ic_dj_headphones, 256, 256)
            ReceiptIconType.CROWN_VIP -> MemoryOptimizer.vectorToBitmap(getApplication(), R.drawable.ic_crown_vip, 256, 256)
            ReceiptIconType.STAR -> MemoryOptimizer.vectorToBitmap(getApplication(), R.drawable.ic_stage_star, 256, 256)
            ReceiptIconType.CUSTOM -> _customIconBitmap.value ?: cachedLogoBitmap
        }
    }

    // Free Event toggles
    fun toggleFreeEvent(enabled: Boolean) {
        if (enabled) {
            _currentReceipt.value = _currentReceipt.value.copy(
                isFreeEvent = true,
                packageTier = PackageTier.FREE_ADMISSION,
                paymentMethod = PaymentMethod.FREE_PASS,
                taxPercent = 0.0,
                discountPercent = 0.0,
                items = listOf(
                    ReceiptItem(name = "Free Community Event Pass", quantity = 1, unitPrice = 0.0)
                )
            )
            viewModelScope.launch {
                _uiMessages.emit(UiMessage.Success("Free Event mode enabled! Admission is £0.00."))
            }
        } else {
            _currentReceipt.value = _currentReceipt.value.copy(
                isFreeEvent = false,
                packageTier = PackageTier.BAR_ADMISSION,
                paymentMethod = PaymentMethod.CARD_TERMINAL,
                taxPercent = 20.0,
                discountPercent = 0.0,
                items = listOf(
                    ReceiptItem(name = "Standard Bar Admission", quantity = 1, unitPrice = 5.0),
                    ReceiptItem(name = "Guest DJ Track Request", quantity = 1, unitPrice = 3.0)
                )
            )
            viewModelScope.launch {
                _uiMessages.emit(UiMessage.Success("Standard Paid Gig mode restored (20% UK VAT)."))
            }
        }
    }

    fun setFreeCommunityPreset() {
        _currentReceipt.value = _currentReceipt.value.copy(
            clientName = "North Pier Showcase",
            clientContact = "01253 623304",
            gigType = GigType.FREE_COMMUNITY,
            venueName = "North Pier Sun Lounge & Deck, Promenade, Blackpool FY1 1NE",
            gigDate = "Tonight • 19:00 - 23:00",
            packageTier = PackageTier.FREE_ADMISSION,
            paymentMethod = PaymentMethod.FREE_PASS,
            isFreeEvent = true,
            taxPercent = 0.0,
            discountPercent = 0.0,
            footerNotes = "Blackpool Community DJ Showcase. Free music & good vibes along the Golden Mile!",
            items = listOf(
                ReceiptItem(name = "Free Community Stage Access Pass", quantity = 1, unitPrice = 0.0),
                ReceiptItem(name = "Complimentary Event Wristband", quantity = 1, unitPrice = 0.0)
            )
        )
        viewModelScope.launch {
            _uiMessages.emit(UiMessage.Success("Free Blackpool Community Event preset applied!"))
        }
    }

    fun setTab(tab: PosTab) {
        _activeTab.value = tab
    }

    fun selectChannel(channel: PrinterChannel) {
        _selectedChannel.value = channel
    }

    fun toggleOfflineSimulation() {
        _isOfflineSimulated.value = !_isOfflineSimulated.value
        val state = if (_isOfflineSimulated.value) "OFFLINE MODE: Transactions buffered locally in Room DB" else "ONLINE: Auto-sync connected"
        viewModelScope.launch {
            _uiMessages.emit(UiMessage.Warning(state))
        }
    }

    // Receipt editing
    fun updateClientName(name: String) {
        _currentReceipt.value = _currentReceipt.value.copy(clientName = name)
    }

    fun updateClientContact(contact: String) {
        _currentReceipt.value = _currentReceipt.value.copy(clientContact = contact)
    }

    fun updateVenue(venue: String) {
        _currentReceipt.value = _currentReceipt.value.copy(venueName = venue)
    }

    fun updateGigDate(date: String) {
        _currentReceipt.value = _currentReceipt.value.copy(gigDate = date)
    }

    fun updateGigType(type: GigType) {
        _currentReceipt.value = _currentReceipt.value.copy(gigType = type)
    }

    fun updatePaymentMethod(method: PaymentMethod) {
        _currentReceipt.value = _currentReceipt.value.copy(paymentMethod = method)
    }

    fun updateFooterNotes(notes: String) {
        _currentReceipt.value = _currentReceipt.value.copy(footerNotes = notes)
    }

    fun updatePackageTier(tier: PackageTier) {
        val currentItems = _currentReceipt.value.items.toMutableList()
        val tierItem = ReceiptItem(
            name = "${tier.title} Performance",
            quantity = 1,
            unitPrice = tier.basePrice
        )
        if (currentItems.isNotEmpty()) {
            currentItems[0] = tierItem
        } else {
            currentItems.add(tierItem)
        }
        _currentReceipt.value = _currentReceipt.value.copy(
            packageTier = tier,
            items = currentItems,
            isFreeEvent = tier == PackageTier.FREE_ADMISSION
        )
    }

    fun addLineItem(item: ReceiptItem) {
        val currentItems = _currentReceipt.value.items.toMutableList()
        currentItems.add(item)
        _currentReceipt.value = _currentReceipt.value.copy(items = currentItems)
    }

    fun removeLineItem(itemId: String) {
        val currentItems = _currentReceipt.value.items.filterNot { it.id == itemId }
        _currentReceipt.value = _currentReceipt.value.copy(items = currentItems)
    }

    fun updateTaxPercent(tax: Double) {
        _currentReceipt.value = _currentReceipt.value.copy(taxPercent = tax)
    }

    fun resetNewReceipt() {
        val newId = "NC-" + (100000 + (Math.random() * 900000).toInt())
        val defaultBar = BlackpoolVenues.ALL_BARS.first()
        _currentReceipt.value = ReceiptData(
            id = newId,
            clientName = defaultBar.name,
            clientContact = defaultBar.contact,
            venueName = "${defaultBar.name}, ${defaultBar.address}",
            gigDate = "Tonight • 22:00",
            taxPercent = 20.0,
            iconType = if (_customIconBitmap.value != null) ReceiptIconType.CUSTOM else ReceiptIconType.NAOMI_LOGO,
            packageTier = PackageTier.BAR_ADMISSION,
            items = listOf(
                ReceiptItem(name = "Standard Bar Admission", quantity = 1, unitPrice = 5.0),
                ReceiptItem(name = "Guest DJ Track Request", quantity = 1, unitPrice = 3.0)
            )
        )
    }

    fun printCurrentReceipt() {
        viewModelScope.launch {
            val receipt = _currentReceipt.value
            val isOffline = _isOfflineSimulated.value

            // 1. Save to Room database (buffered or direct)
            repository.saveReceipt(receipt, isOfflineBuffered = isOffline)

            // 2. Dispatch print job to selected printer channel
            val iconBitmap = getActiveIconBitmap(receipt)
            val result = printerManager.printReceipt(receipt, _selectedChannel.value, iconBitmap)

            when (result) {
                is PrintResult.Success -> {
                    repository.markPrinted(receipt.id, _selectedChannel.value.hardwareCode)
                    _uiMessages.emit(UiMessage.Success(result.message))
                }
                is PrintResult.OutOfPaper -> {
                    _uiMessages.emit(UiMessage.Error(result.message))
                }
                is PrintResult.Error -> {
                    _uiMessages.emit(UiMessage.Error(result.errorReason))
                }
            }
        }
    }

    fun reprintReceipt(receipt: ReceiptData) {
        viewModelScope.launch {
            val iconBitmap = getActiveIconBitmap(receipt)
            val result = printerManager.printReceipt(receipt, _selectedChannel.value, iconBitmap)
            when (result) {
                is PrintResult.Success -> {
                    repository.markPrinted(receipt.id, _selectedChannel.value.hardwareCode)
                    _uiMessages.emit(UiMessage.Success("Reprinted ${receipt.id} successfully!"))
                }
                is PrintResult.OutOfPaper -> {
                    _uiMessages.emit(UiMessage.Error(result.message))
                }
                is PrintResult.Error -> {
                    _uiMessages.emit(UiMessage.Error(result.errorReason))
                }
            }
        }
    }

    fun syncAllBufferedTransactions() {
        viewModelScope.launch {
            _isSyncing.value = true
            val result = repository.syncBufferedReceipts()
            _isSyncing.value = false
            result.onSuccess { count ->
                _uiMessages.emit(UiMessage.Success("Synchronized $count offline buffered transactions with Naomi Cloud!"))
            }.onFailure { err ->
                _uiMessages.emit(UiMessage.Error("Sync failed: ${err.message}"))
            }
        }
    }

    fun togglePaperRollAlert() {
        printerManager.togglePaperRoll()
    }

    fun reloadPaperRoll() {
        printerManager.reloadPaper()
        viewModelScope.launch {
            _uiMessages.emit(UiMessage.Success("58mm Thermal paper reloaded. Ready to print."))
        }
    }

    // Wireless Flask Web App Gateway Management
    fun setWirelessServerUrl(url: String) {
        _wirelessServerUrl.value = url
        wirelessClient.updateBaseUrl(url)
        checkWirelessConnection()
    }

    fun checkWirelessConnection() {
        viewModelScope.launch {
            _isWirelessSyncing.value = true
            val online = wirelessClient.checkServerStatus()
            _isWirelessOnline.value = online
            if (online) {
                val orders = wirelessClient.fetchPendingOrders()
                _pendingWirelessOrders.value = orders
            }
            _isWirelessSyncing.value = false
        }
    }

    fun fetchPendingWirelessOrders() {
        viewModelScope.launch {
            _isWirelessSyncing.value = true
            val orders = wirelessClient.fetchPendingOrders()
            _pendingWirelessOrders.value = orders
            _isWirelessSyncing.value = false
        }
    }

    fun loadAndPrintWirelessOrder(order: WirelessOrder) {
        viewModelScope.launch {
            val loadedReceipt = ReceiptData(
                id = order.id,
                clientName = order.clientName,
                clientContact = order.clientContact,
                venueName = order.venueName,
                items = order.items,
                paymentMethod = order.paymentMethod,
                taxPercent = order.taxPercent,
                footerNotes = if (order.notes.isNotBlank()) order.notes else "Wireless Order • Naomi-Chan™ Blackpool DJ Services",
                packageTier = PackageTier.BAR_ADMISSION,
                iconType = if (_customIconBitmap.value != null) ReceiptIconType.CUSTOM else ReceiptIconType.NAOMI_LOGO
            )
            _currentReceipt.value = loadedReceipt

            // Mark order as printed on the Flask wireless gateway
            wirelessClient.markOrderPrinted(order.id)
            fetchPendingWirelessOrders()

            // Automatically print to hardware thermal printer
            printCurrentReceipt()
            _uiMessages.emit(UiMessage.Success("Wireless order ${order.id} loaded and printed on thermal paper!"))
        }
    }

    fun broadcastCurrentReceiptToWeb() {
        viewModelScope.launch {
            val receipt = _currentReceipt.value
            _isWirelessSyncing.value = true
            val url = wirelessClient.broadcastReceiptToWeb(receipt)
            _isWirelessSyncing.value = false
            if (url != null) {
                _uiMessages.emit(UiMessage.Success("Receipt sent wirelessly to Web Gateway! View at $url"))
            } else {
                _uiMessages.emit(UiMessage.Error("Failed to broadcast receipt to wireless server"))
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        _customIconBitmap.value?.recycle()
        _customIconBitmap.value = null
        if (cachedLogoBitmap?.isRecycled == false) {
            cachedLogoBitmap?.recycle()
        }
        printerManager.cleanup()
    }
}

