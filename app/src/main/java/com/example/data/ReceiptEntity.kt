package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "receipts")
data class ReceiptEntity(
    @PrimaryKey val id: String,
    val clientName: String,
    val clientContact: String,
    val gigType: String,
    val venueName: String,
    val gigDate: String,
    val packageTier: String,
    val itemsJson: String,
    val paymentMethod: String,
    val footerNotes: String,
    val subtotal: Double,
    val discountPercent: Double,
    val taxPercent: Double,
    val grandTotal: Double,
    val createdAt: Long,
    val syncStatus: String,
    val isPrinted: Boolean,
    val printedChannel: String? = null,
    val isFreeEvent: Boolean = false,
    val iconType: String = "NAOMI_LOGO",
    val customIconUri: String? = null,
    val processedBy: String = "",
    val shiftId: String? = null,
    val receiptStatus: String = "ACTIVE",
    val voidReason: String? = null,
    val voidedAt: Long? = null,
    val voidedBy: String? = null,
    val replacesReceiptId: String? = null
)
