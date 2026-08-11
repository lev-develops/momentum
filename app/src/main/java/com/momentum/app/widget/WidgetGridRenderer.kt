package com.momentum.app.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.momentum.app.domain.model.HabitColor
import com.momentum.app.domain.streak.ContributionGrid
import com.momentum.app.ui.theme.HabitPalette

/**
 * Renders a [ContributionGrid] to a bitmap using android.graphics.Canvas, reading colors from
 * [HabitPalette] (ui/theme/Theme.kt) so the widget's grid always matches the Habit Detail
 * screen's Canvas-drawn grid exactly. Glance has no Canvas composable, so a bitmap + Image is
 * the standard way to draw arbitrary shapes in a widget.
 */
object WidgetGridRenderer {

    fun render(
        grid: ContributionGrid,
        colorKey: HabitColor,
        isDark: Boolean,
        widthPx: Int,
        heightPx: Int,
        visibleColumns: Int,
        cellPx: Float,
        gapPx: Float,
        cornerRadiusPx: Float,
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPx.coerceAtLeast(1), heightPx.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val totalColumns = grid.columns.size
        val startColumn = (totalColumns - visibleColumns).coerceAtLeast(0)
        val visible = grid.columns.subList(startColumn, totalColumns)

        visible.forEachIndexed { columnIndex, column ->
            val x = columnIndex * (cellPx + gapPx)
            column.forEachIndexed { rowIndex, cell ->
                val y = rowIndex * (cellPx + gapPx)
                paint.color = HabitPalette.cellArgb(colorKey, cell.level, isDark)
                canvas.drawRoundRect(
                    RectF(x, y, x + cellPx, y + cellPx),
                    cornerRadiusPx,
                    cornerRadiusPx,
                    paint,
                )
            }
        }

        return bitmap
    }
}
