package com.tetris.material.game

import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tetris.material.data.Prefs
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class GameViewModel : ViewModel() {

    private val engine = TetrisEngine()
    private var phase = Phase.Ready
    private var job: Job? = null
    private var saved = false

    var ui by mutableStateOf(engine.snapshot(Phase.Ready))
        private set

    var newRecord by mutableStateOf(false)
        private set

    fun newGame() {
        engine.reset()
        phase = Phase.Playing
        saved = false
        newRecord = false
        publish()
        startLoop()
    }

    fun pause() {
        if (phase == Phase.Playing) {
            phase = Phase.Paused
            job?.cancel()
            publish()
        }
    }

    fun resume() {
        if (phase == Phase.Paused) {
            phase = Phase.Playing
            publish()
            startLoop()
        }
    }

    fun moveLeft() = act { engine.move(-1) }
    fun moveRight() = act { engine.move(1) }
    fun softDrop() = act { engine.softDrop() }
    fun hardDrop() = act { engine.hardDrop() }
    fun rotateLeft() = act { engine.rotate(-1) }
    fun rotateRight() = act { engine.rotate(1) }
    fun holdPiece() = act { engine.holdPiece() }

    /** Enregistre le score une seule fois à la fin de la partie. */
    fun saveScoreOnce(prefs: Prefs) {
        val s = ui
        if (s.phase == Phase.Over && !saved) {
            saved = true
            newRecord = prefs.addScore(s.score, s.lines, s.level)
        }
    }

    private fun act(block: () -> Boolean) {
        if (phase == Phase.Playing && block()) publish()
    }

    private fun publish() {
        if (engine.isOver) {
            phase = Phase.Over
            job?.cancel()
        }
        ui = engine.snapshot(phase)
    }

    private fun startLoop() {
        job?.cancel()
        job = viewModelScope.launch {
            var last = SystemClock.elapsedRealtime()
            while (isActive) {
                delay(16)
                val now = SystemClock.elapsedRealtime()
                val dt = minOf(now - last, 100L).toDouble()
                last = now
                if (engine.update(dt)) publish()
            }
        }
    }
}
