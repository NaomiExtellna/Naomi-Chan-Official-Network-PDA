package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Print
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PrinterChannel
import com.example.ui.PosTab
import com.example.ui.PosViewModel
import com.example.ui.UiMessage
import com.example.ui.components.NaomiHeader
import com.example.ui.theme.NaomiDarkBg
import com.example.ui.theme.NaomiDeepRed
import com.example.ui.theme.NaomiOrange
import com.example.ui.theme.NaomiRed
import com.example.ui.theme.NaomiSurface
import com.example.ui.theme.NaomiTextPrimary
import com.example.ui.theme.NaomiTextSecondary

@Composable
fun MainPosScreen(viewModel: PosViewModel) {
    val activeTab by viewModel.activeTab.collectAsState()
    val currentReceipt by viewModel.currentReceipt.collectAsState()
    val selectedChannel by viewModel.selectedChannel.collectAsState()
    val printerStatus by viewModel.printerStatus.collectAsState()
    val unsyncedCount by viewModel.unsyncedCount.collectAsState()
    val isOfflineSimulated by viewModel.isOfflineSimulated.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showChannelDialog by remember { mutableStateOf(false) }

    // Listen to UI messages (print successes, errors, offline alerts)
    LaunchedEffect(Unit) {
        viewModel.uiMessages.collect { msg ->
            when (msg) {
                is UiMessage.Success -> snackbarHostState.showSnackbar("✓ ${msg.message}")
                is UiMessage.Error -> snackbarHostState.showSnackbar("⚠️ ${msg.message}")
                is UiMessage.Warning -> snackbarHostState.showSnackbar("ℹ ${msg.message}")
            }
        }
    }

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
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("main_navigation_bar")
            ) {
                val navItems = listOf(
                    Triple(PosTab.BUILDER, Icons.Default.EditNote, "POS Builder"),
                    Triple(PosTab.PREVIEW, Icons.Default.Article, "58mm Live"),
                    Triple(PosTab.PRINTERS, Icons.Default.Print, "Printers"),
                    Triple(PosTab.HISTORY, Icons.Default.CloudSync, "Sync Ledger")
                )

                navItems.forEach { (tab, icon, label) ->
                    val isSelected = activeTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { viewModel.setTab(tab) },
                        icon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = NaomiOrange,
                            unselectedIconColor = NaomiTextSecondary,
                            unselectedTextColor = NaomiTextSecondary,
                            indicatorColor = NaomiRed
                        ),
                        modifier = Modifier.testTag("nav_tab_${tab.name}")
                    )
                }
            }
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(16.dp)
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
                PosTab.BUILDER -> ReceiptBuilderScreen(
                    viewModel = viewModel,
                    receipt = currentReceipt,
                    onNavigateToPreview = { viewModel.setTab(PosTab.PREVIEW) }
                )
                PosTab.PREVIEW -> ThermalPreviewScreen(
                    viewModel = viewModel,
                    receipt = currentReceipt
                )
                PosTab.PRINTERS -> PrinterManagerScreen(
                    viewModel = viewModel
                )
                PosTab.HISTORY -> SyncLedgerScreen(
                    viewModel = viewModel
                )
            }
        }
    }

    // Quick Channel Select Dialog
    if (showChannelDialog) {
        AlertDialog(
            onDismissRequest = { showChannelDialog = false },
            containerColor = NaomiSurface,
            title = {
                Text(
                    text = "Select Primary Output Printer",
                    color = NaomiTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column {
                    for (ch in PrinterChannel.values()) {
                        val isCurrent = selectedChannel == ch
                        OutlinedButton(
                            onClick = {
                                viewModel.selectChannel(ch)
                                showChannelDialog = false
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isCurrent) NaomiRed.copy(alpha = 0.25f) else Color.Transparent,
                                contentColor = if (isCurrent) NaomiOrange else NaomiTextPrimary
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                if (isCurrent) 1.5.dp else 1.dp,
                                if (isCurrent) NaomiOrange else NaomiSurface
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = ch.displayName + if (isCurrent) " (Active)" else "",
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
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
