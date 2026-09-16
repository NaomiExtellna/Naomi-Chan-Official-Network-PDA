package com.example.ui

import android.app.Application
import android.content.ComponentCallbacks2
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.AppDatabase
import com.example.data.AuditRepository
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
import com.example.model.generateReceiptId
import com.example.network.WirelessFlaskClient
import com.example.printer.PrintResult
import com.example.printer.UnifiedPrinterManager
import com.example.util.DiagnosticLog
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
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

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

data class OperatorCapabilities(
    val staffId: String = "",
    val displayName: String = "",
    val isAdmin: Boolean = false,
    val canVoid: Boolean = false,
    val canExport: Boolean = false,
    val canEditVenues: Boolean = false,
    val canChangeGateway: Boolean = false,
    val canViewTotals: Boolean = false
) {
    val canVoidReceipts: Boolean get() = isAdmin || canVoid
    val canExportData: Boolean get() = isAdmin || canExport
    val canManageVenues: Boolean get() = isAdmin || canEditVenues
    val canConfigureGateway: Boolean get() = isAdmin || canChangeGateway
    val canViewFinancialTotals: Boolean get() = isAdmin || canViewTotals
}

data class DatabaseStats(
    val receiptCount: Int,
    val activeCount: Int,
    val voidedCount: Int,
    val archivedCount: Int,
    val databaseBytes: Long,
    val oldestReceiptAt: Long?,
    val newestReceiptAt: Long?,
    val lastBackupAt: Long,
    val lastRestoreAt: Long
)

class PosViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = ReceiptRepository(database.receiptDao())
    private val auditRepository = AuditRepository(database.auditDao())
    private val opsPrefs = application.getSharedPreferences("naomi_ops_meta", Context.MODE_PRIVATE)
    val printerManager = UnifiedPrinterManager(application)

    val printerStatus: StateFlow<PrinterStatus> = printerManager.status
    val allReceipts: StateFlow<List<ReceiptData>> = repository.allReceipts.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )
    val unsyncedCount: StateFlow<Int> = repository.unsyncedCount.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        0
    )

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

    private val _ramInfo = MutableStateFlow(
        RamInfo(
            totalMb = MemoryOptimizer.getTotalRamMb(application),
            freeMb = MemoryOptimizer.getAvailableRamMb(application),
            isLowRamDevice = MemoryOptimizer.isLowRam(application),
            lowRamEngineActive = true
        )
    )
    val ramInfo: StateFlow<RamInfo> = _ramInfo.asStateFlow()

    private val _customIconBitmap = MutableStateFlow<Bitmap?>(null)
    val customIconBitmap: StateFlow<Bitmap?> = _customIconBitmap.asStateFlow()

    private var cachedLogoBitmap: Bitmap? = null
    private val customLogoFile = java.io.File(application.filesDir, "custom_receipt_logo.png")

    private val _blackpoolSearchQuery = MutableStateFlow("")
    val blackpoolSearchQuery: StateFlow<String> = _blackpoolSearchQuery.asStateFlow()

    private val _selectedBlackpoolArea = MutableStateFlow("All Blackpool")
    val selectedBlackpoolArea: StateFlow<String> = _selectedBlackpoolArea.asStateFlow()

    private val _operatorCapabilities = MutableStateFlow(OperatorCapabilities())
    val operatorCapabilities: StateFlow<OperatorCapabilities> = _operatorCapabilities.asStateFlow()

    private val _activeShiftId = MutableStateFlow<String?>(null)
    val activeShiftId: StateFlow<String?> = _activeShiftId.asStateFlow()

    private val wirelessClient = WirelessFlaskClient()
    private val _wirelessServerUrl = MutableStateFlow(wirelessClient.getBaseUrl())
    val wirelessServerUrl: StateFlow<String> = _wirelessServerUrl.asStateFlow()

    private val _isWirelessOnline = MutableStateFlow(false)
    val isWirelessOnline: StateFlow<Boolean> = _isWirelessOnline.asStateFlow()

    private val _pendingWirelessOrders = MutableStateFlow<List<WirelessOrder>>(emptyList())
    val pendingWirelessOrders: StateFlow<List<WirelessOrder>> = _pendingWirelessOrders.asStateFlow()

    private val _isWirelessSyncing = MutableStateFlow(false)
    val isWirelessSyncing: StateFlow<Boolean> = _isWirelessSyncing.asStateFlow()

    init {
        try {
            cachedLogoBitmap = MemoryOptimizer.decodeSampledBitmap(application, R.drawable.img_naomi_logo, maxDimension = 320)
        } catch (e: Exception) {
            DiagnosticLog.error(application, "PosViewModel/logo", e)
        }

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
                DiagnosticLog.error(application, "PosViewModel/customLogo", e)
            }
        }

        checkWirelessConnection()
    }

    fun setOperator(
        staffId: String,
        displayName: String,
        shiftId: String?,
        isAdmin: Boolean,
        canVoid: Boolean,
        canExport: Boolean,
        canEditVenues: Boolean,
        canChangeGateway: Boolean,
        canViewTotals: Boolean
    ) {
        _operatorCapabilities.value = OperatorCapabilities(
            staffId = staffId,
            displayName = displayName.trim(),
            isAdmin = isAdmin,
            canVoid = canVoid,
            canExport = canExport,
            canEditVenues = canEditVenues,
            canChangeGateway = canChangeGateway,
            canViewTotals = canViewTotals
        )
        _activeShiftId.value = shiftId
        _currentReceipt.value = _currentReceipt.value.copy(
            processedBy = displayName.trim(),
            shiftId = shiftId
        )
    }

    private fun operatorName(): String = _operatorCapabilities.value.displayName.ifBlank { "Unknown Staff" }

    private suspend fun audit(action: String, target: String = "", details: String = "", severity: String = "INFO") {
        val operator = _operatorCapabilities.value
        auditRepository.log(operator.staffId.ifBlank { null }, operatorName(), action, target, details, severity)
    }

    fun logAction(action: String, target: String = "", details: String = "", severity: String = "INFO") {
        viewModelScope.launch { audit(action, target, details, severity) }
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
        refreshRamInfo()
        viewModelScope.launch {
            audit("MEMORY_BUFFERS_CLEARED")
            _uiMessages.emit(UiMessage.Success("Low-RAM print buffers cleared."))
        }
    }

    fun onTrimMemory(level: Int) {
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            printerManager.clearBuffers()
            refreshRamInfo()
        }
    }

    fun setBlackpoolSearch(query: String) { _blackpoolSearchQuery.value = query }
    fun setBlackpoolArea(area: String) { _selectedBlackpoolArea.value = area }

    fun selectBlackpoolBar(bar: BlackpoolBar) {
        _currentReceipt.value = _currentReceipt.value.copy(
            venueName = "${bar.name}, ${bar.address}",
            clientName = bar.name,
            clientContact = bar.contact,
            processedBy = operatorName(),
            shiftId = _activeShiftId.value
        )
        viewModelScope.launch { _uiMessages.emit(UiMessage.Success("Loaded Blackpool Venue: ${bar.name}")) }
    }

    fun selectIconType(type: ReceiptIconType) { _currentReceipt.value = _currentReceipt.value.copy(iconType = type) }

    fun setCustomIcon(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val bmp = MemoryOptimizer.decodeSampledBitmapFromUri(getApplication(), uri, maxDimension = 320)
            if (bmp != null) {
                _customIconBitmap.value?.recycle()
                _customIconBitmap.value = bmp
                try {
                    java.io.FileOutputStream(customLogoFile).use { out -> bmp.compress(Bitmap.CompressFormat.PNG, 95, out) }
                } catch (e: Exception) {
                    DiagnosticLog.error(getApplication(), "PosViewModel/saveLogo", e)
                }
                _currentReceipt.value = _currentReceipt.value.copy(iconType = ReceiptIconType.CUSTOM, customIconUri = customLogoFile.absolutePath)
                _uiMessages.emit(UiMessage.Success("Custom logo uploaded and saved for thermal receipts."))
            } else {
                _uiMessages.emit(UiMessage.Error("Failed to decode image. Please pick another photo."))
            }
        }
    }

    fun removeCustomIcon() {
        _customIconBitmap.value?.recycle()
        _customIconBitmap.value = null
        if (customLogoFile.exists()) customLogoFile.delete()
        _currentReceipt.value = _currentReceipt.value.copy(iconType = ReceiptIconType.NAOMI_LOGO, customIconUri = null)
        viewModelScope.launch { _uiMessages.emit(UiMessage.Success("Custom logo removed. Default brand restored.")) }
    }

    private fun getActiveIconBitmap(receipt: ReceiptData): Bitmap? = when (receipt.iconType) {
        ReceiptIconType.NONE -> null
        ReceiptIconType.NAOMI_LOGO -> cachedLogoBitmap
        ReceiptIconType.VINYL -> MemoryOptimizer.vectorToBitmap(getApplication(), R.drawable.ic_vinyl_record, 256, 256)
        ReceiptIconType.HEADPHONES -> MemoryOptimizer.vectorToBitmap(getApplication(), R.drawable.ic_dj_headphones, 256, 256)
        ReceiptIconType.CROWN_VIP -> MemoryOptimizer.vectorToBitmap(getApplication(), R.drawable.ic_crown_vip, 256, 256)
        ReceiptIconType.STAR -> MemoryOptimizer.vectorToBitmap(getApplication(), R.drawable.ic_stage_star, 256, 256)
        ReceiptIconType.CUSTOM -> _customIconBitmap.value ?: cachedLogoBitmap
    }

    private fun shouldRecycleAfterPrint(bitmap: Bitmap?): Boolean = bitmap != null && bitmap !== cachedLogoBitmap && bitmap !== _customIconBitmap.value

    fun toggleFreeEvent(enabled: Boolean) {
        if (enabled) {
            _currentReceipt.value = _currentReceipt.value.copy(
                isFreeEvent = true,
                packageTier = PackageTier.FREE_ADMISSION,
                paymentMethod = PaymentMethod.FREE_PASS,
                taxPercent = 0.0,
                discountPercent = 0.0,
                items = listOf(ReceiptItem(name = "Free Community Event Pass", quantity = 1, unitPrice = 0.0))
            )
            viewModelScope.launch { _uiMessages.emit(UiMessage.Success("Free Event mode enabled! Admission is £0.00.")) }
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
            viewModelScope.launch { _uiMessages.emit(UiMessage.Success("Standard Paid Gig mode restored (20% UK VAT).")) }
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
        viewModelScope.launch { _uiMessages.emit(UiMessage.Success("Free Blackpool Community Event preset applied!")) }
    }

    fun setTab(tab: PosTab) { _activeTab.value = tab }
    fun selectChannel(channel: PrinterChannel) { _selectedChannel.value = channel }

    fun toggleOfflineSimulation() {
        _isOfflineSimulated.value = !_isOfflineSimulated.value
        val state = if (_isOfflineSimulated.value) "OFFLINE MODE: Transactions will remain buffered locally" else "ONLINE MODE: Gateway sync enabled"
        viewModelScope.launch { _uiMessages.emit(UiMessage.Warning(state)) }
    }

    fun updateClientName(name: String) { _currentReceipt.value = _currentReceipt.value.copy(clientName = name) }
    fun updateClientContact(contact: String) { _currentReceipt.value = _currentReceipt.value.copy(clientContact = contact) }
    fun updateVenue(venue: String) { _currentReceipt.value = _currentReceipt.value.copy(venueName = venue) }
    fun updateGigDate(date: String) { _currentReceipt.value = _currentReceipt.value.copy(gigDate = date) }
    fun updateGigType(type: GigType) { _currentReceipt.value = _currentReceipt.value.copy(gigType = type) }
    fun updatePaymentMethod(method: PaymentMethod) { _currentReceipt.value = _currentReceipt.value.copy(paymentMethod = method) }
    fun updateFooterNotes(notes: String) { _currentReceipt.value = _currentReceipt.value.copy(footerNotes = notes) }

    fun updatePackageTier(tier: PackageTier) {
        val currentItems = _currentReceipt.value.items.toMutableList()
        val tierItem = ReceiptItem(name = "${tier.title} Performance", quantity = 1, unitPrice = tier.basePrice)
        if (currentItems.isNotEmpty()) currentItems[0] = tierItem else currentItems.add(tierItem)
        _currentReceipt.value = _currentReceipt.value.copy(
            packageTier = tier,
            items = currentItems,
            isFreeEvent = tier == PackageTier.FREE_ADMISSION,
            paymentMethod = if (tier == PackageTier.FREE_ADMISSION) PaymentMethod.FREE_PASS else _currentReceipt.value.paymentMethod,
            taxPercent = if (tier == PackageTier.FREE_ADMISSION) 0.0 else _currentReceipt.value.taxPercent
        )
    }

    fun addLineItem(item: ReceiptItem) {
        _currentReceipt.value = _currentReceipt.value.copy(items = _currentReceipt.value.items + item)
    }

    fun removeLineItem(itemId: String) {
        _currentReceipt.value = _currentReceipt.value.copy(items = _currentReceipt.value.items.filterNot { it.id == itemId })
    }

    fun updateTaxPercent(tax: Double) { _currentReceipt.value = _currentReceipt.value.copy(taxPercent = tax.coerceIn(0.0, 100.0)) }

    fun resetNewReceipt() {
        val defaultBar = BlackpoolVenues.ALL_BARS.first()
        _currentReceipt.value = ReceiptData(
            id = generateReceiptId(),
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
            ),
            processedBy = operatorName(),
            shiftId = _activeShiftId.value
        )
    }

    private fun withCurrentAudit(receipt: ReceiptData): ReceiptData = receipt.copy(
        processedBy = operatorName().ifBlank { receipt.processedBy },
        shiftId = _activeShiftId.value ?: receipt.shiftId,
        receiptStatus = if (receipt.isVoided) receipt.receiptStatus else "ACTIVE"
    )

    private suspend fun persistSyncAndPrint(receipt: ReceiptData): PrintResult {
        if (_activeShiftId.value.isNullOrBlank()) {
            return PrintResult.Error("Open a staff shift before finalising or printing a new transaction.", _selectedChannel.value)
        }
        if (receipt.isVoided) return PrintResult.Error("Voided receipts cannot be printed as active transactions.", _selectedChannel.value)

        val auditedReceipt = withCurrentAudit(receipt)
        if (_currentReceipt.value.id == auditedReceipt.id) _currentReceipt.value = auditedReceipt
        val offline = _isOfflineSimulated.value
        repository.saveReceipt(auditedReceipt, isOfflineBuffered = offline)
        audit("RECEIPT_SAVED", auditedReceipt.id, "venue=${auditedReceipt.venueName} total=${auditedReceipt.grandTotal}")

        if (!offline) {
            val uploadedUrl = wirelessClient.broadcastReceiptToWeb(auditedReceipt)
            if (uploadedUrl != null) {
                repository.markSynced(auditedReceipt.id)
                _isWirelessOnline.value = true
            } else {
                repository.markPendingRetry(auditedReceipt.id)
                _isWirelessOnline.value = false
            }
        }

        val iconBitmap = getActiveIconBitmap(auditedReceipt)
        return try {
            val result = printerManager.printReceipt(auditedReceipt, _selectedChannel.value, iconBitmap)
            if (result is PrintResult.Success) {
                repository.markPrinted(auditedReceipt.id, _selectedChannel.value.hardwareCode)
                audit("RECEIPT_PRINTED", auditedReceipt.id, _selectedChannel.value.hardwareCode)
            }
            result
        } catch (e: Exception) {
            DiagnosticLog.error(getApplication(), "ReceiptPrint", e)
            audit("PRINT_ERROR", auditedReceipt.id, e.message.orEmpty(), "ERROR")
            PrintResult.Error(e.message ?: "Unexpected printer error", _selectedChannel.value)
        } finally {
            if (shouldRecycleAfterPrint(iconBitmap) && iconBitmap?.isRecycled == false) iconBitmap.recycle()
        }
    }

    private suspend fun emitPrintResult(result: PrintResult, successPrefix: String? = null) {
        when (result) {
            is PrintResult.Success -> _uiMessages.emit(UiMessage.Success(successPrefix ?: result.message))
            is PrintResult.OutOfPaper -> _uiMessages.emit(UiMessage.Error(result.message))
            is PrintResult.Error -> _uiMessages.emit(UiMessage.Error(result.errorReason))
        }
    }

    fun printCurrentReceipt() {
        viewModelScope.launch { emitPrintResult(persistSyncAndPrint(_currentReceipt.value)) }
    }

    fun reprintReceipt(receipt: ReceiptData) {
        viewModelScope.launch {
            if (receipt.isVoided) {
                _uiMessages.emit(UiMessage.Warning("${receipt.id} is VOID and cannot be reprinted as a valid receipt."))
                return@launch
            }
            val iconBitmap = getActiveIconBitmap(receipt)
            val result = try { printerManager.printReceipt(receipt, _selectedChannel.value, iconBitmap) } finally {
                if (shouldRecycleAfterPrint(iconBitmap) && iconBitmap?.isRecycled == false) iconBitmap.recycle()
            }
            if (result is PrintResult.Success) {
                repository.markPrinted(receipt.id, _selectedChannel.value.hardwareCode)
                audit("RECEIPT_REPRINTED", receipt.id, _selectedChannel.value.hardwareCode)
                _uiMessages.emit(UiMessage.Success("Reprinted ${receipt.id} successfully!"))
            } else emitPrintResult(result)
        }
    }

    fun voidReceipt(receipt: ReceiptData, reason: String) {
        viewModelScope.launch {
            if (!_operatorCapabilities.value.canVoidReceipts) {
                _uiMessages.emit(UiMessage.Error("Your staff account does not have permission to void receipts."))
                return@launch
            }
            if (receipt.isVoided) {
                _uiMessages.emit(UiMessage.Warning("${receipt.id} is already voided."))
                return@launch
            }
            if (reason.trim().length < 3) {
                _uiMessages.emit(UiMessage.Error("Enter a reason before voiding the receipt."))
                return@launch
            }
            if (repository.voidReceipt(receipt.id, reason, operatorName())) {
                audit("RECEIPT_VOIDED", receipt.id, reason.trim(), "WARN")
                _uiMessages.emit(UiMessage.Success("${receipt.id} voided by ${operatorName()}. The audit record was retained."))
            } else _uiMessages.emit(UiMessage.Error("Unable to void ${receipt.id}."))
        }
    }

    fun prepareCorrection(receipt: ReceiptData) {
        val replacement = receipt.copy(
            id = generateReceiptId(),
            createdAt = System.currentTimeMillis(),
            isPrinted = false,
            isBufferedOffline = false,
            receiptStatus = "ACTIVE",
            voidReason = null,
            voidedAt = null,
            voidedBy = null,
            replacesReceiptId = receipt.id,
            processedBy = operatorName(),
            shiftId = _activeShiftId.value
        )
        _currentReceipt.value = replacement
        viewModelScope.launch {
            audit("RECEIPT_CORRECTION_STARTED", replacement.id, "replaces=${receipt.id}")
            _uiMessages.emit(UiMessage.Warning("Correction started. New receipt ${replacement.id} replaces ${receipt.id}."))
        }
    }

    fun exportReceiptsCsv(): String {
        if (!_operatorCapabilities.value.canExportData) {
            viewModelScope.launch { _uiMessages.emit(UiMessage.Error("Your account does not have export permission.")) }
            return ""
        }
        val header = "receipt_id,status,created_at,processed_by,shift_id,client,venue,payment,subtotal,tax,total,synced,void_reason,replaces_receipt\n"
        val rows = allReceipts.value.joinToString("\n") { receipt ->
            listOf(
                receipt.id, receipt.receiptStatus, receipt.formattedDate(), receipt.processedBy, receipt.shiftId.orEmpty(),
                receipt.clientName, receipt.venueName, receipt.paymentMethod.label,
                String.format(Locale.UK, "%.2f", receipt.subtotal), String.format(Locale.UK, "%.2f", receipt.taxAmount),
                String.format(Locale.UK, "%.2f", receipt.grandTotal), (!receipt.isBufferedOffline).toString(),
                receipt.voidReason.orEmpty(), receipt.replacesReceiptId.orEmpty()
            ).joinToString(",") { csvCell(it) }
        }
        return header + rows
    }

    fun exportReceiptsJson(): String {
        if (!_operatorCapabilities.value.canExportData) {
            viewModelScope.launch { _uiMessages.emit(UiMessage.Error("Your account does not have export permission.")) }
            return ""
        }
        val array = JSONArray()
        allReceipts.value.forEach { receipt ->
            val items = JSONArray()
            receipt.items.forEach { item ->
                items.put(JSONObject().apply {
                    put("id", item.id); put("name", item.name); put("quantity", item.quantity); put("unitPrice", item.unitPrice)
                })
            }
            array.put(JSONObject().apply {
                put("id", receipt.id)
                put("status", receipt.receiptStatus)
                put("createdAt", receipt.createdAt)
                put("processedBy", receipt.processedBy)
                put("shiftId", receipt.shiftId)
                put("clientName", receipt.clientName)
                put("clientContact", receipt.clientContact)
                put("venueName", receipt.venueName)
                put("gigType", receipt.gigType.name)
                put("gigDate", receipt.gigDate)
                put("packageTier", receipt.packageTier.name)
                put("paymentMethod", receipt.paymentMethod.name)
                put("footerNotes", receipt.footerNotes)
                put("discountPercent", receipt.discountPercent)
                put("taxPercent", receipt.taxPercent)
                put("isBufferedOffline", receipt.isBufferedOffline)
                put("isPrinted", receipt.isPrinted)
                put("isFreeEvent", receipt.isFreeEvent)
                put("iconType", receipt.iconType.name)
                put("voidReason", receipt.voidReason)
                put("voidedAt", receipt.voidedAt)
                put("voidedBy", receipt.voidedBy)
                put("replacesReceiptId", receipt.replacesReceiptId)
                put("items", items)
            })
        }
        return JSONObject().apply {
            put("exportedAt", System.currentTimeMillis())
            put("format", "naomi-chan-pos-backup-v2")
            put("databaseVersion", AppDatabase.DATABASE_VERSION)
            put("receipts", array)
        }.toString(2)
    }

    fun recordExport(kind: String) {
        val now = System.currentTimeMillis()
        if (kind.equals("JSON", ignoreCase = true)) opsPrefs.edit().putLong("last_backup_at", now).apply()
        viewModelScope.launch { audit("DATA_EXPORTED", kind.uppercase(), "receiptCount=${allReceipts.value.size}") }
    }

    fun restoreBackupJson(jsonText: String) {
        viewModelScope.launch {
            if (!_operatorCapabilities.value.isAdmin) {
                _uiMessages.emit(UiMessage.Error("Only Naomi (Admin) can restore backups."))
                return@launch
            }
            try {
                val root = JSONObject(jsonText)
                if (root.optString("format") != "naomi-chan-pos-backup-v2") {
                    _uiMessages.emit(UiMessage.Error("Unsupported backup format. Create a new v2 JSON backup from this app first."))
                    return@launch
                }
                val array = root.optJSONArray("receipts") ?: JSONArray()
                val receipts = mutableListOf<ReceiptData>()
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val itemArray = obj.optJSONArray("items") ?: JSONArray()
                    val items = mutableListOf<ReceiptItem>()
                    for (j in 0 until itemArray.length()) {
                        val item = itemArray.optJSONObject(j) ?: continue
                        items.add(
                            ReceiptItem(
                                id = item.optString("id").ifBlank { java.util.UUID.randomUUID().toString() },
                                name = item.optString("name", "Service"),
                                quantity = item.optInt("quantity", 1).coerceAtLeast(1),
                                unitPrice = item.optDouble("unitPrice", 0.0)
                            )
                        )
                    }
                    val restoredIcon = runCatching { ReceiptIconType.valueOf(obj.optString("iconType", "NAOMI_LOGO")) }.getOrDefault(ReceiptIconType.NAOMI_LOGO)
                    receipts.add(
                        ReceiptData(
                            id = obj.optString("id").ifBlank { generateReceiptId() },
                            clientName = obj.optString("clientName", "Guest"),
                            clientContact = obj.optString("clientContact", ""),
                            gigType = runCatching { GigType.valueOf(obj.optString("gigType", GigType.CLUB_NIGHT.name)) }.getOrDefault(GigType.CLUB_NIGHT),
                            venueName = obj.optString("venueName", "Blackpool Event"),
                            gigDate = obj.optString("gigDate", ""),
                            packageTier = runCatching { PackageTier.valueOf(obj.optString("packageTier", PackageTier.BAR_ADMISSION.name)) }.getOrDefault(PackageTier.BAR_ADMISSION),
                            items = items,
                            paymentMethod = runCatching { PaymentMethod.valueOf(obj.optString("paymentMethod", PaymentMethod.CARD_TERMINAL.name)) }.getOrDefault(PaymentMethod.CARD_TERMINAL),
                            footerNotes = obj.optString("footerNotes", ""),
                            discountPercent = obj.optDouble("discountPercent", 0.0),
                            taxPercent = obj.optDouble("taxPercent", 20.0),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                            isBufferedOffline = obj.optBoolean("isBufferedOffline", false),
                            isPrinted = obj.optBoolean("isPrinted", false),
                            isFreeEvent = obj.optBoolean("isFreeEvent", false),
                            iconType = if (restoredIcon == ReceiptIconType.CUSTOM) ReceiptIconType.NAOMI_LOGO else restoredIcon,
                            processedBy = obj.optString("processedBy", "Restored Backup"),
                            shiftId = obj.optNullableString("shiftId"),
                            receiptStatus = obj.optString("status", "ACTIVE"),
                            voidReason = obj.optNullableString("voidReason"),
                            voidedAt = obj.optNullableLong("voidedAt"),
                            voidedBy = obj.optNullableString("voidedBy"),
                            replacesReceiptId = obj.optNullableString("replacesReceiptId")
                        )
                    )
                }
                val restored = repository.restoreReceipts(receipts)
                opsPrefs.edit().putLong("last_restore_at", System.currentTimeMillis()).apply()
                audit("BACKUP_RESTORED", "JSON", "restored=$restored supplied=${receipts.size}", "WARN")
                _uiMessages.emit(UiMessage.Success("Backup validated. Restored $restored new ticket(s); existing IDs were left unchanged."))
            } catch (e: Exception) {
                DiagnosticLog.error(getApplication(), "BackupRestore", e)
                audit("BACKUP_RESTORE_FAILED", "JSON", e.message.orEmpty(), "ERROR")
                _uiMessages.emit(UiMessage.Error("Backup restore failed: ${e.message ?: "invalid JSON"}"))
            }
        }
    }

    fun archiveOldTickets(days: Int = 90) {
        viewModelScope.launch {
            if (!_operatorCapabilities.value.isAdmin) {
                _uiMessages.emit(UiMessage.Error("Only Naomi (Admin) can archive tickets."))
                return@launch
            }
            val count = repository.archiveSyncedOlderThan(days)
            audit("TICKETS_ARCHIVED", "older-than-${days}d", "count=$count")
            _uiMessages.emit(UiMessage.Success("Archived $count synced ticket(s) older than $days days."))
        }
    }

    fun wipeAllTickets() {
        viewModelScope.launch {
            if (!_operatorCapabilities.value.isAdmin) {
                _uiMessages.emit(UiMessage.Error("Only Naomi (Admin) can wipe tickets."))
                return@launch
            }
            val before = allReceipts.value.size
            audit("TICKET_WIPE_STARTED", "ALL_RECEIPTS", "countBefore=$before", "WARN")
            val deleted = repository.wipeAllReceipts()
            resetNewReceipt()
            audit("TICKETS_WIPED", "ALL_RECEIPTS", "deleted=$deleted", "WARN")
            _uiMessages.emit(UiMessage.Success("Wiped $deleted receipt/ticket record(s). Staff accounts, shifts, venues and audit history were kept."))
        }
    }

    fun databaseStats(): DatabaseStats {
        val receipts = allReceipts.value
        val dbFile = getApplication<Application>().getDatabasePath("naomi_pos_database")
        return DatabaseStats(
            receiptCount = receipts.size,
            activeCount = receipts.count { it.receiptStatus.equals("ACTIVE", true) },
            voidedCount = receipts.count { it.isVoided },
            archivedCount = receipts.count { it.receiptStatus.equals("ARCHIVED", true) },
            databaseBytes = if (dbFile.exists()) dbFile.length() else 0L,
            oldestReceiptAt = receipts.minOfOrNull { it.createdAt },
            newestReceiptAt = receipts.maxOfOrNull { it.createdAt },
            lastBackupAt = opsPrefs.getLong("last_backup_at", 0L),
            lastRestoreAt = opsPrefs.getLong("last_restore_at", 0L)
        )
    }

    private fun csvCell(value: String): String = "\"${value.replace("\"", "\"\"")}\""

    fun syncAllBufferedTransactions() {
        viewModelScope.launch {
            if (_isOfflineSimulated.value) {
                _uiMessages.emit(UiMessage.Warning("Disable offline mode before synchronizing buffered receipts."))
                return@launch
            }
            _isSyncing.value = true
            try {
                val pending = repository.getUnsyncedReceipts()
                if (pending.isEmpty()) {
                    _uiMessages.emit(UiMessage.Success("No buffered receipts are waiting to sync."))
                    return@launch
                }
                var synced = 0
                var failed = 0
                for (receipt in pending) {
                    if (wirelessClient.broadcastReceiptToWeb(receipt) != null) {
                        repository.markSynced(receipt.id); synced++
                    } else {
                        repository.markPendingRetry(receipt.id); failed++
                    }
                }
                _isWirelessOnline.value = failed == 0
                audit("BUFFER_SYNC", "gateway", "synced=$synced failed=$failed")
                if (failed == 0) _uiMessages.emit(UiMessage.Success("Synchronized $synced buffered receipt(s) with the wireless gateway."))
                else _uiMessages.emit(UiMessage.Warning("Synchronized $synced receipt(s); $failed remain queued for retry."))
            } catch (e: Exception) {
                DiagnosticLog.error(getApplication(), "SyncAll", e)
                audit("SYNC_ERROR", "gateway", e.message.orEmpty(), "ERROR")
                _uiMessages.emit(UiMessage.Error("Sync failed: ${e.message ?: "unknown error"}"))
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun togglePaperRollAlert() { printerManager.togglePaperRoll() }

    fun reloadPaperRoll() {
        printerManager.reloadPaper()
        viewModelScope.launch { _uiMessages.emit(UiMessage.Success("Printer status refreshed after checking/reloading the 58mm paper roll.")) }
    }

    fun setWirelessServerUrl(url: String) {
        if (!_operatorCapabilities.value.canConfigureGateway) {
            viewModelScope.launch { _uiMessages.emit(UiMessage.Error("Your account does not have permission to change the gateway.")) }
            return
        }
        if (wirelessClient.updateBaseUrl(url)) {
            _wirelessServerUrl.value = wirelessClient.getBaseUrl()
            viewModelScope.launch { audit("GATEWAY_CHANGED", _wirelessServerUrl.value) }
            checkWirelessConnection()
        } else {
            viewModelScope.launch { _uiMessages.emit(UiMessage.Error("Invalid wireless gateway URL.")) }
        }
    }

    fun checkWirelessConnection() {
        viewModelScope.launch {
            _isWirelessSyncing.value = true
            try {
                val online = wirelessClient.checkServerStatus()
                _isWirelessOnline.value = online
                _pendingWirelessOrders.value = if (online) wirelessClient.fetchPendingOrders() else emptyList()
            } catch (e: Exception) {
                DiagnosticLog.error(getApplication(), "GatewayStatus", e)
                _isWirelessOnline.value = false
            } finally {
                _isWirelessSyncing.value = false
            }
        }
    }

    fun fetchPendingWirelessOrders() {
        viewModelScope.launch {
            _isWirelessSyncing.value = true
            try {
                if (wirelessClient.checkServerStatus()) {
                    _isWirelessOnline.value = true
                    _pendingWirelessOrders.value = wirelessClient.fetchPendingOrders()
                } else {
                    _isWirelessOnline.value = false
                    _pendingWirelessOrders.value = emptyList()
                }
            } catch (e: Exception) {
                DiagnosticLog.error(getApplication(), "WirelessOrders", e)
            } finally {
                _isWirelessSyncing.value = false
            }
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
                iconType = if (_customIconBitmap.value != null) ReceiptIconType.CUSTOM else ReceiptIconType.NAOMI_LOGO,
                processedBy = operatorName(),
                shiftId = _activeShiftId.value
            )
            _currentReceipt.value = loadedReceipt
            when (val printResult = persistSyncAndPrint(loadedReceipt)) {
                is PrintResult.Success -> {
                    val acknowledged = wirelessClient.markOrderPrinted(order.id)
                    if (acknowledged) {
                        _pendingWirelessOrders.value = wirelessClient.fetchPendingOrders()
                        _uiMessages.emit(UiMessage.Success("Wireless order ${order.id} printed and acknowledged by the gateway."))
                    } else _uiMessages.emit(UiMessage.Warning("Order ${order.id} printed locally, but the gateway status update failed. It remains pending for safety."))
                }
                is PrintResult.OutOfPaper -> _uiMessages.emit(UiMessage.Error("Order ${order.id} was NOT marked printed: ${printResult.message}"))
                is PrintResult.Error -> _uiMessages.emit(UiMessage.Error("Order ${order.id} was NOT marked printed: ${printResult.errorReason}"))
            }
        }
    }

    fun broadcastCurrentReceiptToWeb() {
        viewModelScope.launch {
            if (_activeShiftId.value.isNullOrBlank()) {
                _uiMessages.emit(UiMessage.Error("Open a staff shift before finalising a transaction."))
                return@launch
            }
            val receipt = withCurrentAudit(_currentReceipt.value)
            _isWirelessSyncing.value = true
            try {
                val url = wirelessClient.broadcastReceiptToWeb(receipt)
                if (url != null) {
                    repository.markSynced(receipt.id)
                    _isWirelessOnline.value = true
                    audit("RECEIPT_BROADCAST", receipt.id, url)
                    _uiMessages.emit(UiMessage.Success("Receipt sent to the Web Gateway. View at $url"))
                } else {
                    repository.markPendingRetry(receipt.id)
                    _isWirelessOnline.value = false
                    _uiMessages.emit(UiMessage.Error("Failed to broadcast receipt to wireless server"))
                }
            } catch (e: Exception) {
                DiagnosticLog.error(getApplication(), "BroadcastReceipt", e)
                audit("BROADCAST_ERROR", receipt.id, e.message.orEmpty(), "ERROR")
                _uiMessages.emit(UiMessage.Error("Failed to broadcast receipt: ${e.message ?: "network error"}"))
            } finally {
                _isWirelessSyncing.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        _customIconBitmap.value?.recycle()
        _customIconBitmap.value = null
        if (cachedLogoBitmap?.isRecycled == false) cachedLogoBitmap?.recycle()
        printerManager.cleanup()
    }
}

private fun JSONObject.optNullableString(key: String): String? {
    if (!has(key) || isNull(key)) return null
    return optString(key).takeIf { it.isNotBlank() }
}

private fun JSONObject.optNullableLong(key: String): Long? {
    if (!has(key) || isNull(key)) return null
    return runCatching { getLong(key) }.getOrNull()
}
