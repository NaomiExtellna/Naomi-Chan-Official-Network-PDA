package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ReceiptData
import com.example.ui.PosViewModel
import com.example.ui.theme.NaomiDarkBg
import com.example.ui.theme.NaomiDeepRed
import com.example.ui.theme.NaomiOrange
import com.example.ui.theme.NaomiRed
import com.example.ui.theme.NaomiSuccess
import com.example.ui.theme.NaomiSurface
import com.example.ui.theme.NaomiTextPrimary
import com.example.ui.theme.NaomiTextSecondary

@Composable
fun SyncLedgerScreen(viewModel: PosViewModel) {
    val receipts by viewModel.allReceipts.collectAsState()
    val unsyncedCount by viewModel.unsyncedCount.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NaomiDarkBg)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("sync_ledger_screen")
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = NaomiSurface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ROOM DB BUFFER & GATEWAY SYNC",
                        color = NaomiOrange,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (unsyncedCount > 0) {
                            "$unsyncedCount receipt(s) waiting to sync"
                        } else {
                            "All saved receipts acknowledged by the gateway"
                        },
                        color = NaomiTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Receipts remain in local Room storage until the configured wireless gateway acknowledges them.",
                        color = NaomiTextSecondary,
                        fontSize = 10.5.sp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))
                Button(
                    onClick = { viewModel.syncAllBufferedTransactions() },
                    enabled = !isSyncing,
                    colors = ButtonDefaults.buttonColors(containerColor = NaomiRed),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("sync_all_btn")
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sync All", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "TRANSACTION HISTORY (${receipts.size})",
            color = NaomiTextSecondary,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        if (receipts.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        tint = NaomiTextSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No saved transactions yet.", color = NaomiTextSecondary, fontSize = 13.sp)
                    Text(
                        "Print or save a receipt in the POS Builder tab.",
                        color = NaomiTextSecondary.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(receipts, key = { it.id }) { receipt ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = NaomiSurface),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("receipt_item_${receipt.id}")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = receipt.id,
                                        color = NaomiTextPrimary,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    if (receipt.isEffectivelyFree) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = NaomiSuccess.copy(alpha = 0.25f)
                                        ) {
                                            Text(
                                                text = "FREE PASS",
                                                color = NaomiSuccess,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 9.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (receipt.isBufferedOffline) {
                                            NaomiOrange.copy(alpha = 0.2f)
                                        } else {
                                            NaomiSuccess.copy(alpha = 0.2f)
                                        }
                                    ) {
                                        Text(
                                            text = if (receipt.isBufferedOffline) "PENDING SYNC" else "SYNCED",
                                            color = if (receipt.isBufferedOffline) NaomiOrange else NaomiSuccess,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "${receipt.clientName} • ${receipt.venueName}",
                                    color = NaomiTextPrimary,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                                Text(
                                    text = "${receipt.gigType.label} • ${receipt.formattedDate()}",
                                    color = NaomiTextSecondary,
                                    fontSize = 10.5.sp
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (receipt.isEffectivelyFree) {
                                        "FREE (£0.00)"
                                    } else {
                                        ReceiptData.formatCurrency(receipt.grandTotal)
                                    },
                                    color = if (receipt.isEffectivelyFree) NaomiSuccess else NaomiOrange,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Button(
                                    onClick = { viewModel.reprintReceipt(receipt) },
                                    colors = ButtonDefaults.buttonColors(containerColor = NaomiDeepRed),
                                    modifier = Modifier.height(30.dp).testTag("reprint_${receipt.id}")
                                ) {
                                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Reprint", fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
