package com.example.taptalk.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_words")
data class CustomWordEntity(
    @PrimaryKey val label: String,
    val folder: String,
    val imagePath: String? = null,
    val synced: Boolean = false,
    val userId: String = ""
)
