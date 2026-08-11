package com.momentum.app.domain.model

import java.time.Instant
import java.time.LocalDate

/**
 * [date] is the authoritative calendar bucket the completion counts toward (streaks, the
 * contribution grid) and is what the habitId+date unique index enforces one-per-day on.
 * [completedAt] is the actual wall-clock moment it was logged, kept only so Insights can derive
 * "best time of day" from the user's own data; it plays no part in streak math.
 */
data class Completion(
    val id: Long = 0L,
    val habitId: Long,
    val date: LocalDate,
    val completedAt: Instant = date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant(),
)
