package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
    val dateFormatter = remember { SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.UK) }
    val minuteKey = now / DASHBOARD_CLOCK_TICK_MS
    val clockText = remember(minuteKey) { clockFormatter.format(Date(now)) }
    val dateText = remember(now / 86_400_000L) { dateFormatter.format(Date(now)) }

    // A seconds clock forced the entire dashboard to recompose every second on the
    // low-RAM SUNMI V2. The UI only needs minute precision, so align updates to the
    // next minute boundary instead of running 60 recompositions per minute.
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
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "STAFF TERMINAL",
                    color = NaomiOrange,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp
                )
                Text(
                    text = "$clockText  •  ${user.displayName}",
                    color = NaomiTextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(dateText, color = NaomiTextSecondary, fontSize = 11.sp)
            }
            StatusPill(
                label = if (authState.activeShift != null) "SHIFT OPEN" else "NO SHIFT",
                active = authState.activeShift != null
            )
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = NaomiSurface),
            border = BorderStroke(1.dp, NaomiBorder),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("QUICK ACTIONS", color = NaomiOrange, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                Button(
                    onClick = { posViewModel.resetNewReceipt(); onNewReceipt() },
                    colors = ButtonDefaults.buttonColors(containerColor = NaomiRed),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(7.dp))
                    Text("New receipt", fontWeight = FontWeight.Black)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onScan,
                        border = BorderStroke(1.dp, NaomiOrange.copy(alpha = 0.75f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.size(5.dp))
                        Text("Scan")
                    }
                    OutlinedButton(
                        onClick = onBlackpool,
                        border = BorderStroke(1.dp, NaomiBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.size(5.dp))
                        Text("Venues")
                    }
                }
            }
        }

        DashboardCard("Current transaction") {
            DashboardLine("Venue", receipt.venueName)
            DashboardLine("Draft", receipt.id)
            DashboardLine("Items", receipt.items.sumOf { it.quantity }.toString())
            DashboardLine("Total", com.example.model.ReceiptData.formatCurrency(receipt.grandTotal))
        }

        DashboardCard("Terminal health") {
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                MiniStatus("Printer", if (printer.isConnected) "READY" else "OFFLINE", printer.isConnected, Modifier.weight(1f))
                MiniStatus("Gateway", if (gatewayOnline) "ONLINE" else "OFFLINE", gatewayOnline, Modifier.weight(1f))
                MiniStatus("Sync", if (unsynced == 0) "CLEAR" else "$unsynced WAIT", unsynced == 0, Modifier.weight(1f))
            }
        }

        if (authState.activeShift == null) {
            DashboardCard("Shift required") {
                Text(
                    "Open a staff shift before finalising transactions.",
                    color = NaomiTextSecondary,
                    fontSize = 10.sp
                )
                Button(
                    onClick = onOps,
                    colors = ButtonDefaults.buttonColors(containerColor = NaomiRed),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open staff operations", fontWeight = FontWeight.Black)
                }
            }
        } else {
            DashboardCard("Current shift") {
                DashboardLine("Status", "OPEN • ${formatDuration(now - authState.activeShift!!.openedAt)}")
                DashboardLine("Reference", authState.activeShift!!.id.take(8).uppercase(Locale.ROOT))
                DashboardLine("Role", if (user.isAdmin) "Naomi Admin" else "Staff")
            }
        }

        DashboardCard("Blackpool • Tower") {
            if (departures.isEmpty()) {
                Text("Live tram information unavailable.", color = NaomiTextSecondary, fontSize = 10.sp)
            } else {
                departures.take(3).forEach { departure ->
                    DashboardLine(
                        "${departure.directionLabel} → ${departure.destination}",
                        departure.departureTime + if (departure.isLive) " LIVE" else ""
                    )
                }
            }
        }

        OutlinedButton(onClick = onOps, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(17.dp))
            Spacer(modifier = Modifier.size(6.dp))
            Text("Staff operations")
        }
    }
}

@Composable
private fun StatusPill(label: String, active: Boolean) {
    Surface(
        color = if (active) NaomiSuccess.copy(alpha = 0.13f) else NaomiOrange.copy(alpha = 0.13f),
        border = BorderStroke(1.dp, if (active) NaomiSuccess.copy(alpha = 0.45f) else NaomiOrange.copy(alpha = 0.45f)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Text(
            text = label,
            color = if (active) NaomiSuccess else NaomiOrange,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun MiniStatus(label: String, value: String, healthy: Boolean, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = if (healthy) NaomiSuccess.copy(alpha = 0.08f) else NaomiOrange.copy(alpha = 0.08f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (healthy) NaomiSuccess.copy(alpha = 0.26f) else NaomiOrange.copy(alpha = 0.26f))
    ) {
        Column(modifier = Modifier.padding(9.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, color = NaomiTextSecondary, fontSize = 8.sp)
            Text(value, color = if (healthy) NaomiSuccess else NaomiOrange, fontWeight = FontWeight.Black, fontSize = 9.sp)
        }
    }
}

@Composable
private fun DashboardCard(title: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = NaomiSurface),
        border = BorderStroke(1.dp, NaomiBorder),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title.uppercase(Locale.ROOT), color = NaomiOrange, fontWeight = FontWeight.Black, fontSize = 9.sp, letterSpacing = 0.8.sp)
            content()
        }
    }
}

@Composable
private fun DashboardLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = NaomiTextSecondary, fontSize = 10.sp, modifier = Modifier.weight(0.38f))
        Text(
            value,
            color = NaomiTextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(0.62f)
        )
    }
}

private fun formatDuration(ms: Long): String {
    val totalMinutes = ms.coerceAtLeast(0L) / 60_000L
    return "%dh %02dm".format(totalMinutes / 60, totalMinutes % 60)
}
