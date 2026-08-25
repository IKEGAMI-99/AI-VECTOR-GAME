package com.aivectorgame.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aivectorgame.app.BuildConfig
import com.aivectorgame.app.ai.ModelManager
import com.aivectorgame.app.game.GameMode
import kotlinx.coroutines.launch

@Composable
internal fun HomeScreen(onMode: (GameMode) -> Unit) {
    val context = LocalContext.current
    val modelManager = remember { ModelManager(context) }
    var embeddingInstalled by remember { mutableStateOf(modelManager.isInstalled(ModelManager.EMBEDDING)) }
    var causalInstalled by remember { mutableStateOf(modelManager.isInstalled(ModelManager.CAUSAL)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("AI//VECTOR", color = TextMain, fontSize = 22.sp, fontWeight = FontWeight.Black, letterSpacing = 0.7.sp)
                Text("RANDOMIZED ON-DEVICE INTELLIGENCE GAME", color = TextDim, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.0.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                ThemeTogglePill()
                Box(
                    Modifier
                        .background(Panel2.copy(alpha = 0.88f), RoundedCornerShape(100.dp))
                        .border(1.dp, GlassStroke, RoundedCornerShape(100.dp))
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                ) {
                    Text("v${BuildConfig.VERSION_NAME}", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        GlassPanel(accent = Purple, padding = 16.dp) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("LIVE MODEL LAB", color = Purple, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.4.sp)
                    Text("6 MODES / ∞ ROUNDS", color = TextMain, fontSize = 26.sp, fontWeight = FontWeight.Black)
                    Text("問題は端末内で毎ラウンド再構成。固定順は廃止。", color = TextSub, fontSize = 11.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("LOCAL", color = Green, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    Text("NO CLOUD", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
            }
        }

        UpdateCard()

        ModuleDeck(
            eyebrow = "SEMANTIC SPACE",
            title = "EMBEDDING",
            accent = Purple,
            spec = ModelManager.EMBEDDING,
            installed = embeddingInstalled,
            modes = listOf(
                GameMode.EMBEDDING_NEAREST,
                GameMode.EMBEDDING_FARTHEST,
                GameMode.EMBEDDING_RANKING,
            ),
            modelManager = modelManager,
            onInstalled = { embeddingInstalled = true },
            onMode = onMode,
        )

        ModuleDeck(
            eyebrow = "CAUSAL PREDICTION",
            title = "LOGIT",
            accent = Cyan,
            spec = ModelManager.CAUSAL,
            installed = causalInstalled,
            modes = listOf(
                GameMode.LOGIT_TOP_TOKEN,
                GameMode.LOGIT_RANKING,
                GameMode.LOGIT_SURPRISE,
            ),
            modelManager = modelManager,
            onInstalled = { causalInstalled = true },
            onMode = onMode,
        )

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("LIVE = 実モデル採点", color = TextDim, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text("DEMO = ローカル擬似採点", color = TextDim, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ModuleDeck(
    eyebrow: String,
    title: String,
    accent: Color,
    spec: ModelManager.ModelSpec,
    installed: Boolean,
    modes: List<GameMode>,
    modelManager: ModelManager,
    onInstalled: () -> Unit,
    onMode: (GameMode) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var downloading by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var error by remember { mutableStateOf<String?>(null) }

    GlassPanel(accent = accent, padding = 15.dp) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(eyebrow, color = accent, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
                Text(title, color = TextMain, fontSize = 24.sp, fontWeight = FontWeight.Black)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(if (installed) "LIVE READY" else "DEMO READY", color = if (installed) Green else Yellow, fontSize = 9.sp, fontWeight = FontWeight.Black)
                Text("~${spec.approxMb} MB", color = TextDim, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            modes.forEach { mode ->
                ModeLaunchButton(
                    mode = mode,
                    accent = accent,
                    modifier = Modifier.weight(1f),
                    onClick = { onMode(mode) },
                )
            }
        }

        if (downloading) {
            ProgressTrack(progress, accent)
            Text("MODEL ${(progress * 100).toInt()}%", color = TextDim, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        } else if (!installed) {
            val shape = RoundedCornerShape(13.dp)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(shape)
                    .background(accent.copy(alpha = 0.08f))
                    .border(1.dp, accent.copy(alpha = 0.20f), shape)
                    .clickable {
                        error = null
                        downloading = true
                        scope.launch {
                            modelManager.download(spec) { progress = it }
                                .onSuccess { onInstalled() }
                                .onFailure { error = it.message ?: "Download failed" }
                            downloading = false
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text("GET ${spec.title}  //  ENABLE LIVE", color = accent, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 0.7.sp)
            }
        }

        if (downloading) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CircularProgressIndicator(modifier = Modifier.height(14.dp), color = accent, strokeWidth = 2.dp)
                Text(spec.subtitle, color = TextDim, fontSize = 9.sp)
            }
        }
        error?.let { Text(it, color = Red, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
    }
}

@Composable
private fun ModeLaunchButton(
    mode: GameMode,
    accent: Color,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(15.dp)
    Column(
        modifier
            .height(68.dp)
            .clip(shape)
            .background(Panel.copy(alpha = 0.86f))
            .border(1.dp, accent.copy(alpha = 0.20f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(mode.code, color = accent, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 0.6.sp)
        Text(mode.shortTitle, color = TextMain, fontSize = 11.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
