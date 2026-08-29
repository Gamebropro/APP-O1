package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas

@Composable
fun WatermarkOverlay(
    customText: String = "",
    vaultId: String = "",
    modifier: Modifier = Modifier
) {
    val watermarkLabel = if (customText.isNotBlank()) {
        "🔒 $customText • NO RECORD • ${vaultId.take(6)}"
    } else {
        "🔒 SECURE VAULT PROTECTED • DO NOT CAPTURE"
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(35, 255, 255, 255)
            textSize = 28f
            isAntiAlias = true
            isFakeBoldText = true
        }

        val stepX = 350f
        val stepY = 220f

        rotate(-28f, pivot = Offset(size.width / 2f, size.height / 2f)) {
            var y = -size.height
            while (y < size.height * 2) {
                var x = -size.width
                while (x < size.width * 2) {
                    drawContext.canvas.nativeCanvas.drawText(
                        watermarkLabel,
                        x,
                        y,
                        paint
                    )
                    x += stepX
                }
                y += stepY
            }
        }
    }
}
