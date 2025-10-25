package com.example.taptalk.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fast_settings")
data class FastSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val volume: Float,
    val selectedVoice: String,
    val aiSupport: Boolean,
    val isSynced: Boolean = false
)
