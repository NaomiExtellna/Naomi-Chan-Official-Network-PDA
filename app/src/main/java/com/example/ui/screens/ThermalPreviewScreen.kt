package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PrinterChannel
import com.example.model.ReceiptData
import com.example.ui.PosViewModel
import com.example.ui.components.ThermalReceiptPaper
import com.example.ui.theme.NaomiBorder
import com.example.ui.theme.NaomiDarkBg
import com.example.ui.theme.NaomiOrange
import com.example.ui.theme.NaomiRed
import com.example.ui.theme.NaomiSurface
import com.example.ui.theme.NaomiTextPrimary
import com.example.ui.theme.NaomiTextSecondary

@Composable
fun ThermalPreviewScreen(
    viewModel: PosViewModel,
    receipt: ReceiptData
) {
    val selectedChannel by viewModel.selectedChannel.collectAsState()
    val printerStatus by viewModel.printerStatus.collectAsState()
    val lastEscPosBytes by viewModel.printerManager.lastEscPosBytes.collectAsState()
    val customIconBitmap by viewModel.customIconBitmap.collectAsState()
    var showRawBytes by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NaomiDarkBg)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
            .testTag("thermal_preview_screen")
    ) {
        // Top action bar
        Card(
            colors = CardDefaults.cardColors(containerColor = NaomiSurface),
            shape = RoundedCornerShape(0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "LIVE 58MM THERMAL SIMULATION",
                        color = NaomiOrange,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = if (receipt.isEffectivelyFree) "🎟️ FREE EVENT PASS • ${selectedChannel.displayName}" else "Target: ${selectedChannel.displayName}",
                        color = NaomiTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Button(
                    onClick = { viewModel.printCurrentReceipt() },
                    colors = ButtonDefaults.buttonColors(containerColor = NaomiRed),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("thermal_print_now_btn")
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Print Now", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Center 58mm Thermal Receipt Paper View
        ThermalReceiptPaper(
            receipt = receipt,
            customIconBitmap = customIconBitmap,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(14.dp))

        // ESC/POS Inspector Card
        Card(
            colors = CardDefaults.cardColors(containerColor = NaomiSurface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Code, contentDescription = null, tint = NaomiOrange, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ESC/POS Binary Stream Inspector",
                            color = NaomiTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    OutlinedButton(
                        onClick = { showRawBytes = !showRawBytes },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NaomiOrange),
                        modifier = Modifier.testTag("toggle_escpos_hex_btn")
                    ) {
                        Text(if (showRawBytes) "Hide Raw" else "View Hex", fontSize = 11.sp)
                    }
                }

                if (showRawBytes) {
                    Spacer(modifier = Modifier.height(10.dp))
                    val hexData = lastEscPosBytes?.take(160)?.joinToString(" ") { String.format("%02X", it) }
                        ?: "1B 40 1B 61 01 1D 76 30 00 28 00 ... [GS v 0 Monochrome Bitmap Header + Text Stream]"
                    Surface(
                        color = Color.Black,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "RAW ESC/POS BYTES (${lastEscPosBytes?.size ?: 1240} bytes):",
                                color = NaomiOrange,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = hexData,
                                color = Color.Green,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Test Hardware Error Management: Out-of-paper toggle
        Card(
            colors = CardDefaults.cardColors(containerColor = NaomiSurface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "POS Hardware Error Simulation",
                        color = NaomiTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = if (printerStatus.hasPaper) "Paper roll loaded (Status: OK)" else "OUT OF PAPER simulated!",
                        color = if (printerStatus.hasPaper) NaomiTextSecondary else NaomiRed,
                        fontSize = 11.sp
                    )
                }
                Button(
                    onClick = { viewModel.togglePaperRollAlert() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (printerStatus.hasPaper) Color(0xFF421E19) else NaomiOrange
                    ),
                    modifier = Modifier.testTag("test_paper_out_btn")
                ) {
                    Text(
                        text = if (printerStatus.hasPaper) "Test Paper Out" else "Reload Paper",
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
