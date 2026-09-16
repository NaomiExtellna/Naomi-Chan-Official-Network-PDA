package com.example.printer

import android.graphics.Bitmap
import android.graphics.Color
import com.example.model.ReceiptData
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset

class EscPosBuilder(private val totalColumns: Int = 32) {

    private val outputStream = ByteArrayOutputStream()
    private val charset: Charset = Charset.forName("GBK") // or UTF-8 / CP437 standard thermal

    companion object {
        val ESC: Byte = 0x1B
        val GS: Byte = 0x1D
        val LF: Byte = 0x0A

        // Commands
        val CMD_INIT = byteArrayOf(ESC, '@'.code.toByte())
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

    fun init(): EscPosBuilder {
        outputStream.write(CMD_INIT)
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
        outputStream.write(text.toByteArray(charset))
        return this
    }

    fun textLine(line: String = ""): EscPosBuilder {
        outputStream.write(line.toByteArray(charset))
        outputStream.write(LF.toInt())
        return this
    }

    fun feedLines(count: Int = 1): EscPosBuilder {
        for (i in 0 until count) {
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

    /**
     * Formats two columns with left alignment for col1 and right alignment for col2.
     * Guaranteed to fit totalColumns (e.g. 32 chars for 58mm paper).
     */
    fun twoColumns(leftText: String, rightText: String, fillChar: Char = ' '): EscPosBuilder {
        val maxLeft = totalColumns - rightText.length - 1
        val safeLeft = if (leftText.length > maxLeft && maxLeft > 3) {
            leftText.substring(0, maxLeft - 2) + ".."
        } else {
            leftText
        }
        val spacesCount = (totalColumns - safeLeft.length - rightText.length).coerceAtLeast(1)
        val combined = safeLeft + fillChar.toString().repeat(spacesCount) + rightText
        textLine(combined)
        return this
    }

    /**
     * Converts an Android Bitmap into standard ESC/POS GS v 0 raster bit image data.
     * Fully optimized for <1GB RAM devices by streaming row-by-row into the output stream
     * using a single row pixel buffer (rather than allocating giant full-frame arrays),
     * and promptly recycling temporary scaled bitmaps.
     */
    fun printBitmap(bitmap: Bitmap, targetWidth: Int = 384): EscPosBuilder {
        val safeWidth = (targetWidth / 8) * 8
        val scale = safeWidth.toFloat() / bitmap.width.coerceAtLeast(1).toFloat()
        val scaledHeight = (bitmap.height * scale).toInt().coerceAtLeast(1)

        val scaledBmp = if (bitmap.width == safeWidth && bitmap.height == scaledHeight) {
            bitmap
        } else {
            Bitmap.createScaledBitmap(bitmap, safeWidth, scaledHeight, true)
        }
        val widthBytes = safeWidth / 8

        // Header: GS v 0 m xL xH yL yH
        outputStream.write(GS.toInt())
        outputStream.write('v'.code)
        outputStream.write('0'.code)
        outputStream.write(0x00) // mode 0

        outputStream.write(widthBytes and 0xFF)
        outputStream.write((widthBytes shr 8) and 0xFF)
        outputStream.write(scaledHeight and 0xFF)
        outputStream.write((scaledHeight shr 8) and 0xFF)

        // Memory-lean: Only allocate 1 row of pixels buffer instead of the entire frame!
        val rowPixels = IntArray(safeWidth)

        for (y in 0 until scaledHeight) {
            scaledBmp.getPixels(rowPixels, 0, safeWidth, 0, y, safeWidth, 1)
            for (xByte in 0 until widthBytes) {
                var byteVal = 0
                for (b in 0 until 8) {
                    val x = xByte * 8 + b
                    val pixel = rowPixels[x]
                    // Fast integer luminance: (299*R + 587*G + 114*B) / 1000
                    val r = Color.red(pixel)
                    val g = Color.green(pixel)
                    val bVal = Color.blue(pixel)
                    val luminance = (r * 299 + g * 587 + bVal * 114) / 1000

                    // Dark pixels burn black on thermal paper
                    if (luminance < 155) {
                        byteVal = byteVal or (0x80 shr b)
                    }
                }
                outputStream.write(byteVal)
            }
        }
        outputStream.write(LF.toInt())

        // Promptly reclaim scaled memory on low-RAM POS terminals
        if (scaledBmp !== bitmap && !scaledBmp.isRecycled) {
            scaledBmp.recycle()
        }

        return this
    }

    /**
     * Native QR Code command (ESC/POS Model 2)
     */
    fun printQrCode(qrData: String, moduleSize: Int = 4): EscPosBuilder {
        val dataBytes = qrData.toByteArray(charset)
        val pL = ((dataBytes.size + 3) and 0xFF).toByte()
        val pH = (((dataBytes.size + 3) shr 8) and 0xFF).toByte()

        // 1. QR Code: Set model
        outputStream.write(byteArrayOf(GS, '('.code.toByte(), 'k'.code.toByte(), 0x04, 0x00, 0x31, 0x41, 0x32, 0x00))
        // 2. QR Code: Set module size
        outputStream.write(byteArrayOf(GS, '('.code.toByte(), 'k'.code.toByte(), 0x03, 0x00, 0x31, 0x43, moduleSize.toByte()))
        // 3. QR Code: Set error correction (Level M = 0x31)
        outputStream.write(byteArrayOf(GS, '('.code.toByte(), 'k'.code.toByte(), 0x03, 0x00, 0x31, 0x45, 0x31))
        // 4. QR Code: Store data in symbol storage area
        outputStream.write(byteArrayOf(GS, '('.code.toByte(), 'k'.code.toByte(), pL, pH, 0x31, 0x50, 0x30))
        outputStream.write(dataBytes)
        // 5. QR Code: Print symbol
        outputStream.write(byteArrayOf(GS, '('.code.toByte(), 'k'.code.toByte(), 0x03, 0x00, 0x31, 0x51, 0x30))
        outputStream.write(LF.toInt())
        return this
    }

    fun cutPaper(partial: Boolean = true): EscPosBuilder {
        feedLines(3)
        outputStream.write(if (partial) CMD_PARTIAL_CUT else CMD_FULL_CUT)
        return this
    }

    fun build(): ByteArray {
        return outputStream.toByteArray()
    }

    /**
     * Helper to assemble a complete official Naomi-Chan™ DJ Receipt in standard ESC/POS format.
     */
    fun assembleNaomiReceipt(receipt: ReceiptData, logoBitmap: Bitmap? = null): ByteArray {
        init()

        // 1. Brand Logo & Header
        alignCenter()
        if (receipt.iconType != com.example.model.ReceiptIconType.NONE && logoBitmap != null) {
            printBitmap(logoBitmap, targetWidth = 304)
        }
        bold(true)
        doubleSize(true)
        textLine("NAOMI-CHAN™")
        doubleSize(false)
        doubleHeight(true)
        textLine("PREMIUM DJ SERVICES")
        doubleHeight(false)
        bold(false)
        textLine("Official Booking & Event Receipt")
        textLine("Blackpool • North West UK • Golden Mile")
        textLine("www.naomichan-dj.com")
        divider('-')

        // If this is a free event / free admission, print prominent free pass header
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

        // 2. Booking Metadata
        alignLeft()
        twoColumns("RECEIPT NO:", receipt.id)
        twoColumns("DATE/TIME:", receipt.formattedDate())
        twoColumns("EVENT TYPE:", receipt.gigType.label)
        twoColumns("VENUE:", receipt.venueName)
        twoColumns("CLIENT:", receipt.clientName)
        twoColumns("PHONE:", receipt.clientContact)
        divider('-')

        // 3. Itemized List
        alignCenter()
        bold(true)
        textLine("--- SERVICES & MERCHANDISE ---")
        bold(false)
        alignLeft()
        twoColumns("ITEM / SERVICE", "AMOUNT")
        divider('.')

        for (item in receipt.items) {
            val title = if (item.quantity > 1) "${item.name} (x${item.quantity})" else item.name
            val amountStr = if (item.unitPrice == 0.0) "FREE" else ReceiptData.formatCurrency(item.total)
            twoColumns(title, amountStr)
        }
        divider('-')

        // 4. Financial Calculations
        if (receipt.isEffectivelyFree) {
            twoColumns("ADMISSION TICKET:", "FREE (${ReceiptData.formatCurrency(0.0)})")
            twoColumns("SUBTOTAL:", ReceiptData.formatCurrency(0.0))
            twoColumns("VAT (0%):", ReceiptData.formatCurrency(0.0))
            doubleDivider()

            bold(true)
            doubleHeight(true)
            twoColumns("TOTAL DUE:", "${ReceiptData.formatCurrency(0.0)} (FREE)")
            doubleHeight(false)
            bold(false)
            twoColumns("PAYMENT METHOD:", "FREE PASS / COMP")
            twoColumns("STATUS:", if (receipt.isBufferedOffline) "BUFFERED (OFFLINE)" else "ADMITTED & VERIFIED")
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
            twoColumns("PAYMENT METHOD:", receipt.paymentMethod.label)
            twoColumns("STATUS:", if (receipt.isBufferedOffline) "BUFFERED (OFFLINE)" else "VERIFIED & PAID")
        }
        divider('-')

        // 6. Footer Notes & Verification QR
        alignCenter()
        if (receipt.footerNotes.isNotBlank()) {
            textLine(receipt.footerNotes)
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
        textLine("Naomi-Chan™ DJ Sound Collective")
        textLine("Thank you for rocking with us!")

        // 7. Feed & Cut
        feedLines(3)
        cutPaper(partial = true)

        return build()
    }
}
