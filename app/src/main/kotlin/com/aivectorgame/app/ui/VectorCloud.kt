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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aivectorgame.app.math.MdsProjector
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

@Composable
internal fun VectorCloud(
    labels: List<String>,
    points: List<MdsProjector.Point3>,
    scores: List<Float>,
    height: Dp = 430.dp,
) {
    var rotX by remember { mutableFloatStateOf(-0.30f) }
    var rotY by remember { mutableFloatStateOf(0.48f) }
    var zoom by remember { mutableFloatStateOf(1f) }
    val revealTarget = if (points.isNotEmpty()) 1f else 0f
    val reveal by animateFloatAsState(revealTarget, animationSpec = tween(950), label = "vectorReveal")
    val shape = RoundedCornerShape(26.dp)
    val lightUi = ThemeController.isLight
    val chartPurple = if (lightUi) Color(0xFFB096FF) else Purple
    val chartCyan = if (lightUi) Color(0xFF62E8FF) else Cyan
    val chartPink = if (lightUi) Color(0xFFFF8FC6) else Pink
    val chartGreen = if (lightUi) Color(0xFF7AF5C9) else Green
    val chartYellow = if (lightUi) Color(0xFFFFDF78) else Yellow

    Box(
        Modifier
            .fillMaxWidth()
            .height(height)
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF080C14), Color(0xFF060910), Color(0xFF0B0A15))
                ),
                shape,
            )
            .border(1.dp, chartPurple.copy(alpha = 0.34f), shape)
            .pointerInput(points) {
                detectTransformGestures { _, pan, gestureZoom, _ ->
                    rotY += pan.x * 0.008f
                    rotX += pan.y * 0.008f
                    zoom = (zoom * gestureZoom).coerceIn(0.65f, 2.35f)
                }
            }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f + 8f

            for (i in 1..7) {
                val y = size.height * i / 8f
                drawLine(Color.White.copy(alpha = 0.035f), Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
            }
            for (i in 1..7) {
                val x = size.width * i / 8f
                drawLine(Color.White.copy(alpha = 0.03f), Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
            }

            drawCircle(
                brush = Brush.radialGradient(
                    listOf(chartPurple.copy(alpha = 0.12f), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = size.minDimension * 0.44f,
                ),
                radius = size.minDimension * 0.44f,
                center = Offset(cx, cy),
            )

            val baseScale = min(size.width, size.height) * 0.30f * zoom
            val transformed = points.mapIndexed { i, p ->
                val x0 = p.x * reveal
                val y0 = p.y * reveal
                val z0 = p.z * reveal
                val cyy = cos(rotY)
                val syy = sin(rotY)
                val x1 = x0 * cyy + z0 * syy
                val z1 = -x0 * syy + z0 * cyy
                val cxx = cos(rotX)
                val sxx = sin(rotX)
                val y2 = y0 * cxx - z1 * sxx
                val z2 = y0 * sxx + z1 * cxx
                val perspective = 1f / (1.58f + z2 * 0.22f)
                Triple(
                    Offset(cx + x1 * baseScale * perspective, cy + y2 * baseScale * perspective),
                    z2,
                    i,
                )
            }

            if (transformed.isNotEmpty()) {
                val origin = transformed[0].first
                transformed.drop(1).forEach { (pos, _, i) ->
                    val sim = scores.getOrElse(i) { 0f }.coerceIn(-1f, 1f)
                    val intensity = 0.10f + 0.48f * max(0f, sim)
                    drawLine(
                        brush = Brush.linearGradient(
                            listOf(chartPurple.copy(alpha = intensity), chartCyan.copy(alpha = intensity * 0.62f)),
                            start = origin,
                            end = pos,
                        ),
                        start = origin,
                        end = pos,
                        strokeWidth = 2f + max(0f, sim) * 2f,
                        cap = StrokeCap.Round,
                    )
                }
                drawCircle(chartPurple.copy(alpha = 0.12f), radius = size.minDimension * 0.20f, center = origin, style = Stroke(1.2f))
                drawCircle(chartCyan.copy(alpha = 0.08f), radius = size.minDimension * 0.31f, center = origin, style = Stroke(1f))
            }

            transformed.sortedBy { it.second }.forEach { (pos, z, i) ->
                val depth = ((z + 2f) / 4f).coerceIn(0.55f, 1.15f)
                val radius = (if (i == 0) 11f else 7.5f) * depth
                val color = when (i) {
                    0 -> chartYellow
                    1 -> chartPurple
                    2 -> chartCyan
                    3 -> chartPink
                    4 -> chartGreen
                    5 -> Color(0xFFFFA66B)
                    else -> Color(0xFF9AB2FF)
                }
                drawCircle(color.copy(alpha = 0.09f), radius = radius * 4.6f, center = pos)
                drawCircle(color.copy(alpha = 0.22f), radius = radius * 2.6f, center = pos)
                drawCircle(color, radius = radius, center = pos)
                drawCircle(Color.White.copy(alpha = 0.78f), radius = radius + 1.4f, center = pos, style = Stroke(1f))

                val paint = Paint().apply {
                    isAntiAlias = true
                    this.color = android.graphics.Color.WHITE
                    textSize = if (i == 0) 42f else 34f
                    textAlign = Paint.Align.CENTER
                    isFakeBoldText = true
                    alpha = (230 + (z * 6).toInt()).coerceIn(170, 255)
                }
                drawContext.canvas.nativeCanvas.drawText(
                    labels.getOrElse(i) { "?" },
                    pos.x,
                    pos.y - radius - 14f,
                    paint,
                )
            }
        }

        Text(
            "MDS / 3D PROJECTION",
            color = chartPurple,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.2.sp,
            modifier = Modifier.align(Alignment.TopStart).padding(15.dp),
        )
        Text(
            "DRAG ROTATE  ·  PINCH ZOOM",
            color = Color(0xFFA5B1C5),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            modifier = Modifier.align(Alignment.BottomEnd).padding(15.dp),
        )
    }
}
