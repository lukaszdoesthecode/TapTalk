package com.example.taptalk.data

import androidx.room.*

/**
 * Data Access Object (DAO) for the [HistoryEntity] class.
 * Provides methods for interacting with the 'history' table in the database.
 */
@Dao
interface HistoryDao {
    @Insert
    fun insert(history: HistoryEntity)

    @Query("SELECT * FROM history ORDER BY timestamp ASC")
    fun getAll(): List<HistoryEntity>

    @Query("SELECT * FROM history ORDER BY timestamp DESC LIMIT 15")
    fun getRecent(): List<HistoryEntity>
}