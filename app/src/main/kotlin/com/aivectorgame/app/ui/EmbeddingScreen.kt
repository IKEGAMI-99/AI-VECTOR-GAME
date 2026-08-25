package com.aivectorgame.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aivectorgame.app.ai.ModelManager
import com.aivectorgame.app.ai.NativeEngine
import com.aivectorgame.app.game.GameMode
import com.aivectorgame.app.game.QuestionFactory
import com.aivectorgame.app.math.MdsProjector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.random.Random

private enum class VectorStage { QUESTION, RESULT }

@Composable
internal fun EmbeddingGame(mode: GameMode, onBack: () -> Unit) {
    val context = LocalContext.current
    val manager = remember { ModelManager(context) }
    val live = manager.isInstalled(ModelManager.EMBEDDING) && NativeEngine.isNativeReady()
    val haptic = LocalHapticFeedback.current

    var round by remember { mutableIntStateOf(1) }
    var seed by remember { mutableIntStateOf(Random.nextInt()) }
    val question = remember(seed) { QuestionFactory.embedding(seed) }

    var stage by remember(seed) { mutableStateOf(VectorStage.QUESTION) }
    var selected by remember(seed) { mutableStateOf<Int?>(null) }
    var userOrder by remember(seed) { mutableStateOf(emptyList<Int>()) }
    var loading by remember(seed) { mutableStateOf(live) }
    var error by remember(seed) { mutableStateOf<String?>(null) }
    var liveResult by remember(seed) { mutableStateOf(false) }
    var scores by remember(seed) { mutableStateOf(question.demoScores) }
    var vectors by remember(seed) { mutableStateOf(MdsProjector.demoVectors(question.demoScores)) }
    var score by remember { mutableIntStateOf(0) }
    var streak by remember { mutableIntStateOf(0) }
    var rewardPoints by remember(seed) { mutableIntStateOf(0) }
    var answerRank by remember(seed) { mutableIntStateOf(-1) }
    var orderAccuracy by remember(seed) { mutableIntStateOf(0) }

    LaunchedEffect(seed, live) {
        if (!live) return@LaunchedEffect
        loading = true
        error = null
        val result = withContext(Dispatchers.IO) {
            runCatching {
                if (!NativeEngine.isEmbeddingLoaded()) {
                    NativeEngine.loadEmbedding(manager.fileFor(ModelManager.EMBEDDING)).getOrThrow()
                }
                val target = NativeEngine.embedding("query: ${question.target}").getOrThrow()
                val candidateVectors = question.choices.map {
                    NativeEngine.embedding("document: $it").getOrThrow()
                }
                val all = listOf(target) + candidateVectors
                val similarities = candidateVectors.map { MdsProjector.cosine(target, it) }
                all to similarities
            }
        }
        result.onSuccess { (all, similarities) ->
            vectors = all
            scores = similarities
            liveResult = true
        }.onFailure {
            error = it.message ?: "Embedding inference failed"
            liveResult = false
        }
        loading = false
    }

    val nearestOrder = scores.indices.sortedByDescending { scores[it] }
    val truthOrder = when (mode) {
        GameMode.EMBEDDING_FARTHEST -> scores.indices.sortedBy { scores[it] }
        else -> nearestOrder
    }
    val bestIndex = truthOrder.firstOrNull() ?: 0

    fun awardSingle(index: Int) {
        selected = index
        val rank = truthOrder.indexOf(index).coerceAtLeast(0)
        answerRank = rank
        val gained = when (rank) {
            0 -> 120 + min(streak, 5) * 20
            1 -> 50
            2 -> 20
            else -> 0
        }
        rewardPoints = gained
        score += gained
        if (rank == 0) {
            streak += 1
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        } else {
            streak = 0
        }
        stage = VectorStage.RESULT
    }

    fun awardRanking() {
        val accuracy = rankingAccuracy(userOrder, nearestOrder)
        orderAccuracy = accuracy
        val gained = (accuracy * 2) + if (accuracy == 100) min(streak, 5) * 30 else 0
        rewardPoints = gained
        score += gained
        if (accuracy == 100) {
            streak += 1
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        } else {
            streak = 0
        }
        stage = VectorStage.RESULT
    }

    when (stage) {
        VectorStage.QUESTION -> VectorQuestionPage(
            mode = mode,
            round = round,
            score = score,
            streak = streak,
            target = question.target,
            choices = question.choices,
            userOrder = userOrder,
            live = live,
            liveResult = liveResult,
            loading = loading,
            error = error,
            onBack = onBack,
            onChoose = { awardSingle(it) },
            onRankChoose = { index ->
                if (index !in userOrder) userOrder = userOrder + index
            },
            onRankReset = { userOrder = emptyList() },
            onRankSubmit = { awardRanking() },
        )

        VectorStage.RESULT -> VectorResultPage(
            mode = mode,
            round = round,
            score = score,
            streak = streak,
            target = question.target,
            choices = question.choices,
            selectedIndex = selected,
            bestIndex = bestIndex,
            userOrder = userOrder,
            answerRank = answerRank,
            orderAccuracy = orderAccuracy,
            rewardPoints = rewardPoints,
            scores = scores,
            vectors = vectors,
            nearestOrder = nearestOrder,
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
private fun VectorQuestionPage(
    mode: GameMode,
    round: Int,
    score: Int,
    streak: Int,
    target: String,
    choices: List<String>,
    userOrder: List<Int>,
    live: Boolean,
    liveResult: Boolean,
    loading: Boolean,
    error: String?,
    onBack: () -> Unit,
    onChoose: (Int) -> Unit,
    onRankChoose: (Int) -> Unit,
    onRankReset: () -> Unit,
    onRankSubmit: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        GameTopBar(mode.shortTitle, "RANDOM SEMANTIC ROUND", Purple, onBack)
        ScoreHud(round, score, streak, Purple)

        GlassPanel(accent = Purple, padding = if (mode == GameMode.EMBEDDING_RANKING) 14.dp else 16.dp) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(mode.code, color = Purple, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.4.sp)
                    Text(target, color = TextMain, fontSize = 36.sp, lineHeight = 40.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (loading) CircularProgressIndicator(modifier = Modifier.height(20.dp), color = Purple, strokeWidth = 2.dp)
            }
            Text(mode.instruction, color = TextSub, fontSize = 12.sp)
            Text(
                when {
                    live && liveResult -> "LIVE / REAL EMBEDDING VECTORS"
                    error != null -> "DEMO FALLBACK / ${error.take(44)}"
                    live -> "VECTOR COMPUTE"
                    else -> "DEMO VECTOR SPACE"
                },
                color = when {
                    live && liveResult -> Green
                    error != null -> Yellow
                    else -> TextDim
                },
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.7.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Text("CANDIDATES / 06", color = TextDim, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)

        if (mode == GameMode.EMBEDDING_RANKING) {
            RankingComposer(
                labels = choices,
                order = userOrder,
                accent = Purple,
                enabled = !loading,
                onChoose = onRankChoose,
                onReset = onRankReset,
                onSubmit = onRankSubmit,
            )
        } else {
            CompactChoiceGrid(
                labels = choices,
                accent = Purple,
                enabled = !loading,
                onChoose = onChoose,
            )
        }
        Spacer(Modifier.height(2.dp))
    }
}

@Composable
private fun VectorResultPage(
    mode: GameMode,
    round: Int,
    score: Int,
    streak: Int,
    target: String,
    choices: List<String>,
    selectedIndex: Int?,
    bestIndex: Int,
    userOrder: List<Int>,
    answerRank: Int,
    orderAccuracy: Int,
    rewardPoints: Int,
    scores: List<Float>,
    vectors: List<FloatArray>,
    nearestOrder: List<Int>,
    live: Boolean,
    liveResult: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    val rankingMode = mode == GameMode.EMBEDDING_RANKING
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .padding(bottom = 72.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            GameTopBar("${mode.shortTitle} / RESULT", "COSINE → 3D", Purple, onBack)
            ScoreHud(round, score, streak, Purple)

            if (rankingMode) {
                CompactResultPanel(
                    title = "ORDER RESULT",
                    headline = "$orderAccuracy% PAIRWISE ACCURACY",
                    detailLeft = choices.getOrElse(nearestOrder.firstOrNull() ?: 0) { "?" },
                    detailRight = userOrder.firstOrNull()?.let { choices.getOrElse(it) { "?" } } ?: "—",
                    points = rewardPoints,
                    accent = Purple,
                    success = orderAccuracy == 100,
                    rankingAccuracy = orderAccuracy,
                )
            } else {
                CompactResultPanel(
                    title = if (answerRank == 0) "VECTOR ANSWER" else "MODEL ANSWER",
                    headline = if (answerRank == 0) "MODEL TOP MATCH" else "YOUR PICK WAS RANK ${answerRank + 1}",
                    detailLeft = choices.getOrElse(bestIndex) { "?" },
                    detailRight = selectedIndex?.let { choices.getOrElse(it) { "?" } } ?: "—",
                    points = rewardPoints,
                    accent = Purple,
                    success = answerRank == 0,
                )
            }

            if (live && liveResult) {
                Text("VERIFIED LIVE / REAL COSINE SCORES", color = Green, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 0.7.sp)
            }

            if (rankingMode) {
                RankingComparison(
                    truthLabels = nearestOrder.map { choices.getOrElse(it) { "?" } },
                    userLabels = userOrder.map { choices.getOrElse(it) { "?" } },
                    accent = Purple,
                )
            }

            VectorCloud(
                labels = listOf(target) + choices,
                points = MdsProjector.project(vectors),
                scores = listOf(1f) + scores,
                height = if (rankingMode) 145.dp else 185.dp,
            )

            if (!rankingMode) {
                GlassPanel(accent = Purple, padding = 9.dp) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("NEAREST → FARTHEST", color = Purple, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.0.sp)
                        Text("TARGET / $target", color = TextDim, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        nearestOrder.forEachIndexed { rank, index ->
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("#${rank + 1}", color = if (rank == 0) Green else TextDim, fontSize = 8.sp, fontWeight = FontWeight.Black)
                                Text(
                                    choices.getOrElse(index) { "?" },
                                    color = if (rank == 0) Green else TextMain,
                                    fontSize = 10.sp,
                                    lineHeight = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text("%.2f".format(scores.getOrElse(index) { 0f }), color = TextDim, fontSize = 8.sp)
                            }
                        }
                    }
                }
            }
        }

        PrimaryAction(
            text = "RANDOM NEXT  →",
            accent = Purple,
            onClick = onNext,
            modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 16.dp, vertical = 9.dp),
        )
    }
}
