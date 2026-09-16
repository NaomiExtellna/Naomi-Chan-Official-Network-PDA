package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import java.util.Locale
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.BlackpoolBar
import com.example.model.BlackpoolVenues
import com.example.model.GigType
import com.example.model.PackageTier
import com.example.model.PaymentMethod
import com.example.model.ReceiptData
import com.example.model.ReceiptIconType
import com.example.model.ReceiptItem
import com.example.ui.PosViewModel
import com.example.ui.theme.NaomiBorder
import com.example.ui.theme.NaomiDarkBg
import com.example.ui.theme.NaomiDeepRed
import com.example.ui.theme.NaomiOrange
import com.example.ui.theme.NaomiRed
import com.example.ui.theme.NaomiSuccess
import com.example.ui.theme.NaomiSurface
import com.example.ui.theme.NaomiSurfaceVariant
import com.example.ui.theme.NaomiTextPrimary
import com.example.ui.theme.NaomiTextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReceiptBuilderScreen(
    viewModel: PosViewModel,
    receipt: ReceiptData,
    onNavigateToPreview: () -> Unit
) {
    val ramInfo by viewModel.ramInfo.collectAsState()
    val customIconBitmap by viewModel.customIconBitmap.collectAsState()
    val blackpoolSearchQuery by viewModel.blackpoolSearchQuery.collectAsState()
    val selectedBlackpoolArea by viewModel.selectedBlackpoolArea.collectAsState()

    val isWirelessOnline by viewModel.isWirelessOnline.collectAsState()
    val pendingWirelessOrders by viewModel.pendingWirelessOrders.collectAsState()
    val wirelessServerUrl by viewModel.wirelessServerUrl.collectAsState()
    val isWirelessSyncing by viewModel.isWirelessSyncing.collectAsState()
    var showWirelessConfigDialog by remember { mutableStateOf(false) }
    var tempServerUrl by remember(wirelessServerUrl) { mutableStateOf(wirelessServerUrl) }

    var newItemName by remember { mutableStateOf("") }
    var newItemPrice by remember { mutableStateOf("") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.setCustomIcon(uri)
        }
    }

    val quickAddOns = if (receipt.isFreeEvent) {
        listOf(
            Pair("Complimentary Wristband", 0.0),
            Pair("VIP Lounge Free Pass", 0.0),
            Pair("Guest DJ Queue Access", 0.0),
            Pair("Community Meet & Greet", 0.0),
            Pair("Free Event Poster", 0.0)
        )
    } else {
        listOf(
            Pair("Track Shoutout", 2.0),
            Pair("Guest Song Request", 3.0),
            Pair("Bar Shot / Drink Voucher", 3.50),
            Pair("VIP Queue Jump", 5.0),
            Pair("Naomi-Chan DJ Lanyard", 6.0),
            Pair("VIP Fast-Track Wristband", 8.0),
            Pair("VIP Booth Entry Token", 10.0),
            Pair("DJ Stage Pass & Meet", 12.0),
            Pair("All-Night VIP Pass", 15.0)
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(NaomiDarkBg)
            .padding(horizontal = 16.dp)
            .testTag("receipt_builder_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Banner / Receipt ID Bar
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = NaomiSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (receipt.isFreeEvent) "FREE COMMUNITY EVENT PASS" else "ACTIVE BOOKING RECEIPT",
                            color = if (receipt.isFreeEvent) NaomiSuccess else NaomiOrange,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = receipt.id,
                            color = NaomiTextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    OutlinedButton(
                        onClick = { viewModel.resetNewReceipt() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NaomiTextSecondary),
                        modifier = Modifier.testTag("reset_receipt_button")
                    ) {
                        Text("New Gig", fontSize = 12.sp)
                    }
                }
            }
        }

        // Section: Wireless Flask Web App Gateway
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = NaomiSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isWirelessOnline) NaomiSuccess else NaomiBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("wireless_flask_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isWirelessOnline) Icons.Default.Wifi else Icons.Default.WifiOff,
                                contentDescription = null,
                                tint = if (isWirelessOnline) NaomiSuccess else NaomiOrange,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "WIRELESS FLASK WEB GATEWAY",
                                    color = if (isWirelessOnline) NaomiSuccess else NaomiOrange,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = if (isWirelessOnline) "Web POS Connected (Port 5000)" else "Offline / Tap Settings to Connect",
                                    color = NaomiTextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { viewModel.checkWirelessConnection() },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Sync",
                                    tint = NaomiTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            OutlinedButton(
                                onClick = { showWirelessConfigDialog = true },
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("Configure IP", fontSize = 11.sp, color = NaomiTextPrimary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Bar staff & guests can order wirelessly at $wirelessServerUrl. All prices calibrated between £2.00 and £15.00.",
                        fontSize = 11.sp,
                        color = NaomiTextSecondary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.broadcastCurrentReceiptToWeb() },
                            colors = ButtonDefaults.buttonColors(containerColor = NaomiSurfaceVariant),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .testTag("broadcast_web_button")
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp), tint = NaomiOrange)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Send to Web", fontSize = 11.sp, color = NaomiTextPrimary)
                        }

                        Button(
                            onClick = { viewModel.fetchPendingWirelessOrders() },
                            colors = ButtonDefaults.buttonColors(containerColor = NaomiDeepRed),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .testTag("check_orders_button")
                        ) {
                            Text(
                                text = if (pendingWirelessOrders.isEmpty()) "Check Orders (0)" else "Orders (${pendingWirelessOrders.size})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Pending Orders List (if any)
                    if (pendingWirelessOrders.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "PENDING WIRELESS ORDERS (${pendingWirelessOrders.size})",
                            color = NaomiOrange,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            pendingWirelessOrders.forEach { order ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = NaomiDarkBg),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(order.id, color = NaomiOrange, fontSize = 12.sp, fontWeight = FontWeight.Black)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("• ${order.clientName}", color = NaomiTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Text(
                                                text = "${order.venueName} • £${String.format(Locale.UK, "%.2f", order.grandTotal)}",
                                                color = NaomiTextSecondary,
                                                fontSize = 11.sp
                                            )
                                            Text(
                                                text = order.items.joinToString(", ") { "${it.name} x${it.quantity} (£${String.format(Locale.UK, "%.2f", it.unitPrice)})" },
                                                color = NaomiTextSecondary,
                                                fontSize = 10.sp,
                                                maxLines = 1
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Button(
                                            onClick = { viewModel.loadAndPrintWirelessOrder(order) },
                                            colors = ButtonDefaults.buttonColors(containerColor = NaomiRed),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier
                                                .height(34.dp)
                                                .testTag("print_wireless_order_${order.id}")
                                        ) {
                                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Print", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section A: Free Event Mode & Community Presets
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (receipt.isFreeEvent) Color(0xFF162D1D) else NaomiSurface
                ),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (receipt.isFreeEvent) NaomiSuccess else NaomiBorder
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Celebration,
                                contentDescription = null,
                                tint = if (receipt.isFreeEvent) NaomiSuccess else NaomiOrange,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Free Event Mode",
                                    color = NaomiTextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (receipt.isFreeEvent) "Admission: FREE ($0.00) • Community Pass" else "Toggle for $0.00 admission gigs & passes",
                                    color = if (receipt.isFreeEvent) NaomiSuccess else NaomiTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Switch(
                            checked = receipt.isFreeEvent,
                            onCheckedChange = { viewModel.toggleFreeEvent(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = NaomiSuccess,
                                uncheckedThumbColor = NaomiTextSecondary,
                                uncheckedTrackColor = NaomiSurfaceVariant
                            ),
                            modifier = Modifier.testTag("free_event_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.setFreeCommunityPreset() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NaomiSuccess),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NaomiSuccess),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("free_community_preset_btn")
                        ) {
                            Text("Load Free Community Preset", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Section B: Receipt Logo & Branding (Custom Logo / 58mm Thermal)
        item {
            SectionHeader(title = "RECEIPT LOGO & BRANDING")

            Card(
                colors = CardDefaults.cardColors(containerColor = NaomiSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Add your own custom logo or choose a DJ icon to print on every receipt:",
                        color = NaomiTextSecondary,
                        fontSize = 12.sp
                    )

                    // Prominent Custom Logo Upload Card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = if (customIconBitmap != null) NaomiDeepRed.copy(alpha = 0.5f) else NaomiSurfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            if (customIconBitmap != null) NaomiOrange else NaomiBorder
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (customIconBitmap != null) {
                                Image(
                                    bitmap = customIconBitmap!!.asImageBitmap(),
                                    contentDescription = "My Custom Logo",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color.White)
                                        .padding(2.dp)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .background(NaomiDarkBg, RoundedCornerShape(6.dp))
                                        .border(1.dp, NaomiBorder, RoundedCornerShape(6.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudUpload,
                                        contentDescription = null,
                                        tint = NaomiOrange,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (customIconBitmap != null) "My Custom Logo" else "Upload My Own Logo",
                                        color = NaomiTextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    if (customIconBitmap != null) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = NaomiSuccess,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = if (customIconBitmap != null) "Saved to device & active for all 58mm receipts" else "Pick photo from gallery for top of receipts",
                                    color = NaomiTextSecondary,
                                    fontSize = 11.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Button(
                                    onClick = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NaomiOrange),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("upload_custom_icon_btn")
                                ) {
                                    Icon(
                                        imageVector = if (customIconBitmap != null) Icons.Default.AddPhotoAlternate else Icons.Default.CloudUpload,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (customIconBitmap != null) "Change" else "Upload",
                                        color = Color.Black,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                if (customIconBitmap != null) {
                                    OutlinedButton(
                                        onClick = { viewModel.removeCustomIcon() },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NaomiRed),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, NaomiRed),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.testTag("remove_custom_icon_btn")
                                    ) {
                                        Text("Remove", fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }

                    // Predefined Icon Choices
                    Text(
                        text = "Or Select Built-In DJ Icon Style:",
                        color = NaomiTextSecondary,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (type in ReceiptIconType.values()) {
                            val isSelected = receipt.iconType == type
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.selectIconType(type) }
                                    .testTag("icon_type_${type.name}"),
                                color = if (isSelected) NaomiDeepRed else NaomiSurfaceVariant,
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, NaomiOrange) else androidx.compose.foundation.BorderStroke(1.dp, NaomiBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (type.drawableResId != null) {
                                        Image(
                                            painter = painterResource(id = type.drawableResId),
                                            contentDescription = type.label,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                    } else if (type == ReceiptIconType.CUSTOM) {
                                        Icon(
                                            imageVector = Icons.Default.Image,
                                            contentDescription = null,
                                            tint = if (isSelected) NaomiOrange else NaomiTextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Text(
                                        text = type.label,
                                        color = if (isSelected) Color.White else NaomiTextPrimary,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section C: Device RAM Monitor & Optimization (<1GB RAM Devices)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = NaomiSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, NaomiBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = null,
                            tint = NaomiSuccess,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Low-RAM Engine (<1GB): ",
                                    color = NaomiTextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "ACTIVE",
                                    color = NaomiSuccess,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Text(
                                text = "Free: ${ramInfo.freeMb} MB / Total: ${ramInfo.totalMb} MB • Streamed ESC/POS",
                                color = NaomiTextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { viewModel.clearMemoryBuffers() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NaomiOrange),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NaomiBorder),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("purge_ram_btn")
                    ) {
                        Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Purge RAM", fontSize = 10.5.sp)
                    }
                }
            }
        }

        // Section 1: Blackpool Venues Directory & Gig Information
        item {
            SectionHeader(title = "1. BLACKPOOL VENUES & EVENT DETAILS")

            Card(
                colors = CardDefaults.cardColors(containerColor = NaomiSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Blackpool Directory Header & Quick Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = NaomiRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "BLACKPOOL BARS DIRECTORY",
                                color = NaomiTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        Surface(
                            color = NaomiRed.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "${BlackpoolVenues.ALL_BARS.size} Local Bars",
                                color = NaomiOrange,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = "1-Tap Blackpool Venue Selection (auto-fills venue address, contact & client):",
                        color = NaomiTextSecondary,
                        fontSize = 11.5.sp
                    )

                    // Search input for Blackpool venues
                    OutlinedTextField(
                        value = blackpoolSearchQuery,
                        onValueChange = { viewModel.setBlackpoolSearch(it) },
                        placeholder = { Text("Search bars (e.g. Flying Handbag, Kaos, Walkabout, Waterloo...)") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NaomiOrange) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("blackpool_bar_search"),
                        colors = outlinedFieldColors(),
                        singleLine = true
                    )

                    // Area Filter Chips
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (area in BlackpoolVenues.AREAS) {
                            val isAreaSelected = selectedBlackpoolArea == area
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { viewModel.setBlackpoolArea(area) }
                                    .testTag("area_chip_${area.replace(" ", "_")}"),
                                color = if (isAreaSelected) NaomiOrange else NaomiSurfaceVariant,
                                border = if (isAreaSelected) null else androidx.compose.foundation.BorderStroke(1.dp, NaomiBorder)
                            ) {
                                Text(
                                    text = area,
                                    color = if (isAreaSelected) Color.Black else NaomiTextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isAreaSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    // Filtered Blackpool Bars List
                    val filteredBars = remember(blackpoolSearchQuery, selectedBlackpoolArea) {
                        BlackpoolVenues.ALL_BARS.filter { bar ->
                            val matchesArea = selectedBlackpoolArea == "All Blackpool" || bar.area == selectedBlackpoolArea
                            val matchesQuery = blackpoolSearchQuery.isBlank() ||
                                bar.name.contains(blackpoolSearchQuery, ignoreCase = true) ||
                                bar.address.contains(blackpoolSearchQuery, ignoreCase = true) ||
                                bar.barType.contains(blackpoolSearchQuery, ignoreCase = true)
                            matchesArea && matchesQuery
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (bar in filteredBars) {
                            val isBarActive = receipt.venueName.contains(bar.name, ignoreCase = true) ||
                                receipt.clientName.equals(bar.name, ignoreCase = true)
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.selectBlackpoolBar(bar) }
                                    .testTag("bar_item_${bar.name.replace(" ", "_")}"),
                                color = if (isBarActive) NaomiDeepRed.copy(alpha = 0.6f) else NaomiSurfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(
                                    if (isBarActive) 1.5.dp else 1.dp,
                                    if (isBarActive) NaomiOrange else NaomiBorder
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = bar.name,
                                                color = if (isBarActive) NaomiOrange else NaomiTextPrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.5.sp
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                color = NaomiSurface,
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = bar.barType,
                                                    color = NaomiTextSecondary,
                                                    fontSize = 9.5.sp,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${bar.address} • Tel: ${bar.contact}",
                                            color = NaomiTextSecondary,
                                            fontSize = 10.5.sp
                                        )
                                    }
                                    if (isBarActive) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = NaomiSuccess,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    } else {
                                        Text(
                                            text = "Select",
                                            color = NaomiOrange,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Customize / Fine-Tune Booking Details:",
                        color = NaomiTextSecondary,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    )

                    OutlinedTextField(
                        value = receipt.clientName,
                        onValueChange = { viewModel.updateClientName(it) },
                        label = { Text("Client or Event Organizer Name") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = NaomiOrange) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("client_name_input"),
                        colors = outlinedFieldColors()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = receipt.clientContact,
                            onValueChange = { viewModel.updateClientContact(it) },
                            label = { Text("Contact Phone / Email") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("client_contact_input"),
                            colors = outlinedFieldColors()
                        )
                        OutlinedTextField(
                            value = receipt.gigDate,
                            onValueChange = { viewModel.updateGigDate(it) },
                            label = { Text("Date & Time") },
                            leadingIcon = { Icon(Icons.Default.Event, contentDescription = null, tint = NaomiOrange) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("gig_date_input"),
                            colors = outlinedFieldColors()
                        )
                    }

                    OutlinedTextField(
                        value = receipt.venueName,
                        onValueChange = { viewModel.updateVenue(it) },
                        label = { Text("Venue / Club / Stage Location") },
                        leadingIcon = { Icon(Icons.Default.Place, contentDescription = null, tint = NaomiOrange) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("venue_input"),
                        colors = outlinedFieldColors()
                    )

                    // Event Type Chips
                    Text(
                        text = "Event Type / Performance Setting",
                        color = NaomiTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (gig in GigType.values()) {
                            val isSelected = receipt.gigType == gig
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.updateGigType(gig) }
                                    .testTag("gig_type_${gig.name}"),
                                color = if (isSelected) NaomiRed else NaomiSurfaceVariant,
                                border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, NaomiBorder)
                            ) {
                                Text(
                                    text = gig.label,
                                    color = if (isSelected) Color.White else NaomiTextPrimary,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Package Tier Selection
        item {
            SectionHeader(title = "2. DJ PACKAGE TIER")

            Card(
                colors = CardDefaults.cardColors(containerColor = NaomiSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    for (tier in PackageTier.values()) {
                        val isSelected = receipt.packageTier == tier
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { viewModel.updatePackageTier(tier) }
                                .testTag("package_tier_${tier.name}"),
                            color = if (isSelected) NaomiRed.copy(alpha = 0.15f) else NaomiSurfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(
                                if (isSelected) 2.dp else 1.dp,
                                if (isSelected) NaomiRed else NaomiBorder
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .border(2.dp, if (isSelected) NaomiRed else NaomiTextSecondary, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .background(NaomiRed, CircleShape)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = tier.title,
                                        color = NaomiTextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = tier.description,
                                        color = NaomiTextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (tier.basePrice == 0.0) "FREE" else ReceiptData.formatCurrency(tier.basePrice),
                                    color = if (tier.basePrice == 0.0) NaomiSuccess else NaomiOrange,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 3: Line Items & Quick Add-Ons
        item {
            SectionHeader(title = "3. ITEMIZATION & MERCHANDISE")

            Card(
                colors = CardDefaults.cardColors(containerColor = NaomiSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = if (receipt.isFreeEvent) "Quick Add Complimentary Items:" else "Quick Add Extras:",
                        color = NaomiTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (addon in quickAddOns) {
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable {
                                        viewModel.addLineItem(
                                            ReceiptItem(
                                                name = addon.first,
                                                quantity = 1,
                                                unitPrice = addon.second
                                            )
                                        )
                                    }
                                    .testTag("quick_add_${addon.first.replace(" ", "_")}"),
                                color = NaomiSurfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(1.dp, NaomiBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = NaomiOrange, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = addon.first, color = NaomiTextPrimary, fontSize = 11.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (addon.second == 0.0) "FREE" else "+£${addon.second.toInt()}",
                                        color = if (addon.second == 0.0) NaomiSuccess else NaomiOrange,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Line Items Table
                    receipt.items.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.name,
                                    color = NaomiTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (item.unitPrice == 0.0) "Complimentary Admission" else "Qty: ${item.quantity} × ${ReceiptData.formatCurrency(item.unitPrice)}",
                                    color = if (item.unitPrice == 0.0) NaomiSuccess else NaomiTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                            Text(
                                text = if (item.unitPrice == 0.0) "FREE" else ReceiptData.formatCurrency(item.total),
                                color = if (item.unitPrice == 0.0) NaomiSuccess else NaomiTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            IconButton(
                                onClick = { viewModel.removeLineItem(item.id) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Remove",
                                    tint = NaomiTextSecondary.copy(alpha = 0.7f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Custom item addition form
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newItemName,
                            onValueChange = { newItemName = it },
                            placeholder = { Text("Custom item / pass...") },
                            modifier = Modifier.weight(1.8f),
                            colors = outlinedFieldColors()
                        )
                        OutlinedTextField(
                            value = newItemPrice,
                            onValueChange = { newItemPrice = it },
                            placeholder = { Text("£") },
                            modifier = Modifier.weight(1f),
                            colors = outlinedFieldColors()
                        )
                        Button(
                            onClick = {
                                val price = newItemPrice.toDoubleOrNull() ?: 0.0
                                if (newItemName.isNotBlank()) {
                                    viewModel.addLineItem(
                                        ReceiptItem(
                                            name = newItemName.trim(),
                                            quantity = 1,
                                            unitPrice = price
                                        )
                                    )
                                    newItemName = ""
                                    newItemPrice = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NaomiRed),
                            modifier = Modifier.testTag("add_custom_item_btn")
                        ) {
                            Text("Add", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Section 4: Payment Method & Footer Notes
        item {
            SectionHeader(title = "4. PAYMENT & FOOTER NOTES")

            Card(
                colors = CardDefaults.cardColors(containerColor = NaomiSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Payment Method Received:",
                        color = NaomiTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (method in PaymentMethod.values()) {
                            val isSelected = receipt.paymentMethod == method
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.updatePaymentMethod(method) }
                                    .testTag("payment_method_${method.name}"),
                                color = if (isSelected) NaomiDeepRed else NaomiSurfaceVariant,
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, NaomiOrange) else androidx.compose.foundation.BorderStroke(1.dp, NaomiBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val icon = when (method) {
                                        PaymentMethod.FREE_PASS -> Icons.Default.Celebration
                                        PaymentMethod.CARD_TERMINAL -> Icons.Default.CreditCard
                                        PaymentMethod.CASH -> Icons.Default.Money
                                        PaymentMethod.BANK_TRANSFER -> Icons.Default.LocalOffer
                                        PaymentMethod.QR_PAY -> Icons.Default.QrCode
                                    }
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isSelected) NaomiOrange else NaomiTextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = method.label,
                                        color = if (isSelected) Color.White else NaomiTextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = receipt.footerNotes,
                        onValueChange = { viewModel.updateFooterNotes(it) },
                        label = { Text("Custom DJ Receipt Message / VIP Note") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("footer_notes_input"),
                        colors = outlinedFieldColors(),
                        minLines = 2
                    )
                }
            }
        }

        // Section 5: Order Summary & Primary Action Buttons
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = NaomiSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (receipt.isEffectivelyFree) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Admission Status", color = NaomiTextSecondary, fontSize = 13.sp)
                            Text("FREE ADMISSION (${ReceiptData.formatCurrency(0.0)})", color = NaomiSuccess, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Subtotal", color = NaomiTextSecondary, fontSize = 13.sp)
                            Text(ReceiptData.formatCurrency(receipt.subtotal), color = NaomiTextPrimary, fontSize = 13.sp)
                        }

                        // UK VAT Rate Selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("UK VAT Rate:", color = NaomiTextSecondary, fontSize = 12.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                val isVat20 = receipt.taxPercent == 20.0
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { viewModel.updateTaxPercent(20.0) },
                                    color = if (isVat20) NaomiOrange else NaomiSurfaceVariant,
                                    border = if (isVat20) null else androidx.compose.foundation.BorderStroke(1.dp, NaomiBorder)
                                ) {
                                    Text(
                                        "20% Standard",
                                        color = if (isVat20) Color.Black else NaomiTextPrimary,
                                        fontSize = 10.5.sp,
                                        fontWeight = if (isVat20) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                                val isVat0 = receipt.taxPercent == 0.0
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { viewModel.updateTaxPercent(0.0) },
                                    color = if (isVat0) NaomiOrange else NaomiSurfaceVariant,
                                    border = if (isVat0) null else androidx.compose.foundation.BorderStroke(1.dp, NaomiBorder)
                                ) {
                                    Text(
                                        "0% Exempt",
                                        color = if (isVat0) Color.Black else NaomiTextPrimary,
                                        fontSize = 10.5.sp,
                                        fontWeight = if (isVat0) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("VAT (${receipt.taxPercent.toInt()}%)", color = NaomiTextSecondary, fontSize = 13.sp)
                            Text(ReceiptData.formatCurrency(receipt.taxAmount), color = NaomiTextPrimary, fontSize = 13.sp)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .height(1.dp)
                            .background(NaomiBorder)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "TOTAL DUE",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp
                        )
                        Text(
                            if (receipt.isEffectivelyFree) "FREE (${ReceiptData.formatCurrency(0.0)})" else ReceiptData.formatCurrency(receipt.grandTotal),
                            color = if (receipt.isEffectivelyFree) NaomiSuccess else NaomiOrange,
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Primary Action Button: PRINT RECEIPT (Naomi Red)
                    Button(
                        onClick = { viewModel.printCurrentReceipt() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("print_receipt_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = NaomiRed),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (receipt.isEffectivelyFree) "PRINT FREE ADMISSION PASS" else "PRINT 58MM THERMAL RECEIPT",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            letterSpacing = 0.5.sp
                        )
                    }

                    // Secondary Action: Live 58mm Thermal Preview
                    OutlinedButton(
                        onClick = { onNavigateToPreview() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("preview_receipt_button"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NaomiOrange),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, NaomiOrange),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "View 58mm Live Paper Preview",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    if (showWirelessConfigDialog) {
        AlertDialog(
            onDismissRequest = { showWirelessConfigDialog = false },
            title = { Text("Wireless Flask Server Configuration", color = NaomiTextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Enter the Flask web app URL. Use http://10.0.2.2:5000 for local emulator or your Wi-Fi LAN IP (e.g. http://192.168.1.50:5000) for real wireless POS devices in Blackpool venues.",
                        fontSize = 12.sp,
                        color = NaomiTextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = tempServerUrl,
                        onValueChange = { tempServerUrl = it },
                        label = { Text("Gateway URL") },
                        colors = outlinedFieldColors(),
                        modifier = Modifier.fillMaxWidth().testTag("wireless_url_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { tempServerUrl = "http://10.0.2.2:5000" },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("10.0.2.2:5000", fontSize = 10.sp, color = NaomiTextPrimary)
                        }
                        OutlinedButton(
                            onClick = { tempServerUrl = "http://127.0.0.1:5000" },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("127.0.0.1:5000", fontSize = 10.sp, color = NaomiTextPrimary)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setWirelessServerUrl(tempServerUrl)
                        showWirelessConfigDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NaomiRed)
                ) {
                    Text("Save & Connect", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWirelessConfigDialog = false }) {
                    Text("Cancel", color = NaomiTextSecondary)
                }
            },
            containerColor = NaomiSurface
        )
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        color = NaomiOrange,
        fontWeight = FontWeight.Bold,
        fontSize = 11.5.sp,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
    )
}

@Composable
fun outlinedFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = NaomiOrange,
    unfocusedBorderColor = NaomiBorder,
    focusedLabelColor = NaomiOrange,
    unfocusedLabelColor = NaomiTextSecondary,
    focusedTextColor = NaomiTextPrimary,
    unfocusedTextColor = NaomiTextPrimary,
    cursorColor = NaomiOrange
)
