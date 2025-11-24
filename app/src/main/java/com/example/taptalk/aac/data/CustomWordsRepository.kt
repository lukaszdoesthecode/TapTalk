package com.example.taptalk.aac.data

import android.content.Context
import android.util.Log
import com.example.taptalk.data.AppDatabase
import com.example.taptalk.data.CustomWordEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class CustomWordsRepository(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val dao = db.customWordDao()

    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun restoreFromFirebase() {
        val userId = auth.currentUser?.uid ?: return
        val wordsRef = firestore.collection("USERS")
            .document(userId)
            .collection("Custom_Words")

        try {
            val snapshot = wordsRef.get().await()

            for (doc in snapshot.documents) {
                val label = doc.getString("label") ?: continue
                val folder = doc.getString("folder") ?: ""
                val audioUrl = doc.getString("audioUrl")
                val imageUrl = doc.getString("imageUrl")

                var localImagePath: String? = null

                if (!imageUrl.isNullOrEmpty()) {
                    localImagePath = downloadWordImage(label, imageUrl)
                }

                val entity = CustomWordEntity(
                    label = label,
                    folder = folder,
                    imagePath = localImagePath,
                    synced = true
                )

                dao.insertOrUpdate(entity)
            }

        } catch (e: Exception) {
            Log.e("WORDS_RESTORE", "Error: ${e.message}")
        }
    }

    private suspend fun downloadWordImage(label: String, url: String): String? =
        withContext(Dispatchers.IO) {
            try {
                val dir = context.getDir("custom_cards", Context.MODE_PRIVATE)
                if (!dir.exists()) dir.mkdirs()

                val file = File(dir, "$label.jpg")
                val ref = storage.getReferenceFromUrl(url)
                val bytes = ref.getBytes(3L * 1024 * 1024).await()

                FileOutputStream(file).use { it.write(bytes) }
                return@withContext file.absolutePath
            } catch (e: Exception) {
                Log.e("IMG_DOWNLOAD", "Failed: ${e.message}")
                return@withContext null
            }
        }
}
