package com.momentum.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.momentum.app.data.db.entity.HabitTombstoneEntity

@Dao
interface TombstoneDao {

    @Query("SELECT * FROM habit_tombstones")
    suspend fun getAllOnce(): List<HabitTombstoneEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(tombstone: HabitTombstoneEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(tombstones: List<HabitTombstoneEntity>)

    @Query("DELETE FROM habit_tombstones WHERE deletedAtEpochMillis < :beforeEpochMillis")
    suspend fun deleteOlderThan(beforeEpochMillis: Long)

    @Query("DELETE FROM habit_tombstones")
    suspend fun deleteAll()
}
