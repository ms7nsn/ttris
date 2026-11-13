package com.tetris.material.game

import kotlin.math.pow
import kotlin.random.Random

const val COLS = 10
const val ROWS = 20

enum class Kind { I, O, T, S, Z, J, L }

enum class Phase { Ready, Playing, Paused, Over }

class Piece(val kind: Kind, val rot: Int, val x: Int, val y: Int)

/** Photo instantanée de la partie, lue par l'interface. */
class UiState(
    val board: IntArray,
    val piece: Piece?,
    val ghostY: Int,
    val next: List<Kind>,
    val hold: Kind?,
    val canHold: Boolean,
    val score: Int,
    val level: Int,
    val lines: Int,
    val phase: Phase,
)

/** Formes des pièces et table de rotations SRS (Super Rotation System). */
object Shapes {
    private val base: Map<Kind, Pair<Int, List<Int>>> = mapOf(
        Kind.I to (4 to listOf(0, 1, 1, 1, 2, 1, 3, 1)),
        Kind.O to (4 to listOf(1, 0, 2, 0, 1, 1, 2, 1)),
        Kind.T to (3 to listOf(1, 0, 0, 1, 1, 1, 2, 1)),
        Kind.S to (3 to listOf(1, 0, 2, 0, 0, 1, 1, 1)),
        Kind.Z to (3 to listOf(0, 0, 1, 0, 1, 1, 2, 1)),
        Kind.J to (3 to listOf(0, 0, 0, 1, 1, 1, 2, 1)),
        Kind.L to (3 to listOf(2, 0, 0, 1, 1, 1, 2, 1)),
    )

    // table[kind][rotation] = x0,y0,x1,y1,x2,y2,x3,y3
    private val table: Array<Array<IntArray>> = Array(Kind.values().size) { k ->
        val kind = Kind.values()[k]
        val (n, list) = base.getValue(kind)
        var cur = list.toIntArray()
        Array(4) {
            val result = cur.copyOf()
            if (kind != Kind.O) {
                val next = IntArray(8)
                for (i in 0 until 4) {
                    val x = cur[i * 2]
                    val y = cur[i * 2 + 1]
                    next[i * 2] = n - 1 - y
                    next[i * 2 + 1] = x
                }
                cur = next
            }
            result
        }
    }

    fun cells(kind: Kind, rot: Int): IntArray = table[kind.ordinal][rot and 3]

    private fun key(from: Int, to: Int) = from * 4 + to

    // Décalages (x, y) avec y vers le HAUT, comme dans la spec SRS.
    private val kicksJlstz: Map<Int, IntArray> = mapOf(
        key(0, 1) to intArrayOf(0, 0, -1, 0, -1, 1, 0, -2, -1, -2),
        key(1, 0) to intArrayOf(0, 0, 1, 0, 1, -1, 0, 2, 1, 2),
        key(1, 2) to intArrayOf(0, 0, 1, 0, 1, -1, 0, 2, 1, 2),
        key(2, 1) to intArrayOf(0, 0, -1, 0, -1, 1, 0, -2, -1, -2),
        key(2, 3) to intArrayOf(0, 0, 1, 0, 1, 1, 0, -2, 1, -2),
        key(3, 2) to intArrayOf(0, 0, -1, 0, -1, -1, 0, 2, -1, 2),
        key(3, 0) to intArrayOf(0, 0, -1, 0, -1, -1, 0, 2, -1, 2),
        key(0, 3) to intArrayOf(0, 0, 1, 0, 1, 1, 0, -2, 1, -2),
    )

    private val kicksI: Map<Int, IntArray> = mapOf(
        key(0, 1) to intArrayOf(0, 0, -2, 0, 1, 0, -2, -1, 1, 2),
        key(1, 0) to intArrayOf(0, 0, 2, 0, -1, 0, 2, 1, -1, -2),
        key(1, 2) to intArrayOf(0, 0, -1, 0, 2, 0, -1, 2, 2, -1),
        key(2, 1) to intArrayOf(0, 0, 1, 0, -2, 0, 1, -2, -2, 1),
        key(2, 3) to intArrayOf(0, 0, 2, 0, -1, 0, 2, 1, -1, -2),
        key(3, 2) to intArrayOf(0, 0, -2, 0, 1, 0, -2, -1, 1, 2),
        key(3, 0) to intArrayOf(0, 0, 1, 0, -2, 0, 1, -2, -2, 1),
        key(0, 3) to intArrayOf(0, 0, -1, 0, 2, 0, -1, 2, 2, -1),
    )

    private val none = intArrayOf(0, 0)

    fun kicks(kind: Kind, from: Int, to: Int): IntArray = when (kind) {
        Kind.O -> none
        Kind.I -> kicksI[key(from, to)] ?: none
        else -> kicksJlstz[key(from, to)] ?: none
    }
}

class TetrisEngine(private val random: Random = Random.Default) {

    private val board = IntArray(COLS * ROWS)
    private val bag = ArrayDeque<Kind>()
    private val queue = ArrayDeque<Kind>()

    private var piece: Piece? = null
    private var hold: Kind? = null
    private var canHold = true

    private var score = 0
    private var lines = 0
    private var level = 1
    private var startLevel = 1

    private var gravityAcc = 0.0
    private var lockTimer = 0.0
    private var lockResets = 0

    var isOver = false
        private set

    fun reset(startLevel: Int = 1) {
        board.fill(0)
        bag.clear()
        queue.clear()
        hold = null
        canHold = true
        score = 0
        lines = 0
        this.startLevel = startLevel
        level = startLevel
        isOver = false
        piece = null
        refillQueue()
        spawnNext()
    }

    // ---------- Actions du joueur ----------

    fun move(dx: Int): Boolean {
        val p = piece ?: return false
        if (!fits(p.kind, p.rot, p.x + dx, p.y)) return false
        piece = Piece(p.kind, p.rot, p.x + dx, p.y)
        onManualMove()
        return true
    }

    fun softDrop(): Boolean {
        val p = piece ?: return false
        if (!fits(p.kind, p.rot, p.x, p.y + 1)) return false
        piece = Piece(p.kind, p.rot, p.x, p.y + 1)
        score += 1
        gravityAcc = 0.0
        onManualMove()
        return true
    }

    fun rotate(dir: Int): Boolean {
        val p = piece ?: return false
        val to = (p.rot + dir + 4) and 3
        val kicks = Shapes.kicks(p.kind, p.rot, to)
        var i = 0
        while (i < kicks.size) {
            val nx = p.x + kicks[i]
            val ny = p.y - kicks[i + 1]
            if (fits(p.kind, to, nx, ny)) {
                piece = Piece(p.kind, to, nx, ny)
                onManualMove()
                return true
            }
            i += 2
        }
        return false
    }

    fun hardDrop(): Boolean {
        val p = piece ?: return false
        var y = p.y
        while (fits(p.kind, p.rot, p.x, y + 1)) y++
        score += 2 * (y - p.y)
        piece = Piece(p.kind, p.rot, p.x, y)
        lockPiece()
        return true
    }

    fun holdPiece(): Boolean {
        val p = piece ?: return false
        if (!canHold) return false
        val stored = hold
        hold = p.kind
        if (stored == null) spawnNext() else spawn(stored)
        canHold = false
        return true
    }

    // ---------- Boucle de jeu ----------

    /** Fait avancer le temps de [dtMs] millisecondes. Renvoie true si quelque chose a changé. */
    fun update(dtMs: Double): Boolean {
        val p = piece ?: return false
        if (isOver) return false

        if (!fits(p.kind, p.rot, p.x, p.y + 1)) {
            // La pièce touche le sol : délai avant verrouillage.
            gravityAcc = 0.0
            lockTimer += dtMs
            if (lockTimer >= LOCK_DELAY_MS) {
                lockPiece()
                return true
            }
            return false
        }

        lockTimer = 0.0
        gravityAcc += dtMs
        val interval = gravityMs()
        var changed = false
        var steps = 0
        while (gravityAcc >= interval && steps < ROWS) {
            val cur = piece ?: break
            if (!fits(cur.kind, cur.rot, cur.x, cur.y + 1)) break
            piece = Piece(cur.kind, cur.rot, cur.x, cur.y + 1)
            gravityAcc -= interval
            steps++
            changed = true
        }
        return changed
    }

    fun snapshot(phase: Phase): UiState {
        val p = piece
        var gy = p?.y ?: 0
        if (p != null) {
            while (fits(p.kind, p.rot, p.x, gy + 1)) gy++
        }
        return UiState(
            board = board.copyOf(),
            piece = p,
            ghostY = gy,
            next = queue.take(3),
            hold = hold,
            canHold = canHold,
            score = score,
            level = level,
            lines = lines,
            phase = if (isOver) Phase.Over else phase,
        )
    }

    // ---------- Interne ----------

    private fun gravityMs(): Double {
        val l = minOf(level, 20)
        val seconds = (0.8 - (l - 1) * 0.007).pow((l - 1).toDouble())
        return maxOf(seconds * 1000.0, 1.0)
    }

    private fun onManualMove() {
        val p = piece ?: return
        if (fits(p.kind, p.rot, p.x, p.y + 1)) {
            lockTimer = 0.0
        } else if (lockResets < MAX_LOCK_RESETS) {
            lockTimer = 0.0
            lockResets++
        }
    }

    private fun fits(kind: Kind, rot: Int, px: Int, py: Int): Boolean {
        val c = Shapes.cells(kind, rot)
        for (i in 0 until 4) {
            val x = px + c[i * 2]
            val y = py + c[i * 2 + 1]
            if (x < 0 || x >= COLS || y >= ROWS) return false
            if (y >= 0 && board[y * COLS + x] != 0) return false
        }
        return true
    }

    private fun refillQueue() {
        while (queue.size < 5) {
            if (bag.isEmpty()) bag.addAll(Kind.values().toList().shuffled(random))
            queue.addLast(bag.removeFirst())
        }
    }

    private fun spawnNext() {
        val kind = queue.removeFirst()
        refillQueue()
        spawn(kind)
    }

    private fun spawn(kind: Kind) {
        val p = Piece(kind, 0, 3, if (kind == Kind.I) -1 else 0)
        gravityAcc = 0.0
        lockTimer = 0.0
        lockResets = 0
        if (fits(kind, 0, p.x, p.y)) {
            piece = p
        } else {
            piece = null
            isOver = true
        }
    }

    private fun lockPiece() {
        val p = piece ?: return
        val c = Shapes.cells(p.kind, p.rot)
        for (i in 0 until 4) {
            val x = p.x + c[i * 2]
            val y = p.y + c[i * 2 + 1]
            if (y >= 0) board[y * COLS + x] = p.kind.ordinal + 1
        }
        piece = null

        var cleared = 0
        var row = ROWS - 1
        while (row >= 0) {
            var full = true
            for (x in 0 until COLS) {
                if (board[row * COLS + x] == 0) {
                    full = false
                    break
                }
            }
            if (full) {
                for (r in row downTo 1) {
                    for (x in 0 until COLS) board[r * COLS + x] = board[(r - 1) * COLS + x]
                }
                for (x in 0 until COLS) board[x] = 0
                cleared++
            } else {
                row--
            }
        }

        val points = when (cleared) {
            1 -> 100
            2 -> 300
            3 -> 500
            4 -> 800
            else -> 0
        }
        score += points * level
        lines += cleared
        level = minOf(startLevel + lines / 10, 30)
        canHold = true
        spawnNext()
    }

    private companion object {
        const val LOCK_DELAY_MS = 500.0
        const val MAX_LOCK_RESETS = 15
    }
}
