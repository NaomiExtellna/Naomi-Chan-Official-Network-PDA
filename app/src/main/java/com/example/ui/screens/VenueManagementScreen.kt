package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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

@Composable
fun VenueManagementScreen(
    viewModel: PosViewModel,
    onStartReceipt: () -> Unit
) {
    val context = LocalContext.current
    val store = remember { VenuePreferences(context) }
    var revision by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    val customVenues = remember(revision) { store.getCustomVenues() }
    val builtInKeys = remember { BlackpoolVenues.EVENT_PLACES.map(VenuePreferences::venueKey).toSet() }
    val allVenues = remember(revision, query) {
        (BlackpoolVenues.EVENT_PLACES + customVenues)
            .distinctBy(VenuePreferences::venueKey)
            .filter { venue ->
                query.isBlank() || listOf(
                    venue.name,
                    venue.address,
                    venue.area,
                    venue.barType,
                    venue.nearestTramStop,
                    venue.notes
                ).any { it.contains(query.trim(), ignoreCase = true) }
            }
            .sortedWith(
                compareByDescending<BlackpoolBar> { store.isFavourite(it) }
                    .thenBy { it.name.lowercase() }
            )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NaomiDarkBg)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("VENUE MANAGER", color = NaomiOrange, fontSize = 10.sp, fontWeight = FontWeight.Black)
                Text("Favourites & Custom Venues", color = NaomiTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Black)
            }
            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = NaomiRed)
            ) {
                Icon(Icons.Default.AddLocationAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add", fontSize = 10.sp)
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search venues") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            "Star venues to pin them to the top. Custom venues stay on this PDA.",
            color = NaomiTextSecondary,
            fontSize = 9.5.sp
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(allVenues, key = VenuePreferences::venueKey) { venue ->
                val favourite = store.isFavourite(venue)
                val isCustom = VenuePreferences.venueKey(venue) !in builtInKeys
                Card(
                    colors = CardDefaults.cardColors(containerColor = NaomiSurface),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (favourite) NaomiOrange else NaomiBorder
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(11.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(venue.name, color = NaomiTextPrimary, fontWeight = FontWeight.Black, fontSize = 14.sp)
                                Text(venue.barType, color = NaomiOrange, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(venue.address, color = NaomiTextSecondary, fontSize = 10.sp)
                                if (venue.nearestTramStop.isNotBlank()) {
                                    Text("Tram: ${venue.nearestTramStop}", color = NaomiTextSecondary, fontSize = 9.sp)
                                }
                            }
                            IconButton(
                                onClick = {
                                    store.toggleFavourite(venue)
                                    revision++
                                }
                            ) {
                                Icon(
                                    if (favourite) Icons.Default.Star else Icons.Outlined.StarOutline,
                                    contentDescription = if (favourite) "Remove favourite" else "Favourite",
                                    tint = if (favourite) NaomiOrange else NaomiTextSecondary
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            Button(
                                onClick = {
                                    viewModel.resetNewReceipt()
                                    viewModel.selectBlackpoolBar(venue)
                                    onStartReceipt()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NaomiRed),
                                modifier = Modifier.weight(1f).height(34.dp)
                            ) {
                                Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Receipt", fontSize = 9.sp)
                            }
                            if (isCustom) {
                                OutlinedButton(
                                    onClick = {
                                        store.deleteCustomVenue(venue)
                                        revision++
                                    },
                                    modifier = Modifier.weight(1f).height(34.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Delete", fontSize = 9.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
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
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
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
            ) {
                Text("Save Venue")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = NaomiTextSecondary) }
        }
    )
}

@Composable
private fun VenueField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}
