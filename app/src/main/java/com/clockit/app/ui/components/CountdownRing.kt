package com.clockit.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.clockit.app.ui.theme.Blue
import com.clockit.app.ui.theme.BlueBright
import com.clockit.app.ui.theme.BlueDeep
import com.clockit.app.ui.theme.Surface2
import com.clockit.app.ui.theme.TextLow
import kotlin.math.cos
import kotlin.math.sin

/**
 * Circular countdown. [progress] is the fraction of time *remaining* (1 = just started, 0 = ringing).
 */
@Composable
fun CountdownRing(
    progress: Float,
    active: Boolean,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 14.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), tween(600), label = "ring")
    val activeAlpha by animateFloatAsState(if (active) 1f else 0.25f, tween(500), label = "alpha")

    Box(modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2 + 10.dp.toPx()
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            val topLeft = Offset(inset, inset)
            val radius = arcSize.width / 2
            val c = center

            // Minute ticks
            for (i in 0 until 60) {
                val a = Math.toRadians(i * 6.0 - 90)
                val major = i % 5 == 0
                val outer = radius + stroke / 2 + 8.dp.toPx()
                val inner = outer - (if (major) 6.dp else 3.dp).toPx()
                drawLine(
                    TextLow.copy(alpha = if (major) 0.6f else 0.25f),
                    Offset(c.x + inner * cos(a).toFloat(), c.y + inner * sin(a).toFloat()),
                    Offset(c.x + outer * cos(a).toFloat(), c.y + outer * sin(a).toFloat()),
                    strokeWidth = 1.dp.toPx(),
                )
            }

            // Track
            drawArc(Surface2, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))

            val sweep = 360f * animated
            if (sweep > 0.5f) {
                rotate(-90f) {
                    val brush = Brush.sweepGradient(
                        0f to BlueDeep.copy(alpha = activeAlpha),
                        (animated * 0.6f) to Blue.copy(alpha = activeAlpha),
                        animated to BlueBright.copy(alpha = activeAlpha),
                        1f to BlueDeep.copy(alpha = activeAlpha),
                    )
                    // Soft glow
                    drawArc(
                        Blue.copy(alpha = 0.18f * activeAlpha), 0f, sweep, false, topLeft, arcSize,
                        style = Stroke(stroke * 2.2f, cap = StrokeCap.Round),
                    )
                    drawArc(brush, 0f, sweep, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                }
                // Leading dot
                val a = Math.toRadians(sweep - 90.0)
                val dot = Offset(c.x + radius * cos(a).toFloat(), c.y + radius * sin(a).toFloat())
                drawCircle(Color.White.copy(alpha = activeAlpha), stroke * 0.32f, dot)
            }
        }
        content()
    }
}
