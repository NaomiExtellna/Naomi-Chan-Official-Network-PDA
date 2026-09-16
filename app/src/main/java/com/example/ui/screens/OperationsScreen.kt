package com.example.ui.screens

import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Schedule
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.StaffAccount
import com.example.data.StaffShift
import com.example.model.PaymentMethod
import com.example.model.ReceiptData
import com.example.ui.AuthViewModel
import com.example.ui.PosViewModel
import com.example.ui.theme.NaomiBorder
import com.example.ui.theme.NaomiDarkBg
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

private enum class OpsMode { SHIFT, VENUES, STAFF, SYSTEM }

@Composable
fun OperationsScreen(
    authViewModel: AuthViewModel,
    posViewModel: PosViewModel,
    onStartReceipt: () -> Unit
) {
    val authState by authViewModel.state.collectAsState()
    val staffAccounts by authViewModel.staffAccounts.collectAsState()
    val shiftHistory by authViewModel.shiftHistory.collectAsState()
    val receipts by posViewModel.allReceipts.collectAsState()
    val printerStatus by posViewModel.printerStatus.collectAsState()
    val unsyncedCount by posViewModel.unsyncedCount.collectAsState()
    val ramInfo by posViewModel.ramInfo.collectAsState()
    val gatewayOnline by posViewModel.isWirelessOnline.collectAsState()
    val gatewayUrl by posViewModel.wirelessServerUrl.collectAsState()

    val user = authState.currentUser ?: return
    var mode by remember { mutableStateOf(OpsMode.SHIFT) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NaomiDarkBg)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = NaomiSurface),
            shape = RoundedCornerShape(0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("STAFF OPERATIONS", color = NaomiOrange, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(user.displayName, color = NaomiTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    }
                    RoleBadge(user)
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item { OpsChip(mode == OpsMode.SHIFT, "Shift", Icons.Default.Schedule) { mode = OpsMode.SHIFT } }
                    item { OpsChip(mode == OpsMode.VENUES, "Venues", Icons.Default.LocationOn) { mode = OpsMode.VENUES } }
                    item { OpsChip(mode == OpsMode.STAFF, "Staff", Icons.Default.Badge) { mode = OpsMode.STAFF } }
                    item { OpsChip(mode == OpsMode.SYSTEM, "System", Icons.Default.Memory) { mode = OpsMode.SYSTEM } }
                }
            }
        }

        when (mode) {
            OpsMode.SHIFT -> ShiftPanel(
                user = user,
                activeShift = authState.activeShift,
                shiftHistory = shiftHistory.filter { it.staffId == user.id },
                receipts = receipts,
                onOpenShift = authViewModel::openShift,
                onCloseShift = authViewModel::closeShift,
                csvData = posViewModel::exportReceiptsCsv,
                jsonData = posViewModel::exportReceiptsJson
            )
            OpsMode.VENUES -> VenueManagementScreen(
                viewModel = posViewModel,
                onStartReceipt = onStartReceipt
            )
            OpsMode.STAFF -> StaffPanel(
                user = user,
                accounts = staffAccounts,
                onSetActive = authViewModel::setStaffActive
            )
            OpsMode.SYSTEM -> SystemPanel(
                posViewModel = posViewModel,
                printerConnected = printerStatus.isConnected,
                printerName = printerStatus.deviceName,
                gatewayOnline = gatewayOnline,
                gatewayUrl = gatewayUrl,
                unsyncedCount = unsyncedCount,
                ramText = "${ramInfo.freeMb} MB free / ${ramInfo.totalMb} MB total",
                recentShiftCount = shiftHistory.size,
                onLogout = authViewModel::logout
            )
        }
    }
}

@Composable
private fun OpsChip(
    selected: Boolean,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontSize = 10.sp) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = NaomiRed,
            selectedLabelColor = Color.White,
            selectedLeadingIconColor = Color.White
        )
    )
}

@Composable
private fun RoleBadge(user: StaffAccount) {
    Surface(
        color = if (user.isAdmin) NaomiOrange.copy(alpha = 0.18f) else NaomiSurfaceVariant,
        shape = RoundedCornerShape(7.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (user.isAdmin) Icons.Default.AdminPanelSettings else Icons.Default.Badge,
                contentDescription = null,
                tint = if (user.isAdmin) NaomiOrange else NaomiTextSecondary,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                if (user.isAdmin) "ADMIN" else "STAFF",
                color = if (user.isAdmin) NaomiOrange else NaomiTextSecondary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun ShiftPanel(
    user: StaffAccount,
    activeShift: StaffShift?,
    shiftHistory: List<StaffShift>,
    receipts: List<ReceiptData>,
    onOpenShift: (String) -> Unit,
    onCloseShift: (String) -> Unit,
    csvData: () -> String,
    jsonData: () -> String
) {
    val context = LocalContext.current
    var note by remember { mutableStateOf("") }
    val summaryShift = activeShift ?: shiftHistory.firstOrNull()
    val shiftReceipts = summaryShift?.let { shift -> receipts.filter { it.shiftId == shift.id } }.orEmpty()
    val activeReceipts = shiftReceipts.filterNot { it.isVoided }
    val voidedReceipts = shiftReceipts.filter { it.isVoided }
    val gross = activeReceipts.sumOf { it.grandTotal }

    val csvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let { context.writeTextToUri(it, csvData()) }
    }
    val jsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { context.writeTextToUri(it, jsonData()) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CardBlock(
            title = if (activeShift == null) "No Open Shift" else "Shift Open",
            subtitle = if (activeShift == null) {
                "Open a shift before taking event transactions so receipts are grouped in the end-of-shift report."
            } else {
                "Opened ${formatDateTime(activeShift.openedAt)} • Operator: ${user.displayName}"
            }
        ) {
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(if (activeShift == null) "Opening note (optional)" else "Closing note (optional)") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    if (activeShift == null) onOpenShift(note) else onCloseShift(note)
                    note = ""
                },
                colors = ButtonDefaults.buttonColors(containerColor = if (activeShift == null) NaomiRed else NaomiOrange),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (activeShift == null) "Open Shift" else "Close Shift", fontWeight = FontWeight.Black)
            }
        }

        CardBlock(
            title = if (summaryShift?.isOpen == true) "Current Shift Summary" else "Last Shift Summary",
            subtitle = summaryShift?.let {
                val closed = it.closedAt?.let(::formatDateTime) ?: "OPEN"
                "${formatDateTime(it.openedAt)} → $closed • ${it.staffDisplayName}"
            } ?: "No shift history yet."
        ) {
            SummaryLine("Receipts", activeReceipts.size.toString())
            SummaryLine("Voided", voidedReceipts.size.toString())
            SummaryLine("Recorded gross", ReceiptData.formatCurrency(gross), emphasize = true)
            PaymentMethod.values().forEach { method ->
                val total = activeReceipts.filter { it.paymentMethod == method }.sumOf { it.grandTotal }
                if (total > 0.0 || activeReceipts.any { it.paymentMethod == method }) {
                    SummaryLine(method.label, ReceiptData.formatCurrency(total))
                }
            }
            Text(
                "These are recorded receipt values; they do not independently confirm that an external card/bank payment settled.",
                color = NaomiTextSecondary,
                fontSize = 9.sp
            )
        }

        if (shiftHistory.isNotEmpty()) {
            CardBlock("Recent Shifts", "Most recent shift records for ${user.displayName}.") {
                shiftHistory.take(5).forEach { shift ->
                    SummaryLine(
                        formatDateTime(shift.openedAt),
                        shift.closedAt?.let { "Closed ${formatDateTime(it)}" } ?: "OPEN"
                    )
                }
            }
        }

        CardBlock("Export & Backup", "Save the local receipt ledger for accounting or recovery.") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { csvLauncher.launch("naomi-pos-receipts.csv") },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.SaveAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("CSV")
                }
                OutlinedButton(
                    onClick = { jsonLauncher.launch("naomi-pos-backup.json") },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("JSON")
                }
            }
        }
    }
}

@Composable
private fun StaffPanel(
    user: StaffAccount,
    accounts: List<StaffAccount>,
    onSetActive: (String, Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (!user.isAdmin) {
            CardBlock("Administrator Only", "Only Naomi can enable or disable staff accounts.") {
                Text("You are signed in as ${user.displayName}.", color = NaomiTextPrimary)
            }
            return@Column
        }

        Text(
            "NAOMI ADMIN • ACCOUNT MANAGEMENT",
            color = NaomiOrange,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(accounts, key = { it.id }) { account ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = NaomiSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NaomiBorder),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(account.displayName, color = NaomiTextPrimary, fontWeight = FontWeight.Black)
                            Text("@${account.username} • ${account.role.name}", color = NaomiTextSecondary, fontSize = 10.sp)
                            Text(
                                if (account.isActive) "ACTIVE" else "DISABLED",
                                color = if (account.isActive) NaomiSuccess else NaomiOrange,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        if (account.id != user.id) {
                            Button(
                                onClick = { onSetActive(account.id, !account.isActive) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (account.isActive) NaomiSurfaceVariant else NaomiRed
                                )
                            ) {
                                Text(if (account.isActive) "Disable" else "Enable", fontSize = 10.sp)
                            }
                        } else {
                            Text("YOU", color = NaomiOrange, fontWeight = FontWeight.Black, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SystemPanel(
    posViewModel: PosViewModel,
    printerConnected: Boolean,
    printerName: String,
    gatewayOnline: Boolean,
    gatewayUrl: String,
    unsyncedCount: Int,
    ramText: String,
    recentShiftCount: Int,
    onLogout: () -> Unit
) {
    var gatewayDraft by remember(gatewayUrl) { mutableStateOf(gatewayUrl) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CardBlock("System Diagnostics", "Useful information when troubleshooting the PDA.") {
            SummaryLine("Device", "${Build.MANUFACTURER} ${Build.MODEL}")
            SummaryLine("Android", "${Build.VERSION.RELEASE} / API ${Build.VERSION.SDK_INT}")
            SummaryLine("Printer", if (printerConnected) "Connected • $printerName" else "Offline • $printerName")
            SummaryLine("Gateway", if (gatewayOnline) "Online" else "Offline")
            SummaryLine("Unsynced receipts", unsyncedCount.toString())
            SummaryLine("Memory", ramText)
            SummaryLine("Shift records", recentShiftCount.toString())
            Button(
                onClick = {
                    posViewModel.refreshRamInfo()
                    posViewModel.checkWirelessConnection()
                },
                colors = ButtonDefaults.buttonColors(containerColor = NaomiSurfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Refresh Diagnostics")
            }
        }

        CardBlock("Wireless Gateway", "Local Flask gateway used for receipt/order sync.") {
            OutlinedTextField(
                value = gatewayDraft,
                onValueChange = { gatewayDraft = it },
                label = { Text("Gateway URL") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { posViewModel.setWirelessServerUrl(gatewayDraft) },
                colors = ButtonDefaults.buttonColors(containerColor = NaomiRed),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save & Test Gateway")
            }
        }

        Button(
            onClick = onLogout,
            colors = ButtonDefaults.buttonColors(containerColor = NaomiRed),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Lock, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Lock PDA / Sign Out", fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun CardBlock(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = NaomiSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, NaomiBorder),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Text(title, color = NaomiTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Black)
            Text(subtitle, color = NaomiTextSecondary, fontSize = 10.sp)
            content()
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String, emphasize: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = NaomiTextSecondary, fontSize = 11.sp)
        Text(
            value,
            color = if (emphasize) NaomiOrange else NaomiTextPrimary,
            fontSize = if (emphasize) 15.sp else 11.sp,
            fontWeight = if (emphasize) FontWeight.Black else FontWeight.Bold
        )
    }
}

private fun formatDateTime(timestamp: Long): String =
    SimpleDateFormat("dd MMM yyyy HH:mm", Locale.UK).format(Date(timestamp))

private fun Context.writeTextToUri(uri: android.net.Uri, text: String) {
    runCatching {
        contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { writer ->
            writer.write(text)
        }
    }
}
