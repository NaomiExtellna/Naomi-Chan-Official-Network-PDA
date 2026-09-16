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

        private const val SUNMI_STATUS_NORMAL = 1
        private const val SUNMI_STATUS_PREPARING = 2
        private const val SUNMI_STATUS_COMMUNICATION_ERROR = 3
        private const val SUNMI_STATUS_OUT_OF_PAPER = 4
        private const val SUNMI_STATUS_OVERHEATED = 5
        private const val SUNMI_STATUS_COVER_OPEN = 6
        private const val SUNMI_STATUS_CUTTER_ERROR = 7
        private const val SUNMI_STATUS_CUTTER_RECOVERED = 8
        private const val SUNMI_STATUS_BLACK_MARK_MISSING = 9
        private const val SUNMI_STATUS_NO_PRINTER = 505
        private const val SUNMI_STATUS_FIRMWARE_FAILED = 507
    }

    private var sunmiPrinterService: SunmiPrinterService? = null
    private var selectedBluetoothAddress: String? = null
    private var selectedUsbDeviceKey: String? = null

    private val _status = MutableStateFlow(
        PrinterStatus(
            channel = PrinterChannel.SUNMI_BUILTIN,
            isConnected = false,
            isPrinting = false,
            hasPaper = true,
            deviceName = "SUNMI V2 (T5930) Built-in 58mm",
            serialNumber = "Unknown",
            paperWidthMm = 58,
            statusCode = null,
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
            Log.i(TAG, "SUNMI printer service connected")

            val hasPrinter = try {
                InnerPrinterManager.getInstance().hasPrinter(service)
            } catch (e: InnerPrinterException) {
                Log.e(TAG, "Unable to verify SUNMI built-in printer", e)
                false
            }

            if (!hasPrinter) {
                _status.value = _status.value.copy(
                    isConnected = false,
                    statusCode = SUNMI_STATUS_NO_PRINTER,
                    lastError = "SUNMI print service connected, but no built-in printer was detected"
                )
                return
            }

            refreshSunmiStatus()
        }

        override fun onDisconnected() {
            sunmiPrinterService = null
            _status.value = _status.value.copy(
                isConnected = false,
                isPrinting = false,
                statusCode = null,
                lastError = "SUNMI print service disconnected"
            )
            Log.w(TAG, "SUNMI printer service disconnected")
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
                    statusCode = SUNMI_STATUS_NO_PRINTER,
                    lastError = "Unable to bind the SUNMI V2 built-in printer service"
                )
            }
        } catch (e: InnerPrinterException) {
            Log.e(TAG, "SUNMI printer service bind failed", e)
            _status.value = _status.value.copy(
                isConnected = false,
                statusCode = SUNMI_STATUS_NO_PRINTER,
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
            val serial = service.printerSerialNo?.takeIf { it.isNotBlank() } ?: "Unknown"
            val model = service.printerModal?.takeIf { it.isNotBlank() } ?: "SUNMI V2"
            val paperWidth = runCatching {
                if (service.printerPaper == 1) 58 else 80
            }.getOrDefault(58)

            val connected = statusCode !in setOf(
                SUNMI_STATUS_COMMUNICATION_ERROR,
                SUNMI_STATUS_NO_PRINTER,
                SUNMI_STATUS_FIRMWARE_FAILED
            )
            val hasPaper = statusCode != SUNMI_STATUS_OUT_OF_PAPER
            val coverOpen = statusCode == SUNMI_STATUS_COVER_OPEN
            val overheated = statusCode == SUNMI_STATUS_OVERHEATED

            _status.value = _status.value.copy(
                channel = PrinterChannel.SUNMI_BUILTIN,
                isConnected = connected,
                hasPaper = hasPaper,
                isCoverOpen = coverOpen,
                isOverheated = overheated,
                deviceName = "$model Built-in Thermal",
                serialNumber = serial,
                paperWidthMm = paperWidth,
                statusCode = statusCode,
                lastError = sunmiStatusMessage(statusCode)
            )
        } catch (e: RemoteException) {
            Log.e(TAG, "Unable to query SUNMI printer status", e)
            _status.value = _status.value.copy(
                isConnected = false,
                statusCode = SUNMI_STATUS_COMMUNICATION_ERROR,
                lastError = "Unable to communicate with the SUNMI printer service"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected SUNMI status error", e)
            _status.value = _status.value.copy(
                isConnected = false,
                lastError = "Unable to read SUNMI printer status: ${e.localizedMessage ?: "unknown error"}"
            )
        }
    }

    private fun sunmiStatusMessage(statusCode: Int): String? = when (statusCode) {
        SUNMI_STATUS_NORMAL -> null
        SUNMI_STATUS_PREPARING -> "Printer is preparing"
        SUNMI_STATUS_COMMUNICATION_ERROR -> "Printer communication error"
        SUNMI_STATUS_OUT_OF_PAPER -> "OUT OF PAPER — reload the 58mm roll"
        SUNMI_STATUS_OVERHEATED -> "Printer is overheated — allow it to cool before printing"
        SUNMI_STATUS_COVER_OPEN -> "Printer cover is open"
        SUNMI_STATUS_CUTTER_ERROR -> "Printer reported cutter error"
        SUNMI_STATUS_CUTTER_RECOVERED -> null
        SUNMI_STATUS_BLACK_MARK_MISSING -> "Black-mark paper marker was not detected"
        SUNMI_STATUS_NO_PRINTER -> "No built-in SUNMI printer detected"
        SUNMI_STATUS_FIRMWARE_FAILED -> "Printer firmware update failed"
        else -> "SUNMI printer status code $statusCode"
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
                        isBonded = true,
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
        } catch (e: SecurityException) {
            _bluetoothDevices.value = emptyList()
            selectedBluetoothAddress = null
            Log.w(TAG, "Bluetooth permission missing: ${e.message}")
        } catch (e: Exception) {
            _bluetoothDevices.value = emptyList()
            selectedBluetoothAddress = null
            Log.w(TAG, "Bluetooth discovery error: ${e.message}")
        }

        try {
            val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager
            val devices = if (usbManager == null) {
                emptyList()
            } else {
                usbManager.deviceList.values.mapNotNull { device ->
                    val isPrinterClass = (0 until device.interfaceCount).any { index ->
                        device.getInterface(index).interfaceClass == UsbConstants.USB_CLASS_PRINTER
                    }
                    if (!isPrinterClass) {
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
                }
            }
            _usbDevices.value = devices
            if (selectedUsbDeviceKey !in devices.map { it.deviceKey }) {
                selectedUsbDeviceKey = devices.singleOrNull()?.deviceKey
            }
        } catch (e: Exception) {
            _usbDevices.value = emptyList()
            selectedUsbDeviceKey = null
            Log.w(TAG, "USB detection error: ${e.message}")
        }
    }

    suspend fun printReceipt(
        receipt: ReceiptData,
        channel: PrinterChannel,
        logoBitmap: Bitmap?
    ): PrintResult = withContext(Dispatchers.IO) {
        _status.value = _status.value.copy(isPrinting = true)

        val escPosData = EscPosBuilder(totalColumns = 32).assembleNaomiReceipt(receipt, logoBitmap)
        _lastEscPosBytes.value = escPosData

        val result = try {
            when (channel) {
                PrinterChannel.SUNMI_BUILTIN -> printViaSunmi(escPosData)
                PrinterChannel.BLUETOOTH -> printViaBluetooth(escPosData)
                PrinterChannel.USB_OTG -> printViaUsb(escPosData)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unhandled print error", e)
            PrintResult.Error(e.localizedMessage ?: "Unexpected printer error", channel)
        } finally {
            _status.value = _status.value.copy(isPrinting = false)
        }

        result
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
            return PrintResult.Error(
                before.lastError ?: "SUNMI V2 printer is unavailable",
                PrinterChannel.SUNMI_BUILTIN
            )
        }
        if (!before.hasPaper) {
            return PrintResult.OutOfPaper()
        }
        if (before.isCoverOpen) {
            return PrintResult.Error("Close the SUNMI V2 printer cover before printing.", PrinterChannel.SUNMI_BUILTIN)
        }
        if (before.isOverheated) {
            return PrintResult.Error("SUNMI V2 printer is overheated. Allow it to cool before retrying.", PrinterChannel.SUNMI_BUILTIN)
        }

        return try {
            service.printerInit(null)
            service.sendRAWData(data, null)

            // Query the built-in printer immediately after dispatch. Do not report a successful
            // transaction if the service is already reporting a hardware fault.
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
        } catch (e: Exception) {
            Log.e(TAG, "SUNMI V2 print failed", e)
            PrintResult.Error(
                "SUNMI V2 printing failed: ${e.localizedMessage ?: "unknown error"}",
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
            _status.value = _status.value.copy(lastError = "Paper feed failed: ${e.localizedMessage ?: "printer error"}")
        }
    }

    @SuppressLint("MissingPermission")
    private fun printViaBluetooth(data: ByteArray): PrintResult {
        val adapter = BluetoothAdapter.getDefaultAdapter()
            ?: return PrintResult.Error("Bluetooth hardware is not available on this terminal", PrinterChannel.BLUETOOTH)

        if (!adapter.isEnabled) {
            return PrintResult.Error("Bluetooth is turned off. Enable Bluetooth before printing.", PrinterChannel.BLUETOOTH)
        }

        val bonded = try {
            adapter.bondedDevices.orEmpty()
        } catch (_: SecurityException) {
            return PrintResult.Error(
                "Bluetooth permission is required before a paired printer can be used.",
                PrinterChannel.BLUETOOTH
            )
        }

        val targetDevice = when {
            selectedBluetoothAddress != null -> bonded.firstOrNull { it.address == selectedBluetoothAddress }
            bonded.size == 1 -> bonded.first()
            else -> null
        } ?: return PrintResult.Error(
            if (bonded.isEmpty()) {
                "No paired Bluetooth printer is available. Pair a printer first."
            } else {
                "Multiple Bluetooth devices are paired. Select the intended printer first."
            },
            PrinterChannel.BLUETOOTH
        )

        val sppUuid = UUID.fromString(SPP_UUID)
        var socket: BluetoothSocket? = null
        return try {
            socket = targetDevice.createRfcommSocketToServiceRecord(sppUuid)
            adapter.cancelDiscovery()
            socket.connect()
            val output: OutputStream = socket.outputStream
            output.write(data)
            output.flush()
            PrintResult.Success(
                "Printed successfully to Bluetooth device '${targetDevice.name ?: targetDevice.address}'",
                PrinterChannel.BLUETOOTH,
                data.size
            )
        } catch (e: Exception) {
            Log.e(TAG, "Bluetooth print failed", e)
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
            ?: return PrintResult.Error("USB Host system service not available", PrinterChannel.USB_OTG)

        val candidates = usbManager.deviceList.values.filter { candidate ->
            (0 until candidate.interfaceCount).any { index ->
                candidate.getInterface(index).interfaceClass == UsbConstants.USB_CLASS_PRINTER
            }
        }

        val device = when {
            selectedUsbDeviceKey != null -> candidates.firstOrNull { it.deviceName == selectedUsbDeviceKey }
            candidates.size == 1 -> candidates.first()
            else -> null
        } ?: return PrintResult.Error(
            if (candidates.isEmpty()) {
                "No USB printer-class device is connected."
            } else {
                "Multiple USB printers are connected. Select the intended printer first."
            },
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
            return PrintResult.Error(
                "USB permission requested for ${device.productName ?: "the thermal printer"}. Approve it, then print again.",
                PrinterChannel.USB_OTG
            )
        }

        var targetInterface: UsbInterface? = null
        var targetEndpoint: UsbEndpoint? = null

        for (i in 0 until device.interfaceCount) {
            val iface = device.getInterface(i)
            if (iface.interfaceClass != UsbConstants.USB_CLASS_PRINTER) continue
            for (j in 0 until iface.endpointCount) {
                val endpoint = iface.getEndpoint(j)
                if (endpoint.type == UsbConstants.USB_ENDPOINT_XFER_BULK &&
                    endpoint.direction == UsbConstants.USB_DIR_OUT
                ) {
                    targetInterface = iface
                    targetEndpoint = endpoint
                    break
                }
            }
            if (targetEndpoint != null) break
        }

        val printerInterface = targetInterface
            ?: return PrintResult.Error("USB printer has no compatible printer interface.", PrinterChannel.USB_OTG)
        val outputEndpoint = targetEndpoint
            ?: return PrintResult.Error("USB printer has no bulk OUT endpoint.", PrinterChannel.USB_OTG)

        var connection: UsbDeviceConnection? = null
        return try {
            val activeConnection = usbManager.openDevice(device)
                ?: return PrintResult.Error(
                    "Unable to open ${device.productName ?: "the USB thermal printer"}.",
                    PrinterChannel.USB_OTG
                )
            connection = activeConnection

            if (!activeConnection.claimInterface(printerInterface, true)) {
                return PrintResult.Error("Unable to claim the USB printer interface.", PrinterChannel.USB_OTG)
            }

            val transferred = activeConnection.bulkTransfer(outputEndpoint, data, data.size, 5000)
            if (transferred <= 0) {
                PrintResult.Error("USB printer transfer failed ($transferred bytes).", PrinterChannel.USB_OTG)
            } else {
                PrintResult.Success(
                    "Transferred $transferred bytes to USB thermal printer",
                    PrinterChannel.USB_OTG,
                    transferred
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "USB print failed", e)
            PrintResult.Error(
                "USB transmission failed: ${e.localizedMessage ?: "unknown error"}",
                PrinterChannel.USB_OTG
            )
        } finally {
            runCatching { connection?.releaseInterface(printerInterface) }
            runCatching { connection?.close() }
        }
    }

    fun cleanup() {
        try {
            InnerPrinterManager.getInstance().unBindService(context, innerPrinterCallback)
        } catch (e: InnerPrinterException) {
            Log.w(TAG, "SUNMI printer service unbind failed: ${e.message}")
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
