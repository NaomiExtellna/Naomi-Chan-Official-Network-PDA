package com.example.printer

import android.graphics.Bitmap
import android.graphics.Color
import com.example.model.ReceiptData
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset

class EscPosBuilder(private val totalColumns: Int = 32) {

    private val outputStream = ByteArrayOutputStream()
    private val charset: Charset = Charset.forName("CP437")

    companion object {
        val ESC: Byte = 0x1B
        val GS: Byte = 0x1D
        val LF: Byte = 0x0A

        private const val BUSINESS_CARD_ID = "BUSINESS-CARD"

        val CMD_INIT = byteArrayOf(ESC, '@'.code.toByte())
        val CMD_CODE_PAGE_CP437 = byteArrayOf(ESC, 't'.code.toByte(), 0x00)
        val CMD_ALIGN_LEFT = byteArrayOf(ESC, 'a'.code.toByte(), 0x00)
        val CMD_ALIGN_CENTER = byteArrayOf(ESC, 'a'.code.toByte(), 0x01)
        val CMD_ALIGN_RIGHT = byteArrayOf(ESC, 'a'.code.toByte(), 0x02)
        val CMD_BOLD_ON = byteArrayOf(ESC, 'E'.code.toByte(), 0x01)
        val CMD_BOLD_OFF = byteArrayOf(ESC, 'E'.code.toByte(), 0x00)
        val CMD_DOUBLE_SIZE_ON = byteArrayOf(GS, '!'.code.toByte(), 0x11)
        val CMD_DOUBLE_HEIGHT = byteArrayOf(GS, '!'.code.toByte(), 0x01)
        val CMD_TEXT_NORMAL = byteArrayOf(GS, '!'.code.toByte(), 0x00)
        val CMD_REVERSE_ON = byteArrayOf(GS, 'B'.code.toByte(), 0x01)
        val CMD_REVERSE_OFF = byteArrayOf(GS, 'B'.code.toByte(), 0x00)
        val CMD_PARTIAL_CUT = byteArrayOf(GS, 'V'.code.toByte(), 0x01)
        val CMD_FULL_CUT = byteArrayOf(GS, 'V'.code.toByte(), 0x00)
    }

    private fun printable(text: String): String {
        return text
            .replace("™", "(TM)")
            .replace("•", "-")
            .replace("–", "-")
            .replace("—", "-")
            .replace("“", "\"")
            .replace("”", "\"")
            .replace("‘", "'")
            .replace("’", "'")
            .replace("…", "...")
            .replace("\u00A0", " ")
    }

    fun init(): EscPosBuilder {
        outputStream.write(CMD_INIT)
        outputStream.write(CMD_CODE_PAGE_CP437)
        return this
    }

    fun alignLeft(): EscPosBuilder {
        outputStream.write(CMD_ALIGN_LEFT)
        return this
    }

    fun alignCenter(): EscPosBuilder {
        outputStream.write(CMD_ALIGN_CENTER)
        return this
    }

    fun alignRight(): EscPosBuilder {
        outputStream.write(CMD_ALIGN_RIGHT)
        return this
    }

    fun bold(enable: Boolean): EscPosBuilder {
        outputStream.write(if (enable) CMD_BOLD_ON else CMD_BOLD_OFF)
        return this
    }

    fun doubleSize(enable: Boolean): EscPosBuilder {
        outputStream.write(if (enable) CMD_DOUBLE_SIZE_ON else CMD_TEXT_NORMAL)
        return this
    }

    fun doubleHeight(enable: Boolean): EscPosBuilder {
        outputStream.write(if (enable) CMD_DOUBLE_HEIGHT else CMD_TEXT_NORMAL)
        return this
    }

    fun reverse(enable: Boolean): EscPosBuilder {
        outputStream.write(if (enable) CMD_REVERSE_ON else CMD_REVERSE_OFF)
        return this
    }

    fun text(text: String): EscPosBuilder {
        outputStream.write(printable(text).toByteArray(charset))
        return this
    }

    fun textLine(line: String = ""): EscPosBuilder {
        outputStream.write(printable(line).toByteArray(charset))
        outputStream.write(LF.toInt())
        return this
    }

    fun feedLines(count: Int = 1): EscPosBuilder {
        repeat(count.coerceAtLeast(0)) {
            outputStream.write(LF.toInt())
        }
        return this
    }

    fun divider(char: Char = '-'): EscPosBuilder {
        textLine(char.toString().repeat(totalColumns))
        return this
    }

    fun doubleDivider(): EscPosBuilder {
        textLine("=".repeat(totalColumns))
        return this
    }

    fun wrappedText(text: String): EscPosBuilder {
        val clean = printable(text).trim()
        if (clean.isEmpty()) {
            textLine()
            return this
        }

        val words = clean.split(Regex("\\s+"))
        var current = ""
        for (word in words) {
            if (word.length > totalColumns) {
                if (current.isNotBlank()) {
                    textLine(current)
                    current = ""
                }
                word.chunked(totalColumns).forEach { textLine(it) }
                continue
            }

            val candidate = if (current.isBlank()) word else "$current $word"
            if (candidate.length <= totalColumns) {
                current = candidate
            } else {
                textLine(current)
                current = word
            }
        }
        if (current.isNotBlank()) textLine(current)
        return this
    }

    fun twoColumns(leftText: String, rightText: String, fillChar: Char = ' '): EscPosBuilder {
        val left = printable(leftText)
        val right = printable(rightText)

        if (totalColumns <= 1) {
            textLine((left + right).take(totalColumns.coerceAtLeast(0)))
            return this
        }

        if (left.length + right.length + 1 <= totalColumns) {
            val spaces = totalColumns - left.length - right.length
            textLine(left + fillChar.toString().repeat(spaces) + right)
            return this
        }

        val maxLeftColumns = minOf(left.length, 12, totalColumns / 2)
        val safeLeft = when {
            maxLeftColumns <= 0 -> ""
            left.length <= maxLeftColumns -> left
            maxLeftColumns <= 2 -> left.take(maxLeftColumns)
            else -> left.take(maxLeftColumns - 2) + ".."
        }

        val remainingRightColumns = (totalColumns - safeLeft.length - 1).coerceAtLeast(0)
        val safeRight = when {
            remainingRightColumns <= 0 -> ""
            right.length <= remainingRightColumns -> right
            remainingRightColumns <= 2 -> right.take(remainingRightColumns)
            else -> right.take(remainingRightColumns - 2) + ".."
        }

        val spaces = (totalColumns - safeLeft.length - safeRight.length).coerceAtLeast(1)
        textLine((safeLeft + fillChar.toString().repeat(spaces) + safeRight).take(totalColumns))
        return this
    }

    fun printBitmap(bitmap: Bitmap, targetWidth: Int = 384): EscPosBuilder {
        val safeWidth = ((targetWidth.coerceIn(8, 384)) / 8) * 8
        val scale = safeWidth.toFloat() / bitmap.width.coerceAtLeast(1).toFloat()
        val scaledHeight = (bitmap.height * scale).toInt().coerceAtLeast(1)

        val scaledBmp = if (bitmap.width == safeWidth && bitmap.height == scaledHeight) {
            bitmap
        } else {
            Bitmap.createScaledBitmap(bitmap, safeWidth, scaledHeight, true)
        }
        val widthBytes = safeWidth / 8

        outputStream.write(GS.toInt())
        outputStream.write('v'.code)
        outputStream.write('0'.code)
        outputStream.write(0x00)
        outputStream.write(widthBytes and 0xFF)
        outputStream.write((widthBytes shr 8) and 0xFF)
        outputStream.write(scaledHeight and 0xFF)
        outputStream.write((scaledHeight shr 8) and 0xFF)

        val rowPixels = IntArray(safeWidth)
        for (y in 0 until scaledHeight) {
            scaledBmp.getPixels(rowPixels, 0, safeWidth, 0, y, safeWidth, 1)
            for (xByte in 0 until widthBytes) {
                var byteVal = 0
                for (bit in 0 until 8) {
                    val pixel = rowPixels[xByte * 8 + bit]
                    val luminance = (
                        Color.red(pixel) * 299 +
                            Color.green(pixel) * 587 +
                            Color.blue(pixel) * 114
                        ) / 1000
                    if (luminance < 155) {
                        byteVal = byteVal or (0x80 shr bit)
                    }
                }
                outputStream.write(byteVal)
            }
        }
        outputStream.write(LF.toInt())

        if (scaledBmp !== bitmap && !scaledBmp.isRecycled) {
            scaledBmp.recycle()
        }
        return this
    }

    fun printQrCode(qrData: String, moduleSize: Int = 4): EscPosBuilder {
        val dataBytes = qrData.toByteArray(Charsets.US_ASCII)
        val pL = ((dataBytes.size + 3) and 0xFF).toByte()
        val pH = (((dataBytes.size + 3) shr 8) and 0xFF).toByte()
        val safeModuleSize = moduleSize.coerceIn(1, 16)

        outputStream.write(byteArrayOf(GS, '('.code.toByte(), 'k'.code.toByte(), 0x04, 0x00, 0x31, 0x41, 0x32, 0x00))
        outputStream.write(byteArrayOf(GS, '('.code.toByte(), 'k'.code.toByte(), 0x03, 0x00, 0x31, 0x43, safeModuleSize.toByte()))
        outputStream.write(byteArrayOf(GS, '('.code.toByte(), 'k'.code.toByte(), 0x03, 0x00, 0x31, 0x45, 0x31))
        outputStream.write(byteArrayOf(GS, '('.code.toByte(), 'k'.code.toByte(), pL, pH, 0x31, 0x50, 0x30))
        outputStream.write(dataBytes)
        outputStream.write(byteArrayOf(GS, '('.code.toByte(), 'k'.code.toByte(), 0x03, 0x00, 0x31, 0x51, 0x30))
        outputStream.write(LF.toInt())
        return this
    }

    fun cutPaper(partial: Boolean = true): EscPosBuilder {
        feedLines(3)
        outputStream.write(if (partial) CMD_PARTIAL_CUT else CMD_FULL_CUT)
        return this
    }

    fun build(): ByteArray = outputStream.toByteArray()

    fun assembleNaomiReceipt(receipt: ReceiptData, logoBitmap: Bitmap? = null): ByteArray {
        if (receipt.id == BUSINESS_CARD_ID) {
            return assembleBusinessCard(receipt)
        }

        init()

        alignCenter()
        if (receipt.iconType != com.example.model.ReceiptIconType.NONE && logoBitmap != null) {
            printBitmap(logoBitmap, targetWidth = 304)
        }
        bold(true)
        doubleSize(true)
        textLine("NAOMI-CHAN(TM)")
        doubleSize(false)
        doubleHeight(true)
        textLine("PREMIUM DJ SERVICES")
        doubleHeight(false)
        bold(false)
        textLine("Official Booking & Event Receipt")
        textLine("Blackpool - North West UK - Golden Mile")
        textLine("www.naomichan-dj.com")
        divider('-')

        if (receipt.isEffectivelyFree) {
            alignCenter()
            bold(true)
            doubleHeight(true)
            textLine("*** FREE ADMISSION ***")
            doubleHeight(false)
            textLine("COMPLIMENTARY ACCESS / NO COVER")
            bold(false)
            divider('=')
        }

        alignLeft()
        twoColumns("RECEIPT NO:", receipt.id)
        twoColumns("DATE/TIME:", receipt.formattedDate())
        twoColumns("EVENT TYPE:", receipt.gigType.label)
        twoColumns("VENUE:", receipt.venueName)
        twoColumns("CLIENT:", receipt.clientName)
        twoColumns("PHONE:", receipt.clientContact)
        if (receipt.processedBy.isNotBlank()) twoColumns("STAFF:", receipt.processedBy)
        receipt.shiftId?.takeIf { it.isNotBlank() }?.let { twoColumns("SHIFT:", it.take(8).uppercase()) }
        receipt.replacesReceiptId?.takeIf { it.isNotBlank() }?.let { twoColumns("CORRECTS:", it) }
        divider('-')

        alignCenter()
        bold(true)
        textLine("--- SERVICES & MERCHANDISE ---")
        bold(false)
        alignLeft()
        twoColumns("ITEM / SERVICE", "AMOUNT")
        divider('.')

        for (item in receipt.items) {
            val title = if (item.quantity > 1) "${item.name} (x${item.quantity})" else item.name
            val amount = if (item.unitPrice == 0.0) "FREE" else ReceiptData.formatCurrency(item.total)
            twoColumns(title, amount)
        }
        divider('-')

        if (receipt.isEffectivelyFree) {
            twoColumns("ADMISSION:", "FREE (${ReceiptData.formatCurrency(0.0)})")
            twoColumns("SUBTOTAL:", ReceiptData.formatCurrency(0.0))
            twoColumns("VAT (0%):", ReceiptData.formatCurrency(0.0))
            doubleDivider()
            bold(true)
            doubleHeight(true)
            twoColumns("TOTAL DUE:", "${ReceiptData.formatCurrency(0.0)} (FREE)")
            doubleHeight(false)
            bold(false)
            twoColumns("PAYMENT:", "FREE PASS / COMP")
            twoColumns("STATUS:", if (receipt.isBufferedOffline) "PENDING SYNC" else "ADMITTED & VERIFIED")
        } else {
            twoColumns("SUBTOTAL:", ReceiptData.formatCurrency(receipt.subtotal))
            if (receipt.discountPercent > 0) {
                twoColumns("DISCOUNT (${receipt.discountPercent.toInt()}%):", "-${ReceiptData.formatCurrency(receipt.discountAmount)}")
            }
            twoColumns("VAT (${receipt.taxPercent.toInt()}%):", ReceiptData.formatCurrency(receipt.taxAmount))
            doubleDivider()
            bold(true)
            doubleHeight(true)
            twoColumns("TOTAL DUE:", ReceiptData.formatCurrency(receipt.grandTotal))
            doubleHeight(false)
            bold(false)
            twoColumns("PAYMENT:", receipt.paymentMethod.label)
            twoColumns("STATUS:", if (receipt.isBufferedOffline) "PENDING SYNC" else "VERIFIED & PAID")
        }
        divider('-')

        alignCenter()
        if (receipt.footerNotes.isNotBlank()) {
            wrappedText(receipt.footerNotes)
            feedLines(1)
        }
        val qrUrl = if (receipt.isEffectivelyFree) {
            "https://naomichan-dj.com/verify?id=${receipt.id}&type=FREE_PASS"
        } else {
            "https://naomichan-dj.com/verify?id=${receipt.id}"
        }
        printQrCode(qrUrl)
        textLine(if (receipt.isEffectivelyFree) "[ SCAN TO VALIDATE FREE ENTRY ]" else "[ SCAN TO VERIFY GIG BOOKING ]")
        textLine("Booking Ref: ${receipt.id}")
        feedLines(1)
        textLine("Naomi-Chan(TM) DJ Sound Collective")
        textLine("Thank you for rocking with us!")
        feedLines(5)
        return build()
    }

    private fun assembleBusinessCard(card: ReceiptData): ByteArray {
        val businessName = card.clientName.ifBlank { "Naomi-Chan(TM)" }
        val displayName = card.clientContact
        val role = card.gigDate
        val location = card.venueName
        val email = card.items.getOrNull(0)?.name.orEmpty()
        val phone = card.items.getOrNull(1)?.name.orEmpty()
        val website = card.footerNotes.trim()

        init()
        alignCenter()
        bold(true)
        doubleSize(true)
        wrappedText(businessName)
        doubleSize(false)

        if (displayName.isNotBlank()) {
            bold(true)
            wrappedText(displayName)
            bold(false)
        }
        if (role.isNotBlank()) wrappedText(role)

        divider('=')

        if (email.isNotBlank()) wrappedText(email)
        if (phone.isNotBlank()) wrappedText(phone)
        if (website.isNotBlank()) wrappedText(website)
        if (location.isNotBlank()) wrappedText(location)

        if (website.isNotBlank()) {
            feedLines(1)
            val qrTarget = if (website.startsWith("http://") || website.startsWith("https://")) {
                website
            } else {
                "https://$website"
            }
            printQrCode(qrTarget, moduleSize = 5)
            bold(true)
            textLine("SCAN TO CONNECT")
            bold(false)
        }

        feedLines(1)
        textLine("Naomi-Chan(TM) Official Network")
        feedLines(5)
        return build()
    }
}
