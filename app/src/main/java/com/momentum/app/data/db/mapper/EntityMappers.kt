package com.momentum.app.data.db.mapper

import com.momentum.app.data.db.entity.CompletionEntity
import com.momentum.app.data.db.entity.HabitEntity
import com.momentum.app.domain.model.Completion
import com.momentum.app.domain.model.Habit
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

fun HabitEntity.toDomain(): Habit = Habit(
    id = id,
    name = name,
    iconKey = iconKey,
    colorKey = colorKey,
    frequency = frequency,
    targetDaysPerWeek = targetDaysPerWeek,
    reminderTime = reminderTimeSecondOfDay?.let { LocalTime.ofSecondOfDay(it.toLong()) },
    sortOrder = sortOrder,
    createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
    archived = archived,
)

fun Habit.toEntity(): HabitEntity = HabitEntity(
    id = id,
    name = name,
    iconKey = iconKey,
    colorKey = colorKey,
    frequency = frequency,
    targetDaysPerWeek = targetDaysPerWeek,
    reminderTimeSecondOfDay = reminderTime?.toSecondOfDay(),
    sortOrder = sortOrder,
    createdAtEpochMillis = createdAt.toEpochMilli(),
    archived = archived,
)

fun CompletionEntity.toDomain(): Completion = Completion(
    id = id,
    habitId = habitId,
    date = LocalDate.ofEpochDay(date),
    completedAt = Instant.ofEpochMilli(completedAtEpochMillis),
)

fun Completion.toEntity(): CompletionEntity = CompletionEntity(
    id = id,
    habitId = habitId,
    date = date.toEpochDay(),
    completedAtEpochMillis = completedAt.toEpochMilli(),
)
