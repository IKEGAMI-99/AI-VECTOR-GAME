package com.aivectorgame.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.aivectorgame.app.game.GameFamily
import com.aivectorgame.app.game.GameMode

@Composable
fun AiVectorGameApp() {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Bg,
            surface = Panel,
            primary = Cyan,
            secondary = Purple,
            onBackground = TextMain,
            onSurface = TextMain,
        )
    ) {
        AtmosphereBackground {
            Box(
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
            ) {
                var mode by remember { mutableStateOf<GameMode?>(null) }
                val activeMode = mode
                if (activeMode == null) {
                    HomeScreen(onMode = { mode = it })
                } else {
                    when (activeMode.family) {
                        GameFamily.EMBEDDING -> EmbeddingGame(
                            mode = activeMode,
                            onBack = { mode = null },
                        )
                        GameFamily.LOGIT -> LogitGame(
                            mode = activeMode,
                            onBack = { mode = null },
                        )
                    }
                }
            }
        }
    }
}
