package com.example.printer

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.os.RemoteException
import android.util.Log
import com.example.model.PrinterChannel
import com.example.model.PrinterStatus
import com.example.model.ReceiptData
import com.sunmi.peripheral.printer.InnerPrinterCallback
import com.sunmi.peripheral.printer.InnerPrinterException
import com.sunmi.peripheral.printer.InnerPrinterManager
import com.sunmi.peripheral.printer.SunmiPrinterService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.util.UUID

sealed class PrintResult {
    data class Success(
        val message: String,
        val channel: PrinterChannel,
        val bytesSent: Int
    ) : PrintResult()

    data class Error(
        val errorReason: String,
        val channel: PrinterChannel
    ) : PrintResult()

    data class OutOfPaper(
        val message: String = "SUNMI V2 is out of 58mm thermal paper. Reload the roll and retry."
    ) : PrintResult()
}

data class DiscoveredPrinter(
    val name: String,
    val address: String,
    val channel: PrinterChannel,
    val isBonded: Boolean = true,
    val deviceKey: String = address
)

class UnifiedPrinterManager(private val context: Context) {

    companion object {
        private const val TAG = "NaomiPrinterManager"
        private const val ACTION_USB_PERMISSION = "com.aistudio.naomichan.pos.USB_PERMISSION"
        private const val SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB"

        private const val STATUS_NORMAL = 1
        private const val STATUS_PREPARING = 2
        private const val STATUS_COMMUNICATION_ERROR = 3
        private const val STATUS_OUT_OF_PAPER = 4
        private const val STATUS_OVERHEATED = 5
        private const val STATUS_COVER_OPEN = 6
        private const val STATUS_CUTTER_ERROR = 7
        private const val STATUS_CUTTER_RECOVERED = 8
        private const val STATUS_BLACK_MARK_MISSING = 9
        private const val STATUS_NO_PRINTER = 505
        private const val STATUS_FIRMWARE_FAILED = 507
    }

    private var sunmiPrinterService: SunmiPrinterService? = null
    private var selectedBluetoothAddress: String? = null
    private var selectedUsbDeviceKey: String? = null

    private val _status = MutableStateFlow(
        PrinterStatus(
            deviceName = "SUNMI V2 (T5930) Built-in 58mm",
            lastError = "Connecting to SUNMI print service"
        )
    )
    val status: StateFlow<PrinterStatus> = _status.asStateFlow()

    private val _bluetoothDevices = MutableStateFlow<List<DiscoveredPrinter>>(emptyList())
    val bluetoothDevices: StateFlow<List<DiscoveredPrinter>> = _bluetoothDevices.asStateFlow()

    private val _usbDevices = MutableStateFlow<List<DiscoveredPrinter>>(emptyList())
    val usbDevices: StateFlow<List<DiscoveredPrinter>> = _usbDevices.asStateFlow()

    private val _lastEscPosBytes = MutableStateFlow<ByteArray?>(null)
    val lastEscPosBytes: StateFlow<ByteArray?> = _lastEscPosBytes.asStateFlow()

    private val innerPrinterCallback = object : InnerPrinterCallback() {
        override fun onConnected(service: SunmiPrinterService) {
            sunmiPrinterService = service
            val hasPrinter = try {
                InnerPrinterManager.getInstance().hasPrinter(service)
            } catch (e: InnerPrinterException) {
                Log.e(TAG, "Unable to verify SUNMI built-in printer", e)
                false
            }

            if (hasPrinter) {
                Log.i(TAG, "SUNMI V2 built-in printer service connected")
                refreshSunmiStatus()
            } else {
                _status.value = _status.value.copy(
                    isConnected = false,
                    statusCode = STATUS_NO_PRINTER,
                    lastError = "SUNMI print service connected, but no built-in printer was detected"
                )
            }
        }

        override fun onDisconnected() {
            sunmiPrinterService = null
            _status.value = _status.value.copy(
                isConnected = false,
                isPrinting = false,
                statusCode = null,
                lastError = "SUNMI print service disconnected"
            )
        }
    }

    init {
        bindSunmiService()
        refreshDiscoveredDevices()
    }

    fun bindSunmiService() {
        if (sunmiPrinterService != null) {
            refreshSunmiStatus()
            return
        }

        try {
            val bound = InnerPrinterManager.getInstance().bindService(context, innerPrinterCallback)
            if (!bound) {
                _status.value = _status.value.copy(
                    isConnected = false,
                    statusCode = STATUS_NO_PRINTER,
                    lastError = "Unable to bind the SUNMI V2 printer service"
                )
            }
        } catch (e: InnerPrinterException) {
            Log.e(TAG, "SUNMI service bind failed", e)
            _status.value = _status.value.copy(
                isConnected = false,
                statusCode = STATUS_NO_PRINTER,
                lastError = "SUNMI printer service bind failed: ${e.localizedMessage ?: "unknown error"}"
            )
        }
    }

    fun refreshSunmiStatus() {
        val service = sunmiPrinterService
        if (service == null) {
            _status.value = _status.value.copy(
                isConnected = false,
                lastError = "SUNMI printer service is not connected"
            )
            return
        }

        try {
            val statusCode = service.updatePrinterState()
            val serial = service.getPrinterSerialNo()?.takeIf { it.isNotBlank() } ?: "Unknown"
            val model = service.getPrinterModal()?.takeIf { it.isNotBlank() } ?: "SUNMI V2"
            val paperWidth = runCatching {
                if (service.getPrinterPaper() == 1) 58 else 80
            }.getOrDefault(58)

            _status.value = _status.value.copy(
                channel = PrinterChannel.SUNMI_BUILTIN,
                isConnected = statusCode !in setOf(
                    STATUS_COMMUNICATION_ERROR,
                    STATUS_NO_PRINTER,
                    STATUS_FIRMWARE_FAILED
                ),
                hasPaper = statusCode != STATUS_OUT_OF_PAPER,
                isCoverOpen = statusCode == STATUS_COVER_OPEN,
                isOverheated = statusCode == STATUS_OVERHEATED,
                deviceName = "$model Built-in Thermal",
                serialNumber = serial,
                paperWidthMm = paperWidth,
                statusCode = statusCode,
                lastError = statusMessage(statusCode)
            )
        } catch (e: RemoteException) {
            Log.e(TAG, "SUNMI status query failed", e)
            _status.value = _status.value.copy(
                isConnected = false,
                statusCode = STATUS_COMMUNICATION_ERROR,
                lastError = "Unable to communicate with the SUNMI printer service"
            )
        } catch (e: Exception) {
            Log.e(TAG, "SUNMI status query failed", e)
            _status.value = _status.value.copy(
                isConnected = false,
                lastError = "Unable to read SUNMI printer status: ${e.localizedMessage ?: "unknown error"}"
            )
        }
    }

    private fun statusMessage(code: Int): String? = when (code) {
        STATUS_NORMAL -> null
        STATUS_PREPARING -> "Printer is preparing"
        STATUS_COMMUNICATION_ERROR -> "Printer communication error"
        STATUS_OUT_OF_PAPER -> "OUT OF PAPER — reload the 58mm roll"
        STATUS_OVERHEATED -> "Printer is overheated — allow it to cool before printing"
        STATUS_COVER_OPEN -> "Printer cover is open"
        STATUS_CUTTER_ERROR -> "Printer reported cutter error"
        STATUS_CUTTER_RECOVERED -> null
        STATUS_BLACK_MARK_MISSING -> "Black-mark paper marker was not detected"
        STATUS_NO_PRINTER -> "No built-in SUNMI printer detected"
        STATUS_FIRMWARE_FAILED -> "Printer firmware update failed"
        else -> "SUNMI printer status code $code"
    }

    fun selectDiscoveredPrinter(device: DiscoveredPrinter) {
        when (device.channel) {
            PrinterChannel.BLUETOOTH -> selectedBluetoothAddress = device.deviceKey
            PrinterChannel.USB_OTG -> selectedUsbDeviceKey = device.deviceKey
            PrinterChannel.SUNMI_BUILTIN -> Unit
        }
    }

    @SuppressLint("MissingPermission")
    fun refreshDiscoveredDevices() {
        try {
            val adapter = BluetoothAdapter.getDefaultAdapter()
            val devices = if (adapter != null && adapter.isEnabled) {
                adapter.bondedDevices.orEmpty().map { device ->
                    DiscoveredPrinter(
                        name = device.name ?: "Unknown Bluetooth Device",
                        address = device.address,
                        channel = PrinterChannel.BLUETOOTH,
                        deviceKey = device.address
                    )
                }
            } else {
                emptyList()
            }
            _bluetoothDevices.value = devices
            if (selectedBluetoothAddress !in devices.map { it.deviceKey }) {
                selectedBluetoothAddress = devices.singleOrNull()?.deviceKey
            }
        } catch (e: Exception) {
            _bluetoothDevices.value = emptyList()
            selectedBluetoothAddress = null
            Log.w(TAG, "Bluetooth discovery failed: ${e.message}")
        }

        try {
            val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager
            val devices = usbManager?.deviceList?.values?.mapNotNull { device ->
                val printerClass = (0 until device.interfaceCount).any { index ->
                    device.getInterface(index).interfaceClass == UsbConstants.USB_CLASS_PRINTER
                }
                if (!printerClass) {
                    null
                } else {
                    DiscoveredPrinter(
                        name = device.productName ?: "USB Thermal Printer (${device.vendorId}:${device.productId})",
                        address = "VID_${device.vendorId}_PID_${device.productId}",
                        channel = PrinterChannel.USB_OTG,
                        isBonded = usbManager.hasPermission(device),
                        deviceKey = device.deviceName
                    )
                }
            }.orEmpty()

            _usbDevices.value = devices
            if (selectedUsbDeviceKey !in devices.map { it.deviceKey }) {
                selectedUsbDeviceKey = devices.singleOrNull()?.deviceKey
            }
        } catch (e: Exception) {
            _usbDevices.value = emptyList()
            selectedUsbDeviceKey = null
            Log.w(TAG, "USB discovery failed: ${e.message}")
        }
    }

    suspend fun printReceipt(
        receipt: ReceiptData,
        channel: PrinterChannel,
        logoBitmap: Bitmap?
    ): PrintResult = withContext(Dispatchers.IO) {
        _status.value = _status.value.copy(isPrinting = true)
        val bytes = EscPosBuilder(totalColumns = 32).assembleNaomiReceipt(receipt, logoBitmap)
        _lastEscPosBytes.value = bytes

        try {
            when (channel) {
                PrinterChannel.SUNMI_BUILTIN -> printViaSunmi(bytes)
                PrinterChannel.BLUETOOTH -> printViaBluetooth(bytes)
                PrinterChannel.USB_OTG -> printViaUsb(bytes)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unhandled print error", e)
            PrintResult.Error(e.localizedMessage ?: "Unexpected printer error", channel)
        } finally {
            _status.value = _status.value.copy(isPrinting = false)
        }
    }

    private fun printViaSunmi(data: ByteArray): PrintResult {
        val service = sunmiPrinterService
            ?: return PrintResult.Error(
                "SUNMI printer service is not connected. No receipt was printed.",
                PrinterChannel.SUNMI_BUILTIN
            )

        refreshSunmiStatus()
        val before = _status.value
        if (!before.isConnected) {
            return PrintResult.Error(before.lastError ?: "SUNMI V2 printer is unavailable", PrinterChannel.SUNMI_BUILTIN)
        }
        if (!before.hasPaper) return PrintResult.OutOfPaper()
        if (before.isCoverOpen) {
            return PrintResult.Error("Close the SUNMI V2 printer cover before printing.", PrinterChannel.SUNMI_BUILTIN)
        }
        if (before.isOverheated) {
            return PrintResult.Error("SUNMI V2 printer is overheated. Allow it to cool before retrying.", PrinterChannel.SUNMI_BUILTIN)
        }

        return try {
            service.printerInit(null)
            service.sendRAWData(data, null)
            refreshSunmiStatus()
            val after = _status.value

            when {
                !after.isConnected -> PrintResult.Error(
                    after.lastError ?: "SUNMI printer communication failed after dispatch",
                    PrinterChannel.SUNMI_BUILTIN
                )
                !after.hasPaper -> PrintResult.OutOfPaper()
                after.isCoverOpen -> PrintResult.Error("SUNMI V2 printer cover is open.", PrinterChannel.SUNMI_BUILTIN)
                after.isOverheated -> PrintResult.Error("SUNMI V2 printer overheated while printing.", PrinterChannel.SUNMI_BUILTIN)
                else -> PrintResult.Success(
                    "Receipt dispatched to SUNMI V2 built-in 58mm printer",
                    PrinterChannel.SUNMI_BUILTIN,
                    data.size
                )
            }
        } catch (e: RemoteException) {
            Log.e(TAG, "SUNMI V2 print failed", e)
            PrintResult.Error(
                "SUNMI V2 printing failed: ${e.localizedMessage ?: "printer service error"}",
                PrinterChannel.SUNMI_BUILTIN
            )
        }
    }

    fun feedPaper(lines: Int = 3) {
        val service = sunmiPrinterService ?: return
        try {
            service.lineWrap(lines.coerceIn(1, 20), null)
            refreshSunmiStatus()
        } catch (e: RemoteException) {
            Log.e(TAG, "SUNMI paper feed failed", e)
            _status.value = _status.value.copy(
                lastError = "Paper feed failed: ${e.localizedMessage ?: "printer error"}"
            )
        }
    }

    // Kept for existing ViewModel calls. These no longer simulate physical hardware state;
    // they simply refresh the real SUNMI V2 printer status.
    fun togglePaperRoll() = refreshSunmiStatus()
    fun reloadPaper() = refreshSunmiStatus()

    @SuppressLint("MissingPermission")
    private fun printViaBluetooth(data: ByteArray): PrintResult {
        val adapter = BluetoothAdapter.getDefaultAdapter()
            ?: return PrintResult.Error("Bluetooth hardware is unavailable", PrinterChannel.BLUETOOTH)
        if (!adapter.isEnabled) {
            return PrintResult.Error("Bluetooth is turned off", PrinterChannel.BLUETOOTH)
        }

        val bonded = try {
            adapter.bondedDevices.orEmpty()
        } catch (_: SecurityException) {
            return PrintResult.Error("Bluetooth permission is required", PrinterChannel.BLUETOOTH)
        }

        val device = when {
            selectedBluetoothAddress != null -> bonded.firstOrNull { it.address == selectedBluetoothAddress }
            bonded.size == 1 -> bonded.first()
            else -> null
        } ?: return PrintResult.Error(
            if (bonded.isEmpty()) "No paired Bluetooth printer is available" else "Select a Bluetooth printer first",
            PrinterChannel.BLUETOOTH
        )

        var socket: BluetoothSocket? = null
        return try {
            socket = device.createRfcommSocketToServiceRecord(UUID.fromString(SPP_UUID))
            adapter.cancelDiscovery()
            socket.connect()
            val output: OutputStream = socket.outputStream
            output.write(data)
            output.flush()
            PrintResult.Success(
                "Printed to Bluetooth device '${device.name ?: device.address}'",
                PrinterChannel.BLUETOOTH,
                data.size
            )
        } catch (e: Exception) {
            PrintResult.Error(
                "Bluetooth printing failed: ${e.localizedMessage ?: "connection error"}",
                PrinterChannel.BLUETOOTH
            )
        } finally {
            runCatching { socket?.close() }
        }
    }

    private fun printViaUsb(data: ByteArray): PrintResult {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager
            ?: return PrintResult.Error("USB Host service unavailable", PrinterChannel.USB_OTG)

        val candidates = usbManager.deviceList.values.filter { device ->
            (0 until device.interfaceCount).any { index ->
                device.getInterface(index).interfaceClass == UsbConstants.USB_CLASS_PRINTER
            }
        }

        val device = when {
            selectedUsbDeviceKey != null -> candidates.firstOrNull { it.deviceName == selectedUsbDeviceKey }
            candidates.size == 1 -> candidates.first()
            else -> null
        } ?: return PrintResult.Error(
            if (candidates.isEmpty()) "No USB printer is connected" else "Select a USB printer first",
            PrinterChannel.USB_OTG
        )

        if (!usbManager.hasPermission(device)) {
            val permissionIntent = PendingIntent.getBroadcast(
                context,
                device.deviceId,
                Intent(ACTION_USB_PERMISSION).setPackage(context.packageName),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            usbManager.requestPermission(device, permissionIntent)
            return PrintResult.Error("USB permission requested. Approve it, then retry.", PrinterChannel.USB_OTG)
        }

        var printerInterface: UsbInterface? = null
        var outputEndpoint: UsbEndpoint? = null
        for (i in 0 until device.interfaceCount) {
            val iface = device.getInterface(i)
            if (iface.interfaceClass != UsbConstants.USB_CLASS_PRINTER) continue
            for (j in 0 until iface.endpointCount) {
                val endpoint = iface.getEndpoint(j)
                if (endpoint.type == UsbConstants.USB_ENDPOINT_XFER_BULK &&
                    endpoint.direction == UsbConstants.USB_DIR_OUT
                ) {
                    printerInterface = iface
                    outputEndpoint = endpoint
                    break
                }
            }
            if (outputEndpoint != null) break
        }

        val iface = printerInterface
            ?: return PrintResult.Error("USB printer has no compatible interface", PrinterChannel.USB_OTG)
        val endpoint = outputEndpoint
            ?: return PrintResult.Error("USB printer has no bulk OUT endpoint", PrinterChannel.USB_OTG)

        var connection: UsbDeviceConnection? = null
        return try {
            val active = usbManager.openDevice(device)
                ?: return PrintResult.Error("Unable to open USB printer", PrinterChannel.USB_OTG)
            connection = active
            if (!active.claimInterface(iface, true)) {
                return PrintResult.Error("Unable to claim USB printer interface", PrinterChannel.USB_OTG)
            }
            val transferred = active.bulkTransfer(endpoint, data, data.size, 5000)
            if (transferred <= 0) {
                PrintResult.Error("USB transfer failed ($transferred bytes)", PrinterChannel.USB_OTG)
            } else {
                PrintResult.Success("Transferred $transferred bytes to USB printer", PrinterChannel.USB_OTG, transferred)
            }
        } catch (e: Exception) {
            PrintResult.Error(
                "USB printing failed: ${e.localizedMessage ?: "unknown error"}",
                PrinterChannel.USB_OTG
            )
        } finally {
            runCatching { connection?.releaseInterface(iface) }
            runCatching { connection?.close() }
        }
    }

    fun cleanup() {
        try {
            InnerPrinterManager.getInstance().unBindService(context, innerPrinterCallback)
        } catch (e: InnerPrinterException) {
            Log.w(TAG, "SUNMI service unbind failed: ${e.message}")
        } catch (e: Exception) {
            Log.w(TAG, "Unexpected SUNMI unbind error: ${e.message}")
        }
        sunmiPrinterService = null
        clearBuffers()
    }

    fun clearBuffers() {
        _lastEscPosBytes.value = null
    }
}
