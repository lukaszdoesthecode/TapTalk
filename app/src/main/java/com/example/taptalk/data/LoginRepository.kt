package com.example.taptalk.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

class LoginRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) {
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
