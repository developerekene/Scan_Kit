package com.example.qr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.InvertedLuminanceSource
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

object QrImageDecoder {

    private val scannerOptions = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(
            Barcode.FORMAT_QR_CODE,
            Barcode.FORMAT_AZTEC,
            Barcode.FORMAT_DATA_MATRIX,
            Barcode.FORMAT_PDF417,
            Barcode.FORMAT_EAN_13,
            Barcode.FORMAT_EAN_8,
            Barcode.FORMAT_UPC_A,
            Barcode.FORMAT_CODE_128
        )
        .build()

    suspend fun decodeFromUri(context: Context, uri: Uri): String? {
        // 1. Primary offline decoder: ML Kit Barcode Scanning
        val mlKitResult = decodeWithMlKit(context, uri)
        if (!mlKitResult.isNullOrBlank()) {
            return mlKitResult
        }

        // 2. Fallback offline decoder: ZXing MultiFormatReader (handles tiny/inverted screenshots)
        return withContext(Dispatchers.Default) {
            decodeWithZxing(context, uri)
        }
    }

    private suspend fun decodeWithMlKit(context: Context, uri: Uri): String? {
        val inputImage = runCatching { InputImage.fromFilePath(context, uri) }.getOrNull()
            ?: return null

        val scanner = BarcodeScanning.getClient(scannerOptions)
        return suspendCancellableCoroutine { continuation ->
            scanner.process(inputImage)
                .addOnSuccessListener { barcodes ->
                    val raw = barcodes.firstNotNullOfOrNull { it.rawValue?.takeIf { v -> v.isNotBlank() } }
                    if (continuation.isActive) continuation.resume(raw)
                }
                .addOnFailureListener {
                    if (continuation.isActive) continuation.resume(null)
                }
                .addOnCanceledListener {
                    if (continuation.isActive) continuation.resume(null)
                }
        }
    }

    private fun decodeWithZxing(context: Context, uri: Uri): String? {
        val bitmap = decodeSampledBitmap(context, uri, maxDimension = 1600) ?: return null
        return try {
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            val source = RGBLuminanceSource(width, height, pixels)
            val hints = mapOf(
                DecodeHintType.TRY_HARDER to true,
                DecodeHintType.CHARACTER_SET to "UTF-8"
            )
            val reader = MultiFormatReader()

            runCatching {
                reader.decode(BinaryBitmap(HybridBinarizer(source)), hints).text
            }.getOrNull() ?: runCatching {
                reader.decode(BinaryBitmap(HybridBinarizer(InvertedLuminanceSource(source))), hints).text
            }.getOrNull()
        } finally {
            bitmap.recycle()
        }
    }

    private fun decodeSampledBitmap(context: Context, uri: Uri, maxDimension: Int): Bitmap? {
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOptions)
        }
        val rawWidth = boundsOptions.outWidth
        val rawHeight = boundsOptions.outHeight
        if (rawWidth <= 0 || rawHeight <= 0) return null

        var sampleSize = 1
        while (rawWidth / sampleSize > maxDimension || rawHeight / sampleSize > maxDimension) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        }
    }
}
