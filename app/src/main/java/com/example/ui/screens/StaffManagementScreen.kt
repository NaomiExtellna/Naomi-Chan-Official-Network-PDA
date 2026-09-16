package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
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

    Column(modifier = Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("STAFF & SECURITY", color = NaomiOrange, fontSize = 10.sp, fontWeight = FontWeight.Black)
        Text(if (user.isAdmin) "Naomi Admin Controls" else "My Staff Access", color = NaomiTextPrimary, fontSize = 19.sp, fontWeight = FontWeight.Black)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { showChangeOwn = true }, modifier = Modifier.weight(1f)) { Text("Change My PIN") }
            if (user.isAdmin) {
                Button(
                    onClick = authViewModel::rotateAdminRecovery,
                    colors = ButtonDefaults.buttonColors(containerColor = NaomiRed),
                    modifier = Modifier.weight(1f)
                ) { Text("Recovery Code", fontSize = 10.sp) }
            }
        }

        if (!user.isAdmin) {
            PermissionSummary(user)
        }

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(if (user.isAdmin) accounts else accounts.filter { it.id == user.id }, key = { it.id }) { account ->
                StaffCard(account, user, authViewModel)
            }
        }
    }

    if (showChangeOwn) {
        ChangeCredentialDialog(
            title = "Change My PIN / Password",
            onDismiss = { showChangeOwn = false },
            onSave = { a, b -> authViewModel.changeOwnCredential(a, b); showChangeOwn = false }
        )
    }

    state.adminGeneratedCode?.let { code ->
        AlertDialog(
            onDismissRequest = authViewModel::clearAdminGeneratedCode,
            containerColor = NaomiSurface,
            title = { Text(state.adminGeneratedCodeLabel ?: "Generated Credential", color = NaomiTextPrimary, fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(code, color = NaomiOrange, fontSize = 24.sp, fontWeight = FontWeight.Black)
                    Text("Give this temporary PIN directly to the staff member. They must replace it immediately after signing in.", color = NaomiTextSecondary, fontSize = 11.sp)
                }
            },
            confirmButton = { Button(onClick = authViewModel::clearAdminGeneratedCode) { Text("Done") } }
        )
    }
}

@Composable
private fun StaffCard(account: StaffAccount, requester: StaffAccount, auth: AuthViewModel) {
    Card(
        colors = CardDefaults.cardColors(containerColor = NaomiSurface),
        border = BorderStroke(1.dp, if (account.isLocked) NaomiOrange else NaomiBorder),
        shape = RoundedCornerShape(11.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(account.displayName, color = NaomiTextPrimary, fontWeight = FontWeight.Black)
                    Text("@${account.username} • ${account.role.name}", color = NaomiTextSecondary, fontSize = 10.sp)
                    val status = when {
                        account.isLocked -> "LOCKED"
                        account.isActive -> "ACTIVE"
                        else -> "PENDING / DISABLED"
                    }
                    Text(status, color = if (account.isActive && !account.isLocked) NaomiSuccess else NaomiOrange, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    account.lastLoginAt?.let {
                        Text("Last login: ${formatStaffTime(it)}", color = NaomiTextSecondary, fontSize = 9.sp)
                    }
                }
                if (account.id == requester.id) Text("YOU", color = NaomiOrange, fontWeight = FontWeight.Black, fontSize = 10.sp)
            }

            if (requester.isAdmin && !account.isAdmin) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = { auth.setStaffActive(account.id, !account.isActive) },
                        colors = ButtonDefaults.buttonColors(containerColor = if (account.isActive) NaomiSurfaceVariant else NaomiRed),
                        modifier = Modifier.weight(1f)
                    ) { Text(if (account.isActive) "Disable" else "Approve / Enable", fontSize = 9.sp) }
                    OutlinedButton(onClick = { auth.resetStaffCredential(account.id) }, modifier = Modifier.weight(1f)) {
                        Text("Reset PIN", fontSize = 9.sp)
                    }
                }
                if (account.isLocked || account.failedAttempts > 0) {
                    OutlinedButton(onClick = { auth.unlockStaff(account.id) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Clear Login Lock (${account.failedAttempts} failed attempt(s))", fontSize = 9.sp)
                    }
                }
                Text("Permissions", color = NaomiTextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                PermissionChip("Void", account.canVoid) {
                    auth.setStaffPermissions(account.id, it, account.canExport, account.canEditVenues, account.canChangeGateway, account.canViewTotals)
                }
                PermissionChip("Export / Backup", account.canExport) {
                    auth.setStaffPermissions(account.id, account.canVoid, it, account.canEditVenues, account.canChangeGateway, account.canViewTotals)
                }
                PermissionChip("Edit Venues", account.canEditVenues) {
                    auth.setStaffPermissions(account.id, account.canVoid, account.canExport, it, account.canChangeGateway, account.canViewTotals)
                }
                PermissionChip("Change Gateway", account.canChangeGateway) {
                    auth.setStaffPermissions(account.id, account.canVoid, account.canExport, account.canEditVenues, it, account.canViewTotals)
                }
                PermissionChip("View Financial Totals", account.canViewTotals) {
                    auth.setStaffPermissions(account.id, account.canVoid, account.canExport, account.canEditVenues, account.canChangeGateway, it)
                }
            } else if (!account.isAdmin) {
                PermissionSummary(account)
            }
        }
    }
}

@Composable
private fun PermissionChip(label: String, selected: Boolean, onChange: (Boolean) -> Unit) {
    FilterChip(selected = selected, onClick = { onChange(!selected) }, label = { Text(label, fontSize = 9.sp) })
}

@Composable
private fun PermissionSummary(account: StaffAccount) {
    Card(colors = CardDefaults.cardColors(containerColor = NaomiSurfaceVariant), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("Effective permissions", color = NaomiTextPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            Text(
                listOfNotNull(
                    "Create receipts",
                    if (account.canVoidReceipts) "Void" else null,
                    if (account.canExportData) "Export" else null,
                    if (account.canManageVenues) "Edit venues" else null,
                    if (account.canConfigureGateway) "Gateway" else null,
                    if (account.canViewFinancialTotals) "Financial totals" else null
                ).joinToString(" • "),
                color = NaomiTextSecondary,
                fontSize = 9.sp
            )
        }
    }
}

@Composable
private fun ChangeCredentialDialog(title: String, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var first by remember { mutableStateOf("") }
    var second by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NaomiSurface,
        title = { Text(title, color = NaomiTextPrimary, fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(first, { first = it }, label = { Text("New PIN / password") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
                OutlinedTextField(second, { second = it }, label = { Text("Confirm") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
                Text("6–12 digit PIN or password of at least 8 characters.", color = NaomiTextSecondary, fontSize = 9.sp)
            }
        },
        confirmButton = { Button(onClick = { onSave(first, second) }, enabled = first.length >= 6 && second.isNotBlank()) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun formatStaffTime(value: Long): String = SimpleDateFormat("dd MMM HH:mm", Locale.UK).format(Date(value))
