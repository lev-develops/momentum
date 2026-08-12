package com.momentum.app.util

import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Emits today's date immediately, then again every time local midnight passes. Screens combine
 * this into their state flow so a habit's "completed today" checkmark clears itself at midnight
 * even if the app/ViewModel stays alive and no habit or completion data actually changes —
 * without this, `LocalDate.now()` was only ever recomputed when the database emitted, so a
 * completion ticked yesterday could still show as done today.
 */
fun currentDateFlow(clock: Clock): Flow<LocalDate> = flow {
    while (true) {
        val today = LocalDate.now(clock)
        emit(today)
        val nextMidnight = today.plusDays(1).atStartOfDay(clock.zone)
        val delayMillis = Duration.between(ZonedDateTime.now(clock), nextMidnight).toMillis().coerceAtLeast(1_000L)
        delay(delayMillis)
    }
}
