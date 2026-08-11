package com.momentum.app.domain.streak

import com.momentum.app.domain.model.HabitFrequency
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/**
 * All streak math is pure and operates on calendar [LocalDate]s only — no time-of-day, no
 * timezone offsets — so it is naturally immune to local-midnight rollover and DST quirks as
 * long as callers convert "now" to a [LocalDate] once, up front, in the correct zone.
 */
object StreakCalculator {

    /** Weeks run Sunday through Saturday, matching the contribution grid's row layout. */
    private fun weekStartOf(date: LocalDate): LocalDate =
        date.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))

    fun currentStreak(
        completedDates: Set<LocalDate>,
        frequency: HabitFrequency,
        targetDaysPerWeek: Int,
        today: LocalDate,
    ): Int = when (frequency) {
        HabitFrequency.DAILY -> currentStreakDaily(completedDates, today)
        HabitFrequency.WEEKLY_TARGET -> currentStreakWeekly(completedDates, targetDaysPerWeek, today)
    }

    fun bestStreak(
        completedDates: Set<LocalDate>,
        frequency: HabitFrequency,
        targetDaysPerWeek: Int,
        today: LocalDate,
    ): Int = when (frequency) {
        HabitFrequency.DAILY -> bestStreakDaily(completedDates)
        HabitFrequency.WEEKLY_TARGET -> bestStreakWeekly(completedDates, targetDaysPerWeek, today)
    }

    /**
     * Fraction of eligible days, since the habit's creation, that were completed. For DAILY
     * habits every day since creation is eligible; for WEEKLY_TARGET habits the denominator is
     * (weeks elapsed * targetDaysPerWeek), capped so a brand-new habit doesn't divide by zero.
     */
    fun completionRate(
        completedDates: Set<LocalDate>,
        createdAt: LocalDate,
        frequency: HabitFrequency,
        targetDaysPerWeek: Int,
        today: LocalDate,
    ): Float {
        if (today.isBefore(createdAt)) return 0f
        val daysElapsed = ChronoUnit.DAYS.between(createdAt, today) + 1
        if (daysElapsed <= 0) return 0f
        val relevantCompletions = completedDates.count { it in createdAt..today }
        return when (frequency) {
            HabitFrequency.DAILY -> relevantCompletions.toFloat() / daysElapsed.toFloat()
            HabitFrequency.WEEKLY_TARGET -> {
                val weeksElapsed = (daysElapsed + 6) / 7
                val expected = (weeksElapsed * targetDaysPerWeek.coerceAtLeast(1)).toFloat()
                (relevantCompletions.toFloat() / expected).coerceAtMost(1f)
            }
        }
    }

    // ---- DAILY ----

    private fun currentStreakDaily(completedDates: Set<LocalDate>, today: LocalDate): Int {
        var cursor = if (today in completedDates) today else today.minusDays(1)
        if (cursor !in completedDates) return 0
        var streak = 0
        while (cursor in completedDates) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    private fun bestStreakDaily(completedDates: Set<LocalDate>): Int {
        if (completedDates.isEmpty()) return 0
        val sorted = completedDates.sorted()
        var best = 1
        var run = 1
        for (i in 1 until sorted.size) {
            run = if (sorted[i] == sorted[i - 1].plusDays(1)) run + 1 else 1
            best = maxOf(best, run)
        }
        return best
    }

    // ---- WEEKLY_TARGET ----

    private fun countInWeek(completedDates: Set<LocalDate>, weekStart: LocalDate): Int {
        val weekEndExclusive = weekStart.plusDays(7)
        return completedDates.count { it >= weekStart && it < weekEndExclusive }
    }

    private fun currentStreakWeekly(
        completedDates: Set<LocalDate>,
        targetDaysPerWeek: Int,
        today: LocalDate,
    ): Int {
        val target = targetDaysPerWeek.coerceAtLeast(1)
        // weekStartOf(today) is, by construction, always still in progress (today is part of
        // it), so an unmet current week never breaks the streak — it just isn't counted yet.
        // Every earlier week is by definition fully elapsed, so those are judged strictly.
        var weekStart = weekStartOf(today)
        val currentWeekMet = countInWeek(completedDates, weekStart) >= target

        var streak = if (currentWeekMet) 1 else 0
        weekStart = weekStart.minusDays(7)

        while (countInWeek(completedDates, weekStart) >= target) {
            streak++
            weekStart = weekStart.minusDays(7)
        }
        return streak
    }

    private fun bestStreakWeekly(
        completedDates: Set<LocalDate>,
        targetDaysPerWeek: Int,
        today: LocalDate,
    ): Int {
        if (completedDates.isEmpty()) return 0
        val target = targetDaysPerWeek.coerceAtLeast(1)
        val firstWeek = weekStartOf(completedDates.min())
        val lastWeek = weekStartOf(today)

        var best = 0
        var run = 0
        var weekStart = firstWeek
        while (!weekStart.isAfter(lastWeek)) {
            if (countInWeek(completedDates, weekStart) >= target) {
                run++
                best = maxOf(best, run)
            } else {
                run = 0
            }
            weekStart = weekStart.plusDays(7)
        }
        return best
    }
}
