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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.network.BlackpoolTramClient
import com.example.network.BlackpoolTramStops
import com.example.network.TramDeparture
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
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val DASHBOARD_CLOCK_TICK_MS = 60_000L
private const val TRAM_REFRESH_MS = 60_000L

@Composable
fun EventDashboardScreen(
    authViewModel: AuthViewModel,
    posViewModel: PosViewModel,
    onNewReceipt: () -> Unit,
    onScan: () -> Unit,
    onBlackpool: () -> Unit,
    onOps: () -> Unit
) {
    val authState by authViewModel.state.collectAsStateWithLifecycle()
    val receipt by posViewModel.currentReceipt.collectAsStateWithLifecycle()
    val printer by posViewModel.printerStatus.collectAsStateWithLifecycle()
    val unsynced by posViewModel.unsyncedCount.collectAsStateWithLifecycle()
    val gatewayOnline by posViewModel.isWirelessOnline.collectAsStateWithLifecycle()
    val user = authState.currentUser ?: return

    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    var departures by remember { mutableStateOf<List<TramDeparture>>(emptyList()) }
    val tramClient = remember { BlackpoolTramClient() }
    val tower = remember {
        BlackpoolTramStops.FEATURED.firstOrNull { it.name == "Tower" }
            ?: BlackpoolTramStops.FEATURED.first()
    }
    val clockFormatter = remember { SimpleDateFormat("HH:mm", Locale.UK) }
    val dateFormatter = remember { SimpleDateFormat("EEE, dd MMM", Locale.UK) }
    val minuteKey = now / DASHBOARD_CLOCK_TICK_MS
    val clockText = remember(minuteKey) { clockFormatter.format(Date(now)) }
    val dateText = remember(now / 86_400_000L) { dateFormatter.format(Date(now)) }

    LaunchedEffect(Unit) {
        while (true) {
            val current = System.currentTimeMillis()
            now = current
            val untilNextMinute = DASHBOARD_CLOCK_TICK_MS - (current % DASHBOARD_CLOCK_TICK_MS)
            delay(untilNextMinute.coerceAtLeast(1_000L))
        }
    }

    LaunchedEffect(tower.name) {
        while (true) {
            departures = tramClient.fetchDepartures(tower)
            delay(TRAM_REFRESH_MS)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NaomiDarkBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Home",
                    color = NaomiTextPrimary,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "$dateText · $clockText",
                    color = NaomiTextSecondary,
                    fontSize = 11.sp
                )
            }
            ShiftBadge(active = authState.activeShift != null)
        }

        Text(
            text = "Hi ${user.displayName}",
            color = NaomiTextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = NaomiSurface),
            border = BorderStroke(1.dp, NaomiBorder),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("QUICK ACTIONS", color = NaomiTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.7.sp)
                Button(
                    onClick = { posViewModel.resetNewReceipt(); onNewReceipt() },
                    colors = ButtonDefaults.buttonColors(containerColor = NaomiRed),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Start new sale", fontWeight = FontWeight.Black, fontSize = 14.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onScan,
                        border = BorderStroke(1.dp, NaomiBorder),
                        shape = RoundedCornerShape(13.dp),
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Scan")
                    }
                    OutlinedButton(
                        onClick = onBlackpool,
                        border = BorderStroke(1.dp, NaomiBorder),
                        shape = RoundedCornerShape(13.dp),
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Venues")
                    }
                }
            }
        }

        SummaryCard("Current sale") {
            if (receipt.items.isEmpty() && receipt.clientName.isBlank()) {
                Text("No sale in progress", color = NaomiTextSecondary, fontSize = 12.sp)
            } else {
                SummaryLine("Customer", receipt.clientName.ifBlank { "Not set" })
                SummaryLine("Items", receipt.items.sumOf { it.quantity }.toString())
                SummaryLine("Total", com.example.model.ReceiptData.formatCurrency(receipt.grandTotal), emphasize = true)
            }
        }

        SummaryCard("Device status") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HealthTile("Printer", if (printer.isConnected) "Ready" else "Offline", printer.isConnected, Modifier.weight(1f))
                HealthTile("Gateway", if (gatewayOnline) "Online" else "Offline", gatewayOnline, Modifier.weight(1f))
                HealthTile("Sync", if (unsynced == 0) "Clear" else "$unsynced pending", unsynced == 0, Modifier.weight(1f))
            }
        }

        if (authState.activeShift == null) {
            SummaryCard("Shift") {
                Text("Open a staff shift before finalising transactions.", color = NaomiTextSecondary, fontSize = 11.sp)
                Button(
                    onClick = onOps,
                    colors = ButtonDefaults.buttonColors(containerColor = NaomiSurfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) {
                    Text("Open staff operations", color = NaomiTextPrimary, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            SummaryCard("Shift") {
                SummaryLine("Status", "Open · ${formatDuration(now - authState.activeShift!!.openedAt)}")
                SummaryLine("Role", if (user.isAdmin) "Naomi Admin" else "Staff")
            }
        }

        SummaryCard("Blackpool · Tower") {
            if (departures.isEmpty()) {
                Text("Live tram information unavailable.", color = NaomiTextSecondary, fontSize = 11.sp)
            } else {
                departures.take(3).forEach { departure ->
                    SummaryLine(
                        "${departure.directionLabel} → ${departure.destination}",
                        departure.departureTime + if (departure.isLive) " live" else ""
                    )
                }
            }
        }

        OutlinedButton(
            onClick = onOps,
            border = BorderStroke(1.dp, NaomiBorder),
            shape = RoundedCornerShape(13.dp),
            modifier = Modifier.fillMaxWidth().height(46.dp)
        ) {
            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.size(7.dp))
            Text("Staff operations")
        }
    }
}

@Composable
private fun ShiftBadge(active: Boolean) {
    Surface(
        color = if (active) NaomiSuccess.copy(alpha = 0.12f) else NaomiOrange.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, if (active) NaomiSuccess.copy(alpha = 0.35f) else NaomiOrange.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = if (active) "Shift open" else "No shift",
            color = if (active) NaomiSuccess else NaomiOrange,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun HealthTile(label: String, value: String, healthy: Boolean, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = NaomiSurfaceVariant,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, NaomiBorder)
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(label, color = NaomiTextSecondary, fontSize = 9.sp)
            Text(
                value,
                color = if (healthy) NaomiSuccess else NaomiOrange,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SummaryCard(title: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = NaomiSurface),
        border = BorderStroke(1.dp, NaomiBorder),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = NaomiTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            content()
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String, emphasize: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = NaomiTextSecondary, fontSize = 11.sp, modifier = Modifier.weight(0.48f))
        Text(
            value,
            color = NaomiTextPrimary,
            fontWeight = if (emphasize) FontWeight.Black else FontWeight.Medium,
            fontSize = if (emphasize) 13.sp else 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(0.52f)
        )
    }
}

private fun formatDuration(ms: Long): String {
    val totalMinutes = ms.coerceAtLeast(0L) / 60_000L
    return "%dh %02dm".format(totalMinutes / 60, totalMinutes % 60)
}
