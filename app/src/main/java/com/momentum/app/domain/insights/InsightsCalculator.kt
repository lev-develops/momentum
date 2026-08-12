package com.momentum.app.domain.insights

import com.momentum.app.domain.model.Completion
import com.momentum.app.domain.model.Habit
import com.momentum.app.domain.streak.StreakCalculator
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId

object InsightsCalculator {

    fun summarize(
        habits: List<Habit>,
        completionsByHabit: Map<Long, List<Completion>>,
        today: LocalDate,
        zone: ZoneId = ZoneId.systemDefault(),
    ): InsightsSummary {
        val allCompletions = completionsByHabit.values.flatten()
        if (allCompletions.isEmpty()) {
            return InsightsSummary(
                hasData = false,
                bestWeekday = null,
                worstWeekday = null,
                bestTimeOfDay = null,
                longestStreakEver = 0,
                longestStreakHabitName = null,
                completionsThisMonth = 0,
                completionsLastMonth = 0,
                monthOverMonthDelta = 0,
                activeHabitCount = habits.size,
            )
        }

        val weekdayStats = weekdayStats(habits, completionsByHabit, today)
        val bestWeekday = weekdayStats.filter { it.completions > 0 }.maxByOrNull { it.rate }
        val worstWeekday = weekdayStats.filter { it.completions > 0 }.minByOrNull { it.rate }

        val bestTimeOfDay = allCompletions
            .groupingBy { bucketOf(it.completedAt, zone) }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key

        var longestStreak = 0
        var longestStreakHabitName: String? = null
        var completionRateSum = 0f
        for (habit in habits) {
            val dates = completionsByHabit[habit.id].orEmpty().map { it.date }.toSet()
            val best = StreakCalculator.bestStreak(dates, habit.frequency, habit.targetDaysPerWeek, today)
            if (best > longestStreak) {
                longestStreak = best
                longestStreakHabitName = habit.name
            }
            completionRateSum += StreakCalculator.completionRate(
                dates,
                habit.createdAt.atZone(zone).toLocalDate(),
                habit.frequency,
                habit.targetDaysPerWeek,
                today,
            )
        }
        val overallCompletionRate = if (habits.isNotEmpty()) completionRateSum / habits.size else 0f

        val thisMonth = YearMonth.from(today)
        val lastMonth = thisMonth.minusMonths(1)
        val completionsThisMonth = allCompletions.count { YearMonth.from(it.date) == thisMonth }
        val completionsLastMonth = allCompletions.count { YearMonth.from(it.date) == lastMonth }

        return InsightsSummary(
            hasData = true,
            bestWeekday = bestWeekday,
            worstWeekday = worstWeekday,
            weekdayBreakdown = weekdayStats,
            bestTimeOfDay = bestTimeOfDay,
            longestStreakEver = longestStreak,
            longestStreakHabitName = longestStreakHabitName,
            completionsThisMonth = completionsThisMonth,
            completionsLastMonth = completionsLastMonth,
            monthOverMonthDelta = completionsThisMonth - completionsLastMonth,
            activeHabitCount = habits.size,
            overallCompletionRate = overallCompletionRate,
        )
    }

    /** Rate = completions on that weekday / how many of that weekday have occurred since each habit's creation. */
    private fun weekdayStats(
        habits: List<Habit>,
        completionsByHabit: Map<Long, List<Completion>>,
        today: LocalDate,
    ): List<WeekdayStat> = DayOfWeek.entries.map { dow ->
        var eligible = 0
        var completed = 0
        for (habit in habits) {
            val createdDate = habit.createdAt.atZone(ZoneId.systemDefault()).toLocalDate()
            if (today.isBefore(createdDate)) continue
            eligible += countWeekdayOccurrences(createdDate, today, dow)
            completed += completionsByHabit[habit.id].orEmpty().count { it.date.dayOfWeek == dow }
        }
        val rate = if (eligible > 0) completed.toFloat() / eligible.toFloat() else 0f
        WeekdayStat(dayOfWeek = dow, rate = rate, completions = completed)
    }

    private fun countWeekdayOccurrences(start: LocalDate, end: LocalDate, dow: DayOfWeek): Int {
        if (end.isBefore(start)) return 0
        var first = start
        while (first.dayOfWeek != dow) first = first.plusDays(1)
        if (first.isAfter(end)) return 0
        val totalDays = java.time.temporal.ChronoUnit.DAYS.between(first, end)
        return (totalDays / 7 + 1).toInt()
    }

    private fun bucketOf(instant: Instant, zone: ZoneId): TimeOfDayBucket {
        val time = instant.atZone(zone).toLocalTime()
        return when {
            time >= LocalTime.of(5, 0) && time < LocalTime.of(12, 0) -> TimeOfDayBucket.MORNING
            time >= LocalTime.of(12, 0) && time < LocalTime.of(17, 0) -> TimeOfDayBucket.AFTERNOON
            time >= LocalTime.of(17, 0) && time < LocalTime.of(21, 0) -> TimeOfDayBucket.EVENING
            else -> TimeOfDayBucket.NIGHT
        }
    }
}
