package com.momentum.app.domain.streak

import com.momentum.app.domain.model.HabitFrequency
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class StreakCalculatorTest {

    private val today = LocalDate.of(2024, 6, 15) // a Saturday

    private fun daily(dates: Set<LocalDate>, asOf: LocalDate = today) =
        StreakCalculator.currentStreak(dates, HabitFrequency.DAILY, targetDaysPerWeek = 7, today = asOf)

    private fun bestDaily(dates: Set<LocalDate>, asOf: LocalDate = today) =
        StreakCalculator.bestStreak(dates, HabitFrequency.DAILY, targetDaysPerWeek = 7, today = asOf)

    // ---- today incomplete ----

    @Test
    fun `current streak counts through yesterday when today is not yet done`() {
        val dates = setOf(today.minusDays(1), today.minusDays(2), today.minusDays(3))
        assertEquals(3, daily(dates))
    }

    @Test
    fun `current streak includes today once it is done`() {
        val dates = setOf(today, today.minusDays(1), today.minusDays(2))
        assertEquals(3, daily(dates))
    }

    @Test
    fun `today incomplete does not erase an already-broken streak`() {
        // Yesterday also missing -> no active streak, regardless of today.
        val dates = setOf(today.minusDays(3))
        assertEquals(0, daily(dates))
    }

    // ---- gaps ----

    @Test
    fun `a gap breaks the current streak at the gap`() {
        val dates = setOf(today, today.minusDays(1), today.minusDays(3)) // missing minusDays(2)
        assertEquals(2, daily(dates))
    }

    @Test
    fun `best streak finds the longest run across multiple gaps`() {
        val d = today
        val dates = setOf(
            d.minusDays(10), d.minusDays(9), d.minusDays(8), // run of 3
            d.minusDays(6), d.minusDays(5), // run of 2
            d.minusDays(2), d.minusDays(1), d, // run of 3
        )
        assertEquals(3, bestDaily(dates))
    }

    @Test
    fun `best streak ignores order of insertion`() {
        val dates = setOf(today.minusDays(1), today, today.minusDays(2))
        assertEquals(3, bestDaily(dates))
    }

    // ---- single day ----

    @Test
    fun `single completion today is a streak of one`() {
        assertEquals(1, daily(setOf(today)))
    }

    @Test
    fun `single completion yesterday only is a streak of one`() {
        assertEquals(1, daily(setOf(today.minusDays(1))))
    }

    @Test
    fun `no completions ever is a streak of zero`() {
        assertEquals(0, daily(emptySet()))
        assertEquals(0, bestDaily(emptySet()))
    }

    // ---- local-midnight rollover ----

    @Test
    fun `local date derived from instant rolls over exactly at local midnight, not UTC midnight`() {
        val zone = ZoneId.of("America/New_York")
        // 2024-03-08 23:59 local (America/New_York, UTC-5) == 2024-03-09T04:59:00Z
        val justBeforeMidnight = Clock.fixed(Instant.parse("2024-03-09T04:59:00Z"), zone)
        // 2024-03-09 00:01 local == 2024-03-09T05:01:00Z
        val justAfterMidnight = Clock.fixed(Instant.parse("2024-03-09T05:01:00Z"), zone)

        assertEquals(LocalDate.of(2024, 3, 8), LocalDate.now(justBeforeMidnight))
        assertEquals(LocalDate.of(2024, 3, 9), LocalDate.now(justAfterMidnight))
    }

    @Test
    fun `a streak logged right at local midnight rollover still counts as consecutive days`() {
        val zone = ZoneId.of("America/New_York")
        val mar8 = LocalDate.of(2024, 3, 8)
        val mar9 = LocalDate.of(2024, 3, 9)
        val dates = setOf(mar8, mar9)
        val asOfMar9Morning = Clock.fixed(Instant.parse("2024-03-09T13:00:00Z"), zone) // 8am local Mar 9
        assertEquals(2, StreakCalculator.currentStreak(dates, HabitFrequency.DAILY, 7, LocalDate.now(asOfMar9Morning)))
    }

    // ---- DST transitions ----

    @Test
    fun `streak across a spring-forward DST transition still counts each calendar day once`() {
        // America/New_York springs forward on 2024-03-10 (02:00 -> 03:00); the calendar date
        // boundary itself (midnight) is unaffected, so three consecutive dates around it must
        // still read as a 3-day streak.
        val mar9 = LocalDate.of(2024, 3, 9)
        val mar10 = LocalDate.of(2024, 3, 10)
        val mar11 = LocalDate.of(2024, 3, 11)
        val dates = setOf(mar9, mar10, mar11)
        assertEquals(3, StreakCalculator.currentStreak(dates, HabitFrequency.DAILY, 7, today = mar11))
        assertEquals(3, StreakCalculator.bestStreak(dates, HabitFrequency.DAILY, 7, today = mar11))
    }

    @Test
    fun `streak across a fall-back DST transition still counts each calendar day once`() {
        // America/New_York falls back on 2024-11-03 (02:00 -> 01:00), a 25-hour local day.
        val nov2 = LocalDate.of(2024, 11, 2)
        val nov3 = LocalDate.of(2024, 11, 3)
        val nov4 = LocalDate.of(2024, 11, 4)
        val dates = setOf(nov2, nov3, nov4)
        assertEquals(3, StreakCalculator.currentStreak(dates, HabitFrequency.DAILY, 7, today = nov4))
    }

    @Test
    fun `instant-to-local-date conversion is safe even across a midnight DST transition`() {
        // America/Sao_Paulo historically sprang forward at local midnight (00:00 -> 01:00),
        // the one case where a DST shift coincides exactly with the date boundary we care about.
        val zone = ZoneId.of("America/Sao_Paulo")
        val nov3_2018 = Instant.parse("2018-11-03T20:00:00Z") // afternoon, Nov 3 local
        val nov4_2018 = Instant.parse("2018-11-04T20:00:00Z") // afternoon, Nov 4 local, after the jump

        val dateBefore = LocalDate.now(Clock.fixed(nov3_2018, zone))
        val dateAfter = LocalDate.now(Clock.fixed(nov4_2018, zone))

        assertEquals(LocalDate.of(2018, 11, 3), dateBefore)
        assertEquals(LocalDate.of(2018, 11, 4), dateAfter)
    }

    // ---- weekly target ----

    @Test
    fun `weekly target streak counts consecutive weeks meeting target`() {
        // Weeks run Sunday-Saturday. today = Sat 2024-06-15 is in the week starting Sun 2024-06-09.
        val weekStart = LocalDate.of(2024, 6, 9)
        val prevWeekStart = weekStart.minusDays(7)
        val dates = setOf(
            weekStart, weekStart.plusDays(1), weekStart.plusDays(2), // 3 this week (target met)
            prevWeekStart, prevWeekStart.plusDays(1), prevWeekStart.plusDays(3), // 3 last week
        )
        val streak = StreakCalculator.currentStreak(dates, HabitFrequency.WEEKLY_TARGET, targetDaysPerWeek = 3, today = today)
        assertEquals(2, streak)
    }

    @Test
    fun `weekly target streak is not broken by an in-progress current week`() {
        val weekStart = LocalDate.of(2024, 6, 9)
        val prevWeekStart = weekStart.minusDays(7)
        // Only 1 completion so far this week (in progress, target 3) but last week fully met.
        val dates = setOf(
            weekStart,
            prevWeekStart, prevWeekStart.plusDays(1), prevWeekStart.plusDays(3),
        )
        val streak = StreakCalculator.currentStreak(dates, HabitFrequency.WEEKLY_TARGET, targetDaysPerWeek = 3, today = today)
        assertEquals(1, streak)
    }
}
