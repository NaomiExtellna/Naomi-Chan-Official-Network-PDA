package com.example.model

import com.example.R
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class GigType(val label: String, val iconName: String) {
    CLUB_NIGHT("Club Night / Residency", "Nightlife"),
    FREE_COMMUNITY("Free Community Showcase", "Community"),
    FESTIVAL("Music Festival Stage", "Festival"),
    WEDDING("Wedding DJ Experience", "Celebration"),
    CORPORATE("Corporate Gala / Launch", "Business"),
    PRIVATE_PARTY("VIP Private Party", "Party"),
    VINYL_EXCLUSIVE("Analog Vinyl Set", "Record")
}

enum class PackageTier(val title: String, val basePrice: Double, val description: String) {
    FREE_ADMISSION("Free Gig / Guest Pass", 0.0, "Complimentary DJ guest list & free admission"),
    TRACK_SHOUTOUT("Track Shoutout", 2.0, "Live microphone shoutout & dedication during the DJ set"),
    TRACK_REQUEST("Guest Track Request", 3.0, "Song request played live with priority queue"),
    DRINK_VOUCHER("Bar Shot / Drink Voucher", 4.0, "Blackpool bar drink token & DJ celebration stamp"),
    BAR_ADMISSION("Standard Bar Admission", 5.0, "Standard evening admission pass to Blackpool bar event"),
    VIP_QUEUE_JUMP("VIP Fast-Track Wristband", 8.0, "Priority express entry, queue jump & neon wristband"),
    DJ_BOOTH_PASS("DJ Booth Access & Lanyard", 12.0, "Behind-the-decks access with official Naomi-Chan lanyard"),
    ALL_NIGHT_VIP("All-Night All-Access VIP Pass", 15.0, "Complete evening admission, track priority & VIP booth pass")
}

enum class PaymentMethod(val label: String, val code: String) {
    FREE_PASS("Free Pass / Comp", "FREE"),
    CARD_TERMINAL("Card / POS Terminal", "CARD"),
    CASH("Cash Payment", "CASH"),
    BANK_TRANSFER("Direct Bank Wire", "WIRE"),
    QR_PAY("QR Instant Pay", "QR")
}

enum class ReceiptIconType(val label: String, val description: String, val drawableResId: Int?) {
    NAOMI_LOGO("Naomi-Chan™", "Official DJ brand insignia", R.drawable.img_naomi_logo),
    VINYL("Vinyl Turntable", "Analog vinyl record", R.drawable.ic_vinyl_record),
    HEADPHONES("DJ Headphones", "Audio wave & cans", R.drawable.ic_dj_headphones),
    CROWN_VIP("VIP Stage", "Royalty crown emblem", R.drawable.ic_crown_vip),
    STAR("Festival Star", "Electric showcase star", R.drawable.ic_stage_star),
    CUSTOM("Custom Icon", "Uploaded from Gallery", null),
    NONE("No Icon", "Minimal text-only receipt", null)
}

enum class PrinterChannel(val displayName: String, val hardwareCode: String) {
    SUNMI_BUILTIN("Sunmi V2 Built-in Thermal", "SUNMI_V2"),
    BLUETOOTH("Bluetooth Thermal Printer", "BT_SPP"),
    USB_OTG("USB-OTG Thermal Printer", "USB_RAW")
}

data class ReceiptItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val quantity: Int = 1,
    val unitPrice: Double = 0.0
) {
    val total: Double get() = quantity * unitPrice
}

data class WirelessOrder(
    val id: String = UUID.randomUUID().toString(),
    val clientName: String = "Guest",
    val clientContact: String = "",
    val venueName: String = "The Flying Handbag",
    val items: List<ReceiptItem> = emptyList(),
    val subtotal: Double = 0.0,
    val taxPercent: Double = 20.0,
    val taxAmount: Double = 0.0,
    val grandTotal: Double = 0.0,
    val paymentMethod: PaymentMethod = PaymentMethod.CARD_TERMINAL,
    val notes: String = "",
    val status: String = "PENDING", // PENDING, PRINTED
    val createdAt: Long = System.currentTimeMillis()
)

data class ReceiptData(
    val id: String = "NC-" + (100000 + (Math.random() * 900000).toInt()),
    val clientName: String = "The Flying Handbag",
    val clientContact: String = "01253 624519",
    val gigType: GigType = GigType.CLUB_NIGHT,
    val venueName: String = "The Flying Handbag, Queen St, Blackpool FY1 2NL",
    val gigDate: String = "2026-10-24 22:00",
    val packageTier: PackageTier = PackageTier.BAR_ADMISSION,
    val items: List<ReceiptItem> = listOf(
        ReceiptItem(name = "Standard Bar Admission", quantity = 1, unitPrice = 5.0),
        ReceiptItem(name = "Guest DJ Track Request", quantity = 1, unitPrice = 3.0),
        ReceiptItem(name = "VIP Fast-Track Wristband", quantity = 1, unitPrice = 8.0)
    ),
    val paymentMethod: PaymentMethod = PaymentMethod.CARD_TERMINAL,
    val footerNotes: String = "Keep Blackpool dancing! Thank you for booking Naomi-Chan™ DJ Services.",
    val discountPercent: Double = 0.0,
    val taxPercent: Double = 20.0,
    val createdAt: Long = System.currentTimeMillis(),
    val isBufferedOffline: Boolean = false,
    val isPrinted: Boolean = false,
    val isFreeEvent: Boolean = false,
    val iconType: ReceiptIconType = ReceiptIconType.NAOMI_LOGO,
    val customIconUri: String? = null
) {
    val subtotal: Double get() = items.sumOf { it.total }
    val discountAmount: Double get() = subtotal * (discountPercent / 100.0)
    val taxableAmount: Double get() = (subtotal - discountAmount).coerceAtLeast(0.0)
    val taxAmount: Double get() = if (isFreeEvent || grandSubtotalZero) 0.0 else taxableAmount * (taxPercent / 100.0)
    val grandTotal: Double get() = if (isFreeEvent || grandSubtotalZero) 0.0 else taxableAmount + taxAmount

    private val grandSubtotalZero: Boolean get() = subtotal <= 0.0
    val isEffectivelyFree: Boolean get() = isFreeEvent || grandTotal == 0.0

    fun formattedDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.UK)
        return sdf.format(Date(createdAt))
    }

    companion object {
        fun formatCurrency(amount: Double): String {
            val format = NumberFormat.getCurrencyInstance(Locale.UK)
            return format.format(amount)
        }
    }
}

data class RamInfo(
    val totalMb: Long,
    val freeMb: Long,
    val isLowRamDevice: Boolean,
    val lowRamEngineActive: Boolean = true
)

data class PrinterStatus(
    val channel: PrinterChannel = PrinterChannel.SUNMI_BUILTIN,
    val isConnected: Boolean = true,
    val isPrinting: Boolean = false,
    val hasPaper: Boolean = true,
    val isCoverOpen: Boolean = false,
    val isOverheated: Boolean = false,
    val deviceName: String = "Sunmi V2 Inner Thermal (58mm)",
    val serialNumber: String = "V2P-8839-NC01",
    val headTemperatureCelsius: Int = 38,
    val paperRollRemainingPercent: Int = 85,
    val lastError: String? = null
)
