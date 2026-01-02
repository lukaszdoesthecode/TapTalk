package com.example.taptalk.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FastSettingsSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val db = AppDatabase.getDatabase(applicationContext)
        val fastDao = db.fastSettingsDao()

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.success()

        val settings = fastDao.getSettings(userId) ?: return Result.success()

        return try {
            FirebaseFirestore.getInstance()
                .collection("USERS")
                .document(userId)
                .collection("Fast_Settings")
                .document("current")
                .set(settings)
                .await()

            fastDao.updateSyncStatus(userId, true)
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
