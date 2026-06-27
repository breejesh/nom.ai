package com.calmcalories.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.calmcalories.app.ui.theme.*

@Composable
fun ProgressRing(fraction: Float, modifier: Modifier = Modifier) {
    val trackColor = Divider
    val amberColor = Amber
    val emeraldColor = Emerald
    val redColor = BrandRed

    Canvas(modifier) {
        val s = 5.dp.toPx(); val r = (size.minDimension - s) / 2f
        val tl = Offset(size.width / 2 - r, size.height / 2 - r); val sz = Size(r * 2, r * 2)
        
        // Background track (soft sand crema)
        drawArc(trackColor, -90f, 360f, false, tl, sz, style = Stroke(s, cap = StrokeCap.Round))
        
        val activeColor = when {
            fraction < 0.8f -> amberColor
            fraction <= 1.0f -> emeraldColor
            else -> redColor
        }
        
        // Normal target zone (up to 100%)
        val normalFrac = fraction.coerceAtMost(1f)
        if (normalFrac > 0f) {
            drawArc(activeColor, -90f, 360f * normalFrac, false, tl, sz, style = Stroke(s, cap = StrokeCap.Round))
        }
        
        // Exceeded zone overlay wrapping around
        if (fraction > 1f) {
            val exceededFrac = (fraction - 1f).coerceAtMost(1f)
            drawArc(activeColor, -90f, 360f * exceededFrac, false, tl, sz, style = Stroke(s, cap = StrokeCap.Round))
        }
    }
}
