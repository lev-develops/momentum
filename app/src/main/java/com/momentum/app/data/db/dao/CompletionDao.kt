package com.momentum.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.momentum.app.data.db.entity.CompletionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CompletionDao {

    @Query("SELECT * FROM completions WHERE habitId = :habitId ORDER BY date ASC")
    fun observeForHabit(habitId: Long): Flow<List<CompletionEntity>>

    @Query("SELECT * FROM completions")
    fun observeAll(): Flow<List<CompletionEntity>>

    @Query("SELECT * FROM completions")
    suspend fun getAllOnce(): List<CompletionEntity>

    @Query("SELECT * FROM completions WHERE habitId = :habitId AND date = :date LIMIT 1")
    suspend fun find(habitId: Long, date: Long): CompletionEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(completion: CompletionEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(completions: List<CompletionEntity>)

    @Query("DELETE FROM completions WHERE habitId = :habitId AND date = :date")
    suspend fun deleteFor(habitId: Long, date: Long)

    @Query("DELETE FROM completions")
    suspend fun deleteAll()

    @Query("DELETE FROM completions WHERE habitId = :habitId")
    suspend fun deleteAllForHabit(habitId: Long)
}
