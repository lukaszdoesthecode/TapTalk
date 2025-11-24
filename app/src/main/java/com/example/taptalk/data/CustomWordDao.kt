package com.example.taptalk.data

import androidx.room.*

@Dao
interface CustomWordDao {

    @Query("SELECT * FROM custom_words")
    suspend fun getAll(): List<CustomWordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(word: CustomWordEntity)

    @Delete
    suspend fun delete(word: CustomWordEntity)

    @Query("DELETE FROM custom_words")
    suspend fun deleteAll()
}
