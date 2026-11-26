package com.devekene.scankit.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp
import com.devekene.scankit.data.model.ScanMode
import com.devekene.scankit.ui.theme.ScannerBeamCyan
import com.devekene.scankit.ui.theme.ScannerScrim
import com.devekene.scankit.ui.theme.ScannerSuccessEmerald

@Composable
fun ScannerViewfinderOverlay(
    isActiveScanning: Boolean,
    scanMode: ScanMode = ScanMode.QR,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "scanner_beam")
    val beamProgress by transition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.92f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beam_y"
    )
    val bracketAlpha by transition.animateFloat(
        initialValue = 0.78f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bracket_alpha"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val frameWidth: Float
        val frameHeight: Float
        val cornerRadius: Float

        if (scanMode == ScanMode.BARCODE) {
            frameWidth = (size.width * 0.84f).coerceIn(240.dp.toPx(), 420.dp.toPx())
            frameHeight = (frameWidth * 0.44f).coerceIn(120.dp.toPx(), 190.dp.toPx())
            cornerRadius = 18.dp.toPx()
        } else {
            val square = (minOf(size.width, size.height) * 0.66f).coerceIn(200.dp.toPx(), 320.dp.toPx())
            frameWidth = square
            frameHeight = square
            cornerRadius = 24.dp.toPx()
        }

        val left = (size.width - frameWidth) / 2f
        val top = (size.height - frameHeight) / 2f - 24.dp.toPx().coerceAtMost(size.height * 0.06f)
        val right = left + frameWidth
        val bottom = top + frameHeight

        val cutoutPath = Path().apply {
            addRoundRect(
                RoundRect(
                    rect = Rect(left, top, right, bottom),
                    cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                )
            )
        }

        // Dark translucent scrim around the optical frame
        clipPath(cutoutPath, clipOp = ClipOp.Difference) {
            drawRect(color = ScannerScrim)
        }

        // Subtle inner frame border
        drawRoundRect(
            color = Color.White.copy(alpha = 0.22f),
            topLeft = Offset(left, top),
            size = Size(frameWidth, frameHeight),
            cornerRadius = CornerRadius(cornerRadius, cornerRadius),
            style = Stroke(width = 1.dp.toPx())
        )

        // Sweeping laser beam (QR sweeps vertically; Barcode pulses across center laser line)
        if (isActiveScanning) {
            val beamY = if (scanMode == ScanMode.BARCODE) {
                top + frameHeight * (0.44f + 0.12f * beamProgress)
            } else {
                top + frameHeight * beamProgress
            }
            val beamColor = if (scanMode == ScanMode.BARCODE) {
                Color(0xFF38BDF8)
            } else {
                ScannerBeamCyan
            }
            val beamBrush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    beamColor.copy(alpha = 0.92f),
                    Color.White,
                    beamColor.copy(alpha = 0.92f),
                    Color.Transparent
                ),
                startX = left + 16.dp.toPx(),
                endX = right - 16.dp.toPx()
            )
            drawLine(
                brush = beamBrush,
                start = Offset(left + 16.dp.toPx(), beamY),
                end = Offset(right - 16.dp.toPx(), beamY),
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round
            )

            val haloHeight = 24.dp.toPx()
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        beamColor.copy(alpha = 0.18f),
                        Color.Transparent
                    ),
                    startY = beamY - haloHeight / 2f,
                    endY = beamY + haloHeight / 2f
                ),
                topLeft = Offset(left + 16.dp.toPx(), beamY - haloHeight / 2f),
                size = Size(frameWidth - 32.dp.toPx(), haloHeight)
            )
        }

        // Precision corner brackets
        val bracketColor = if (isActiveScanning) {
            ScannerBeamCyan.copy(alpha = bracketAlpha)
        } else {
            ScannerSuccessEmerald
        }
        val strokeWidth = 4.dp.toPx()
        val armLength = if (scanMode == ScanMode.BARCODE) 28.dp.toPx() else 34.dp.toPx()

        // Top-left
        drawPath(
            path = Path().apply {
                moveTo(left, top + armLength)
                lineTo(left, top + cornerRadius)
                quadraticTo(left, top, left + cornerRadius, top)
                lineTo(left + armLength, top)
            },
            color = bracketColor,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        // Top-right
        drawPath(
            path = Path().apply {
                moveTo(right - armLength, top)
                lineTo(right - cornerRadius, top)
                quadraticTo(right, top, right, top + cornerRadius)
                lineTo(right, top + armLength)
            },
            color = bracketColor,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        // Bottom-left
        drawPath(
            path = Path().apply {
                moveTo(left, bottom - armLength)
                lineTo(left, bottom - cornerRadius)
                quadraticTo(left, bottom, left + cornerRadius, bottom)
                lineTo(left + armLength, bottom)
            },
            color = bracketColor,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        // Bottom-right
        drawPath(
            path = Path().apply {
                moveTo(right - armLength, bottom)
                lineTo(right - cornerRadius, bottom)
                quadraticTo(right, bottom, right, bottom - cornerRadius)
                lineTo(right, bottom - armLength)
            },
            color = bracketColor,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}
