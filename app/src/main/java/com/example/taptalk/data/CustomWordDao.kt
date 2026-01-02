package com.example.taptalk.data

import androidx.room.*

@Dao
interface CustomWordDao {

    @Query("SELECT * FROM custom_words WHERE userId = :uid")
    suspend fun getAll(uid: String): List<CustomWordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(word: CustomWordEntity)

    @Delete
    suspend fun delete(word: CustomWordEntity)

    @Query("DELETE FROM custom_words WHERE userId = :uid")
    suspend fun deleteAll(uid: String)
}
