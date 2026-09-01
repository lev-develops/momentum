package com.momentum.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.momentum.app.domain.model.HabitColor
import com.momentum.app.domain.model.HabitFrequency
import com.momentum.app.domain.model.HabitIcon

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val iconKey: HabitIcon,
    val colorKey: HabitColor,
    val frequency: HabitFrequency,
    val targetDaysPerWeek: Int,
    val reminderTimeSecondOfDay: Int?,
    val sortOrder: Int,
    val createdAtEpochMillis: Long,
    val archived: Boolean,
    val updatedAtEpochMillis: Long = createdAtEpochMillis,
    val category: String = "",
    val freezesAvailable: Int = 2,
)
