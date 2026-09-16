package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Schedule
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
import com.example.ui.theme.NaomiSurface
import com.example.ui.theme.NaomiSurfaceVariant
import com.example.ui.theme.NaomiTextPrimary
import com.example.ui.theme.NaomiTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class OpsMode { SHIFT, VENUES, STAFF, AUDIT, SYSTEM }

@Composable
fun OperationsScreen(
    authViewModel: AuthViewModel,
    posViewModel: PosViewModel,
    onStartReceipt: () -> Unit
) {
    val authState by authViewModel.state.collectAsState()
    val shiftHistory by authViewModel.shiftHistory.collectAsState()
    val receipts by posViewModel.allReceipts.collectAsState()
    val user = authState.currentUser ?: return
    var mode by remember { mutableStateOf(OpsMode.SHIFT) }

    Column(modifier = Modifier.fillMaxSize().background(NaomiDarkBg)) {
        Card(colors = CardDefaults.cardColors(containerColor = NaomiSurface), shape = RoundedCornerShape(0.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
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
                    if (user.isAdmin) item { OpsChip(mode == OpsMode.AUDIT, "Audit", Icons.Default.History) { mode = OpsMode.AUDIT } }
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
                onCloseShift = authViewModel::closeShift
            )
            OpsMode.VENUES -> {
                if (user.canManageVenues) VenueManagementScreen(posViewModel, onStartReceipt)
                else PermissionDenied("Venue editing", "Naomi can grant Edit Venues permission from Ops → Staff.")
            }
            OpsMode.STAFF -> StaffManagementScreen(authViewModel)
            OpsMode.AUDIT -> AuditLogScreen(authViewModel)
            OpsMode.SYSTEM -> SystemMaintenanceScreen(authViewModel, posViewModel)
        }
    }
}

@Composable
private fun OpsChip(selected: Boolean, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
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
    Surface(color = if (user.isAdmin) NaomiOrange.copy(alpha = 0.18f) else NaomiSurfaceVariant, shape = RoundedCornerShape(7.dp)) {
        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (user.isAdmin) Icons.Default.AdminPanelSettings else Icons.Default.Badge, contentDescription = null, tint = if (user.isAdmin) NaomiOrange else NaomiTextSecondary, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(if (user.isAdmin) "ADMIN" else "STAFF", color = if (user.isAdmin) NaomiOrange else NaomiTextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Black)
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
    onCloseShift: (String) -> Unit
) {
    var note by remember { mutableStateOf("") }
    val summaryShift = activeShift ?: shiftHistory.firstOrNull()
    val shiftReceipts = summaryShift?.let { shift -> receipts.filter { it.shiftId == shift.id } }.orEmpty()
    val activeReceipts = shiftReceipts.filterNot { it.isVoided || it.receiptStatus.equals("ARCHIVED", true) }
    val voidedReceipts = shiftReceipts.filter { it.isVoided }
    val gross = activeReceipts.sumOf { it.grandTotal }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CardBlock(
            title = if (activeShift == null) "No Open Shift" else "Shift Open",
            subtitle = if (activeShift == null) "A shift is required before any new ticket/receipt can be finalised." else "Opened ${formatDateTime(activeShift.openedAt)} • Operator: ${user.displayName}"
        ) {
            androidx.compose.material3.OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(if (activeShift == null) "Opening note (optional)" else "Closing note (optional)") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { if (activeShift == null) onOpenShift(note) else onCloseShift(note); note = "" },
                colors = ButtonDefaults.buttonColors(containerColor = if (activeShift == null) NaomiRed else NaomiOrange),
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (activeShift == null) "Open Shift" else "Close Shift", fontWeight = FontWeight.Black) }
        }

        CardBlock(
            title = if (summaryShift?.isOpen == true) "Current Shift Summary" else "Last Shift Summary",
            subtitle = summaryShift?.let { "${formatDateTime(it.openedAt)} → ${it.closedAt?.let(::formatDateTime) ?: "OPEN"} • ${it.staffDisplayName}" } ?: "No shift history yet."
        ) {
            SummaryLine("Receipts", activeReceipts.size.toString())
            SummaryLine("Voided", voidedReceipts.size.toString())
            if (user.canViewFinancialTotals) {
                SummaryLine("Recorded gross", ReceiptData.formatCurrency(gross), emphasize = true)
                PaymentMethod.values().forEach { method ->
                    val total = activeReceipts.filter { it.paymentMethod == method }.sumOf { it.grandTotal }
                    if (total > 0.0 || activeReceipts.any { it.paymentMethod == method }) SummaryLine(method.label, ReceiptData.formatCurrency(total))
                }
                Text("Recorded values do not independently prove external payment settlement.", color = NaomiTextSecondary, fontSize = 9.sp)
            } else {
                Text("Financial totals are hidden for this account.", color = NaomiTextSecondary, fontSize = 9.sp)
            }
        }

        if (shiftHistory.isNotEmpty()) {
            CardBlock("Recent Shifts", "Most recent shift records for ${user.displayName}.") {
                shiftHistory.take(5).forEach { shift -> SummaryLine(formatDateTime(shift.openedAt), shift.closedAt?.let { "Closed ${formatDateTime(it)}" } ?: "OPEN") }
            }
        }
    }
}

@Composable
private fun PermissionDenied(title: String, text: String) {
    Column(modifier = Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = NaomiTextPrimary, fontSize = 19.sp, fontWeight = FontWeight.Black)
        Text(text, color = NaomiTextSecondary)
    }
}

@Composable
private fun CardBlock(title: String, subtitle: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = NaomiSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, NaomiBorder),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text(title, color = NaomiTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Black)
            Text(subtitle, color = NaomiTextSecondary, fontSize = 10.sp)
            content()
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String, emphasize: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = NaomiTextSecondary, fontSize = 11.sp)
        Text(value, color = if (emphasize) NaomiOrange else NaomiTextPrimary, fontSize = if (emphasize) 15.sp else 11.sp, fontWeight = if (emphasize) FontWeight.Black else FontWeight.Bold)
    }
}

private fun formatDateTime(timestamp: Long): String = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.UK).format(Date(timestamp))
