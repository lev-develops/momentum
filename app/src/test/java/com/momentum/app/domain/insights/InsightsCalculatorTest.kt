package com.momentum.app.domain.insights

import com.momentum.app.domain.model.Completion
import com.momentum.app.domain.model.Habit
import com.momentum.app.domain.model.HabitFrequency
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InsightsCalculatorTest {

    private val zone = ZoneId.of("UTC")

    @Test
    fun `no completions yields hasData false`() {
        val summary = InsightsCalculator.summarize(
            habits = emptyList(),
            completionsByHabit = emptyMap(),
            today = LocalDate.of(2024, 6, 15),
            zone = zone,
        )
        assertFalse(summary.hasData)
    }

    @Test
    fun `longest streak ever picks the habit with the longest run`() {
        val createdAt = LocalDate.of(2024, 1, 1).atStartOfDay(zone).toInstant()
        val habitA = Habit(id = 1, name = "Run", frequency = HabitFrequency.DAILY, targetDaysPerWeek = 7, createdAt = createdAt)
        val habitB = Habit(id = 2, name = "Read", frequency = HabitFrequency.DAILY, targetDaysPerWeek = 7, createdAt = createdAt)

        val today = LocalDate.of(2024, 6, 15)
        val aDates = (0..2).map { today.minusDays(it.toLong()) } // 3-day streak
        val bDates = (0..9).map { today.minusDays(it.toLong()) } // 10-day streak

        val summary = InsightsCalculator.summarize(
            habits = listOf(habitA, habitB),
            completionsByHabit = mapOf(
                1L to aDates.map { Completion(habitId = 1, date = it) },
                2L to bDates.map { Completion(habitId = 2, date = it) },
            ),
            today = today,
            zone = zone,
        )

        assertEquals(10, summary.longestStreakEver)
        assertEquals("Read", summary.longestStreakHabitName)
    }

    @Test
    fun `best time of day reflects when completions actually happened`() {
        val createdAt = LocalDate.of(2024, 1, 1).atStartOfDay(zone).toInstant()
        val habit = Habit(id = 1, name = "Meditate", frequency = HabitFrequency.DAILY, targetDaysPerWeek = 7, createdAt = createdAt)
        val morning = Instant.parse("2024-06-10T07:00:00Z")
        val alsoMorning = Instant.parse("2024-06-11T08:00:00Z")
        val night = Instant.parse("2024-06-12T23:00:00Z")

        val summary = InsightsCalculator.summarize(
            habits = listOf(habit),
            completionsByHabit = mapOf(
                1L to listOf(
                    Completion(habitId = 1, date = LocalDate.of(2024, 6, 10), completedAt = morning),
                    Completion(habitId = 1, date = LocalDate.of(2024, 6, 11), completedAt = alsoMorning),
                    Completion(habitId = 1, date = LocalDate.of(2024, 6, 12), completedAt = night),
                ),
            ),
            today = LocalDate.of(2024, 6, 15),
            zone = zone,
        )

        assertEquals(TimeOfDayBucket.MORNING, summary.bestTimeOfDay)
    }

    @Test
    fun `month over month delta reflects this month vs last month completions`() {
        val createdAt = LocalDate.of(2023, 1, 1).atStartOfDay(zone).toInstant()
        val habit = Habit(id = 1, name = "Write", frequency = HabitFrequency.DAILY, targetDaysPerWeek = 7, createdAt = createdAt)
        val today = LocalDate.of(2024, 6, 15)

        val thisMonthDates = listOf(LocalDate.of(2024, 6, 1), LocalDate.of(2024, 6, 2))
        val lastMonthDates = listOf(LocalDate.of(2024, 5, 1))

        val summary = InsightsCalculator.summarize(
            habits = listOf(habit),
            completionsByHabit = mapOf(
                1L to (thisMonthDates + lastMonthDates).map { Completion(habitId = 1, date = it) },
            ),
            today = today,
            zone = zone,
        )

        assertEquals(2, summary.completionsThisMonth)
        assertEquals(1, summary.completionsLastMonth)
        assertEquals(1, summary.monthOverMonthDelta)
        assertTrue(summary.hasData)
    }
}
