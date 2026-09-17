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
import androidx.compose.material3.Surface
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
import com.example.ui.theme.NaomiSurfaceVariant
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
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "More",
            color = NaomiTextPrimary,
            fontSize = 26.sp,
            fontWeight = FontWeight.Black
        )
        Text(
            text = "Business tools, device settings and staff controls.",
            color = NaomiTextSecondary,
            fontSize = 12.sp
        )

        MenuSection(title = "BUSINESS") {
            MenuRow(
                icon = Icons.Default.LocationOn,
                title = "Venues & Blackpool",
                subtitle = "Venue tools, local operations and live information.",
                onClick = onBlackpool
            )
            MenuRow(
                icon = Icons.Default.CreditCard,
                title = "Business card",
                subtitle = "Create and print Naomi-Chan contact cards.",
                onClick = onBusinessCard
            )
        }

        MenuSection(title = "DEVICE & STAFF") {
            MenuRow(
                icon = Icons.Default.Print,
                title = "Printers",
                subtitle = "SUNMI, Bluetooth and USB printer setup.",
                onClick = onPrinters
            )
            MenuRow(
                icon = Icons.Default.Badge,
                title = "Staff operations",
                subtitle = "Shifts, maintenance and privileged actions.",
                onClick = onOperations
            )
        }
    }
}

@Composable
private fun MenuSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            color = NaomiTextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = NaomiSurface),
            border = BorderStroke(1.dp, NaomiBorder),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column { content() }
        }
    }
}

@Composable
private fun MenuRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = NaomiSurfaceVariant,
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = NaomiOrange,
                modifier = Modifier.padding(10.dp)
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = NaomiTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(subtitle, color = NaomiTextSecondary, fontSize = 11.sp)
        }
        Text("›", color = NaomiTextSecondary, fontSize = 24.sp, fontWeight = FontWeight.Light)
    }
}
