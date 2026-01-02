package com.example.taptalk.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fast_settings")
data class FastSettingsEntity(
    @PrimaryKey val id: Int = 0,
    val volume: Float = 50f,
    val selectedVoice: String = "Kate",
    val aiSupport: Boolean = true,
    val isSynced: Boolean = false,
    val gridSize: String = "Medium",
    val voiceSpeed: Float = 1.0f,
    val voicePitch: Float = 1.0f,
    val userId: String = ""
)
