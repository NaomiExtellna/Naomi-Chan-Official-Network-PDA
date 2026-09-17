package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
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
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("MORE", color = NaomiOrange, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                Text("Terminal tools", color = NaomiTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Black)
            }
            Surface(
                color = if (shiftOpen) NaomiSuccess.copy(alpha = 0.10f) else NaomiSurfaceVariant,
                border = BorderStroke(1.dp, if (shiftOpen) NaomiSuccess.copy(alpha = 0.35f) else NaomiBorder),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    if (shiftOpen) "SHIFT OPEN" else "NO SHIFT",
                    color = if (shiftOpen) NaomiSuccess else NaomiTextSecondary,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
                )
            }
        }

        AccountStrip(staffName, staffRole)

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth().height(58.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PosToolTile(Icons.Default.LocationOn, "Blackpool", "Venues & trams", onBlackpool, Modifier.weight(1f))
                PosToolTile(Icons.Default.CreditCard, "Business card", "Create & print", onBusinessCard, Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth().height(58.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PosToolTile(Icons.Default.Print, "Printers", "Output devices", onPrinters, Modifier.weight(1f))
                PosToolTile(Icons.Default.Badge, "Staff ops", "Shifts & admin", onOperations, Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth().height(58.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PosToolTile(Icons.Default.Contactless, "Idle display", "Preview attract", onPreviewAttractMode, Modifier.weight(1f))
                PosToolTile(Icons.Default.Lock, "Lock terminal", "Return to PIN", onLockTerminal, Modifier.weight(1f), NaomiOrange)
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = NaomiSurface),
            border = BorderStroke(1.dp, NaomiBorder),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clickable(onClick = onSignOut)
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Logout, contentDescription = null, tint = NaomiRed, modifier = Modifier.size(19.dp))
                    Text("Sign out", color = NaomiTextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Text("END SESSION", color = NaomiRed, fontSize = 8.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun AccountStrip(staffName: String, staffRole: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = NaomiSurface),
        border = BorderStroke(1.dp, NaomiBorder),
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(color = NaomiSurfaceVariant, shape = RoundedCornerShape(3.dp)) {
                Icon(Icons.Default.Person, contentDescription = null, tint = NaomiOrange, modifier = Modifier.padding(9.dp).size(18.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(staffName, color = NaomiTextPrimary, fontWeight = FontWeight.Black, fontSize = 13.sp)
                Text(staffRole, color = NaomiTextSecondary, fontSize = 9.sp)
            }
        }
    }
}

@Composable
private fun PosToolTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = NaomiOrange
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = NaomiSurface),
        border = BorderStroke(1.dp, NaomiBorder),
        shape = RoundedCornerShape(4.dp),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(color = NaomiSurfaceVariant, shape = RoundedCornerShape(3.dp)) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.padding(6.dp).size(17.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(title, color = NaomiTextPrimary, fontWeight = FontWeight.Black, fontSize = 10.sp, maxLines = 1)
                Text(subtitle, color = NaomiTextSecondary, fontSize = 7.sp, maxLines = 1)
            }
        }
    }
}