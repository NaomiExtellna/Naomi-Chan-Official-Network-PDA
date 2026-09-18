package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.StaffAccount
import com.example.ui.AuthViewModel
import com.example.ui.theme.NaomiBorder
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

@Composable
fun StaffManagementScreen(authViewModel: AuthViewModel) {
    val state by authViewModel.state.collectAsState()
    val accounts by authViewModel.staffAccounts.collectAsState()
    val user = state.currentUser ?: return
    var showChangeOwn by remember { mutableStateOf(false) }
    var page by remember { mutableIntStateOf(0) }

    val visibleAccounts = if (user.isAdmin) accounts else accounts.filter { it.id == user.id }
    val pages = pageCount(visibleAccounts.size, 1)
    val account = pageSlice(visibleAccounts, page, 1).firstOrNull()

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text("STAFF & SECURITY", color = NaomiOrange, fontSize = 8.sp, fontWeight = FontWeight.Black)
                Text(
                    if (user.isAdmin) "Admin controls" else "My staff access",
                    color = NaomiTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            OutlinedButton(onClick = { showChangeOwn = true }, modifier = Modifier.weight(1f).height(38.dp)) {
                Text("Change my PIN", fontSize = 8.sp)
            }
            if (user.isAdmin) {
                Button(
                    onClick = authViewModel::rotateAdminRecovery,
                    colors = ButtonDefaults.buttonColors(containerColor = NaomiRed),
                    modifier = Modifier.weight(1f).height(38.dp)
                ) { Text("Recovery code", fontSize = 8.sp) }
            }
        }

        if (account == null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = NaomiSurface),
                border = BorderStroke(1.dp, NaomiBorder),
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.Center) {
                    Text("No staff accounts available", color = NaomiTextSecondary, fontSize = 10.sp)
                }
            }
        } else {
            StaffCardCompact(account, user, authViewModel, Modifier.fillMaxWidth().weight(1f))
        }

        PosPager(
            page = page,
            totalPages = pages,
            onPrevious = { page = (page - 1).coerceAtLeast(0) },
            onNext = { page = (page + 1).coerceAtMost(pages - 1) },
            label = "STAFF"
        )
    }

    if (showChangeOwn) {
        ChangeCredentialDialog(
            title = "Change My PIN / Password",
            onDismiss = { showChangeOwn = false },
            onSave = { a, b ->
                authViewModel.changeOwnCredential(a, b)
                showChangeOwn = false
            }
        )
    }

    state.adminGeneratedCode?.let { code ->
        AlertDialog(
            onDismissRequest = authViewModel::clearAdminGeneratedCode,
            containerColor = NaomiSurface,
            title = {
                Text(
                    state.adminGeneratedCodeLabel ?: "Generated Credential",
                    color = NaomiTextPrimary,
                    fontWeight = FontWeight.Black
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(code, color = NaomiOrange, fontSize = 24.sp, fontWeight = FontWeight.Black)
                    Text(
                        "Give this temporary PIN directly to the staff member. They must replace it after signing in.",
                        color = NaomiTextSecondary,
                        fontSize = 10.sp
                    )
                }
            },
            confirmButton = { Button(onClick = authViewModel::clearAdminGeneratedCode) { Text("Done") } }
        )
    }
}

@Composable
private fun StaffCardCompact(
    account: StaffAccount,
    requester: StaffAccount,
    auth: AuthViewModel,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = NaomiSurface),
        border = BorderStroke(1.dp, if (account.isLocked) NaomiOrange else NaomiBorder),
        shape = RoundedCornerShape(4.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(account.displayName, color = NaomiTextPrimary, fontWeight = FontWeight.Black, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("@${account.username} · ${account.role.name}", color = NaomiTextSecondary, fontSize = 8.sp)
                    val status = when {
                        account.isLocked -> "LOCKED"
                        account.isActive -> "ACTIVE"
                        else -> "PENDING / DISABLED"
                    }
                    Text(status, color = if (account.isActive && !account.isLocked) NaomiSuccess else NaomiOrange, fontSize = 8.sp, fontWeight = FontWeight.Black)
                    account.lastLoginAt?.let {
                        Text("Last login ${formatStaffTime(it)}", color = NaomiTextSecondary, fontSize = 7.sp)
                    }
                }
                if (account.id == requester.id) Text("YOU", color = NaomiOrange, fontWeight = FontWeight.Black, fontSize = 8.sp)
            }

            if (requester.isAdmin && !account.isAdmin) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = { auth.setStaffActive(account.id, !account.isActive) },
                        colors = ButtonDefaults.buttonColors(containerColor = if (account.isActive) NaomiSurfaceVariant else NaomiRed),
                        modifier = Modifier.weight(1f).height(36.dp)
                    ) { Text(if (account.isActive) "Disable" else "Enable", fontSize = 8.sp) }
                    OutlinedButton(
                        onClick = { auth.resetStaffCredential(account.id) },
                        modifier = Modifier.weight(1f).height(36.dp)
                    ) { Text("Reset PIN", fontSize = 8.sp) }
                }

                if (account.isLocked || account.failedAttempts > 0) {
                    OutlinedButton(
                        onClick = { auth.unlockStaff(account.id) },
                        modifier = Modifier.fillMaxWidth().height(34.dp)
                    ) {
                        Text("Clear login lock · ${account.failedAttempts} failed", fontSize = 7.sp)
                    }
                }

                Text("PERMISSIONS", color = NaomiTextSecondary, fontSize = 7.sp, fontWeight = FontWeight.Black)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    PermissionChip("Void", account.canVoid, Modifier.weight(1f)) {
                        auth.setStaffPermissions(account.id, it, account.canExport, account.canEditVenues, account.canChangeGateway, account.canViewTotals)
                    }
                    PermissionChip("Export", account.canExport, Modifier.weight(1f)) {
                        auth.setStaffPermissions(account.id, account.canVoid, it, account.canEditVenues, account.canChangeGateway, account.canViewTotals)
                    }
                    PermissionChip("Venues", account.canEditVenues, Modifier.weight(1f)) {
                        auth.setStaffPermissions(account.id, account.canVoid, account.canExport, it, account.canChangeGateway, account.canViewTotals)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    PermissionChip("Totals", account.canViewTotals, Modifier.weight(1f)) {
                        auth.setStaffPermissions(account.id, account.canVoid, account.canExport, account.canEditVenues, account.canChangeGateway, it)
                    }
                    Spacer(modifier = Modifier.weight(2f))
                }
            } else if (!account.isAdmin) {
                PermissionSummary(account)
            } else {
                Card(colors = CardDefaults.cardColors(containerColor = NaomiSurfaceVariant), modifier = Modifier.fillMaxWidth().weight(1f)) {
                    Column(modifier = Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.Center) {
                        Text("ADMINISTRATOR", color = NaomiOrange, fontWeight = FontWeight.Black, fontSize = 9.sp)
                        Text("Full terminal privileges are active for this account.", color = NaomiTextSecondary, fontSize = 8.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onChange: (Boolean) -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = { onChange(!selected) },
        label = { Text(label, fontSize = 7.sp) },
        modifier = modifier.height(34.dp)
    )
}

@Composable
private fun PermissionSummary(account: StaffAccount) {
    Card(colors = CardDefaults.cardColors(containerColor = NaomiSurfaceVariant), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(9.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("Effective permissions", color = NaomiTextPrimary, fontWeight = FontWeight.Bold, fontSize = 9.sp)
            Text(
                listOfNotNull(
                    "Create receipts",
                    if (account.canVoidReceipts) "Void" else null,
                    if (account.canExportData) "Export" else null,
                    if (account.canManageVenues) "Edit venues" else null,
                    if (account.canViewFinancialTotals) "Financial totals" else null
                ).joinToString(" · "),
                color = NaomiTextSecondary,
                fontSize = 7.sp
            )
        }
    }
}

@Composable
private fun ChangeCredentialDialog(
    title: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var first by remember { mutableStateOf("") }
    var second by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NaomiSurface,
        title = { Text(title, color = NaomiTextPrimary, fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(first, { first = it }, label = { Text("New PIN / password") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
                OutlinedTextField(second, { second = it }, label = { Text("Confirm") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
                Text("6–12 digit PIN or password of at least 8 characters.", color = NaomiTextSecondary, fontSize = 9.sp)
            }
        },
        confirmButton = {
            Button(onClick = { onSave(first, second) }, enabled = first.length >= 6 && second.isNotBlank()) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun formatStaffTime(value: Long): String =
    SimpleDateFormat("dd MMM HH:mm", Locale.UK).format(Date(value))