package com.tetris.material.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback

/** Retour haptique désactivé partout : aucun composant ne peut faire vibrer. */
private object NoHaptics : HapticFeedback {
    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) = Unit
}

@Composable
fun TetrisTheme(dark: Boolean, dynamic: Boolean, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val scheme = when {
        dynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }
    CompositionLocalProvider(LocalHapticFeedback provides NoHaptics) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}

@Composable
fun resolveDark(mode: Int): Boolean = when (mode) {
    1 -> false
    2 -> true
    else -> isSystemInDarkTheme()
}

private val lightPalette = listOf(
    Color(0xFF00ACC1), // I
    Color(0xFFFBC02D), // O
    Color(0xFF8E24AA), // T
    Color(0xFF43A047), // S
    Color(0xFFE53935), // Z
    Color(0xFF1E88E5), // J
    Color(0xFFFB8C00), // L
)

private val darkPalette = listOf(
    Color(0xFF4DD0E1),
    Color(0xFFFFD54F),
    Color(0xFFBA68C8),
    Color(0xFF81C784),
    Color(0xFFE57373),
    Color(0xFF64B5F6),
    Color(0xFFFFB74D),
)

/** Couleurs des 7 pièces, légèrement teintées par la couleur principale du thème. */
@Composable
fun pieceColors(): List<Color> {
    val scheme = MaterialTheme.colorScheme
    val palette = if (scheme.background.luminance() < 0.5f) darkPalette else lightPalette
    return palette.map { lerp(it, scheme.primary, 0.18f) }
}
