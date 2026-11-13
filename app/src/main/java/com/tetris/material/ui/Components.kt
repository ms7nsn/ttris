package com.tetris.material.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.tetris.material.game.Kind
import com.tetris.material.game.Shapes
import kotlin.math.cos
import kotlin.math.sin

enum class Ic { Left, Right, Down, RotCw, RotCcw, Drop, Pause, Back }

/** Icônes dessinées à la main (aucune dépendance à une bibliothèque d'icônes). */
@Composable
fun GlyphIcon(
    ic: Ic,
    modifier: Modifier = Modifier.size(28.dp),
    color: Color = LocalContentColor.current,
) {
    Canvas(modifier) {
        val w = size.width
        val stroke = Stroke(width = w * 0.11f, cap = StrokeCap.Round, join = StrokeJoin.Round)

        fun line(vararg p: Float) {
            val path = Path()
            path.moveTo(p[0] * w, p[1] * w)
            var i = 2
            while (i < p.size) {
                path.lineTo(p[i] * w, p[i + 1] * w)
                i += 2
            }
            drawPath(path, color, style = stroke)
        }

        when (ic) {
            Ic.Left -> line(0.62f, 0.2f, 0.32f, 0.5f, 0.62f, 0.8f)
            Ic.Right -> line(0.38f, 0.2f, 0.68f, 0.5f, 0.38f, 0.8f)
            Ic.Down -> line(0.2f, 0.34f, 0.5f, 0.64f, 0.8f, 0.34f)
            Ic.Drop -> {
                line(0.2f, 0.1f, 0.5f, 0.38f, 0.8f, 0.1f)
                line(0.2f, 0.4f, 0.5f, 0.68f, 0.8f, 0.4f)
                line(0.2f, 0.88f, 0.8f, 0.88f)
            }
            Ic.Back -> {
                line(0.5f, 0.2f, 0.25f, 0.5f, 0.5f, 0.8f)
                line(0.28f, 0.5f, 0.8f, 0.5f)
            }
            Ic.Pause -> {
                drawRoundRect(color, Offset(w * 0.24f, w * 0.2f), Size(w * 0.17f, w * 0.6f), CornerRadius(w * 0.05f))
                drawRoundRect(color, Offset(w * 0.59f, w * 0.2f), Size(w * 0.17f, w * 0.6f), CornerRadius(w * 0.05f))
            }
            Ic.RotCw, Ic.RotCcw -> {
                val cw = ic == Ic.RotCw
                val cx = w / 2f
                val r = w * 0.34f
                val start = if (cw) -40f else 220f
                val sweep = if (cw) 270f else -270f
                drawArc(
                    color = color,
                    startAngle = start,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(cx - r, cx - r),
                    size = Size(2 * r, 2 * r),
                    style = stroke,
                )
                val end = Math.toRadians((start + sweep).toDouble())
                val px = cx + r * cos(end).toFloat()
                val py = cx + r * sin(end).toFloat()
                val dir = if (cw) 1f else -1f
                val tx = -sin(end).toFloat() * dir
                val ty = cos(end).toFloat() * dir
                val nx = -ty
                val ny = tx
                val len = w * 0.2f
                val half = w * 0.15f
                val head = Path()
                head.moveTo(px + tx * len, py + ty * len)
                head.lineTo(px + nx * half, py + ny * half)
                head.lineTo(px - nx * half, py - ny * half)
                head.close()
                drawPath(head, color)
            }
        }
    }
}

/** Dessine un bloc arrondi, style Material. */
fun DrawScope.drawBlock(x: Float, y: Float, cell: Float, color: Color) {
    val gap = cell * 0.05f
    drawRoundRect(
        color,
        Offset(x + gap, y + gap),
        Size(cell - 2 * gap, cell - 2 * gap),
        CornerRadius(cell * 0.24f),
    )
    drawRoundRect(
        Color.White.copy(alpha = 0.2f),
        Offset(x + cell * 0.22f, y + cell * 0.22f),
        Size(cell * 0.56f, cell * 0.56f),
        CornerRadius(cell * 0.14f),
    )
}

/** Petit aperçu d'une pièce (zones « Garder » et « Suivant »). */
@Composable
fun PiecePreview(
    kind: Kind?,
    colors: List<Color>,
    modifier: Modifier = Modifier,
    dim: Boolean = false,
) {
    Canvas(modifier) {
        if (kind == null) return@Canvas
        val cells = Shapes.cells(kind, 0)
        var minX = 99
        var maxX = -1
        var minY = 99
        var maxY = -1
        for (i in 0 until 4) {
            val x = cells[i * 2]
            val y = cells[i * 2 + 1]
            if (x < minX) minX = x
            if (x > maxX) maxX = x
            if (y < minY) minY = y
            if (y > maxY) maxY = y
        }
        val bw = maxX - minX + 1
        val bh = maxY - minY + 1
        val cell = minOf(size.width / 4f, size.height / 2.5f)
        val ox = (size.width - bw * cell) / 2f
        val oy = (size.height - bh * cell) / 2f
        val color = colors[kind.ordinal].let { if (dim) it.copy(alpha = 0.35f) else it }
        for (i in 0 until 4) {
            drawBlock(ox + (cells[i * 2] - minX) * cell, oy + (cells[i * 2 + 1] - minY) * cell, cell, color)
        }
    }
}
