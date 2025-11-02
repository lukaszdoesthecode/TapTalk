package com.example.taptalk.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Repository for managing "Fast Settings".
 *
 * This class handles the data operations for fast settings, which are likely user-configurable
 * quick access settings within the application. It acts as a single source of truth by
 * coordinating between a local Room database (via [FastSettingsDao]) for offline access and
 * a remote Firebase Firestore database for cloud backup and synchronization across devices.
 *
 * @param fastSettingsDao Data Access Object for the local fast settings database table.
 * @param historyDao Data Access Object for the history table (injected but not used directly in this class).
 */
class FastSettingsRepository(
    private val fastSettingsDao: FastSettingsDao,
    historyDao: HistoryDao,
) {
    private val firestore = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    /**
     * Retrieves the fast settings from the local database.
     *
     * This function accesses the local data source via the DAO to get the current
     * user's fast settings. It's a suspending function, so it should be called
     * from a coroutine scope.
     *
     * @return A [FastSettingsEntity] object if settings are found in the local database,
     *         otherwise null.
     */
    suspend fun getLocalSettings(): FastSettingsEntity? =
        fastSettingsDao.getSettings()

    /**
     * Saves the provided settings to the local database.
     * Before saving, it creates a copy of the settings object and sets its `isSynced` flag to `false`.
     * This indicates that the local settings have been modified and are not yet synchronized with Firebase.
     *
     * @param settings The [FastSettingsEntity] object to be saved locally.
     */
    suspend fun saveLocalSettings(settings: FastSettingsEntity) {
        fastSettingsDao.insertOrUpdate(settings.copy(isSynced = false))
    }

    /**
     * Fetches the user's fast settings from Firebase Firestore and updates the local database.
     *
     * This function retrieves the 'current' settings document from the 'Fast_Settings' collection
     * for the currently logged-in user. If the document exists, it's converted to a
     * [FastSettingsEntity] object, marked as synced, and then inserted or updated in the
     * local Room database via the [FastSettingsDao].
     *
     * The operation is skipped if there is no logged-in user. Any exceptions during the
     * Firestore operation are caught and logged, preventing the app from crashing.
     *
     * This is a suspend function and should be called from a coroutine scope.
     */
    suspend fun fetchFromFirebase() {
        if (userId == null) return
        try {
            val snapshot = firestore.collection("USERS")
                .document(userId)
                .collection("Fast_Settings")
                .document("current")
                .get()
                .await()

            if (snapshot.exists()) {
                val data = snapshot.toObject(FastSettingsEntity::class.java)
                if (data != null) {
                    fastSettingsDao.insertOrUpdate(data.copy(isSynced = true))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
