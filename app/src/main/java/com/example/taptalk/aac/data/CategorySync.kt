package com.example.taptalk.aac.data

import android.content.Context
import android.net.Uri
import androidx.room.Room
import com.example.taptalk.data.AppDatabase
import com.example.taptalk.data.UserCategoryEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

/**
 * Saves a user-created category to both the local Room database and Firebase.
 *
 * This function follows an "offline-first" approach. It first saves the category data,
 * including its icon (if provided), to the local Room database. This ensures the category
 * is immediately available in the app, even without an internet connection.
 *
 * After the local save, it attempts to sync the data to the cloud:
 * 1.  If an image URI is provided, it uploads the category icon to Firebase Storage.
 * 2.  It then saves the category's metadata (name, color, associated card filenames, and the
 *     icon's download URL from Storage) to Firebase Firestore.
 *
 * The function provides feedback on its progress through the `onResult` callback, indicating
 * local save success and the final cloud sync status. If the cloud sync fails, the local
 * data remains, marked as "not synced," allowing for a future sync attempt.
 *
 * @param context The application context, used for file operations and database access.
 * @param imageUri The URI of the category's icon image. Can be null if no image is selected.
 * @param name The name of the category. This is used as the primary key in the database and
 *             the document ID in Firestore.
 * @param colorHex The background color for the category, represented as a hex string (e.g., "#FFFFFF").
 * @param selectedCardFileNames A list of strings, where each string is the filename of a card
 *                              associated with this category.
 * @param onResult A callback function that receives status messages (e.g., success, failure,
 *                 progress) as a String.
 */
fun saveCategoryLocallyAndToFirebase(
    context: Context,
    imageUri: Uri?,
    name: String,
    colorHex: String,
    selectedCardFileNames: List<String>,
    onResult: (String) -> Unit
) {
    val userId = FirebaseAuth.getInstance().currentUser?.uid
    if (userId == null) {
        onResult("User not logged in.")
        return
    }

    // Save category icon locally
    var localImagePath: String? = null
    if (imageUri != null) {
        try {
            val localDir = File(context.filesDir, "Custom_Categories")
            if (!localDir.exists()) localDir.mkdirs()

            val safeName = name.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
            val localFile = File(localDir, "${safeName}.jpg")
            context.contentResolver.openInputStream(imageUri)?.use { input ->
                FileOutputStream(localFile).use { output ->
                    input.copyTo(output)
                }
            }
            localImagePath = localFile.absolutePath
        } catch (e: Exception) {
            onResult("Failed to save category icon locally: ${e.message}")
        }
    }

    // Save to Room
    CoroutineScope(Dispatchers.IO).launch {

        val db = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "tap_talk_db"
        ).build()

        val catDao = db.userCategoryDao()

        val entity = UserCategoryEntity(
            name = name,
            colorHex = colorHex,
            imagePath = localImagePath,
            cardFileNames = selectedCardFileNames,
            synced = false,
            userId = userId
        )

        // Save locally
        catDao.insertOrUpdate(entity)

        CoroutineScope(Dispatchers.Main).launch {
            onResult("Category saved locally (syncing with cloud...)")
        }

        // Firebase Storage + Firestore
        try {
            var downloadUrl: String? = null

            if (imageUri != null) {
                val storageRef = FirebaseStorage.getInstance()
                    .reference.child("users/$userId/Custom_Categories/${name}.jpg")

                storageRef.putFile(imageUri).addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { uri ->
                        downloadUrl = uri.toString()

                        uploadCategoryMetadata(
                            name, colorHex, selectedCardFileNames, downloadUrl, userId, catDao, entity, onResult
                        )
                    }.addOnFailureListener { e ->
                        onResult("Category icon upload URL failed: ${e.message}")
                    }
                }.addOnFailureListener { e ->
                    onResult("Category icon upload failed: ${e.message}")
                }

            } else {
                uploadCategoryMetadata(
                    name, colorHex, selectedCardFileNames, null, userId, catDao, entity, onResult
                )
            }

        } catch (e: Exception) {
            e.printStackTrace()
            CoroutineScope(Dispatchers.Main).launch {
                onResult("Category cloud sync failed: ${e.message}")
            }
        }
    }
}


/**
 * Uploads the metadata for a custom category to Firestore.
 *
 * This function is called after the category has been saved locally and, if applicable,
 * its icon has been uploaded to Firebase Storage. It creates a document in the user's
 * `Custom_Categories` collection in Firestore. Upon successful upload, it updates the
 * local Room database to mark the category as synced.
 *
 * @param name The name of the category, which also serves as the document ID in Firestore.
 * @param colorHex The hex color code string for the category's background.
 * @param cardFileNames A list of filenames for the cards associated with this category.
 * @param imageUrl The optional public download URL for the category's icon from Firebase Storage.
 * @param userId The UID of the currently authenticated Firebase user.
 * @param catDao The DAO for accessing the local user category database.
 * @param entity The Room entity object representing the category that was saved locally.
 * @param onResult A callback function to report the final status of the cloud sync operation.
 */
private fun uploadCategoryMetadata(
    name: String,
    colorHex: String,
    cardFileNames: List<String>,
    imageUrl: String?,
    userId: String,
    catDao: com.example.taptalk.data.UserCategoryDao,
    entity: UserCategoryEntity,
    onResult: (String) -> Unit
) {
    val firestore = FirebaseFirestore.getInstance()
    val data = mutableMapOf<String, Any>(
        "name" to name,
        "colorHex" to colorHex,
        "cardFileNames" to cardFileNames,
        "timestamp" to com.google.firebase.Timestamp.now()
    )
    if (imageUrl != null) data["imageUrl"] = imageUrl

    firestore.collection("USERS")
        .document(userId)
        .collection("Custom_Categories")
        .document(name)
        .set(data)
        .addOnSuccessListener {
            CoroutineScope(Dispatchers.IO).launch {

                catDao.insertOrUpdate(
                    entity.copy(
                        synced = true,
                        userId = userId
                    )
                )
            }
            onResult("Category synced with cloud")
        }
        .addOnFailureListener { e ->
            onResult("Category metadata upload failed: ${e.message}")
        }
}
