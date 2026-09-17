package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
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
import com.example.ui.theme.NaomiRed
import com.example.ui.theme.NaomiSuccess
import com.example.ui.theme.NaomiSurface
import com.example.ui.theme.NaomiSurfaceVariant
import com.example.ui.theme.NaomiTextPrimary
import com.example.ui.theme.NaomiTextSecondary

@Composable
fun MoreHubScreen(
    staffName: String,
    staffRole: String,
    shiftOpen: Boolean,
    onBusinessCard: () -> Unit,
    onBlackpool: () -> Unit,
    onPrinters: () -> Unit,
    onOperations: () -> Unit,
    onPreviewAttractMode: () -> Unit,
    onLockTerminal: () -> Unit,
    onSignOut: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NaomiDarkBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "More",
            color = NaomiTextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Black
        )
        Text(
            text = "Business tools, device settings and staff controls.",
            color = NaomiTextSecondary,
            fontSize = 12.sp
        )

        AccountCard(
            staffName = staffName,
            staffRole = staffRole,
            shiftOpen = shiftOpen
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

        MenuSection(title = "DISPLAY") {
            MenuRow(
                icon = Icons.Default.Contactless,
                title = "Preview idle display",
                subtitle = "Preview the contactless-style screen and promotional ad rotation.",
                onClick = onPreviewAttractMode
            )
        }

        MenuSection(title = "SESSION") {
            MenuRow(
                icon = Icons.Default.Lock,
                title = "Lock terminal",
                subtitle = "Return to the PIN screen and keep the current sale in memory.",
                iconTint = NaomiOrange,
                onClick = onLockTerminal
            )
            MenuRow(
                icon = Icons.Default.Logout,
                title = "Sign out",
                subtitle = "End this signed-in terminal session.",
                iconTint = NaomiRed,
                onClick = onSignOut
            )
        }
    }
}

@Composable
private fun AccountCard(staffName: String, staffRole: String, shiftOpen: Boolean) {
    Card(
        colors = CardDefaults.cardColors(containerColor = NaomiSurface),
        border = BorderStroke(1.dp, NaomiBorder),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = NaomiSurfaceVariant,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, NaomiBorder)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = NaomiOrange,
                    modifier = Modifier.padding(12.dp)
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(staffName, color = NaomiTextPrimary, fontWeight = FontWeight.Black, fontSize = 16.sp)
                Text(staffRole, color = NaomiTextSecondary, fontSize = 11.sp)
            }
            Surface(
                color = if (shiftOpen) NaomiSuccess.copy(alpha = 0.10f) else NaomiSurfaceVariant,
                border = BorderStroke(1.dp, if (shiftOpen) NaomiSuccess.copy(alpha = 0.35f) else NaomiBorder),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = if (shiftOpen) "SHIFT OPEN" else "NO SHIFT",
                    color = if (shiftOpen) NaomiSuccess else NaomiTextSecondary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
                )
            }
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
    iconTint: androidx.compose.ui.graphics.Color = NaomiOrange,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = NaomiSurfaceVariant,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, NaomiBorder)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
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
