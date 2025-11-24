package com.example.taptalk.data

import androidx.room.*

/**
 * Data Access Object (DAO) for the [UserCategoryEntity] table.
 *
 * This interface provides the methods that the rest of the app uses to interact with the
 * `user_categories` table in the database.
 */
@Dao
interface UserCategoryDao {

    @Query("SELECT * FROM user_categories")
    suspend fun getAll(): List<UserCategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(cat: UserCategoryEntity)

    @Delete
    suspend fun delete(cat: UserCategoryEntity)

    @Query("DELETE FROM user_categories WHERE name = :name")
    suspend fun deleteByName(name: String)

    @Query("DELETE FROM user_categories")
    suspend fun deleteAll()
}
