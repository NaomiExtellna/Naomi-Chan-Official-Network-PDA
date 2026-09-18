package com.example.ui.screens

import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.text.style.TextOverflow
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

private enum class MaintenanceMode { DEVICE, DATA, OUTPUT, LOGS }

@Composable
fun SystemMaintenanceScreen(authViewModel: AuthViewModel, posViewModel: PosViewModel) {
    val authState by authViewModel.state.collectAsState()
    val user = authState.currentUser ?: return
    val printerStatus by posViewModel.printerStatus.collectAsState()
    val selectedChannel by posViewModel.selectedChannel.collectAsState()
    val ram by posViewModel.ramInfo.collectAsState()
    val receipts by posViewModel.allReceipts.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var mode by remember { mutableStateOf(MaintenanceMode.DEVICE) }
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
        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Column {
            Text("SYSTEM", color = NaomiOrange, fontSize = 8.sp, fontWeight = FontWeight.Black)
            Text("Maintenance console", color = NaomiTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Black)
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            MaintenanceMode.values().forEach { item ->
                FilterChip(
                    selected = mode == item,
                    onClick = { mode = item },
                    label = {
                        Text(
                            when (item) {
                                MaintenanceMode.DEVICE -> "Device"
                                MaintenanceMode.DATA -> "Data"
                                MaintenanceMode.OUTPUT -> "Output"
                                MaintenanceMode.LOGS -> "Logs"
                            },
                            fontSize = 7.sp
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = NaomiRed, selectedLabelColor = androidx.compose.ui.graphics.Color.White),
                    modifier = Modifier.weight(1f).height(34.dp)
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = NaomiSurface),
            border = BorderStroke(1.dp, NaomiBorder),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            when (mode) {
                MaintenanceMode.DEVICE -> Column(
                    modifier = Modifier.fillMaxSize().padding(11.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PanelHeading("PDA & App", "Build and hardware identity")
                    InfoLine("App", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                    InfoLine("Build UTC", BuildConfig.BUILD_UTC)
                    InfoLine("Database", "Room v${AppDatabase.DATABASE_VERSION}")
                    InfoLine("Device", "${Build.MANUFACTURER} ${Build.MODEL}")
                    InfoLine("Android", "${Build.VERSION.RELEASE} / API ${Build.VERSION.SDK_INT}")
                    InfoLine("Install ID", installId.take(12))
                    InfoLine("Memory", "${ram.freeMb} MB free / ${ram.totalMb} MB")
                    Spacer(modifier = Modifier.weight(1f))
                    OutlinedButton(
                        onClick = { refresh++; posViewModel.refreshRamInfo() },
                        modifier = Modifier.fillMaxWidth().height(38.dp)
                    ) { Text("Refresh device status", fontSize = 8.sp) }
                }

                MaintenanceMode.DATA -> Column(
                    modifier = Modifier.fillMaxSize().padding(11.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PanelHeading("Local Data", "CSV export and local backup")
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        MiniMetric("Tickets", stats.receiptCount.toString(), Modifier.weight(1f))
                        MiniMetric("Active", stats.activeCount.toString(), Modifier.weight(1f))
                        MiniMetric("Voided", stats.voidedCount.toString(), Modifier.weight(1f))
                        MiniMetric("Size", formatBytes(stats.databaseBytes), Modifier.weight(1f))
                    }
                    InfoLine("Oldest", formatOptionalTime(stats.oldestReceiptAt))
                    InfoLine("Newest", formatOptionalTime(stats.newestReceiptAt))
                    InfoLine("Last backup", formatOptionalTime(stats.lastBackupAt.takeIf { it > 0 }))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = { csvLauncher.launch("naomi-pos-receipts.csv") },
                            enabled = user.canExportData,
                            modifier = Modifier.weight(1f).height(38.dp)
                        ) {
                            Icon(Icons.Default.SaveAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("CSV", fontSize = 8.sp)
                        }
                        OutlinedButton(
                            onClick = { jsonLauncher.launch("naomi-pos-backup-v2.json") },
                            enabled = user.canExportData,
                            modifier = Modifier.weight(1f).height(38.dp)
                        ) {
                            Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("JSON", fontSize = 8.sp)
                        }
                        if (user.isAdmin) {
                            OutlinedButton(
                                onClick = { restoreLauncher.launch(arrayOf("application/json", "text/json", "text/plain")) },
                                modifier = Modifier.weight(1f).height(38.dp)
                            ) {
                                Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(14.dp))
                                Text("Restore", fontSize = 7.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    if (user.isAdmin) {
                        OutlinedButton(
                            onClick = { posViewModel.archiveOldTickets(90); refresh++ },
                            modifier = Modifier.fillMaxWidth().height(36.dp)
                        ) { Text("Archive local tickets older than 90 days", fontSize = 7.sp) }
                        Button(
                            onClick = { showWipe = true },
                            colors = ButtonDefaults.buttonColors(containerColor = NaomiRed),
                            modifier = Modifier.fillMaxWidth().height(38.dp)
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Wipe current tickets", fontSize = 8.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }

                MaintenanceMode.OUTPUT -> Column(
                    modifier = Modifier.fillMaxSize().padding(11.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    PanelHeading("Printer", "Local output diagnostics")
                    InfoLine("Printer", if (printerStatus.isConnected) "Connected · ${printerStatus.deviceName}" else "Offline · ${printerStatus.deviceName}")
                    InfoLine("Storage", "Local Room database")
                    InfoLine("Export", "CSV available from Activity or Data")
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                testMessage = "Sending diagnostic slip…"
                                testMessage = when (val result = printPrinterDiagnosticSlip(posViewModel.printerManager, selectedChannel, printerStatus, user.displayName)) {
                                    is PrintResult.Success -> {
                                        posViewModel.logAction("PRINTER_DIAGNOSTIC", selectedChannel.name)
                                        "Diagnostic printed."
                                    }
                                    is PrintResult.OutOfPaper -> result.message
                                    is PrintResult.Error -> result.errorReason
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(38.dp)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Print diagnostic slip", fontSize = 8.sp)
                    }
                    testMessage?.let { Text(it, color = NaomiOrange, fontSize = 8.sp, maxLines = 2, overflow = TextOverflow.Ellipsis) }
                }

                MaintenanceMode.LOGS -> Column(
                    modifier = Modifier.fillMaxSize().padding(11.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PanelHeading("Error / Crash History", "Local support log · no credentials")
                    Card(
                        colors = CardDefaults.cardColors(containerColor = NaomiSurfaceVariant),
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    ) {
                        Column(modifier = Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            if (diagnostics.isEmpty()) {
                                Text("No recorded diagnostic errors.", color = NaomiTextSecondary, fontSize = 8.sp)
                            } else {
                                diagnostics.take(8).forEach {
                                    Text(it, color = NaomiTextSecondary, fontSize = 6.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = { refresh++; posViewModel.refreshRamInfo() },
                            modifier = Modifier.weight(1f).height(36.dp)
                        ) { Text("Refresh", fontSize = 8.sp) }
                        if (user.isAdmin) {
                            OutlinedButton(
                                onClick = { DiagnosticLog.clear(context); authViewModel.logAudit("DIAGNOSTICS_CLEARED"); refresh++ },
                                modifier = Modifier.weight(1f).height(36.dp)
                            ) { Text("Clear", fontSize = 8.sp) }
                        }
                    }
                }
            }
        }

        Button(
            onClick = authViewModel::logout,
            colors = ButtonDefaults.buttonColors(containerColor = NaomiRed),
            modifier = Modifier.fillMaxWidth().height(38.dp),
            shape = RoundedCornerShape(4.dp)
        ) {
            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(5.dp))
            Text("Lock PDA / Sign out", fontWeight = FontWeight.Black, fontSize = 8.sp)
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
private fun PanelHeading(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(title, color = NaomiTextPrimary, fontWeight = FontWeight.Black, fontSize = 12.sp)
        Text(subtitle, color = NaomiTextSecondary, fontSize = 7.sp)
    }
}

@Composable
private fun MiniMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Card(colors = CardDefaults.cardColors(containerColor = NaomiSurfaceVariant), modifier = modifier) {
        Column(modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp)) {
            Text(label.uppercase(), color = NaomiTextSecondary, fontSize = 6.sp, fontWeight = FontWeight.Bold)
            Text(value, color = NaomiTextPrimary, fontSize = 8.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "This permanently deletes every receipt/ticket row from this PDA. Staff accounts, shifts, custom venues and the audit log are kept. Export a JSON backup first if needed.",
                    color = NaomiTextSecondary,
                    fontSize = 10.sp
                )
                Text("Type WIPE TICKETS to continue.", color = NaomiOrange, fontWeight = FontWeight.Bold)
                OutlinedTextField(confirmation, { confirmation = it.uppercase() }, label = { Text("Confirmation") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = confirmation.trim() == "WIPE TICKETS",
                colors = ButtonDefaults.buttonColors(containerColor = NaomiRed)
            ) { Text("Permanently Wipe") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = NaomiTextSecondary, fontSize = 8.sp)
        Text(value, color = NaomiTextPrimary, fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private fun Context.writeText(uri: android.net.Uri, value: String): Boolean = runCatching {
    contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { it.write(value) } ?: error("Unable to open output")
    true
}.getOrDefault(false)

private fun Context.readText(uri: android.net.Uri): String? = runCatching {
    contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
}.getOrNull()

private fun formatOptionalTime(value: Long?): String =
    value?.let { SimpleDateFormat("dd MMM yyyy HH:mm", Locale.UK).format(Date(it)) } ?: "—"

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