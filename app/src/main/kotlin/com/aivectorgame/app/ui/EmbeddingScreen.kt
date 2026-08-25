package com.aivectorgame.app.ui

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aivectorgame.app.ai.ModelManager
import com.aivectorgame.app.ai.NativeEngine
import com.aivectorgame.app.game.GameData
import com.aivectorgame.app.math.MdsProjector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min

@Composable
internal fun EmbeddingGame(onBack: () -> Unit) {
    val context = LocalContext.current
    val manager = remember { ModelManager(context) }
    val live = manager.isInstalled(ModelManager.EMBEDDING) && NativeEngine.isNativeReady()
    val questions = if (live) GameData.embeddingLiveQuestions else GameData.embeddingDemoQuestions
    val haptic = LocalHapticFeedback.current
    var questionIndex by remember { mutableIntStateOf(0) }
    val question = questions[questionIndex % questions.size]
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

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Header("Embedding Mode", if (live) "LIVE • LFM2.5 Embedding 350M" else "DEMO DATA", onBack)
        ScoreStrip(questionIndex + 1, score, streak, Purple)

        if (live && liveResult) LiveProof("LIVE ENGINE • llama.cpp/JNI • DEMO SCORE TABLE NOT USED")

        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("最も近い単語は？", color = TextSub, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                Text(question.target, color = TextMain, fontSize = 42.sp, fontWeight = FontWeight.Black)
            }
        }

        if (loading) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Purple)
                Spacer(Modifier.width(10.dp))
                Text("実モデルでEmbeddingを計算中…", color = TextSub)
            }
        }
        error?.let { Text("LIVE推論エラー: $it\nこのラウンドだけfallback値で続行します。", color = Red, fontSize = 12.sp) }

        question.choices.forEachIndexed { index, choice ->
            val answered = selected != null
            val container = when {
                answered && index == bestIndex -> Green.copy(alpha = 0.20f)
                answered && index == selected -> Red.copy(alpha = 0.18f)
                else -> Panel2
            }
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = !loading && selected == null,
                onClick = {
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
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = container,
                    contentColor = TextMain,
                    disabledContainerColor = container,
                    disabledContentColor = TextMain,
                ),
                shape = RoundedCornerShape(15.dp),
            ) {
                Text(choice, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start, fontSize = 17.sp)
            }
        }

        selected?.let {
            RewardCard(answerRank, rewardPoints, streak, Purple, "VECTOR")
            Text("正解: ${question.choices[bestIndex]}  •  cosine ${"%.3f".format(scores[bestIndex])}", color = TextSub)
            Text("${vectors.firstOrNull()?.size ?: 0}D → 3D CLASSICAL MDS", color = Purple, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            VectorCloud(
                labels = listOf(question.target) + question.choices,
                points = MdsProjector.project(vectors),
                scores = listOf(1f) + scores,
            )
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                ranking.forEachIndexed { rank, i ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("#${rank + 1}  ${question.choices[i]}", color = TextMain)
                        Text("${"%.3f".format(scores[i])}", color = TextSub)
                    }
                }
            }
            Button(
                onClick = { questionIndex += 1 },
                colors = ButtonDefaults.buttonColors(containerColor = Purple, contentColor = Color.White),
            ) {
                Text("NEXT VECTOR", fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
