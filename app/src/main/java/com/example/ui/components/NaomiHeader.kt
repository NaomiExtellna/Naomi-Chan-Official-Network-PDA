package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import com.example.ui.theme.NaomiBorder
import com.example.ui.theme.NaomiError
import com.example.ui.theme.NaomiOrange
import com.example.ui.theme.NaomiSuccess
import com.example.ui.theme.NaomiSurface
import com.example.ui.theme.NaomiSurfaceVariant
import com.example.ui.theme.NaomiTextPrimary
import com.example.ui.theme.NaomiTextSecondary

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
    val printerHealthy = printerStatus.isConnected &&
        printerStatus.hasPaper &&
        !printerStatus.isCoverOpen &&
        !printerStatus.isOverheated

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(NaomiSurface)
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
                modifier = Modifier.size(34.dp),
                color = NaomiSurfaceVariant,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, NaomiBorder)
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
                    text = "Naomi-Chan™ POS",
                    color = NaomiTextPrimary,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "SUNMI V2",
                    color = NaomiTextSecondary,
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }

            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onSelectChannelClick)
                    .testTag("printer_channel_badge"),
                color = NaomiSurfaceVariant,
                border = BorderStroke(1.dp, NaomiBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
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
                    Icon(icon, contentDescription = null, tint = NaomiOrange, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(label, color = NaomiTextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompactStatus(
                label = printerStateLabel(printerStatus),
                healthy = printerHealthy,
                modifier = Modifier
                    .weight(1f)
                    .clickable(enabled = !printerStatus.hasPaper, onClick = onReloadPaper)
            )
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onToggleOffline)
                    .testTag("offline_sync_toggle"),
                color = NaomiSurfaceVariant,
                border = BorderStroke(1.dp, NaomiBorder),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isOfflineSimulated) Icons.Default.CloudOff else Icons.Default.CloudDone,
                        contentDescription = null,
                        tint = if (isOfflineSimulated || unsyncedCount > 0) NaomiOrange else NaomiSuccess,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = when {
                            isOfflineSimulated -> "Offline"
                            unsyncedCount > 0 -> "$unsyncedCount pending"
                            else -> "Synced"
                        },
                        color = NaomiTextPrimary,
                        fontSize = 10.sp,
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
private fun CompactStatus(
    label: String,
    healthy: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = NaomiSurfaceVariant,
        border = BorderStroke(1.dp, NaomiBorder),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(if (healthy) NaomiSuccess else NaomiError, CircleShape)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                color = NaomiTextPrimary,
                fontSize = 10.sp,
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
