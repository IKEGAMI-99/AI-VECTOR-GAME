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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aivectorgame.app.ai.ModelManager
import com.aivectorgame.app.ai.NativeEngine
import com.aivectorgame.app.game.GameData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.random.Random

private enum class TokenStage { QUESTION, RESULT }

@Composable
internal fun LogitGame(onBack: () -> Unit) {
    val context = LocalContext.current
    val manager = remember { ModelManager(context) }
    val live = manager.isInstalled(ModelManager.CAUSAL) && NativeEngine.isNativeReady()
    val questions = if (live) GameData.logitLiveQuestions else GameData.logitDemoQuestions
    val haptic = LocalHapticFeedback.current

    var questionIndex by remember { mutableIntStateOf(0) }
    val question = questions[questionIndex % questions.size]
    var stage by remember(questionIndex) { mutableStateOf(TokenStage.QUESTION) }
    var predictions by remember(questionIndex) { mutableStateOf(question.demoPredictions) }
    var loading by remember(questionIndex) { mutableStateOf(live) }
    var error by remember(questionIndex) { mutableStateOf<String?>(null) }
    var liveResult by remember(questionIndex) { mutableStateOf(false) }
    var selectedTokenId by remember(questionIndex) { mutableStateOf<Int?>(null) }
    var score by remember { mutableIntStateOf(0) }
    var streak by remember { mutableIntStateOf(0) }
    var rewardPoints by remember(questionIndex) { mutableIntStateOf(0) }
    var answerRank by remember(questionIndex) { mutableIntStateOf(-1) }

    LaunchedEffect(questionIndex, live) {
        if (!live) return@LaunchedEffect
        loading = true
        val result = withContext(Dispatchers.IO) {
            runCatching {
                if (!NativeEngine.isCausalLoaded()) {
                    NativeEngine.loadCausal(manager.fileFor(ModelManager.CAUSAL)).getOrThrow()
                }
                NativeEngine.topTokens(question.prompt, 6).getOrThrow().also {
                    check(it.size >= 2) { "Not enough displayable tokens returned" }
                }
            }
        }
        result.onSuccess {
            predictions = it
            liveResult = true
        }.onFailure {
            error = it.message ?: "Logit inference failed"
            liveResult = false
        }
        loading = false
    }

    val sorted = predictions.sortedByDescending { it.probability }
    val shuffled = remember(questionIndex, predictions) {
        predictions.shuffled(Random(question.prompt.hashCode() + predictions.joinToString { it.tokenId.toString() }.hashCode()))
    }

    when (stage) {
        TokenStage.QUESTION -> TokenQuestionPage(
            round = questionIndex + 1,
            score = score,
            streak = streak,
            prompt = question.prompt,
            choices = shuffled,
            live = live,
            liveResult = liveResult,
            loading = loading,
            error = error,
            onBack = onBack,
            onChoose = { token ->
                selectedTokenId = token.tokenId
                val rank = sorted.indexOfFirst { it.tokenId == token.tokenId }.coerceAtLeast(0)
                answerRank = rank
                val gained = when (rank) {
                    0 -> 150 + min(streak, 5) * 25
                    1 -> 70
                    2 -> 35
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
                stage = TokenStage.RESULT
            },
        )

        TokenStage.RESULT -> TokenResultPage(
            round = questionIndex + 1,
            score = score,
            streak = streak,
            prompt = question.prompt,
            sorted = sorted,
            selectedTokenId = selectedTokenId,
            answerRank = answerRank,
            rewardPoints = rewardPoints,
            live = live,
            liveResult = liveResult,
            onBack = onBack,
            onNext = { questionIndex += 1 },
        )
    }
}

@Composable
private fun TokenQuestionPage(
    round: Int,
    score: Int,
    streak: Int,
    prompt: String,
    choices: List<NativeEngine.TokenPrediction>,
    live: Boolean,
    liveResult: Boolean,
    loading: Boolean,
    error: String?,
    onBack: () -> Unit,
    onChoose: (NativeEngine.TokenPrediction) -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        GameTopBar("Token / Prediction", if (live) "LFM2.5 230M" else "DEMO DATA", Cyan, onBack)
        ScoreHud(round, score, streak, Cyan)

        if (live && liveResult) {
            LiveBadge("LIVE ENGINE  //  REAL FINAL-POSITION LOGITS")
        } else if (error != null) {
            LiveBadge("LIVE FALLBACK  //  ${error.take(48)}", isLive = false)
        }

        GlassPanel(accent = Cyan, padding = 22.dp) {
            Text("CAUSAL PROMPT", color = Cyan, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.8.sp)
            Text("「$prompt▌」", color = TextMain, fontSize = 27.sp, lineHeight = 36.sp, fontWeight = FontWeight.Black)
            Text("次の位置でlogitが最大になるtokenを選択", color = TextSub, fontSize = 12.sp)
        }

        if (loading) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Cyan, strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text("Transformer forward pass…", color = TextSub, fontSize = 11.sp)
            }
        }

        Text("TOKEN CANDIDATES / 06", color = TextDim, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
        choices.forEachIndexed { index, token ->
            ChoiceTile(
                index = index,
                text = displayToken(token.piece),
                accent = Cyan,
                enabled = !loading,
                onClick = { onChoose(token) },
            )
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun TokenResultPage(
    round: Int,
    score: Int,
    streak: Int,
    prompt: String,
    sorted: List<NativeEngine.TokenPrediction>,
    selectedTokenId: Int?,
    answerRank: Int,
    rewardPoints: Int,
    live: Boolean,
    liveResult: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    val top = sorted.firstOrNull()
    val selected = sorted.firstOrNull { it.tokenId == selectedTokenId }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 12.dp)
                .padding(bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            GameTopBar("Token / Result", "LOGITS → SOFTMAX → PROBABILITY", Cyan, onBack)
            ScoreHud(round, score, streak, Cyan)

            ResultHero(
                rank = answerRank,
                points = rewardPoints,
                streak = streak,
                accent = Cyan,
                mode = "LOGIT",
                answer = displayToken(top?.piece ?: "?"),
                selected = displayToken(selected?.piece ?: "?"),
            )

            if (live && liveResult) LiveBadge("VERIFIED LIVE  //  DEMO TOKEN TABLE NOT USED")

            GlassPanel(accent = Cyan, padding = 18.dp) {
                Text("MODEL CONFIDENCE", color = Cyan, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                    Text("${"%.1f".format((top?.probability ?: 0f) * 100)}", color = TextMain, fontSize = 46.sp, fontWeight = FontWeight.Black)
                    Text("%", color = Cyan, fontSize = 18.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(bottom = 7.dp))
                    Spacer(Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.End) {
                        Text("TOP TOKEN", color = TextDim, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Text(displayToken(top?.piece ?: "?"), color = Green, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    }
                }
                Text("「$prompt…」の次位置に対する語彙全体Softmax", color = TextSub, fontSize = 11.sp)
            }

            GlassPanel(accent = Cyan, padding = 16.dp) {
                Text("TOP-6 DISTRIBUTION", color = Cyan, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
                sorted.forEachIndexed { index, token -> ProbabilityBar(index + 1, token, Cyan) }
                Text(
                    "Top-6だけで再正規化せず、語彙全体に対する実Softmax確率を表示。",
                    color = TextDim,
                    fontSize = 9.sp,
                    lineHeight = 14.sp,
                )
            }
        }

        PrimaryAction(
            text = "NEXT TOKEN  →",
            accent = Cyan,
            onClick = onNext,
            modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 18.dp, vertical = 12.dp),
        )
    }
}
