package com.devekene.scankit.qr

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.devekene.scankit.data.model.BarcodeFormatType
import com.devekene.scankit.data.model.QrStyleConfig
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

object QrCodeGenerator {

    fun generateBitmap(
        content: String,
        sizePx: Int = 720,
        styleConfig: QrStyleConfig = QrStyleConfig()
    ): Bitmap? {
        if (content.isBlank()) return null

        return runCatching {
            val hints = mapOf(
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.ERROR_CORRECTION to if (styleConfig.errorCorrectionHigh) {
                    ErrorCorrectionLevel.H
                } else {
                    ErrorCorrectionLevel.M
                },
                EncodeHintType.MARGIN to 0
            )

            val bitMatrix = QRCodeWriter().encode(
                content,
                BarcodeFormat.QR_CODE,
                0,
                0,
                hints
            )

            val matrixSize = bitMatrix.width
            val quietZoneModules = 3
            val totalModules = matrixSize + quietZoneModules * 2
            val moduleSize = sizePx.toFloat() / totalModules.toFloat()

            val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(styleConfig.backgroundColor)

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = styleConfig.foregroundColor
                style = Paint.Style.FILL
            }

            val cornerRadius = if (styleConfig.roundedModules) moduleSize * 0.36f else 0f
            val rect = RectF()

            for (y in 0 until matrixSize) {
                for (x in 0 until matrixSize) {
                    if (bitMatrix.get(x, y)) {
                        val left = (x + quietZoneModules) * moduleSize
                        val top = (y + quietZoneModules) * moduleSize
                        val right = left + moduleSize
                        val bottom = top + moduleSize

                        val isFinderArea = isInFinderPattern(x, y, matrixSize)
                        if (styleConfig.roundedModules && !isFinderArea) {
                            rect.set(left, top, right, bottom)
                            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
                        } else {
                            // Keep finder eyes sharp & connected for rock-solid scan reliability
                            rect.set(left, top, right + 0.5f, bottom + 0.5f)
                            canvas.drawRect(rect, paint)
                        }
                    }
                }
            }
            bitmap
        }.getOrNull()
    }

    fun validateBarcodeInput(input: String, format: BarcodeFormatType): String? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null

        return when (format) {
            BarcodeFormatType.EAN_13 -> {
                if (!trimmed.all { it.isDigit() } || (trimmed.length != 12 && trimmed.length != 13)) {
                    "EAN-13 requires 12 or 13 digits"
                } else null
            }
            BarcodeFormatType.EAN_8 -> {
                if (!trimmed.all { it.isDigit() } || (trimmed.length != 7 && trimmed.length != 8)) {
                    "EAN-8 requires 7 or 8 digits"
                } else null
            }
            BarcodeFormatType.UPC_A -> {
                if (!trimmed.all { it.isDigit() } || (trimmed.length != 11 && trimmed.length != 12)) {
                    "UPC-A requires 11 or 12 digits"
                } else null
            }
            BarcodeFormatType.ITF -> {
                if (!trimmed.all { it.isDigit() } || trimmed.length % 2 != 0) {
                    "ITF-14 requires an even number of digits"
                } else null
            }
            BarcodeFormatType.CODE_39 -> {
                val allowed = Regex("^[A-Z0-9\\-. $/+%]+$")
                if (!allowed.matches(trimmed)) {
                    "Code 39 supports uppercase A-Z, 0-9, and - . $ / + % space"
                } else null
            }
            BarcodeFormatType.CODABAR -> {
                val allowed = Regex("^[A-Da-d]?[0-9\\-$:/.+]+[A-Da-d]?$")
                if (!allowed.matches(trimmed)) {
                    "Codabar supports digits 0-9 and - $ : / . +"
                } else null
            }
            BarcodeFormatType.CODE_128 -> {
                if (trimmed.any { it.code > 127 }) {
                    "Code 128 supports standard ASCII characters"
                } else null
            }
        }
    }

    fun generateBarcodeBitmap(
        content: String,
        format: BarcodeFormatType,
        widthPx: Int = 960,
        heightPx: Int = 380,
        foregroundColor: Int = 0xFF0F172A.toInt(),
        backgroundColor: Int = 0xFFFFFFFF.toInt()
    ): Bitmap? {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return null

        return runCatching {
            val horizontalPadding = (widthPx * 0.07f).toInt()
            val topPadding = (heightPx * 0.12f).toInt()
            val labelHeight = (heightPx * 0.22f).toInt()
            val barWidth = (widthPx - horizontalPadding * 2).coerceAtLeast(100)
            val barHeight = (heightPx - topPadding - labelHeight - 12).coerceAtLeast(60)

            val bitMatrix = MultiFormatWriter().encode(
                trimmed,
                format.zxingFormat,
                barWidth,
                barHeight,
                mapOf(EncodeHintType.MARGIN to 0)
            )

            val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(backgroundColor)

            val barPaint = Paint().apply {
                color = foregroundColor
                style = Paint.Style.FILL
                isAntiAlias = false
            }

            // Center encoded matrix inside target area
            val matrixWidth = bitMatrix.width
            val matrixHeight = bitMatrix.height
            val offsetX = (widthPx - matrixWidth) / 2f
            val offsetY = topPadding.toFloat()

            for (x in 0 until matrixWidth) {
                if (bitMatrix.get(x, matrixHeight / 2)) {
                    canvas.drawRect(
                        offsetX + x,
                        offsetY,
                        offsetX + x + 1f,
                        offsetY + matrixHeight,
                        barPaint
                    )
                }
            }

            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = foregroundColor
                textSize = (heightPx * 0.115f).coerceIn(22f, 44f)
                typeface = Typeface.MONOSPACE
                textAlign = Paint.Align.CENTER
                letterSpacing = 0.08f
            }
            val textY = offsetY + matrixHeight + textPaint.textSize + (heightPx * 0.03f)
            canvas.drawText(trimmed, widthPx / 2f, textY, textPaint)

            bitmap
        }.getOrNull()
    }

    private fun isInFinderPattern(x: Int, y: Int, size: Int): Boolean {
        val inTopLeft = x < 7 && y < 7
        val inTopRight = x >= size - 7 && y < 7
        val inBottomLeft = x < 7 && y >= size - 7
        return inTopLeft || inTopRight || inBottomLeft
    }
}
