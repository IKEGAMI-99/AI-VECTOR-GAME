package com.aivectorgame.app.ui

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

internal const val ROUND_TIME_LIMIT_SECONDS = 30

@Stable
internal class RoundTimerState {
    var secondsRemaining by mutableIntStateOf(ROUND_TIME_LIMIT_SECONDS)
        internal set
    var timedOut by mutableStateOf(false)
        internal set
}

@Composable
internal fun rememberRoundTimer(
    roundKey: Int,
    enabled: Boolean,
    onTimeout: () -> Unit,
): RoundTimerState {
    val state = remember(roundKey) { RoundTimerState() }
    val latestTimeout by rememberUpdatedState(onTimeout)

    LaunchedEffect(roundKey, enabled) {
        if (!enabled || state.timedOut) return@LaunchedEffect
        val startedAt = SystemClock.elapsedRealtime()
        while (true) {
            val elapsedMs = SystemClock.elapsedRealtime() - startedAt
            val remaining = (ROUND_TIME_LIMIT_SECONDS - (elapsedMs / 1000L).toInt()).coerceAtLeast(0)
            state.secondsRemaining = remaining
            if (elapsedMs >= ROUND_TIME_LIMIT_SECONDS * 1000L) {
                state.secondsRemaining = 0
                state.timedOut = true
                latestTimeout()
                break
            }
            delay(100L)
        }
    }
    return state
}

internal fun timeMultiplier(secondsRemaining: Int): Float {
    val ratio = secondsRemaining.coerceIn(0, ROUND_TIME_LIMIT_SECONDS) / ROUND_TIME_LIMIT_SECONDS.toFloat()
    return 0.75f + (0.75f * ratio)
}

internal fun applyTimeMultiplier(basePoints: Int, secondsRemaining: Int): Int {
    if (basePoints <= 0) return 0
    return (basePoints * timeMultiplier(secondsRemaining)).roundToInt()
}

internal fun timeScoreLabel(secondsRemaining: Int): String =
    "TIME ×${"%.2f".format(timeMultiplier(secondsRemaining))}  •  ${secondsRemaining}s LEFT"

@Composable
internal fun TimedScoreHud(
    round: Int,
    score: Int,
    streak: Int,
    secondsRemaining: Int,
    accent: Color,
) {
    val timeColor = when {
        secondsRemaining <= 5 -> Red
        secondsRemaining <= 10 -> Yellow
        else -> Green
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        TimedHudCell("ROUND", round.toString().padStart(2, '0'), accent, Modifier.weight(1f))
        TimedHudCell("SCORE", score.toString().padStart(4, '0'), Yellow, Modifier.weight(1f))
        TimedHudCell("CHAIN", "×$streak", if (streak >= 2) Pink else TextSub, Modifier.weight(1f))
        TimedHudCell("TIME", secondsRemaining.toString().padStart(2, '0'), timeColor, Modifier.weight(1f))
    }
}

@Composable
private fun TimedHudCell(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier,
) {
    val shape = RoundedCornerShape(15.dp)
    Column(
        modifier
            .clip(shape)
            .background(Panel.copy(alpha = if (ThemeController.isLight) 0.94f else 0.72f))
            .border(1.dp, GlassStroke.copy(alpha = 0.72f), shape)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, color = TextDim, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
        Text(value, color = color, fontSize = 17.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
internal fun TimeoutResultPanel(accent: Color) {
    GlassPanel(accent = Red, padding = 16.dp) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(Modifier.weight(1f)) {
                Text("TIME LIMIT", color = Red, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
                Text("⌛ TIME OUT", color = Red, fontSize = 27.sp, fontWeight = FontWeight.Black)
                Text("30 SECOND LIMIT EXPIRED", color = TextSub, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("0", color = accent, fontSize = 29.sp, fontWeight = FontWeight.Black)
                Text("PTS", color = TextDim, fontSize = 8.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}
