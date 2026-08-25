package com.aivectorgame.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aivectorgame.app.ai.ModelManager
import com.aivectorgame.app.ai.NativeEngine
import com.aivectorgame.app.game.LongFormQuestionFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.random.Random

private enum class LongFormStage { QUESTION, RESULT }

@Composable
internal fun LongFormGame(onBack: () -> Unit) {
    val context = LocalContext.current
    val manager = remember { ModelManager(context) }
    val live = manager.isInstalled(ModelManager.CAUSAL) && NativeEngine.isNativeReady()
    val haptic = LocalHapticFeedback.current

    var round by remember { mutableIntStateOf(1) }
    var seed by remember { mutableIntStateOf(Random.nextInt()) }
    val question = remember(seed) { LongFormQuestionFactory.create(seed) }

    var stage by remember(seed) { mutableStateOf(LongFormStage.QUESTION) }
    var selectedIndex by remember(seed) { mutableStateOf<Int?>(null) }
    var loading by remember(seed) { mutableStateOf(live) }
    var error by remember(seed) { mutableStateOf<String?>(null) }
    var liveResult by remember(seed) { mutableStateOf(false) }
    var sequenceScores by remember(seed) {
        mutableStateOf(
            question.demoScores.map {
                NativeEngine.ContinuationScore(
                    sumLogProb = it,
                    avgLogProb = it,
                    tokenCount = 0,
                )
            }
        )
    }
    var score by remember { mutableIntStateOf(0) }
    var streak by remember { mutableIntStateOf(0) }
    var rewardPoints by remember(seed) { mutableIntStateOf(0) }
    var answerRank by remember(seed) { mutableIntStateOf(-1) }
    var answerSeconds by remember(seed) { mutableIntStateOf(ROUND_TIME_LIMIT_SECONDS) }

    val timer = rememberRoundTimer(
        roundKey = seed,
        enabled = stage == LongFormStage.QUESTION && !loading,
        onTimeout = {
            rewardPoints = 0
            answerRank = -1
            answerSeconds = 0
            streak = 0
            stage = LongFormStage.RESULT
        },
    )

    LaunchedEffect(seed, live) {
        if (!live) return@LaunchedEffect
        loading = true
        error = null
        val result = withContext(Dispatchers.IO) {
            runCatching {
                if (!NativeEngine.isCausalLoaded()) {
                    NativeEngine.loadCausal(manager.fileFor(ModelManager.CAUSAL)).getOrThrow()
                }
                NativeEngine.scoreContinuations(question.prompt, question.choices).getOrThrow()
            }
        }
        result.onSuccess {
            sequenceScores = it
            liveResult = true
        }.onFailure {
            error = it.message ?: "Continuation scoring failed"
            liveResult = false
        }
        loading = false
    }

    val truthOrder = sequenceScores.indices.sortedByDescending { sequenceScores[it].avgLogProb }
    val bestIndex = truthOrder.firstOrNull() ?: 0

    fun choose(index: Int) {
        selectedIndex = index
        answerSeconds = timer.secondsRemaining
        val rank = truthOrder.indexOf(index).coerceAtLeast(0)
        answerRank = rank
        val base = when (rank) {
            0 -> 200 + min(streak, 5) * 30
            1 -> 80
            2 -> 35
            else -> 0
        }
        val gained = applyTimeMultiplier(base, answerSeconds)
        rewardPoints = gained
        score += gained
        if (rank == 0) {
            streak += 1
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        } else {
            streak = 0
        }
        stage = LongFormStage.RESULT
    }

    when (stage) {
        LongFormStage.QUESTION -> LongFormQuestionPage(
            round = round,
            score = score,
            streak = streak,
            secondsRemaining = timer.secondsRemaining,
            prompt = question.prompt,
            choices = question.choices,
            loading = loading,
            live = live,
            liveResult = liveResult,
            error = error,
            onBack = onBack,
            onChoose = ::choose,
        )

        LongFormStage.RESULT -> LongFormResultPage(
            round = round,
            score = score,
            streak = streak,
            prompt = question.prompt,
            choices = question.choices,
            sequenceScores = sequenceScores,
            truthOrder = truthOrder,
            bestIndex = bestIndex,
            selectedIndex = selectedIndex,
            answerRank = answerRank,
            rewardPoints = rewardPoints,
            answerSeconds = answerSeconds,
            timedOut = timer.timedOut,
            live = live,
            liveResult = liveResult,
            onBack = onBack,
            onNext = {
                round += 1
                seed = Random.nextInt()
            },
        )
    }
}

@Composable
private fun LongFormQuestionPage(
    round: Int,
    score: Int,
    streak: Int,
    secondsRemaining: Int,
    prompt: String,
    choices: List<String>,
    loading: Boolean,
    live: Boolean,
    liveResult: Boolean,
    error: String?,
    onBack: () -> Unit,
    onChoose: (Int) -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        GameTopBar("LONG FORM", "SEQUENCE LIKELIHOOD", Cyan, onBack)
        TimedScoreHud(round, score, streak, secondsRemaining, Cyan)

        GlassPanel(accent = Cyan, padding = 14.dp) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("LOG/L", color = Cyan, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.4.sp)
                    Text(
                        "「$prompt▌」",
                        color = TextMain,
                        fontSize = 18.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp),
                        color = Cyan,
                        strokeWidth = 2.dp,
                    )
                }
            }
            Text("この先に続く文章を、AIが最も自然だと評価するのは？", color = TextSub, fontSize = 11.sp)
            Text(
                when {
                    live && liveResult -> "LIVE / 6 SEQUENCES SCORED TOKEN BY TOKEN"
                    error != null -> "DEMO FALLBACK / ${error.take(38)}"
                    live -> "SCORING 6 MULTI-TOKEN CONTINUATIONS"
                    else -> "DEMO / AVERAGE TOKEN LOG-PROBABILITY"
                },
                color = when {
                    live && liveResult -> Green
                    error != null -> Yellow
                    else -> TextDim
                },
                fontSize = 8.sp,
                lineHeight = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.55.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Text("LONG CONTINUATIONS / 06", color = TextDim, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
        LongChoiceGrid(
            choices = choices,
            enabled = !loading,
            onChoose = onChoose,
        )
    }
}

@Composable
private fun LongChoiceGrid(
    choices: List<String>,
    enabled: Boolean,
    onChoose: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        choices.chunked(2).forEachIndexed { rowIndex, row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEachIndexed { columnIndex, text ->
                    val index = rowIndex * 2 + columnIndex
                    LongChoiceCard(
                        index = index,
                        text = text,
                        enabled = enabled,
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
private fun LongChoiceCard(
    index: Int,
    text: String,
    enabled: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(17.dp)
    Column(
        modifier
            .height(96.dp)
            .clip(shape)
            .background(Panel.copy(alpha = 0.90f))
            .border(1.dp, Cyan.copy(alpha = 0.22f), shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            "${('A'.code + index).toChar()}  /  ${index + 1}",
            color = Cyan,
            fontSize = 8.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.7.sp,
        )
        Text(
            text,
            color = if (enabled) TextMain else TextDim,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun LongFormResultPage(
    round: Int,
    score: Int,
    streak: Int,
    prompt: String,
    choices: List<String>,
    sequenceScores: List<NativeEngine.ContinuationScore>,
    truthOrder: List<Int>,
    bestIndex: Int,
    selectedIndex: Int?,
    answerRank: Int,
    rewardPoints: Int,
    answerSeconds: Int,
    timedOut: Boolean,
    live: Boolean,
    liveResult: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    val success = !timedOut && answerRank == 0
    val statusColor = if (success) Green else Red
    val selected = selectedIndex ?: -1

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .padding(bottom = 70.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GameTopBar("LONG FORM / RESULT", "AVG TOKEN LOGP", Cyan, onBack)
            ScoreHud(round, score, streak, Cyan)

            if (timedOut) {
                TimeoutResultPanel(Cyan)
            } else {
                GlassPanel(accent = statusColor, padding = 14.dp) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("SEQUENCE ANSWER", color = statusColor, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.0.sp)
                            Text(
                                if (success) "✓ CORRECT" else "✕ WRONG  /  RANK ${answerRank + 1}",
                                color = statusColor,
                                fontSize = 24.sp,
                                lineHeight = 28.sp,
                                fontWeight = FontWeight.Black,
                            )
                            Text(timeScoreLabel(answerSeconds), color = TextSub, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(if (rewardPoints > 0) "+$rewardPoints" else "0", color = Cyan, fontSize = 27.sp, fontWeight = FontWeight.Black)
                            Text("PTS", color = TextDim, fontSize = 8.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    Text("MODEL PREFERS", color = Green, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp)
                    Text(
                        choices.getOrElse(bestIndex) { "?" },
                        color = TextMain,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (!success && selected >= 0) {
                        Text("YOUR PICK", color = Red, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp)
                        Text(
                            choices.getOrElse(selected) { "?" },
                            color = TextSub,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            if (live && liveResult) {
                Text(
                    "VERIFIED LIVE / FULL CONTINUATIONS SCORED BY LFM",
                    color = Green,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.6.sp,
                )
            }

            SequenceScorePanel(
                prompt = prompt,
                choices = choices,
                scores = sequenceScores,
                truthOrder = truthOrder,
                selectedIndex = selected,
            )
        }

        PrimaryAction(
            text = "RANDOM NEXT  →",
            accent = Cyan,
            onClick = onNext,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 9.dp),
        )
    }
}

@Composable
private fun SequenceScorePanel(
    prompt: String,
    choices: List<String>,
    scores: List<NativeEngine.ContinuationScore>,
    truthOrder: List<Int>,
    selectedIndex: Int,
) {
    val values = scores.map { it.avgLogProb }
    val minScore = values.minOrNull() ?: -4f
    val maxScore = values.maxOrNull() ?: -1f
    val span = (maxScore - minScore).coerceAtLeast(0.0001f)

    GlassPanel(accent = Cyan, padding = 11.dp) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("SEQUENCE LIKELIHOOD", color = Cyan, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 0.9.sp)
            Text("AVG LOGP / TOKEN", color = TextDim, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            "「${prompt.take(26)}…」",
            color = TextDim,
            fontSize = 8.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        truthOrder.forEachIndexed { rank, index ->
            val score = scores.getOrNull(index) ?: return@forEachIndexed
            val isModelTop = rank == 0
            val isUser = index == selectedIndex
            val rowColor: Color = when {
                isModelTop -> Green
                isUser -> Red
                else -> TextMain
            }
            val relative = ((score.avgLogProb - minScore) / span).coerceIn(0.06f, 1f)

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Text("#${rank + 1}", color = if (isModelTop) Green else TextDim, fontSize = 8.sp, fontWeight = FontWeight.Black)
                Text(
                    "${('A'.code + index).toChar()}  ${choices.getOrElse(index) { "?" }}",
                    color = rowColor,
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                    fontWeight = if (isModelTop || isUser) FontWeight.Black else FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "%.2f".format(score.avgLogProb),
                    color = if (isModelTop) Green else TextSub,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                )
                if (score.tokenCount > 0) {
                    Text("${score.tokenCount}t", color = TextDim, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                }
            }
            ProgressTrack(relative, if (isModelTop) Cyan else if (isUser) Red else TextDim)
        }
    }
}
