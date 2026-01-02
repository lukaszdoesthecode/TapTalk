package com.example.taptalk.data

import androidx.room.*

@Dao
interface HistoryDao {

    @Insert
    suspend fun insert(history: HistoryEntity)

    @Query("SELECT * FROM history WHERE userId = :uid ORDER BY timestamp ASC")
    suspend fun getAll(uid: String): List<HistoryEntity>

    @Query("SELECT * FROM history WHERE userId = :uid ORDER BY timestamp DESC LIMIT 15")
    suspend fun getRecent(uid: String): List<HistoryEntity>

    @Query("DELETE FROM history WHERE userId = :uid")
    suspend fun deleteAll(uid: String)
}
