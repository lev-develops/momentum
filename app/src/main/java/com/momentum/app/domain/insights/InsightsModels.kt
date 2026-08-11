package com.momentum.app.domain.insights

import java.time.DayOfWeek

enum class TimeOfDayBucket(val label: String) {
    MORNING("Morning"),
    AFTERNOON("Afternoon"),
    EVENING("Evening"),
    NIGHT("Night"),
}

data class WeekdayStat(val dayOfWeek: DayOfWeek, val rate: Float, val completions: Int)

data class InsightsSummary(
    val hasData: Boolean,
    val bestWeekday: WeekdayStat?,
    val worstWeekday: WeekdayStat?,
    val bestTimeOfDay: TimeOfDayBucket?,
    val longestStreakEver: Int,
    val longestStreakHabitName: String?,
    val completionsThisMonth: Int,
    val completionsLastMonth: Int,
    val monthOverMonthDelta: Int,
)
