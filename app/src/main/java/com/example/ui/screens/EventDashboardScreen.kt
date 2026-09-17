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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.example.model.ReceiptData
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
private const val DASHBOARD_TRAM_REFRESH_MS = 180_000L

@Composable
fun EventDashboardScreen(
    authViewModel: AuthViewModel,
    posViewModel: PosViewModel,
    onNewReceipt: () -> Unit,
    onScan: () -> Unit,
    onBlackpool: () -> Unit,
    onOps: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
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
    val clockText = remember(now / DASHBOARD_CLOCK_TICK_MS) { clockFormatter.format(Date(now)) }
    val dateText = remember(now / 86_400_000L) { dateFormatter.format(Date(now)) }

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                val current = System.currentTimeMillis()
                now = current
                delay((DASHBOARD_CLOCK_TICK_MS - (current % DASHBOARD_CLOCK_TICK_MS)).coerceAtLeast(1_000L))
            }
        }
    }

    LaunchedEffect(lifecycleOwner, tower.name) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                departures = tramClient.fetchDepartures(tower)
                delay(DASHBOARD_TRAM_REFRESH_MS)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NaomiDarkBg)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("HOME", color = NaomiOrange, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                Text("Hi ${user.displayName}", color = NaomiTextPrimary, fontSize = 19.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("$dateText · $clockText", color = NaomiTextSecondary, fontSize = 9.sp)
            }
            ShiftBadge(active = authState.activeShift != null)
        }

        Button(
            onClick = { posViewModel.resetNewReceipt(); onNewReceipt() },
            colors = ButtonDefaults.buttonColors(containerColor = NaomiRed),
            shape = RoundedCornerShape(13.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(19.dp))
            Spacer(modifier = Modifier.size(7.dp))
            Text("START NEW SALE", fontWeight = FontWeight.Black, fontSize = 12.sp)
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            OutlinedButton(
                onClick = onScan,
                border = BorderStroke(1.dp, NaomiBorder),
                shape = RoundedCornerShape(11.dp),
                modifier = Modifier.weight(1f).height(40.dp)
            ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.size(4.dp))
                Text("Scan", fontSize = 10.sp)
            }
            OutlinedButton(
                onClick = onBlackpool,
                border = BorderStroke(1.dp, NaomiBorder),
                shape = RoundedCornerShape(11.dp),
                modifier = Modifier.weight(1f).height(40.dp)
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.size(4.dp))
                Text("Blackpool", fontSize = 10.sp)
            }
            OutlinedButton(
                onClick = onOps,
                border = BorderStroke(1.dp, NaomiBorder),
                shape = RoundedCornerShape(11.dp),
                modifier = Modifier.weight(1f).height(40.dp)
            ) {
                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.size(4.dp))
                Text("Ops", fontSize = 10.sp)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            DashboardSaleCard(receipt, Modifier.weight(1.25f))
            DashboardShiftCard(
                shiftOpen = authState.activeShift != null,
                duration = authState.activeShift?.let { formatDuration(now - it.openedAt) },
                role = if (user.isAdmin) "Admin" else "Staff",
                onOps = onOps,
                modifier = Modifier.weight(0.75f)
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            HealthTile("Printer", if (printer.isConnected) "Ready" else "Offline", printer.isConnected, Modifier.weight(1f))
            HealthTile("Gateway", if (gatewayOnline) "Online" else "Offline", gatewayOnline, Modifier.weight(1f))
            HealthTile("Sync", if (unsynced == 0) "Clear" else "$unsynced pending", unsynced == 0, Modifier.weight(1f))
        }

        TramMiniBoard(
            departures = departures.take(3),
            modifier = Modifier.weight(1f).fillMaxWidth()
        )
    }
}

@Composable
private fun DashboardSaleCard(receipt: ReceiptData, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = NaomiSurface),
        border = BorderStroke(1.dp, NaomiBorder),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("CURRENT SALE", color = NaomiTextSecondary, fontSize = 8.sp, fontWeight = FontWeight.Black)
            val empty = receipt.items.isEmpty() && receipt.clientName.isBlank()
            Text(
                if (empty) "No sale in progress" else receipt.clientName.ifBlank { "Customer not set" },
                color = NaomiTextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!empty) {
                Text(receipt.venueName.ifBlank { "Venue not set" }, color = NaomiTextSecondary, fontSize = 8.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${receipt.items.sumOf { it.quantity }} item(s)", color = NaomiTextSecondary, fontSize = 8.sp)
                    Text(ReceiptData.formatCurrency(receipt.grandTotal), color = NaomiOrange, fontSize = 11.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun DashboardShiftCard(
    shiftOpen: Boolean,
    duration: String?,
    role: String,
    onOps: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = NaomiSurface),
        border = BorderStroke(1.dp, if (shiftOpen) NaomiSuccess.copy(alpha = 0.32f) else NaomiOrange.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("SHIFT", color = NaomiTextSecondary, fontSize = 8.sp, fontWeight = FontWeight.Black)
                Text(if (shiftOpen) "Open" else "Required", color = if (shiftOpen) NaomiSuccess else NaomiOrange, fontWeight = FontWeight.Black, fontSize = 12.sp)
                Text(duration ?: role, color = NaomiTextSecondary, fontSize = 8.sp)
            }
            if (!shiftOpen) {
                OutlinedButton(onClick = onOps, modifier = Modifier.fillMaxWidth().height(30.dp), contentPadding = ButtonDefaults.ContentPadding) {
                    Text("Open", fontSize = 8.sp)
                }
            }
        }
    }
}

@Composable
private fun TramMiniBoard(departures: List<TramDeparture>, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = NaomiSurface),
        border = BorderStroke(1.dp, NaomiBorder),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("BLACKPOOL · TOWER", color = NaomiOrange, fontSize = 8.sp, fontWeight = FontWeight.Black)
                Text("LIVE TRAMS", color = NaomiTextSecondary, fontSize = 7.sp, fontWeight = FontWeight.Bold)
            }
            if (departures.isEmpty()) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Live tram data unavailable", color = NaomiTextSecondary, fontSize = 9.sp)
                }
            } else {
                departures.forEach { departure ->
                    Surface(color = NaomiSurfaceVariant, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(departure.destination, color = NaomiTextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(departure.directionLabel, color = NaomiTextSecondary, fontSize = 7.sp, maxLines = 1)
                            }
                            Text(departure.departureTime, color = NaomiOrange, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShiftBadge(active: Boolean) {
    Surface(
        color = if (active) NaomiSuccess.copy(alpha = 0.12f) else NaomiOrange.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, if (active) NaomiSuccess.copy(alpha = 0.35f) else NaomiOrange.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Text(
            if (active) "SHIFT OPEN" else "NO SHIFT",
            color = if (active) NaomiSuccess else NaomiOrange,
            fontSize = 8.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun HealthTile(label: String, value: String, healthy: Boolean, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = NaomiSurface,
        shape = RoundedCornerShape(11.dp),
        border = BorderStroke(1.dp, NaomiBorder)
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(label.uppercase(), color = NaomiTextSecondary, fontSize = 7.sp, fontWeight = FontWeight.Bold)
            Text(
                value,
                color = if (healthy) NaomiSuccess else NaomiOrange,
                fontWeight = FontWeight.Black,
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalMinutes = ms.coerceAtLeast(0L) / 60_000L
    return "%dh %02dm".format(totalMinutes / 60, totalMinutes % 60)
}
