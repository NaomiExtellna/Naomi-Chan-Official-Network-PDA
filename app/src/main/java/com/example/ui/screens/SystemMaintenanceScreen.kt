package com.example.ui.screens

import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.data.AppDatabase
import com.example.printer.PrintResult
import com.example.printer.printPrinterDiagnosticSlip
import com.example.ui.AuthViewModel
import com.example.ui.PosViewModel
import com.example.ui.theme.NaomiBorder
import com.example.ui.theme.NaomiOrange
import com.example.ui.theme.NaomiRed
import com.example.ui.theme.NaomiSurface
import com.example.ui.theme.NaomiSurfaceVariant
import com.example.ui.theme.NaomiTextPrimary
import com.example.ui.theme.NaomiTextSecondary
import com.example.util.DiagnosticLog
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@Composable
fun SystemMaintenanceScreen(authViewModel: AuthViewModel, posViewModel: PosViewModel) {
    val authState by authViewModel.state.collectAsState()
    val user = authState.currentUser ?: return
    val printerStatus by posViewModel.printerStatus.collectAsState()
    val selectedChannel by posViewModel.selectedChannel.collectAsState()
    val unsynced by posViewModel.unsyncedCount.collectAsState()
    val ram by posViewModel.ramInfo.collectAsState()
    val gatewayOnline by posViewModel.isWirelessOnline.collectAsState()
    val gatewayUrl by posViewModel.wirelessServerUrl.collectAsState()
    val receipts by posViewModel.allReceipts.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var gatewayDraft by remember(gatewayUrl) { mutableStateOf(gatewayUrl) }
    var testMessage by remember { mutableStateOf<String?>(null) }
    var showWipe by remember { mutableStateOf(false) }
    var refresh by remember { mutableIntStateOf(0) }
    val stats = remember(receipts, refresh) { posViewModel.databaseStats() }
    val diagnostics = remember(refresh) { DiagnosticLog.readRecent(context, 20) }
    val installId = remember { getOrCreateInstallId(context) }

    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let {
            val data = posViewModel.exportReceiptsCsv()
            if (data.isNotBlank() && context.writeText(it, data)) posViewModel.recordExport("CSV")
        }
    }
    val jsonLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let {
            val data = posViewModel.exportReceiptsJson()
            if (data.isNotBlank() && context.writeText(it, data)) posViewModel.recordExport("JSON")
        }
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { context.readText(it)?.let(posViewModel::restoreBackupJson) }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SectionCard("PDA & App", "Build and hardware identity for support.") {
            InfoLine("App", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            InfoLine("Build UTC", BuildConfig.BUILD_UTC)
            InfoLine("Database", "Room v${AppDatabase.DATABASE_VERSION}")
            InfoLine("Device", "${Build.MANUFACTURER} ${Build.MODEL}")
            InfoLine("Android", "${Build.VERSION.RELEASE} / API ${Build.VERSION.SDK_INT}")
            InfoLine("Install ID", installId.take(12))
            InfoLine("Memory", "${ram.freeMb} MB free / ${ram.totalMb} MB")
        }

        SectionCard("Database Maintenance", "Receipt/ticket storage only. Staff and audit data are separate.") {
            InfoLine("Tickets", stats.receiptCount.toString())
            InfoLine("Active", stats.activeCount.toString())
            InfoLine("Voided", stats.voidedCount.toString())
            InfoLine("Archived", stats.archivedCount.toString())
            InfoLine("Database size", formatBytes(stats.databaseBytes))
            InfoLine("Oldest", formatOptionalTime(stats.oldestReceiptAt))
            InfoLine("Newest", formatOptionalTime(stats.newestReceiptAt))
            InfoLine("Last JSON backup", formatOptionalTime(stats.lastBackupAt.takeIf { it > 0 }))
            InfoLine("Last restore", formatOptionalTime(stats.lastRestoreAt.takeIf { it > 0 }))

            if (user.isAdmin) {
                OutlinedButton(onClick = { posViewModel.archiveOldTickets(90); refresh++ }, modifier = Modifier.fillMaxWidth()) {
                    Text("Archive Synced Tickets Older Than 90 Days")
                }
                Button(
                    onClick = { showWipe = true },
                    colors = ButtonDefaults.buttonColors(containerColor = NaomiRed),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Wipe All Current Tickets", fontWeight = FontWeight.Black)
                }
            }
        }

        SectionCard("Backup & Restore", "JSON restore is merge-only: matching receipt IDs are never overwritten.") {
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                OutlinedButton(
                    onClick = { csvLauncher.launch("naomi-pos-receipts.csv") },
                    enabled = user.canExportData,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.SaveAlt, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("CSV")
                }
                OutlinedButton(
                    onClick = { jsonLauncher.launch("naomi-pos-backup-v2.json") },
                    enabled = user.canExportData,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("JSON")
                }
            }
            if (user.isAdmin) {
                OutlinedButton(onClick = { restoreLauncher.launch(arrayOf("application/json", "text/json", "text/plain")) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("Restore / Merge JSON Backup")
                }
            }
        }

        SectionCard("Printer & Gateway", "Operational diagnostics and network configuration.") {
            InfoLine("Printer", if (printerStatus.isConnected) "Connected • ${printerStatus.deviceName}" else "Offline • ${printerStatus.deviceName}")
            InfoLine("Gateway", if (gatewayOnline) "Online" else "Offline")
            InfoLine("Unsynced", unsynced.toString())
            OutlinedButton(
                onClick = {
                    scope.launch {
                        testMessage = "Sending diagnostic slip…"
                        testMessage = when (val result = printPrinterDiagnosticSlip(posViewModel.printerManager, selectedChannel, printerStatus, user.displayName)) {
                            is PrintResult.Success -> {
                                posViewModel.logAction("PRINTER_DIAGNOSTIC", selectedChannel.name)
                                "Diagnostic printed. No ledger transaction was created."
                            }
                            is PrintResult.OutOfPaper -> result.message
                            is PrintResult.Error -> result.errorReason
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(5.dp))
                Text("Print Diagnostic Slip")
            }
            testMessage?.let { Text(it, color = NaomiOrange, fontSize = 9.5.sp) }

            OutlinedTextField(
                value = gatewayDraft,
                onValueChange = { gatewayDraft = it },
                enabled = user.canConfigureGateway,
                label = { Text("Flask gateway URL") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { posViewModel.setWirelessServerUrl(gatewayDraft) },
                enabled = user.canConfigureGateway,
                colors = ButtonDefaults.buttonColors(containerColor = NaomiSurfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save & Test Gateway") }
        }

        SectionCard("Error / Crash History", "Local support log; credentials are never written here.") {
            if (diagnostics.isEmpty()) Text("No recorded diagnostic errors.", color = NaomiTextSecondary, fontSize = 10.sp)
            diagnostics.take(10).forEach { Text(it, color = NaomiTextSecondary, fontSize = 8.5.sp) }
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                OutlinedButton(onClick = { refresh++; posViewModel.refreshRamInfo(); posViewModel.checkWirelessConnection() }, modifier = Modifier.weight(1f)) { Text("Refresh") }
                if (user.isAdmin) {
                    OutlinedButton(onClick = { DiagnosticLog.clear(context); authViewModel.logAudit("DIAGNOSTICS_CLEARED"); refresh++ }, modifier = Modifier.weight(1f)) { Text("Clear") }
                }
            }
        }

        Button(onClick = authViewModel::logout, colors = ButtonDefaults.buttonColors(containerColor = NaomiRed), modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Lock, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Lock PDA / Sign Out", fontWeight = FontWeight.Black)
        }
    }

    if (showWipe) {
        WipeTicketsDialog(onDismiss = { showWipe = false }) {
            posViewModel.wipeAllTickets()
            showWipe = false
            refresh++
        }
    }
}

@Composable
private fun WipeTicketsDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    var confirmation by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NaomiSurface,
        title = { Text("Wipe All Current Tickets?", color = NaomiTextPrimary, fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("This permanently deletes every receipt/ticket row from this PDA. Staff accounts, shifts, custom venues and the audit log are kept. Export a JSON backup first if you may need the tickets later.", color = NaomiTextSecondary, fontSize = 11.sp)
                Text("Type WIPE TICKETS to continue.", color = NaomiOrange, fontWeight = FontWeight.Bold)
                OutlinedTextField(confirmation, { confirmation = it.uppercase() }, label = { Text("Confirmation") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, enabled = confirmation.trim() == "WIPE TICKETS", colors = ButtonDefaults.buttonColors(containerColor = NaomiRed)) { Text("Permanently Wipe") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun SectionCard(title: String, subtitle: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = NaomiSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, NaomiBorder),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = NaomiTextPrimary, fontWeight = FontWeight.Black, fontSize = 15.sp)
            Text(subtitle, color = NaomiTextSecondary, fontSize = 9.5.sp)
            content()
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = NaomiTextSecondary, fontSize = 10.sp)
        Text(value, color = NaomiTextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

private fun Context.writeText(uri: android.net.Uri, value: String): Boolean = runCatching {
    contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { it.write(value) } ?: error("Unable to open output")
    true
}.getOrDefault(false)

private fun Context.readText(uri: android.net.Uri): String? = runCatching {
    contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
}.getOrNull()

private fun formatOptionalTime(value: Long?): String = value?.let { SimpleDateFormat("dd MMM yyyy HH:mm", Locale.UK).format(Date(it)) } ?: "—"
private fun formatBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024L -> String.format(Locale.UK, "%.1f MB", bytes / (1024.0 * 1024.0))
    bytes >= 1024L -> String.format(Locale.UK, "%.1f KB", bytes / 1024.0)
    else -> "$bytes B"
}

private fun getOrCreateInstallId(context: Context): String {
    val prefs = context.getSharedPreferences("naomi_ops_meta", Context.MODE_PRIVATE)
    val existing = prefs.getString("install_id", null)
    if (!existing.isNullOrBlank()) return existing
    val created = UUID.randomUUID().toString()
    prefs.edit().putString("install_id", created).apply()
    return created
}
