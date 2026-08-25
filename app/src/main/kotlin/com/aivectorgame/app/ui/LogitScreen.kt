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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.random.Random

@Composable
internal fun LogitGame(onBack: () -> Unit) {
    val context = LocalContext.current
    val manager = remember { ModelManager(context) }
    val live = manager.isInstalled(ModelManager.CAUSAL) && NativeEngine.isNativeReady()
    val questions = if (live) GameData.logitLiveQuestions else GameData.logitDemoQuestions
    val haptic = LocalHapticFeedback.current
    var questionIndex by remember { mutableIntStateOf(0) }
    val question = questions[questionIndex % questions.size]
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
    val correctId = sorted.firstOrNull()?.tokenId
    val shuffled = remember(questionIndex, predictions) {
        predictions.shuffled(Random(question.prompt.hashCode() + predictions.joinToString { it.tokenId.toString() }.hashCode()))
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Header("Logit Mode", if (live) "LIVE • LFM2.5 230M" else "DEMO DATA", onBack)
        ScoreStrip(questionIndex + 1, score, streak, Cyan)

        if (live && liveResult) LiveProof("LIVE ENGINE • REAL MODEL LOGITS • DEMO TOKEN TABLE NOT USED")

        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.padding(20.dp)) {
                Text("次に来る確率が最も高いTOKENは？", color = TextSub, fontSize = 13.sp)
                Spacer(Modifier.height(10.dp))
                Text("「${question.prompt}▌」", color = TextMain, fontSize = 25.sp, fontWeight = FontWeight.Bold, lineHeight = 34.sp)
            }
        }

        if (loading) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Cyan)
                Spacer(Modifier.width(10.dp))
                Text("実Transformerをforward中…", color = TextSub)
            }
        }
        error?.let { Text("LIVE推論エラー: $it\nこのラウンドだけfallback値で続行します。", color = Red, fontSize = 12.sp) }

        shuffled.forEach { token ->
            val answered = selectedTokenId != null
            val container = when {
                answered && token.tokenId == correctId -> Green.copy(alpha = 0.20f)
                answered && token.tokenId == selectedTokenId -> Red.copy(alpha = 0.18f)
                else -> Panel2
            }
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = !loading && selectedTokenId == null,
                onClick = {
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
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = container,
                    contentColor = TextMain,
                    disabledContainerColor = container,
                    disabledContentColor = TextMain,
                ),
                shape = RoundedCornerShape(15.dp),
            ) {
                Text(displayToken(token.piece), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start, fontSize = 17.sp)
            }
        }

        selectedTokenId?.let {
            RewardCard(answerRank, rewardPoints, streak, Cyan, "LOGIT")
            Text("LOGITS → SOFTMAX → PROBABILITY", color = Cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            sorted.forEachIndexed { index, token -> ProbabilityBar(index + 1, token) }
            Text(
                "確率は語彙全体に対するSoftmax。Top-6だけを再正規化していません。",
                color = TextSub,
                fontSize = 11.sp,
                lineHeight = 16.sp,
            )
            Button(
                onClick = { questionIndex += 1 },
                colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Color.Black),
            ) {
                Text("NEXT TOKEN", fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
