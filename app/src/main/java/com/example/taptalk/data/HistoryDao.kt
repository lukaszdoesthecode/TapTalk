package com.example.taptalk.data

import androidx.room.*

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY timestamp DESC LIMIT 500")
    suspend fun getRecent(): List<HistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: HistoryEntity)

    @Query("UPDATE history SET isSynced = :synced WHERE id = :id")
    suspend fun updateSyncStatus(id: Int, synced: Boolean)
}
