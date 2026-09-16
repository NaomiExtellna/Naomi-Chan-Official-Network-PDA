package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.network.BlackpoolTramClient
import com.example.network.BlackpoolTramStops
import com.example.network.TramDeparture
import com.example.ui.AuthViewModel
import com.example.ui.PosViewModel
import com.example.ui.theme.NaomiBorder
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

@Composable
fun EventDashboardScreen(
    authViewModel: AuthViewModel,
    posViewModel: PosViewModel,
    onNewReceipt: () -> Unit,
    onBlackpool: () -> Unit,
    onOps: () -> Unit
) {
    val authState by authViewModel.state.collectAsState()
    val receipt by posViewModel.currentReceipt.collectAsState()
    val printer by posViewModel.printerStatus.collectAsState()
    val unsynced by posViewModel.unsyncedCount.collectAsState()
    val gatewayOnline by posViewModel.isWirelessOnline.collectAsState()
    val user = authState.currentUser ?: return
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    var departures by remember { mutableStateOf<List<TramDeparture>>(emptyList()) }
    val tramClient = remember { BlackpoolTramClient() }
    val tower = remember { BlackpoolTramStops.FEATURED.firstOrNull { it.name == "Tower" } ?: BlackpoolTramStops.FEATURED.first() }

    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    LaunchedEffect(tower.name) {
        while (true) {
            departures = tramClient.fetchDepartures(tower)
            delay(60_000)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("EVENT MODE", color = NaomiOrange, fontSize = 10.sp, fontWeight = FontWeight.Black)
        Text("${formatClock(now)} • ${user.displayName}", color = NaomiTextPrimary, fontSize = 23.sp, fontWeight = FontWeight.Black)
        Text(formatDate(now), color = NaomiTextSecondary, fontSize = 11.sp)

        DashboardCard("Current Shift") {
            if (authState.activeShift == null) {
                Text("NO OPEN SHIFT", color = NaomiOrange, fontWeight = FontWeight.Black)
                Text("Open a shift in Ops before finalising any new transaction.", color = NaomiTextSecondary, fontSize = 10.sp)
                Button(onClick = onOps, colors = ButtonDefaults.buttonColors(containerColor = NaomiRed), modifier = Modifier.fillMaxWidth()) { Text("Open Ops / Start Shift") }
            } else {
                Text("OPEN • ${formatDuration(now - authState.activeShift!!.openedAt)}", color = NaomiSuccess, fontWeight = FontWeight.Black)
                Text("Shift ${authState.activeShift!!.id.take(8)}", color = NaomiTextSecondary, fontSize = 9.sp)
            }
        }

        DashboardCard("Event / Venue") {
            Text(receipt.venueName, color = NaomiTextPrimary, fontWeight = FontWeight.Bold)
            Text("Receipt draft: ${receipt.id}", color = NaomiTextSecondary, fontSize = 9.sp)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { posViewModel.resetNewReceipt(); onNewReceipt() },
                colors = ButtonDefaults.buttonColors(containerColor = NaomiRed),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(5.dp))
                Text("New Receipt", fontWeight = FontWeight.Black)
            }
            OutlinedButton(onClick = onBlackpool, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.LocationOn, contentDescription = null)
                Spacer(modifier = Modifier.width(5.dp))
                Text("Blackpool")
            }
        }

        DashboardCard("PDA Status") {
            DashboardLine("Printer", if (printer.isConnected) "READY" else "OFFLINE")
            DashboardLine("Gateway", if (gatewayOnline) "ONLINE" else "OFFLINE")
            DashboardLine("Unsynced tickets", unsynced.toString())
            DashboardLine("Role", if (user.isAdmin) "Naomi Admin" else "Staff")
        }

        DashboardCard("Next Trams • Tower") {
            if (departures.isEmpty()) {
                Text("No live departures loaded. Use Blackpool → Live for the full board.", color = NaomiTextSecondary, fontSize = 10.sp)
            } else {
                departures.take(4).forEach { departure ->
                    DashboardLine(
                        "${departure.directionLabel} → ${departure.destination}",
                        departure.departureTime + if (departure.isLive) " LIVE" else ""
                    )
                }
            }
        }

        OutlinedButton(onClick = onOps, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Settings, contentDescription = null)
            Spacer(modifier = Modifier.width(5.dp))
            Text("Staff Operations & Maintenance")
        }
    }
}

@Composable
private fun DashboardCard(title: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = NaomiSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, NaomiBorder),
        shape = RoundedCornerShape(11.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, color = NaomiOrange, fontWeight = FontWeight.Black, fontSize = 10.sp)
            content()
        }
    }
}

@Composable
private fun DashboardLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = NaomiTextSecondary, fontSize = 10.sp)
        Text(value, color = NaomiTextPrimary, fontWeight = FontWeight.Bold, fontSize = 10.sp)
    }
}

private fun formatClock(value: Long): String = SimpleDateFormat("HH:mm:ss", Locale.UK).format(Date(value))
private fun formatDate(value: Long): String = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.UK).format(Date(value))
private fun formatDuration(ms: Long): String {
    val totalMinutes = (ms.coerceAtLeast(0L) / 60_000L)
    return "%dh %02dm".format(totalMinutes / 60, totalMinutes % 60)
}
