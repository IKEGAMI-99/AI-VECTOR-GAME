package com.aivectorgame.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aivectorgame.app.ai.NativeEngine

internal val Bg = Color(0xFF080B12)
internal val Panel = Color(0xFF111827)
internal val Panel2 = Color(0xFF172033)
internal val TextMain = Color(0xFFF3F4F6)
internal val TextSub = Color(0xFFA8B0C0)
internal val Purple = Color(0xFF8B5CF6)
internal val Cyan = Color(0xFF22D3EE)
internal val Pink = Color(0xFFF472B6)
internal val Green = Color(0xFFA7F3D0)
internal val Yellow = Color(0xFFFDE68A)
internal val Red = Color(0xFFFCA5A5)

@Composable
internal fun Header(title: String, subtitle: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(onClick = onBack) { Text("← BACK", color = TextMain) }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, color = TextMain, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text(subtitle, color = TextSub, fontSize = 12.sp)
        }
    }
}

@Composable
internal fun ScoreStrip(round: Int, score: Int, streak: Int, accent: Color) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatChip("ROUND", round.toString(), accent, Modifier.weight(1f))
        StatChip("SCORE", score.toString(), Yellow, Modifier.weight(1f))
        StatChip("STREAK", "×$streak", if (streak > 1) Pink else TextSub, Modifier.weight(1f))
    }
}

@Composable
private fun StatChip(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.fillMaxWidth().padding(vertical = 9.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = TextSub, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text(value, color = color, fontSize = 17.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
internal fun LiveProof(text: String) {
    Text(
        "● $text",
        color = Green,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .background(Green.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .border(1.dp, Green.copy(alpha = 0.22f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp),
    )
}

@Composable
internal fun RewardCard(rank: Int, points: Int, streak: Int, accent: Color, mode: String) {
    val title = when (rank) {
        0 -> if (mode == "VECTOR") "✦ PERFECT VECTOR ✦" else "✦ TOP-1 PREDICTED ✦"
        1 -> "NEAR HIT • TOP-2"
        2 -> "GOOD READ • TOP-3"
        else -> "MISS • MODEL REVEALED"
    }
    val color = if (rank == 0) Green else if (rank <= 2) Yellow else Pink
    Card(
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.10f)),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, color.copy(alpha = 0.40f), RoundedCornerShape(18.dp)),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, color = color, fontSize = 15.sp, fontWeight = FontWeight.Black)
            if (points > 0) Text("+$points PTS", color = TextMain, fontSize = 29.sp, fontWeight = FontWeight.Black)
            if (rank == 0 && streak >= 2) Text("STREAK ×$streak BONUS", color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
internal fun ProbabilityBar(rank: Int, token: NativeEngine.TokenPrediction) {
    val normalized = (token.probability / 0.60f).coerceIn(0f, 1f)
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("#$rank  ${displayToken(token.piece)}", color = TextMain, fontSize = 13.sp)
            Text("${"%.2f".format(token.probability * 100)}%  logit ${"%.2f".format(token.logit)}", color = TextSub, fontSize = 12.sp)
        }
        ProgressTrack(normalized, Cyan)
    }
}

@Composable
internal fun ProgressTrack(progress: Float, color: Color) {
    Canvas(Modifier.fillMaxWidth().height(9.dp)) {
        drawRoundRect(Panel2, size = size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2))
        drawRoundRect(
            color,
            size = Size(size.width * progress.coerceIn(0f, 1f), size.height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2),
        )
    }
}

internal fun displayToken(piece: String): String {
    if (piece.isEmpty()) return "∅"
    return piece
        .replace(" ", "␠")
        .replace("\n", "↵")
        .replace("\t", "⇥")
}
