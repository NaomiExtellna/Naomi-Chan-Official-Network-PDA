package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
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

private enum class BlackpoolHubMode { VENUES, TRAMS, ALL_STOPS }
private const val VENUES_PER_PAGE = 2
private const val STOPS_PER_PAGE = 4
private const val DEPARTURES_PER_PAGE = 4
private const val TRAM_CLOCK_TICK_MS = 60_000L
private const val TRAM_AUTO_REFRESH_MS = 120_000L

private data class PagedTramStop(val number: Int?, val stop: TramStopInfo)

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
            border = BorderStroke(0.dp, Color.Transparent),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("BLACKPOOL STAFF HUB", color = NaomiOrange, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                        Text("Venues & Tramway", color = NaomiTextPrimary, fontSize = 19.sp, fontWeight = FontWeight.Black)
                    }
                    Icon(Icons.Default.DirectionsTransit, contentDescription = null, tint = NaomiOrange, modifier = Modifier.size(26.dp))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    HubModeChip(mode == BlackpoolHubMode.VENUES, "Venues", Icons.Default.LocationOn, { mode = BlackpoolHubMode.VENUES }, Modifier.weight(1f))
                    HubModeChip(mode == BlackpoolHubMode.TRAMS, "Live", Icons.Default.DirectionsTransit, { mode = BlackpoolHubMode.TRAMS }, Modifier.weight(1f))
                    HubModeChip(mode == BlackpoolHubMode.ALL_STOPS, "Stops", Icons.Default.List, { mode = BlackpoolHubMode.ALL_STOPS }, Modifier.weight(1f))
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when (mode) {
                BlackpoolHubMode.VENUES -> VenueDirectory(viewModel, onStartReceipt)
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
        label = { Text(label, fontSize = 9.sp) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp)) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = NaomiRed,
            selectedLabelColor = Color.White,
            selectedLeadingIconColor = Color.White
        ),
        modifier = modifier.height(36.dp)
    )
}

@Composable
private fun VenueDirectory(viewModel: PosViewModel, onStartReceipt: () -> Unit) {
    var query by remember { mutableStateOf("") }
    var areaIndex by remember { mutableIntStateOf(0) }
    var page by remember { mutableIntStateOf(0) }
    val areas = remember { BlackpoolVenues.AREAS.ifEmpty { listOf("All Blackpool") } }
    val selectedArea = areas[areaIndex.coerceIn(0, areas.lastIndex)]

    val filtered = remember(query, selectedArea) {
        BlackpoolVenues.EVENT_PLACES.filter { venue ->
            val matchesArea = selectedArea == "All Blackpool" || venue.area == selectedArea
            val search = query.trim()
            val matchesSearch = search.isBlank() || listOf(
                venue.name, venue.address, venue.area, venue.barType, venue.nearestTramStop, venue.notes
            ).any { it.contains(search, ignoreCase = true) }
            matchesArea && matchesSearch
        }
    }
    val pages = pageCount(filtered.size, VENUES_PER_PAGE)
    val visible = pageSlice(filtered, page, VENUES_PER_PAGE)

    LaunchedEffect(query, areaIndex, filtered.size) { page = page.coerceIn(0, pages - 1) }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it; page = 0 },
            label = { Text("Search venues") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        )

        SelectorBar(
            label = selectedArea,
            detail = "${filtered.size} venue${if (filtered.size == 1) "" else "s"}",
            canPrevious = areaIndex > 0,
            canNext = areaIndex < areas.lastIndex,
            onPrevious = { areaIndex--; page = 0 },
            onNext = { areaIndex++; page = 0 }
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            if (visible.isEmpty()) {
                EmptyPosPanel("No venues match this view", "Change the area or search term.")
            } else {
                visible.forEach { venue ->
                    VenueCardCompact(
                        venue = venue,
                        onCreateReceipt = {
                            viewModel.resetNewReceipt()
                            viewModel.selectBlackpoolBar(venue)
                            onStartReceipt()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat((VENUES_PER_PAGE - visible.size).coerceAtLeast(0)) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        PosPager(
            page = page,
            totalPages = pages,
            onPrevious = { page = (page - 1).coerceAtLeast(0) },
            onNext = { page = (page + 1).coerceAtMost(pages - 1) },
            label = "VENUE"
        )
    }
}

@Composable
private fun VenueCardCompact(
    venue: BlackpoolBar,
    onCreateReceipt: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = NaomiSurface),
        shape = RoundedCornerShape(5.dp),
        border = BorderStroke(1.dp, NaomiBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(11.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        venue.name,
                        color = NaomiTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(venue.barType, color = NaomiOrange, fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
                Text(venue.address, color = NaomiTextSecondary, fontSize = 9.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (venue.nearestTramStop.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DirectionsTransit, contentDescription = null, tint = NaomiTextSecondary, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("${venue.nearestTramStop} tram", color = NaomiTextSecondary, fontSize = 8.sp, maxLines = 1)
                    }
                }
                if (venue.notes.isNotBlank()) {
                    Text(venue.notes, color = NaomiTextSecondary, fontSize = 8.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Button(
                onClick = onCreateReceipt,
                colors = ButtonDefaults.buttonColors(containerColor = NaomiRed),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.fillMaxWidth().height(38.dp)
            ) {
                Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(5.dp))
                Text("Create sale here", fontWeight = FontWeight.Bold, fontSize = 9.sp)
            }
        }
    }
}

@Composable
private fun AllTramStops() {
    var query by remember { mutableStateOf("") }
    var page by remember { mutableIntStateOf(0) }

    val allStops = remember(query) {
        val main = BlackpoolTramStops.ALL_STOPS.mapIndexed { index, stop -> PagedTramStop(index + 1, stop) }
        val branch = BlackpoolTramStops.NORTH_STATION_BRANCH.map { PagedTramStop(null, it) }
        (main + branch).filter { entry ->
            val q = query.trim()
            q.isBlank() || entry.stop.name.contains(q, true) || entry.stop.area.contains(q, true) || entry.stop.note.contains(q, true)
        }
    }
    val pages = pageCount(allStops.size, STOPS_PER_PAGE)
    val visible = pageSlice(allStops, page, STOPS_PER_PAGE)
    LaunchedEffect(query, allStops.size) { page = page.coerceIn(0, pages - 1) }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it; page = 0 },
            label = { Text("Search tram stops") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        )

        Surface(
            color = NaomiSurfaceVariant,
            shape = RoundedCornerShape(4.dp),
            border = BorderStroke(1.dp, NaomiBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("BLACKPOOL TRAMWAY", color = NaomiOrange, fontSize = 8.sp, fontWeight = FontWeight.Black)
                    Text("Starr Gate ↔ Fleetwood + North Station", color = NaomiTextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Text("${allStops.size} stops", color = NaomiTextSecondary, fontSize = 9.sp)
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (visible.isEmpty()) {
                EmptyPosPanel("No tram stops found", "Change the search term.")
            } else {
                visible.forEach { entry ->
                    TramStopCardCompact(entry.number, entry.stop, Modifier.weight(1f))
                }
                repeat((STOPS_PER_PAGE - visible.size).coerceAtLeast(0)) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }

        PosPager(
            page = page,
            totalPages = pages,
            onPrevious = { page = (page - 1).coerceAtLeast(0) },
            onNext = { page = (page + 1).coerceAtMost(pages - 1) },
            label = "STOP"
        )
    }
}

@Composable
private fun TramStopCardCompact(number: Int?, stop: TramStopInfo, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = NaomiSurface),
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, if (stop.isBranchStop) NaomiOrange.copy(alpha = 0.55f) else NaomiBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(32.dp).background(
                    if (stop.isBranchStop) NaomiOrange.copy(alpha = 0.13f) else NaomiSurfaceVariant,
                    RoundedCornerShape(3.dp)
                ),
                contentAlignment = Alignment.Center
            ) {
                Text(number?.toString() ?: "B", color = if (stop.isBranchStop) NaomiOrange else NaomiTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
            Spacer(modifier = Modifier.width(9.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(stop.name, color = NaomiTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(stop.area, color = NaomiTextSecondary, fontSize = 8.sp)
                if (stop.note.isNotBlank()) Text(stop.note, color = if (stop.isBranchStop) NaomiOrange else NaomiTextSecondary, fontSize = 7.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun TramBoard() {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val client = remember(context.applicationContext) { BlackpoolTramClient(context.applicationContext) }
    val featured = remember { BlackpoolTramStops.FEATURED }
    var stopIndex by remember { mutableIntStateOf(0) }
    var departures by remember { mutableStateOf<List<TramDeparture>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var refreshKey by remember { mutableIntStateOf(0) }
    var lastHandledRefreshKey by remember { mutableIntStateOf(0) }
    var clockText by remember { mutableStateOf("--:--") }
    var page by remember { mutableIntStateOf(0) }
    val selectedStop = featured[stopIndex.coerceIn(0, featured.lastIndex)]

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            val clockFormat = SimpleDateFormat("HH:mm", Locale.UK).apply {
                timeZone = TimeZone.getTimeZone("Europe/London")
            }
            while (true) {
                val now = System.currentTimeMillis()
                clockText = clockFormat.format(Date(now))
                delay((TRAM_CLOCK_TICK_MS - (now % TRAM_CLOCK_TICK_MS)).coerceAtLeast(1_000L))
            }
        }
    }

    LaunchedEffect(lifecycleOwner, selectedStop, refreshKey) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            var forceNetwork = refreshKey != lastHandledRefreshKey
            if (forceNetwork) lastHandledRefreshKey = refreshKey
            while (true) {
                loading = true
                val latest = client.fetchDepartures(
                    stop = selectedStop,
                    forceRefresh = forceNetwork
                )
                forceNetwork = false
                departures = latest
                errorText = if (latest.isEmpty()) "No cached or live departures available." else null
                page = 0
                loading = false
                delay(TRAM_AUTO_REFRESH_MS)
            }
        }
    }

    val pages = pageCount(departures.size, DEPARTURES_PER_PAGE)
    val visible = pageSlice(departures, page, DEPARTURES_PER_PAGE)

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = NaomiSurface),
            shape = RoundedCornerShape(5.dp),
            border = BorderStroke(1.dp, NaomiBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("BLACKPOOL LOCAL TIME", color = NaomiOrange, fontSize = 8.sp, fontWeight = FontWeight.Black)
                    Text(clockText, color = NaomiTextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = NaomiOrange, modifier = Modifier.size(24.dp))
                    Text("Room cache · HTTP max 5m", color = NaomiTextSecondary, fontSize = 7.sp)
                }
            }
        }

        SelectorBar(
            label = selectedStop.name,
            detail = if (loading) {
                "Checking local cache…"
            } else {
                val cached = departures.any { departure ->
                    departure.directionLabel.contains("CACHE", ignoreCase = true) ||
                        departure.directionLabel.contains("OFFLINE", ignoreCase = true)
                }
                if (cached) "Cached stop ${stopIndex + 1}/${featured.size}" else "Live stop ${stopIndex + 1}/${featured.size}"
            },
            canPrevious = stopIndex > 0,
            canNext = stopIndex < featured.lastIndex,
            onPrevious = { stopIndex--; page = 0 },
            onNext = { stopIndex++; page = 0 },
            trailing = {
                OutlinedButton(onClick = { refreshKey++ }, modifier = Modifier.height(36.dp)) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                }
            }
        )

        Surface(
            color = NaomiSurfaceVariant,
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "Local database first · network only when cache is stale · refresh forces live",
                color = NaomiTextSecondary,
                fontSize = 8.sp,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            when {
                errorText != null -> EmptyPosPanel("Live data unavailable", errorText.orEmpty())
                departures.isEmpty() -> EmptyPosPanel("Waiting for departures", "Live departures will appear here when available.")
                else -> {
                    visible.forEach { departure ->
                        DepartureCardCompact(departure, Modifier.weight(1f))
                    }
                    repeat((DEPARTURES_PER_PAGE - visible.size).coerceAtLeast(0)) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }
        }

        PosPager(
            page = page,
            totalPages = pages,
            onPrevious = { page = (page - 1).coerceAtLeast(0) },
            onNext = { page = (page + 1).coerceAtMost(pages - 1) },
            label = "DEP"
        )
    }
}

@Composable
private fun DepartureCardCompact(departure: TramDeparture, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = NaomiSurface),
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, if (departure.isLive) NaomiSuccess.copy(alpha = 0.55f) else NaomiBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 11.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(departure.destination, color = NaomiTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${departure.directionLabel} · ${if (departure.isLive) "LIVE" else "Scheduled"}",
                    color = if (departure.isLive) NaomiSuccess else NaomiTextSecondary,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(departure.departureTime, color = NaomiOrange, fontSize = 17.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun SelectorBar(
    label: String,
    detail: String,
    canPrevious: Boolean,
    canNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(onClick = onPrevious, enabled = canPrevious, modifier = Modifier.height(38.dp)) { Text("‹", fontSize = 18.sp) }
        Surface(
            modifier = Modifier.weight(1f).height(38.dp),
            color = NaomiSurface,
            border = BorderStroke(1.dp, NaomiBorder),
            shape = RoundedCornerShape(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 9.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, color = NaomiTextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text(detail, color = NaomiTextSecondary, fontSize = 7.sp, maxLines = 1)
            }
        }
        trailing?.invoke()
        OutlinedButton(onClick = onNext, enabled = canNext, modifier = Modifier.height(38.dp)) { Text("›", fontSize = 18.sp) }
    }
}

@Composable
private fun EmptyPosPanel(title: String, subtitle: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = NaomiSurface),
        border = BorderStroke(1.dp, NaomiBorder),
        shape = RoundedCornerShape(5.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(title, color = NaomiTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Black)
            Text(subtitle, color = NaomiTextSecondary, fontSize = 9.sp)
        }
    }
}
