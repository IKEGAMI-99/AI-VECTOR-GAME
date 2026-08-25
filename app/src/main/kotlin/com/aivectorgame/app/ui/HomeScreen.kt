package com.aivectorgame.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("AI VECTOR GAME", color = TextMain, fontSize = 30.sp, fontWeight = FontWeight.Black)
        Text("VECTOR SPACE × NEXT TOKEN", color = Cyan, fontSize = 13.sp, letterSpacing = 1.5.sp)
        Text("v${BuildConfig.VERSION_NAME}  •  ON-DEVICE AI", color = TextSub, fontSize = 12.sp)

        UpdateCard()

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
