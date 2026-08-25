package com.aivectorgame.app.ui

import android.app.Activity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat
import com.aivectorgame.app.game.GameFamily
import com.aivectorgame.app.game.GameMode

@Composable
fun AiVectorGameApp() {
    val context = LocalContext.current
    remember(context) {
        ThemeController.load(context)
        true
    }
    val light = ThemeController.isLight
    val systemDensity = LocalDensity.current
    val appDensity = Density(systemDensity.density, systemDensity.fontScale * 1.08f)

    val scheme = if (light) {
        lightColorScheme(
            background = Bg,
            surface = Panel,
            primary = Purple,
            secondary = Cyan,
            onBackground = TextMain,
            onSurface = TextMain,
        )
    } else {
        darkColorScheme(
            background = Bg,
            surface = Panel,
            primary = Cyan,
            secondary = Purple,
            onBackground = TextMain,
            onSurface = TextMain,
        )
    }

    SideEffect {
        val activity = context as? Activity ?: return@SideEffect
        val window = activity.window
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = light
        controller.isAppearanceLightNavigationBars = light
        @Suppress("DEPRECATION")
        run {
            window.statusBarColor = Bg.toArgb()
            window.navigationBarColor = Bg.toArgb()
        }
    }

    CompositionLocalProvider(LocalDensity provides appDensity) {
        MaterialTheme(colorScheme = scheme) {
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
}
