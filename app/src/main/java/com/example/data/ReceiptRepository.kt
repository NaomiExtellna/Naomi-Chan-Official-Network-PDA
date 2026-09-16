package com.example.data

import com.example.model.GigType
import com.example.model.PackageTier
import com.example.model.PaymentMethod
import com.example.model.ReceiptData
import com.example.model.ReceiptItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class ReceiptRepository(private val receiptDao: ReceiptDao) {

    val allReceipts: Flow<List<ReceiptData>> = receiptDao.getAllReceipts().map { list ->
        list.map { entityToData(it) }
    }

    val unsyncedCount: Flow<Int> = receiptDao.getUnsyncedCount()

    suspend fun saveReceipt(receipt: ReceiptData, isOfflineBuffered: Boolean): Long = withContext(Dispatchers.IO) {
        val syncStatus = if (isOfflineBuffered) "BUFFERED_OFFLINE" else "PENDING_RETRY"
        receiptDao.insertReceipt(dataToEntity(receipt, syncStatus))
    }

    suspend fun markPrinted(receiptId: String, channel: String) = withContext(Dispatchers.IO) {
        receiptDao.updatePrintStatus(receiptId, true, channel)
    }

    suspend fun getUnsyncedReceipts(): List<ReceiptData> = withContext(Dispatchers.IO) {
        receiptDao.getUnsyncedReceipts().map { entityToData(it) }
    }

    suspend fun markSynced(receiptId: String) = withContext(Dispatchers.IO) {
        receiptDao.updateSyncStatus(receiptId, "SYNCED")
    }

    suspend fun markPendingRetry(receiptId: String) = withContext(Dispatchers.IO) {
        receiptDao.updateSyncStatus(receiptId, "PENDING_RETRY")
    }

    suspend fun voidReceipt(receiptId: String, reason: String, voidedBy: String): Boolean = withContext(Dispatchers.IO) {
        receiptDao.voidReceipt(receiptId, reason.trim(), System.currentTimeMillis(), voidedBy) > 0
    }

    suspend fun restoreReceipts(receipts: List<ReceiptData>): Int = withContext(Dispatchers.IO) {
        var restored = 0
        for (receipt in receipts) {
            val syncStatus = if (receipt.isBufferedOffline) "PENDING_RETRY" else "SYNCED"
            if (receiptDao.insertReceiptIgnore(dataToEntity(receipt, syncStatus)) != -1L) restored++
        }
        restored
    }

    suspend fun archiveSyncedOlderThan(days: Int): Int = withContext(Dispatchers.IO) {
        val cutoff = System.currentTimeMillis() - days.coerceAtLeast(1) * 86_400_000L
        receiptDao.archiveSyncedBefore(cutoff)
    }

    suspend fun wipeAllReceipts(): Int = withContext(Dispatchers.IO) {
        receiptDao.deleteAllReceipts()
    }

    suspend fun deleteReceipt(receiptId: String) = withContext(Dispatchers.IO) {
        receiptDao.deleteReceipt(receiptId)
    }

    private fun serializeItems(items: List<ReceiptItem>): String {
        val jsonArray = JSONArray()
        for (item in items) {
            jsonArray.put(JSONObject().apply {
                put("id", item.id)
                put("name", item.name)
                put("quantity", item.quantity)
                put("unitPrice", item.unitPrice)
            })
        }
        return jsonArray.toString()
    }

    private fun deserializeItems(jsonString: String): List<ReceiptItem> {
        val list = mutableListOf<ReceiptItem>()
        return try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.optJSONObject(i) ?: continue
                list.add(
                    ReceiptItem(
                        id = obj.optString("id"),
                        name = obj.optString("name", "Service"),
                        quantity = obj.optInt("quantity", 1).coerceAtLeast(1),
                        unitPrice = obj.optDouble("unitPrice", 0.0)
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun dataToEntity(data: ReceiptData, syncStatus: String): ReceiptEntity {
        return ReceiptEntity(
            id = data.id,
            clientName = data.clientName,
            clientContact = data.clientContact,
            gigType = data.gigType.name,
            venueName = data.venueName,
            gigDate = data.gigDate,
            packageTier = data.packageTier.name,
            itemsJson = serializeItems(data.items),
            paymentMethod = data.paymentMethod.name,
            footerNotes = data.footerNotes,
            subtotal = data.subtotal,
            discountPercent = data.discountPercent,
            taxPercent = data.taxPercent,
            grandTotal = data.grandTotal,
            createdAt = data.createdAt,
            syncStatus = syncStatus,
            isPrinted = data.isPrinted,
            isFreeEvent = data.isFreeEvent,
            iconType = data.iconType.name,
            customIconUri = data.customIconUri,
            processedBy = data.processedBy,
            shiftId = data.shiftId,
            receiptStatus = data.receiptStatus,
            voidReason = data.voidReason,
            voidedAt = data.voidedAt,
            voidedBy = data.voidedBy,
            replacesReceiptId = data.replacesReceiptId
        )
    }

    private fun entityToData(entity: ReceiptEntity): ReceiptData {
        val gigType = runCatching { GigType.valueOf(entity.gigType) }.getOrDefault(GigType.CLUB_NIGHT)
        val packageTier = runCatching { PackageTier.valueOf(entity.packageTier) }.getOrDefault(PackageTier.BAR_ADMISSION)
        val paymentMethod = runCatching { PaymentMethod.valueOf(entity.paymentMethod) }.getOrDefault(PaymentMethod.CARD_TERMINAL)
        val iconType = runCatching { com.example.model.ReceiptIconType.valueOf(entity.iconType) }
            .getOrDefault(com.example.model.ReceiptIconType.NAOMI_LOGO)

        return ReceiptData(
            id = entity.id,
            clientName = entity.clientName,
            clientContact = entity.clientContact,
            gigType = gigType,
            venueName = entity.venueName,
            gigDate = entity.gigDate,
            packageTier = packageTier,
            items = deserializeItems(entity.itemsJson),
            paymentMethod = paymentMethod,
            footerNotes = entity.footerNotes,
            discountPercent = entity.discountPercent,
            taxPercent = entity.taxPercent,
            createdAt = entity.createdAt,
            isBufferedOffline = entity.syncStatus != "SYNCED",
            isPrinted = entity.isPrinted,
            isFreeEvent = entity.isFreeEvent,
            iconType = iconType,
            customIconUri = entity.customIconUri,
            processedBy = entity.processedBy,
            shiftId = entity.shiftId,
            receiptStatus = entity.receiptStatus,
            voidReason = entity.voidReason,
            voidedAt = entity.voidedAt,
            voidedBy = entity.voidedBy,
            replacesReceiptId = entity.replacesReceiptId
        )
    }
}
