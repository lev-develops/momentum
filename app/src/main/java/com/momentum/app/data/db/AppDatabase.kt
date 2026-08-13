package com.momentum.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.momentum.app.data.db.dao.CompletionDao
import com.momentum.app.data.db.dao.HabitDao
import com.momentum.app.data.db.dao.TombstoneDao
import com.momentum.app.data.db.entity.CompletionEntity
import com.momentum.app.data.db.entity.HabitEntity
import com.momentum.app.data.db.entity.HabitTombstoneEntity

@Database(
    entities = [HabitEntity::class, CompletionEntity::class, HabitTombstoneEntity::class],
    version = 3,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun habitDao(): HabitDao
    abstract fun completionDao(): CompletionDao
    abstract fun tombstoneDao(): TombstoneDao

    companion object {
        private const val DATABASE_NAME = "momentum.db"

        /** Adds the column cloud sync uses for last-write-wins conflict resolution on habits. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE habits ADD COLUMN updatedAtEpochMillis INTEGER NOT NULL DEFAULT 0",
                )
                db.execSQL(
                    "UPDATE habits SET updatedAtEpochMillis = createdAtEpochMillis",
                )
            }
        }

        /** Tracks habit deletions so cloud sync doesn't resurrect them from a stale remote copy. */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS habit_tombstones (" +
                        "habitId INTEGER NOT NULL PRIMARY KEY, " +
                        "deletedAtEpochMillis INTEGER NOT NULL)",
                )
            }
        }

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME,
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { instance = it }
            }
    }
}
