package com.example.taptalk.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * A [CoroutineWorker] responsible for synchronizing fast settings from the local Room database
 * to Firebase Firestore.
 *
 * This worker is typically scheduled when local settings are updated and need to be persisted
 * to the cloud. It retrieves the current fast settings from the local `AppDatabase`,
 * gets the current user's ID from Firebase Authentication, and then uploads the settings
 * to the user's specific document in the "Fast_Settings" collection on Firestore.
 *
 * If the upload is successful, it updates the local setting's sync status to `true`.
 * If the upload fails (e.g., due to a network issue), it returns [Result.retry] to
 * allow WorkManager to reschedule the task.
 *
 * @param context The application context.
 * @param params Parameters to setup the worker, provided by WorkManager.
 */
class FastSettingsSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    /**
     * Executes the background work to synchronize local fast settings with Firebase Firestore.
     *
     * This function retrieves the current user's fast settings from the local Room database.
     * If settings and a logged-in user exist, it attempts to upload these settings to the
     * user's "Fast_Settings" collection in Firestore.
     *
     * On a successful upload, it updates the local sync status of the settings to `true`.
     *
     * @return [Result.success] if the synchronization is successful, not needed (no settings or user),
     * or if it completes without errors. Returns [Result.retry] if an exception occurs during the
     * Firestore operation, indicating that the work should be attempted again later.
     */
    override suspend fun doWork(): Result {
        val db = AppDatabase.getInstance(applicationContext)
        val fastDao = db.fastSettingsDao()
        val settings = fastDao.getSettings()
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (settings == null || userId == null) return Result.success()

        return try {
            FirebaseFirestore.getInstance()
                .collection("USERS")
                .document(userId)
                .collection("Fast_Settings")
                .document("current")
                .set(settings)
                .await()

            fastDao.updateSyncStatus(true)
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
