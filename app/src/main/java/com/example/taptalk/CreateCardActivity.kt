package com.example.taptalk

import android.app.Activity
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
                .background(Color(0xFFF7F4FF))
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

fun saveCardLocallyAndToFirebase(
    context: Context,
    imageUri: Uri,
    label: String,
    selectedCategory: String,
    userId: String?,
    onResult: (String) -> Unit
) {
    if (userId == null) {
        onResult("User not logged in.")
        return
    }

    val categoryFolder = "${selectedCategory}_A1"

    // Save image locally
    try {
        val localDir = File(context.filesDir, "Custom_Words/$categoryFolder")
        if (!localDir.exists()) localDir.mkdirs()

        val localFile = File(localDir, "${label}.jpg")
        context.contentResolver.openInputStream(imageUri)?.use { input ->
            FileOutputStream(localFile).use { output ->
                input.copyTo(output)
            }
        }
    } catch (e: Exception) {
        onResult("Failed to save locally: ${e.message}")
        return
    }

    // Upload to Firebase Storage
    val storageRef = FirebaseStorage.getInstance()
        .reference.child("users/$userId/Custom_Words/$categoryFolder/${label}.jpg")

    storageRef.putFile(imageUri)
        .addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { downloadUri ->

                val data = mapOf(
                    "label" to label,
                    "category" to selectedCategory,
                    "level" to "A1",
                    "imageUrl" to downloadUri.toString(),
                    "timestamp" to com.google.firebase.Timestamp.now()
                )

                // Save metadata to Firestore
                val firestore = FirebaseFirestore.getInstance()
                firestore.collection("USERS")
                    .document(userId)
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
        "adjective" in f -> Color(0xFFADD8E6)
        "conjunction" in f -> Color(0xFFD3D3D3)
        "negation" in f -> Color(0xFFFF6B6B)
        "noun" in f -> Color(0xFFFFB347)
        "preposition" in f -> Color(0xFFFFC0CB)
        "pronoun" in f -> Color(0xFFFFF176)
        "question" in f -> Color(0xFFB39DDB)
        "social" in f -> Color(0xFFFFC0CB)
        "verb" in f -> Color(0xFF81C784)
        "determiner" in f -> Color(0xFF90A4AE)
        else -> Color.Black
    }
}
