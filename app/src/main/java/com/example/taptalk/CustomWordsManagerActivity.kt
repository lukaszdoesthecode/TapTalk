package com.example.taptalk

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File

/**
 * An activity that allows users to manage their custom-added words.
 *
 * This activity provides a user interface for viewing, deleting, and re-categorizing
 * custom words that the user has previously created. It utilizes Jetpack Compose
 * for its UI, displaying a list of custom words fetched from local storage and
 * handling synchronization with Firebase for authenticated users.
 *
 * The main UI is encapsulated within the [CustomWordsManagerScreen] composable.
 */
class CustomWordsManagerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                CustomWordsManagerScreen()
            }
        }
    }
}

/**
 * Deletes a custom word from local storage and, if a user is logged in, from Firebase.
 *
 * This function performs the following actions:
 * 1. Deletes the associated image file from the device's local storage.
 * 2. If a `userId` is provided, it proceeds to delete the word from the user's Firebase account.
 * 3. Deletes the word's image from Firebase Storage.
 * 4. Deletes the word's metadata document from Cloud Firestore.
 *
 * Any exceptions during the process are caught and printed to the stack trace.
 *
 * @param context The application context, used for file system access.
 * @param card The [AccCard] object representing the custom word to be deleted.
 * @param userId The unique ID of the currently logged-in user. If null, only local deletion is performed.
 */
fun deleteCustomWord(context: Context, card: AccCard, userId: String?) {
    try {
        val file = File(card.path)
        if (file.exists()) file.delete()

        if (userId != null) {
            val storageRef = FirebaseStorage.getInstance()
                .reference.child("users/$userId/Custom_Words/${card.folder}/${card.fileName}")
            storageRef.delete()

            val firestore = FirebaseFirestore.getInstance()
            firestore.collection("USERS")
                .document(userId)
                .collection("Custom_Words")
                .document("${card.folder}")
                .collection("words")
                .document(card.label)
                .delete()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * Moves a custom word to a new category.
 *
 * This function handles both local file system changes and synchronization with Firebase.
 *
 * 1.  **Local File System:** It moves the word's image file from its old category folder to the new one
 *     within the app's internal storage. It creates the new category folder if it doesn't exist.
 *     The old file is deleted after being successfully copied.
 *
 * 2.  **Firebase (if user is logged in):**
 *     - It uploads the image file to the corresponding new category path in Firebase Storage.
 *     - It creates a new document in Firestore under the new category with updated metadata (category, level, imageUrl, timestamp).
 *     - After successful creation of the new entries, it deletes the old image file from Firebase Storage
 *       and the old document from Firestore.
 *
 * All Firebase operations are chained using success listeners to ensure data consistency.
 *
 * @param context The application context, used to access the file system.
 * @param card The [AccCard] object representing the custom word to be moved. It contains information
 *             like the current path, label, and folder.
 * @param newCategory The name of the new category to move the word into (e.g., "noun", "verbs").
 * @param userId The unique ID of the currently logged-in user. If null, only local file
 *               operations are performed.
 */
fun moveCustomWord(context: Context, card: AccCard, newCategory: String, userId: String?) {
    try {
        val oldFile = File(card.path)
        val newFolder = File(context.filesDir, "Custom_Words/${newCategory}_A1")
        if (!newFolder.exists()) newFolder.mkdirs()

        val newFile = File(newFolder, card.fileName)
        oldFile.copyTo(newFile, overwrite = true)
        oldFile.delete()

        if (userId != null) {
            val storage = FirebaseStorage.getInstance()
            val firestore = FirebaseFirestore.getInstance()

            val oldRef = storage.reference.child("users/$userId/Custom_Words/${card.folder}/${card.fileName}")
            val newRef = storage.reference.child("users/$userId/Custom_Words/${newCategory}_A1/${card.fileName}")

            oldRef.downloadUrl.addOnSuccessListener { uri ->
                newRef.putFile(Uri.fromFile(newFile)).addOnSuccessListener {
                    newRef.downloadUrl.addOnSuccessListener { newUri ->
                        val data = mapOf(
                            "label" to card.label,
                            "category" to newCategory,
                            "level" to "A1",
                            "imageUrl" to newUri.toString(),
                            "timestamp" to com.google.firebase.Timestamp.now()
                        )

                        firestore.collection("USERS")
                            .document(userId)
                            .collection("Custom_Words")
                            .document("${newCategory}_A1")
                            .collection("words")
                            .document(card.label)
                            .set(data)

                        oldRef.delete()
                        firestore.collection("USERS")
                            .document(userId)
                            .collection("Custom_Words")
                            .document(card.folder)
                            .collection("words")
                            .document(card.label)
                            .delete()
                    }
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * A Composable screen that allows users to manage their custom-added words.
 *
 * This screen displays a list of all custom words created by the user. It fetches these words
 * from local storage upon composition. For each word, it presents an item view
 * (`CustomWordItem`) which shows the word's image, label, and current category.
 *
 * Users can perform two main actions on each custom word:
 * 1.  **Delete:** Permanently remove the word from both local storage and Firebase.
 * 2.  **Change Category:** Move the word to a different category, which updates its location
 *     in both local storage and Firebase.
 *
 * If no custom words are found, a placeholder message is displayed. The screen includes a
 * persistent bottom navigation bar (`BottomNavBar`) for navigating to other parts of the application.
 *
 * The layout is a vertically scrollable column, ensuring all custom words can be viewed even if the
 * list exceeds the screen height.
 */
@Composable
fun CustomWordsManagerScreen() {
    val context = LocalContext.current
    var customCards by remember { mutableStateOf(loadCustomCards(context)) }
    val userId = FirebaseAuth.getInstance().currentUser?.uid

    Scaffold(
        bottomBar = { BottomNavBar() }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .background(Color(0xFFF8F8F8))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (customCards.isEmpty()) {
                Text(
                    "No custom words found.",
                    fontSize = 18.sp,
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else {
                customCards.forEach { card ->
                    CustomWordItem(
                        card = card,
                        onDelete = {
                            deleteCustomWord(context, card, userId)
                            customCards = loadCustomCards(context)
                        },
                        onCategoryChange = { newCategory ->
                            moveCustomWord(context, card, newCategory, userId)
                            customCards = loadCustomCards(context)
                        }
                    )
                }
            }
        }
    }
}

/**
 * A composable that displays a single custom word item in a card layout.
 *
 * This item shows the word's image, label, and current category. It provides
 * controls for the user to delete the word or change its category via a dropdown menu.
 *
 * @param card The [AccCard] data object containing information about the custom word,
 *             such as its label, image path, and current folder (category).
 * @param onDelete A lambda function that is invoked when the user clicks the delete icon.
 *                 This should handle the logic for removing the word.
 * @param onCategoryChange A lambda function that is invoked when the user selects a new
 *                         category from the dropdown menu. It passes the new category
 *                         name as a [String].
 */
@Composable
fun CustomWordItem(
    card: AccCard,
    onDelete: () -> Unit,
    onCategoryChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val categories = listOf(
        "noun", "verbs", "adjective", "determiner", "pronoun",
        "preposition", "conjunction", "emergency", "question", "social"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, borderColorFor(card.folder), RoundedCornerShape(10.dp))
            .background(Color.White)
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Image(
                painter = rememberAsyncImagePainter(Uri.parse(card.path)),
                contentDescription = card.label,
                modifier = Modifier.size(80.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(card.label, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Category: ${card.folder}", color = Color.Gray, fontSize = 14.sp)

                Box {
                    Button(
                        onClick = { expanded = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDFF0D8)),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text("Change Category", color = Color.Black)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.replaceFirstChar { it.uppercase() }) },
                                onClick = {
                                    expanded = false
                                    onCategoryChange(cat)
                                }
                            )
                        }
                    }
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.Red
                )
            }
        }
    }
}