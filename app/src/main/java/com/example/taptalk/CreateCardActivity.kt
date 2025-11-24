package com.example.taptalk

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.taptalk.ui.components.BottomNavBar
import com.example.taptalk.ui.theme.Adjective
import com.example.taptalk.ui.theme.Conjunction
import com.example.taptalk.ui.theme.Determiner
import com.example.taptalk.ui.theme.Emergency
import com.example.taptalk.ui.theme.Negation
import com.example.taptalk.ui.theme.Noun
import com.example.taptalk.ui.theme.Pronoun
import com.example.taptalk.ui.theme.Proposition
import com.example.taptalk.ui.theme.Question
import com.example.taptalk.ui.theme.Social
import com.example.taptalk.ui.theme.Verb
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.io.FileOutputStream

/**
 * An activity for creating new communication cards.
 * This activity hosts the [CreateCardScreen] Composable, which provides the UI
 * for users to select an image, enter a label, choose a category, and save
 * the new card. The card data is then saved both locally on the device and
 * to Firebase for synchronization.
 */
class CreateCardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                CreateCardScreen()
            }
        }
    }
}

/**
 * A Composable function that provides the user interface for creating a new communication card.
 *
 * This screen allows users to:
 * 1.  Pick an image from their device's gallery.
 * 2.  Enter a text label for the card.
 * 3.  Select a grammatical category for the card (e.g., noun, verb).
 * 4.  Save the card, which triggers a process to save the card's data locally and to Firebase.
 *
 * The UI consists of an image picker area, a text field for the label, a flow layout of
 * category selection chips, and a save button. It also displays status messages (success or failure)
 * after the save operation.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateCardScreen() {
    val context = LocalContext.current
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var label by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("nouns") }
    var message by remember { mutableStateOf("") }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
            uri -> if (uri != null) selectedImageUri = uri
    }

    Scaffold(bottomBar = { BottomNavBar() }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.White)
                .padding(horizontal = 40.dp, vertical = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            // IMAGE PICKER
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .border(4.dp, Color.Black, RoundedCornerShape(12.dp))
                    .background(Color(0xFFD9D9D9), RoundedCornerShape(12.dp))
                    .clickable { pickImage.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (selectedImageUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(selectedImageUri),
                        contentDescription = "Selected image",
                        modifier = Modifier.fillMaxSize().padding(10.dp)
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.AddPhotoAlternate,
                            contentDescription = "Add image",
                            tint = Color.Gray,
                            modifier = Modifier.size(60.dp)
                        )
                        Text("Choose Image", color = Color.Gray, fontSize = 18.sp)
                    }
                }
            }

            // LABEL FIELD
            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Label (word or phrase)", fontSize = 18.sp) },
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(fontSize = 20.sp),
                modifier = Modifier.fillMaxWidth().height(80.dp)
            )

            // CATEGORY PICKER
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Select Category", fontSize = 20.sp, color = Color.DarkGray)
                val categories = listOf(
                    "nouns",
                    "verbs",
                    "adjectives",
                    "pronouns",
                    "determiners",
                    "prepositions",
                    "conjunctions",
                    "negations",
                    "questions"
                )
                FlowRow(
                    horizontalArrangement = Arrangement.Center,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    categories.forEach { cat ->
                        val isSelected = selectedCategory == cat
                        val color = borderColorForCategory(cat)
                        Surface(
                            color = if (isSelected) color else Color.White,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(3.dp, if (isSelected) Color.Black else color),
                            modifier = Modifier
                                .height(70.dp)
                                .clickable { selectedCategory = cat }
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    cat.replaceFirstChar { it.uppercase() },
                                    fontSize = 18.sp
                                )
                            }
                        }
                    }
                }
            }

            // SAVE BUTTON
            Button(
                onClick = {
                    if (selectedImageUri != null && label.isNotBlank()) {
                        val userId = FirebaseAuth.getInstance().currentUser?.uid
                        if (userId != null) {
                            saveCardLocallyAndToFirebase(
                                context,
                                selectedImageUri!!,
                                label,
                                selectedCategory,
                                userId
                            ) { msg -> message = msg }
                        } else {
                            message = "Please sign in first!"
                        }
                    } else {
                        message = "Please select image and enter label"
                    }
                },
                modifier = Modifier.fillMaxWidth().height(90.dp),
                shape = RoundedCornerShape(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDFF0D8))
            ) {
                Text("Save Card", color = Color.Black, fontSize = 22.sp)
            }

            if (message.isNotEmpty()) {
                Text(message, color = if ("success" in message.lowercase()) Color(0xFF4CAF50) else Color.Red)
            }
        }
    }
}

/**
 * Saves a custom communication card's image to local storage and uploads the image
 * and its metadata to Firebase.
 *
 * This function performs a multi-step process:
 * 1.  It first checks if a user is logged in. If not, it returns an error.
 * 2.  It saves the provided image (`imageUri`) to the application's internal files directory
 *     under a structured path: `Custom_Words/{category}_A1/{label}.jpg`. This allows for
 *     offline access to the created card.
 * 3.  If local save is successful, it uploads the same image to Firebase Storage under a
 *     user-specific path: `users/{userId}/Custom_Words/{category}_A1/{label}.jpg`.
 * 4.  Upon successful upload, it retrieves the public download URL for the image.
 * 5.  Finally, it saves the card's metadata (label, category, level, image URL, and timestamp)
 *     to a Firestore document, creating a record of the custom card for the user.
 *
 * The result of the entire operation (success or failure with a message) is communicated
 * back through the `onResult` callback.
 *
 * @param context The application context, used for accessing the file system.
 * @param imageUri The URI of the image to be saved and uploaded.
 * @param label The text label for the card (e.g., "Hello", "Water"). This is also used as the filename.
 * @param selectedCategory The category the card belongs to (e.g., "nouns", "verbs").
 * @param userId The unique ID of the current Firebase user. If null, the operation fails.
 * @param onResult A lambda function that receives a String message indicating the outcome
 *                 (e.g., "Saved successfully!", "Upload failed: ...").
 */
fun saveCardLocallyAndToFirebase(
    context: Context,
    imageUri: Uri,
    label: String,
    selectedCategory: String,
    userId: String?,
    onResult: (String) -> Unit
) {
    if (userId == null) {
        onResult("Please sign in first!")
        return
    }

    com.google.firebase.FirebaseApp.initializeApp(context)

    val categoryFolder = "${selectedCategory}_A1"
    val localDir = File(context.filesDir, "Custom_Words/$categoryFolder").apply { mkdirs() }
    val localFile = File(localDir, "$label.jpg")

    try {
        context.contentResolver.openInputStream(imageUri)?.use { input ->
            FileOutputStream(localFile).use { output -> input.copyTo(output) }
        }
    } catch (e: Exception) {
        onResult("Failed to save locally: ${e.message}")
        return
    }

    val uploadUri = Uri.fromFile(localFile)
    val storageRef = FirebaseStorage.getInstance()
        .reference.child("users/$userId/Custom_Words/$categoryFolder/${label}.jpg")

    storageRef.putFile(uploadUri)
        .addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                val data = mapOf(
                    "label" to label,
                    "category" to selectedCategory,
                    "level" to "A1",
                    "imageUrl" to downloadUri.toString(),
                    "timestamp" to com.google.firebase.Timestamp.now()
                )

                FirebaseFirestore.getInstance()
                    .collection("USERS").document(userId)
                    .collection("Custom_Words")
                    .document(categoryFolder)
                    .collection("words")
                    .document(label)
                    .set(data)
                    .addOnSuccessListener {
                        onResult("Saved successfully!")
                    }
                    .addOnFailureListener { e ->
                        onResult("Firestore error: ${e.message}")
                    }
            }.addOnFailureListener { e ->
                onResult("Failed to get download URL: ${e.message}")
            }
        }
        .addOnFailureListener { e ->
            onResult("Upload failed: ${e.message}")
        }
}


fun borderColorForCategory(f: String): Color {
    return when {
        "adjective" in f   -> Adjective
        "conjunction" in f -> Conjunction
        "emergency" in f    -> Emergency
        "negation" in f    -> Negation
        "noun" in f        -> Noun
        "preposition" in f -> Proposition
        "pronoun" in f     -> Pronoun
        "question" in f    -> Question
        "social" in f      -> Social
        "verbs" in f        -> Verb
        "determiner" in f  -> Determiner
        else               -> Color.Black
    }
}
