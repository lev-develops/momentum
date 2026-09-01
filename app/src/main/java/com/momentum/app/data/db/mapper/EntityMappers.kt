package com.momentum.app.data.db.mapper

import com.momentum.app.data.db.entity.CompletionEntity
import com.momentum.app.data.db.entity.HabitEntity
import com.momentum.app.data.db.entity.HabitFreezeEntity
import com.momentum.app.data.db.entity.HabitTombstoneEntity
import com.momentum.app.domain.model.Completion
import com.momentum.app.domain.model.Habit
import com.momentum.app.domain.model.HabitTombstone
import com.momentum.app.domain.model.StreakFreeze
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
    updatedAt = Instant.ofEpochMilli(updatedAtEpochMillis),
    category = category,
    freezesAvailable = freezesAvailable,
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
    updatedAtEpochMillis = updatedAt.toEpochMilli(),
    category = category,
    freezesAvailable = freezesAvailable,
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

fun HabitTombstoneEntity.toDomain(): HabitTombstone = HabitTombstone(
    habitId = habitId,
    deletedAt = Instant.ofEpochMilli(deletedAtEpochMillis),
)

fun HabitTombstone.toEntity(): HabitTombstoneEntity = HabitTombstoneEntity(
    habitId = habitId,
    deletedAtEpochMillis = deletedAt.toEpochMilli(),
)

fun HabitFreezeEntity.toDomain(): StreakFreeze = StreakFreeze(
    habitId = habitId,
    date = LocalDate.ofEpochDay(date),
)

fun StreakFreeze.toEntity(): HabitFreezeEntity = HabitFreezeEntity(
    habitId = habitId,
    date = date.toEpochDay(),
)
