package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ReceiptData
import com.example.ui.PosViewModel
import com.example.ui.theme.NaomiBorder
import com.example.ui.theme.NaomiDarkBg
import com.example.ui.theme.NaomiOrange
import com.example.ui.theme.NaomiPaperInk
import com.example.ui.theme.NaomiPaperWhite
import com.example.ui.theme.NaomiRed
import com.example.ui.theme.NaomiSurface
import com.example.ui.theme.NaomiSurfaceVariant
import com.example.ui.theme.NaomiTextPrimary
import com.example.ui.theme.NaomiTextSecondary

private enum class PreviewMode { RECEIPT, DIAGNOSTICS }

@Composable
fun ThermalPreviewScreen(
    viewModel: PosViewModel,
    receipt: ReceiptData
) {
    val selectedChannel by viewModel.selectedChannel.collectAsState()
    val printerStatus by viewModel.printerStatus.collectAsState()
    val lastEscPosBytes by viewModel.printerManager.lastEscPosBytes.collectAsState()
    var mode by remember { mutableStateOf(PreviewMode.RECEIPT) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NaomiDarkBg)
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag("thermal_preview_screen"),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("58MM PROOF", color = NaomiOrange, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                Text("Thermal preview", color = NaomiTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Black)
                Text(selectedChannel.displayName, color = NaomiTextSecondary, fontSize = 7.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Button(
                onClick = { viewModel.printCurrentReceipt() },
                colors = ButtonDefaults.buttonColors(containerColor = NaomiRed),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.height(38.dp).testTag("thermal_print_now_btn")
            ) {
                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Print", fontSize = 8.sp, fontWeight = FontWeight.Black)
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(
                selected = mode == PreviewMode.RECEIPT,
                onClick = { mode = PreviewMode.RECEIPT },
                label = { Text("Receipt proof", fontSize = 8.sp) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = NaomiRed, selectedLabelColor = Color.White),
                modifier = Modifier.weight(1f).height(34.dp)
            )
            FilterChip(
                selected = mode == PreviewMode.DIAGNOSTICS,
                onClick = { mode = PreviewMode.DIAGNOSTICS },
                label = { Text("ESC/POS", fontSize = 8.sp) },
                leadingIcon = { Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(13.dp)) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = NaomiRed, selectedLabelColor = Color.White, selectedLeadingIconColor = Color.White),
                modifier = Modifier.weight(1f).height(34.dp).testTag("toggle_escpos_hex_btn")
            )
        }

        if (mode == PreviewMode.RECEIPT) {
            CompactReceiptProof(receipt, Modifier.fillMaxWidth().weight(1f))
        } else {
            EscPosPanel(lastEscPosBytes, Modifier.fillMaxWidth().weight(1f))
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = NaomiSurface),
            border = BorderStroke(1.dp, if (printerStatus.hasPaper) NaomiBorder else NaomiRed.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = if (printerStatus.hasPaper) NaomiTextSecondary else NaomiRed, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Column {
                        Text("Hardware test", color = NaomiTextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(if (printerStatus.hasPaper) "Paper loaded · status OK" else "OUT OF PAPER simulated", color = if (printerStatus.hasPaper) NaomiTextSecondary else NaomiRed, fontSize = 7.sp)
                    }
                }
                Button(
                    onClick = { viewModel.togglePaperRollAlert() },
                    colors = ButtonDefaults.buttonColors(containerColor = if (printerStatus.hasPaper) NaomiSurfaceVariant else NaomiOrange),
                    modifier = Modifier.height(34.dp).testTag("test_paper_out_btn")
                ) {
                    Text(if (printerStatus.hasPaper) "Test paper" else "Reload", color = if (printerStatus.hasPaper) NaomiTextPrimary else Color.White, fontSize = 7.sp)
                }
            }
        }
    }
}

@Composable
private fun CompactReceiptProof(receipt: ReceiptData, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = NaomiPaperWhite),
        shape = RoundedCornerShape(3.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 11.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("NAOMI-CHAN™", color = NaomiPaperInk, fontFamily = FontFamily.Monospace, fontSize = 17.sp, fontWeight = FontWeight.Black)
            Text("PREMIUM DJ SERVICES · BLACKPOOL", color = NaomiPaperInk, fontFamily = FontFamily.Monospace, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            if (receipt.isEffectivelyFree) {
                Surface(color = Color(0xFFF1F8E9), border = BorderStroke(1.dp, NaomiPaperInk), modifier = Modifier.fillMaxWidth()) {
                    Text("*** FREE ADMISSION ***", color = NaomiPaperInk, textAlign = TextAlign.Center, fontFamily = FontFamily.Monospace, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(4.dp))
                }
            }
            ReceiptProofLine("RECEIPT", receipt.id)
            ReceiptProofLine("CLIENT", receipt.clientName)
            ReceiptProofLine("VENUE", receipt.venueName)
            ReceiptProofLine("GIG", receipt.gigType.label)

            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(NaomiPaperInk.copy(alpha = 0.45f)))
            Text("SERVICES & MERCHANDISE", color = NaomiPaperInk, fontFamily = FontFamily.Monospace, fontSize = 8.sp, fontWeight = FontWeight.Black)

            receipt.items.take(3).forEach { item ->
                ReceiptProofLine(
                    if (item.quantity > 1) "${item.name} x${item.quantity}" else item.name,
                    if (item.unitPrice == 0.0) "FREE" else ReceiptData.formatCurrency(item.total)
                )
            }
            if (receipt.items.size > 3) {
                Text("+ ${receipt.items.size - 3} additional item(s)", color = NaomiPaperInk.copy(alpha = 0.7f), fontFamily = FontFamily.Monospace, fontSize = 7.sp)
            }

            Spacer(modifier = Modifier.weight(1f))
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(NaomiPaperInk))
            ReceiptProofLine("SUBTOTAL", ReceiptData.formatCurrency(if (receipt.isEffectivelyFree) 0.0 else receipt.subtotal))
            ReceiptProofLine("VAT ${receipt.taxPercent.toInt()}%", ReceiptData.formatCurrency(receipt.taxAmount))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("TOTAL", color = NaomiPaperInk, fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Black)
                Text(if (receipt.isEffectivelyFree) "FREE" else ReceiptData.formatCurrency(receipt.grandTotal), color = NaomiPaperInk, fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Black)
            }
            Text("POS proof · printed receipt includes full detail", color = NaomiPaperInk.copy(alpha = 0.55f), fontFamily = FontFamily.Monospace, fontSize = 6.sp)
        }
    }
}

@Composable
private fun ReceiptProofLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = NaomiPaperInk, fontFamily = FontFamily.Monospace, fontSize = 7.sp, modifier = Modifier.weight(0.45f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(value, color = NaomiPaperInk, fontFamily = FontFamily.Monospace, fontSize = 7.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.55f), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun EscPosPanel(bytes: ByteArray?, modifier: Modifier = Modifier) {
    val hexData = bytes?.take(240)?.chunked(16)?.take(10)?.joinToString("\n") { row ->
        row.joinToString(" ") { String.format("%02X", it) }
    } ?: "1B 40 1B 61 01 1D 76 30 00\n[Generate a print to capture real bytes]"

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Black),
        border = BorderStroke(1.dp, NaomiBorder),
        shape = RoundedCornerShape(4.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(11.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text("RAW ESC/POS STREAM", color = NaomiOrange, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black)
            Text("${bytes?.size ?: 0} captured bytes · showing first 10 rows", color = Color.Gray, fontSize = 7.sp, fontFamily = FontFamily.Monospace)
            Surface(color = Color(0xFF071007), shape = RoundedCornerShape(7.dp), modifier = Modifier.fillMaxWidth().weight(1f)) {
                Text(
                    hexData,
                    color = Color.Green,
                    fontSize = 8.sp,
                    lineHeight = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(9.dp)
                )
            }
        }
    }
}