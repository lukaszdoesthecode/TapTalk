package com.example.taptalk.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * The main database class for the application.
 *
 * This class is annotated with `@Database` and serves as the main access point to the
 * persisted data. It defines the list of entities included in the database and provides
 * abstract methods to get Data Access Objects (DAOs) for each entity.
 *
 * The database is managed by the Room persistence library.
 *
 * @property entities The list of entity classes that are part of this database.
 * @property version The version of the database schema. This must be incremented when the schema changes.
 */
@Database(entities = [FastSettingsEntity::class, HistoryEntity::class], version = 3)
abstract class AppDatabase : RoomDatabase() {
    abstract fun fastSettingsDao(): FastSettingsDao
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tap_talk_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
