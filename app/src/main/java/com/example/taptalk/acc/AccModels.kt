package com.example.taptalk.acc

import com.google.mlkit.nl.smartreply.TextMessage
import org.json.JSONObject

data class AccCard(
    val fileName: String,
    val label: String,
    val path: String,
    val folder: String
)

data class VerbForms(
    val base: String,
    val past: String,
    val perfect: String,
    val negatives: List<String> = emptyList()
)

data class AccUserSettings(
    val volume: Float = 50f,
    val selectedVoice: String = "Kate",
    val aiSupport: Boolean = true,
    val autoSpeak: Boolean = true,
    val gridSize: String = "Medium",
    val visibleLevels: List<String> = listOf("A1", "A2", "B1"),
    val darkMode: Boolean = false,
    val lowVisionMode: Boolean = false
)

data class AccUiState(
    val isLoading: Boolean = true,
    val userId: String? = null,
    val allCards: List<AccCard> = emptyList(),
    val favourites: List<AccCard> = emptyList(),
    val categories: List<AccCard> = emptyList(),
    val baseConversation: List<TextMessage> = emptyList(),
    val irregularVerbs: JSONObject? = null,
    val irregularPlurals: JSONObject? = null,
    val settings: AccUserSettings = AccUserSettings()
)

sealed class AccEvent {
    data class FavouriteToggled(val added: Boolean) : AccEvent()
    data class Error(val message: String) : AccEvent()
}
