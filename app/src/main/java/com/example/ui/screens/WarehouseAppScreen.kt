package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.ui.AuthViewModel
import com.example.ui.WarehouseManifestLine
import com.example.ui.WarehouseOrder
import com.example.ui.WarehouseStage
import com.example.ui.WarehouseStockItem
import com.example.ui.WarehouseUiState
import com.example.ui.WarehouseViewModel
import com.example.ui.theme.NaomiAccentSurface
import com.example.ui.theme.NaomiBorder
import com.example.ui.theme.NaomiDarkBg
import com.example.ui.theme.NaomiError
import com.example.ui.theme.NaomiHoldSurface
import com.example.ui.theme.NaomiOrange
import com.example.ui.theme.NaomiSuccess
import com.example.ui.theme.NaomiSurface
import com.example.ui.theme.NaomiSurfaceVariant
import com.example.ui.theme.NaomiTextPrimary
import com.example.ui.theme.NaomiTextSecondary
import com.example.ui.theme.NaomiTextTertiary
import com.example.ui.theme.NaomiTransBlue
import com.example.ui.theme.NaomiTransPink
import com.example.ui.theme.NaomiWarning

private enum class WarehouseTab {
    HOME,
    PICK,
    SCAN,
    STOCK,
    MORE
}

private data class WarehouseNavItem(
    val tab: WarehouseTab,
    val label: String,
    val icon: ImageVector
)

@Composable
fun WarehouseAppScreen(
    viewModel: WarehouseViewModel,
    authViewModel: AuthViewModel
) {
    val ui by viewModel.state.collectAsStateWithLifecycle()
    val authState by authViewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var activeTab by remember { mutableStateOf(WarehouseTab.HOME) }

    val navItems = remember {
        listOf(
            WarehouseNavItem(WarehouseTab.HOME, "Home", Icons.Default.Home),
            WarehouseNavItem(WarehouseTab.PICK, "Pick", Icons.Default.LocalShipping),
            WarehouseNavItem(WarehouseTab.SCAN, "Scan", Icons.Default.QrCodeScanner),
            WarehouseNavItem(WarehouseTab.STOCK, "Stock", Icons.Default.Inventory2),
            WarehouseNavItem(WarehouseTab.MORE, "More", Icons.Default.MoreHoriz)
        )
    }

    LaunchedEffect(ui.message) {
        val message = ui.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.dismissMessage()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            WarehouseTopBar(
                operatorName = ui.operatorName,
                shiftOpen = authState.activeShift != null
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = NaomiSurface,
                tonalElevation = 0.dp,
                modifier = Modifier
                    .height(62.dp)
                    .navigationBarsPadding()
            ) {
                navItems.forEach { item ->
                    val selected = activeTab == item.tab
                    NavigationBarItem(
                        selected = selected,
                        onClick = { activeTab = item.tab },
                        icon = {
                            Icon(
                                item.icon,
                                contentDescription = item.label,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = {
                            Text(
                                item.label,
                                fontSize = 9.sp,
                                fontWeight = if (selected) FontWeight.Black else FontWeight.Medium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NaomiTextPrimary,
                            selectedTextColor = NaomiTextPrimary,
                            indicatorColor = NaomiSurfaceVariant,
                            unselectedIconColor = NaomiTextTertiary,
                            unselectedTextColor = NaomiTextSecondary
                        )
                    )
                }
            }
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        },
        containerColor = NaomiDarkBg
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (activeTab) {
                WarehouseTab.HOME -> WarehouseHomeScreen(
                    ui = ui,
                    onPick = { activeTab = WarehouseTab.PICK },
                    onScan = { activeTab = WarehouseTab.SCAN },
                    onStock = { activeTab = WarehouseTab.STOCK }
                )
                WarehouseTab.PICK -> WarehousePickScreen(
                    ui = ui,
                    onTogglePicked = viewModel::togglePicked,
                    onPacked = viewModel::markPacked,
                    onDispatched = viewModel::markDispatched,
                    onResolveHold = viewModel::resolveHold
                )
                WarehouseTab.SCAN -> WarehouseScannerScreen(
                    lastScan = ui.lastScan,
                    onScan = viewModel::scan,
                    onClear = viewModel::clearLastScan
                )
                WarehouseTab.STOCK -> WarehouseStockScreen(
                    ui = ui,
                    onAdjust = viewModel::adjustStock
                )
                WarehouseTab.MORE -> WarehouseMoreScreen(
                    ui = ui,
                    isAdmin = authState.currentUser?.isAdmin == true,
                    onReceiveReturn = viewModel::receiveReturn,
                    onResolveHold = viewModel::resolveHold,
                    onLock = authViewModel::logout
                )
            }
        }
    }
}

@Composable
private fun WarehouseTopBar(
    operatorName: String,
    shiftOpen: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = NaomiSurface,
        border = BorderStroke(0.5.dp, NaomiBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(
                        brush = Brush.linearGradient(
                            listOf(
                                NaomiTransBlue.copy(alpha = 0.20f),
                                NaomiTransPink.copy(alpha = 0.22f)
                            )
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "N",
                    color = NaomiTextPrimary,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "NAOMI-CHAN™ STAFF",
                    color = NaomiTextTertiary,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.8.sp
                )
                Text(
                    "BFC Fulfilment Centre",
                    color = NaomiTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(
                                if (shiftOpen) NaomiSuccess else NaomiWarning,
                                RoundedCornerShape(99.dp)
                            )
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(
                        if (shiftOpen) "ACTIVE" else "SECURE",
                        color = NaomiTextSecondary,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Text(
                    operatorName,
                    color = NaomiTextPrimary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun WarehouseHomeScreen(
    ui: WarehouseUiState,
    onPick: () -> Unit,
    onScan: () -> Unit,
    onStock: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(NaomiDarkBg),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(10.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        item {
            SectionIntro(
                kicker = "WAREHOUSE OPERATIONS",
                title = "BFC command",
                detail = "Pick, pack, quality check, dispatch, returns and physical inventory on the Sunmi V2."
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                SummaryCard(
                    label = "Pick & pack",
                    value = ui.pickPackCount.toString(),
                    detail = "processing / packed",
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    label = "In delivery",
                    value = ui.dispatchCount.toString(),
                    detail = "active parcels",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                SummaryCard(
                    label = "Returns",
                    value = ui.activeReturnsCount.toString(),
                    detail = "active cases",
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    label = "Low stock",
                    value = ui.lowStockCount.toString(),
                    detail = "needs attention",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            SummaryCard(
                label = "Open holds",
                value = ui.openHoldCount.toString(),
                detail = "exceptions blocking normal flow",
                modifier = Modifier.fillMaxWidth(),
                warning = ui.openHoldCount > 0
            )
        }

        item {
            Panel(
                kicker = "QUICK ACTIONS",
                title = "Warehouse tools"
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = onPick,
                        colors = ButtonDefaults.buttonColors(containerColor = NaomiTextPrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Pick queue", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onScan,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Scan", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onStock,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Stock", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Panel(
                kicker = "TODAY",
                title = "Warehouse attention"
            ) {
                val heldOrder = ui.orders.firstOrNull { it.isOnHold }
                val lowStock = ui.stock.filter { it.isLowStock }
                if (heldOrder == null && lowStock.isEmpty()) {
                    EmptyState("No warehouse exceptions need attention.")
                } else {
                    if (heldOrder != null) {
                        AttentionRow(
                            title = heldOrder.reference,
                            detail = (heldOrder.holdType ?: "Hold") + " · " + (heldOrder.holdNote ?: "Review required"),
                            warning = true
                        )
                    }
                    lowStock.take(3).forEach { item ->
                        AttentionRow(
                            title = item.name,
                            detail = item.quantity.toString() + " remaining · " + item.location,
                            warning = item.isLowStock
                        )
                    }
                }
            }
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = NaomiAccentSurface,
                border = BorderStroke(1.dp, NaomiTransBlue.copy(alpha = 0.35f))
            ) {
                Text(
                    "Warehouse data handling · customer contact and address data is for authorised fulfilment and returns only. Financial refund controls stay outside the handheld workflow.",
                    color = NaomiTextSecondary,
                    fontSize = 9.sp,
                    lineHeight = 13.sp,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }
    }
}

@Composable
private fun WarehousePickScreen(
    ui: WarehouseUiState,
    onTogglePicked: (String, String) -> Unit,
    onPacked: (String) -> Unit,
    onDispatched: (String) -> Unit,
    onResolveHold: (String) -> Unit
) {
    val queue = ui.orders.filter {
        it.stage == WarehouseStage.PROCESSING || it.stage == WarehouseStage.PACKED
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(NaomiDarkBg),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(10.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        item {
            SectionIntro(
                kicker = "01 · PICK & PACK",
                title = "Warehouse queue",
                detail = "Pick against labelled locations, confirm every manifest line, then pack and dispatch."
            )
        }

        if (queue.isEmpty()) {
            item { EmptyState("Pick & pack queue clear.") }
        } else {
            items(queue, key = { it.reference }) { order ->
                WarehouseOrderCard(
                    order = order,
                    onTogglePicked = onTogglePicked,
                    onPacked = onPacked,
                    onDispatched = onDispatched,
                    onResolveHold = onResolveHold
                )
            }
        }
    }
}

@Composable
private fun WarehouseOrderCard(
    order: WarehouseOrder,
    onTogglePicked: (String, String) -> Unit,
    onPacked: (String) -> Unit,
    onDispatched: (String) -> Unit,
    onResolveHold: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = NaomiSurface),
        border = BorderStroke(
            1.dp,
            if (order.isOnHold) NaomiWarning.copy(alpha = 0.45f) else NaomiBorder
        )
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            order.reference,
                            color = NaomiTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black
                        )
                        if (order.isDemo) {
                            Spacer(modifier = Modifier.size(5.dp))
                            StatusPill("SAMPLE", NaomiWarning)
                        }
                    }
                    Text(
                        order.recipient + " · " + order.postcode + " · " + order.createdLabel,
                        color = NaomiTextSecondary,
                        fontSize = 9.sp
                    )
                }
                StatusPill(order.stage.label.uppercase(), NaomiOrange)
            }

            if (order.isOnHold) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = NaomiHoldSurface,
                    border = BorderStroke(1.dp, NaomiWarning.copy(alpha = 0.35f))
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.WarningAmber,
                                contentDescription = null,
                                tint = NaomiWarning,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.size(5.dp))
                            Text(
                                order.holdType ?: "Warehouse hold",
                                color = NaomiTextPrimary,
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp
                            )
                        }
                        Text(
                            order.holdNote ?: "Review required before continuing.",
                            color = NaomiTextSecondary,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(top = 3.dp)
                        )
                        OutlinedButton(
                            onClick = { onResolveHold(order.reference) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp)
                        ) {
                            Text("Resolve hold", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Text(
                "PICK MANIFEST",
                color = NaomiTextTertiary,
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.8.sp
            )

            order.manifest.forEach { line ->
                ManifestRow(
                    line = line,
                    enabled = !order.isOnHold && order.stage == WarehouseStage.PROCESSING,
                    onToggle = { onTogglePicked(order.reference, line.id) }
                )
            }

            when (order.stage) {
                WarehouseStage.PROCESSING -> {
                    Button(
                        onClick = { onPacked(order.reference) },
                        enabled = !order.isOnHold,
                        colors = ButtonDefaults.buttonColors(containerColor = NaomiTextPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Mark parcel packed", fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                }
                WarehouseStage.PACKED -> {
                    Button(
                        onClick = { onDispatched(order.reference) },
                        enabled = !order.isOnHold,
                        colors = ButtonDefaults.buttonColors(containerColor = NaomiTextPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Dispatch parcel", fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                }
                else -> Unit
            }
        }
    }
}

@Composable
private fun ManifestRow(
    line: WarehouseManifestLine,
    enabled: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onToggle),
        shape = RoundedCornerShape(9.dp),
        color = NaomiSurfaceVariant,
        border = BorderStroke(1.dp, NaomiBorder)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                modifier = Modifier.size(24.dp),
                shape = RoundedCornerShape(6.dp),
                color = if (line.picked) NaomiSuccess else NaomiSurface,
                border = BorderStroke(1.dp, if (line.picked) NaomiSuccess else NaomiBorder)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (line.picked) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Picked",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    line.quantity.toString() + " × " + line.name,
                    color = NaomiTextPrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    line.sku + (line.size?.let { " · Size " + it } ?: ""),
                    color = NaomiTextSecondary,
                    fontSize = 8.sp
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "LOCATION",
                    color = NaomiTextTertiary,
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    line.location,
                    color = NaomiTextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
private fun WarehouseStockScreen(
    ui: WarehouseUiState,
    onAdjust: (String, Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(NaomiDarkBg),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            SectionIntro(
                kicker = "04 · INVENTORY",
                title = "Physical stock",
                detail = "Quick-count adjustments for the handheld. Low-stock records are highlighted."
            )
        }

        items(ui.stock, key = { it.sku }) { stock ->
            StockRow(stock = stock, onAdjust = onAdjust)
        }
    }
}

@Composable
private fun StockRow(
    stock: WarehouseStockItem,
    onAdjust: (String, Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(11.dp),
        colors = CardDefaults.cardColors(containerColor = NaomiSurface),
        border = BorderStroke(
            1.dp,
            if (stock.isLowStock) NaomiWarning.copy(alpha = 0.45f) else NaomiBorder
        )
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stock.name,
                        color = NaomiTextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                    if (stock.isLowStock) {
                        Spacer(modifier = Modifier.size(5.dp))
                        StatusPill("LOW", NaomiWarning)
                    }
                }
                Text(
                    stock.variant + " · " + stock.sku,
                    color = NaomiTextSecondary,
                    fontSize = 8.sp
                )
                Text(
                    "Bin " + stock.location + " · threshold " + stock.lowStockThreshold,
                    color = NaomiTextTertiary,
                    fontSize = 8.sp
                )
            }

            OutlinedButton(
                onClick = { onAdjust(stock.sku, -1) },
                modifier = Modifier.size(width = 44.dp, height = 40.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            ) {
                Text("−", fontSize = 16.sp, fontWeight = FontWeight.Black)
            }

            Text(
                stock.quantity.toString(),
                color = NaomiTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black
            )

            Button(
                onClick = { onAdjust(stock.sku, 1) },
                colors = ButtonDefaults.buttonColors(containerColor = NaomiTextPrimary),
                modifier = Modifier.size(width = 44.dp, height = 40.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            ) {
                Text("+", fontSize = 16.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun WarehouseMoreScreen(
    ui: WarehouseUiState,
    isAdmin: Boolean,
    onReceiveReturn: (String) -> Unit,
    onResolveHold: (String) -> Unit,
    onLock: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(NaomiDarkBg),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(10.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        item {
            SectionIntro(
                kicker = "WAREHOUSE CONTROL",
                title = "Returns & terminal",
                detail = "Returns intake, operational holds, session controls and device information."
            )
        }

        item {
            Panel(
                kicker = "03 · RETURNS INTAKE",
                title = "Physical returns"
            ) {
                if (ui.returns.isEmpty()) {
                    EmptyState("Returns intake clear.")
                } else {
                    ui.returns.forEach { item ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(9.dp),
                            color = NaomiSurfaceVariant,
                            border = BorderStroke(1.dp, NaomiBorder)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    item.reference,
                                    color = NaomiTextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    item.customer + " · " + item.reason,
                                    color = NaomiTextSecondary,
                                    fontSize = 9.sp
                                )
                                Text(
                                    item.status,
                                    color = if (item.received) NaomiSuccess else NaomiWarning,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 3.dp)
                                )
                                if (!item.received) {
                                    Button(
                                        onClick = { onReceiveReturn(item.reference) },
                                        colors = ButtonDefaults.buttonColors(containerColor = NaomiTextPrimary),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 6.dp)
                                    ) {
                                        Text("Receive return", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }

        item {
            Panel(
                kicker = "05 · EXCEPTIONS",
                title = "Operational holds"
            ) {
                val holds = ui.orders.filter { it.isOnHold }
                if (holds.isEmpty()) {
                    EmptyState("No operational holds.")
                } else {
                    holds.forEach { order ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(9.dp),
                            color = NaomiHoldSurface,
                            border = BorderStroke(1.dp, NaomiWarning.copy(alpha = 0.35f))
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    order.reference,
                                    color = NaomiTextPrimary,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.sp
                                )
                                Text(
                                    (order.holdType ?: "Hold") + " · " + (order.holdNote ?: ""),
                                    color = NaomiTextSecondary,
                                    fontSize = 8.sp
                                )
                                OutlinedButton(
                                    onClick = { onResolveHold(order.reference) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 5.dp)
                                ) {
                                    Text("Resolve hold", fontSize = 9.sp)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }

        item {
            Panel(
                kicker = "TERMINAL",
                title = "Sunmi V2"
            ) {
                KeyValueRow("Operator", ui.operatorName)
                KeyValueRow("Access", if (isAdmin) "Administrator" else "Warehouse staff")
                KeyValueRow("Shift", ui.shiftId ?: "No open shift")
                KeyValueRow("App", "Naomi-Chan BFC Warehouse " + BuildConfig.VERSION_NAME)
                KeyValueRow("Target", "SUNMI V2 · Android 7.1+")
                Button(
                    onClick = onLock,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NaomiTextPrimary,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text("Lock terminal", fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
            }
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = NaomiSurface,
                border = BorderStroke(1.dp, NaomiBorder)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        "BFC HANDHELD POLICY",
                        color = NaomiTextTertiary,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        "Use the device only for authorised warehouse activity. Do not photograph, copy or retain customer address/contact details outside the fulfilment workflow.",
                        color = NaomiTextSecondary,
                        fontSize = 9.sp,
                        lineHeight = 13.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionIntro(
    kicker: String,
    title: String,
    detail: String
) {
    Column(modifier = Modifier.padding(bottom = 2.dp)) {
        Text(
            kicker,
            color = NaomiTextTertiary,
            fontSize = 8.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.9.sp
        )
        Text(
            title,
            color = NaomiTextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black
        )
        Text(
            detail,
            color = NaomiTextSecondary,
            fontSize = 9.sp,
            lineHeight = 13.sp
        )
    }
}

@Composable
private fun SummaryCard(
    label: String,
    value: String,
    detail: String,
    modifier: Modifier,
    warning: Boolean = false
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(11.dp),
        colors = CardDefaults.cardColors(containerColor = NaomiSurface),
        border = BorderStroke(
            1.dp,
            if (warning) NaomiWarning.copy(alpha = 0.40f) else NaomiBorder
        )
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                label.uppercase(),
                color = NaomiTextTertiary,
                fontSize = 7.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.7.sp
            )
            Text(
                value,
                color = NaomiTextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(top = 2.dp)
            )
            Text(
                detail,
                color = NaomiTextSecondary,
                fontSize = 8.sp
            )
        }
    }
}

@Composable
private fun Panel(
    kicker: String,
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = NaomiSurface),
        border = BorderStroke(1.dp, NaomiBorder)
    ) {
        Column {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                Text(
                    kicker,
                    color = NaomiTextTertiary,
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.8.sp
                )
                Text(
                    title,
                    color = NaomiTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NaomiSurface)
                    .padding(start = 10.dp, end = 10.dp, bottom = 10.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun AttentionRow(
    title: String,
    detail: String,
    warning: Boolean
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp),
        shape = RoundedCornerShape(8.dp),
        color = if (warning) NaomiHoldSurface else NaomiSurfaceVariant,
        border = BorderStroke(
            1.dp,
            if (warning) NaomiWarning.copy(alpha = 0.30f) else NaomiBorder
        )
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (warning) Icons.Default.WarningAmber else Icons.Default.Inventory2,
                contentDescription = null,
                tint = if (warning) NaomiWarning else NaomiOrange,
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.size(6.dp))
            Column {
                Text(
                    title,
                    color = NaomiTextPrimary,
                    fontWeight = FontWeight.Black,
                    fontSize = 10.sp
                )
                Text(
                    detail,
                    color = NaomiTextSecondary,
                    fontSize = 8.sp
                )
            }
        }
    }
}

@Composable
private fun StatusPill(
    text: String,
    accent: Color
) {
    Surface(
        shape = RoundedCornerShape(99.dp),
        color = accent.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.28f))
    ) {
        Text(
            text,
            color = accent,
            fontSize = 7.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun EmptyState(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(9.dp),
        color = NaomiSurfaceVariant,
        border = BorderStroke(1.dp, NaomiBorder)
    ) {
        Text(
            text,
            color = NaomiTextSecondary,
            fontSize = 9.sp,
            modifier = Modifier.padding(10.dp)
        )
    }
}

@Composable
private fun KeyValueRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = NaomiTextSecondary, fontSize = 9.sp)
        Text(
            value,
            color = NaomiTextPrimary,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 10.dp)
        )
    }
}
