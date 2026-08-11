package com.momentum.app.domain.streak

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** One cell of the contribution grid. [date] is null for cells before a habit existed. */
data class GridCell(
    val date: LocalDate?,
    val level: Int,
    val completed: Boolean,
)

/**
 * rows[dayOfWeekIndex][weekIndex], 7 rows (Sunday..Saturday) by [weeks] columns, oldest week
 * first, most recent (today's) week last. Shared by the Habit Detail screen's Canvas grid and
 * the Glance widget's bitmap-rendered grid so both read identical intensity levels.
 */
data class ContributionGrid(
    val columns: List<List<GridCell>>,
    val weeks: Int,
) {
    val rows: Int get() = 7
}

object ContributionGridBuilder {

    const val DEFAULT_WEEKS = 53

    /**
     * Builds a grid ending on [today]'s week. A day's level is driven by how many completions
     * have accumulated in its (Sunday-start) week up to and including that day, relative to
     * [targetDaysPerWeek] — level 4 once the week's target is met or exceeded, tapering down to
     * level 1 for the first completion of the week, matching the "L1 = minimum completion,
     * L4 = target met or exceeded" grid spec.
     */
    fun build(
        completedDates: Set<LocalDate>,
        targetDaysPerWeek: Int,
        today: LocalDate,
        weeks: Int = DEFAULT_WEEKS,
    ): ContributionGrid {
        val target = targetDaysPerWeek.coerceAtLeast(1)
        val lastWeekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
        val firstWeekStart = lastWeekStart.minusWeeks((weeks - 1).toLong())

        val columns = (0 until weeks).map { weekIndex ->
            val weekStart = firstWeekStart.plusWeeks(weekIndex.toLong())
            var cumulative = 0
            (0 until 7).map { dayIndex ->
                val date = weekStart.plusDays(dayIndex.toLong())
                if (date.isAfter(today)) {
                    GridCell(date = null, level = 0, completed = false)
                } else {
                    val completed = date in completedDates
                    if (completed) cumulative++
                    val level = levelFor(cumulative, target, completed)
                    GridCell(date = date, level = level, completed = completed)
                }
            }
        }

        return ContributionGrid(columns = columns, weeks = weeks)
    }

    private fun levelFor(cumulativeInWeek: Int, target: Int, completedToday: Boolean): Int {
        if (!completedToday) return 0
        val ratio = cumulativeInWeek.toFloat() / target.toFloat()
        return when {
            ratio >= 1f -> 4
            ratio >= 0.66f -> 3
            ratio >= 0.33f -> 2
            else -> 1
        }
    }
}
