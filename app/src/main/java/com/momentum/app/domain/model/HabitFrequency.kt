package com.momentum.app.domain.model

/**
 * How often a habit is expected to be done.
 * [WEEKLY_TARGET] tracks a target count of days per week rather than every calendar day,
 * so a miss doesn't necessarily break the streak the way it does for [DAILY].
 */
enum class HabitFrequency {
    DAILY,
    WEEKLY_TARGET,
}
