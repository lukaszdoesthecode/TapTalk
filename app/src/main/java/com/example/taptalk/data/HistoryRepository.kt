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
 * This class handles all data operations related to the user's sentence history. It acts as a
 * mediator between the UI/ViewModel and the data sources, which include a local Room database
 * (for offline storage) and Firebase Firestore (for cloud synchronization).
 *
 * The repository provides methods to:
 * - Save new sentences to the local database.
 * - Retrieve recent sentences from the local database.
 * - Synchronize the entire local history with the user's Firestore collection.
 *
 * All database and network operations are performed on a background thread using coroutines.
 *
 * @param context The application context, used to initialize the Room database.
 */
class HistoryRepository(private var context: Context) {

    private val db = HistoryDatabase.getDatabase(context)
    private val dao = db.historyDao()

    /**
     * Saves a generated sentence to the local Room database for offline access.
     * This function operates on the IO dispatcher to avoid blocking the main thread.
     * It creates a [HistoryEntity] with the given sentence and the current timestamp,
     * then inserts it into the database.
     *
     * @param sentence The sentence string to be saved.
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
     * This operation is performed on an IO thread.
     *
     * @return A list of [HistoryEntity] objects, ordered by timestamp in descending order.
     */
    suspend fun getRecentSentences() = withContext(Dispatchers.IO) {
        dao.getRecent()
    }

    /**
     * Synchronizes the entire local history from the Room database to Firebase Firestore.
     *
     * This function performs a one-way sync, uploading all local history entries to the cloud.
     * It first checks if a user is currently logged in. If not, the sync is aborted.
     * It then retrieves all history entries from the local database. If there's nothing
     * to sync, it also aborts.
     *
     * For each local history entry, it creates a corresponding document in the user's
     * `History` subcollection in Firestore. The document ID is the timestamp of the entry,
     * ensuring uniqueness and chronological order.
     *
     * The entire operation is performed as a single Firestore batch write to ensure atomicity.
     * If the batch commit fails, an error is logged. All operations are executed on the
     * IO dispatcher.
     */
    suspend fun syncToFirebase() {
        withContext(Dispatchers.IO) {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId == null) {
                Log.w("HISTORY_DEBUG", "User not logged in, skipping sync")
                return@withContext
            }

            val firestore = FirebaseFirestore.getInstance()
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
                        Log.d("HISTORY_DEBUG", "✅ Synced ${allHistory.size} history entries to Firebase!")
                    }
                    .addOnFailureListener { e ->
                        Log.e("HISTORY_DEBUG", "❌ Sync failed: ${e.message}")
                    }
            } catch (e: Exception) {
                Log.e("HISTORY_DEBUG", "Exception during sync: ${e.message}")
            }
        }
    }
}
