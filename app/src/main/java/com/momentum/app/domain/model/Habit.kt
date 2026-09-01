package com.momentum.app.domain.model

import java.time.Instant
import java.time.LocalTime

data class Habit(
    val id: Long = 0L,
    val name: String,
    val iconKey: HabitIcon = HabitIcon.Default,
    val colorKey: HabitColor = HabitColor.Default,
    val frequency: HabitFrequency = HabitFrequency.DAILY,
    val targetDaysPerWeek: Int = 7,
    val reminderTime: LocalTime? = null,
    val sortOrder: Int = 0,
    val createdAt: Instant = Instant.now(),
    val archived: Boolean = false,
    /** Last time this habit's own fields (not its completions) changed; drives last-write-wins
     * conflict resolution when merging with cloud sync. */
    val updatedAt: Instant = createdAt,
    /** Optional grouping label, e.g. "Health", "Work" — blank means uncategorized. */
    val category: String = "",
    /** Streak-freeze tokens left; spending one covers a missed day without breaking the streak. */
    val freezesAvailable: Int = DEFAULT_FREEZES,
) {
    companion object {
        const val DEFAULT_FREEZES = 2
    }
}
