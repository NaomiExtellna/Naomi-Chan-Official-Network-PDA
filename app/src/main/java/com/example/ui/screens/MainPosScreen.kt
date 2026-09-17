package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.PrinterChannel
import com.example.ui.AuthViewModel
import com.example.ui.PosViewModel
import com.example.ui.UiMessage
import com.example.ui.components.NaomiHeader
import com.example.ui.theme.NaomiBorder
import com.example.ui.theme.NaomiDarkBg
import com.example.ui.theme.NaomiOrange
import com.example.ui.theme.NaomiSurface
import com.example.ui.theme.NaomiTextPrimary
import com.example.ui.theme.NaomiTextSecondary
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeoutOrNull

private const val IDLE_ATTRACT_DELAY_MS = 45_000L

private enum class MainTab {
    DASHBOARD,
    RECEIPT,
    SCAN,
    HISTORY,
    MORE,
    PREVIEW,
    BUSINESS_CARD,
    BLACKPOOL,
    PRINTERS,
    OPERATIONS
}

private data class MainNavItem(
    val tab: MainTab,
    val icon: ImageVector,
    val label: String
)

@Composable
fun MainPosScreen(viewModel: PosViewModel, authViewModel: AuthViewModel) {
    val authState by authViewModel.state.collectAsStateWithLifecycle()
    val currentUser = authState.currentUser
    val currentReceipt by viewModel.currentReceipt.collectAsStateWithLifecycle()
    val selectedChannel by viewModel.selectedChannel.collectAsStateWithLifecycle()
    val printerStatus by viewModel.printerStatus.collectAsStateWithLifecycle()
    val unsyncedCount by viewModel.unsyncedCount.collectAsStateWithLifecycle()
    val isOfflineSimulated by viewModel.isOfflineSimulated.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var showChannelDialog by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf(MainTab.RECEIPT) }
    var receiptStep by remember { mutableIntStateOf(0) }
    var businessCardDraft by remember { mutableStateOf(BusinessCardDraft()) }
    var showAttractMode by remember { mutableStateOf(false) }

    // Touches are frequent on a POS. A conflated channel resets the idle countdown without
    // mutating Compose state, avoiding a top-level recomposition on every button press/tap.
    val idleResetChannel = remember { Channel<Unit>(capacity = Channel.CONFLATED) }

    val utilityTabs = remember {
        setOf(MainTab.BUSINESS_CARD, MainTab.BLACKPOOL, MainTab.PRINTERS, MainTab.OPERATIONS)
    }
    val navItems = remember {
        listOf(
            MainNavItem(MainTab.RECEIPT, Icons.Default.ReceiptLong, "Sell"),
            MainNavItem(MainTab.SCAN, Icons.Default.QrCodeScanner, "Scan"),
            MainNavItem(MainTab.HISTORY, Icons.Default.CloudSync, "Activity"),
            MainNavItem(MainTab.DASHBOARD, Icons.Default.Home, "Home"),
            MainNavItem(MainTab.MORE, Icons.Default.MoreHoriz, "More")
        )
    }

    val attractModeEligible = !showChannelDialog &&
        !printerStatus.isPrinting &&
        activeTab != MainTab.SCAN &&
        activeTab != MainTab.PREVIEW

    fun dismissAttractMode() {
        showAttractMode = false
        idleResetChannel.trySend(Unit)
    }

    fun openReceiptFromVenue() {
        receiptStep = 1
        activeTab = MainTab.RECEIPT
    }

    LaunchedEffect(attractModeEligible, showAttractMode) {
        if (!attractModeEligible || showAttractMode) return@LaunchedEffect

        while (true) {
            val resetReceived = withTimeoutOrNull(IDLE_ATTRACT_DELAY_MS) {
                idleResetChannel.receive()
                true
            } ?: false

            if (!resetReceived) {
                showAttractMode = true
                break
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.uiMessages.collect { msg ->
            when (msg) {
                is UiMessage.Success -> snackbarHostState.showSnackbar("✓ ${msg.message}")
                is UiMessage.Error -> snackbarHostState.showSnackbar("⚠ ${msg.message}")
                is UiMessage.Warning -> snackbarHostState.showSnackbar("ℹ ${msg.message}")
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(showAttractMode) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (!showAttractMode && event.changes.any { it.pressed && !it.previousPressed }) {
                            idleResetChannel.trySend(Unit)
                        }
                    }
                }
            }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                NaomiHeader(
                    selectedChannel = selectedChannel,
                    printerStatus = printerStatus,
                    unsyncedCount = unsyncedCount,
                    isOfflineSimulated = isOfflineSimulated,
                    onToggleOffline = { viewModel.toggleOfflineSimulation() },
                    onReloadPaper = { viewModel.reloadPaperRoll() },
                    onSelectChannelClick = { showChannelDialog = true }
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = NaomiSurface,
                    tonalElevation = 0.dp,
                    modifier = Modifier
                        .height(66.dp)
                        .navigationBarsPadding()
                        .testTag("main_navigation_bar")
                ) {
                    navItems.forEach { item ->
                        val isSelected = activeTab == item.tab ||
                            (item.tab == MainTab.MORE && activeTab in utilityTabs)
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { activeTab = item.tab },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    modifier = Modifier.size(23.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = item.label,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = NaomiOrange,
                                selectedTextColor = NaomiTextPrimary,
                                unselectedIconColor = NaomiTextSecondary,
                                unselectedTextColor = NaomiTextSecondary,
                                indicatorColor = NaomiOrange.copy(alpha = 0.10f)
                            ),
                            modifier = Modifier.testTag("nav_tab_${item.tab.name}")
                        )
                    }
                }
            },
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            },
            containerColor = NaomiDarkBg
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                when (activeTab) {
                    MainTab.DASHBOARD -> EventDashboardScreen(
                        authViewModel = authViewModel,
                        posViewModel = viewModel,
                        onNewReceipt = {
                            receiptStep = 0
                            activeTab = MainTab.RECEIPT
                        },
                        onScan = { activeTab = MainTab.SCAN },
                        onBlackpool = { activeTab = MainTab.BLACKPOOL },
                        onOps = { activeTab = MainTab.OPERATIONS }
                    )

                    MainTab.RECEIPT -> ReceiptWizardScreen(
                        viewModel = viewModel,
                        receipt = currentReceipt,
                        step = receiptStep,
                        onStepChange = { receiptStep = it },
                        onNavigateToPreview = { activeTab = MainTab.PREVIEW }
                    )

                    MainTab.SCAN -> BarcodeScannerScreen(
                        viewModel = viewModel,
                        onOpenLedger = { activeTab = MainTab.HISTORY }
                    )

                    MainTab.HISTORY -> SyncLedgerScreen(
                        viewModel = viewModel,
                        showFinancials = currentUser?.canViewFinancialTotals == true,
                        canVoid = currentUser?.canVoidReceipts == true,
                        onEditCorrection = {
                            receiptStep = 0
                            activeTab = MainTab.RECEIPT
                        }
                    )

                    MainTab.MORE -> MoreHubScreen(
                        staffName = currentUser?.displayName ?: "Staff",
                        staffRole = if (currentUser?.isAdmin == true) "Administrator" else "Staff",
                        shiftOpen = authState.activeShift != null,
                        onBusinessCard = { activeTab = MainTab.BUSINESS_CARD },
                        onBlackpool = { activeTab = MainTab.BLACKPOOL },
                        onPrinters = { activeTab = MainTab.PRINTERS },
                        onOperations = { activeTab = MainTab.OPERATIONS },
                        onPreviewAttractMode = { showAttractMode = true },
                        onLockTerminal = authViewModel::logout,
                        onSignOut = authViewModel::logout
                    )

                    MainTab.PREVIEW -> ThermalPreviewScreen(viewModel = viewModel, receipt = currentReceipt)
                    MainTab.BUSINESS_CARD -> BusinessCardScreen(
                        viewModel = viewModel,
                        draft = businessCardDraft,
                        onDraftChange = { businessCardDraft = it }
                    )
                    MainTab.BLACKPOOL -> BlackpoolHubScreen(
                        viewModel = viewModel,
                        onStartReceipt = ::openReceiptFromVenue
                    )
                    MainTab.PRINTERS -> PrinterManagerScreen(viewModel = viewModel)
                    MainTab.OPERATIONS -> OperationsScreen(
                        authViewModel = authViewModel,
                        posViewModel = viewModel,
                        onStartReceipt = ::openReceiptFromVenue
                    )
                }
            }
        }

        if (showAttractMode) {
            IdleAttractScreen(onDismiss = ::dismissAttractMode)
        }
    }

    if (showChannelDialog) {
        AlertDialog(
            onDismissRequest = { showChannelDialog = false },
            containerColor = NaomiSurface,
            title = {
                Text(
                    "Printer output",
                    color = NaomiTextPrimary,
                    fontWeight = FontWeight.Black,
                    fontSize = 17.sp
                )
            },
            text = {
                Column {
                    PrinterChannel.entries.forEach { channel ->
                        val isCurrent = selectedChannel == channel
                        OutlinedButton(
                            onClick = {
                                viewModel.selectChannel(channel)
                                showChannelDialog = false
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isCurrent) NaomiOrange.copy(alpha = 0.10f) else Color.Transparent,
                                contentColor = if (isCurrent) NaomiOrange else NaomiTextPrimary
                            ),
                            border = BorderStroke(1.dp, if (isCurrent) NaomiOrange else NaomiBorder),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Text(
                                channel.displayName + if (isCurrent) "  • ACTIVE" else "",
                                fontWeight = if (isCurrent) FontWeight.Black else FontWeight.Medium,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showChannelDialog = false }) {
                    Text("Close", color = NaomiOrange)
                }
            }
        )
    }
}
