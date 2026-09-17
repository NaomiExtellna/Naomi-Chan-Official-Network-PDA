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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.ReceiptData
import com.example.model.WirelessOrder
import com.example.ui.PosViewModel
import com.example.ui.syncGatewayNow
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
    showFinancials: Boolean = true,
    canVoid: Boolean = true,
    onEditCorrection: () -> Unit = {}
) {
    val receipts by viewModel.allReceipts.collectAsStateWithLifecycle()
    val unsyncedCount by viewModel.unsyncedCount.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val isWirelessSyncing by viewModel.isWirelessSyncing.collectAsStateWithLifecycle()
    val gatewayOnline by viewModel.isWirelessOnline.collectAsStateWithLifecycle()
    val pendingOrders by viewModel.pendingWirelessOrders.collectAsStateWithLifecycle()

    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(LedgerFilter.ALL) }
    var voidTarget by remember { mutableStateOf<ReceiptData?>(null) }
    var voidReason by remember { mutableStateOf("") }

    val todayKey = remember { SimpleDateFormat("yyyyMMdd", Locale.UK).format(Date()) }
    val dayFormat = remember { SimpleDateFormat("yyyyMMdd", Locale.UK) }
    val visibleReceipts = remember(receipts, query, filter, todayKey) {
        val normalizedQuery = query.trim()
        receipts.filter { receipt ->
            val matchesQuery = normalizedQuery.isBlank() || listOf(
                receipt.id,
                receipt.clientName,
                receipt.venueName,
                receipt.processedBy,
                receipt.paymentMethod.label,
                receipt.voidReason.orEmpty()
            ).any { it.contains(normalizedQuery, ignoreCase = true) }
            val matchesFilter = when (filter) {
                LedgerFilter.ALL -> true
                LedgerFilter.TODAY -> dayFormat.format(Date(receipt.createdAt)) == todayKey
                LedgerFilter.UNSYNCED -> receipt.isBufferedOffline && !receipt.isVoided
                LedgerFilter.VOIDED -> receipt.isVoided
            }
            matchesQuery && matchesFilter
        }
    }

    val syncBusy = isSyncing || isWirelessSyncing
    val needsAttention = !gatewayOnline || unsyncedCount > 0 || pendingOrders.isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NaomiDarkBg)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag("sync_ledger_screen")
    ) {
        Text("Activity", color = NaomiTextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Black)
        Text(
            "Transactions, corrections and sync status.",
            color = NaomiTextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
        )

        SyncStatusCard(
            gatewayOnline = gatewayOnline,
            unsyncedCount = unsyncedCount,
            pendingCount = pendingOrders.size,
            busy = syncBusy,
            emphasized = needsAttention,
            onSync = { viewModel.syncGatewayNow() }
        )

        if (pendingOrders.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            IncomingOrdersCard(
                orders = pendingOrders,
                onPrint = { order -> viewModel.loadAndPrintWirelessOrder(order) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search transactions") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            items(LedgerFilter.values()) { item ->
                FilterChip(
                    selected = filter == item,
                    onClick = { filter = item },
                    label = {
                        Text(
                            item.name.lowercase().replaceFirstChar { it.titlecase() },
                            fontSize = 11.sp
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NaomiRed,
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.height(44.dp)
                )
            }
        }

        Text(
            if (receipts.isEmpty()) "No transactions" else "${visibleReceipts.size} transaction(s)",
            color = NaomiTextSecondary,
            fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 7.dp)
        )

        if (visibleReceipts.isEmpty()) {
            EmptyActivityState(
                title = if (receipts.isEmpty()) "No transactions yet" else "No transactions match this view",
                subtitle = if (receipts.isEmpty()) {
                    "Completed sales will appear here automatically."
                } else {
                    "Try another search or filter."
                },
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(visibleReceipts, key = { it.id }) { receipt ->
                    ReceiptActivityCard(
                        receipt = receipt,
                        showFinancials = showFinancials,
                        canVoid = canVoid,
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
            title = {
                Text("Void ${receipt.id}?", color = NaomiTextPrimary, fontWeight = FontWeight.Black)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "This keeps the transaction in Activity and records the staff member, reason and time.",
                        color = NaomiTextSecondary,
                        fontSize = 12.sp
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
                    enabled = canVoid && voidReason.trim().length >= 3,
                    colors = ButtonDefaults.buttonColors(containerColor = NaomiRed)
                ) {
                    Text("Void transaction")
                }
            },
            dismissButton = {
                TextButton(onClick = { voidTarget = null }) {
                    Text("Cancel", color = NaomiTextSecondary)
                }
            }
        )
    }
}

@Composable
private fun SyncStatusCard(
    gatewayOnline: Boolean,
    unsyncedCount: Int,
    pendingCount: Int,
    busy: Boolean,
    emphasized: Boolean,
    onSync: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = NaomiSurface),
        border = BorderStroke(
            1.dp,
            if (emphasized) NaomiOrange.copy(alpha = 0.45f) else NaomiBorder
        ),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        color = if (gatewayOnline) NaomiSuccess.copy(alpha = 0.10f) else NaomiOrange.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = if (gatewayOnline) Icons.Default.CloudDone else Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = if (gatewayOnline) NaomiSuccess else NaomiOrange,
                            modifier = Modifier.padding(9.dp).size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            if (gatewayOnline) "Sync ready" else "Working offline",
                            color = NaomiTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            when {
                                unsyncedCount > 0 -> "$unsyncedCount transaction(s) waiting to sync"
                                pendingCount > 0 -> "$pendingCount incoming order(s)"
                                gatewayOnline -> "Everything is up to date"
                                else -> "Sales stay safely on this PDA until connection returns"
                            },
                            color = NaomiTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            if (emphasized || busy) {
                Button(
                    onClick = onSync,
                    enabled = !busy,
                    colors = ButtonDefaults.buttonColors(containerColor = NaomiRed),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("sync_all_btn")
                ) {
                    if (busy) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(17.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(7.dp))
                        Text("Synchronising…", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(7.dp))
                        Text("Sync now", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun IncomingOrdersCard(
    orders: List<WirelessOrder>,
    onPrint: (WirelessOrder) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = NaomiSurface),
        border = BorderStroke(1.dp, NaomiOrange.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text("Incoming orders", color = NaomiTextPrimary, fontWeight = FontWeight.Black, fontSize = 15.sp)
            orders.take(4).forEach { order ->
                Surface(
                    color = NaomiSurfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, NaomiBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(11.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(order.clientName, color = NaomiTextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(
                                "${order.venueName} · ${ReceiptData.formatCurrency(order.grandTotal)}",
                                color = NaomiTextSecondary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Button(
                            onClick = { onPrint(order) },
                            colors = ButtonDefaults.buttonColors(containerColor = NaomiRed),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Print", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
            if (orders.size > 4) {
                Text("+ ${orders.size - 4} more queued", color = NaomiTextSecondary, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun EmptyActivityState(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Surface(color = NaomiSurfaceVariant, shape = RoundedCornerShape(18.dp)) {
                Icon(
                    Icons.Default.History,
                    contentDescription = null,
                    tint = NaomiTextSecondary,
                    modifier = Modifier.padding(14.dp).size(30.dp)
                )
            }
            Text(title, color = NaomiTextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(subtitle, color = NaomiTextSecondary, fontSize = 11.sp)
        }
    }
}

@Composable
private fun ReceiptActivityCard(
    receipt: ReceiptData,
    showFinancials: Boolean,
    canVoid: Boolean,
    onReprint: () -> Unit,
    onVoid: () -> Unit,
    onCorrect: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = NaomiSurface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (receipt.isVoided) NaomiOrange.copy(alpha = 0.55f) else NaomiBorder),
        modifier = Modifier.fillMaxWidth().testTag("receipt_item_${receipt.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(receipt.id, color = NaomiTextPrimary, fontWeight = FontWeight.Black, fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        ActivityStatusPill(
                            if (receipt.isVoided) "VOID" else if (receipt.isBufferedOffline) "PENDING" else "SYNCED",
                            receipt
                        )
                    }
                    Text(
                        "${receipt.clientName} · ${receipt.venueName}",
                        color = NaomiTextPrimary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${receipt.formattedDate()} · ${receipt.paymentMethod.label}",
                        color = NaomiTextSecondary,
                        fontSize = 11.sp
                    )
                    if (receipt.processedBy.isNotBlank()) {
                        Text("Processed by ${receipt.processedBy}", color = NaomiTextSecondary, fontSize = 10.sp)
                    }
                    receipt.replacesReceiptId?.let {
                        Text("Correction of $it", color = NaomiOrange, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    if (receipt.isVoided) {
                        Text(
                            "VOID: ${receipt.voidReason.orEmpty()} · ${receipt.voidedBy.orEmpty()}",
                            color = NaomiOrange,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    if (!showFinancials) "HIDDEN" else if (receipt.isEffectivelyFree) "FREE" else ReceiptData.formatCurrency(receipt.grandTotal),
                    color = if (!showFinancials || receipt.isVoided) NaomiTextSecondary else if (receipt.isEffectivelyFree) NaomiSuccess else NaomiTextPrimary,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                OutlinedButton(
                    onClick = onReprint,
                    enabled = !receipt.isVoided,
                    modifier = Modifier.weight(1f).height(44.dp)
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reprint", fontSize = 10.sp)
                }
                OutlinedButton(
                    onClick = onCorrect,
                    modifier = Modifier.weight(1f).height(44.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Correct", fontSize = 10.sp)
                }
                Button(
                    onClick = onVoid,
                    enabled = canVoid && !receipt.isVoided,
                    colors = ButtonDefaults.buttonColors(containerColor = NaomiDeepRed),
                    modifier = Modifier.weight(1f).height(44.dp)
                ) {
                    Icon(Icons.Default.VisibilityOff, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Void", fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun ActivityStatusPill(label: String, receipt: ReceiptData) {
    val color = when {
        receipt.isVoided -> NaomiOrange
        receipt.isBufferedOffline -> NaomiOrange
        else -> NaomiSuccess
    }
    Surface(shape = RoundedCornerShape(10.dp), color = color.copy(alpha = 0.12f)) {
        Text(
            label,
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 8.sp,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}
