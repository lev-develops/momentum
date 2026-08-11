package com.momentum.app.data.repository

import com.momentum.app.data.db.dao.CompletionDao
import com.momentum.app.data.db.dao.HabitDao
import com.momentum.app.data.db.mapper.toDomain
import com.momentum.app.data.db.mapper.toEntity
import com.momentum.app.domain.model.Completion
import com.momentum.app.domain.model.Habit
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

interface HabitRepository {
    fun observeActiveHabits(): Flow<List<Habit>>
    fun observeAllHabits(): Flow<List<Habit>>
    fun observeHabit(id: Long): Flow<Habit?>
    fun observeCompletions(habitId: Long): Flow<List<Completion>>
    fun observeCompletionsByHabit(): Flow<Map<Long, List<Completion>>>
    fun observeCompletedDates(habitId: Long): Flow<Set<LocalDate>>

    suspend fun getHabit(id: Long): Habit?
    suspend fun isCompleted(habitId: Long, date: LocalDate): Boolean
    suspend fun getAllHabitsOnce(): List<Habit>
    suspend fun getAllCompletionsOnce(): List<Completion>

    /** Flips the habit's completion for [date] and returns the new state. */
    suspend fun toggleCompletion(habitId: Long, date: LocalDate): Boolean

    suspend fun addHabit(habit: Habit): Long
    suspend fun updateHabit(habit: Habit)
    suspend fun deleteHabit(habit: Habit)
    suspend fun setArchived(habitId: Long, archived: Boolean)
    suspend fun reorder(orderedHabitIds: List<Long>)

    /** Wipes all habits and completions, then inserts the given data (used by JSON import). */
    suspend fun replaceAllData(habits: List<Habit>, completions: List<Completion>)
}

class HabitRepositoryImpl(
    private val habitDao: HabitDao,
    private val completionDao: CompletionDao,
    private val clock: Clock = Clock.systemDefaultZone(),
) : HabitRepository {

    override fun observeActiveHabits(): Flow<List<Habit>> =
        habitDao.observeActive().map { list -> list.map { it.toDomain() } }

    override fun observeAllHabits(): Flow<List<Habit>> =
        habitDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeHabit(id: Long): Flow<Habit?> =
        habitDao.observeById(id).map { it?.toDomain() }

    override fun observeCompletions(habitId: Long): Flow<List<Completion>> =
        completionDao.observeForHabit(habitId).map { list -> list.map { it.toDomain() } }

    override fun observeCompletedDates(habitId: Long): Flow<Set<LocalDate>> =
        observeCompletions(habitId).map { list -> list.map { it.date }.toSet() }

    override fun observeCompletionsByHabit(): Flow<Map<Long, List<Completion>>> =
        combine(habitDao.observeAll(), completionDao.observeAll()) { habits, completions ->
            val domainCompletions = completions.map { it.toDomain() }
            habits.associate { habit -> habit.id to domainCompletions.filter { it.habitId == habit.id } }
        }

    override suspend fun getHabit(id: Long): Habit? = habitDao.getById(id)?.toDomain()

    override suspend fun isCompleted(habitId: Long, date: LocalDate): Boolean =
        completionDao.find(habitId, date.toEpochDay()) != null

    override suspend fun getAllHabitsOnce(): List<Habit> = habitDao.getAllOnce().map { it.toDomain() }

    override suspend fun getAllCompletionsOnce(): List<Completion> =
        completionDao.getAllOnce().map { it.toDomain() }

    override suspend fun toggleCompletion(habitId: Long, date: LocalDate): Boolean {
        val epochDay = date.toEpochDay()
        val existing = completionDao.find(habitId, epochDay)
        return if (existing != null) {
            completionDao.deleteFor(habitId, epochDay)
            false
        } else {
            val completion = Completion(habitId = habitId, date = date, completedAt = Instant.now(clock))
            completionDao.insert(completion.toEntity())
            true
        }
    }

    override suspend fun addHabit(habit: Habit): Long = habitDao.upsert(habit.toEntity())

    override suspend fun updateHabit(habit: Habit) = habitDao.update(habit.toEntity())

    override suspend fun deleteHabit(habit: Habit) = habitDao.delete(habit.toEntity())

    override suspend fun setArchived(habitId: Long, archived: Boolean) {
        val habit = habitDao.getById(habitId) ?: return
        habitDao.update(habit.copy(archived = archived))
    }

    override suspend fun reorder(orderedHabitIds: List<Long>) {
        orderedHabitIds.forEachIndexed { index, id -> habitDao.updateSortOrder(id, index) }
    }

    override suspend fun replaceAllData(habits: List<Habit>, completions: List<Completion>) {
        completionDao.deleteAll()
        habitDao.deleteAll()
        habitDao.upsertAll(habits.map { it.toEntity() })
        completionDao.insertAll(completions.map { it.toEntity() })
    }
}
