package com.tetris.material.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class ScoreEntry(val score: Int, val lines: Int, val level: Int, val date: Long)

class Prefs(context: Context) {

    private val sp = context.applicationContext.getSharedPreferences("tetris_prefs", Context.MODE_PRIVATE)

    private var _ghost by mutableStateOf(sp.getBoolean(K_GHOST, true))
    private var _grid by mutableStateOf(sp.getBoolean(K_GRID, true))
    private var _dynamicColor by mutableStateOf(sp.getBoolean(K_DYNAMIC, true))
    private var _themeMode by mutableStateOf(sp.getInt(K_THEME, 0))

    val ghost: Boolean get() = _ghost
    val grid: Boolean get() = _grid
    val dynamicColor: Boolean get() = _dynamicColor

    /** 0 = système, 1 = clair, 2 = sombre */
    val themeMode: Int get() = _themeMode

    var scores by mutableStateOf(loadScores())
        private set

    fun setGhost(v: Boolean) {
        _ghost = v
        sp.edit().putBoolean(K_GHOST, v).apply()
    }

    fun setGrid(v: Boolean) {
        _grid = v
        sp.edit().putBoolean(K_GRID, v).apply()
    }

    fun setDynamicColor(v: Boolean) {
        _dynamicColor = v
        sp.edit().putBoolean(K_DYNAMIC, v).apply()
    }

    fun setThemeMode(v: Int) {
        _themeMode = v
        sp.edit().putInt(K_THEME, v).apply()
    }

    /** Ajoute un score. Renvoie true s'il s'agit du nouveau meilleur score. */
    fun addScore(score: Int, lines: Int, level: Int): Boolean {
        if (score <= 0) return false
        val entry = ScoreEntry(score, lines, level, System.currentTimeMillis())
        val sorted = (scores + entry).sortedByDescending { it.score }.take(MAX_SCORES)
        val isRecord = sorted.firstOrNull() === entry
        scores = sorted
        saveScores()
        return isRecord
    }

    fun clearScores() {
        scores = emptyList()
        saveScores()
    }

    private fun saveScores() {
        val text = scores.joinToString(";") { "${it.score},${it.lines},${it.level},${it.date}" }
        sp.edit().putString(K_SCORES, text).apply()
    }

    private fun loadScores(): List<ScoreEntry> {
        val text = sp.getString(K_SCORES, "") ?: ""
        return text.split(";")
            .filter { it.isNotBlank() }
            .mapNotNull { part ->
                runCatching {
                    val f = part.split(",")
                    ScoreEntry(f[0].toInt(), f[1].toInt(), f[2].toInt(), f[3].toLong())
                }.getOrNull()
            }
            .sortedByDescending { it.score }
            .take(MAX_SCORES)
    }

    private companion object {
        const val K_GHOST = "ghost"
        const val K_GRID = "grid"
        const val K_DYNAMIC = "dynamic"
        const val K_THEME = "theme"
        const val K_SCORES = "scores"
        const val MAX_SCORES = 10
    }
}