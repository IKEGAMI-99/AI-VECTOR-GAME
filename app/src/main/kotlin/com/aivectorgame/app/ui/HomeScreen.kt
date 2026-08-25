package com.aivectorgame.app.ui

import androidx.compose.foundation.background
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aivectorgame.app.BuildConfig
import com.aivectorgame.app.ai.ModelManager

@Composable
internal fun HomeScreen(onEmbedding: () -> Unit, onLogit: () -> Unit) {
    val context = LocalContext.current
    val modelManager = remember { ModelManager(context) }
    var embeddingInstalled by remember { mutableStateOf(modelManager.isInstalled(ModelManager.EMBEDDING)) }
    var causalInstalled by remember { mutableStateOf(modelManager.isInstalled(ModelManager.CAUSAL)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("AI//VECTOR", color = TextMain, fontSize = 20.sp, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp)
                Text("ON-DEVICE INTELLIGENCE GAME", color = TextDim, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            }
            Box(
                Modifier
                    .background(Panel2.copy(alpha = 0.78f), RoundedCornerShape(100.dp))
                    .padding(horizontal = 11.dp, vertical = 7.dp)
            ) {
                Text("v${BuildConfig.VERSION_NAME}", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        GlassPanel(accent = Purple, padding = 22.dp) {
            Text("LOCAL INTELLIGENCE LAB", color = Purple, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            Text("VECTOR\n/ TOKEN", color = TextMain, fontSize = 44.sp, lineHeight = 43.sp, fontWeight = FontWeight.Black, letterSpacing = (-1.5).sp)
            Text(
                "意味空間を読む。次のtokenを読む。\nAIの内部出力そのものをゲームにする。",
                color = TextSub,
                fontSize = 14.sp,
                lineHeight = 21.sp,
            )
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HeroTag("100% LOCAL", Green)
                HeroTag("NO CLOUD", Cyan)
                HeroTag("REAL MODEL", Purple)
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Purple, Cyan, Green, Purple.copy(alpha = 0f))
                        ),
                        RoundedCornerShape(100.dp),
                    )
            )
        }

        UpdateCard()

        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("SELECT MODULE", color = TextMain, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
            Text("モデルの違う2つの知覚をプレイする", color = TextDim, fontSize = 11.sp)
        }

        ModeCard(
            index = "01",
            eyebrow = "SEMANTIC SPACE",
            title = "VECTOR",
            description = "6つの候補から、Embedding空間で最も近い単語をロック。結果は高次元ベクトルを3Dへ射影して可視化。",
            accent = Purple,
            spec = ModelManager.EMBEDDING,
            installed = embeddingInstalled,
            modelManager = modelManager,
            onInstalled = { embeddingInstalled = true },
            onStart = onEmbedding,
        )

        ModeCard(
            index = "02",
            eyebrow = "CAUSAL PREDICTION",
            title = "TOKEN",
            description = "文章の次に来るtokenを予測。実LLMのlogitとSoftmax確率をそのまま勝敗判定に使う。",
            accent = Cyan,
            spec = ModelManager.CAUSAL,
            installed = causalInstalled,
            modelManager = modelManager,
            onInstalled = { causalInstalled = true },
            onStart = onLogit,
        )

        GlassPanel(accent = GlassStroke, padding = 16.dp) {
            Text("PIPELINE", color = Yellow, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.6.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TechNode("1024D", "EMBED")
                Text("→", color = TextDim)
                TechNode("COS", "SIM")
                Text("→", color = TextDim)
                TechNode("3D", "MDS")
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TechNode("PROMPT", "INPUT")
                Text("→", color = TextDim)
                TechNode("LOGIT", "RAW")
                Text("→", color = TextDim)
                TechNode("TOP-6", "SOFTMAX")
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun HeroTag(text: String, color: androidx.compose.ui.graphics.Color) {
    Box(
        Modifier
            .background(color.copy(alpha = 0.08f), RoundedCornerShape(100.dp))
            .padding(horizontal = 9.dp, vertical = 6.dp)
    ) {
        Text(text, color = color, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 0.7.sp)
    }
}

@Composable
private fun TechNode(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = TextMain, fontSize = 12.sp, fontWeight = FontWeight.Black)
        Text(label, color = TextDim, fontSize = 7.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
    }
}
