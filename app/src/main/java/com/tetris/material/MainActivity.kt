package com.tetris.material

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tetris.material.data.Prefs
import com.tetris.material.game.GameViewModel
import com.tetris.material.ui.GameScreen
import com.tetris.material.ui.HomeScreen
import com.tetris.material.ui.ScoresScreen
import com.tetris.material.ui.SettingsScreen
import com.tetris.material.ui.TetrisTheme
import com.tetris.material.ui.resolveDark

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = Prefs(this)

        setContent {
            val dark = resolveDark(prefs.themeMode)

            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT,
                    ) { dark },
                    navigationBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.argb(0xE6, 0xFF, 0xFF, 0xFF),
                        android.graphics.Color.argb(0x80, 0x1B, 0x1B, 0x1B),
                    ) { dark },
                )
                onDispose { }
            }

            TetrisTheme(dark = dark, dynamic = prefs.dynamicColor) {
                App(prefs)
            }
        }
    }
}

private enum class Screen { Home, Game, Scores, Settings }

@Composable
private fun App(prefs: Prefs) {
    val vm: GameViewModel = viewModel()
    var screen by rememberSaveable { mutableStateOf(Screen.Home) }

    // L'écran de jeu gère lui-même son bouton retour (il met en pause).
    BackHandler(enabled = screen == Screen.Scores || screen == Screen.Settings) {
        screen = Screen.Home
    }

    Crossfade(targetState = screen, label = "screen") { current ->
        when (current) {
            Screen.Home -> HomeScreen(
                vm = vm,
                prefs = prefs,
                onPlay = { screen = Screen.Game },
                onScores = { screen = Screen.Scores },
                onSettings = { screen = Screen.Settings },
            )
            Screen.Game -> GameScreen(vm = vm, prefs = prefs, onExit = { screen = Screen.Home })
            Screen.Scores -> ScoresScreen(prefs = prefs, onBack = { screen = Screen.Home })
            Screen.Settings -> SettingsScreen(prefs = prefs, onBack = { screen = Screen.Home })
        }
    }
}
