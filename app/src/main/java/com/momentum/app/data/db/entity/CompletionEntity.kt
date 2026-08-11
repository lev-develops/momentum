package com.momentum.app.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "completions",
    indices = [Index(value = ["habitId", "date"], unique = true)],
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class CompletionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val habitId: Long,
    /** Days since the epoch (1970-01-01), i.e. [java.time.LocalDate.toEpochDay]. */
    val date: Long,
    /** Wall-clock moment the completion was logged; used only for Insights' time-of-day stats. */
    val completedAtEpochMillis: Long,
)
