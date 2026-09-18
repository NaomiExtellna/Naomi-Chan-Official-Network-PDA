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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

private val SCANNER_ANALYSIS_SIZE = Size(960, 540)
private enum class ScannerMode { CAMERA, MANUAL }

@Composable
fun BarcodeScannerScreen(
    viewModel: PosViewModel,
    onOpenLedger: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val receipts by viewModel.allReceipts.collectAsStateWithLifecycle()
    val currentReceipt by viewModel.currentReceipt.collectAsStateWithLifecycle()
    val gatewayUrl by viewModel.wirelessServerUrl.collectAsStateWithLifecycle()

    var mode by remember { mutableStateOf(ScannerMode.CAMERA) }
    var hasCameraPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var lastValue by remember { mutableStateOf("") }
    var lastFormat by remember { mutableStateOf("") }
    var manualValue by remember { mutableStateOf("") }
    var heldCameraCode by remember { mutableStateOf<String?>(null) }
    var catalogueItem by remember { mutableStateOf<CatalogItem?>(null) }
    var catalogueMessage by remember { mutableStateOf<String?>(null) }
    var catalogueBusy by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCameraPermission = granted
    }

    val lookupId = remember(lastValue) { extractReceiptId(lastValue) }
    val matchedReceipt = remember(lookupId, receipts) {
        lookupId.takeIf { it.isNotBlank() }?.let { id -> receipts.firstOrNull { it.id.equals(id, ignoreCase = true) } }
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
            viewModel.logAction("BARCODE_SCANNED", receiptId, "format=$format")

            if (localReceipt == null && !catalogueBusy) {
                catalogueBusy = true
                scope.launch {
                    try {
                        when (val result = CatalogClient.lookupBarcode(gatewayUrl, value)) {
                            is CatalogLookupResult.Found -> {
                                val item = result.item
                                val existing = viewModel.currentReceipt.value.items.firstOrNull { it.id == item.id }
                                if (existing != null) {
                                    viewModel.removeLineItem(existing.id)
                                    viewModel.addLineItem(existing.copy(quantity = existing.quantity + 1, unitPrice = item.price))
                                    catalogueMessage = "Quantity increased on current sale"
                                } else {
                                    viewModel.addLineItem(ReceiptItem(id = item.id, name = item.name, quantity = 1, unitPrice = item.price))
                                    catalogueMessage = "Added to current sale"
                                }
                                catalogueItem = item
                                viewModel.logAction("CATALOG_ITEM_SCANNED", item.id, "barcode=${item.barcode}; event=${item.eventId.ifBlank { "GLOBAL" }}")
                            }
                            is CatalogLookupResult.NotFound -> catalogueMessage = "No active catalogue item is mapped to this code."
                            is CatalogLookupResult.Error -> catalogueMessage = "Gateway lookup failed: ${result.message}"
                        }
                    } finally {
                        catalogueBusy = false
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(NaomiDarkBg).padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("EVENT SCANNER", color = NaomiOrange, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                Text("Scan to sell or verify", color = NaomiTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Black)
            }
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (hasCameraPermission) NaomiSuccess.copy(alpha = 0.12f) else NaomiOrange.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, if (hasCameraPermission) NaomiSuccess.copy(alpha = 0.35f) else NaomiOrange.copy(alpha = 0.35f))
            ) {
                Text(
                    if (hasCameraPermission) "CAMERA LIVE" else "CAMERA OFF",
                    color = if (hasCameraPermission) NaomiSuccess else NaomiOrange,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = mode == ScannerMode.CAMERA,
                onClick = { mode = ScannerMode.CAMERA },
                label = { Text("Camera", fontSize = 9.sp) },
                leadingIcon = { Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(14.dp)) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = NaomiRed, selectedLabelColor = Color.White, selectedLeadingIconColor = Color.White),
                modifier = Modifier.weight(1f).height(40.dp)
            )
            FilterChip(
                selected = mode == ScannerMode.MANUAL,
                onClick = { mode = ScannerMode.MANUAL },
                label = { Text("Manual code", fontSize = 9.sp) },
                leadingIcon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(14.dp)) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = NaomiRed, selectedLabelColor = Color.White, selectedLeadingIconColor = Color.White),
                modifier = Modifier.weight(1f).height(40.dp)
            )
        }

        if (mode == ScannerMode.CAMERA) {
            Card(
                colors = CardDefaults.cardColors(containerColor = NaomiSurface),
                border = BorderStroke(1.dp, NaomiBorder),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                if (hasCameraPermission) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
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
                                .fillMaxWidth(0.76f)
                                .height(145.dp)
                                .border(2.dp, NaomiOrange, RoundedCornerShape(10.dp))
                        )
                        Surface(
                            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp),
                            color = Color.Black.copy(alpha = 0.70f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Scan once · move code away to re-arm", color = Color.White, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = NaomiOrange, modifier = Modifier.size(36.dp))
                        Text("Camera access required", color = NaomiTextPrimary, fontWeight = FontWeight.Black, fontSize = 13.sp)
                        Text("Rear camera is used only while this screen is open.", color = NaomiTextSecondary, fontSize = 9.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }, colors = ButtonDefaults.buttonColors(containerColor = NaomiRed)) {
                            Text("Enable camera", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (lastValue.isNotBlank()) {
                CompactScanResult(
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
        } else {
            Card(
                colors = CardDefaults.cardColors(containerColor = NaomiSurface),
                border = BorderStroke(1.dp, NaomiBorder),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("MANUAL BARCODE / RECEIPT", color = NaomiOrange, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    OutlinedTextField(
                        value = manualValue,
                        onValueChange = { manualValue = it },
                        label = { Text("Code or receipt reference") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
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
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                    ) { Text("Resolve code", fontSize = 9.sp, fontWeight = FontWeight.Black) }
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (lastValue.isBlank()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = NaomiSurface),
                        border = BorderStroke(1.dp, NaomiBorder),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = NaomiTextSecondary, modifier = Modifier.size(30.dp))
                            Text("Enter a code above", color = NaomiTextSecondary, fontSize = 10.sp)
                        }
                    }
                } else {
                    CompactScanResult(
                        rawValue = lastValue,
                        format = lastFormat,
                        receipt = matchedReceipt,
                        catalogueItem = catalogueItem,
                        catalogueMessage = catalogueMessage,
                        catalogueBusy = catalogueBusy,
                        quantity = catalogueItem?.let { item -> currentReceipt.items.firstOrNull { it.id == item.id }?.quantity ?: 0 } ?: 0,
                        onOpenLedger = onOpenLedger,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactScanResult(
    rawValue: String,
    format: String,
    receipt: ReceiptData?,
    catalogueItem: CatalogItem?,
    catalogueMessage: String?,
    catalogueBusy: Boolean,
    quantity: Int,
    onOpenLedger: () -> Unit,
    modifier: Modifier = Modifier
) {
    val found = receipt != null || catalogueItem != null
    Card(
        colors = CardDefaults.cardColors(containerColor = if (found) NaomiSuccess.copy(alpha = 0.08f) else NaomiSurface),
        border = BorderStroke(1.dp, if (found) NaomiSuccess.copy(alpha = 0.45f) else NaomiBorder),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (found) Icons.Default.CheckCircle else Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    tint = if (found) NaomiSuccess else NaomiOrange,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.size(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        when {
                            receipt != null -> "Verified receipt"
                            catalogueItem != null -> "Catalogue item added"
                            catalogueBusy -> "Checking catalogue…"
                            else -> "Code captured"
                        },
                        color = NaomiTextPrimary,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp
                    )
                    Text("${format.ifBlank { "UNKNOWN" }} · $rawValue", color = NaomiTextSecondary, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            when {
                receipt != null -> {
                    ResultLine("Receipt", receipt.id)
                    ResultLine("Venue", receipt.venueName)
                    ResultLine("Total", ReceiptData.formatCurrency(receipt.grandTotal))
                    OutlinedButton(onClick = onOpenLedger, modifier = Modifier.fillMaxWidth().height(40.dp)) {
                        Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.size(3.dp))
                        Text("Open Activity", fontSize = 9.sp)
                    }
                }
                catalogueItem != null -> {
                    ResultLine("Item", catalogueItem.name)
                    ResultLine("Price", ReceiptData.formatCurrency(catalogueItem.price))
                    ResultLine("Qty", quantity.toString())
                    if (!catalogueMessage.isNullOrBlank()) Text(catalogueMessage, color = NaomiSuccess, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
                !catalogueMessage.isNullOrBlank() -> Text(catalogueMessage, color = NaomiTextSecondary, fontSize = 9.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun ResultLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = NaomiTextSecondary, fontSize = 9.sp)
        Text(value, color = NaomiTextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
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
        var lastReportedValue: String? = null
        val listener = Runnable {
            runCatching {
                val cameraProvider = providerFuture.get()
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                val analysis = ImageAnalysis.Builder()
                    .setTargetResolution(SCANNER_ANALYSIS_SIZE)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                analysis.setAnalyzer(analyzerExecutor) { imageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage == null) {
                        if (lastReportedValue != null) {
                            lastReportedValue = null
                            currentOnBarcode(null, "")
                        }
                        imageProxy.close()
                        return@setAnalyzer
                    }

                    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                    scanner.process(image)
                        .addOnSuccessListener { barcodes ->
                            val barcode = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }
                            val value = barcode?.rawValue
                            when {
                                value.isNullOrBlank() && lastReportedValue != null -> {
                                    lastReportedValue = null
                                    currentOnBarcode(null, "")
                                }
                                !value.isNullOrBlank() && value != lastReportedValue -> {
                                    lastReportedValue = value
                                    currentOnBarcode(value, barcodeFormatLabel(barcode.format))
                                }
                            }
                        }
                        .addOnFailureListener {
                            if (lastReportedValue != null) {
                                lastReportedValue = null
                                currentOnBarcode(null, "")
                            }
                        }
                        .addOnCompleteListener { imageProxy.close() }
                }

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            }.onFailure { LogScannerFailure.log(it) }
        }
        providerFuture.addListener(listener, ContextCompat.getMainExecutor(context))

        onDispose {
            if (providerFuture.isDone) runCatching { providerFuture.get().unbindAll() }
            runCatching { scanner.close() }
            analyzerExecutor.shutdown()
        }
    }

    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
}

private object LogScannerFailure {
    fun log(error: Throwable) {
        android.util.Log.w("NaomiBarcodeScanner", "Unable to bind camera: ${error.message}")
    }
}

private fun extractReceiptId(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isBlank()) return ""
    return runCatching { Uri.parse(trimmed).getQueryParameter("id")?.takeIf { it.isNotBlank() } }.getOrNull() ?: trimmed
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