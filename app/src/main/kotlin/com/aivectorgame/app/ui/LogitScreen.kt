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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.random.Random

private enum class TokenStage { QUESTION, RESULT }

@Composable
internal fun LogitGame(mode: GameMode, onBack: () -> Unit) {
    val context = LocalContext.current
    val manager = remember { ModelManager(context) }
    val live = manager.isInstalled(ModelManager.CAUSAL) && NativeEngine.isNativeReady()
    val haptic = LocalHapticFeedback.current

    var round by remember { mutableIntStateOf(1) }
    var seed by remember { mutableIntStateOf(Random.nextInt()) }
    var question by remember(seed, mode) {
        mutableStateOf(QuestionFactory.logit(seed, surprise = mode == GameMode.LOGIT_SURPRISE))
    }
    var stage by remember(seed) { mutableStateOf(TokenStage.QUESTION) }
    var predictions by remember(seed) { mutableStateOf(question.demoPredictions) }
    var loading by remember(seed) { mutableStateOf(live) }
    var error by remember(seed) { mutableStateOf<String?>(null) }
    var liveResult by remember(seed) { mutableStateOf(false) }
    var selectedIndex by remember(seed) { mutableStateOf<Int?>(null) }
    var userOrder by remember(seed) { mutableStateOf(emptyList<Int>()) }
    var score by remember { mutableIntStateOf(0) }
    var streak by remember { mutableIntStateOf(0) }
    var rewardPoints by remember(seed) { mutableIntStateOf(0) }
    var answerRank by remember(seed) { mutableIntStateOf(-1) }
    var orderAccuracy by remember(seed) { mutableIntStateOf(0) }

    LaunchedEffect(seed, live, mode) {
        if (!live) return@LaunchedEffect
        loading = true
        error = null
        val result = withContext(Dispatchers.IO) {
            runCatching {
                if (!NativeEngine.isCausalLoaded()) {
                    NativeEngine.loadCausal(manager.fileFor(ModelManager.CAUSAL)).getOrThrow()
                }

                if (mode == GameMode.LOGIT_SURPRISE) {
                    var acceptedQuestion = question
                    var acceptedPredictions = NativeEngine.topTokens(acceptedQuestion.prompt, 6).getOrThrow()
                    var found = acceptedPredictions.firstOrNull()?.let {
                        !QuestionFactory.humanMatches(it.piece, acceptedQuestion.humanExpected)
                    } ?: false

                    var attempt = 1
                    while (!found && attempt < 12) {
                        val candidate = QuestionFactory.logit(
                            seed = seed + attempt * 7919,
                            surprise = true,
                        )
                        val candidatePredictions = NativeEngine.topTokens(candidate.prompt, 6).getOrThrow()
                        val top = candidatePredictions.firstOrNull()
                        if (top != null && !QuestionFactory.humanMatches(top.piece, candidate.humanExpected)) {
                            acceptedQuestion = candidate
                            acceptedPredictions = candidatePredictions
                            found = true
                        }
                        attempt += 1
                    }
                    Triple(acceptedQuestion, acceptedPredictions, found)
                } else {
                    val resultPredictions = NativeEngine.topTokens(question.prompt, 6).getOrThrow()
                    Triple(question, resultPredictions, true)
                }
            }
        }
        result.onSuccess { (resolvedQuestion, resolvedPredictions, foundSurprise) ->
            question = resolvedQuestion
            predictions = resolvedPredictions
            liveResult = true
            if (mode == GameMode.LOGIT_SURPRISE && !foundSurprise) {
                error = "SURPRISE scan exhausted; last random prompt used"
            }
        }.onFailure {
            error = it.message ?: "Logit inference failed"
            liveResult = false
        }
        loading = false
    }

    val choices = remember(seed, predictions) {
        predictions.shuffled(Random(seed xor 0x51A7C3))
    }
    val truthOrder = choices.indices.sortedByDescending { choices[it].probability }
    val bestIndex = truthOrder.firstOrNull() ?: 0

    fun awardSingle(index: Int) {
        selectedIndex = index
        val rank = truthOrder.indexOf(index).coerceAtLeast(0)
        answerRank = rank
        val gained = when (rank) {
            0 -> 150 + min(streak, 5) * 25
            1 -> 70
            2 -> 35
            else -> 0
        } + if (mode == GameMode.LOGIT_SURPRISE && rank == 0) 30 else 0
        rewardPoints = gained
        score += gained
        if (rank == 0) {
            streak += 1
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        } else {
            streak = 0
        }
        stage = TokenStage.RESULT
    }

    fun awardRanking() {
        val accuracy = rankingAccuracy(userOrder, truthOrder)
        orderAccuracy = accuracy
        val gained = (accuracy * 2) + if (accuracy == 100) min(streak, 5) * 35 else 0
        rewardPoints = gained
        score += gained
        if (accuracy == 100) {
            streak += 1
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        } else {
            streak = 0
        }
        stage = TokenStage.RESULT
    }

    when (stage) {
        TokenStage.QUESTION -> TokenQuestionPage(
            mode = mode,
            round = round,
            score = score,
            streak = streak,
            prompt = question.prompt,
            humanExpected = question.humanExpected,
            choices = choices,
            userOrder = userOrder,
            live = live,
            liveResult = liveResult,
            loading = loading,
            error = error,
            onBack = onBack,
            onChoose = { awardSingle(it) },
            onRankChoose = { index -> if (index !in userOrder) userOrder = userOrder + index },
            onRankReset = { userOrder = emptyList() },
            onRankSubmit = { awardRanking() },
        )

        TokenStage.RESULT -> TokenResultPage(
            mode = mode,
            round = round,
            score = score,
            streak = streak,
            prompt = question.prompt,
            humanExpected = question.humanExpected,
            choices = choices,
            truthOrder = truthOrder,
            selectedIndex = selectedIndex,
            userOrder = userOrder,
            bestIndex = bestIndex,
            answerRank = answerRank,
            orderAccuracy = orderAccuracy,
            rewardPoints = rewardPoints,
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
private fun TokenQuestionPage(
    mode: GameMode,
    round: Int,
    score: Int,
    streak: Int,
    prompt: String,
    humanExpected: String,
    choices: List<NativeEngine.TokenPrediction>,
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
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        GameTopBar(mode.shortTitle, "RANDOM CAUSAL ROUND", Cyan, onBack)
        ScoreHud(round, score, streak, Cyan)

        GlassPanel(accent = Cyan, padding = 16.dp) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(mode.code, color = Cyan, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
                    Text("「$prompt▌」", color = TextMain, fontSize = 21.sp, lineHeight = 27.sp, fontWeight = FontWeight.Black, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
                if (loading) CircularProgressIndicator(modifier = Modifier.height(20.dp), color = Cyan, strokeWidth = 2.dp)
            }
            Text(mode.instruction, color = TextSub, fontSize = 11.sp)
            if (mode == GameMode.LOGIT_SURPRISE) {
                Text("HUMAN EXPECTS  /  $humanExpected", color = Pink, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp)
            }
            Text(
                when {
                    live && liveResult -> "LIVE / REAL FINAL-POSITION LOGITS"
                    error != null -> "DEMO FALLBACK / ${error.take(44)}"
                    live -> if (mode == GameMode.LOGIT_SURPRISE) "SCANNING FOR MODEL/HUMAN MISMATCH" else "TRANSFORMER FORWARD PASS"
                    else -> "DEMO LOGIT SPACE"
                },
                color = when {
                    live && liveResult -> Green
                    error != null -> Yellow
                    else -> TextDim
                },
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.7.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Text("TOKEN CANDIDATES / 06", color = TextDim, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 1.3.sp)
        val labels = choices.map { displayToken(it.piece) }
        if (mode == GameMode.LOGIT_RANKING) {
            RankingComposer(
                labels = labels,
                order = userOrder,
                accent = Cyan,
                enabled = !loading,
                onChoose = onRankChoose,
                onReset = onRankReset,
                onSubmit = onRankSubmit,
            )
        } else {
            CompactChoiceGrid(
                labels = labels,
                accent = Cyan,
                enabled = !loading,
                onChoose = onChoose,
            )
        }
        Spacer(Modifier.height(2.dp))
    }
}

@Composable
private fun TokenResultPage(
    mode: GameMode,
    round: Int,
    score: Int,
    streak: Int,
    prompt: String,
    humanExpected: String,
    choices: List<NativeEngine.TokenPrediction>,
    truthOrder: List<Int>,
    selectedIndex: Int?,
    userOrder: List<Int>,
    bestIndex: Int,
    answerRank: Int,
    orderAccuracy: Int,
    rewardPoints: Int,
    live: Boolean,
    liveResult: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    val rankingMode = mode == GameMode.LOGIT_RANKING
    val top = choices.getOrNull(bestIndex)
    val selected = selectedIndex?.let { choices.getOrNull(it) }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .padding(bottom = 68.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            GameTopBar("${mode.shortTitle} / RESULT", "LOGIT → SOFTMAX", Cyan, onBack)
            ScoreHud(round, score, streak, Cyan)

            if (rankingMode) {
                CompactResultPanel(
                    title = "ORDER MATCH",
                    headline = "$orderAccuracy% PAIRWISE",
                    detailLeft = displayToken(top?.piece ?: "?"),
                    detailRight = userOrder.firstOrNull()?.let { displayToken(choices.getOrNull(it)?.piece ?: "?") } ?: "—",
                    points = rewardPoints,
                    accent = Cyan,
                    success = orderAccuracy == 100,
                )
            } else {
                CompactResultPanel(
                    title = if (mode == GameMode.LOGIT_SURPRISE) "AI / HUMAN DIVERGENCE" else if (answerRank == 0) "TOP-1 LOCK" else "MODEL REVEAL",
                    headline = if (answerRank == 0) "CORRECT" else "RANK ${answerRank + 1}",
                    detailLeft = displayToken(top?.piece ?: "?"),
                    detailRight = displayToken(selected?.piece ?: "—"),
                    points = rewardPoints,
                    accent = Cyan,
                    success = answerRank == 0,
                )
            }

            if (mode == GameMode.LOGIT_SURPRISE) {
                GlassPanel(accent = Pink, padding = 11.dp) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text("HUMAN EXPECTATION", color = Pink, fontSize = 7.sp, fontWeight = FontWeight.Black, letterSpacing = 0.9.sp)
                            Text(humanExpected, color = TextMain, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                            Text("AI TOP-1", color = Cyan, fontSize = 7.sp, fontWeight = FontWeight.Black, letterSpacing = 0.9.sp)
                            Text(displayToken(top?.piece ?: "?"), color = Green, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (live && liveResult) {
                Text("VERIFIED LIVE / TOP-6 FROM REAL VOCABULARY SOFTMAX", color = Green, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 0.7.sp)
            }

            GlassPanel(accent = Cyan, padding = 12.dp) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("TOP-6 DISTRIBUTION", color = Cyan, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
                    Text("「${prompt.take(18)}…」", color = TextDim, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
                truthOrder.forEachIndexed { rank, index ->
                    val token = choices.getOrNull(index) ?: return@forEachIndexed
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("#${rank + 1}", color = if (rank == 0) Green else TextDim, fontSize = 8.sp, fontWeight = FontWeight.Black)
                        Text(
                            displayToken(token.piece),
                            color = if (rank == 0) Green else TextMain,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 8.dp).weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text("${"%.2f".format(token.probability * 100)}%", color = if (rank == 0) Green else TextSub, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    ProgressTrack((token.probability / ((top?.probability ?: 0.01f).coerceAtLeast(0.01f))).coerceIn(0f, 1f), if (rank == 0) Cyan else TextDim)
                }
            }
        }

        PrimaryAction(
            text = "RANDOM NEXT  →",
            accent = Cyan,
            onClick = onNext,
            modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 16.dp, vertical = 9.dp),
        )
    }
}
