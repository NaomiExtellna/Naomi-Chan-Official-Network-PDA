package com.example.printer

import android.graphics.Bitmap
import com.example.model.ReceiptData
import com.example.model.ReceiptIconType
import com.example.model.PrinterChannel
import com.sunmi.peripheral.printer.SunmiPrinterService

/**
 * Reliable built-in printer path for SUNMI handheld terminals such as the V2/T5930.
 *
 * The V2 print service is much more reliable when normal receipt content is sent
 * through the native service APIs instead of dispatching an entire mixed text/image/
 * QR receipt as one large ESC/POS byte array.
 */
object SunmiNativePrinter {
    private const val PAPER_COLUMNS = 32
    private const val BUSINESS_CARD_ID = "BUSINESS-CARD"

    fun print(
        service: SunmiPrinterService,
        receipt: ReceiptData,
        logoBitmap: Bitmap?
    ): PrintResult {
        return try {
            service.printerInit(null)

            if (receipt.id == BUSINESS_CARD_ID) {
                printBusinessCard(service, receipt)
            } else {
                printReceipt(service, receipt, logoBitmap)
            }

            // A forced feed flushes any final buffered line on older V2 print services.
            service.lineWrap(4, null)

            PrintResult.Success(
                message = "Printed with SUNMI V2 native printer service",
                channel = PrinterChannel.SUNMI_BUILTIN,
                bytesSent = estimateCharacters(receipt)
            )
        } catch (e: Exception) {
            PrintResult.Error(
                errorReason = "SUNMI native printing failed: ${e.localizedMessage ?: e.javaClass.simpleName}",
                channel = PrinterChannel.SUNMI_BUILTIN
            )
        }
    }

    private fun printReceipt(
        service: SunmiPrinterService,
        receipt: ReceiptData,
        logoBitmap: Bitmap?
    ) {
        service.setAlignment(1, null)

        if (receipt.iconType != ReceiptIconType.NONE && logoBitmap != null) {
            runCatching {
                val scaled = scaleFor58mm(logoBitmap, 304)
                service.printBitmap(scaled, null)
                service.lineWrap(1, null)
                if (scaled !== logoBitmap && !scaled.isRecycled) scaled.recycle()
            }
            // A logo failure must never prevent the actual receipt from printing.
        }

        service.setFontSize(30f, null)
        text(service, "NAOMI-CHAN(TM)")
        service.setFontSize(24f, null)
        text(service, "PREMIUM DJ SERVICES")
        service.setFontSize(22f, null)
        text(service, "Official Booking & Event Receipt")
        text(service, "Blackpool - North West UK")
        text(service, "www.naomichan-dj.com")
        divider(service, '-')

        if (receipt.isEffectivelyFree) {
            text(service, "*** FREE ADMISSION ***")
            text(service, "COMPLIMENTARY ACCESS / NO COVER")
            divider(service, '=')
        }

        service.setAlignment(0, null)
        columns(service, "RECEIPT NO:", receipt.id)
        columns(service, "DATE/TIME:", receipt.formattedDate())
        columns(service, "EVENT TYPE:", receipt.gigType.label)
        columns(service, "VENUE:", receipt.venueName)
        columns(service, "CLIENT:", receipt.clientName)
        if (receipt.clientContact.isNotBlank()) columns(service, "PHONE:", receipt.clientContact)
        if (receipt.processedBy.isNotBlank()) columns(service, "STAFF:", receipt.processedBy)
        receipt.shiftId?.takeIf { it.isNotBlank() }?.let { columns(service, "SHIFT:", it.take(8).uppercase()) }
        receipt.replacesReceiptId?.takeIf { it.isNotBlank() }?.let { columns(service, "CORRECTS:", it) }
        divider(service, '-')

        service.setAlignment(1, null)
        text(service, "SERVICES & MERCHANDISE")
        service.setAlignment(0, null)
        columns(service, "ITEM / SERVICE", "AMOUNT")
        divider(service, '.')

        receipt.items.forEach { item ->
            val title = if (item.quantity > 1) "${item.name} (x${item.quantity})" else item.name
            val amount = if (item.unitPrice == 0.0) "FREE" else ReceiptData.formatCurrency(item.total)
            columns(service, title, amount)
        }
        divider(service, '-')

        if (receipt.isEffectivelyFree) {
            columns(service, "ADMISSION:", "FREE")
            columns(service, "SUBTOTAL:", ReceiptData.formatCurrency(0.0))
            columns(service, "VAT (0%):", ReceiptData.formatCurrency(0.0))
            divider(service, '=')
            columns(service, "TOTAL DUE:", "${ReceiptData.formatCurrency(0.0)} FREE")
            columns(service, "PAYMENT:", "FREE PASS / COMP")
            columns(service, "STATUS:", if (receipt.isBufferedOffline) "PENDING SYNC" else "ADMITTED")
        } else {
            columns(service, "SUBTOTAL:", ReceiptData.formatCurrency(receipt.subtotal))
            if (receipt.discountPercent > 0) {
                columns(
                    service,
                    "DISCOUNT (${receipt.discountPercent.toInt()}%):",
                    "-${ReceiptData.formatCurrency(receipt.discountAmount)}"
                )
            }
            columns(service, "VAT (${receipt.taxPercent.toInt()}%):", ReceiptData.formatCurrency(receipt.taxAmount))
            divider(service, '=')
            columns(service, "TOTAL DUE:", ReceiptData.formatCurrency(receipt.grandTotal))
            columns(service, "PAYMENT:", receipt.paymentMethod.label)
            columns(service, "STATUS:", if (receipt.isBufferedOffline) "PENDING SYNC" else "VERIFIED & PAID")
        }

        divider(service, '-')
        service.setAlignment(1, null)

        if (receipt.footerNotes.isNotBlank()) {
            wrapped(service, receipt.footerNotes)
            service.lineWrap(1, null)
        }

        val qrUrl = if (receipt.isEffectivelyFree) {
            "https://naomichan-dj.com/verify?id=${receipt.id}&type=FREE_PASS"
        } else {
            "https://naomichan-dj.com/verify?id=${receipt.id}"
        }

        // QR support varies across early V2 firmware. If the QR call fails, keep the
        // receipt useful by printing the verification URL as text instead.
        val qrPrinted = runCatching {
            service.printQRCode(qrUrl, 5, 1, null)
            service.lineWrap(1, null)
        }.isSuccess

        if (!qrPrinted) {
            wrapped(service, qrUrl)
        }

        text(service, if (receipt.isEffectivelyFree) "[ SCAN TO VALIDATE FREE ENTRY ]" else "[ SCAN TO VERIFY BOOKING ]")
        text(service, "Booking Ref: ${receipt.id}")
        service.lineWrap(1, null)
        text(service, "Naomi-Chan(TM) DJ Sound Collective")
        text(service, "Thank you for rocking with us!")
    }

    private fun printBusinessCard(service: SunmiPrinterService, card: ReceiptData) {
        val businessName = card.clientName.ifBlank { "Naomi-Chan(TM)" }
        val displayName = card.clientContact
        val role = card.gigDate
        val location = card.venueName
        val email = card.items.getOrNull(0)?.name.orEmpty()
        val phone = card.items.getOrNull(1)?.name.orEmpty()
        val website = card.footerNotes.trim()

        service.setAlignment(1, null)
        service.setFontSize(30f, null)
        wrapped(service, businessName)
        service.setFontSize(24f, null)
        if (displayName.isNotBlank()) wrapped(service, displayName)
        if (role.isNotBlank()) wrapped(service, role)
        divider(service, '=')
        if (email.isNotBlank()) wrapped(service, email)
        if (phone.isNotBlank()) wrapped(service, phone)
        if (website.isNotBlank()) wrapped(service, website)
        if (location.isNotBlank()) wrapped(service, location)

        if (website.isNotBlank()) {
            val qrTarget = if (website.startsWith("http://") || website.startsWith("https://")) website else "https://$website"
            service.lineWrap(1, null)
            val qrPrinted = runCatching {
                service.printQRCode(qrTarget, 5, 1, null)
                service.lineWrap(1, null)
            }.isSuccess
            if (!qrPrinted) wrapped(service, qrTarget)
            text(service, "SCAN TO CONNECT")
        }

        service.lineWrap(1, null)
        text(service, "Naomi-Chan(TM) Official Network")
    }

    private fun text(service: SunmiPrinterService, value: String) {
        // SUNMI documentation requires a newline to force immediate printing on
        // text shorter than a complete line; without it older V2 services can cache it.
        service.printText(sanitize(value) + "\n", null)
    }

    private fun wrapped(service: SunmiPrinterService, value: String) {
        val clean = sanitize(value).trim()
        if (clean.isEmpty()) {
            text(service, "")
            return
        }

        val words = clean.split(Regex("\\s+"))
        var line = ""
        for (word in words) {
            if (word.length > PAPER_COLUMNS) {
                if (line.isNotBlank()) {
                    text(service, line)
                    line = ""
                }
                word.chunked(PAPER_COLUMNS).forEach { text(service, it) }
                continue
            }
            val candidate = if (line.isBlank()) word else "$line $word"
            if (candidate.length <= PAPER_COLUMNS) {
                line = candidate
            } else {
                text(service, line)
                line = word
            }
        }
        if (line.isNotBlank()) text(service, line)
    }

    private fun columns(service: SunmiPrinterService, leftValue: String, rightValue: String) {
        val left = sanitize(leftValue)
        val right = sanitize(rightValue)
        val maxLeft = 17
        val maxRight = PAPER_COLUMNS - maxLeft - 1
        val safeLeft = ellipsize(left, maxLeft)
        val safeRight = ellipsize(right, maxRight)
        val spaces = (PAPER_COLUMNS - safeLeft.length - safeRight.length).coerceAtLeast(1)
        text(service, (safeLeft + " ".repeat(spaces) + safeRight).take(PAPER_COLUMNS))
    }

    private fun divider(service: SunmiPrinterService, character: Char) {
        text(service, character.toString().repeat(PAPER_COLUMNS))
    }

    private fun ellipsize(value: String, limit: Int): String {
        if (limit <= 0) return ""
        if (value.length <= limit) return value
        if (limit <= 2) return value.take(limit)
        return value.take(limit - 2) + ".."
    }

    private fun sanitize(value: String): String = value
        .replace("™", "(TM)")
        .replace("•", "-")
        .replace("–", "-")
        .replace("—", "-")
        .replace("“", "\"")
        .replace("”", "\"")
        .replace("‘", "'")
        .replace("’", "'")
        .replace("…", "...")
        .replace('\u00A0', ' ')

    private fun scaleFor58mm(bitmap: Bitmap, targetWidth: Int): Bitmap {
        val width = targetWidth.coerceIn(8, 384)
        if (bitmap.width <= width) return bitmap
        val ratio = width.toFloat() / bitmap.width.toFloat()
        val height = (bitmap.height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }

    private fun estimateCharacters(receipt: ReceiptData): Int {
        return receipt.items.sumOf { it.name.length + 16 } + receipt.footerNotes.length + 256
    }
}
