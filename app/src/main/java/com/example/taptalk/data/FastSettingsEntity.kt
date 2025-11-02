package com.example.taptalk.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents the user's fast settings configuration, stored in the "fast_settings" table.
 *
 * This data class defines the structure for user-configurable settings that provide quick
 * access to common application features. It is used by Room to create the database table.
 *
 * @property id The primary key for the entity. Defaults to 0, as there should only be one row.
 * @property volume The current volume level for text-to-speech, represented as a float. Defaults to 50f.
 * @property selectedVoice The name or identifier of the selected text-to-speech voice. Defaults to "Kate".
 * @property aiSupport Flag indicating whether AI-powered suggestions are enabled. Defaults to true.
 * @property autoSpeak Flag indicating whether entered text should be spoken automatically. Defaults to true.
 * @property isSynced Flag to check if the settings are synchronized with a remote server. Defaults to true.
 * @property gridSize The selected grid size for the main interface (e.g., "Small", "Medium", "Large"). Defaults to "Medium".
 */
@Entity(tableName = "fast_settings")
data class FastSettingsEntity(
    @PrimaryKey val id: Int = 0,
    val volume: Float = 50f,
    val selectedVoice: String = "Kate",
    val aiSupport: Boolean = true,
    val autoSpeak: Boolean = true,
    val isSynced: Boolean = true,
    val gridSize: String = "Medium"
)
