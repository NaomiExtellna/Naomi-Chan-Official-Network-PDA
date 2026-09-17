package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.model.ReceiptData
import com.example.model.ReceiptItem
import com.example.network.CatalogClient
import com.example.network.CatalogItem
import com.example.network.CatalogLookupResult
import com.example.ui.PosViewModel
import com.example.ui.theme.NaomiBorder
import com.example.ui.theme.NaomiDarkBg
import com.example.ui.theme.NaomiOrange
import com.example.ui.theme.NaomiRed
import com.example.ui.theme.NaomiSuccess
import com.example.ui.theme.NaomiSurface
import com.example.ui.theme.NaomiTextPrimary
import com.example.ui.theme.NaomiTextSecondary
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

@Composable
fun BarcodeScannerScreen(
    viewModel: PosViewModel,
    onOpenLedger: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val receipts by viewModel.allReceipts.collectAsState()
    val currentReceipt by viewModel.currentReceipt.collectAsState()
    val gatewayUrl by viewModel.wirelessServerUrl.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var lastValue by remember { mutableStateOf("") }
    var lastFormat by remember { mutableStateOf("") }
    var manualValue by remember { mutableStateOf("") }
    var heldCameraCode by remember { mutableStateOf<String?>(null) }
    var catalogueItem by remember { mutableStateOf<CatalogItem?>(null) }
    var catalogueMessage by remember { mutableStateOf<String?>(null) }
    var catalogueBusy by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    val lookupId = remember(lastValue) { extractReceiptId(lastValue) }
    val matchedReceipt = remember(lookupId, receipts) {
        lookupId.takeIf { it.isNotBlank() }?.let { id ->
            receipts.firstOrNull { it.id.equals(id, ignoreCase = true) }
        }
    }

    val processCode: (String, String) -> Unit = { rawValue, format ->
        val value = rawValue.trim()
        if (value.isNotBlank()) {
            lastValue = value
            lastFormat = format
            catalogueItem = null
            catalogueMessage = null

            val receiptId = extractReceiptId(value)
            val localReceipt = receipts.firstOrNull { it.id.equals(receiptId, ignoreCase = true) }
            viewModel.logAction(
                action = "BARCODE_SCANNED",
                target = receiptId,
                details = "format=$format"
            )

            if (localReceipt == null) {
                catalogueBusy = true
                scope.launch {
                    when (val result = CatalogClient.lookupBarcode(gatewayUrl, value)) {
                        is CatalogLookupResult.Found -> {
                            val item = result.item
                            val existing = viewModel.currentReceipt.value.items.firstOrNull { it.id == item.id }
                            if (existing != null) {
                                viewModel.removeLineItem(existing.id)
                                viewModel.addLineItem(existing.copy(quantity = existing.quantity + 1, unitPrice = item.price))
                                catalogueMessage = "Quantity increased on the current receipt"
                            } else {
                                viewModel.addLineItem(
                                    ReceiptItem(
                                        id = item.id,
                                        name = item.name,
                                        quantity = 1,
                                        unitPrice = item.price
                                    )
                                )
                                catalogueMessage = "Added to the current receipt"
                            }
                            catalogueItem = item
                            viewModel.logAction(
                                action = "CATALOG_ITEM_SCANNED",
                                target = item.id,
                                details = "barcode=${item.barcode}; event=${item.eventId.ifBlank { "GLOBAL" }}"
                            )
                        }
                        is CatalogLookupResult.NotFound -> {
                            catalogueMessage = "No active web catalogue item is mapped to this barcode."
                        }
                        is CatalogLookupResult.Error -> {
                            catalogueMessage = "Gateway lookup failed: ${result.message}"
                        }
                    }
                    catalogueBusy = false
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NaomiDarkBg)
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "EVENT SCANNER",
                    color = NaomiOrange,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp
                )
                Text(
                    text = "Scan to sell or verify",
                    color = NaomiTextPrimary,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Tickets, admission, VIP upgrades, merch, services and receipt QR codes.",
                    color = NaomiTextSecondary,
                    fontSize = 11.sp
                )
            }
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = if (hasCameraPermission) NaomiSuccess.copy(alpha = 0.16f) else NaomiOrange.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, if (hasCameraPermission) NaomiSuccess.copy(alpha = 0.45f) else NaomiOrange.copy(alpha = 0.45f))
            ) {
                Text(
                    text = if (hasCameraPermission) "CAMERA LIVE" else "CAMERA OFF",
                    color = if (hasCameraPermission) NaomiSuccess else NaomiOrange,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = NaomiSurface),
            border = BorderStroke(1.dp, NaomiBorder),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (hasCameraPermission) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(285.dp)
                        .background(Color.Black)
                ) {
                    BarcodeCameraPreview { value, format ->
                        if (value == null) {
                            heldCameraCode = null
                        } else if (heldCameraCode != value) {
                            heldCameraCode = value
                            processCode(value, format)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth(0.78f)
                            .height(150.dp)
                            .border(2.dp, NaomiOrange, RoundedCornerShape(18.dp))
                    )

                    Surface(
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp),
                        color = Color.Black.copy(alpha = 0.72f),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = NaomiOrange, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.size(6.dp))
                            Text("Scan once • move code away to re-arm", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = NaomiOrange, modifier = Modifier.size(40.dp))
                    Text("Camera access required", color = NaomiTextPrimary, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    Text(
                        "The scanner uses the rear camera only while this screen is open.",
                        color = NaomiTextSecondary,
                        fontSize = 11.sp
                    )
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(containerColor = NaomiRed)
                    ) {
                        Text("Enable camera", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (lastValue.isNotBlank()) {
            ScanResultCard(
                rawValue = lastValue,
                format = lastFormat,
                receipt = matchedReceipt,
                catalogueItem = catalogueItem,
                catalogueMessage = catalogueMessage,
                catalogueBusy = catalogueBusy,
                quantity = catalogueItem?.let { item -> currentReceipt.items.firstOrNull { it.id == item.id }?.quantity ?: 0 } ?: 0,
                onOpenLedger = onOpenLedger
            )
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = NaomiSurface),
            border = BorderStroke(1.dp, NaomiBorder),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("MANUAL BARCODE / RECEIPT", color = NaomiOrange, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                OutlinedTextField(
                    value = manualValue,
                    onValueChange = { manualValue = it },
                    label = { Text("Scan value or receipt reference") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = NaomiTextPrimary,
                        unfocusedTextColor = NaomiTextPrimary,
                        focusedBorderColor = NaomiOrange,
                        unfocusedBorderColor = NaomiBorder,
                        focusedLabelColor = NaomiOrange,
                        unfocusedLabelColor = NaomiTextSecondary
                    )
                )
                Button(
                    onClick = {
                        if (manualValue.isNotBlank()) {
                            processCode(manualValue, "MANUAL")
                            manualValue = ""
                        }
                    },
                    enabled = manualValue.isNotBlank() && !catalogueBusy,
                    colors = ButtonDefaults.buttonColors(containerColor = NaomiRed),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Resolve code", fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun ScanResultCard(
    rawValue: String,
    format: String,
    receipt: ReceiptData?,
    catalogueItem: CatalogItem?,
    catalogueMessage: String?,
    catalogueBusy: Boolean,
    quantity: Int,
    onOpenLedger: () -> Unit
) {
    val found = receipt != null || catalogueItem != null
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (found) NaomiSuccess.copy(alpha = 0.10f) else NaomiSurface
        ),
        border = BorderStroke(1.dp, if (found) NaomiSuccess.copy(alpha = 0.55f) else NaomiBorder),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (found) Icons.Default.CheckCircle else Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    tint = if (found) NaomiSuccess else NaomiOrange,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Column {
                    Text(
                        text = when {
                            receipt != null -> "Verified receipt"
                            catalogueItem != null -> "Catalogue item added"
                            catalogueBusy -> "Checking event catalogue…"
                            else -> "Code captured"
                        },
                        color = NaomiTextPrimary,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp
                    )
                    Text(format.ifBlank { "UNKNOWN FORMAT" }, color = NaomiTextSecondary, fontSize = 9.sp)
                }
            }

            Text(
                text = rawValue,
                color = NaomiTextSecondary,
                fontSize = 10.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            if (receipt != null) {
                ResultLine("Receipt", receipt.id)
                ResultLine("Venue", receipt.venueName)
                ResultLine("Total", ReceiptData.formatCurrency(receipt.grandTotal))
                ResultLine("Status", receipt.receiptStatus)
                if (receipt.processedBy.isNotBlank()) ResultLine("Processed by", receipt.processedBy)

                OutlinedButton(onClick = onOpenLedger, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("Open receipt ledger")
                }
            } else if (catalogueItem != null) {
                ResultLine("Item", catalogueItem.name)
                ResultLine("Type", catalogueItem.itemType.replace('_', ' '))
                ResultLine("Category", catalogueItem.category)
                ResultLine("Price", ReceiptData.formatCurrency(catalogueItem.price))
                ResultLine("Receipt quantity", quantity.toString())
                ResultLine("Event", catalogueItem.eventId.ifBlank { "All events / global" })
                if (catalogueItem.description.isNotBlank()) {
                    Text(catalogueItem.description, color = NaomiTextSecondary, fontSize = 10.sp)
                }
                if (!catalogueMessage.isNullOrBlank()) {
                    Text(catalogueMessage, color = NaomiSuccess, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            } else if (!catalogueMessage.isNullOrBlank()) {
                Text(catalogueMessage, color = NaomiTextSecondary, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun ResultLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = NaomiTextSecondary, fontSize = 10.sp)
        Text(value, color = NaomiTextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun BarcodeCameraPreview(onBarcode: (String?, String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnBarcode by rememberUpdatedState(onBarcode)
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    val analyzerExecutor = remember { Executors.newSingleThreadExecutor() }
    val scanner = remember {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_CODE_93,
                Barcode.FORMAT_CODABAR,
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_ITF,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_DATA_MATRIX,
                Barcode.FORMAT_PDF417,
                Barcode.FORMAT_AZTEC
            )
            .build()
        BarcodeScanning.getClient(options)
    }

    DisposableEffect(lifecycleOwner) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        val listener = Runnable {
            runCatching {
                val cameraProvider = providerFuture.get()
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                val analysis = ImageAnalysis.Builder()
                    .setTargetResolution(Size(1280, 720))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                analysis.setAnalyzer(analyzerExecutor) { imageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage == null) {
                        currentOnBarcode(null, "")
                        imageProxy.close()
                        return@setAnalyzer
                    }
                    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                    scanner.process(image)
                        .addOnSuccessListener { barcodes ->
                            val barcode = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }
                            val value = barcode?.rawValue
                            if (value.isNullOrBlank()) {
                                currentOnBarcode(null, "")
                            } else {
                                currentOnBarcode(value, barcodeFormatLabel(barcode.format))
                            }
                        }
                        .addOnFailureListener { currentOnBarcode(null, "") }
                        .addOnCompleteListener { imageProxy.close() }
                }

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis
                )
            }
        }
        providerFuture.addListener(listener, ContextCompat.getMainExecutor(context))

        onDispose {
            if (providerFuture.isDone) {
                runCatching { providerFuture.get().unbindAll() }
            }
            runCatching { scanner.close() }
            analyzerExecutor.shutdown()
        }
    }

    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
}

private fun extractReceiptId(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isBlank()) return ""
    return runCatching {
        Uri.parse(trimmed).getQueryParameter("id")?.takeIf { it.isNotBlank() }
    }.getOrNull() ?: trimmed
}

private fun barcodeFormatLabel(format: Int): String = when (format) {
    Barcode.FORMAT_QR_CODE -> "QR CODE"
    Barcode.FORMAT_CODE_128 -> "CODE 128"
    Barcode.FORMAT_CODE_39 -> "CODE 39"
    Barcode.FORMAT_CODE_93 -> "CODE 93"
    Barcode.FORMAT_CODABAR -> "CODABAR"
    Barcode.FORMAT_EAN_13 -> "EAN-13"
    Barcode.FORMAT_EAN_8 -> "EAN-8"
    Barcode.FORMAT_ITF -> "ITF"
    Barcode.FORMAT_UPC_A -> "UPC-A"
    Barcode.FORMAT_UPC_E -> "UPC-E"
    Barcode.FORMAT_DATA_MATRIX -> "DATA MATRIX"
    Barcode.FORMAT_PDF417 -> "PDF417"
    Barcode.FORMAT_AZTEC -> "AZTEC"
    else -> "BARCODE"
}
