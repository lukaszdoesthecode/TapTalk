package com.example.taptalk.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [FastSettingsEntity::class, HistoryEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun fastSettingsDao(): FastSettingsDao
    abstract fun historyDao(): HistoryDao
}
