package com.momentum.app.ui.detail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.momentum.app.domain.model.HabitColor
import com.momentum.app.domain.streak.ContributionGrid
import com.momentum.app.ui.theme.HabitPalette
import com.momentum.app.ui.theme.LocalMomentumColors

val GridCellSize = 12.dp
val GridCellGap = 3.dp
val GridCornerRadius = 2.dp

/** 7 rows x [grid].weeks columns, drawn with Canvas, horizontally scrollable and opened at today's column. */
@Composable
fun ContributionGridCanvas(
    grid: ContributionGrid,
    colorKey: HabitColor,
    modifier: Modifier = Modifier,
) {
    val colors = LocalMomentumColors.current
    val scrollState = rememberScrollState()

    LaunchedEffect(grid) {
        scrollState.scrollTo(scrollState.maxValue)
    }

    val totalWidth = grid.weeks * (GridCellSize + GridCellGap) - GridCellGap
    val totalHeight = 7 * (GridCellSize + GridCellGap) - GridCellGap

    Row(modifier = modifier.horizontalScroll(scrollState)) {
        Canvas(
            modifier = Modifier
                .width(totalWidth)
                .height(totalHeight),
        ) {
            drawGrid(grid, colorKey, colors.isDark)
        }
    }
}

private fun DrawScope.drawGrid(grid: ContributionGrid, colorKey: HabitColor, isDark: Boolean) {
    val cellPx = GridCellSize.toPx()
    val gapPx = GridCellGap.toPx()
    val radiusPx = GridCornerRadius.toPx()
    val pitch = cellPx + gapPx

    grid.columns.forEachIndexed { columnIndex, column ->
        val x = columnIndex * pitch
        column.forEachIndexed { rowIndex, cell ->
            val y = rowIndex * pitch
            val color = HabitPalette.cellColor(colorKey, cell.level, isDark)
            drawRoundRect(
                color = color,
                topLeft = Offset(x, y),
                size = Size(cellPx, cellPx),
                cornerRadius = CornerRadius(radiusPx, radiusPx),
            )
        }
    }
}
