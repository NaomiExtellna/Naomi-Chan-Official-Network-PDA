package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.PrinterChannel
import com.example.printer.DiscoveredPrinter
import com.example.ui.PosViewModel
import com.example.ui.theme.NaomiBorder
import com.example.ui.theme.NaomiDarkBg
import com.example.ui.theme.NaomiDeepRed
import com.example.ui.theme.NaomiError
import com.example.ui.theme.NaomiOrange
import com.example.ui.theme.NaomiRed
import com.example.ui.theme.NaomiSuccess
import com.example.ui.theme.NaomiSurface
import com.example.ui.theme.NaomiSurfaceVariant
import com.example.ui.theme.NaomiTextPrimary
import com.example.ui.theme.NaomiTextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val DEVICES_PER_PAGE = 3

@Composable
fun PrinterManagerScreen(viewModel: PosViewModel) {
    val selectedChannel by viewModel.selectedChannel.collectAsStateWithLifecycle()
    val printerStatus by viewModel.printerStatus.collectAsStateWithLifecycle()
    val bluetoothDevices by viewModel.printerManager.bluetoothDevices.collectAsStateWithLifecycle()
    val usbDevices by viewModel.printerManager.usbDevices.collectAsStateWithLifecycle()
    var devicePage by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            viewModel.printerManager.refreshDiscoveredDevices()
        }
    }
    LaunchedEffect(selectedChannel) { devicePage = 0 }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NaomiDarkBg)
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag("printer_manager_screen"),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("PRINT OUTPUT", color = NaomiOrange, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                Text("Printer console", color = NaomiTextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Black)
            }
            IconButton(onClick = { viewModel.printerManager.refreshDiscoveredDevices() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh printers", tint = NaomiOrange)
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PrinterChannel.values().forEach { channel ->
                ChannelTile(
                    channel = channel,
                    selected = selectedChannel == channel,
                    onClick = { viewModel.selectChannel(channel) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        when (selectedChannel) {
            PrinterChannel.SUNMI_BUILTIN -> SunmiPanel(
                viewModel = viewModel,
                connected = printerStatus.isConnected,
                hasPaper = printerStatus.hasPaper,
                coverOpen = printerStatus.isCoverOpen,
                overheated = printerStatus.isOverheated,
                deviceName = printerStatus.deviceName,
                serialNumber = printerStatus.serialNumber,
                paperWidth = printerStatus.paperWidthMm,
                statusCode = printerStatus.statusCode,
                error = printerStatus.lastError,
                modifier = Modifier.weight(1f)
            )

            PrinterChannel.BLUETOOTH -> ExternalPrinterPanel(
                title = "Bluetooth printers",
                subtitle = "Paired ESC/POS fallback devices",
                icon = Icons.Default.Bluetooth,
                devices = bluetoothDevices,
                page = devicePage,
                onPageChange = { devicePage = it },
                onRefresh = { viewModel.printerManager.refreshDiscoveredDevices() },
                onSelect = { device ->
                    viewModel.printerManager.selectDiscoveredPrinter(device)
                    viewModel.selectChannel(PrinterChannel.BLUETOOTH)
                },
                modifier = Modifier.weight(1f)
            )

            PrinterChannel.USB_OTG -> ExternalPrinterPanel(
                title = "USB-OTG printers",
                subtitle = "USB printer-class fallback devices",
                icon = Icons.Default.Usb,
                devices = usbDevices,
                page = devicePage,
                onPageChange = { devicePage = it },
                onRefresh = { viewModel.printerManager.refreshDiscoveredDevices() },
                onSelect = { device ->
                    viewModel.printerManager.selectDiscoveredPrinter(device)
                    viewModel.selectChannel(PrinterChannel.USB_OTG)
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ChannelTile(
    channel: PrinterChannel,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon = when (channel) {
        PrinterChannel.SUNMI_BUILTIN -> Icons.Default.Print
        PrinterChannel.BLUETOOTH -> Icons.Default.Bluetooth
        PrinterChannel.USB_OTG -> Icons.Default.Usb
    }
    val label = when (channel) {
        PrinterChannel.SUNMI_BUILTIN -> "SUNMI"
        PrinterChannel.BLUETOOTH -> "Bluetooth"
        PrinterChannel.USB_OTG -> "USB"
    }
    Surface(
        modifier = modifier.height(56.dp).clickable(onClick = onClick).testTag("channel_selector_${channel.name}"),
        color = if (selected) NaomiRed.copy(alpha = 0.10f) else NaomiSurface,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) NaomiRed else NaomiBorder),
        shape = RoundedCornerShape(4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = if (selected) NaomiOrange else NaomiTextSecondary, modifier = Modifier.size(18.dp))
            Text(label, color = NaomiTextPrimary, fontSize = 8.sp, fontWeight = if (selected) FontWeight.Black else FontWeight.Bold)
        }
    }
}

@Composable
private fun SunmiPanel(
    viewModel: PosViewModel,
    connected: Boolean,
    hasPaper: Boolean,
    coverOpen: Boolean,
    overheated: Boolean,
    deviceName: String,
    serialNumber: String,
    paperWidth: Int,
    statusCode: Int?,
    error: String?,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = NaomiSurface),
        border = BorderStroke(1.dp, if (connected) NaomiSuccess.copy(alpha = 0.35f) else NaomiError.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(15.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(13.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    Box(
                        modifier = Modifier.size(38.dp).background(NaomiDeepRed, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, tint = Color.White, modifier = Modifier.size(21.dp))
                    }
                    Column {
                        Text(deviceName, color = NaomiTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Black)
                        Text("Built-in 58mm thermal", color = NaomiTextSecondary, fontSize = 8.sp)
                    }
                }
                StatusBadge(if (connected) "ONLINE" else "OFFLINE", connected)
            }

            Surface(color = NaomiSurfaceVariant, shape = RoundedCornerShape(4.dp), modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    HardwareMetric("Paper", "${paperWidth}mm")
                    HardwareMetric("Width", if (paperWidth == 58) "384 dots" else "Default")
                    HardwareMetric("State", sunmiStateLabel(statusCode, hasPaper))
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Serial", color = NaomiTextSecondary, fontSize = 8.sp)
                Text(serialNumber, color = NaomiTextPrimary, fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            error?.let {
                Surface(color = NaomiError.copy(alpha = 0.08f), shape = RoundedCornerShape(4.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(it, color = NaomiError, fontSize = 9.sp, modifier = Modifier.padding(9.dp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }

            Surface(color = NaomiSurfaceVariant, shape = RoundedCornerShape(4.dp), modifier = Modifier.fillMaxWidth().weight(1f)) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(10.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("SUNMI V2 PRIMARY OUTPUT", color = NaomiOrange, fontSize = 8.sp, fontWeight = FontWeight.Black)
                    Text("Manual tear bar · cutter commands disabled · paper feeds forward after print.", color = NaomiTextSecondary, fontSize = 9.sp)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                OutlinedButton(
                    onClick = { viewModel.printerManager.refreshSunmiStatus() },
                    modifier = Modifier.weight(0.8f).height(40.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Refresh", fontSize = 8.sp)
                }
                OutlinedButton(
                    onClick = { viewModel.printerManager.feedPaper(3) },
                    enabled = connected,
                    modifier = Modifier.weight(1f).height(40.dp)
                ) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Feed", fontSize = 8.sp)
                }
                Button(
                    onClick = {
                        viewModel.selectChannel(PrinterChannel.SUNMI_BUILTIN)
                        viewModel.printCurrentReceipt()
                    },
                    enabled = connected && hasPaper && !coverOpen && !overheated,
                    colors = ButtonDefaults.buttonColors(containerColor = NaomiRed),
                    modifier = Modifier.weight(1.2f).height(40.dp)
                ) {
                    Text("Print current", fontSize = 8.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun ExternalPrinterPanel(
    title: String,
    subtitle: String,
    icon: ImageVector,
    devices: List<DiscoveredPrinter>,
    page: Int,
    onPageChange: (Int) -> Unit,
    onRefresh: () -> Unit,
    onSelect: (DiscoveredPrinter) -> Unit,
    modifier: Modifier = Modifier
) {
    val pages = pageCount(devices.size, DEVICES_PER_PAGE)
    val safePage = page.coerceIn(0, pages - 1)
    val visible = pageSlice(devices, safePage, DEVICES_PER_PAGE)

    Card(
        colors = CardDefaults.cardColors(containerColor = NaomiSurface),
        border = BorderStroke(1.dp, NaomiBorder),
        shape = RoundedCornerShape(15.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(color = NaomiSurfaceVariant, shape = RoundedCornerShape(4.dp)) {
                        Icon(icon, contentDescription = null, tint = NaomiOrange, modifier = Modifier.padding(9.dp).size(19.dp))
                    }
                    Column {
                        Text(title, color = NaomiTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Black)
                        Text(subtitle, color = NaomiTextSecondary, fontSize = 8.sp)
                    }
                }
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh devices", tint = NaomiOrange)
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                if (visible.isEmpty()) {
                    EmptyDeviceState("No compatible printer detected. SUNMI built-in remains the recommended output.")
                } else {
                    visible.forEach { device ->
                        DiscoveredPrinterRow(device, { onSelect(device) }, Modifier.weight(1f))
                    }
                    repeat((DEVICES_PER_PAGE - visible.size).coerceAtLeast(0)) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }

            PosPager(
                page = safePage,
                totalPages = pages,
                onPrevious = { onPageChange((safePage - 1).coerceAtLeast(0)) },
                onNext = { onPageChange((safePage + 1).coerceAtMost(pages - 1)) },
                label = "DEVICE"
            )
        }
    }
}

private fun sunmiStateLabel(statusCode: Int?, hasPaper: Boolean): String = when {
    !hasPaper -> "No paper"
    statusCode == null -> "Connecting"
    statusCode == 1 -> "Ready"
    statusCode == 2 -> "Preparing"
    statusCode == 3 -> "Comm error"
    statusCode == 5 -> "Hot"
    statusCode == 6 -> "Cover open"
    statusCode == 505 -> "Missing"
    else -> "Code $statusCode"
}

@Composable
private fun StatusBadge(text: String, healthy: Boolean) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = if (healthy) NaomiSuccess.copy(alpha = 0.12f) else NaomiError.copy(alpha = 0.12f)
    ) {
        Text(
            text,
            color = if (healthy) NaomiSuccess else NaomiError,
            fontWeight = FontWeight.Black,
            fontSize = 8.sp,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun HardwareMetric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = NaomiTextSecondary, fontSize = 7.sp)
        Text(value, color = NaomiTextPrimary, fontWeight = FontWeight.Black, fontSize = 9.sp)
    }
}

@Composable
private fun EmptyDeviceState(message: String) {
    Surface(
        color = NaomiSurfaceVariant,
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.Center) {
            Text(message, color = NaomiTextSecondary, fontSize = 9.sp)
        }
    }
}

@Composable
fun DiscoveredPrinterRow(
    device: DiscoveredPrinter,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth().clickable(onClick = onSelect),
        color = NaomiSurfaceVariant,
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, NaomiBorder)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(device.name, color = NaomiTextPrimary, fontWeight = FontWeight.Black, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(device.address, color = NaomiTextSecondary, fontSize = 8.sp, maxLines = 1)
                if (!device.isBonded) Text("Permission may be requested on first print", color = NaomiOrange, fontSize = 7.sp)
            }
            Button(onClick = onSelect, colors = ButtonDefaults.buttonColors(containerColor = NaomiRed), modifier = Modifier.height(34.dp)) {
                Text("Use", fontSize = 8.sp)
            }
        }
    }
}