package com.aivectorgame.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
    var message by remember { mutableStateOf("GitHub releaseを同期中") }
    var downloading by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var downloadedApk by remember { mutableStateOf<File?>(null) }

    fun checkNow() {
        checking = true
        message = "GitHub releaseを同期中"
        scope.launch {
            manager.checkLatest()
                .onSuccess {
                    release = it
                    message = if (it == null) {
                        "SYSTEM CURRENT  //  v${BuildConfig.VERSION_NAME}"
                    } else {
                        "NEW BUILD  //  v${it.version}"
                    }
                }
                .onFailure { message = "UPDATE LINK ERROR  //  ${it.message}" }
            checking = false
        }
    }

    LaunchedEffect(Unit) { checkNow() }

    GlassPanel(accent = if (release != null) Green else Cyan, padding = 14.dp) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(38.dp)
                    .background(Cyan.copy(alpha = 0.08f), CircleShape)
                    .border(1.dp, Cyan.copy(alpha = 0.20f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("↻", color = Cyan, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text("APP CHANNEL / GITHUB", color = TextDim, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
                Text(message, color = if (release != null) Green else TextMain, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            if (checking) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Cyan, strokeWidth = 2.dp)
        }

        if (downloading) {
            ProgressTrack(progress, Cyan)
            Text("PACKAGE ${(progress * 100).toInt()}%", color = TextDim, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
                                        message = "v${found.version} PACKAGE READY"
                                    }
                                    .onFailure { message = "DOWNLOAD ERROR  //  ${it.message}" }
                                downloading = false
                            }
                        },
                        enabled = !downloading,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = ActionText),
                    ) { Text("GET v${found.version}", fontWeight = FontWeight.Black, fontSize = 11.sp) }
                } else {
                    Button(
                        onClick = {
                            when (manager.install(downloadedApk!!)) {
                                UpdateManager.InstallResult.STARTED -> message = "ANDROID INSTALLER OPENED"
                                UpdateManager.InstallResult.PERMISSION_REQUIRED ->
                                    message = "ALLOW THIS SOURCE, THEN TAP INSTALL AGAIN"
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = ActionText),
                    ) { Text("INSTALL UPDATE", fontWeight = FontWeight.Black, fontSize = 11.sp) }
                }
                OutlinedButton(onClick = { checkNow() }, enabled = !downloading, shape = RoundedCornerShape(14.dp)) {
                    Text("CHECK", color = TextMain, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
internal fun ModeCard(
    index: String,
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

    GlassPanel(accent = accent, padding = 20.dp) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column {
                Text("$index / $eyebrow", color = accent, fontWeight = FontWeight.Black, fontSize = 10.sp, letterSpacing = 1.3.sp)
                Text(title, color = TextMain, fontSize = 35.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.7).sp)
            }
            Box(
                Modifier
                    .background(accent.copy(alpha = 0.08f), RoundedCornerShape(100.dp))
                    .border(1.dp, accent.copy(alpha = 0.20f), RoundedCornerShape(100.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(if (installed) "LIVE READY" else "DEMO READY", color = if (installed) Green else Yellow, fontSize = 9.sp, fontWeight = FontWeight.Black)
            }
        }

        Text(description, color = TextSub, fontSize = 14.sp, lineHeight = 20.sp)

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Column {
                Text(spec.title, color = TextMain, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(spec.subtitle, color = TextDim, fontSize = 10.sp)
            }
            Text("~${spec.approxMb} MB", color = TextSub, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        if (downloading) {
            ProgressTrack(progress, accent)
            Text("MODEL PACKAGE ${(progress * 100).toInt()}%", color = TextDim, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }

        error?.let { Text(it, color = Red, fontSize = 11.sp) }

        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Button(
                onClick = onStart,
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = ActionText),
            ) {
                Text(if (installed) "ENTER LIVE" else "ENTER DEMO", fontWeight = FontWeight.Black, fontSize = 12.sp)
            }
            if (!installed && !downloading) {
                OutlinedButton(
                    onClick = {
                        error = null
                        downloading = true
                        scope.launch {
                            val result = modelManager.download(spec) { progress = it }
                            downloading = false
                            result.onSuccess { onInstalled() }.onFailure { error = it.message ?: "Download failed" }
                        }
                    },
                    shape = RoundedCornerShape(15.dp),
                ) {
                    Text("GET MODEL", color = TextMain, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
