package com.example.taptalk.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

/**
 * Repository class for handling user authentication-related data operations.
 * This class abstracts the data source (Firebase Authentication) from the rest of the application.
 *
 * @property auth An instance of [FirebaseAuth] used to communicate with Firebase Authentication services.
 */
class LoginRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) {
    /**
     * Attempts to sign in a user with their email and password using Firebase Authentication.
     *
     * This is a suspending function that should be called from a coroutine scope. It encapsulates
     * the Firebase `signInWithEmailAndPassword` call in a try-catch block to handle potential
     * exceptions, such as invalid credentials or network errors.
     *
     * @param email The user's email address.
     * @param password The user's password.
     * @return A [Result] object. On success, it returns `Result.success(Unit)`.
     * On failure, it returns `Result.failure(Exception)` containing the exception that occurred.
     */
    suspend fun loginWithEmailPassword(email: String, password: String): Result<Unit> {
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("LoginRepository", "Login failed", e)
            Result.failure(e)
        }
    }
}
