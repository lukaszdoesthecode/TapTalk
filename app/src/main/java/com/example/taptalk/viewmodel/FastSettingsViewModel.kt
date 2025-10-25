package com.example.taptalk.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taptalk.data.*
import kotlinx.coroutines.launch

class FastSettingsViewModel(
    private val repository: FastSettingsRepository,
    private val userId: String
) : ViewModel() {

    var volume = 65f
    var selectedVoice = "Kate"
    var aiSupport = true

    fun saveSettings() {
        val settings = FastSettingsEntity(volume = volume, selectedVoice = selectedVoice, aiSupport = aiSupport)
        viewModelScope.launch {
            repository.saveSettingsLocally(settings)
            repository.syncIfOnline(userId)
        }
    }

    fun addSentence(sentence: String) {
        viewModelScope.launch {
            repository.addSentenceLocally(sentence)
            repository.syncIfOnline(userId)
        }
    }
}
