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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ReceiptData
import com.example.ui.PosViewModel
import com.example.ui.theme.NaomiBorder
import com.example.ui.theme.NaomiDarkBg
import com.example.ui.theme.NaomiDeepRed
import com.example.ui.theme.NaomiOrange
import com.example.ui.theme.NaomiRed
import com.example.ui.theme.NaomiSuccess
import com.example.ui.theme.NaomiSurface
import com.example.ui.theme.NaomiSurfaceVariant
import com.example.ui.theme.NaomiTextPrimary
import com.example.ui.theme.NaomiTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class LedgerFilter { ALL, TODAY, UNSYNCED, VOIDED }

@Composable
fun SyncLedgerScreen(
    viewModel: PosViewModel,
    onEditCorrection: () -> Unit = {}
) {
    val receipts by viewModel.allReceipts.collectAsState()
    val unsyncedCount by viewModel.unsyncedCount.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()

    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(LedgerFilter.ALL) }
    var voidTarget by remember { mutableStateOf<ReceiptData?>(null) }
    var voidReason by remember { mutableStateOf("") }

    val todayKey = remember { SimpleDateFormat("yyyyMMdd", Locale.UK).format(Date()) }
    val dayFormat = remember { SimpleDateFormat("yyyyMMdd", Locale.UK) }
    val visibleReceipts = receipts.filter { receipt ->
        val matchesQuery = query.isBlank() || listOf(
            receipt.id,
            receipt.clientName,
            receipt.venueName,
            receipt.processedBy,
            receipt.paymentMethod.label,
            receipt.voidReason.orEmpty()
        ).any { it.contains(query.trim(), ignoreCase = true) }
        val matchesFilter = when (filter) {
            LedgerFilter.ALL -> true
            LedgerFilter.TODAY -> dayFormat.format(Date(receipt.createdAt)) == todayKey
            LedgerFilter.UNSYNCED -> receipt.isBufferedOffline && !receipt.isVoided
            LedgerFilter.VOIDED -> receipt.isVoided
        }
        matchesQuery && matchesFilter
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NaomiDarkBg)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("sync_ledger_screen")
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = NaomiSurface),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, NaomiBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("AUDIT LEDGER & GATEWAY SYNC", color = NaomiOrange, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    Text(
                        if (unsyncedCount > 0) "$unsyncedCount receipt(s) waiting to sync" else "All active receipts acknowledged",
                        color = NaomiTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text("Voided receipts remain in the ledger and are never silently deleted.", color = NaomiTextSecondary, fontSize = 9.5.sp)
                }
                Button(
                    onClick = { viewModel.syncAllBufferedTransactions() },
                    enabled = !isSyncing,
                    colors = ButtonDefaults.buttonColors(containerColor = NaomiRed),
                    modifier = Modifier.testTag("sync_all_btn")
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sync", fontSize = 10.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search receipt, venue, customer or staff") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            items(LedgerFilter.values()) { item ->
                FilterChip(
                    selected = filter == item,
                    onClick = { filter = item },
                    label = { Text(item.name.lowercase().replaceFirstChar { it.titlecase() }, fontSize = 9.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NaomiRed,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Text(
            "${visibleReceipts.size} of ${receipts.size} receipt(s)",
            color = NaomiTextSecondary,
            fontSize = 10.sp,
            modifier = Modifier.padding(vertical = 5.dp)
        )

        if (visibleReceipts.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.History, contentDescription = null, tint = NaomiTextSecondary, modifier = Modifier.size(42.dp))
                    Text("No receipts match this view.", color = NaomiTextSecondary, fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                items(visibleReceipts, key = { it.id }) { receipt ->
                    ReceiptLedgerCard(
                        receipt = receipt,
                        onReprint = { viewModel.reprintReceipt(receipt) },
                        onVoid = { voidTarget = receipt; voidReason = "" },
                        onCorrect = {
                            viewModel.prepareCorrection(receipt)
                            onEditCorrection()
                        }
                    )
                }
            }
        }
    }

    voidTarget?.let { receipt ->
        AlertDialog(
            onDismissRequest = { voidTarget = null },
            containerColor = NaomiSurface,
            title = { Text("Void ${receipt.id}?", color = NaomiTextPrimary, fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "This does not delete the transaction. It records a VOID entry with staff name, reason and timestamp.",
                        color = NaomiTextSecondary,
                        fontSize = 11.sp
                    )
                    OutlinedTextField(
                        value = voidReason,
                        onValueChange = { voidReason = it },
                        label = { Text("Reason for void") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.voidReceipt(receipt, voidReason)
                        voidTarget = null
                    },
                    enabled = voidReason.trim().length >= 3,
                    colors = ButtonDefaults.buttonColors(containerColor = NaomiRed)
                ) { Text("Void Receipt") }
            },
            dismissButton = {
                TextButton(onClick = { voidTarget = null }) { Text("Cancel", color = NaomiTextSecondary) }
            }
        )
    }
}

@Composable
private fun ReceiptLedgerCard(
    receipt: ReceiptData,
    onReprint: () -> Unit,
    onVoid: () -> Unit,
    onCorrect: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (receipt.isVoided) NaomiSurfaceVariant else NaomiSurface
        ),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (receipt.isVoided) NaomiOrange else NaomiBorder
        ),
        modifier = Modifier.fillMaxWidth().testTag("receipt_item_${receipt.id}")
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(11.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(receipt.id, color = NaomiTextPrimary, fontWeight = FontWeight.Black, fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        StatusPill(if (receipt.isVoided) "VOID" else if (receipt.isBufferedOffline) "PENDING" else "SYNCED", receipt)
                    }
                    Text("${receipt.clientName} • ${receipt.venueName}", color = NaomiTextPrimary, fontSize = 11.sp)
                    Text("${receipt.formattedDate()} • ${receipt.paymentMethod.label}", color = NaomiTextSecondary, fontSize = 9.5.sp)
                    if (receipt.processedBy.isNotBlank()) {
                        Text("Processed by ${receipt.processedBy}", color = NaomiTextSecondary, fontSize = 9.5.sp)
                    }
                    receipt.replacesReceiptId?.let {
                        Text("Correction of $it", color = NaomiOrange, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                    }
                    if (receipt.isVoided) {
                        Text(
                            "VOID: ${receipt.voidReason.orEmpty()} • ${receipt.voidedBy.orEmpty()}",
                            color = NaomiOrange,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    if (receipt.isEffectivelyFree) "FREE" else ReceiptData.formatCurrency(receipt.grandTotal),
                    color = if (receipt.isVoided) NaomiTextSecondary else if (receipt.isEffectivelyFree) NaomiSuccess else NaomiOrange,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = onReprint,
                    enabled = !receipt.isVoided,
                    modifier = Modifier.weight(1f).height(34.dp)
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Reprint", fontSize = 9.sp)
                }
                OutlinedButton(
                    onClick = onCorrect,
                    modifier = Modifier.weight(1f).height(34.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Correct", fontSize = 9.sp)
                }
                Button(
                    onClick = onVoid,
                    enabled = !receipt.isVoided,
                    colors = ButtonDefaults.buttonColors(containerColor = NaomiDeepRed),
                    modifier = Modifier.weight(1f).height(34.dp)
                ) {
                    Icon(Icons.Default.VisibilityOff, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Void", fontSize = 9.sp)
                }
            }
        }
    }
}

@Composable
private fun StatusPill(label: String, receipt: ReceiptData) {
    val color = when {
        receipt.isVoided -> NaomiOrange
        receipt.isBufferedOffline -> NaomiOrange
        else -> NaomiSuccess
    }
    Surface(shape = RoundedCornerShape(4.dp), color = color.copy(alpha = 0.18f)) {
        Text(
            label,
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 8.sp,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
        )
    }
}
