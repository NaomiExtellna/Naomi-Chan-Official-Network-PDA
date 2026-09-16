package com.example.printer

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.os.IBinder
import android.util.Log
import com.example.model.PrinterChannel
import com.example.model.PrinterStatus
import com.example.model.ReceiptData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import woyou.aidlservice.jiuiv5.ICallback
import woyou.aidlservice.jiuiv5.IWoyouService
import java.io.OutputStream
import java.util.UUID

sealed class PrintResult {
    data class Success(val message: String, val channel: PrinterChannel, val bytesSent: Int) : PrintResult()
    data class Error(val errorReason: String, val channel: PrinterChannel) : PrintResult()
    data class OutOfPaper(val message: String = "Thermal printer roll is empty or missing! Please reload 58mm paper.") : PrintResult()
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
        private const val ACTION_USB_PERMISSION = "com.aistudio.naomichan.pos.USB_PERMISSION"
        private const val SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB"
    }

    private val tag = "NaomiPrinterManager"

    private var woyouService: IWoyouService? = null
    private var isSunmiServiceBound = false
    private var selectedBluetoothAddress: String? = null
    private var selectedUsbDeviceKey: String? = null

    private val _status = MutableStateFlow(
        PrinterStatus(
            channel = PrinterChannel.SUNMI_BUILTIN,
            isConnected = false,
            isPrinting = false,
            hasPaper = true,
            deviceName = "Sunmi V2 Inner Thermal (58mm)",
            serialNumber = "Unknown",
            paperRollRemainingPercent = 0,
            lastError = "Waiting for Sunmi printer service"
        )
    )
    val status: StateFlow<PrinterStatus> = _status.asStateFlow()

    private val _bluetoothDevices = MutableStateFlow<List<DiscoveredPrinter>>(emptyList())
    val bluetoothDevices: StateFlow<List<DiscoveredPrinter>> = _bluetoothDevices.asStateFlow()

    private val _usbDevices = MutableStateFlow<List<DiscoveredPrinter>>(emptyList())
    val usbDevices: StateFlow<List<DiscoveredPrinter>> = _usbDevices.asStateFlow()

    private val _lastEscPosBytes = MutableStateFlow<ByteArray?>(null)
    val lastEscPosBytes: StateFlow<ByteArray?> = _lastEscPosBytes.asStateFlow()

    private val sunmiConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            woyouService = IWoyouService.Stub.asInterface(service)
            isSunmiServiceBound = true
            Log.d(tag, "Sunmi inner printer AIDL service connected successfully.")
            updateSunmiStatus()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            woyouService = null
            isSunmiServiceBound = false
            Log.w(tag, "Sunmi inner printer AIDL service disconnected.")
            _status.value = _status.value.copy(
                isConnected = false,
                paperRollRemainingPercent = 0,
                lastError = "Sunmi service disconnected"
            )
        }
    }

    init {
        bindSunmiService()
        refreshDiscoveredDevices()
    }

    fun bindSunmiService() {
        val intent = Intent().apply {
            `package` = "woyou.aidlservice.jiuiv5"
            action = "woyou.aidlservice.jiuiv5.IWoyouService"
        }
        try {
            val bound = context.bindService(intent, sunmiConnection, Context.BIND_AUTO_CREATE)
            if (!bound) {
                Log.w(tag, "Native Sunmi hardware service not found.")
                _status.value = _status.value.copy(
                    isConnected = false,
                    deviceName = "Sunmi V2 Inner Thermal (not detected)",
                    serialNumber = "Unknown",
                    paperRollRemainingPercent = 0,
                    lastError = "Sunmi printer service not available"
                )
            }
        } catch (e: Exception) {
            Log.e(tag, "Error binding Sunmi service", e)
            _status.value = _status.value.copy(
                isConnected = false,
                deviceName = "Sunmi V2 Inner Thermal (unavailable)",
                serialNumber = "Unknown",
                paperRollRemainingPercent = 0,
                lastError = "Sunmi service bind failed: ${e.localizedMessage ?: "unknown error"}"
            )
        }
    }

    private fun updateSunmiStatus() {
        val service = woyouService ?: return
        try {
            val statusCode = service.printerStatus
            val serial = service.printerSerialNo ?: "Unknown"
            val model = service.printerModal ?: "Sunmi V2"
            val hasPaper = statusCode != 4
            val overheated = statusCode == 5
            _status.value = _status.value.copy(
                channel = PrinterChannel.SUNMI_BUILTIN,
                isConnected = true,
                hasPaper = hasPaper,
                isOverheated = overheated,
                deviceName = "$model Thermal Printer",
                serialNumber = serial,
                paperRollRemainingPercent = if (hasPaper) 88 else 0,
                lastError = when {
                    !hasPaper -> "OUT_OF_PAPER"
                    overheated -> "PRINTER_OVERHEATED"
                    else -> null
                }
            )
        } catch (e: Exception) {
            Log.e(tag, "Failed to query Sunmi printer status", e)
            _status.value = _status.value.copy(
                isConnected = false,
                lastError = "Unable to query Sunmi printer status"
            )
        }
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
            Log.w(tag, "Bluetooth permission missing: ${e.message}")
        } catch (e: Exception) {
            _bluetoothDevices.value = emptyList()
            selectedBluetoothAddress = null
            Log.w(tag, "Bluetooth discovery error: ${e.message}")
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
            Log.w(tag, "USB detection error: ${e.message}")
        }
    }

    suspend fun printReceipt(
        receipt: ReceiptData,
        channel: PrinterChannel,
        logoBitmap: Bitmap?
    ): PrintResult = withContext(Dispatchers.IO) {
        _status.value = _status.value.copy(isPrinting = true)

        if (channel == PrinterChannel.SUNMI_BUILTIN && !_status.value.hasPaper) {
            _status.value = _status.value.copy(isPrinting = false)
            return@withContext PrintResult.OutOfPaper()
        }

        val escPosData = EscPosBuilder(totalColumns = 32).assembleNaomiReceipt(receipt, logoBitmap)
        _lastEscPosBytes.value = escPosData

        val result = try {
            when (channel) {
                PrinterChannel.SUNMI_BUILTIN -> printViaSunmi(escPosData)
                PrinterChannel.BLUETOOTH -> printViaBluetooth(escPosData)
                PrinterChannel.USB_OTG -> printViaUsb(escPosData)
            }
        } catch (e: Exception) {
            Log.e(tag, "Unhandled print error", e)
            PrintResult.Error(e.localizedMessage ?: "Unexpected printer error", channel)
        }

        _status.value = if (result is PrintResult.Success && channel == PrinterChannel.SUNMI_BUILTIN) {
            _status.value.copy(
                isPrinting = false,
                paperRollRemainingPercent = (_status.value.paperRollRemainingPercent - 2).coerceAtLeast(0)
            )
        } else {
            _status.value.copy(isPrinting = false)
        }
        result
    }

    private fun printViaSunmi(data: ByteArray): PrintResult {
        val service = woyouService
        if (service == null || !isSunmiServiceBound) {
            return PrintResult.Error(
                "Sunmi printer service is not connected. No receipt was printed.",
                PrinterChannel.SUNMI_BUILTIN
            )
        }

        return try {
            val callback = object : ICallback.Stub() {
                override fun onRunResult(isSuccess: Boolean) {
                    Log.d(tag, "Sunmi execution result: $isSuccess")
                }

                override fun onReturnString(result: String?) = Unit

                override fun onRaiseException(code: Int, msg: String?) {
                    Log.e(tag, "Sunmi printer exception: $code - $msg")
                }

                override fun onPrintResult(code: Int, msg: String?) {
                    Log.d(tag, "Sunmi print result code: $code, msg: $msg")
                }
            }

            service.printerInit(callback)
            service.sendRAWData(data, callback)
            service.lineWrap(3, callback)
            service.cutPaper(callback)
            PrintResult.Success(
                "Receipt sent successfully to the Sunmi V2 thermal printer",
                PrinterChannel.SUNMI_BUILTIN,
                data.size
            )
        } catch (e: Exception) {
            Log.e(tag, "Native Sunmi print error", e)
            PrintResult.Error(
                "Sunmi printing error: ${e.localizedMessage ?: "unknown error"}",
                PrinterChannel.SUNMI_BUILTIN
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun printViaBluetooth(data: ByteArray): PrintResult {
        val adapter = BluetoothAdapter.getDefaultAdapter()
            ?: return PrintResult.Error("Bluetooth hardware is not available on this terminal", PrinterChannel.BLUETOOTH)

        if (!adapter.isEnabled) {
            return PrintResult.Error(
                "Bluetooth is turned off. Enable Bluetooth before printing.",
                PrinterChannel.BLUETOOTH
            )
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
            Log.e(tag, "Bluetooth print failed", e)
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
            Log.e(tag, "USB print failed", e)
            PrintResult.Error(
                "USB transmission failed: ${e.localizedMessage ?: "unknown error"}",
                PrinterChannel.USB_OTG
            )
        } finally {
            runCatching { connection?.releaseInterface(printerInterface) }
            runCatching { connection?.close() }
        }
    }

    fun togglePaperRoll() {
        val current = _status.value.hasPaper
        _status.value = _status.value.copy(
            hasPaper = !current,
            paperRollRemainingPercent = if (!current) 100 else 0,
            lastError = if (current) "OUT_OF_PAPER: 58mm Thermal paper roll depleted." else null
        )
    }

    fun reloadPaper() {
        _status.value = _status.value.copy(
            hasPaper = true,
            paperRollRemainingPercent = 100,
            lastError = null
        )
    }

    fun feedPaper(lines: Int = 3) {
        val service = woyouService
        if (service != null && isSunmiServiceBound) {
            try {
                service.lineWrap(lines, null)
            } catch (e: Exception) {
                Log.e(tag, "Feed error", e)
            }
        }
    }

    fun cutPaper() {
        val service = woyouService
        if (service != null && isSunmiServiceBound) {
            try {
                service.cutPaper(null)
            } catch (e: Exception) {
                Log.e(tag, "Cut error", e)
            }
        }
    }

    fun cleanup() {
        if (isSunmiServiceBound) {
            try {
                context.unbindService(sunmiConnection)
                isSunmiServiceBound = false
            } catch (e: Exception) {
                Log.e(tag, "Unbind error", e)
            }
        }
        woyouService = null
        clearBuffers()
    }

    fun clearBuffers() {
        _lastEscPosBytes.value = null
    }
}
