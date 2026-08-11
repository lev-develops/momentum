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
)
