package com.aivectorgame.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aivectorgame.app.ai.NativeEngine

internal val Bg = Color(0xFF04060A)
internal val BgLift = Color(0xFF080D16)
internal val Panel = Color(0xFF0B111C)
internal val Panel2 = Color(0xFF101928)
internal val Panel3 = Color(0xFF162238)
internal val TextMain = Color(0xFFF5F7FB)
internal val TextSub = Color(0xFF8D9AAF)
internal val TextDim = Color(0xFF59667A)
internal val Purple = Color(0xFF9D7BFF)
internal val Cyan = Color(0xFF5BE7FF)
internal val Pink = Color(0xFFFF78BA)
internal val Green = Color(0xFF72F5C4)
internal val Yellow = Color(0xFFFFDF78)
internal val Red = Color(0xFFFF7D91)
internal val GlassStroke = Color(0xFF223149)

@Composable
internal fun AtmosphereBackground(content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF05070B),
                        Color(0xFF070B13),
                        Color(0xFF04060A),
                    )
                )
            )
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Purple.copy(alpha = 0.14f), Color.Transparent),
                    center = Offset(size.width * 1.02f, size.height * 0.02f),
                    radius = size.width * 0.82f,
                ),
                radius = size.width * 0.82f,
                center = Offset(size.width * 1.02f, size.height * 0.02f),
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Cyan.copy(alpha = 0.08f), Color.Transparent),
                    center = Offset(-size.width * 0.10f, size.height * 0.78f),
                    radius = size.width * 0.75f,
                ),
                radius = size.width * 0.75f,
                center = Offset(-size.width * 0.10f, size.height * 0.78f),
            )
        }
        content()
    }
}

@Composable
internal fun GlassPanel(
    modifier: Modifier = Modifier,
    accent: Color = GlassStroke,
    padding: Dp = 18.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xE8121A28),
                        Color(0xCC090F19),
                    )
                )
            )
            .border(1.dp, accent.copy(alpha = 0.30f), shape)
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

@Composable
internal fun GameTopBar(title: String, subtitle: String, accent: Color, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Panel2.copy(alpha = 0.84f))
                .border(1.dp, GlassStroke, CircleShape)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Text("‹", color = TextMain, fontSize = 30.sp, fontWeight = FontWeight.Light)
        }
        Column(Modifier.weight(1f)) {
            Text(title, color = TextMain, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(subtitle, color = TextSub, fontSize = 10.sp, letterSpacing = 0.8.sp)
        }
        Box(
            Modifier
                .clip(RoundedCornerShape(100.dp))
                .background(accent.copy(alpha = 0.10f))
                .border(1.dp, accent.copy(alpha = 0.28f), RoundedCornerShape(100.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text("LIVE AI", color = accent, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
        }
    }
}

@Composable
internal fun ScoreHud(round: Int, score: Int, streak: Int, accent: Color) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        HudCell("ROUND", round.toString().padStart(2, '0'), accent, Modifier.weight(1f))
        HudCell("SCORE", score.toString().padStart(4, '0'), Yellow, Modifier.weight(1f))
        HudCell("CHAIN", "×${streak}", if (streak >= 2) Pink else TextSub, Modifier.weight(1f))
    }
}

@Composable
private fun HudCell(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier
            .clip(shape)
            .background(Panel.copy(alpha = 0.72f))
            .border(1.dp, GlassStroke.copy(alpha = 0.65f), shape)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, color = TextDim, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
        Text(value, color = color, fontSize = 17.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
internal fun LiveBadge(text: String, isLive: Boolean = true) {
    val color = if (isLive) Green else Red
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.07f))
            .border(1.dp, color.copy(alpha = 0.18f), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(7.dp).background(color, CircleShape))
        Text(text, color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.65.sp)
    }
}

@Composable
internal fun ChoiceTile(
    index: Int,
    text: String,
    accent: Color,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(19.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    listOf(Panel2.copy(alpha = 0.92f), Panel.copy(alpha = 0.88f))
                )
            )
            .border(1.dp, accent.copy(alpha = 0.14f), shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            (index + 1).toString().padStart(2, '0'),
            color = accent.copy(alpha = 0.70f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
        )
        Spacer(Modifier.width(15.dp))
        Text(text, color = TextMain, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.weight(1f))
        Text("↗", color = TextDim, fontSize = 15.sp)
    }
}

@Composable
internal fun ResultHero(
    rank: Int,
    points: Int,
    streak: Int,
    accent: Color,
    mode: String,
    answer: String,
    selected: String,
) {
    val hit = rank == 0
    val color = when {
        hit -> Green
        rank <= 2 -> Yellow
        else -> Pink
    }
    val title = when (rank) {
        0 -> if (mode == "VECTOR") "VECTOR LOCK" else "TOP-1 LOCK"
        1 -> "NEAR HIT / #02"
        2 -> "CLOSE READ / #03"
        else -> "MODEL REVEAL"
    }
    GlassPanel(accent = color, padding = 20.dp) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column {
                Text(title, color = color, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.7.sp)
                Text(if (hit) "CORRECT" else "RANK ${rank + 1}", color = TextMain, fontSize = 28.sp, fontWeight = FontWeight.Black)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(if (points > 0) "+$points" else "0", color = accent, fontSize = 32.sp, fontWeight = FontWeight.Black)
                Text("POINTS", color = TextDim, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(GlassStroke.copy(alpha = 0.7f)))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.weight(1f)) {
                Text("MODEL TOP", color = TextDim, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                Text(answer, color = Green, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Column(Modifier.weight(1f)) {
                Text("YOUR PICK", color = TextDim, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                Text(selected, color = if (hit) Green else TextMain, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
        if (hit && streak >= 2) {
            Text("CHAIN ×$streak  //  BONUS ACTIVE", color = Pink, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        }
    }
}

@Composable
internal fun ProbabilityBar(rank: Int, token: NativeEngine.TokenPrediction, accent: Color = Cyan) {
    val normalized = (token.probability / 0.60f).coerceIn(0f, 1f)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Text(rank.toString().padStart(2, '0'), color = if (rank == 1) accent else TextDim, fontSize = 10.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.width(10.dp))
            Text(displayToken(token.piece), color = TextMain, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text("${"%.2f".format(token.probability * 100)}%", color = if (rank == 1) accent else TextSub, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        ProgressTrack(normalized, if (rank == 1) accent else TextSub.copy(alpha = 0.55f))
        Text("logit ${"%.2f".format(token.logit)}", color = TextDim, fontSize = 9.sp, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
internal fun ProgressTrack(progress: Float, color: Color) {
    Canvas(Modifier.fillMaxWidth().height(6.dp)) {
        drawRoundRect(Panel3, size = size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2))
        drawRoundRect(
            brush = Brush.horizontalGradient(listOf(color.copy(alpha = 0.55f), color)),
            size = Size(size.width * progress.coerceIn(0f, 1f), size.height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2),
        )
    }
}

@Composable
internal fun PrimaryAction(text: String, accent: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(58.dp),
        shape = RoundedCornerShape(19.dp),
        colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color(0xFF061015)),
    ) {
        Text(text, fontWeight = FontWeight.Black, fontSize = 13.sp, letterSpacing = 1.2.sp)
    }
}

internal fun displayToken(piece: String): String {
    if (piece.isEmpty()) return "∅"
    return piece
        .replace(" ", "␠")
        .replace("\n", "↵")
        .replace("\t", "⇥")
}
