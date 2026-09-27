package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.AuditRepository
import com.example.printer.PrintResult
import com.example.printer.UnifiedPrinterManager
import com.example.printer.WarehousePickSlip
import com.example.printer.WarehouseSlipLine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class WarehouseStage(val label: String) {
    PROCESSING("Processing"),
    PACKED("Packed"),
    SHIPPED("Shipped"),
    IN_TRANSIT("In transit"),
    OUT_FOR_DELIVERY("Out for delivery"),
    DELIVERED("Delivered")
}

data class WarehouseManifestLine(
    val id: String,
    val sku: String,
    val name: String,
    val size: String? = null,
    val location: String,
    val quantity: Int,
    val picked: Boolean = false
)

data class WarehouseOrder(
    val reference: String,
    val recipient: String,
    val postcode: String,
    val createdLabel: String,
    val stage: WarehouseStage,
    val carrier: String? = null,
    val trackingNumber: String? = null,
    val manifest: List<WarehouseManifestLine>,
    val holdType: String? = null,
    val holdNote: String? = null,
    val isDemo: Boolean = false
) {
    val allPicked: Boolean get() = manifest.isNotEmpty() && manifest.all { it.picked }
    val isOnHold: Boolean get() = !holdType.isNullOrBlank()
}

data class WarehouseStockItem(
    val sku: String,
    val name: String,
    val variant: String,
    val location: String,
    val quantity: Int,
    val lowStockThreshold: Int
) {
    val isLowStock: Boolean get() = quantity <= lowStockThreshold
}

data class WarehouseReturnCase(
    val reference: String,
    val customer: String,
    val status: String,
    val reason: String,
    val received: Boolean = false
)

enum class WarehouseScanKind {
    ORDER,
    TRACKING,
    STOCK,
    UNKNOWN
}

data class WarehouseScanResult(
    val code: String,
    val kind: WarehouseScanKind,
    val title: String,
    val detail: String
)

data class WarehouseUiState(
    val operatorName: String = "Staff",
    val shiftId: String? = null,
    val orders: List<WarehouseOrder> = emptyList(),
    val stock: List<WarehouseStockItem> = emptyList(),
    val returns: List<WarehouseReturnCase> = emptyList(),
    val lastScan: WarehouseScanResult? = null,
    val message: String? = null
) {
    val pickPackCount: Int
        get() = orders.count { it.stage == WarehouseStage.PROCESSING || it.stage == WarehouseStage.PACKED }

    val dispatchCount: Int
        get() = orders.count {
            it.stage == WarehouseStage.SHIPPED ||
                it.stage == WarehouseStage.IN_TRANSIT ||
                it.stage == WarehouseStage.OUT_FOR_DELIVERY
        }

    val lowStockCount: Int get() = stock.count { it.isLowStock }
    val openHoldCount: Int get() = orders.count { it.isOnHold }
    val activeReturnsCount: Int get() = returns.count { !it.received }
}

class WarehouseViewModel(application: Application) : AndroidViewModel(application) {
    private val auditRepository = AuditRepository(AppDatabase.getDatabase(application).auditDao())
    private val printerManager = UnifiedPrinterManager(application)

    val printerStatus = printerManager.status

    private var operatorId: String? = null
    private var operatorName: String = "Staff"

    private val _state = MutableStateFlow(
        WarehouseUiState(
            orders = seedOrders(),
            stock = seedStock(),
            returns = seedReturns()
        )
    )
    val state: StateFlow<WarehouseUiState> = _state.asStateFlow()

    fun setOperator(staffId: String, displayName: String, shiftId: String?) {
        operatorId = staffId
        operatorName = displayName
        _state.update { it.copy(operatorName = displayName, shiftId = shiftId) }
    }

    fun togglePicked(orderReference: String, lineId: String) {
        _state.update { current ->
            val updated = current.orders.map { order ->
                if (order.reference != orderReference || order.isOnHold) {
                    order
                } else {
                    order.copy(
                        manifest = order.manifest.map { line ->
                            if (line.id == lineId) line.copy(picked = !line.picked) else line
                        }
                    )
                }
            }
            current.copy(orders = updated, message = "Pick manifest updated.")
        }
        log("BFC_PICK_TOGGLE", orderReference, "line=" + lineId)
    }

    fun markPacked(orderReference: String) {
        val order = _state.value.orders.firstOrNull { it.reference == orderReference } ?: return
        when {
            order.isOnHold -> setMessage("Resolve the BFC hold before packing this order.")
            !order.allPicked -> setMessage("Pick every manifest line before marking the parcel packed.")
            else -> {
                updateOrder(orderReference) { it.copy(stage = WarehouseStage.PACKED) }
                setMessage(orderReference + " marked Packed.")
                log("BFC_ORDER_PACKED", orderReference)
            }
        }
    }

    fun markDispatched(orderReference: String) {
        val order = _state.value.orders.firstOrNull { it.reference == orderReference } ?: return
        when {
            order.isOnHold -> setMessage("Resolve the BFC hold before dispatch.")
            order.stage != WarehouseStage.PACKED -> setMessage("The parcel must be Packed before dispatch.")
            else -> {
                updateOrder(orderReference) {
                    it.copy(
                        stage = WarehouseStage.SHIPPED,
                        carrier = it.carrier ?: "Royal Mail",
                        trackingNumber = it.trackingNumber ?: "BFC-PKG-" + orderReference.takeLast(4)
                    )
                }
                setMessage(orderReference + " moved to Shipped.")
                log("BFC_ORDER_DISPATCHED", orderReference)
            }
        }
    }

    fun resolveHold(orderReference: String) {
        val order = _state.value.orders.firstOrNull { it.reference == orderReference } ?: return
        if (!order.isOnHold) {
            setMessage("This order has no active warehouse hold.")
            return
        }
        updateOrder(orderReference) { it.copy(holdType = null, holdNote = null) }
        setMessage("BFC hold resolved for " + orderReference + ".")
        log("BFC_HOLD_RESOLVED", orderReference)
    }

    fun adjustStock(sku: String, delta: Int) {
        if (delta == 0) return
        var applied = false
        _state.update { current ->
            val updated = current.stock.map { item ->
                if (item.sku != sku) {
                    item
                } else {
                    val newQuantity = (item.quantity + delta).coerceAtLeast(0)
                    applied = newQuantity != item.quantity
                    item.copy(quantity = newQuantity)
                }
            }
            current.copy(
                stock = updated,
                message = if (applied) "Stock movement recorded for " + sku + "." else "Stock is already at zero."
            )
        }
        if (applied) log("BFC_STOCK_ADJUST", sku, "delta=" + delta)
    }

    fun receiveReturn(reference: String) {
        var changed = false
        _state.update { current ->
            val updated = current.returns.map { item ->
                if (item.reference == reference && !item.received) {
                    changed = true
                    item.copy(status = "Received at BFC", received = true)
                } else {
                    item
                }
            }
            current.copy(
                returns = updated,
                message = if (changed) reference + " received into BFC returns." else "Return is already received."
            )
        }
        if (changed) log("BFC_RETURN_RECEIVED", reference)
    }

    fun scan(rawCode: String, format: String = "BARCODE") {
        val code = rawCode.trim()
        if (code.isBlank()) return

        val orderByReference = _state.value.orders.firstOrNull {
            it.reference.equals(code, ignoreCase = true)
        }
        val orderByTracking = _state.value.orders.firstOrNull {
            !it.trackingNumber.isNullOrBlank() && it.trackingNumber.equals(code, ignoreCase = true)
        }
        val stockItem = _state.value.stock.firstOrNull {
            it.sku.equals(code, ignoreCase = true)
        }

        val result = when {
            orderByReference != null -> WarehouseScanResult(
                code = code,
                kind = WarehouseScanKind.ORDER,
                title = orderByReference.reference,
                detail = orderByReference.recipient + " · " + orderByReference.stage.label
            )
            orderByTracking != null -> WarehouseScanResult(
                code = code,
                kind = WarehouseScanKind.TRACKING,
                title = orderByTracking.reference,
                detail = (orderByTracking.carrier ?: "Carrier") + " · " + orderByTracking.stage.label
            )
            stockItem != null -> WarehouseScanResult(
                code = code,
                kind = WarehouseScanKind.STOCK,
                title = stockItem.name,
                detail = stockItem.variant + " · " + stockItem.quantity + " in " + stockItem.location
            )
            else -> WarehouseScanResult(
                code = code,
                kind = WarehouseScanKind.UNKNOWN,
                title = "Code not recognised",
                detail = "No local BFC order, tracking number or stock SKU matches this code."
            )
        }

        _state.update { it.copy(lastScan = result, message = null) }
        log("BFC_BARCODE_SCAN", code, "format=" + format + "; kind=" + result.kind.name)
    }

    fun printPickSlip(orderReference: String) {
        val order = _state.value.orders.firstOrNull { it.reference == orderReference }
        if (order == null) {
            setMessage("Warehouse order not found.")
            return
        }

        val document = WarehousePickSlip(
            reference = order.reference,
            recipient = order.recipient,
            postcode = order.postcode,
            stage = order.stage.label,
            operatorName = operatorName,
            lines = order.manifest.map { line ->
                WarehouseSlipLine(
                    quantity = line.quantity,
                    name = line.name,
                    variant = line.size?.let { "Size " + it },
                    sku = line.sku,
                    location = line.location
                )
            }
        )

        viewModelScope.launch {
            val result = printerManager.printWarehousePickSlip(document)
            val message = when (result) {
                is PrintResult.Success -> "Pick slip printed for " + order.reference + "."
                is PrintResult.OutOfPaper -> result.message
                is PrintResult.Error -> result.errorReason
            }
            setMessage(message)
            log(
                action = if (result is PrintResult.Success) "BFC_PICK_SLIP_PRINTED" else "BFC_PICK_SLIP_PRINT_FAILED",
                target = order.reference,
                details = message
            )
        }
    }

    fun clearLastScan() {
        _state.update { it.copy(lastScan = null) }
    }

    fun dismissMessage() {
        _state.update { it.copy(message = null) }
    }

    private fun setMessage(message: String) {
        _state.update { it.copy(message = message) }
    }

    private fun updateOrder(reference: String, transform: (WarehouseOrder) -> WarehouseOrder) {
        _state.update { current ->
            current.copy(
                orders = current.orders.map { order ->
                    if (order.reference == reference) transform(order) else order
                }
            )
        }
    }

    private fun log(action: String, target: String, details: String = "") {
        viewModelScope.launch {
            runCatching {
                auditRepository.log(
                    actorId = operatorId,
                    actorName = operatorName,
                    action = action,
                    target = target,
                    details = details
                )
            }
        }
    }

    override fun onCleared() {
        printerManager.cleanup()
        super.onCleared()
    }

    companion object {
        private fun seedOrders(): List<WarehouseOrder> = listOf(
            WarehouseOrder(
                reference = "NCHN-1042-BKPL",
                recipient = "BFC Demo Customer",
                postcode = "FY1 4BJ",
                createdLabel = "Today · 12:42",
                stage = WarehouseStage.PROCESSING,
                carrier = "Royal Mail",
                trackingNumber = "BFC-PKG-0001",
                manifest = listOf(
                    WarehouseManifestLine(
                        id = "1042-1",
                        sku = "NCHN-TEE-BLK-M",
                        name = "Naomi-Chan Logo Tee",
                        size = "M",
                        location = "A-01-02",
                        quantity = 1
                    ),
                    WarehouseManifestLine(
                        id = "1042-2",
                        sku = "NCHN-STKR-TECH",
                        name = "Technical Support Sticker",
                        location = "C-03-01",
                        quantity = 2
                    )
                ),
                isDemo = true
            ),
            WarehouseOrder(
                reference = "NCHN-1043-BKPL",
                recipient = "Sample Recipient",
                postcode = "FY3 8AA",
                createdLabel = "Today · 13:10",
                stage = WarehouseStage.PROCESSING,
                manifest = listOf(
                    WarehouseManifestLine(
                        id = "1043-1",
                        sku = "NCHN-HOOD-PNK-L",
                        name = "Naomi-Chan Warehouse Hoodie",
                        size = "L",
                        location = "A-02-04",
                        quantity = 1
                    )
                ),
                holdType = "Quality control",
                holdNote = "Check garment print alignment before pick completion."
            ),
            WarehouseOrder(
                reference = "NCHN-1038-BKPL",
                recipient = "Dispatch Demo",
                postcode = "FY4 2QJ",
                createdLabel = "Yesterday · 16:32",
                stage = WarehouseStage.IN_TRANSIT,
                carrier = "Royal Mail",
                trackingNumber = "BFC-PKG-1038",
                manifest = listOf(
                    WarehouseManifestLine(
                        id = "1038-1",
                        sku = "NCHN-MUG-001",
                        name = "Naomi-Chan Ceramic Mug",
                        location = "B-02-01",
                        quantity = 1,
                        picked = true
                    )
                )
            )
        )

        private fun seedStock(): List<WarehouseStockItem> = listOf(
            WarehouseStockItem("NCHN-TEE-BLK-M", "Naomi-Chan Logo Tee", "Black · M", "A-01-02", 12, 4),
            WarehouseStockItem("NCHN-HOOD-PNK-L", "Naomi-Chan Warehouse Hoodie", "Pink · L", "A-02-04", 3, 4),
            WarehouseStockItem("NCHN-MUG-001", "Naomi-Chan Ceramic Mug", "Standard", "B-02-01", 7, 3),
            WarehouseStockItem("NCHN-STKR-TECH", "Technical Support Sticker", "Standard", "C-03-01", 28, 10)
        )

        private fun seedReturns(): List<WarehouseReturnCase> = listOf(
            WarehouseReturnCase(
                reference = "NCHN-1029-BKPL",
                customer = "Returns Demo",
                status = "Authorised · awaiting parcel",
                reason = "Wrong size supplied"
            )
        )
    }
}
