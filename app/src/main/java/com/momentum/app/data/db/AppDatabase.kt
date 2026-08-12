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
import com.momentum.app.data.db.entity.CompletionEntity
import com.momentum.app.data.db.entity.HabitEntity

@Database(
    entities = [HabitEntity::class, CompletionEntity::class],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun habitDao(): HabitDao
    abstract fun completionDao(): CompletionDao

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

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME,
                ).addMigrations(MIGRATION_1_2).build().also { instance = it }
            }
    }
}
