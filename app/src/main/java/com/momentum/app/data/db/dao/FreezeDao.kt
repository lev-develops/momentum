package com.momentum.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.momentum.app.data.db.entity.HabitFreezeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FreezeDao {

    @Query("SELECT * FROM habit_freezes WHERE habitId = :habitId")
    fun observeForHabit(habitId: Long): Flow<List<HabitFreezeEntity>>

    @Query("SELECT * FROM habit_freezes")
    fun observeAll(): Flow<List<HabitFreezeEntity>>

    @Query("SELECT * FROM habit_freezes WHERE habitId = :habitId AND date = :date LIMIT 1")
    suspend fun find(habitId: Long, date: Long): HabitFreezeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(freeze: HabitFreezeEntity)
}
