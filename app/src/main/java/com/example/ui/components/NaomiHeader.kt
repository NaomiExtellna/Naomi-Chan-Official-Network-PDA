package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.PrinterChannel
import com.example.model.PrinterStatus
import com.example.ui.theme.NaomiDeepRed
import com.example.ui.theme.NaomiError
import com.example.ui.theme.NaomiOrange
import com.example.ui.theme.NaomiRed
import com.example.ui.theme.NaomiSuccess

@Composable
fun NaomiHeader(
    selectedChannel: PrinterChannel,
    printerStatus: PrinterStatus,
    unsyncedCount: Int,
    isOfflineSimulated: Boolean,
    onToggleOffline: () -> Unit,
    onReloadPaper: () -> Unit,
    onSelectChannelClick: () -> Unit
) {
    val headerGradient = Brush.horizontalGradient(listOf(NaomiDeepRed, NaomiRed))
    val printerHealthy = printerStatus.isConnected && printerStatus.hasPaper && !printerStatus.isCoverOpen && !printerStatus.isOverheated

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(headerGradient)
            .statusBarsPadding()
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .testTag("naomi_pos_header"),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, NaomiOrange, CircleShape),
                color = Color.White
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_naomi_logo),
                    contentDescription = "Naomi-Chan logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(34.dp)
                )
            }

            Spacer(modifier = Modifier.width(9.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "NAOMI-CHAN POS",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    letterSpacing = 0.6.sp,
                    maxLines = 1
                )
                Text(
                    text = "Staff terminal • SUNMI V2",
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 9.sp,
                    maxLines = 1
                )
            }

            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(onClick = onSelectChannelClick)
                    .testTag("printer_channel_badge"),
                color = Color.Black.copy(alpha = 0.22f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val icon = when (selectedChannel) {
                        PrinterChannel.SUNMI_BUILTIN -> Icons.Default.Print
                        PrinterChannel.BLUETOOTH -> Icons.Default.Bluetooth
                        PrinterChannel.USB_OTG -> Icons.Default.Usb
                    }
                    val label = when (selectedChannel) {
                        PrinterChannel.SUNMI_BUILTIN -> "SUNMI"
                        PrinterChannel.BLUETOOTH -> "BT"
                        PrinterChannel.USB_OTG -> "USB"
                    }
                    Icon(icon, contentDescription = null, tint = NaomiOrange, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusChip(
                text = printerStateLabel(printerStatus),
                active = printerHealthy,
                modifier = Modifier
                    .weight(1f)
                    .clickable(enabled = !printerStatus.hasPaper, onClick = onReloadPaper)
            )

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onToggleOffline)
                    .testTag("offline_sync_toggle"),
                color = if (isOfflineSimulated || unsyncedCount > 0) NaomiOrange.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.10f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isOfflineSimulated || unsyncedCount > 0) NaomiOrange.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.12f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (isOfflineSimulated) Icons.Default.CloudOff else Icons.Default.CloudDone,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = when {
                            isOfflineSimulated -> "Offline • $unsyncedCount"
                            unsyncedCount > 0 -> "$unsyncedCount pending"
                            else -> "Synced"
                        },
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusChip(text: String, active: Boolean, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = if (active) NaomiSuccess.copy(alpha = 0.16f) else NaomiError.copy(alpha = 0.20f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (active) NaomiSuccess.copy(alpha = 0.48f) else NaomiError.copy(alpha = 0.55f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(if (active) NaomiSuccess else NaomiError, CircleShape)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = text,
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun printerStateLabel(status: PrinterStatus): String = when {
    !status.hasPaper -> "No paper"
    status.isCoverOpen -> "Cover open"
    status.isOverheated -> "Overheated"
    status.statusCode == 2 -> "Preparing"
    status.isConnected -> "Printer ready"
    else -> "Printer offline"
}
