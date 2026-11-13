package com.tetris.material.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tetris.material.R
import com.tetris.material.data.Prefs
import com.tetris.material.game.GameViewModel
import com.tetris.material.game.Phase

@Composable
fun HomeScreen(
    vm: GameViewModel,
    prefs: Prefs,
    onPlay: () -> Unit,
    onScores: () -> Unit,
    onSettings: () -> Unit,
) {
    val colors = pieceColors()
    val paused = vm.ui.phase == Phase.Paused
    val best = prefs.scores.firstOrNull()?.score

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                buildAnnotatedString {
                    "TETRIS".forEachIndexed { i, ch ->
                        withStyle(SpanStyle(color = colors[i % colors.size])) { append(ch) }
                    }
                },
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 4.sp,
                ),
            )
            Text(
                stringResource(R.string.subtitle),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            if (best != null) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ) {
                    Text(
                        stringResource(R.string.best, best),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
            Spacer(Modifier.height(32.dp))

            val buttonModifier = Modifier
                .widthIn(max = 360.dp)
                .fillMaxWidth()
                .height(56.dp)

            Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Button(
                    onClick = {
                        if (paused) vm.resume() else vm.newGame()
                        onPlay()
                    },
                    modifier = buttonModifier,
                ) {
                    Text(
                        stringResource(if (paused) R.string.continue_game else R.string.play),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                if (paused) {
                    FilledTonalButton(
                        onClick = {
                            vm.newGame()
                            onPlay()
                        },
                        modifier = buttonModifier,
                    ) { Text(stringResource(R.string.new_game)) }
                }
                FilledTonalButton(onClick = onScores, modifier = buttonModifier) {
                    Text(stringResource(R.string.high_scores))
                }
                OutlinedButton(onClick = onSettings, modifier = buttonModifier) {
                    Text(stringResource(R.string.settings))
                }
            }
        }
    }
}
