package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.WarehouseScanKind
import com.example.ui.WarehouseScanResult
import com.example.ui.theme.NaomiBorder
import com.example.ui.theme.NaomiDarkBg
import com.example.ui.theme.NaomiError
import com.example.ui.theme.NaomiOrange
import com.example.ui.theme.NaomiSuccess
import com.example.ui.theme.NaomiSurface
import com.example.ui.theme.NaomiTextPrimary
import com.example.ui.theme.NaomiTextSecondary
import com.example.ui.theme.NaomiTextTertiary
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

private val WAREHOUSE_SCANNER_SIZE = Size(640, 480)

@Composable
fun WarehouseScannerScreen(
    lastScan: WarehouseScanResult?,
    onScan: (String, String) -> Unit,
    onClear: () -> Unit
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var manualCode by remember { mutableStateOf("") }
    var cameraEnabled by remember { mutableStateOf(true) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NaomiDarkBg)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column {
            Text(
                "BFC · SCANNER",
                color = NaomiTextTertiary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            Text(
                "Order & stock lookup",
                color = NaomiTextPrimary,
                fontSize = 21.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                "Scan an order reference, parcel tracking code or warehouse SKU.",
                color = NaomiTextSecondary,
                fontSize = 10.sp
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = NaomiSurface,
            border = BorderStroke(1.dp, NaomiBorder)
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = manualCode,
                    onValueChange = { manualCode = it },
                    label = { Text("Manual code", fontSize = 9.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        val value = manualCode.trim()
                        if (value.isNotBlank()) {
                            onScan(value, "MANUAL")
                            manualCode = ""
                        }
                    },
                    enabled = manualCode.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = NaomiTextPrimary),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Lookup", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black),
            border = BorderStroke(1.dp, NaomiBorder)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    !cameraEnabled -> {
                        ScannerPlaceholder(
                            icon = Icons.Default.QrCodeScanner,
                            title = "Camera paused",
                            detail = "Use manual lookup above or resume the camera.",
                            action = "Resume",
                            onAction = { cameraEnabled = true }
                        )
                    }
                    !hasCameraPermission -> {
                        ScannerPlaceholder(
                            icon = Icons.Default.CameraAlt,
                            title = "Camera permission required",
                            detail = "The BFC scanner needs camera access to read parcel and stock barcodes.",
                            action = "Allow camera",
                            onAction = { permissionLauncher.launch(Manifest.permission.CAMERA) }
                        )
                    }
                    else -> {
                        WarehouseBarcodeCamera(
                            onBarcode = { value, format ->
                                onScan(value, format)
                            }
                        )
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Black.copy(alpha = 0.65f)
                        ) {
                            Text(
                                "CAMERA LIVE",
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }

        if (lastScan != null) {
            val accent = when (lastScan.kind) {
                WarehouseScanKind.ORDER,
                WarehouseScanKind.TRACKING,
                WarehouseScanKind.STOCK -> NaomiSuccess
                WarehouseScanKind.UNKNOWN -> NaomiError
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = NaomiSurface),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.45f))
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (lastScan.kind == WarehouseScanKind.UNKNOWN) {
                                Icons.Default.WarningAmber
                            } else {
                                Icons.Default.CheckCircle
                            },
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                lastScan.title,
                                color = NaomiTextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                lastScan.detail,
                                color = NaomiTextSecondary,
                                fontSize = 9.sp
                            )
                        }
                    }
                    Text(
                        lastScan.code,
                        color = NaomiTextTertiary,
                        fontSize = 8.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = onClear,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Clear", fontSize = 9.sp)
                        }
                        OutlinedButton(
                            onClick = { cameraEnabled = !cameraEnabled },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (cameraEnabled) "Pause camera" else "Resume camera", fontSize = 9.sp)
                        }
                    }
                }
            }
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = NaomiSurface,
                border = BorderStroke(1.dp, NaomiBorder)
            ) {
                Text(
                    "Ready for BFC scan · CODE 128, EAN, QR and Data Matrix supported",
                    color = NaomiTextSecondary,
                    modifier = Modifier.padding(9.dp),
                    fontSize = 9.sp
                )
            }
        }
    }
}

@Composable
private fun ScannerPlaceholder(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    detail: String,
    action: String,
    onAction: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF111827))
            .padding(18.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(34.dp))
        Text(
            title,
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            detail,
            color = Color.White.copy(alpha = 0.72f),
            fontSize = 9.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
        )
        Button(
            onClick = onAction,
            colors = ButtonDefaults.buttonColors(containerColor = NaomiOrange)
        ) {
            Text(action, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun WarehouseBarcodeCamera(
    onBarcode: (String, String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnBarcode by rememberUpdatedState(onBarcode)

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val scanner = remember {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_DATA_MATRIX
            )
            .build()
        BarcodeScanning.getClient(options)
    }

    DisposableEffect(lifecycleOwner) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        var lastCode: String? = null

        val listener = Runnable {
            runCatching {
                val cameraProvider = providerFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val analysis = ImageAnalysis.Builder()
                    .setTargetResolution(WAREHOUSE_SCANNER_SIZE)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                analysis.setAnalyzer(executor) { imageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage == null) {
                        imageProxy.close()
                        return@setAnalyzer
                    }

                    val input = InputImage.fromMediaImage(
                        mediaImage,
                        imageProxy.imageInfo.rotationDegrees
                    )

                    scanner.process(input)
                        .addOnSuccessListener { barcodes ->
                            val barcode = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }
                            val value = barcode?.rawValue?.trim()
                            if (!value.isNullOrBlank() && value != lastCode) {
                                lastCode = value
                                currentOnBarcode(value, barcodeFormatLabel(barcode.format))
                            } else if (value.isNullOrBlank()) {
                                lastCode = null
                            }
                        }
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
            executor.shutdown()
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize()
    )
}

private fun barcodeFormatLabel(format: Int): String = when (format) {
    Barcode.FORMAT_QR_CODE -> "QR CODE"
    Barcode.FORMAT_CODE_128 -> "CODE 128"
    Barcode.FORMAT_CODE_39 -> "CODE 39"
    Barcode.FORMAT_EAN_13 -> "EAN-13"
    Barcode.FORMAT_EAN_8 -> "EAN-8"
    Barcode.FORMAT_UPC_A -> "UPC-A"
    Barcode.FORMAT_UPC_E -> "UPC-E"
    Barcode.FORMAT_DATA_MATRIX -> "DATA MATRIX"
    else -> "BARCODE"
}
