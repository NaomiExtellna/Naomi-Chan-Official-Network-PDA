package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
        Card(
            colors = CardDefaults.cardColors(containerColor = NaomiSurface),
            shape = RoundedCornerShape(0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("STAFF OPERATIONS", color = NaomiOrange, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                        Text(user.displayName, color = NaomiTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    RoleBadge(user)
                }

                val modes = if (user.isAdmin) {
                    listOf(OpsMode.SHIFT, OpsMode.VENUES, OpsMode.STAFF, OpsMode.AUDIT, OpsMode.SYSTEM)
                } else {
                    listOf(OpsMode.SHIFT, OpsMode.VENUES, OpsMode.STAFF, OpsMode.SYSTEM)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    modes.forEach { item ->
                        OpsChip(
                            selected = mode == item,
                            label = when (item) {
                                OpsMode.SHIFT -> "Shift"
                                OpsMode.VENUES -> "Venues"
                                OpsMode.STAFF -> "Staff"
                                OpsMode.AUDIT -> "Audit"
                                OpsMode.SYSTEM -> "System"
                            },
                            onClick = { mode = item },
                            modifier = Modifier.weight(1f)
                        )
                    }
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
private fun OpsChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontSize = 9.sp, fontWeight = if (selected) FontWeight.Black else FontWeight.Bold) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = NaomiRed,
            selectedLabelColor = Color.White
        ),
        modifier = modifier.height(40.dp)
    )
}

@Composable
private fun RoleBadge(user: StaffAccount) {
    Surface(
        color = if (user.isAdmin) NaomiOrange.copy(alpha = 0.14f) else NaomiSurfaceVariant,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (user.isAdmin) Icons.Default.AdminPanelSettings else Icons.Default.Badge,
                contentDescription = null,
                tint = if (user.isAdmin) NaomiOrange else NaomiTextSecondary,
                modifier = Modifier.size(14.dp)
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
    onCloseShift: (String) -> Unit
) {
    var note by remember { mutableStateOf("") }
    val summaryShift = activeShift ?: shiftHistory.firstOrNull()
    val shiftReceipts = summaryShift?.let { shift -> receipts.filter { it.shiftId == shift.id } }.orEmpty()
    val activeReceipts = shiftReceipts.filterNot { it.isVoided || it.receiptStatus.equals("ARCHIVED", true) }
    val voidedReceipts = shiftReceipts.filter { it.isVoided }
    val gross = activeReceipts.sumOf { it.grandTotal }
    val paymentTotals = PaymentMethod.values().mapNotNull { method ->
        val total = activeReceipts.filter { it.paymentMethod == method }.sumOf { it.grandTotal }
        if (total > 0.0 || activeReceipts.any { it.paymentMethod == method }) method.label to total else null
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = NaomiSurface),
            border = BorderStroke(1.dp, NaomiBorder),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (activeShift == null) "NO OPEN SHIFT" else "SHIFT OPEN",
                            color = if (activeShift == null) NaomiOrange else com.example.ui.theme.NaomiSuccess,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            activeShift?.let { "Since ${formatDateTime(it.openedAt)}" } ?: "Open a shift before finalising sales",
                            color = NaomiTextSecondary,
                            fontSize = 9.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text(if (activeShift == null) "Opening note" else "Closing note", fontSize = 10.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f).height(54.dp)
                    )
                    Button(
                        onClick = {
                            if (activeShift == null) onOpenShift(note) else onCloseShift(note)
                            note = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (activeShift == null) NaomiRed else NaomiOrange),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Text(if (activeShift == null) "OPEN" else "CLOSE", fontWeight = FontWeight.Black, fontSize = 10.sp)
                    }
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = NaomiSurface),
            border = BorderStroke(1.dp, NaomiBorder),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    if (summaryShift?.isOpen == true) "CURRENT SHIFT SUMMARY" else "LAST SHIFT SUMMARY",
                    color = NaomiOrange,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    summaryShift?.let { "${formatDateTime(it.openedAt)} → ${it.closedAt?.let(::formatDateTime) ?: "OPEN"}" } ?: "No shift history yet",
                    color = NaomiTextSecondary,
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ShiftMetric("Receipts", activeReceipts.size.toString(), Modifier.weight(1f))
                    ShiftMetric("Voided", voidedReceipts.size.toString(), Modifier.weight(1f))
                    ShiftMetric("Role", if (user.isAdmin) "Admin" else "Staff", Modifier.weight(1f))
                }

                if (user.canViewFinancialTotals) {
                    ShiftMetric("Recorded gross", ReceiptData.formatCurrency(gross), Modifier.fillMaxWidth(), emphasize = true)
                    paymentTotals.take(3).forEach { (label, total) ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(label, color = NaomiTextSecondary, fontSize = 9.sp)
                            Text(ReceiptData.formatCurrency(total), color = NaomiTextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (paymentTotals.size > 3) {
                        Text("+ ${paymentTotals.size - 3} additional payment method(s)", color = NaomiTextSecondary, fontSize = 9.sp)
                    }
                } else {
                    Text("Financial totals hidden for this account.", color = NaomiTextSecondary, fontSize = 9.sp)
                }

                if (shiftHistory.isNotEmpty()) {
                    Surface(color = NaomiSurfaceVariant, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth().weight(1f)) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(8.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("RECENT SHIFTS", color = NaomiTextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Black)
                            shiftHistory.take(2).forEach { shift ->
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(formatDateTime(shift.openedAt), color = NaomiTextPrimary, fontSize = 9.sp)
                                    Text(shift.closedAt?.let { "Closed ${formatDateTime(it)}" } ?: "OPEN", color = NaomiTextSecondary, fontSize = 9.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShiftMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    emphasize: Boolean = false
) {
    Surface(
        modifier = modifier,
        color = if (emphasize) NaomiOrange.copy(alpha = 0.08f) else NaomiSurfaceVariant,
        shape = RoundedCornerShape(10.dp),
        border = if (emphasize) BorderStroke(1.dp, NaomiOrange.copy(alpha = 0.30f)) else null
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(label.uppercase(), color = NaomiTextSecondary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Text(value, color = if (emphasize) NaomiOrange else NaomiTextPrimary, fontSize = if (emphasize) 12.sp else 9.sp, fontWeight = FontWeight.Black, maxLines = 1)
        }
    }
}

@Composable
private fun PermissionDenied(title: String, text: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(18.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, color = NaomiTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Black)
        Text(text, color = NaomiTextSecondary, fontSize = 11.sp)
    }
}

private fun formatDateTime(timestamp: Long): String =
    SimpleDateFormat("dd MMM HH:mm", Locale.UK).format(Date(timestamp))