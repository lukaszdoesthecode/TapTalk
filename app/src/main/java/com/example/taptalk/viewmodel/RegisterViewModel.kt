package com.example.taptalk.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Represents the state of the user registration process.
 * This data class is used to communicate the current status of the registration flow
 * from the [RegisterViewModel] to the UI.
 *
 * @property isLoading Indicates whether a registration operation is currently in progress.
 *                     The UI can use this to show a loading indicator. Defaults to `false`.
 * @property success Indicates whether the registration was completed successfully.
 *                   Defaults to `false`.
 * @property error Contains an error message if the registration failed.
 *                 This will be `null` if there is no error or if the operation is in progress or successful.
 *                 The UI can display this message to the user. Defaults to `null`.
 */
data class RegisterState(
    val isLoading: Boolean = false,
    val success: Boolean = false,
    val error: String? = null
)

/**
 * ViewModel responsible for handling user registration logic and managing the state of the registration screen.
 *
 * This ViewModel interacts with Firebase Authentication to create a new user and with
 * Firebase Firestore to store the user's profile information. It exposes the registration
 * state (loading, success, or error) via a [StateFlow] so that the UI can react accordingly.
 *
 * @property state A [StateFlow] that emits the current [RegisterState], representing the UI state.
 */
class RegisterViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val _state = MutableStateFlow(RegisterState())
    val state: StateFlow<RegisterState> = _state

    /**
     * Attempts to register a new user with the provided credentials and personal information.
     *
     * This function performs validation on the input data, such as checking if the passwords
     * match and if the email format is valid. If validation passes, it proceeds to create a
     * new user account using Firebase Authentication. Upon successful creation, it then saves
     * the user's profile information, including name, email, date of birth, and login preferences,
     * to a Firestore database.
     *
     * The state of the registration process (loading, success, or error) is updated in the
     * `_state` `MutableStateFlow`, which can be observed by the UI.
     *
     * @param name The user's full name.
     * @param email The user's email address. Must be a valid format.
     * @param password The desired password for the account.
     * @param repeatPassword The password confirmation. Must match `password`.
     * @param dob The user's date of birth.
     * @param pin An optional 4-digit PIN for an alternative login method. Null if not provided.
     * @param optPassword A boolean flag indicating if password login is enabled (currently unused in the function body).
     * @param optPin A boolean flag indicating if PIN login is enabled.
     */
    fun register(
        name: String,
        email: String,
        password: String,
        repeatPassword: String,
        dob: String,
        pin: String?,
        optPassword: Boolean,
        optPin: Boolean
    ) {
        if (password != repeatPassword) {
            _state.value = RegisterState(error = "Passwords do not match")
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _state.value = RegisterState(error = "Please enter a valid email")
            return
        }

        viewModelScope.launch {
            _state.value = RegisterState(isLoading = true)

            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { authResult ->
                    val uid = authResult.user?.uid
                    if (uid == null) {
                        _state.value = RegisterState(error = "Registration failed: UID is null")
                        return@addOnSuccessListener
                    }

                    val userMap = mapOf(
                        "name" to name,
                        "email" to email,
                        "dob" to dob,
                        "pin" to pin,
                        "log_by_pin" to optPin
                    )

                    db.collection("USERS")
                        .document(uid)
                        .collection("User_Data")
                        .document("Profile")
                        .set(userMap)
                        .addOnSuccessListener {
                            Log.d("Firestore", "User saved to Firestore")
                            _state.value = RegisterState(success = true)
                        }
                        .addOnFailureListener { e ->
                            Log.e("Firestore", "Error saving user", e)
                            _state.value = RegisterState(error = e.localizedMessage)
                        }

                }
                .addOnFailureListener { e ->
                    _state.value = RegisterState(error = e.localizedMessage)
                }
        }
    }

}
