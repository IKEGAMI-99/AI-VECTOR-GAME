package com.aivectorgame.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun CompactChoiceGrid(
    labels: List<String>,
    accent: Color,
    enabled: Boolean,
    selected: Set<Int> = emptySet(),
    onChoose: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        labels.chunked(2).forEachIndexed { rowIndex, row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEachIndexed { columnIndex, label ->
                    val index = rowIndex * 2 + columnIndex
                    CompactChoice(
                        index = index,
                        label = label,
                        accent = accent,
                        enabled = enabled && index !in selected,
                        selected = index in selected,
                        modifier = Modifier.weight(1f),
                        onClick = { onChoose(index) },
                    )
                }
                if (row.size == 1) Box(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CompactChoice(
    index: Int,
    label: String,
    accent: Color,
    enabled: Boolean,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(17.dp)
    Row(
        modifier
            .height(58.dp)
            .clip(shape)
            .background(if (selected) accent.copy(alpha = 0.10f) else Panel.copy(alpha = 0.90f))
            .border(1.dp, if (selected) accent.copy(alpha = 0.52f) else GlassStroke.copy(alpha = 0.82f), shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            (index + 1).toString().padStart(2, '0'),
            color = if (selected) accent else TextDim,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
        )
        Text(
            label,
            color = if (selected) TextSub else TextMain,
            fontSize = 16.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
internal fun RankingComposer(
    labels: List<String>,
    order: List<Int>,
    accent: Color,
    enabled: Boolean,
    onChoose: (Int) -> Unit,
    onReset: () -> Unit,
    onSubmit: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("TAP ORDER  //  01 → 06", color = accent, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
            Text(
                if (order.isEmpty()) "RESET" else "RESET ×${order.size}",
                color = if (order.isEmpty()) TextDim else TextSub,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(enabled = order.isNotEmpty(), onClick = onReset),
            )
        }

        orderSlots(labels, order, accent, 0)
        orderSlots(labels, order, accent, 3)

        CompactChoiceGrid(
            labels = labels,
            accent = accent,
            enabled = enabled,
            selected = order.toSet(),
            onChoose = onChoose,
        )

        val shape = RoundedCornerShape(17.dp)
        Box(
            Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clip(shape)
                .background(if (order.size == labels.size) accent else Panel3)
                .clickable(enabled = enabled && order.size == labels.size, onClick = onSubmit),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (order.size == labels.size) "LOCK RANKING  →" else "SELECT ${labels.size - order.size} MORE",
                color = if (order.size == labels.size) ActionText else TextDim,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.9.sp,
            )
        }
    }
}

@Composable
private fun orderSlots(labels: List<String>, order: List<Int>, accent: Color, start: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        for (rank in start until (start + 3)) {
            val itemIndex = order.getOrNull(rank)
            val shape = RoundedCornerShape(14.dp)
            Row(
                Modifier
                    .weight(1f)
                    .height(50.dp)
                    .clip(shape)
                    .background(if (itemIndex != null) accent.copy(alpha = 0.09f) else Panel.copy(alpha = 0.82f))
                    .border(1.dp, if (itemIndex != null) accent.copy(alpha = 0.32f) else GlassStroke.copy(alpha = 0.65f), shape)
                    .padding(horizontal = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Text(
                    "#${rank + 1}",
                    color = if (itemIndex != null) accent else TextDim,
                    fontSize = 8.sp,
                    lineHeight = 11.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    itemIndex?.let { labels.getOrElse(it) { "?" } } ?: "—",
                    color = if (itemIndex != null) TextMain else TextDim,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
internal fun CompactResultPanel(
    title: String,
    headline: String,
    detailLeft: String,
    detailRight: String,
    points: Int,
    accent: Color,
    success: Boolean,
    rankingAccuracy: Int? = null,
) {
    val isRanking = rankingAccuracy != null
    val statusColor = when {
        success -> Green
        isRanking -> Yellow
        else -> Red
    }
    val verdict = when {
        isRanking && success -> "✓ PERFECT ORDER"
        isRanking -> "△ PARTIAL MATCH"
        success -> "✓ CORRECT"
        else -> "✕ WRONG"
    }

    GlassPanel(accent = statusColor, padding = 16.dp) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(title, color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
                Text(verdict, color = statusColor, fontSize = 27.sp, lineHeight = 31.sp, fontWeight = FontWeight.Black)
                Text(headline, color = TextSub, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(if (points > 0) "+$points" else "0", color = accent, fontSize = 29.sp, fontWeight = FontWeight.Black)
                Text("PTS", color = TextDim, fontSize = 8.sp, fontWeight = FontWeight.Black)
            }
        }
        if (!isRanking) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniResultCell("CORRECT", detailLeft, Green, Modifier.weight(1f))
                MiniResultCell("YOUR ANSWER", detailRight, if (success) Green else Red, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MiniResultCell(label: String, value: String, color: Color, modifier: Modifier) {
    val shape = RoundedCornerShape(13.dp)
    Column(
        modifier
            .clip(shape)
            .background(color.copy(alpha = 0.045f))
            .border(1.dp, color.copy(alpha = 0.12f), shape)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(label, color = TextDim, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 0.9.sp)
        Text(value, color = color, fontSize = 14.sp, lineHeight = 19.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun RankingComparison(
    truthLabels: List<String>,
    userLabels: List<String>,
    accent: Color,
) {
    GlassPanel(accent = accent, padding = 12.dp) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("AI ↔ YOU / ORDER COMPARE", color = accent, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 0.9.sp)
            Text("✓ SAME POSITION", color = Green, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("AI ORDER", color = accent, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp)
                truthLabels.take(6).forEachIndexed { rank, label ->
                    RankCompareLine(rank = rank, label = label, color = TextMain, marker = null)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("YOUR ORDER", color = TextSub, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp)
                for (rank in 0 until 6) {
                    val truth = truthLabels.getOrElse(rank) { "—" }
                    val user = userLabels.getOrElse(rank) { "—" }
                    val match = truth == user
                    RankCompareLine(
                        rank = rank,
                        label = user,
                        color = if (match) Green else Red,
                        marker = if (match) "✓" else "×",
                    )
                }
            }
        }
    }
}

@Composable
private fun RankCompareLine(rank: Int, label: String, color: Color, marker: String?) {
    val shape = RoundedCornerShape(9.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(color.copy(alpha = 0.045f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Text(
            (rank + 1).toString().padStart(2, '0'),
            color = TextDim,
            fontSize = 8.sp,
            fontWeight = FontWeight.Black,
        )
        Text(
            label,
            color = color,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (marker != null) {
            Text(marker, color = color, fontSize = 11.sp, fontWeight = FontWeight.Black)
        }
    }
}

internal fun rankingAccuracy(userOrder: List<Int>, truthOrder: List<Int>): Int {
    if (userOrder.size < 2 || truthOrder.size < 2) return 0
    val truthPosition = truthOrder.withIndex().associate { it.value to it.index }
    var correct = 0
    var total = 0
    for (i in 0 until userOrder.lastIndex) {
        for (j in i + 1 until userOrder.size) {
            val a = truthPosition[userOrder[i]] ?: continue
            val b = truthPosition[userOrder[j]] ?: continue
            total += 1
            if (a < b) correct += 1
        }
    }
    return if (total == 0) 0 else (correct * 100f / total).toInt()
}
