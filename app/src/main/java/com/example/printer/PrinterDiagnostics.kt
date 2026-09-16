package com.example.printer

import com.example.model.GigType
import com.example.model.PackageTier
import com.example.model.PaymentMethod
import com.example.model.PrinterChannel
import com.example.model.PrinterStatus
import com.example.model.ReceiptData
import com.example.model.ReceiptIconType
import com.example.model.ReceiptItem

suspend fun printPrinterDiagnosticSlip(
    printerManager: UnifiedPrinterManager,
    channel: PrinterChannel,
    status: PrinterStatus,
    operatorName: String
): PrintResult {
    val diagnostic = ReceiptData(
        id = "TEST-${System.currentTimeMillis().toString().takeLast(6)}",
        clientName = "PRINTER DIAGNOSTIC",
        clientContact = status.serialNumber,
        gigType = GigType.CORPORATE,
        venueName = status.deviceName,
        gigDate = "Hardware test",
        packageTier = PackageTier.FREE_ADMISSION,
        items = listOf(
            ReceiptItem(name = "Printer channel: ${channel.displayName}", unitPrice = 0.0),
            ReceiptItem(name = "Paper width: ${status.paperWidthMm}mm", unitPrice = 0.0),
            ReceiptItem(name = "Status code: ${status.statusCode ?: -1}", unitPrice = 0.0)
        ),
        paymentMethod = PaymentMethod.FREE_PASS,
        footerNotes = "DIAGNOSTIC ONLY - NOT A TRANSACTION. If this text and QR print cleanly, the ESC/POS path is operational.",
        taxPercent = 0.0,
        isFreeEvent = true,
        iconType = ReceiptIconType.NONE,
        processedBy = operatorName
    )

    // This deliberately bypasses ReceiptRepository, so diagnostics never appear in
    // the transaction ledger, shift totals or gateway sync queue.
    return printerManager.printReceipt(diagnostic, channel, null)
}
