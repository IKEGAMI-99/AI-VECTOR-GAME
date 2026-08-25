package com.aivectorgame.app.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aivectorgame.app.BuildConfig
import com.aivectorgame.app.ai.ModelManager
import com.aivectorgame.app.ai.UpdateManager
import kotlinx.coroutines.launch
import java.io.File

@Composable
internal fun UpdateCard() {
    val context = LocalContext.current
    val manager = remember { UpdateManager(context) }
    val scope = rememberCoroutineScope()
    var checking by remember { mutableStateOf(true) }
    var release by remember { mutableStateOf<UpdateManager.ReleaseInfo?>(null) }
    var message by remember { mutableStateOf("GitHubの最新版を確認中…") }
    var downloading by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var downloadedApk by remember { mutableStateOf<File?>(null) }

    fun checkNow() {
        checking = true
        message = "GitHubの最新版を確認中…"
        scope.launch {
            manager.checkLatest()
                .onSuccess {
                    release = it
                    message = if (it == null) {
                        "最新版です • v${BuildConfig.VERSION_NAME}"
                    } else {
                        "UPDATE AVAILABLE • v${it.version}"
                    }
                }
                .onFailure { message = "更新確認エラー: ${it.message}" }
            checking = false
        }
    }

    LaunchedEffect(Unit) { checkNow() }

    Card(
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, Cyan.copy(alpha = 0.18f), RoundedCornerShape(18.dp)),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("APP UPDATE", color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text(message, color = if (release != null) Green else TextSub, fontSize = 12.sp)
                }
                if (checking) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Cyan, strokeWidth = 2.dp)
            }

            if (downloading) {
                ProgressTrack(progress, Cyan)
                Text("APK取得中 ${(progress * 100).toInt()}%", color = TextSub, fontSize = 11.sp)
            }

            val found = release
            if (found != null && !checking) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (downloadedApk == null) {
                        Button(
                            onClick = {
                                downloading = true
                                progress = 0f
                                scope.launch {
                                    manager.download(found) { progress = it }
                                        .onSuccess {
                                            downloadedApk = it
                                            message = "v${found.version} を取得済み • インストールできます"
                                        }
                                        .onFailure { message = "更新DLエラー: ${it.message}" }
                                    downloading = false
                                }
                            },
                            enabled = !downloading,
                            colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Color.Black),
                        ) { Text("DOWNLOAD v${found.version}", fontWeight = FontWeight.Bold) }
                    } else {
                        Button(
                            onClick = {
                                when (manager.install(downloadedApk!!)) {
                                    UpdateManager.InstallResult.STARTED -> message = "Androidの更新画面を開きました"
                                    UpdateManager.InstallResult.PERMISSION_REQUIRED ->
                                        message = "インストール許可をONにして、戻ったら INSTALL をもう一度押してください"
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Color.Black),
                        ) { Text("INSTALL UPDATE", fontWeight = FontWeight.Bold) }
                    }
                    OutlinedButton(onClick = { checkNow() }, enabled = !downloading) {
                        Text("CHECK", color = TextMain)
                    }
                }
            }
        }
    }
}

@Composable
internal fun ModeCard(
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
                ProgressTrack(progress, accent)
                Text("モデル取得中 ${(progress * 100).toInt()}%", color = TextSub, fontSize = 13.sp)
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
