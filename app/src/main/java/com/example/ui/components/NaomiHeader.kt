package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.HorizontalDivider
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
import com.example.ui.theme.NaomiRed
import com.example.ui.theme.NaomiSuccess
import com.example.ui.theme.NaomiSurface
import com.example.ui.theme.NaomiSurfaceVariant
import com.example.ui.theme.NaomiTextPrimary
import com.example.ui.theme.NaomiTextSecondary

@Composable
fun NaomiHeader(
    selectedChannel: PrinterChannel,
    printerStatus: PrinterStatus,
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
            .testTag("naomi_pos_header")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(30.dp),
                color = NaomiSurfaceVariant,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, NaomiBorder)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_naomi_logo),
                    contentDescription = "Naomi-Chan logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(30.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Naomi-Chan™ POS",
                    color = NaomiTextPrimary,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "STANDALONE · LOCAL RECORDS · SUNMI V2",
                    color = NaomiTextSecondary,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }

            PrinterChannelBadge(
                selectedChannel = selectedChannel,
                onClick = onSelectChannelClick
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusChip(
                label = printerStateLabel(printerStatus),
                healthy = printerHealthy,
                modifier = Modifier
                    .weight(1f)
                    .clickable(enabled = !printerStatus.hasPaper, onClick = onReloadPaper)
            )

            Surface(
                modifier = Modifier.weight(1f),
                color = NaomiSuccess.copy(alpha = 0.07f),
                border = BorderStroke(1.dp, NaomiSuccess.copy(alpha = 0.22f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.SaveAlt,
                        contentDescription = null,
                        tint = NaomiSuccess,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Local · CSV ready",
                        color = NaomiTextPrimary,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }
        }

        HorizontalDivider(color = NaomiBorder, thickness = 1.dp)
    }
}

@Composable
private fun PrinterChannelBadge(
    selectedChannel: PrinterChannel,
    onClick: () -> Unit
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

    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .testTag("printer_channel_badge"),
        color = NaomiSurfaceVariant,
        border = BorderStroke(1.dp, NaomiBorder),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = NaomiRed, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(5.dp))
            Text(label, color = NaomiTextPrimary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StatusChip(
    label: String,
    healthy: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = if (healthy) NaomiSuccess.copy(alpha = 0.07f) else NaomiSurfaceVariant,
        border = BorderStroke(
            1.dp,
            if (healthy) NaomiSuccess.copy(alpha = 0.22f) else NaomiBorder
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(if (healthy) NaomiSuccess else NaomiError, CircleShape)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                color = NaomiTextPrimary,
                fontSize = 8.sp,
                fontWeight = FontWeight.SemiBold,
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
    status.statusCode == 2 -> "Preparing printer"
    status.isConnected -> "Printer ready"
    else -> "Printer offline"
}
