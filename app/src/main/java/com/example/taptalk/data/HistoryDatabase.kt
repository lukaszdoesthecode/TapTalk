package com.example.taptalk.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * The Room database for this app, which contains the 'history_table'.
 *
 * This class is a singleton to prevent having multiple instances of the database opened at the
 * same time. It provides access to the [HistoryDao].
 *
 * @property historyDao The Data Access Object for the history table.
 */
@Database(entities = [HistoryEntity::class], version = 3, exportSchema = false)
abstract class HistoryDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile private var INSTANCE: HistoryDatabase? = null

        fun getDatabase(context: Context): HistoryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HistoryDatabase::class.java,
                    "history_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
