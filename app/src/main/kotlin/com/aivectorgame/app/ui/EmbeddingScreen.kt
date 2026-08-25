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
import com.aivectorgame.app.math.MdsProjector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min

private enum class VectorStage { QUESTION, RESULT }

@Composable
internal fun EmbeddingGame(onBack: () -> Unit) {
    val context = LocalContext.current
    val manager = remember { ModelManager(context) }
    val live = manager.isInstalled(ModelManager.EMBEDDING) && NativeEngine.isNativeReady()
    val questions = if (live) GameData.embeddingLiveQuestions else GameData.embeddingDemoQuestions
    val haptic = LocalHapticFeedback.current

    var questionIndex by remember { mutableIntStateOf(0) }
    val question = questions[questionIndex % questions.size]
    var stage by remember(questionIndex) { mutableStateOf(VectorStage.QUESTION) }
    var selected by remember(questionIndex) { mutableStateOf<Int?>(null) }
    var loading by remember(questionIndex) { mutableStateOf(live) }
    var error by remember(questionIndex) { mutableStateOf<String?>(null) }
    var liveResult by remember(questionIndex) { mutableStateOf(false) }
    var scores by remember(questionIndex) { mutableStateOf(question.demoScores) }
    var vectors by remember(questionIndex) { mutableStateOf(MdsProjector.demoVectors(question.demoScores)) }
    var score by remember { mutableIntStateOf(0) }
    var streak by remember { mutableIntStateOf(0) }
    var rewardPoints by remember(questionIndex) { mutableIntStateOf(0) }
    var answerRank by remember(questionIndex) { mutableIntStateOf(-1) }

    LaunchedEffect(questionIndex, live) {
        if (!live) return@LaunchedEffect
        loading = true
        val result = withContext(Dispatchers.IO) {
            runCatching {
                if (!NativeEngine.isEmbeddingLoaded()) {
                    NativeEngine.loadEmbedding(manager.fileFor(ModelManager.EMBEDDING)).getOrThrow()
                }
                val target = NativeEngine.embedding("query: ${question.target}").getOrThrow()
                val candidateVectors = question.choices.map { NativeEngine.embedding("document: $it").getOrThrow() }
                val all = listOf(target) + candidateVectors
                val sims = candidateVectors.map { MdsProjector.cosine(target, it) }
                all to sims
            }
        }
        result.onSuccess { (all, sims) ->
            vectors = all
            scores = sims
            liveResult = true
        }.onFailure {
            error = it.message ?: "Embedding inference failed"
            liveResult = false
        }
        loading = false
    }

    val ranking = scores.indices.sortedByDescending { scores[it] }
    val bestIndex = ranking.firstOrNull() ?: 0

    when (stage) {
        VectorStage.QUESTION -> VectorQuestionPage(
            round = questionIndex + 1,
            score = score,
            streak = streak,
            target = question.target,
            choices = question.choices,
            live = live,
            liveResult = liveResult,
            loading = loading,
            error = error,
            onBack = onBack,
            onChoose = { index ->
                selected = index
                val rank = ranking.indexOf(index).coerceAtLeast(0)
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
            },
        )

        VectorStage.RESULT -> VectorResultPage(
            round = questionIndex + 1,
            score = score,
            streak = streak,
            target = question.target,
            choices = question.choices,
            selectedIndex = selected ?: 0,
            bestIndex = bestIndex,
            answerRank = answerRank,
            rewardPoints = rewardPoints,
            scores = scores,
            vectors = vectors,
            ranking = ranking,
            live = live,
            liveResult = liveResult,
            onBack = onBack,
            onNext = { questionIndex += 1 },
        )
    }
}

@Composable
private fun VectorQuestionPage(
    round: Int,
    score: Int,
    streak: Int,
    target: String,
    choices: List<String>,
    live: Boolean,
    liveResult: Boolean,
    loading: Boolean,
    error: String?,
    onBack: () -> Unit,
    onChoose: (Int) -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        GameTopBar("Vector / Semantic", if (live) "LFM2.5 EMBEDDING 350M" else "DEMO DATA", Purple, onBack)
        ScoreHud(round, score, streak, Purple)

        if (live && liveResult) {
            LiveBadge("LIVE ENGINE  //  REAL EMBEDDING VECTORS")
        } else if (error != null) {
            LiveBadge("LIVE FALLBACK  //  ${error.take(48)}", isLive = false)
        }

        GlassPanel(accent = Purple, padding = 22.dp) {
            Text("SEMANTIC TARGET", color = Purple, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.8.sp)
            Text(target, color = TextMain, fontSize = 48.sp, lineHeight = 52.sp, fontWeight = FontWeight.Black)
            Text("Embedding空間で最も近いvectorを選択", color = TextSub, fontSize = 12.sp)
        }

        if (loading) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Purple, strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text("1024D vectors computing…", color = TextSub, fontSize = 11.sp)
            }
        }

        Text("CANDIDATES / 06", color = TextDim, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
        choices.forEachIndexed { index, choice ->
            ChoiceTile(
                index = index,
                text = choice,
                accent = Purple,
                enabled = !loading,
                onClick = { onChoose(index) },
            )
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun VectorResultPage(
    round: Int,
    score: Int,
    streak: Int,
    target: String,
    choices: List<String>,
    selectedIndex: Int,
    bestIndex: Int,
    answerRank: Int,
    rewardPoints: Int,
    scores: List<Float>,
    vectors: List<FloatArray>,
    ranking: List<Int>,
    live: Boolean,
    liveResult: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 12.dp)
                .padding(bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            GameTopBar("Vector / Result", "${vectors.firstOrNull()?.size ?: 0}D → 3D MDS", Purple, onBack)
            ScoreHud(round, score, streak, Purple)

            ResultHero(
                rank = answerRank,
                points = rewardPoints,
                streak = streak,
                accent = Purple,
                mode = "VECTOR",
                answer = choices.getOrElse(bestIndex) { "?" },
                selected = choices.getOrElse(selectedIndex) { "?" },
            )

            if (live && liveResult) LiveBadge("VERIFIED LIVE  //  DEMO SCORE TABLE NOT USED")

            VectorCloud(
                labels = listOf(target) + choices,
                points = MdsProjector.project(vectors),
                scores = listOf(1f) + scores,
            )

            GlassPanel(accent = Purple, padding = 16.dp) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("COSINE RANK", color = Purple, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
                    Text("TARGET / $target", color = TextDim, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
                ranking.forEachIndexed { rank, i ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${(rank + 1).toString().padStart(2, '0')}  ${choices[i]}", color = if (rank == 0) Green else TextMain, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("${"%.3f".format(scores[i])}", color = if (rank == 0) Green else TextSub, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        PrimaryAction(
            text = "NEXT VECTOR  →",
            accent = Purple,
            onClick = onNext,
            modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 18.dp, vertical = 12.dp),
        )
    }
}
