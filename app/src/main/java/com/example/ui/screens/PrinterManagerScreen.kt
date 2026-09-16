package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Warning
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
import com.example.model.PrinterStatus
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
fun PrinterManagerScreen(
    viewModel: PosViewModel
) {
    val selectedChannel by viewModel.selectedChannel.collectAsState()
    val printerStatus by viewModel.printerStatus.collectAsState()
    val btDevices by viewModel.printerManager.bluetoothDevices.collectAsState()
    val usbDevices by viewModel.printerManager.usbDevices.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(NaomiDarkBg)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("printer_manager_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Active Routing Card
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
                        for (ch in PrinterChannel.values()) {
                            val isSelected = selectedChannel == ch
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.selectChannel(ch) }
                                    .testTag("channel_selector_${ch.name}"),
                                color = if (isSelected) NaomiRed.copy(alpha = 0.2f) else NaomiSurfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(
                                    if (isSelected) 2.dp else 1.dp,
                                    if (isSelected) NaomiRed else NaomiBorder
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = when (ch) {
                                            PrinterChannel.SUNMI_BUILTIN -> Icons.Default.Print
                                            PrinterChannel.BLUETOOTH -> Icons.Default.Bluetooth
                                            PrinterChannel.USB_OTG -> Icons.Default.Usb
                                        },
                                        contentDescription = null,
                                        tint = if (isSelected) NaomiOrange else NaomiTextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = ch.displayName.split(" ").take(2).joinToString(" "),
                                        color = if (isSelected) Color.White else NaomiTextPrimary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 11.sp
                                    )
                                    if (isSelected) {
                                        Text(
                                            text = "ACTIVE",
                                            color = NaomiOrange,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 9.sp,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 1. Sunmi V2 Inner Thermal Printer Channel
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = NaomiSurface),
                shape = RoundedCornerShape(12.dp),
                border = if (selectedChannel == PrinterChannel.SUNMI_BUILTIN) androidx.compose.foundation.BorderStroke(1.5.dp, NaomiRed) else null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(NaomiDeepRed, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Print, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Sunmi V2 Inner Printer",
                                    color = NaomiTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Direct AIDL Service Integration",
                                    color = NaomiTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (printerStatus.isConnected) NaomiSuccess.copy(alpha = 0.2f) else NaomiError.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = if (printerStatus.isConnected) "ONLINE" else "OFFLINE",
                                color = if (printerStatus.isConnected) NaomiSuccess else NaomiError,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Hardware Specifications Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        HardwareMetric(label = "Hardware Model", value = printerStatus.deviceName)
                        HardwareMetric(label = "Serial No.", value = printerStatus.serialNumber)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        HardwareMetric(label = "Paper Roll (58mm)", value = "${printerStatus.paperRollRemainingPercent}% loaded")
                        HardwareMetric(label = "Thermal Head Temp", value = "${printerStatus.headTemperatureCelsius}°C Normal")
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Direct Hardware Commands
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.printerManager.feedPaper(3) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NaomiTextPrimary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Feed 3L", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = { viewModel.printerManager.cutPaper() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NaomiTextPrimary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ContentCut, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Partial Cut", fontSize = 11.sp)
                        }

                        Button(
                            onClick = { viewModel.printCurrentReceipt() },
                            colors = ButtonDefaults.buttonColors(containerColor = NaomiRed),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Text("Test Print", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 2. Bluetooth Peripheral Thermal Printers
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = NaomiSurface),
                shape = RoundedCornerShape(12.dp),
                border = if (selectedChannel == PrinterChannel.BLUETOOTH) androidx.compose.foundation.BorderStroke(1.5.dp, NaomiRed) else null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFF0D47A1), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Bluetooth, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Bluetooth External Printers",
                                    color = NaomiTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "RFCOMM / SPP UUID Thermal Output",
                                    color = NaomiTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        IconButton(
                            onClick = { viewModel.printerManager.refreshDiscoveredDevices() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Scan", tint = NaomiOrange)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (btDevices.isEmpty()) {
                        Surface(
                            color = NaomiSurfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Mobile BT Thermal Printer Ready",
                                    color = NaomiTextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "Standard SPP (UUID 00001101-0000-1000-8000-00805F9B34FB) supported. Ready for wireless thermal printing.",
                                    color = NaomiTextSecondary,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    } else {
                        btDevices.forEach { dev ->
                            DiscoveredPrinterRow(device = dev, isSelected = selectedChannel == PrinterChannel.BLUETOOTH) {
                                viewModel.selectChannel(PrinterChannel.BLUETOOTH)
                            }
                        }
                    }
                }
            }
        }

        // 3. USB-OTG Printer Channel
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = NaomiSurface),
                shape = RoundedCornerShape(12.dp),
                border = if (selectedChannel == PrinterChannel.USB_OTG) androidx.compose.foundation.BorderStroke(1.5.dp, NaomiRed) else null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFF2E7D32), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Usb, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "USB-OTG Hardwired Printers",
                                    color = NaomiTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Host Class 7 Bulk Transfer",
                                    color = NaomiTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        IconButton(
                            onClick = { viewModel.printerManager.refreshDiscoveredDevices() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Scan", tint = NaomiOrange)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        color = NaomiSurfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "USB Host Controller Status: Active",
                                color = NaomiTextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "Supports ESC/POS bulk endpoints via USB-C OTG cable adapter. Automatic interface claim & transfer timeout safety.",
                                color = NaomiTextSecondary,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        }
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
fun DiscoveredPrinterRow(
    device: DiscoveredPrinter,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(NaomiSurfaceVariant)
            .clickable { onSelect() }
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = device.name, color = NaomiTextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text(text = device.address, color = NaomiTextSecondary, fontSize = 10.sp)
        }
        Button(
            onClick = { onSelect() },
            colors = ButtonDefaults.buttonColors(containerColor = if (isSelected) NaomiRed else NaomiSurface),
            modifier = Modifier.height(32.dp)
        ) {
            Text(if (isSelected) "Active" else "Select", fontSize = 11.sp)
        }
    }
}
