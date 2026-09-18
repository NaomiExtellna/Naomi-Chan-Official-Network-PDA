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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.VenuePreferences
import com.example.model.BlackpoolBar
import com.example.model.BlackpoolVenues
import com.example.ui.PosViewModel
import com.example.ui.theme.NaomiBorder
import com.example.ui.theme.NaomiDarkBg
import com.example.ui.theme.NaomiOrange
import com.example.ui.theme.NaomiRed
import com.example.ui.theme.NaomiSurface
import com.example.ui.theme.NaomiSurfaceVariant
import com.example.ui.theme.NaomiTextPrimary
import com.example.ui.theme.NaomiTextSecondary

private const val MANAGED_VENUES_PER_PAGE = 2

@Composable
fun VenueManagementScreen(
    viewModel: PosViewModel,
    onStartReceipt: () -> Unit
) {
    val context = LocalContext.current
    val store = remember { VenuePreferences(context) }
    var revision by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    var page by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }

    val customVenues = remember(revision) { store.getCustomVenues() }
    val builtInKeys = remember { BlackpoolVenues.EVENT_PLACES.map(VenuePreferences::venueKey).toSet() }
    val allVenues = remember(revision, query) {
        (BlackpoolVenues.EVENT_PLACES + customVenues)
            .distinctBy(VenuePreferences::venueKey)
            .filter { venue ->
                query.isBlank() || listOf(
                    venue.name, venue.address, venue.area, venue.barType, venue.nearestTramStop, venue.notes
                ).any { it.contains(query.trim(), ignoreCase = true) }
            }
            .sortedWith(
                compareByDescending<BlackpoolBar> { store.isFavourite(it) }
                    .thenBy { it.name.lowercase() }
            )
    }
    val pages = pageCount(allVenues.size, MANAGED_VENUES_PER_PAGE)
    val visible = pageSlice(allVenues, page, MANAGED_VENUES_PER_PAGE)
    LaunchedEffect(query, revision, allVenues.size) { page = page.coerceIn(0, pages - 1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NaomiDarkBg)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("VENUE MANAGER", color = NaomiOrange, fontSize = 8.sp, fontWeight = FontWeight.Black)
                Text("Favourites & custom", color = NaomiTextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Black)
            }
            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = NaomiRed),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.height(38.dp)
            ) {
                Icon(Icons.Default.AddLocationAlt, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add", fontSize = 9.sp)
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it; page = 0 },
            label = { Text("Search venues") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(17.dp)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        )

        Text(
            "${allVenues.size} venue(s) · favourites are pinned first",
            color = NaomiTextSecondary,
            fontSize = 8.sp
        )

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            if (visible.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = NaomiSurface),
                    border = BorderStroke(1.dp, NaomiBorder),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No venues found", color = NaomiTextPrimary, fontWeight = FontWeight.Black)
                        Text("Change the search term or add a venue.", color = NaomiTextSecondary, fontSize = 9.sp)
                    }
                }
            } else {
                visible.forEach { venue ->
                    ManagedVenueCard(
                        venue = venue,
                        favourite = store.isFavourite(venue),
                        isCustom = VenuePreferences.venueKey(venue) !in builtInKeys,
                        onFavourite = {
                            store.toggleFavourite(venue)
                            revision++
                        },
                        onReceipt = {
                            viewModel.resetNewReceipt()
                            viewModel.selectBlackpoolBar(venue)
                            onStartReceipt()
                        },
                        onDelete = {
                            store.deleteCustomVenue(venue)
                            revision++
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat((MANAGED_VENUES_PER_PAGE - visible.size).coerceAtLeast(0)) { Spacer(modifier = Modifier.weight(1f)) }
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

    if (showAddDialog) {
        AddVenueDialog(
            onDismiss = { showAddDialog = false },
            onSave = { venue ->
                store.saveCustomVenue(venue)
                revision++
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun ManagedVenueCard(
    venue: BlackpoolBar,
    favourite: Boolean,
    isCustom: Boolean,
    onFavourite: () -> Unit,
    onReceipt: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = NaomiSurface),
        border = BorderStroke(1.dp, if (favourite) NaomiOrange else NaomiBorder),
        shape = RoundedCornerShape(4.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(venue.name, color = NaomiTextPrimary, fontWeight = FontWeight.Black, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(venue.barType, color = NaomiOrange, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    Text(venue.address, color = NaomiTextSecondary, fontSize = 8.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (venue.nearestTramStop.isNotBlank()) Text("Tram: ${venue.nearestTramStop}", color = NaomiTextSecondary, fontSize = 7.sp)
                }
                IconButton(onClick = onFavourite) {
                    Icon(
                        if (favourite) Icons.Default.Star else Icons.Outlined.StarOutline,
                        contentDescription = if (favourite) "Remove favourite" else "Favourite",
                        tint = if (favourite) NaomiOrange else NaomiTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = onReceipt,
                    colors = ButtonDefaults.buttonColors(containerColor = NaomiRed),
                    modifier = Modifier.weight(1f).height(36.dp),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Sale", fontSize = 8.sp)
                }
                if (isCustom) {
                    OutlinedButton(onClick = onDelete, modifier = Modifier.weight(1f).height(36.dp), shape = RoundedCornerShape(4.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Delete", fontSize = 8.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun AddVenueDialog(
    onDismiss: () -> Unit,
    onSave: (BlackpoolBar) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var area by remember { mutableStateOf("Custom Venue") }
    var type by remember { mutableStateOf("Event Venue") }
    var contact by remember { mutableStateOf("") }
    var tramStop by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NaomiSurface,
        title = { Text("Add Custom Venue", color = NaomiTextPrimary, fontWeight = FontWeight.Black) },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                VenueField("Venue name", name) { name = it }
                VenueField("Address", address) { address = it }
                VenueField("Area", area) { area = it }
                VenueField("Venue type", type) { type = it }
                VenueField("Contact", contact) { contact = it }
                VenueField("Nearest tram stop", tramStop) { tramStop = it }
                VenueField("Notes", notes) { notes = it }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        BlackpoolBar(
                            name = name.trim(),
                            address = address.trim(),
                            area = area.trim().ifBlank { "Custom Venue" },
                            barType = type.trim().ifBlank { "Event Venue" },
                            contact = contact.trim(),
                            nearestTramStop = tramStop.trim(),
                            notes = notes.trim().ifBlank { "Staff-added venue" }
                        )
                    )
                },
                enabled = name.trim().length >= 2 && address.trim().length >= 3,
                colors = ButtonDefaults.buttonColors(containerColor = NaomiRed)
            ) { Text("Save Venue") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = NaomiTextSecondary) } }
    )
}

@Composable
private fun VenueField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 9.sp) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().height(49.dp)
    )
}