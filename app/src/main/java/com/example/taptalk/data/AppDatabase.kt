package com.example.taptalk.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * The main database class for the application.
 *
 * This class is annotated with `@Database` and serves as the main access point to the
 * persisted data. It defines the list of entities included in the database, provides
 * abstract methods to get Data Access Objects (DAOs) for each entity, and specifies
 * type converters for custom data types.
 *
 * The database is managed by the Room persistence library.
 *
 * @property entities The list of entity classes that are part of this database.
 * @property version The version of the database schema. This must be incremented when the schema changes.
 * @property typeConverters The list of [TypeConverter] classes to use in the database.
 */
@Database(
    entities = [
        FastSettingsEntity::class,
        HistoryEntity::class,
        CustomWordEntity::class,
        UserCategoryEntity::class
    ],
    version = 4,
)

@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun fastSettingsDao(): FastSettingsDao
    abstract fun historyDao(): HistoryDao
    abstract fun userCategoryDao(): UserCategoryDao
    abstract fun customWordDao(): CustomWordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
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
