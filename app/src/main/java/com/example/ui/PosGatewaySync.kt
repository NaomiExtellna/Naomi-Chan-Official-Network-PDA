package com.example.ui

import androidx.lifecycle.viewModelScope
import com.example.network.WirelessFlaskClient
import kotlinx.coroutines.launch

/**
 * Runs a full two-way gateway sync without putting networking logic in Compose.
 *
 * 1. Publishes current PDA/operator/printer state to Flask.
 * 2. Pulls orders targeted to this PDA.
 * 3. Retries locally buffered receipts back to the web gateway.
 *
 * The existing ViewModel methods own their UI state and queue mutations, so
 * this coordinator only sequences the registration step before triggering them.
 */
fun PosViewModel.syncGatewayNow() {
    viewModelScope.launch {
        val client = WirelessFlaskClient()
        val registered = client.syncDevicePresence(
            operatorName = operatorCapabilities.value.displayName,
            shiftId = activeShiftId.value.orEmpty(),
            printerConnected = printerStatus.value.isConnected,
            unsyncedReceipts = unsyncedCount.value
        )

        logAction(
            action = if (registered) "PDA_GATEWAY_SYNC" else "PDA_GATEWAY_SYNC_FAILED",
            target = client.getDeviceId(),
            details = "gateway=${client.getBaseUrl()}; printer=${printerStatus.value.isConnected}; unsynced=${unsyncedCount.value}",
            severity = if (registered) "INFO" else "WARN"
        )

        if (registered) {
            fetchPendingWirelessOrders()
            syncAllBufferedTransactions()
        } else {
            // Refresh the public online/offline state so the Ledger immediately
            // reports an unreachable gateway instead of leaving stale UI state.
            checkWirelessConnection()
        }
    }
}
