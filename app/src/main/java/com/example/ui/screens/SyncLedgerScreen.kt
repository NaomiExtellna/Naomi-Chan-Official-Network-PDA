package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.ReceiptData
import com.example.ui.PosViewModel
import com.example.ui.theme.NaomiBorder
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

private enum class LedgerFilter { ALL, TODAY, VOIDED }
private const val RECEIPTS_PER_PAGE = 1

@Composable
fun SyncLedgerScreen(
    viewModel: PosViewModel,
    showFinancials: Boolean = true,
    canVoid: Boolean = true,
    canExport: Boolean = true,
    onEditCorrection: () -> Unit = {}
) {
    val receipts by viewModel.allReceipts.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(LedgerFilter.ALL) }
    var page by remember { mutableIntStateOf(0) }
    var voidTarget by remember { mutableStateOf<ReceiptData?>(null) }
    var voidReason by remember { mutableStateOf("") }

    val dayFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.UK) }
    val todayKey = remember { dayFormat.format(Date()) }

    val visibleReceipts = remember(receipts, query, filter, todayKey) {
        receipts.filter { receipt ->
            val needle = query.trim()
            val matchesQuery = needle.isBlank() || listOf(
                receipt.id,
                receipt.clientName,
                receipt.clientContact,
                receipt.venueName,
                receipt.paymentMethod.label,
                receipt.processedBy
            ).any { it.contains(needle, ignoreCase = true) }

            val matchesFilter = when (filter) {
                LedgerFilter.ALL -> true
                LedgerFilter.TODAY -> dayFormat.format(Date(receipt.createdAt)) == todayKey
                LedgerFilter.VOIDED -> receipt.isVoided
            }
            matchesQuery && matchesFilter
        }
    }

    val pages = pageCount(visibleReceipts.size, RECEIPTS_PER_PAGE)
    val slice = pageSlice(visibleReceipts, page, RECEIPTS_PER_PAGE)

    LaunchedEffect(query, filter, visibleReceipts.size) {
        page = page.coerceIn(0, pages - 1)
    }

    val csvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val csv = viewModel.exportReceiptsCsv()
        if (csv.isBlank()) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                writer.write(csv)
            }
        }.onSuccess {
            viewModel.recordExport("CSV")
            viewModel.logAction("CSV_EXPORTED", "activity", "receipts=${receipts.size}")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(com.example.ui.theme.NaomiDarkBg)
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .testTag("sync_ledger_screen"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "ACTIVITY",
                    color = NaomiOrange,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(
                    "Local transactions",
                    color = NaomiTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    "${receipts.size} stored on this PDA",
                    color = NaomiTextSecondary,
                    fontSize = 10.sp
                )
            }

            Button(
                onClick = { csvLauncher.launch("naomi-pos-receipts.csv") },
                enabled = canExport && receipts.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = NaomiRed),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(42.dp).testTag("export_csv_btn")
            ) {
                Icon(Icons.Default.SaveAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(5.dp))
                Text("CSV", fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
        }

        Surface(
            color = NaomiSuccess.copy(alpha = 0.08f),
            border = BorderStroke(1.dp, NaomiSuccess.copy(alpha = 0.24f)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "Standalone mode · transactions remain local until you export a CSV file",
                color = NaomiTextPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp)
            )
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it; page = 0 },
            label = { Text("Search transactions") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().height(54.dp)
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            LedgerFilter.entries.forEach { item ->
                FilterChip(
                    selected = filter == item,
                    onClick = { filter = item; page = 0 },
                    label = {
                        Text(
                            when (item) {
                                LedgerFilter.ALL -> "All"
                                LedgerFilter.TODAY -> "Today"
                                LedgerFilter.VOIDED -> "Voided"
                            },
                            fontSize = 10.sp
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NaomiRed,
                        selectedLabelColor = androidx.compose.ui.graphics.Color.White
                    ),
                    modifier = Modifier.weight(1f).height(40.dp)
                )
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (visibleReceipts.isEmpty()) {
                EmptyActivityState(
                    title = if (receipts.isEmpty()) "No transactions yet" else "No matching transactions",
                    subtitle = if (receipts.isEmpty()) {
                        "Completed sales are stored locally and appear here."
                    } else {
                        "Change the search or filter."
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                    slice.forEach { receipt ->
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
            page = page,
            totalPages = pages,
            onPrevious = { page = (page - 1).coerceAtLeast(0) },
            onNext = { page = (page + 1).coerceAtMost(pages - 1) },
            label = "TXN"
        )
    }

    voidTarget?.let { receipt ->
        AlertDialog(
            onDismissRequest = { voidTarget = null },
            containerColor = NaomiSurface,
            title = { Text("Void ${receipt.id}?", color = NaomiTextPrimary, fontWeight = FontWeight.Black) },
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
                ) { Text("Void transaction") }
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
private fun EmptyActivityState(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(color = NaomiSurfaceVariant, shape = RoundedCornerShape(18.dp)) {
                Icon(
                    Icons.Default.History,
                    contentDescription = null,
                    tint = NaomiTextSecondary,
                    modifier = Modifier.padding(13.dp).size(28.dp)
                )
            }
            Text(title, color = NaomiTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            1.dp,
            if (receipt.isVoided) NaomiOrange.copy(alpha = 0.55f) else NaomiBorder
        ),
        modifier = Modifier.fillMaxWidth().testTag("receipt_item_${receipt.id}")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            receipt.id,
                            color = NaomiTextPrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        ActivityStatusPill(
                            if (receipt.isVoided) "VOID" else if (receipt.isPrinted) "PRINTED" else "LOCAL",
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
                    Text(
                        "${receipt.formattedDate()} · ${receipt.paymentMethod.label}",
                        color = NaomiTextSecondary,
                        fontSize = 10.sp
                    )
                    if (receipt.processedBy.isNotBlank()) {
                        Text(
                            "Processed by ${receipt.processedBy}",
                            color = NaomiTextSecondary,
                            fontSize = 10.sp
                        )
                    }
                    receipt.replacesReceiptId?.let {
                        Text(
                            "Correction of $it",
                            color = NaomiOrange,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (receipt.isVoided) {
                        Text(
                            "VOID: ${receipt.voidReason.orEmpty()} · ${receipt.voidedBy.orEmpty()}",
                            color = NaomiOrange,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Text(
                    if (!showFinancials) "HIDDEN"
                    else if (receipt.isEffectivelyFree) "FREE"
                    else ReceiptData.formatCurrency(receipt.grandTotal),
                    color = if (!showFinancials || receipt.isVoided) {
                        NaomiTextSecondary
                    } else if (receipt.isEffectivelyFree) {
                        NaomiSuccess
                    } else {
                        NaomiTextPrimary
                    },
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onReprint,
                    enabled = !receipt.isVoided,
                    modifier = Modifier.weight(1f).height(44.dp)
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Reprint", fontSize = 10.sp)
                }
                OutlinedButton(
                    onClick = onCorrect,
                    modifier = Modifier.weight(1f).height(44.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Correct", fontSize = 10.sp)
                }
                Button(
                    onClick = onVoid,
                    enabled = canVoid && !receipt.isVoided,
                    colors = ButtonDefaults.buttonColors(containerColor = NaomiDeepRed),
                    modifier = Modifier.weight(1f).height(44.dp)
                ) {
                    Icon(Icons.Default.VisibilityOff, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Void", fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun ActivityStatusPill(label: String, receipt: ReceiptData) {
    val color = if (receipt.isVoided) NaomiOrange else NaomiSuccess
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            label,
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
        )
    }
}
