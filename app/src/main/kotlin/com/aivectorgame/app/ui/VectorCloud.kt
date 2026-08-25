package com.aivectorgame.app.ui

import android.graphics.Paint
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aivectorgame.app.math.MdsProjector
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

@Composable
internal fun VectorCloud(labels: List<String>, points: List<MdsProjector.Point3>, scores: List<Float>) {
    var rotX by remember { mutableFloatStateOf(-0.28f) }
    var rotY by remember { mutableFloatStateOf(0.42f) }
    var zoom by remember { mutableFloatStateOf(1f) }
    val revealTarget = if (points.isNotEmpty()) 1f else 0f
    val reveal by animateFloatAsState(revealTarget, animationSpec = tween(900), label = "reveal")

    Box(
        Modifier.fillMaxWidth().height(390.dp).background(Color(0xFF070A11), RoundedCornerShape(22.dp))
            .border(1.dp, Purple.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
            .pointerInput(points) {
                detectTransformGestures { _, pan, gestureZoom, _ ->
                    rotY += pan.x * 0.008f
                    rotX += pan.y * 0.008f
                    zoom = (zoom * gestureZoom).coerceIn(0.65f, 2.2f)
                }
            }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val baseScale = min(size.width, size.height) * 0.27f * zoom
            val transformed = points.mapIndexed { i, p ->
                val x0 = p.x * reveal
                val y0 = p.y * reveal
                val z0 = p.z * reveal
                val cyy = cos(rotY); val syy = sin(rotY)
                val x1 = x0 * cyy + z0 * syy
                val z1 = -x0 * syy + z0 * cyy
                val cxx = cos(rotX); val sxx = sin(rotX)
                val y2 = y0 * cxx - z1 * sxx
                val z2 = y0 * sxx + z1 * cxx
                val perspective = 1f / (1.55f + z2 * 0.22f)
                Triple(Offset(cx + x1 * baseScale * perspective, cy + y2 * baseScale * perspective), z2, i)
            }

            if (transformed.isNotEmpty()) {
                val origin = transformed[0].first
                transformed.drop(1).forEach { (pos, _, i) ->
                    val sim = scores.getOrElse(i) { 0f }.coerceIn(-1f, 1f)
                    drawLine(Purple.copy(alpha = 0.12f + 0.35f * max(0f, sim)), origin, pos, strokeWidth = 2f, cap = StrokeCap.Round)
                }
            }

            transformed.sortedBy { it.second }.forEach { (pos, z, i) ->
                val radius = if (i == 0) 12f else 8f
                val color = when (i) {
                    0 -> Yellow
                    1 -> Purple
                    2 -> Cyan
                    3 -> Pink
                    4 -> Green
                    else -> TextSub
                }
                drawCircle(color.copy(alpha = 0.18f), radius = radius * 2.4f, center = pos)
                drawCircle(color, radius = radius, center = pos)
                drawCircle(Color.White.copy(alpha = 0.65f), radius = radius + 1f, center = pos, style = Stroke(1f))
                val paint = Paint().apply {
                    isAntiAlias = true
                    this.color = android.graphics.Color.WHITE
                    textSize = if (i == 0) 34f else 27f
                    textAlign = Paint.Align.CENTER
                    alpha = (220 + (z * 8).toInt()).coerceIn(140, 255)
                }
                drawContext.canvas.nativeCanvas.drawText(labels.getOrElse(i) { "?" }, pos.x, pos.y - radius - 11f, paint)
            }
        }
        Text("DRAG: ROTATE  •  PINCH: ZOOM", color = TextSub, fontSize = 10.sp, modifier = Modifier.align(Alignment.BottomCenter).padding(10.dp))
    }
}
