package com.aivectorgame.app.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

private enum class Screen { HOME, EMBEDDING, LOGIT }

@Composable
fun AiVectorGameApp() {
    MaterialTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
            color = Bg,
        ) {
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
