package com.momentum.app.domain.streak

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContributionGridBuilderTest {

    @Test
    fun `grid ends on today's week and future days within that week are null`() {
        val today = LocalDate.of(2024, 6, 12) // Wednesday
        val grid = ContributionGridBuilder.build(emptySet(), targetDaysPerWeek = 7, today = today, weeks = 2)

        val lastColumn = grid.columns.last()
        // Sunday..Wednesday exist, Thursday..Saturday are in the future and must be null.
        assertEquals(today, lastColumn[3].date)
        assertNull(lastColumn[4].date)
        assertNull(lastColumn[5].date)
        assertNull(lastColumn[6].date)
    }

    @Test
    fun `level 4 is reached once the week's target is met`() {
        val today = LocalDate.of(2024, 6, 15) // Saturday, week starts Sun 2024-06-09
        val weekStart = LocalDate.of(2024, 6, 9)
        val completions = (0..6).map { weekStart.plusDays(it.toLong()) }.toSet() // every day this week
        val grid = ContributionGridBuilder.build(completions, targetDaysPerWeek = 7, today = today, weeks = 1)

        val column = grid.columns.single()
        assertEquals(1, column[0].level) // 1/7
        assertTrue(column[6].level == 4) // 7/7 -> target met
    }

    @Test
    fun `an incomplete day has level zero regardless of the week's progress`() {
        val today = LocalDate.of(2024, 6, 15)
        val weekStart = LocalDate.of(2024, 6, 9)
        val completions = setOf(weekStart, weekStart.plusDays(1), weekStart.plusDays(2), weekStart.plusDays(4))
        val grid = ContributionGridBuilder.build(completions, targetDaysPerWeek = 7, today = today, weeks = 1)

        val column = grid.columns.single()
        assertEquals(0, column[3].level) // weekStart+3 wasn't completed
        assertFalseCompleted(column[3])
    }

    private fun assertFalseCompleted(cell: GridCell) {
        assertTrue(!cell.completed)
    }
}
