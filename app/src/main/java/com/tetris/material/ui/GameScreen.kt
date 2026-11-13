package com.tetris.material.ui

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.tetris.material.R
import com.tetris.material.data.Prefs
import com.tetris.material.game.COLS
import com.tetris.material.game.GameViewModel
import com.tetris.material.game.Phase
import com.tetris.material.game.ROWS
import com.tetris.material.game.Shapes
import com.tetris.material.game.UiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun GameScreen(vm: GameViewModel, prefs: Prefs, onExit: () -> Unit) {
    val ui = vm.ui
    val context = LocalContext.current
    val colors = pieceColors()

    // Pause automatique quand l'app passe en arrière-plan.
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { vm.pause() }
    LaunchedEffect(Unit) { if (vm.ui.phase == Phase.Ready) vm.newGame() }
    LaunchedEffect(ui.phase) { if (ui.phase == Phase.Over) vm.saveScoreOnce(prefs) }
    BackHandler {
        vm.pause()
        onExit()
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatCard(stringResource(R.string.score), ui.score.toString(), Modifier.weight(1.7f))
                StatCard(stringResource(R.string.level), ui.level.toString(), Modifier.weight(1f))
                StatCard(stringResource(R.string.lines), ui.lines.toString(), Modifier.weight(1f))
                val pauseDesc = stringResource(R.string.cd_pause)
                FilledTonalIconButton(
                    onClick = { vm.pause() },
                    modifier = Modifier.semantics { contentDescription = pauseDesc },
                ) { GlyphIcon(Ic.Pause) }
            }

            Row(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center,
                ) {
                    Board(ui, prefs.ghost, prefs.grid, colors, Modifier.aspectRatio(0.5f))
                }
                Column(
                    Modifier
                        .width(78.dp)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SideCard(stringResource(R.string.hold)) {
                        PiecePreview(
                            ui.hold,
                            colors,
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f),
                            dim = !ui.canHold,
                        )
                    }
                    SideCard(stringResource(R.string.next)) {
                        ui.next.forEach {
                            PiecePreview(
                                it,
                                colors,
                                Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1.3f),
                            )
                        }
                    }
                }
            }

            Controls(vm)
        }
    }

    if (ui.phase == Phase.Paused) {
        AlertDialog(
            onDismissRequest = { vm.resume() },
            title = { Text(stringResource(R.string.paused)) },
            confirmButton = {
                Button(onClick = { vm.resume() }) { Text(stringResource(R.string.resume)) }
            },
            dismissButton = {
                TextButton(onClick = onExit) { Text(stringResource(R.string.menu)) }
            },
        )
    }

    if (ui.phase == Phase.Over) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.game_over)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (vm.newRecord) {
                        Text(
                            stringResource(R.string.new_record),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Text(stringResource(R.string.score_line, ui.score, ui.lines, ui.level))
                    TextButton(onClick = { shareScore(context, ui.score) }) {
                        Text(stringResource(R.string.share))
                    }
                }
            },
            confirmButton = {
                Button(onClick = { vm.newGame() }) { Text(stringResource(R.string.play_again)) }
            },
            dismissButton = {
                TextButton(onClick = onExit) { Text(stringResource(R.string.menu)) }
            },
        )
    }
}

fun shareScore(context: Context, score: Int) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, context.getString(R.string.share_text, score))
    }
    context.startActivity(Intent.createChooser(send, null))
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun SideCard(label: String, content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            content()
        }
    }
}

@Composable
private fun Board(
    ui: UiState,
    ghost: Boolean,
    grid: Boolean,
    colors: List<Color>,
    modifier: Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    val bg = MaterialTheme.colorScheme.surfaceContainerLow
    val border = MaterialTheme.colorScheme.outlineVariant
    val lineColor = border.copy(alpha = 0.4f)

    Canvas(
        modifier
            .clip(shape)
            .background(bg)
            .border(1.dp, border, shape),
    ) {
        val cell = size.width / COLS
        if (grid) {
            for (i in 1 until COLS) {
                drawLine(lineColor, Offset(i * cell, 0f), Offset(i * cell, size.height), strokeWidth = 1f)
            }
            for (j in 1 until ROWS) {
                drawLine(lineColor, Offset(0f, j * cell), Offset(size.width, j * cell), strokeWidth = 1f)
            }
        }
        for (y in 0 until ROWS) {
            for (x in 0 until COLS) {
                val v = ui.board[y * COLS + x]
                if (v != 0) drawBlock(x * cell, y * cell, cell, colors[v - 1])
            }
        }
        val p = ui.piece
        if (p != null) {
            val cells = Shapes.cells(p.kind, p.rot)
            val color = colors[p.kind.ordinal]
            if (ghost && ui.ghostY > p.y) {
                for (i in 0 until 4) {
                    val gx = p.x + cells[i * 2]
                    val gy = ui.ghostY + cells[i * 2 + 1]
                    if (gy >= 0) {
                        drawRoundRect(
                            color.copy(alpha = 0.7f),
                            Offset(gx * cell + cell * 0.1f, gy * cell + cell * 0.1f),
                            Size(cell * 0.8f, cell * 0.8f),
                            CornerRadius(cell * 0.2f),
                            style = Stroke(width = cell * 0.08f),
                        )
                    }
                }
            }
            for (i in 0 until 4) {
                val bx = p.x + cells[i * 2]
                val by = p.y + cells[i * 2 + 1]
                if (by >= 0) drawBlock(bx * cell, by * cell, cell, color)
            }
        }
    }
}

@Composable
private fun Controls(vm: GameViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PadButton(Modifier.weight(1f), false, stringResource(R.string.hold), { vm.holdPiece() }) { c ->
                Text(
                    stringResource(R.string.hold),
                    color = c,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }
            PadButton(Modifier.weight(1f), false, stringResource(R.string.cd_rot_left), { vm.rotateLeft() }) { c ->
                GlyphIcon(Ic.RotCcw, color = c)
            }
            PadButton(Modifier.weight(1f), false, stringResource(R.string.cd_rot_right), { vm.rotateRight() }) { c ->
                GlyphIcon(Ic.RotCw, color = c)
            }
            PadButton(Modifier.weight(1f), false, stringResource(R.string.cd_drop), { vm.hardDrop() }) { c ->
                GlyphIcon(Ic.Drop, color = c)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PadButton(Modifier.weight(1f), true, stringResource(R.string.cd_left), { vm.moveLeft() }) { c ->
                GlyphIcon(Ic.Left, color = c)
            }
            PadButton(Modifier.weight(1f), true, stringResource(R.string.cd_down), { vm.softDrop() }) { c ->
                GlyphIcon(Ic.Down, color = c)
            }
            PadButton(Modifier.weight(1f), true, stringResource(R.string.cd_right), { vm.moveRight() }) { c ->
                GlyphIcon(Ic.Right, color = c)
            }
        }
    }
}

/**
 * Bouton de jeu : réagit dès l'appui (pas au relâchement), avec répétition automatique
 * si on reste appuyé (gauche, droite, descente). Aucun retour haptique.
 */
@Composable
private fun PadButton(
    modifier: Modifier,
    repeat: Boolean,
    description: String,
    onPress: () -> Unit,
    content: @Composable (Color) -> Unit,
) {
    var pressed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val press by rememberUpdatedState(onPress)
    val scheme = MaterialTheme.colorScheme
    val container = if (pressed) scheme.primary else scheme.secondaryContainer
    val contentColor = if (pressed) scheme.onPrimary else scheme.onSecondaryContainer

    Box(
        modifier
            .height(56.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(container)
            .semantics(mergeDescendants = true) {
                contentDescription = description
                onClick {
                    press()
                    true
                }
            }
            .pointerInput(repeat) {
                awaitEachGesture {
                    awaitFirstDown()
                    pressed = true
                    val job = scope.launch {
                        press()
                        if (repeat) {
                            delay(170)
                            while (true) {
                                press()
                                delay(55)
                            }
                        }
                    }
                    waitForUpOrCancellation()
                    job.cancel()
                    pressed = false
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        content(contentColor)
    }
}
