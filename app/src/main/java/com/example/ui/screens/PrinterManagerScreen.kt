package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.ContentCut
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

@Composable
fun PrinterManagerScreen(viewModel: PosViewModel) {
    val selectedChannel by viewModel.selectedChannel.collectAsState()
    val printerStatus by viewModel.printerStatus.collectAsState()
    val bluetoothDevices by viewModel.printerManager.bluetoothDevices.collectAsState()
    val usbDevices by viewModel.printerManager.usbDevices.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(NaomiDarkBg)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("printer_manager_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = NaomiSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "PRIMARY PRINT DISPATCH CHANNEL",
                        color = NaomiOrange,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PrinterChannel.values().forEach { channel ->
                            val selected = selectedChannel == channel
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.selectChannel(channel) }
                                    .testTag("channel_selector_${channel.name}"),
                                color = if (selected) NaomiRed.copy(alpha = 0.2f) else NaomiSurfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(
                                    if (selected) 2.dp else 1.dp,
                                    if (selected) NaomiRed else NaomiBorder
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = when (channel) {
                                            PrinterChannel.SUNMI_BUILTIN -> Icons.Default.Print
                                            PrinterChannel.BLUETOOTH -> Icons.Default.Bluetooth
                                            PrinterChannel.USB_OTG -> Icons.Default.Usb
                                        },
                                        contentDescription = null,
                                        tint = if (selected) NaomiOrange else NaomiTextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = when (channel) {
                                            PrinterChannel.SUNMI_BUILTIN -> "Sunmi"
                                            PrinterChannel.BLUETOOTH -> "Bluetooth"
                                            PrinterChannel.USB_OTG -> "USB-OTG"
                                        },
                                        color = if (selected) Color.White else NaomiTextPrimary,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            PrinterSectionCard(
                title = "Sunmi V2 Inner Printer",
                subtitle = "Direct AIDL service integration",
                icon = Icons.Default.Print,
                iconBackground = NaomiDeepRed,
                selected = selectedChannel == PrinterChannel.SUNMI_BUILTIN
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(printerStatus.deviceName, color = NaomiTextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(
                            "Serial: ${printerStatus.serialNumber}",
                            color = NaomiTextSecondary,
                            fontSize = 10.sp
                        )
                    }
                    StatusBadge(
                        text = if (printerStatus.isConnected) "ONLINE" else "OFFLINE",
                        healthy = printerStatus.isConnected
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    HardwareMetric("Paper", "${printerStatus.paperRollRemainingPercent}%")
                    HardwareMetric("Head", "${printerStatus.headTemperatureCelsius}°C")
                    HardwareMetric("Paper state", if (printerStatus.hasPaper) "Ready" else "Empty")
                }

                printerStatus.lastError?.let { error ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(error, color = NaomiError, fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.printerManager.feedPaper(3) },
                        enabled = printerStatus.isConnected,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NaomiTextPrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Feed", fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = { viewModel.printerManager.cutPaper() },
                        enabled = printerStatus.isConnected,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NaomiTextPrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCut, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cut", fontSize = 11.sp)
                    }
                    Button(
                        onClick = {
                            viewModel.selectChannel(PrinterChannel.SUNMI_BUILTIN)
                            viewModel.printCurrentReceipt()
                        },
                        enabled = printerStatus.isConnected && printerStatus.hasPaper,
                        colors = ButtonDefaults.buttonColors(containerColor = NaomiRed),
                        modifier = Modifier.weight(1.25f)
                    ) {
                        Text("Print Current", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            PrinterSectionCard(
                title = "Bluetooth Thermal Printers",
                subtitle = "RFCOMM / SPP ESC/POS output",
                icon = Icons.Default.Bluetooth,
                iconBackground = Color(0xFF0D47A1),
                selected = selectedChannel == PrinterChannel.BLUETOOTH,
                trailing = {
                    IconButton(onClick = { viewModel.printerManager.refreshDiscoveredDevices() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Bluetooth printers", tint = NaomiOrange)
                    }
                }
            ) {
                if (bluetoothDevices.isEmpty()) {
                    EmptyDeviceState(
                        "No paired Bluetooth devices found. Pair the thermal printer in Android settings, grant Bluetooth permission, then refresh."
                    )
                } else {
                    bluetoothDevices.forEach { device ->
                        DiscoveredPrinterRow(device = device) {
                            viewModel.printerManager.selectDiscoveredPrinter(device)
                            viewModel.selectChannel(PrinterChannel.BLUETOOTH)
                        }
                    }
                }
            }
        }

        item {
            PrinterSectionCard(
                title = "USB-OTG Thermal Printers",
                subtitle = "USB printer-class bulk output",
                icon = Icons.Default.Usb,
                iconBackground = Color(0xFF2E7D32),
                selected = selectedChannel == PrinterChannel.USB_OTG,
                trailing = {
                    IconButton(onClick = { viewModel.printerManager.refreshDiscoveredDevices() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh USB printers", tint = NaomiOrange)
                    }
                }
            ) {
                if (usbDevices.isEmpty()) {
                    EmptyDeviceState(
                        "No USB printer-class device detected. Connect a supported ESC/POS printer through USB-OTG, then refresh."
                    )
                } else {
                    usbDevices.forEach { device ->
                        DiscoveredPrinterRow(device = device) {
                            viewModel.printerManager.selectDiscoveredPrinter(device)
                            viewModel.selectChannel(PrinterChannel.USB_OTG)
                        }
                        if (!device.isBonded) {
                            Text(
                                "USB permission will be requested on the first print attempt.",
                                color = NaomiOrange,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(start = 10.dp, bottom = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PrinterSectionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBackground: Color,
    selected: Boolean,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable Column.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = NaomiSurface),
        shape = RoundedCornerShape(12.dp),
        border = if (selected) androidx.compose.foundation.BorderStroke(1.5.dp, NaomiRed) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier.size(36.dp).background(iconBackground, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(title, color = NaomiTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(subtitle, color = NaomiTextSecondary, fontSize = 11.sp)
                    }
                }
                trailing?.invoke()
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun StatusBadge(text: String, healthy: Boolean) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (healthy) NaomiSuccess.copy(alpha = 0.2f) else NaomiError.copy(alpha = 0.2f)
    ) {
        Text(
            text = text,
            color = if (healthy) NaomiSuccess else NaomiError,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun HardwareMetric(label: String, value: String) {
    Column {
        Text(text = label, color = NaomiTextSecondary, fontSize = 10.sp)
        Text(text = value, color = NaomiTextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
private fun EmptyDeviceState(message: String) {
    Surface(
        color = NaomiSurfaceVariant,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = message,
            color = NaomiTextSecondary,
            fontSize = 11.sp,
            modifier = Modifier.padding(12.dp)
        )
    }
}

@Composable
fun DiscoveredPrinterRow(device: DiscoveredPrinter, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(NaomiSurfaceVariant)
            .clickable(onClick = onSelect)
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(device.name, color = NaomiTextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text(device.address, color = NaomiTextSecondary, fontSize = 10.sp)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Button(
            onClick = onSelect,
            colors = ButtonDefaults.buttonColors(containerColor = NaomiRed),
            modifier = Modifier.height(32.dp)
        ) {
            Text("Use", fontSize = 11.sp)
        }
    }
}
