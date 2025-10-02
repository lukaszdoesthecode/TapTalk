package com.example.taptalk.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taptalk.data.LoginRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)

class LoginViewModel(
    private val repo: LoginRepository = LoginRepository()
) : ViewModel() {
    private val _state = MutableStateFlow(LoginUiState())
    val state = _state.asStateFlow()

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _state.value = LoginUiState(error = "E-mail and password required")
            return
        }
        _state.value = LoginUiState(isLoading = true)
        viewModelScope.launch {
            val res = repo.loginWithEmailPassword(email, password)
            _state.value = res.fold(
                onSuccess = { LoginUiState(success = true) },
                onFailure = { LoginUiState(error = it.localizedMessage) }
            )
        }
    }
}
