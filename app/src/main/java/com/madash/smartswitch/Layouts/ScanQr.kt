package com.madash.smartswitch.Layouts

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanQrCode(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Scan QR Code",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            ScanQrCodeMain()
        }
    }
}

@Composable
fun ScanQrCodeMain() {
    var hasPermission by remember { mutableStateOf(false) }
    var qrCodeDetected by remember { mutableStateOf(false) }
    var detectedQrCode by remember { mutableStateOf("") }

    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
    }

    LaunchedEffect(Unit) {
        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) -> {
                hasPermission = true
            }
            else -> {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (hasPermission) {
            CameraPreview(
                onQrCodeDetected = { qrCode ->
                    if (!qrCodeDetected) {
                        qrCodeDetected = true
                        detectedQrCode = qrCode
                        // Handle QR code detection here
                    }
                }
            )

            // QR Scanner Overlay
            QrScannerOverlay(
                modifier = Modifier.fillMaxSize(),
                isScanning = !qrCodeDetected
            )

            // Bottom text
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Scan QR code ",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Create connection to transfer files",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                if (!qrCodeDetected) {
                    Text(
                        text = "If No QR code detected \n Clean lens or move closer to the QR code.",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                }
            }
        } else {
            // Permission denied state
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Camera permission is required to scan QR codes",
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )

                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }
                ) {
                    Text("Grant Permission")
                }
            }
        }
    }
}

@Composable
fun CameraPreview(
    onQrCodeDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val executor = ContextCompat.getMainExecutor(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                val imageAnalyzer = ImageAnalysis.Builder()
                    .setTargetResolution(Size(1280, 720))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                            processImageProxy(imageProxy, onQrCodeDetected)
                        }
                    }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalyzer
                    )
                } catch (exc: Exception) {
                    // Handle binding errors
                }
            }, executor)

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
private fun processImageProxy(
    imageProxy: ImageProxy,
    onQrCodeDetected: (String) -> Unit
) {
    val inputImage = InputImage.fromMediaImage(
        imageProxy.image!!,
        imageProxy.imageInfo.rotationDegrees
    )

    val scanner = BarcodeScanning.getClient()

    scanner.process(inputImage)
        .addOnSuccessListener { barcodes ->
            for (barcode in barcodes) {
                when (barcode.valueType) {
                    Barcode.TYPE_TEXT -> {
                        barcode.displayValue?.let { onQrCodeDetected(it) }
                    }
                    Barcode.TYPE_URL -> {
                        barcode.url?.url?.let { onQrCodeDetected(it) }
                    }
                    else -> {
                        barcode.rawValue?.let { onQrCodeDetected(it) }
                    }
                }
            }
        }
        .addOnFailureListener {
            // Handle errors
        }
        .addOnCompleteListener {
            imageProxy.close()
        }
}

@Composable
fun QrScannerOverlay(
    modifier: Modifier = Modifier,
    isScanning: Boolean = true
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    // Animation for the scanning line
    val infiniteTransition = rememberInfiniteTransition(label = "scanning")
    val scanLinePosition by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scanLine"
    )

    Canvas(
        modifier = modifier.clipToBounds()
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        // Calculate QR scanning area (square in center)
        val scanAreaSize = minOf(canvasWidth, canvasHeight) * 0.6f
        val left = (canvasWidth - scanAreaSize) / 2
        val top = (canvasHeight - scanAreaSize) / 2
        val right = left + scanAreaSize
        val bottom = top + scanAreaSize

        // Draw semi-transparent overlay
        drawRect(
            color = Color.Black.copy(alpha = 0.5f),
            size = size
        )

        // Draw transparent scanning area
        drawRect(
            color = Color.Transparent,
            topLeft = Offset(left, top),
            size = androidx.compose.ui.geometry.Size(scanAreaSize, scanAreaSize),
            blendMode = androidx.compose.ui.graphics.BlendMode.Clear
        )

        // Draw scanning frame corners
        drawScanningFrame(
            primaryColor = primaryColor,
            left = left,
            top = top,
            right = right,
            bottom = bottom
        )

        // Draw animated scanning line
        if (isScanning) {
            val lineY = top + (scanAreaSize * scanLinePosition)
            drawLine(
                color = primaryColor,
                start = Offset(left, lineY),
                end = Offset(right, lineY),
                strokeWidth = 3.dp.toPx()
            )
        }
    }
}

private fun DrawScope.drawScanningFrame(
    primaryColor: Color,
    left: Float,
    top: Float,
    right: Float,
    bottom: Float
) {
    val cornerLength = 40.dp.toPx()
    val strokeWidth = 4.dp.toPx()

    // Top-left corner
    drawLine(
        color = primaryColor,
        start = Offset(left, top),
        end = Offset(left + cornerLength, top),
        strokeWidth = strokeWidth
    )
    drawLine(
        color = primaryColor,
        start = Offset(left, top),
        end = Offset(left, top + cornerLength),
        strokeWidth = strokeWidth
    )

    // Top-right corner
    drawLine(
        color = primaryColor,
        start = Offset(right - cornerLength, top),
        end = Offset(right, top),
        strokeWidth = strokeWidth
    )
    drawLine(
        color = primaryColor,
        start = Offset(right, top),
        end = Offset(right, top + cornerLength),
        strokeWidth = strokeWidth
    )

    // Bottom-left corner
    drawLine(
        color = primaryColor,
        start = Offset(left, bottom - cornerLength),
        end = Offset(left, bottom),
        strokeWidth = strokeWidth
    )
    drawLine(
        color = primaryColor,
        start = Offset(left, bottom),
        end = Offset(left + cornerLength, bottom),
        strokeWidth = strokeWidth
    )

    // Bottom-right corner
    drawLine(
        color = primaryColor,
        start = Offset(right, bottom - cornerLength),
        end = Offset(right, bottom),
        strokeWidth = strokeWidth
    )
    drawLine(
        color = primaryColor,
        start = Offset(right - cornerLength, bottom),
        end = Offset(right, bottom),
        strokeWidth = strokeWidth
    )
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, device = Devices.PIXEL_7_PRO)
@Composable
fun ScanQrCodePreview() {
    ScanQrCode(navController = NavController(LocalContext.current))
}