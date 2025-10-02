package com.example.taptalk.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class LoginRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
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

   /* suspend fun loginWithPin(email: String, pin: String): Result<Unit> {
        return try {
            val query = db.collection("USERS")
                .whereEqualTo("User Data.Profile.E-mail", email)
                .get().await()

            if (query.isEmpty) return Result.failure(Exception("No user found"))

            val userDoc = query.documents.first()
            val profile = db.collection("USERS")
                .document(userDoc.id)
                .collection("User Data")
                .document("Profile")
                .get().await()

            val storedPin = profile.getString("PIN")
            val logByPin = profile.getBoolean("Log_by_PIN") ?: false

            if (logByPin && !storedPin.isNullOrBlank() && storedPin == pin) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Invalid PIN login"))
            }
        } catch (e: Exception) {
            Log.e("LoginRepository", "PIN login failed", e)
            Result.failure(e)
        }
    } */
}
