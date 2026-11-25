package com.devekene.scankit.qr

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.devekene.scankit.data.model.QrStyleConfig
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
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

    private fun isInFinderPattern(x: Int, y: Int, size: Int): Boolean {
        val inTopLeft = x < 7 && y < 7
        val inTopRight = x >= size - 7 && y < 7
        val inBottomLeft = x < 7 && y >= size - 7
        return inTopLeft || inTopRight || inBottomLeft
    }
}
