package com.example.taptalk.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taptalk.data.LoginRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Represents the state of the login screen UI.
 *
 * This data class holds all the information necessary to render the login view at any given time.
 *
 * @property isLoading Indicates whether a login operation is currently in progress.
 *                     If true, the UI should show a loading indicator.
 * @property error A string containing an error message if the login attempt failed.
 *                 It is null if there is no error.
 * @property success Indicates whether the login was successful. If true, the UI should navigate
 *                   to the next screen.
 */
data class LoginUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)

/**
 * ViewModel for the Login screen.
 *
 * This class manages the UI state for the login screen and handles the business logic
 * for user authentication. It communicates with the [LoginRepository] to perform
 * the login operation and updates the UI state accordingly.
 *
 * @param repo The repository responsible for handling login data operations.
 *             Defaults to a new instance of [LoginRepository].
 */
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
