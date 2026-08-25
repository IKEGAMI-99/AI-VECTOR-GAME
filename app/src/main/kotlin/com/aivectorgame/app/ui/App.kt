package com.aivectorgame.app.ui

import android.graphics.Paint
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aivectorgame.app.BuildConfig
import com.aivectorgame.app.ai.ModelManager
import com.aivectorgame.app.ai.NativeEngine
import com.aivectorgame.app.game.GameData
import com.aivectorgame.app.math.MdsProjector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

private val Bg = Color(0xFF080B12)
private val Panel = Color(0xFF111827)
private val Panel2 = Color(0xFF172033)
private val TextMain = Color(0xFFF3F4F6)
private val TextSub = Color(0xFFA8B0C0)
private val Purple = Color(0xFF8B5CF6)
private val Cyan = Color(0xFF22D3EE)
private val Pink = Color(0xFFF472B6)
private val Green = Color(0xFFA7F3D0)
private val Yellow = Color(0xFFFDE68A)
private val Red = Color(0xFFFCA5A5)

private enum class Screen { HOME, EMBEDDING, LOGIT }

@Composable
fun AiVectorGameApp() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Bg) {
            var screen by remember { mutableStateOf(Screen.HOME) }
            when (screen) {
                Screen.HOME -> HomeScreen(
                    onEmbedding = { screen = Screen.EMBEDDING },
                    onLogit = { screen = Screen.LOGIT },
                )
                Screen.EMBEDDING -> EmbeddingGame(onBack = { screen = Screen.HOME })
                Screen.LOGIT -> LogitGame(onBack = { screen = Screen.HOME })
            }
        }
    }
}

@Composable
private fun HomeScreen(onEmbedding: () -> Unit, onLogit: () -> Unit) {
    val context = LocalContext.current
    val modelManager = remember { ModelManager(context) }
    var embeddingInstalled by remember { mutableStateOf(modelManager.isInstalled(ModelManager.EMBEDDING)) }
    var causalInstalled by remember { mutableStateOf(modelManager.isInstalled(ModelManager.CAUSAL)) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(10.dp))
        Text("AI VECTOR GAME", color = TextMain, fontSize = 30.sp, fontWeight = FontWeight.Black)
        Text("VECTOR SPACE × NEXT TOKEN", color = Cyan, fontSize = 13.sp, letterSpacing = 1.5.sp)
        Text("v${BuildConfig.VERSION_NAME}  •  ON-DEVICE AI", color = TextSub, fontSize = 12.sp)

        Spacer(Modifier.height(8.dp))
        Text(
            "Embeddingの距離と、LLMの次トークン予測をゲームとして体験する。モデルは端末内で実行され、入力文章は外部へ送信しません。",
            color = TextSub,
            lineHeight = 22.sp,
        )

        ModeCard(
            eyebrow = "VECTOR SPACE",
            title = "Embedding Mode",
            description = "6つの候補から意味的に最も近い単語を当てる。回答後、実Embeddingを3D MDS空間へ展開。",
            accent = Purple,
            spec = ModelManager.EMBEDDING,
            installed = embeddingInstalled,
            modelManager = modelManager,
            onInstalled = { embeddingInstalled = true },
            onStart = onEmbedding,
        )

        ModeCard(
            eyebrow = "NEXT TOKEN",
            title = "Logit Mode",
            description = "文章の次に来るトークンを予測。実際のlogitをSoftmaxし、Top-6の確率分布を公開。",
            accent = Cyan,
            spec = ModelManager.CAUSAL,
            installed = causalInstalled,
            modelManager = modelManager,
            onInstalled = { causalInstalled = true },
            onStart = onLogit,
        )

        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("HOW IT WORKS", color = Yellow, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("Embedding", color = TextMain, fontWeight = FontWeight.Bold)
                Text("1024D vector → cosine similarity → classical MDS → interactive 3D", color = TextSub, fontSize = 13.sp)
                Text("Logit", color = TextMain, fontWeight = FontWeight.Bold)
                Text("Prompt → Transformer → logits → Softmax → Top-6 tokens", color = TextSub, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun ModeCard(
    eyebrow: String,
    title: String,
    description: String,
    accent: Color,
    spec: ModelManager.ModelSpec,
    installed: Boolean,
    modelManager: ModelManager,
    onInstalled: () -> Unit,
    onStart: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var downloading by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var error by remember { mutableStateOf<String?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth().border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Text(eyebrow, color = accent, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.4.sp)
            Text(title, color = TextMain, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(description, color = TextSub, lineHeight = 20.sp)
            Text("${spec.title}  •  約${spec.approxMb}MB", color = TextMain, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(spec.subtitle, color = TextSub, fontSize = 12.sp)

            if (downloading) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(progress = { progress }, modifier = Modifier.size(24.dp), strokeWidth = 3.dp, color = accent)
                    Spacer(Modifier.width(12.dp))
                    Text("モデル取得中 ${(progress * 100).toInt()}%", color = TextSub, fontSize = 13.sp)
                }
            } else if (installed) {
                Text("● MODEL READY", color = Green, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            } else {
                Text("モデル未取得。DEMO DATAでも遊べます。", color = Yellow, fontSize = 12.sp)
            }

            error?.let { Text(it, color = Red, fontSize = 12.sp) }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onStart, colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.Black)) {
                    Text(if (installed) "PLAY LIVE" else "PLAY DEMO", fontWeight = FontWeight.Bold)
                }
                if (!installed && !downloading) {
                    OutlinedButton(onClick = {
                        error = null
                        downloading = true
                        scope.launch {
                            val result = modelManager.download(spec) { progress = it }
                            downloading = false
                            result.onSuccess { onInstalled() }.onFailure { error = it.message ?: "Download failed" }
                        }
                    }) {
                        Text("MODEL GET", color = TextMain)
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(title: String, subtitle: String, onBack: () -> Unit) {
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
private fun EmbeddingGame(onBack: () -> Unit) {
    val context = LocalContext.current
    val manager = remember { ModelManager(context) }
    val live = manager.isInstalled(ModelManager.EMBEDDING) && NativeEngine.isNativeReady()
    var questionIndex by remember { mutableIntStateOf(0) }
    val question = GameData.embeddingQuestions[questionIndex % GameData.embeddingQuestions.size]
    var selected by remember(questionIndex) { mutableStateOf<Int?>(null) }
    var loading by remember(questionIndex) { mutableStateOf(live) }
    var error by remember(questionIndex) { mutableStateOf<String?>(null) }
    var scores by remember(questionIndex) { mutableStateOf(question.demoScores) }
    var vectors by remember(questionIndex) { mutableStateOf(MdsProjector.demoVectors(question.demoScores)) }

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
        }.onFailure {
            error = it.message ?: "Embedding inference failed"
        }
        loading = false
    }

    val bestIndex = scores.indices.maxByOrNull { scores[it] } ?: 0

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Header("Embedding Mode", if (live) "LIVE • LFM2.5 Embedding 350M" else "DEMO DATA", onBack)
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
                Text("1024次元Embeddingを計算中…", color = TextSub)
            }
        }
        error?.let { Text("LIVE推論エラー: $it\nDEMO DATAで続行します。", color = Red, fontSize = 12.sp) }

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
                onClick = { selected = index },
                colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = TextMain, disabledContainerColor = container, disabledContentColor = TextMain),
                shape = RoundedCornerShape(15.dp),
            ) {
                Text(choice, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start, fontSize = 17.sp)
            }
        }

        selected?.let { answer ->
            val correct = answer == bestIndex
            Text(if (correct) "✓ VECTOR MATCH" else "× CLOSE, BUT NOT CLOSEST", color = if (correct) Green else Pink, fontWeight = FontWeight.Black)
            Text("正解: ${question.choices[bestIndex]}  •  cosine ${"%.3f".format(scores[bestIndex])}", color = TextSub)
            Text("${vectors.firstOrNull()?.size ?: 0}D → 3D CLASSICAL MDS", color = Purple, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            VectorCloud(
                labels = listOf(question.target) + question.choices,
                points = MdsProjector.project(vectors),
                scores = listOf(1f) + scores,
            )
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                question.choices.indices.sortedByDescending { scores[it] }.forEach { i ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(question.choices[i], color = TextMain)
                        Text("${"%.3f".format(scores[i])}", color = TextSub)
                    }
                }
            }
            Button(onClick = { questionIndex += 1 }, colors = ButtonDefaults.buttonColors(containerColor = Purple, contentColor = Color.White)) {
                Text("NEXT VECTOR")
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun VectorCloud(labels: List<String>, points: List<MdsProjector.Point3>, scores: List<Float>) {
    var rotX by remember { mutableFloatStateOf(-0.28f) }
    var rotY by remember { mutableFloatStateOf(0.42f) }
    var zoom by remember { mutableFloatStateOf(1f) }
    val revealTarget = if (points.isNotEmpty()) 1f else 0f
    val reveal by animateFloatAsState(revealTarget, animationSpec = tween(900), label = "reveal")

    Box(
        Modifier.fillMaxWidth().height(390.dp).background(Color(0xFF070A11), RoundedCornerShape(22.dp))
            .border(1.dp, Purple.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
            .pointerInput(points) {
                detectTransformGestures { _, pan, gestureZoom, _ ->
                    rotY += pan.x * 0.008f
                    rotX += pan.y * 0.008f
                    zoom = (zoom * gestureZoom).coerceIn(0.65f, 2.2f)
                }
            }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val baseScale = min(size.width, size.height) * 0.27f * zoom
            val transformed = points.mapIndexed { i, p ->
                val x0 = p.x * reveal
                val y0 = p.y * reveal
                val z0 = p.z * reveal
                val cyy = cos(rotY); val syy = sin(rotY)
                val x1 = x0 * cyy + z0 * syy
                val z1 = -x0 * syy + z0 * cyy
                val cxx = cos(rotX); val sxx = sin(rotX)
                val y2 = y0 * cxx - z1 * sxx
                val z2 = y0 * sxx + z1 * cxx
                val perspective = 1f / (1.55f + z2 * 0.22f)
                Triple(Offset(cx + x1 * baseScale * perspective, cy + y2 * baseScale * perspective), z2, i)
            }

            if (transformed.isNotEmpty()) {
                val origin = transformed[0].first
                transformed.drop(1).forEach { (pos, _, i) ->
                    val sim = scores.getOrElse(i) { 0f }.coerceIn(-1f, 1f)
                    drawLine(Purple.copy(alpha = 0.12f + 0.35f * max(0f, sim)), origin, pos, strokeWidth = 2f, cap = StrokeCap.Round)
                }
            }

            transformed.sortedBy { it.second }.forEach { (pos, z, i) ->
                val radius = if (i == 0) 12f else 8f
                val color = when (i) {
                    0 -> Yellow
                    1 -> Purple
                    2 -> Cyan
                    3 -> Pink
                    4 -> Green
                    else -> TextSub
                }
                drawCircle(color.copy(alpha = 0.18f), radius = radius * 2.4f, center = pos)
                drawCircle(color, radius = radius, center = pos)
                drawCircle(Color.White.copy(alpha = 0.65f), radius = radius + 1f, center = pos, style = Stroke(1f))
                val paint = Paint().apply {
                    isAntiAlias = true
                    this.color = android.graphics.Color.WHITE
                    textSize = if (i == 0) 34f else 27f
                    textAlign = Paint.Align.CENTER
                    alpha = (220 + (z * 8).toInt()).coerceIn(140, 255)
                }
                drawContext.canvas.nativeCanvas.drawText(labels.getOrElse(i) { "?" }, pos.x, pos.y - radius - 11f, paint)
            }
        }
        Text("DRAG: ROTATE  •  PINCH: ZOOM", color = TextSub, fontSize = 10.sp, modifier = Modifier.align(Alignment.BottomCenter).padding(10.dp))
    }
}

@Composable
private fun LogitGame(onBack: () -> Unit) {
    val context = LocalContext.current
    val manager = remember { ModelManager(context) }
    val live = manager.isInstalled(ModelManager.CAUSAL) && NativeEngine.isNativeReady()
    var questionIndex by remember { mutableIntStateOf(0) }
    val question = GameData.logitQuestions[questionIndex % GameData.logitQuestions.size]
    var predictions by remember(questionIndex) { mutableStateOf(question.demoPredictions) }
    var loading by remember(questionIndex) { mutableStateOf(live) }
    var error by remember(questionIndex) { mutableStateOf<String?>(null) }
    var selectedTokenId by remember(questionIndex) { mutableStateOf<Int?>(null) }

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
        result.onSuccess { predictions = it }.onFailure {
            error = it.message ?: "Logit inference failed"
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
                Text("Transformer forward中…", color = TextSub)
            }
        }
        error?.let { Text("LIVE推論エラー: $it\nDEMO DATAで続行します。", color = Red, fontSize = 12.sp) }

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
                onClick = { selectedTokenId = token.tokenId },
                colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = TextMain, disabledContainerColor = container, disabledContentColor = TextMain),
                shape = RoundedCornerShape(15.dp),
            ) {
                Text(displayToken(token.piece), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start, fontSize = 17.sp)
            }
        }

        selectedTokenId?.let { selected ->
            val correct = selected == correctId
            Text(if (correct) "✓ TOP LOGIT" else "× NOT TOP-1", color = if (correct) Green else Pink, fontWeight = FontWeight.Black)
            Text("LOGITS → SOFTMAX → PROBABILITY", color = Cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            sorted.forEach { token -> ProbabilityBar(token) }
            Text(
                "確率は語彙全体に対するSoftmax。Top-6だけを再正規化していません。",
                color = TextSub,
                fontSize = 11.sp,
                lineHeight = 16.sp,
            )
            Button(onClick = { questionIndex += 1 }, colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Color.Black)) {
                Text("NEXT TOKEN", fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ProbabilityBar(token: NativeEngine.TokenPrediction) {
    val normalized = (token.probability / 0.60f).coerceIn(0f, 1f)
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(displayToken(token.piece), color = TextMain, fontSize = 13.sp)
            Text("${"%.2f".format(token.probability * 100)}%  logit ${"%.2f".format(token.logit)}", color = TextSub, fontSize = 12.sp)
        }
        Canvas(Modifier.fillMaxWidth().height(9.dp)) {
            drawRoundRect(Panel2, size = size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2))
            drawRoundRect(Cyan, size = Size(size.width * normalized, size.height), cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2))
        }
    }
}

private fun displayToken(piece: String): String {
    if (piece.isEmpty()) return "∅"
    return piece
        .replace(" ", "␠")
        .replace("\n", "↵")
        .replace("\t", "⇥")
}
