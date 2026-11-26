package com.devekene.scankit.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.FlashlightOff
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material.icons.rounded.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devekene.scankit.R
import com.devekene.scankit.data.model.BarcodeFormatType
import com.devekene.scankit.data.model.ScanMode
import com.devekene.scankit.ui.components.QrResultBottomSheet
import com.devekene.scankit.ui.components.ScannerViewfinderOverlay
import com.devekene.scankit.ui.theme.ScannerBeamCyan
import com.devekene.scankit.ui.viewmodel.ScanViewModel
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@Composable
fun ScanScreen(
    viewModel: ScanViewModel,
    scanMode: ScanMode = ScanMode.QR,
    onOpenDrawer: () -> Unit,
    onEditInGenerator: (String, BarcodeFormatType?) -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptic = LocalHapticFeedback.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionRequestedOnce by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf<String?>(null) }

    // Refresh permission state when returning from system settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasCameraPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionRequestedOnce = true
        hasCameraPermission = granted
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.decodeGalleryImage(context, uri, scanMode)
        }
    }

    LaunchedEffect(uiState.eventMessage) {
        uiState.eventMessage?.let { msg ->
            onShowMessage(msg)
            viewModel.consumeEventMessage()
        }
    }

    // Trigger subtle haptic pulse when a new code is captured
    LaunchedEffect(uiState.activeResult) {
        if (uiState.activeResult != null) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070B14))
    ) {
        if (hasCameraPermission && cameraError == null) {
            CameraScannerPreview(
                scanMode = scanMode,
                isTorchOn = uiState.isTorchOn,
                isZoomed = uiState.isZoomed,
                isScanningPaused = uiState.isScanningPaused,
                onCameraInfoReady = { hasFlash -> viewModel.onCameraInfoUpdated(hasFlash) },
                onBarcodeDetected = { raw, format ->
                    viewModel.onBarcodeDetected(rawValue = raw, mlKitFormat = format, scanMode = scanMode)
                },
                onCameraUnavailable = { reason -> cameraError = reason },
                modifier = Modifier.fillMaxSize()
            )

            ScannerViewfinderOverlay(
                isActiveScanning = !uiState.isScanningPaused,
                scanMode = scanMode,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            CameraPermissionOrUnavailableCard(
                scanMode = scanMode,
                isCameraUnavailable = cameraError != null,
                permissionRequestedOnce = permissionRequestedOnce,
                onRequestCamera = {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                },
                onOpenSettings = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                },
                onPickFromGallery = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
            )
        }

        // Top Floating Studio HUD
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = Color(0xAA0F172A),
                shape = RoundedCornerShape(50),
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(start = 12.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.scankit_app_icon_1795000689141),
                        contentDescription = "ScanKit App Icon",
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        color = ScannerBeamCyan.copy(alpha = 0.22f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = if (scanMode == ScanMode.BARCODE) "Barcode" else "QR",
                            style = MaterialTheme.typography.labelSmall,
                            color = ScannerBeamCyan,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (hasCameraPermission && cameraError == null) {
                    IconButton(
                        onClick = { viewModel.toggleZoom() },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (uiState.isZoomed) ScannerBeamCyan else Color(0xAA0F172A),
                            contentColor = if (uiState.isZoomed) Color(0xFF082F49) else Color.White
                        ),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("scanner_zoom_button")
                    ) {
                        Icon(
                            imageVector = if (uiState.isZoomed) Icons.Rounded.ZoomOut else Icons.Rounded.ZoomIn,
                            contentDescription = if (uiState.isZoomed) "Reset zoom to 1x" else "Zoom in 2x"
                        )
                    }

                    if (uiState.hasFlashUnit) {
                        IconButton(
                            onClick = { viewModel.toggleTorch() },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = if (uiState.isTorchOn) ScannerBeamCyan else Color(0xAA0F172A),
                                contentColor = if (uiState.isTorchOn) Color(0xFF082F49) else Color.White
                            ),
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("scanner_torch_button")
                        ) {
                            Icon(
                                imageVector = if (uiState.isTorchOn) Icons.Rounded.FlashlightOn else Icons.Rounded.FlashlightOff,
                                contentDescription = stringResource(R.string.scan_flashlight)
                            )
                        }
                    }
                }

                // Top-right Hamburger Menu button (opens History drawer from the left)
                IconButton(
                    onClick = onOpenDrawer,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color(0xAA0F172A),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("scanner_menu_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Menu,
                        contentDescription = stringResource(R.string.scan_menu)
                    )
                }
            }
        }

        // Bottom Floating Scanner Dock (Gallery + Hint)
        if (hasCameraPermission && cameraError == null) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    color = Color(0x990F172A),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = if (scanMode == ScanMode.BARCODE) {
                            stringResource(R.string.scan_barcode_hint)
                        } else {
                            stringResource(R.string.scan_hint)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                Button(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1E293B).copy(alpha = 0.94f),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .height(50.dp)
                        .testTag("scanner_gallery_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PhotoLibrary,
                        contentDescription = null,
                        tint = ScannerBeamCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (scanMode == ScanMode.BARCODE) {
                            stringResource(R.string.scan_barcode_from_gallery_cta)
                        } else {
                            stringResource(R.string.scan_from_gallery_cta)
                        },
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        // Gallery decoding loading overlay
        AnimatedVisibility(
            visible = uiState.isDecodingGallery,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(20.dp),
                tonalElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                    Text(
                        text = "Scanning image...",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }
        }
    }

    // Result Modal Sheet
    uiState.activeResult?.let { payload ->
        QrResultBottomSheet(
            payload = payload,
            onEditInGenerator = onEditInGenerator,
            onFeedbackMessage = onShowMessage,
            onDismiss = { viewModel.dismissResult() }
        )
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun CameraScannerPreview(
    scanMode: ScanMode,
    isTorchOn: Boolean,
    isZoomed: Boolean,
    isScanningPaused: Boolean,
    onCameraInfoReady: (Boolean) -> Unit,
    onBarcodeDetected: (String, Int) -> Unit,
    onCameraUnavailable: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }

    // Keep latest callbacks accessible without rebinding camera
    val pausedState = remember { mutableStateOf(isScanningPaused) }
    pausedState.value = isScanningPaused
    val latestOnBarcodeDetected by rememberUpdatedState(onBarcodeDetected)

    LaunchedEffect(boundCamera, isTorchOn) {
        runCatching {
            boundCamera?.cameraControl?.enableTorch(isTorchOn)
        }
    }

    LaunchedEffect(boundCamera, isZoomed) {
        runCatching {
            boundCamera?.cameraControl?.setLinearZoom(if (isZoomed) 0.45f else 0f)
        }
    }

    DisposableEffect(lifecycleOwner, scanMode) {
        val executor = Executors.newSingleThreadExecutor()
        val scannerOptions = if (scanMode == ScanMode.BARCODE) {
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                    Barcode.FORMAT_EAN_13,
                    Barcode.FORMAT_EAN_8,
                    Barcode.FORMAT_UPC_A,
                    Barcode.FORMAT_UPC_E,
                    Barcode.FORMAT_CODE_128,
                    Barcode.FORMAT_CODE_39,
                    Barcode.FORMAT_CODE_93,
                    Barcode.FORMAT_CODABAR,
                    Barcode.FORMAT_ITF
                )
                .build()
        } else {
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                    Barcode.FORMAT_QR_CODE,
                    Barcode.FORMAT_AZTEC,
                    Barcode.FORMAT_DATA_MATRIX,
                    Barcode.FORMAT_PDF417
                )
                .build()
        }
        val barcodeScanner = BarcodeScanning.getClient(scannerOptions)
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        var providerRef: ProcessCameraProvider? = null

        val listener = Runnable {
            runCatching {
                val cameraProvider = cameraProviderFuture.get()
                providerRef = cameraProvider

                if (!cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                    onCameraUnavailable("No rear camera available")
                    return@runCatching
                }

                val preview = Preview.Builder().build().apply {
                    surfaceProvider = previewView.surfaceProvider
                }

                val resolutionSelector = ResolutionSelector.Builder()
                    .setResolutionStrategy(
                        ResolutionStrategy(
                            Size(1280, 720),
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                        )
                    )
                    .build()

                val imageAnalysis = ImageAnalysis.Builder()
                    .setResolutionSelector(resolutionSelector)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(executor) { imageProxy ->
                    if (pausedState.value) {
                        imageProxy.close()
                        return@setAnalyzer
                    }
                    val mediaImage = imageProxy.image
                    if (mediaImage != null) {
                        val inputImage = InputImage.fromMediaImage(
                            mediaImage,
                            imageProxy.imageInfo.rotationDegrees
                        )
                        barcodeScanner.process(inputImage)
                            .addOnSuccessListener { barcodes ->
                                val match = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }
                                val raw = match?.rawValue
                                if (match != null && !raw.isNullOrBlank()) {
                                    latestOnBarcodeDetected(raw, match.format)
                                }
                            }
                            .addOnCompleteListener {
                                imageProxy.close()
                            }
                    } else {
                        imageProxy.close()
                    }
                }

                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis
                )
                boundCamera = camera
                onCameraInfoReady(camera.cameraInfo.hasFlashUnit())
            }.onFailure { err ->
                onCameraUnavailable(err.localizedMessage ?: "Camera unavailable")
            }
        }

        cameraProviderFuture.addListener(listener, ContextCompat.getMainExecutor(context))

        onDispose {
            runCatching { providerRef?.unbindAll() }
            runCatching { barcodeScanner.close() }
            executor.shutdown()
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier
    )
}

@Composable
private fun CameraPermissionOrUnavailableCard(
    scanMode: ScanMode,
    isCameraUnavailable: Boolean,
    permissionRequestedOnce: Boolean,
    onRequestCamera: () -> Unit,
    onOpenSettings: () -> Unit,
    onPickFromGallery: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 6.dp,
        modifier = modifier.widthIn(max = 420.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.scankit_app_icon_1795000689141),
                contentDescription = "ScanKit App Icon",
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(20.dp))
            )

            Text(
                text = if (isCameraUnavailable) {
                    "Camera Not Available"
                } else {
                    stringResource(R.string.scan_camera_permission_title)
                },
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = if (isCameraUnavailable) {
                    if (scanMode == ScanMode.BARCODE) {
                        "You can still scan any barcode from screenshots or saved photos using your gallery."
                    } else {
                        "You can still scan any QR code from screenshots or saved photos using your gallery."
                    }
                } else {
                    stringResource(R.string.scan_camera_permission_body)
                },
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!isCameraUnavailable) {
                Button(
                    onClick = onRequestCamera,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("enable_camera_button"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.scan_enable_camera))
                }

                if (permissionRequestedOnce) {
                    OutlinedButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("open_settings_button"),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(stringResource(R.string.scan_open_settings))
                    }
                }
            }

            FilledTonalButton(
                onClick = onPickFromGallery,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("fallback_gallery_button"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.PhotoLibrary,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (scanMode == ScanMode.BARCODE) {
                        stringResource(R.string.scan_barcode_from_gallery_cta)
                    } else {
                        stringResource(R.string.scan_from_gallery_cta)
                    }
                )
            }
        }
    }
}
