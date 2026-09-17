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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
private enum class ActivityMode { TRANSACTIONS, INCOMING }
private const val RECEIPTS_PER_PAGE = 1
private const val INCOMING_PER_PAGE = 2

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
    var mode by remember { mutableStateOf(ActivityMode.TRANSACTIONS) }
    var receiptPage by remember { mutableIntStateOf(0) }
    var incomingPage by remember { mutableIntStateOf(0) }
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

    val receiptPages = pageCount(visibleReceipts.size, RECEIPTS_PER_PAGE)
    val incomingPages = pageCount(pendingOrders.size, INCOMING_PER_PAGE)
    val receiptSlice = pageSlice(visibleReceipts, receiptPage, RECEIPTS_PER_PAGE)
    val incomingSlice = pageSlice(pendingOrders, incomingPage, INCOMING_PER_PAGE)
    val syncBusy = isSyncing || isWirelessSyncing
    val needsAttention = !gatewayOnline || unsyncedCount > 0 || pendingOrders.isNotEmpty()

    LaunchedEffect(query, filter, visibleReceipts.size) {
        receiptPage = receiptPage.coerceIn(0, receiptPages - 1)
    }
    LaunchedEffect(pendingOrders.size) {
        incomingPage = incomingPage.coerceIn(0, incomingPages - 1)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NaomiDarkBg)
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag("sync_ledger_screen"),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("ACTIVITY", color = NaomiOrange, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                Text("Transactions", color = NaomiTextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Black)
            }
            Surface(
                color = if (gatewayOnline) NaomiSuccess.copy(alpha = 0.10f) else NaomiOrange.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, if (gatewayOnline) NaomiSuccess.copy(alpha = 0.30f) else NaomiOrange.copy(alpha = 0.35f)),
                shape = RoundedCornerShape(5.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        if (gatewayOnline) Icons.Default.CloudDone else Icons.Default.CloudOff,
                        contentDescription = null,
                        tint = if (gatewayOnline) NaomiSuccess else NaomiOrange,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        if (gatewayOnline) "ONLINE" else "OFFLINE",
                        color = if (gatewayOnline) NaomiSuccess else NaomiOrange,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        CompactSyncBar(
            gatewayOnline = gatewayOnline,
            unsyncedCount = unsyncedCount,
            pendingCount = pendingOrders.size,
            busy = syncBusy,
            emphasized = needsAttention,
            onSync = { viewModel.syncGatewayNow() }
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            FilterChip(
                selected = mode == ActivityMode.TRANSACTIONS,
                onClick = { mode = ActivityMode.TRANSACTIONS },
                label = { Text("Transactions", fontSize = 10.sp) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = NaomiRed, selectedLabelColor = Color.White),
                modifier = Modifier.weight(1f).height(38.dp)
            )
            FilterChip(
                selected = mode == ActivityMode.INCOMING,
                onClick = { mode = ActivityMode.INCOMING },
                label = { Text("Incoming (${pendingOrders.size})", fontSize = 10.sp) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = NaomiRed, selectedLabelColor = Color.White),
                modifier = Modifier.weight(1f).height(38.dp)
            )
        }

        if (mode == ActivityMode.TRANSACTIONS) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it; receiptPage = 0 },
                label = { Text("Search transactions") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                LedgerFilter.values().forEach { item ->
                    FilterChip(
                        selected = filter == item,
                        onClick = { filter = item; receiptPage = 0 },
                        label = {
                            Text(
                                when (item) {
                                    LedgerFilter.ALL -> "All"
                                    LedgerFilter.TODAY -> "Today"
                                    LedgerFilter.UNSYNCED -> "Pending"
                                    LedgerFilter.VOIDED -> "Voided"
                                },
                                fontSize = 9.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = NaomiRed, selectedLabelColor = Color.White),
                        modifier = Modifier.weight(1f).height(36.dp)
                    )
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (visibleReceipts.isEmpty()) {
                    EmptyActivityState(
                        title = if (receipts.isEmpty()) "No transactions yet" else "No matching transactions",
                        subtitle = if (receipts.isEmpty()) "Completed sales appear here automatically." else "Change the search or filter.",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                        receiptSlice.forEach { receipt ->
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

            PosPager(
                page = receiptPage,
                totalPages = receiptPages,
                onPrevious = { receiptPage = (receiptPage - 1).coerceAtLeast(0) },
                onNext = { receiptPage = (receiptPage + 1).coerceAtMost(receiptPages - 1) },
                label = "TXN"
            )
        } else {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (pendingOrders.isEmpty()) {
                    EmptyActivityState(
                        title = "No incoming orders",
                        subtitle = "Wireless orders waiting to print will appear here.",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
                    ) {
                        incomingSlice.forEach { order ->
                            IncomingOrderRow(order) { viewModel.loadAndPrintWirelessOrder(order) }
                        }
                    }
                }
            }
            PosPager(
                page = incomingPage,
                totalPages = incomingPages,
                onPrevious = { incomingPage = (incomingPage - 1).coerceAtLeast(0) },
                onNext = { incomingPage = (incomingPage + 1).coerceAtMost(incomingPages - 1) },
                label = "ORDER"
            )
        }
    }

    voidTarget?.let { receipt ->
        AlertDialog(
            onDismissRequest = { voidTarget = null },
            containerColor = NaomiSurface,
            title = { Text("Void ${receipt.id}?", color = NaomiTextPrimary, fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                ) { Text("Void transaction") }
            },
            dismissButton = {
                TextButton(onClick = { voidTarget = null }) { Text("Cancel", color = NaomiTextSecondary) }
            }
        )
    }
}

@Composable
private fun CompactSyncBar(
    gatewayOnline: Boolean,
    unsyncedCount: Int,
    pendingCount: Int,
    busy: Boolean,
    emphasized: Boolean,
    onSync: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = NaomiSurface),
        border = BorderStroke(1.dp, if (emphasized) NaomiOrange.copy(alpha = 0.45f) else NaomiBorder),
        shape = RoundedCornerShape(5.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    when {
                        unsyncedCount > 0 -> "$unsyncedCount waiting to sync"
                        pendingCount > 0 -> "$pendingCount incoming order(s)"
                        gatewayOnline -> "Everything up to date"
                        else -> "Offline · sales stay on this PDA"
                    },
                    color = NaomiTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Button(
                onClick = onSync,
                enabled = !busy,
                colors = ButtonDefaults.buttonColors(containerColor = if (emphasized) NaomiRed else NaomiSurfaceVariant),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.height(36.dp).testTag("sync_all_btn")
            ) {
                if (busy) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(15.dp), tint = if (emphasized) Color.White else NaomiOrange)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Sync", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (emphasized) Color.White else NaomiTextPrimary)
                }
            }
        }
    }
}

@Composable
private fun IncomingOrderRow(order: WirelessOrder, onPrint: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = NaomiSurface),
        border = BorderStroke(1.dp, NaomiOrange.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(5.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(order.clientName, color = NaomiTextPrimary, fontWeight = FontWeight.Black, fontSize = 12.sp)
                Text(
                    "${order.venueName} · ${ReceiptData.formatCurrency(order.grandTotal)}",
                    color = NaomiTextSecondary,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Button(
                onClick = onPrint,
                colors = ButtonDefaults.buttonColors(containerColor = NaomiRed),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.height(40.dp)
            ) {
                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Print", fontSize = 9.sp, fontWeight = FontWeight.Bold)
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
                    modifier = Modifier.padding(13.dp).size(28.dp)
                )
            }
            Text(title, color = NaomiTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(subtitle, color = NaomiTextSecondary, fontSize = 10.sp)
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
        shape = RoundedCornerShape(5.dp),
        border = BorderStroke(1.dp, if (receipt.isVoided) NaomiOrange.copy(alpha = 0.55f) else NaomiBorder),
        modifier = Modifier.fillMaxWidth().testTag("receipt_item_${receipt.id}")
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(receipt.id, color = NaomiTextPrimary, fontWeight = FontWeight.Black, fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(5.dp))
                        ActivityStatusPill(
                            if (receipt.isVoided) "VOID" else if (receipt.isBufferedOffline) "PENDING" else "SYNCED",
                            receipt
                        )
                    }
                    Text(
                        "${receipt.clientName} · ${receipt.venueName}",
                        color = NaomiTextPrimary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text("${receipt.formattedDate()} · ${receipt.paymentMethod.label}", color = NaomiTextSecondary, fontSize = 9.sp)
                    if (receipt.processedBy.isNotBlank()) {
                        Text("Processed by ${receipt.processedBy}", color = NaomiTextSecondary, fontSize = 9.sp)
                    }
                    receipt.replacesReceiptId?.let {
                        Text("Correction of $it", color = NaomiOrange, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                    if (receipt.isVoided) {
                        Text(
                            "VOID: ${receipt.voidReason.orEmpty()} · ${receipt.voidedBy.orEmpty()}",
                            color = NaomiOrange,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Text(
                    if (!showFinancials) "HIDDEN" else if (receipt.isEffectivelyFree) "FREE" else ReceiptData.formatCurrency(receipt.grandTotal),
                    color = if (!showFinancials || receipt.isVoided) NaomiTextSecondary else if (receipt.isEffectivelyFree) NaomiSuccess else NaomiTextPrimary,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(onClick = onReprint, enabled = !receipt.isVoided, modifier = Modifier.weight(1f).height(40.dp)) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Reprint", fontSize = 9.sp)
                }
                OutlinedButton(onClick = onCorrect, modifier = Modifier.weight(1f).height(40.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Correct", fontSize = 9.sp)
                }
                Button(
                    onClick = onVoid,
                    enabled = canVoid && !receipt.isVoided,
                    colors = ButtonDefaults.buttonColors(containerColor = NaomiDeepRed),
                    modifier = Modifier.weight(1f).height(40.dp)
                ) {
                    Icon(Icons.Default.VisibilityOff, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Void", fontSize = 9.sp)
                }
            }
        }
    }
}

@Composable
private fun ActivityStatusPill(label: String, receipt: ReceiptData) {
    val color = if (receipt.isVoided || receipt.isBufferedOffline) NaomiOrange else NaomiSuccess
    Surface(shape = RoundedCornerShape(4.dp), color = color.copy(alpha = 0.12f)) {
        Text(
            label,
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 7.sp,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
        )
    }
}