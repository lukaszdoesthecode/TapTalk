package com.example.taptalk.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.taptalk.acc.AccCard
import com.example.taptalk.acc.AccEvent
import com.example.taptalk.acc.AccRepository
import com.example.taptalk.acc.AccUiState
import com.example.taptalk.acc.AccUserSettings
import com.google.firebase.auth.FirebaseAuth
import com.google.mlkit.nl.smartreply.TextMessage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AccViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AccRepository(application.applicationContext)
    private val _uiState = MutableStateFlow(AccUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AccEvent>()
    val events = _events.asSharedFlow()

    init {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        _uiState.update { it.copy(userId = userId) }

        viewModelScope.launch {
            loadInitialData()
        }
    }

    private suspend fun loadInitialData() {
        _uiState.update { it.copy(isLoading = true) }

        val cards = repository.loadAllCards()
        val categories = repository.loadCategories()
        val conversation = repository.buildBaseConversation(cards)
        val settings = repository.loadInitialSettings(_uiState.value.userId)
        val irregularVerbs = repository.loadIrregularVerbs()
        val irregularPlurals = repository.loadIrregularPlurals()
        repository.syncHistory()

        _uiState.update {
            it.copy(
                isLoading = false,
                allCards = cards,
                categories = categories,
                baseConversation = conversation,
                settings = settings,
                irregularVerbs = irregularVerbs,
                irregularPlurals = irregularPlurals
            )
        }
    }

    fun refreshFavourites() {
        val userId = _uiState.value.userId
        if (userId == null) {
            _uiState.update { it.copy(favourites = emptyList()) }
            return
        }
        viewModelScope.launch {
            val favourites = repository.refreshFavourites(userId)
            _uiState.update { it.copy(favourites = favourites) }
        }
    }

    fun toggleFavourite(card: AccCard) {
        val userId = _uiState.value.userId ?: run {
            viewModelScope.launch {
                _events.emit(AccEvent.Error("User not logged in"))
            }
            return
        }
        viewModelScope.launch {
            val added = repository.toggleFavourite(userId, card)
            val favourites = repository.refreshFavourites(userId)
            _uiState.update { it.copy(favourites = favourites) }
            _events.emit(AccEvent.FavouriteToggled(added))
        }
    }

    fun onSentenceSpoken(sentence: String) {
        val aiSupport = _uiState.value.settings.aiSupport
        viewModelScope.launch {
            repository.recordSentence(sentence)
        }
        if (aiSupport) {
            val message = TextMessage.createForLocalUser(sentence, System.currentTimeMillis())
            _uiState.update { state ->
                state.copy(baseConversation = state.baseConversation + message)
            }
        }
    }

    fun updateSettings(settings: AccUserSettings) {
        _uiState.update { it.copy(settings = settings) }
    }
}
