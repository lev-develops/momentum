package com.momentum.app.data.repository

import androidx.room.RoomDatabase
import androidx.room.withTransaction
import com.momentum.app.data.db.dao.CompletionDao
import com.momentum.app.data.db.dao.FreezeDao
import com.momentum.app.data.db.dao.HabitDao
import com.momentum.app.data.db.dao.TombstoneDao
import com.momentum.app.data.db.mapper.toDomain
import com.momentum.app.data.db.mapper.toEntity
import com.momentum.app.domain.model.Completion
import com.momentum.app.domain.model.Habit
import com.momentum.app.domain.model.HabitTombstone
import com.momentum.app.domain.model.StreakFreeze
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
    fun observeFrozenDates(habitId: Long): Flow<Set<LocalDate>>
    fun observeFreezesByHabit(): Flow<Map<Long, Set<LocalDate>>>

    suspend fun getHabit(id: Long): Habit?
    suspend fun isCompleted(habitId: Long, date: LocalDate): Boolean
    suspend fun getAllHabitsOnce(): List<Habit>
    suspend fun getAllCompletionsOnce(): List<Completion>

    /** Flips the habit's completion for [date] and returns the new state. */
    suspend fun toggleCompletion(habitId: Long, date: LocalDate): Boolean

    /** Spends one of the habit's freeze tokens to cover [date] (must be missed, not already
     * frozen, and the habit must have a token left). Returns whether it was applied. */
    suspend fun useFreeze(habitId: Long, date: LocalDate): Boolean

    suspend fun addHabit(habit: Habit): Long
    suspend fun updateHabit(habit: Habit)
    suspend fun deleteHabit(habit: Habit)
    suspend fun setArchived(habitId: Long, archived: Boolean)
    suspend fun reorder(orderedHabitIds: List<Long>)

    /** Wipes all habits and completions, then inserts the given data (used by JSON import). */
    suspend fun replaceAllData(habits: List<Habit>, completions: List<Completion>)

    /** Records of deleted habits, kept so cloud sync doesn't resurrect them from a stale remote copy. */
    suspend fun getAllTombstones(): List<HabitTombstone>
    suspend fun saveTombstones(tombstones: List<HabitTombstone>)
    suspend fun pruneTombstonesOlderThan(cutoff: Instant)
}

class HabitRepositoryImpl(
    private val habitDao: HabitDao,
    private val completionDao: CompletionDao,
    private val tombstoneDao: TombstoneDao,
    private val freezeDao: FreezeDao,
    private val database: RoomDatabase,
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

    override fun observeFrozenDates(habitId: Long): Flow<Set<LocalDate>> =
        freezeDao.observeForHabit(habitId).map { list -> list.map { it.toDomain().date }.toSet() }

    override fun observeFreezesByHabit(): Flow<Map<Long, Set<LocalDate>>> =
        freezeDao.observeAll().map { list ->
            list.map { it.toDomain() }
                .groupBy({ it.habitId }, { it.date })
                .mapValues { (_, dates) -> dates.toSet() }
        }

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

    override suspend fun useFreeze(habitId: Long, date: LocalDate): Boolean = database.withTransaction {
        val habit = habitDao.getById(habitId) ?: return@withTransaction false
        if (habit.freezesAvailable <= 0) return@withTransaction false
        if (completionDao.find(habitId, date.toEpochDay()) != null) return@withTransaction false
        if (freezeDao.find(habitId, date.toEpochDay()) != null) return@withTransaction false
        freezeDao.insert(StreakFreeze(habitId = habitId, date = date).toEntity())
        habitDao.update(habit.copy(freezesAvailable = habit.freezesAvailable - 1))
        true
    }

    override suspend fun addHabit(habit: Habit): Long = habitDao.upsert(habit.toEntity())

    override suspend fun updateHabit(habit: Habit) = habitDao.update(habit.toEntity())

    override suspend fun deleteHabit(habit: Habit) {
        database.withTransaction {
            habitDao.delete(habit.toEntity())
            tombstoneDao.upsert(HabitTombstone(habitId = habit.id, deletedAt = Instant.now(clock)).toEntity())
        }
    }

    override suspend fun setArchived(habitId: Long, archived: Boolean) {
        val habit = habitDao.getById(habitId) ?: return
        habitDao.update(habit.copy(archived = archived, updatedAtEpochMillis = Instant.now(clock).toEpochMilli()))
    }

    override suspend fun reorder(orderedHabitIds: List<Long>) {
        orderedHabitIds.forEachIndexed { index, id -> habitDao.updateSortOrder(id, index) }
    }

    override suspend fun replaceAllData(habits: List<Habit>, completions: List<Completion>) {
        // Wrapped in a single transaction so a crash or error partway through (e.g. a malformed
        // row) rolls back instead of leaving the database wiped with nothing restored.
        database.withTransaction {
            completionDao.deleteAll()
            habitDao.deleteAll()
            habitDao.upsertAll(habits.map { it.toEntity() })
            completionDao.insertAll(completions.map { it.toEntity() })
        }
    }

    override suspend fun getAllTombstones(): List<HabitTombstone> =
        tombstoneDao.getAllOnce().map { it.toDomain() }

    override suspend fun saveTombstones(tombstones: List<HabitTombstone>) {
        tombstoneDao.upsertAll(tombstones.map { it.toEntity() })
    }

    override suspend fun pruneTombstonesOlderThan(cutoff: Instant) {
        tombstoneDao.deleteOlderThan(cutoff.toEpochMilli())
    }
}
