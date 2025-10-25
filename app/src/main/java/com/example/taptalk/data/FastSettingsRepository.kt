package com.example.taptalk.data

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class FastSettingsRepository(
    private val dao: FastSettingsDao,
    private val historyDao: HistoryDao
) {
    suspend fun saveSettingsLocally(settings: FastSettingsEntity) {
        dao.insertOrUpdate(settings)
    }

    suspend fun addSentenceLocally(sentence: String) {
        historyDao.insert(HistoryEntity(sentence = sentence))
    }

    suspend fun syncIfOnline(userId: String) {
        if (isNetworkAvailable()) {
            dao.getSettings()?.let {
                syncSettingsToFirestore(userId, it)
                dao.updateSyncStatus(true)
            }

            val unsyncedHistory = historyDao.getRecent().filter { !it.isSynced }
            if (unsyncedHistory.isNotEmpty()) {
                syncHistoryToFirestore(userId, unsyncedHistory)
                unsyncedHistory.forEach { historyDao.updateSyncStatus(it.id, true) }
            }
        }
    }

    private suspend fun syncSettingsToFirestore(userId: String, settings: FastSettingsEntity) {
        val firestore = Firebase.firestore
        firestore.collection("USERS")
            .document(userId)
            .collection("Fast_Settings")
            .document("current")
            .set(
                mapOf(
                    "volume" to settings.volume,
                    "selectedVoice" to settings.selectedVoice,
                    "aiSupport" to settings.aiSupport
                )
            )
    }

    private suspend fun syncHistoryToFirestore(userId: String, history: List<HistoryEntity>) {
        val firestore = Firebase.firestore
        val historyCollection = firestore.collection("USERS")
            .document(userId)
            .collection("History")

        history.forEach { entry ->
            val doc = historyCollection.document()
            doc.set(
                mapOf(
                    "sentence" to entry.sentence,
                    "timestamp" to entry.timestamp
                )
            )
        }
    }

    private fun isNetworkAvailable(): Boolean {
        // TODO: implement ConnectivityManager for real
        return true
    }
}
