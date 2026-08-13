package com.momentum.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habit_tombstones")
data class HabitTombstoneEntity(
    @PrimaryKey
    val habitId: Long,
    val deletedAtEpochMillis: Long,
)
