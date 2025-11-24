package com.example.taptalk.aac.data

import android.content.Context
import android.util.Log
import com.example.taptalk.data.AppDatabase
import com.example.taptalk.data.UserCategoryEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class CategoryRepository(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val dao = db.userCategoryDao()

    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()

    /**
     * Restores categories + icons from Firebase into local Room + files
     */
    suspend fun restoreFromFirebase() {
        val userId = auth.currentUser?.uid ?: return
        val categoriesRef = firestore.collection("USERS")
            .document(userId)
            .collection("Custom_Categories")

        try {
            val snapshot = categoriesRef.get().await()

            for (doc in snapshot.documents) {
                val name = doc.getString("name") ?: continue
                val colorHex = doc.getString("colorHex") ?: "#FFFFFF"
                val cardFileNames = doc.get("cardFileNames") as? List<String> ?: emptyList()
                val imageUrl = doc.getString("imageUrl")

                var localImagePath: String? = null

                if (!imageUrl.isNullOrEmpty()) {
                    localImagePath = downloadCategoryImage(name, imageUrl)
                }

                val entity = UserCategoryEntity(
                    name = name,
                    colorHex = colorHex,
                    imagePath = localImagePath,
                    cardFileNames = cardFileNames,
                    synced = true
                )

                dao.insertOrUpdate(entity)
            }

        } catch (e: Exception) {
            Log.e("CATEGORY_RESTORE", "Error: ${e.message}")
        }
    }

    /**
     * Download category icon into local storage
     */
    private suspend fun downloadCategoryImage(name: String, url: String): String? =
        withContext(Dispatchers.IO) {
            try {
                val localDir = File(context.filesDir, "Custom_Categories")
                if (!localDir.exists()) localDir.mkdirs()

                val file = File(localDir, "${name}.jpg")
                val ref = storage.getReferenceFromUrl(url)
                val bytes = ref.getBytes(5L * 1024 * 1024).await()

                FileOutputStream(file).use { it.write(bytes) }
                return@withContext file.absolutePath
            } catch (e: Exception) {
                Log.e("IMG_DOWNLOAD", "Failed: ${e.message}")
                return@withContext null
            }
        }
}
