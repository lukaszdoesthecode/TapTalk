package com.example.taptalk.viewmodel

import androidx.lifecycle.ViewModel
import com.example.taptalk.data.FastSettingsEntity
import com.example.taptalk.data.FastSettingsRepository

/**
 * ViewModel for managing the user's "fast settings", such as volume, voice selection, and AI support.
 *
 * This ViewModel is responsible for:
 * - Loading the settings from a local repository or fetching them from a remote source (Firebase) if no local data exists.
 * - Providing access to the current settings values (volume, selectedVoice, aiSupport).
 * - Saving the updated settings to both the local repository and the remote Firebase database.
 *
 * @param repo The repository responsible for data operations (local and remote fetching/saving).
 * @param userId The ID of the current user, used to identify the correct document in Firebase.
 */
class FastSettingsViewModel(
    private val repo: FastSettingsRepository,
    private val userId: String
) : ViewModel() {

    var volume: Float = 50f
    var selectedVoice: String = "Kate"
    var aiSupport: Boolean = true

    /**
     * Saves the current fast settings to both the local database and Firebase Firestore.
     *
     * This function first creates a `FastSettingsEntity` with the current ViewModel properties
     * (`volume`, `selectedVoice`, `aiSupport`) and saves it to the local repository.
     * It then updates the corresponding document in the user's "Fast_Settings" collection
     * on Firebase Firestore to ensure the settings are synchronized across devices.
     *
     * This is a suspend function and should be called from a coroutine scope.
     */
    suspend fun saveSettings() {
        val entity = FastSettingsEntity(
            id = 0,
            volume = volume,
            selectedVoice = selectedVoice,
            aiSupport = aiSupport
        )
        repo.saveLocalSettings(entity)

        val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        firestore.collection("USERS")
            .document(userId)
            .collection("Fast_Settings")
            .document("current")
            .update(
                mapOf(
                    "volume" to volume,
                    "selectedVoice" to selectedVoice,
                    "aiSupport" to aiSupport
                )
            )
    }

}
