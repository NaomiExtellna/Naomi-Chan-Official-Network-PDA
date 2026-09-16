package com.example

import com.example.model.ReceiptData
import com.example.model.ReceiptItem
import com.example.printer.EscPosBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testReceiptCalculations() {
        val items = listOf(
            ReceiptItem(name = "Main Club Set", quantity = 1, unitPrice = 450.0),
            ReceiptItem(name = "Wireless Mic", quantity = 2, unitPrice = 25.0)
        )
        val receipt = ReceiptData(
            id = "NC-TEST-01",
            clientName = "DJ test",
            venueName = "Womb Shibuya",
            items = items,
            taxPercent = 10.0,
            discountPercent = 0.0
        )

        assertEquals(500.0, receipt.subtotal, 0.001)
        assertEquals(50.0, receipt.taxAmount, 0.001)
        assertEquals(550.0, receipt.grandTotal, 0.001)
    }

    @Test
    fun testEscPosReceiptBuilder() {
        val builder = EscPosBuilder()
            .init()
            .alignCenter()
            .bold(true)
            .textLine("NAOMI-CHAN™")
            .bold(false)
            .textLine("DJ SERVICES")
            .alignLeft()
            .twoColumns("CLIENT:", "Elena Rostova")
            .twoColumns("VENUE:", "Berghain Berlin")
            .feedLines(3)
            .cutPaper(partial = true)

        val bytes = builder.build()
        assertNotNull(bytes)
        assertTrue(bytes.isNotEmpty())

        // Check ESC @ init sequence (0x1B, 0x40)
        assertEquals(0x1B.toByte(), bytes[0])
        assertEquals(0x40.toByte(), bytes[1])
    }

    @Test
    fun testTwoColumns32CharFormatting() {
        val builder = EscPosBuilder(totalColumns = 32)
        builder.twoColumns("TOTAL DUE:", "$1,250.00")
        val bytes = builder.build()
        val text = String(bytes)

        assertTrue(text.contains("TOTAL DUE:"))
        assertTrue(text.contains("$1,250.00"))
    }

    @Test
    fun testFreeEventCalculation() {
        val freeReceipt = ReceiptData(
            id = "NC-FREE-99",
            clientName = "Community Festival",
            venueName = "Civic Park",
            isFreeEvent = true,
            packageTier = com.example.model.PackageTier.FREE_ADMISSION,
            items = listOf(ReceiptItem(name = "Free Stage Access", quantity = 1, unitPrice = 0.0))
        )

        assertTrue(freeReceipt.isEffectivelyFree)
        assertEquals(0.0, freeReceipt.grandTotal, 0.001)
        assertEquals(0.0, freeReceipt.taxAmount, 0.001)
    }

    @Test
    fun testFreeEventEscPosOutput() {
        val builder = EscPosBuilder()
            .init()
            .alignCenter()
            .bold(true)
            .textLine("*** FREE ADMISSION PASS ***")
            .textLine("COMPLIMENTARY ACCESS")
            .bold(false)
            .twoColumns("ADMISSION:", "FREE ($0.00)")
            .build()

        val text = String(builder)
        assertTrue(text.contains("FREE ADMISSION PASS"))
        assertTrue(text.contains("COMPLIMENTARY ACCESS"))
        assertTrue(text.contains("FREE ($0.00)"))
    }
}

