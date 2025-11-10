package com.example.taptalk.data

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for managing sentence history.
 *
 * Same as before — but now FirebaseAuth and FirebaseFirestore are injected,
 * so the class can be easily tested without static mocking.
 */
class HistoryRepository(
    private val context: Context,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val db = HistoryDatabase.getDatabase(context)
    private val dao = db.historyDao()

    /**
     * Saves a generated sentence to the local Room database for offline access.
     */
    suspend fun saveSentenceOffline(sentence: String) {
        withContext(Dispatchers.IO) {
            val entity = HistoryEntity(sentence = sentence, timestamp = System.currentTimeMillis())
            dao.insert(entity)
            Log.d("HISTORY_DEBUG", "Saved offline: $sentence")
        }
    }

    /**
     * Retrieves the most recent history entries from the local database.
     */
    suspend fun getRecentSentences() = withContext(Dispatchers.IO) {
        dao.getRecent()
    }

    /**
     * Synchronizes the entire local history from the Room database to Firebase Firestore.
     * All logic identical to your original version.
     */
    suspend fun syncToFirebase() {
        withContext(Dispatchers.IO) {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                Log.w("HISTORY_DEBUG", "User not logged in, skipping sync")
                return@withContext
            }

            val allHistory = dao.getAll()
            if (allHistory.isEmpty()) {
                Log.d("HISTORY_DEBUG", "No local history to sync")
                return@withContext
            }

            val batch = firestore.batch()
            val userRef = firestore.collection("USERS")
                .document(userId)
                .collection("History")

            allHistory.forEach { entity ->
                val docRef = userRef.document(entity.timestamp.toString())
                val data = mapOf(
                    "sentence" to entity.sentence,
                    "timestamp" to entity.timestamp
                )
                batch.set(docRef, data)
            }

            try {
                batch.commit()
                    .addOnSuccessListener {
                        Log.d("HISTORY_DEBUG", "Synced ${allHistory.size} history entries to Firebase!")
                    }
                    .addOnFailureListener { e ->
                        Log.e("HISTORY_DEBUG", "Sync failed: ${e.message}")
                    }
            } catch (e: Exception) {
                Log.e("HISTORY_DEBUG", "Exception during sync: ${e.message}")
            }
        }
    }
}
