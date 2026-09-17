package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AuthViewModel
import com.example.ui.theme.NaomiBorder
import com.example.ui.theme.NaomiOrange
import com.example.ui.theme.NaomiSurface
import com.example.ui.theme.NaomiSurfaceVariant
import com.example.ui.theme.NaomiTextPrimary
import com.example.ui.theme.NaomiTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val AUDIT_EVENTS_PER_PAGE = 3

@Composable
fun AuditLogScreen(authViewModel: AuthViewModel) {
    val state by authViewModel.state.collectAsState()
    val events by authViewModel.auditEvents.collectAsState()
    val user = state.currentUser ?: return
    var query by remember { mutableStateOf("") }
    var page by remember { mutableIntStateOf(0) }

    if (!user.isAdmin) {
        Column(
            modifier = Modifier.fillMaxSize().padding(18.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Audit Log", color = NaomiTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Black)
            Text("Only Naomi (Admin) can view the operational audit trail.", color = NaomiTextSecondary, fontSize = 10.sp)
        }
        return
    }

    val filtered = remember(events, query) {
        events.filter { event ->
            query.isBlank() || listOf(event.actorName, event.action, event.target, event.details, event.severity)
                .any { it.contains(query.trim(), ignoreCase = true) }
        }
    }
    val pages = pageCount(filtered.size, AUDIT_EVENTS_PER_PAGE)
    val visible = pageSlice(filtered, page, AUDIT_EVENTS_PER_PAGE)
    LaunchedEffect(query, filtered.size) { page = page.coerceIn(0, pages - 1) }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Column {
            Text("AUDIT TRAIL", color = NaomiOrange, fontSize = 8.sp, fontWeight = FontWeight.Black)
            Text("Staff & System Activity", color = NaomiTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Black)
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it; page = 0 },
            label = { Text("Search audit") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().height(50.dp)
        )

        Text("${filtered.size} event(s) · audit survives ticket wipes", color = NaomiTextSecondary, fontSize = 7.sp)

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (visible.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = NaomiSurface),
                    border = BorderStroke(1.dp, NaomiBorder),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No audit events match", color = NaomiTextPrimary, fontWeight = FontWeight.Black, fontSize = 11.sp)
                        Text("Change the search term.", color = NaomiTextSecondary, fontSize = 8.sp)
                    }
                }
            } else {
                visible.forEach { event ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = NaomiSurface),
                        border = BorderStroke(1.dp, NaomiBorder),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(9.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(event.action, color = NaomiTextPrimary, fontWeight = FontWeight.Black, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${event.actorName} · ${formatAuditTime(event.createdAt)} · ${event.severity}", color = NaomiOrange, fontSize = 7.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (event.target.isNotBlank()) Text("Target: ${event.target}", color = NaomiTextSecondary, fontSize = 7.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (event.details.isNotBlank()) Text(event.details, color = NaomiTextSecondary, fontSize = 7.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                repeat((AUDIT_EVENTS_PER_PAGE - visible.size).coerceAtLeast(0)) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }

        PosPager(
            page = page,
            totalPages = pages,
            onPrevious = { page = (page - 1).coerceAtLeast(0) },
            onNext = { page = (page + 1).coerceAtMost(pages - 1) },
            label = "AUDIT"
        )
    }
}

private fun formatAuditTime(value: Long): String =
    SimpleDateFormat("dd MMM HH:mm:ss", Locale.UK).format(Date(value))