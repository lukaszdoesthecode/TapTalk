package com.example.taptalk.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sentence: String,
    val timestamp: Long = System.currentTimeMillis(),
    val userId: String = ""
)
