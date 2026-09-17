package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NaomiBorder
import com.example.ui.theme.NaomiDarkBg
import com.example.ui.theme.NaomiOrange
import com.example.ui.theme.NaomiSurface
import com.example.ui.theme.NaomiTextPrimary
import com.example.ui.theme.NaomiTextSecondary

@Composable
fun MoreHubScreen(
    onBusinessCard: () -> Unit,
    onBlackpool: () -> Unit,
    onPrinters: () -> Unit,
    onOperations: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NaomiDarkBg)
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "TOOLS & OPERATIONS",
            color = NaomiOrange,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.2.sp
        )
        Text(
            text = "More",
            color = NaomiTextPrimary,
            fontSize = 23.sp,
            fontWeight = FontWeight.Black
        )
        Text(
            text = "Secondary terminal tools kept out of the main transaction flow.",
            color = NaomiTextSecondary,
            fontSize = 11.sp
        )

        ToolCard(
            icon = Icons.Default.CreditCard,
            title = "Business card",
            subtitle = "Create and print Naomi-Chan contact cards.",
            onClick = onBusinessCard
        )
        ToolCard(
            icon = Icons.Default.LocationOn,
            title = "Blackpool hub",
            subtitle = "Venue tools, local operations and live information.",
            onClick = onBlackpool
        )
        ToolCard(
            icon = Icons.Default.Print,
            title = "Printer manager",
            subtitle = "SUNMI, Bluetooth and USB printer diagnostics.",
            onClick = onPrinters
        )
        ToolCard(
            icon = Icons.Default.Badge,
            title = "Staff operations",
            subtitle = "Shifts, maintenance and privileged terminal actions.",
            onClick = onOperations
        )
    }
}

@Composable
private fun ToolCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = NaomiSurface),
        border = BorderStroke(1.dp, NaomiBorder),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = NaomiOrange.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = NaomiOrange,
                    modifier = Modifier.padding(11.dp)
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, color = NaomiTextPrimary, fontWeight = FontWeight.Black, fontSize = 14.sp)
                Text(subtitle, color = NaomiTextSecondary, fontSize = 10.sp)
            }
            Text("›", color = NaomiTextSecondary, fontSize = 22.sp, fontWeight = FontWeight.Light)
        }
    }
}
