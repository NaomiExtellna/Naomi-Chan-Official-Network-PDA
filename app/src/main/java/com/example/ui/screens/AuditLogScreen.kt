package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AuthViewModel
import com.example.ui.theme.NaomiBorder
import com.example.ui.theme.NaomiOrange
import com.example.ui.theme.NaomiSurface
import com.example.ui.theme.NaomiTextPrimary
import com.example.ui.theme.NaomiTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AuditLogScreen(authViewModel: AuthViewModel) {
    val state by authViewModel.state.collectAsState()
    val events by authViewModel.auditEvents.collectAsState()
    val user = state.currentUser ?: return
    var query by remember { mutableStateOf("") }

    if (!user.isAdmin) {
        Column(modifier = Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Audit Log", color = NaomiTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Text("Only Naomi (Admin) can view the full operational audit trail.", color = NaomiTextSecondary)
        }
        return
    }

    val filtered = remember(events, query) {
        events.filter { event ->
            query.isBlank() || listOf(event.actorName, event.action, event.target, event.details, event.severity)
                .any { it.contains(query.trim(), ignoreCase = true) }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text("AUDIT TRAIL", color = NaomiOrange, fontSize = 10.sp, fontWeight = FontWeight.Black)
        Text("Staff & System Activity", color = NaomiTextPrimary, fontSize = 19.sp, fontWeight = FontWeight.Black)
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search action, staff, target…") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Text("${filtered.size} recent event(s). Ticket wipes do not remove this log.", color = NaomiTextSecondary, fontSize = 9.sp)

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            items(filtered, key = { it.id }) { event ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = NaomiSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NaomiBorder),
                    shape = RoundedCornerShape(9.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(event.action, color = NaomiTextPrimary, fontWeight = FontWeight.Black, fontSize = 11.sp)
                        Text("${event.actorName} • ${formatAuditTime(event.createdAt)} • ${event.severity}", color = NaomiOrange, fontSize = 8.5.sp)
                        if (event.target.isNotBlank()) Text("Target: ${event.target}", color = NaomiTextSecondary, fontSize = 9.sp)
                        if (event.details.isNotBlank()) Text(event.details, color = NaomiTextSecondary, fontSize = 9.sp)
                    }
                }
            }
        }
    }
}

private fun formatAuditTime(value: Long): String = SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.UK).format(Date(value))
