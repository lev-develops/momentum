package com.momentum.app.data.export

import com.momentum.app.domain.model.Completion
import com.momentum.app.domain.model.Habit
import com.momentum.app.domain.model.HabitColor
import com.momentum.app.domain.model.HabitFrequency
import com.momentum.app.domain.model.HabitIcon
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.serialization.Serializable

@Serializable
data class ExportPayload(
    val version: Int = 1,
    val exportedAt: String,
    val habits: List<HabitDto>,
    val completions: List<CompletionDto>,
)

@Serializable
data class HabitDto(
    val id: Long,
    val name: String,
    val iconKey: String,
    val colorKey: String,
    val frequency: String,
    val targetDaysPerWeek: Int,
    val reminderTime: String?,
    val sortOrder: Int,
    val createdAt: String,
    val archived: Boolean,
    val updatedAt: String? = null,
    val category: String? = null,
)

@Serializable
data class CompletionDto(
    val id: Long,
    val habitId: Long,
    val date: String,
    val completedAt: String,
)

fun Habit.toDto(): HabitDto = HabitDto(
    id = id,
    name = name,
    iconKey = iconKey.name,
    colorKey = colorKey.name,
    frequency = frequency.name,
    targetDaysPerWeek = targetDaysPerWeek,
    reminderTime = reminderTime?.toString(),
    sortOrder = sortOrder,
    createdAt = createdAt.toString(),
    archived = archived,
    updatedAt = updatedAt.toString(),
    category = category,
)

fun HabitDto.toDomain(): Habit {
    val parsedCreatedAt = runCatching { Instant.parse(createdAt) }.getOrDefault(Instant.now())
    return Habit(
        id = id,
        name = name,
        iconKey = runCatching { HabitIcon.valueOf(iconKey) }.getOrDefault(HabitIcon.Default),
        colorKey = runCatching { HabitColor.valueOf(colorKey) }.getOrDefault(HabitColor.Default),
        frequency = runCatching { HabitFrequency.valueOf(frequency) }.getOrDefault(HabitFrequency.DAILY),
        targetDaysPerWeek = targetDaysPerWeek,
        reminderTime = reminderTime?.let { LocalTime.parse(it) },
        sortOrder = sortOrder,
        createdAt = parsedCreatedAt,
        archived = archived,
        updatedAt = updatedAt?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: parsedCreatedAt,
        category = category.orEmpty(),
    )
}

fun Completion.toDto(): CompletionDto = CompletionDto(
    id = id,
    habitId = habitId,
    date = date.toString(),
    completedAt = completedAt.toString(),
)

fun CompletionDto.toDomain(): Completion = Completion(
    id = id,
    habitId = habitId,
    date = LocalDate.parse(date),
    completedAt = runCatching { Instant.parse(completedAt) }.getOrDefault(Instant.now()),
)
