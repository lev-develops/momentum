package com.momentum.app.domain.model

import java.time.LocalDate

/** A single missed day that was covered by spending one of the habit's freeze tokens, so it
 * still counts toward streak continuity even though it wasn't actually completed. */
data class StreakFreeze(
    val habitId: Long,
    val date: LocalDate,
)
