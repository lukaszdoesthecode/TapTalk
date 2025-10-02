package com.example.taptalk.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class RegisterState(
    val isLoading: Boolean = false,
    val success: Boolean = false,
    val error: String? = null
)

class RegisterViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val _state = MutableStateFlow(RegisterState())
    val state: StateFlow<RegisterState> = _state

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
