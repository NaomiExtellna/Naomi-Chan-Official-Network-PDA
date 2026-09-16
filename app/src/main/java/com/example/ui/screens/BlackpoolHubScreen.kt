package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsTransit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Schedule
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BlackpoolBar
import com.example.model.BlackpoolVenues
import com.example.network.BlackpoolTramClient
import com.example.network.BlackpoolTramStops
import com.example.network.TramDeparture
import com.example.network.TramStopInfo
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
import java.util.TimeZone

private enum class BlackpoolHubMode {
    VENUES,
    TRAMS,
    ALL_STOPS
}

@Composable
fun BlackpoolHubScreen(
    viewModel: PosViewModel,
    onStartReceipt: () -> Unit
) {
    var mode by remember { mutableStateOf(BlackpoolHubMode.VENUES) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NaomiDarkBg)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = NaomiSurface),
            shape = RoundedCornerShape(0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "BLACKPOOL STAFF HUB",
                    color = NaomiOrange,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Venues & Tramway",
                    color = NaomiTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HubModeChip(
                        selected = mode == BlackpoolHubMode.VENUES,
                        label = "Venues",
                        icon = Icons.Default.LocationOn,
                        onClick = { mode = BlackpoolHubMode.VENUES },
                        modifier = Modifier.weight(1f)
                    )
                    HubModeChip(
                        selected = mode == BlackpoolHubMode.TRAMS,
                        label = "Live",
                        icon = Icons.Default.DirectionsTransit,
                        onClick = { mode = BlackpoolHubMode.TRAMS },
                        modifier = Modifier.weight(1f)
                    )
                    HubModeChip(
                        selected = mode == BlackpoolHubMode.ALL_STOPS,
                        label = "All Stops",
                        icon = Icons.Default.List,
                        onClick = { mode = BlackpoolHubMode.ALL_STOPS },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when (mode) {
                BlackpoolHubMode.VENUES -> VenueDirectory(
                    viewModel = viewModel,
                    onStartReceipt = onStartReceipt
                )
                BlackpoolHubMode.TRAMS -> TramBoard()
                BlackpoolHubMode.ALL_STOPS -> AllTramStops()
            }
        }
    }
}

@Composable
private fun HubModeChip(
    selected: Boolean,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontSize = 10.sp) },
        leadingIcon = {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = NaomiRed,
            selectedLabelColor = Color.White,
            selectedLeadingIconColor = Color.White
        ),
        modifier = modifier
    )
}

@Composable
private fun VenueDirectory(
    viewModel: PosViewModel,
    onStartReceipt: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var selectedArea by remember { mutableStateOf("All Blackpool") }

    val filtered = remember(query, selectedArea) {
        BlackpoolVenues.EVENT_PLACES.filter { venue ->
            val matchesArea = selectedArea == "All Blackpool" || venue.area == selectedArea
            val search = query.trim()
            val matchesSearch = search.isBlank() || listOf(
                venue.name,
                venue.address,
                venue.area,
                venue.barType,
                venue.nearestTramStop,
                venue.notes
            ).any { it.contains(search, ignoreCase = true) }
            matchesArea && matchesSearch
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search Blackpool venues") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(BlackpoolVenues.AREAS) { area ->
                FilterChip(
                    selected = selectedArea == area,
                    onClick = { selectedArea = area },
                    label = { Text(area, fontSize = 10.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NaomiRed,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Text(
            text = "${filtered.size} venue${if (filtered.size == 1) "" else "s"} • tap Create Receipt to pre-fill the booking location",
            color = NaomiTextSecondary,
            fontSize = 10.sp
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(
                items = filtered,
                key = { "${it.name}|${it.address}" }
            ) { venue ->
                VenueCard(
                    venue = venue,
                    onCreateReceipt = {
                        viewModel.resetNewReceipt()
                        viewModel.selectBlackpoolBar(venue)
                        onStartReceipt()
                    }
                )
            }
            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun VenueCard(
    venue: BlackpoolBar,
    onCreateReceipt: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = NaomiSurface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, NaomiBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(venue.name, color = NaomiTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Black)
            Text(venue.barType, color = NaomiOrange, fontSize = 11.sp, fontWeight = FontWeight.Bold)

            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = NaomiTextSecondary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(venue.address, color = NaomiTextSecondary, fontSize = 11.sp)
            }

            if (venue.nearestTramStop.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DirectionsTransit, contentDescription = null, tint = NaomiTextSecondary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Nearest tram: ${venue.nearestTramStop}", color = NaomiTextSecondary, fontSize = 11.sp)
                }
            }

            if (venue.contact.isNotBlank()) {
                Text("Contact: ${venue.contact}", color = NaomiTextSecondary, fontSize = 10.sp)
            }
            if (venue.notes.isNotBlank()) {
                Text(venue.notes, color = NaomiTextSecondary, fontSize = 10.sp)
            }

            Button(
                onClick = onCreateReceipt,
                colors = ButtonDefaults.buttonColors(containerColor = NaomiRed),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Create Receipt Here", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun AllTramStops() {
    var query by remember { mutableStateOf("") }
    val mainStops = remember(query) {
        BlackpoolTramStops.ALL_STOPS.filter {
            query.isBlank() || it.name.contains(query, true) || it.area.contains(query, true) || it.note.contains(query, true)
        }
    }
    val branchStops = remember(query) {
        BlackpoolTramStops.NORTH_STATION_BRANCH.filter {
            query.isBlank() || it.name.contains(query, true) || it.area.contains(query, true) || it.note.contains(query, true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search tram stops") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = NaomiSurfaceVariant),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "BLACKPOOL TRAMWAY • COMPLETE STOP LIST",
                    color = NaomiOrange,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Starr Gate → Fleetwood Ferry, plus the North Station branch",
                    color = NaomiTextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Current 2026 base daytime timetable: from every 15 minutes. Extra/overlapping workings can create shorter gaps through central Blackpool.",
                    color = NaomiTextSecondary,
                    fontSize = 10.sp
                )
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            if (mainStops.isNotEmpty()) {
                item {
                    Text(
                        "MAIN LINE • SOUTH TO NORTH",
                        color = NaomiTextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                items(
                    items = mainStops,
                    key = { "main-${it.name}" }
                ) { stop ->
                    val officialIndex = BlackpoolTramStops.ALL_STOPS.indexOf(stop) + 1
                    TramStopCard(number = officialIndex, stop = stop)
                }
            }

            if (branchStops.isNotEmpty()) {
                item {
                    Text(
                        "NORTH STATION BRANCH",
                        color = NaomiOrange,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(
                    items = branchStops,
                    key = { "branch-${it.name}" }
                ) { stop ->
                    TramStopCard(number = null, stop = stop)
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun TramStopCard(number: Int?, stop: TramStopInfo) {
    Card(
        colors = CardDefaults.cardColors(containerColor = NaomiSurface),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (stop.isBranchStop) NaomiOrange else NaomiBorder
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(
                        if (stop.isBranchStop) NaomiOrange.copy(alpha = 0.16f) else NaomiSurfaceVariant,
                        RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = number?.toString() ?: "B",
                    color = if (stop.isBranchStop) NaomiOrange else NaomiTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(stop.name, color = NaomiTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(stop.area, color = NaomiTextSecondary, fontSize = 10.sp)
                if (stop.note.isNotBlank()) {
                    Text(stop.note, color = if (stop.isBranchStop) NaomiOrange else NaomiTextSecondary, fontSize = 9.sp)
                }
            }
        }
    }
}

@Composable
private fun TramBoard() {
    val client = remember { BlackpoolTramClient() }
    var selectedStop by remember { mutableStateOf(BlackpoolTramStops.FEATURED.first()) }
    var departures by remember { mutableStateOf<List<TramDeparture>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var refreshKey by remember { mutableIntStateOf(0) }
    var clockText by remember { mutableStateOf("--:--:--") }

    LaunchedEffect(Unit) {
        val clockFormat = SimpleDateFormat("HH:mm:ss", Locale.UK).apply {
            timeZone = TimeZone.getTimeZone("Europe/London")
        }
        while (true) {
            clockText = clockFormat.format(Date())
            delay(1_000)
        }
    }

    LaunchedEffect(selectedStop, refreshKey) {
        while (true) {
            loading = true
            val latest = client.fetchDepartures(selectedStop)
            departures = latest
            errorText = if (latest.isEmpty()) {
                "No live departures were returned. Check connectivity or refresh again."
            } else {
                null
            }
            loading = false
            delay(60_000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = NaomiSurface),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, NaomiBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("BLACKPOOL LOCAL TIME", color = NaomiOrange, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = clockText,
                        color = NaomiTextPrimary,
                        fontSize = 29.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Icon(Icons.Default.Schedule, contentDescription = null, tint = NaomiOrange, modifier = Modifier.size(30.dp))
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = NaomiSurfaceVariant),
            shape = RoundedCornerShape(9.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Service guide: the current 2026 base daytime timetable is from every 15 mins. Central sections may have shorter gaps where services overlap. Live departures below take priority.",
                color = NaomiTextSecondary,
                fontSize = 9.sp,
                modifier = Modifier.padding(10.dp)
            )
        }

        Text("Live tram stop", color = NaomiTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(BlackpoolTramStops.FEATURED) { stop ->
                FilterChip(
                    selected = selectedStop == stop,
                    onClick = { selectedStop = stop },
                    label = { Text(stop.name, fontSize = 10.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NaomiRed,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(selectedStop.name, color = NaomiTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Black)
                Text(
                    text = if (loading) "Refreshing Blackpool Transport…" else "Auto-refreshes every 60 seconds",
                    color = NaomiTextSecondary,
                    fontSize = 10.sp
                )
            }
            OutlinedButton(onClick = { refreshKey++ }) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(5.dp))
                Text("Refresh", fontSize = 11.sp)
            }
        }

        if (errorText != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = NaomiSurfaceVariant),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = errorText.orEmpty(),
                    color = NaomiOrange,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = departures,
                key = { "${it.directionLabel}|${it.destination}|${it.departureTime}" }
            ) { departure ->
                DepartureCard(departure)
            }
            item {
                Text(
                    text = "Departure data is read from Blackpool Transport public live-departure pages. Times can change during disruption; the official BPL Transport service remains authoritative.",
                    color = NaomiTextSecondary,
                    fontSize = 9.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun DepartureCard(departure: TramDeparture) {
    Card(
        colors = CardDefaults.cardColors(containerColor = NaomiSurface),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (departure.isLive) NaomiSuccess else NaomiBorder
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(departure.destination, color = NaomiTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = "${departure.directionLabel} • ${if (departure.isLive) "LIVE" else "Scheduled"}",
                    color = if (departure.isLive) NaomiSuccess else NaomiTextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = departure.departureTime,
                color = NaomiOrange,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
