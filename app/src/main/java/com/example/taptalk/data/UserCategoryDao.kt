package com.example.taptalk.data

import androidx.room.*

@Dao
interface UserCategoryDao {

    @Query("SELECT * FROM user_categories WHERE userId = :uid")
    suspend fun getAll(uid: String): List<UserCategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(cat: UserCategoryEntity)

    @Delete
    suspend fun delete(cat: UserCategoryEntity)

    @Query("DELETE FROM user_categories WHERE name = :name AND userId = :uid")
    suspend fun deleteByName(name: String, uid: String)

    @Query("DELETE FROM user_categories WHERE userId = :uid")
    suspend fun deleteAll(uid: String)

    @Transaction
    @Query("SELECT * FROM user_categories WHERE userId = :uid")
    suspend fun getCategoriesWithWords(uid: String): List<CategoryWithWords>
}
