package com.example.taptalk.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FastSettingsRepository(
    private val fastSettingsDao: FastSettingsDao,
    historyDao: HistoryDao,
) {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val userId: String?
        get() = auth.currentUser?.uid

    suspend fun getLocalSettings(): FastSettingsEntity? {
        val uid = userId ?: ""
        return fastSettingsDao.getSettings(uid)
    }

    suspend fun saveLocalSettings(settings: FastSettingsEntity) {
        val uid = userId ?: ""
        fastSettingsDao.insertOrUpdate(
            settings.copy(
                userId = uid,
                isSynced = false
            )
        )
    }

    suspend fun fetchFromFirebase() {
        val uid = userId ?: return
        try {
            val snapshot = firestore.collection("USERS")
                .document(uid)
                .collection("Fast_Settings")
                .document("current")
                .get()
                .await()

            if (snapshot.exists()) {
                val data = snapshot.toObject(FastSettingsEntity::class.java)
                if (data != null) {
                    fastSettingsDao.insertOrUpdate(
                        data.copy(
                            userId = uid,
                            isSynced = true
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
