package com.example.taptalk.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_categories")
data class UserCategoryEntity(
    @PrimaryKey val name: String,
    val colorHex: String,
    val imagePath: String?,
    val cardFileNames: List<String>,
    val synced: Boolean = false,
    val userId: String = ""
)
