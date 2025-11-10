package com.example.taptalk.aac.data

import android.content.Context
import androidx.work.*
import com.example.taptalk.data.AppDatabase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Uploads unsynced categories from local Room DB to Firebase Firestore.
 * Does NOT create duplicates — it only pushes unsynced items.
 */
class CategorySyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.success()
        val db = AppDatabase.getDatabase(applicationContext)
        val dao = db.userCategoryDao()
        val unsynced = dao.getAll().filter { !it.synced }

        if (unsynced.isEmpty()) return Result.success()

        val firestore = FirebaseFirestore.getInstance()
        val userRef = firestore.collection("USERS")
            .document(userId)
            .collection("Custom_Categories")

        try {
            for (cat in unsynced) {
                val data = mapOf(
                    "name" to cat.name,
                    "colorHex" to cat.colorHex,
                    "imagePath" to cat.imagePath,
                    "cardFileNames" to cat.cardFileNames
                )

                userRef.document(cat.name).set(data).await()

                dao.insertOrUpdate(cat.copy(synced = true))
            }
            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.retry()
        }
    }
}

/**
 * Helper that schedules a background sync when network is available.
 */
fun scheduleCategorySync(context: Context) {
    val request = OneTimeWorkRequestBuilder<CategorySyncWorker>()
        .setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        )
        .build()

    WorkManager.getInstance(context).enqueueUniqueWork(
        "CategorySync",
        ExistingWorkPolicy.REPLACE,
        request
    )
}
