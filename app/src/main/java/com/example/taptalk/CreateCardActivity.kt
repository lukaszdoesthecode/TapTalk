package com.example.taptalk

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCardScreen() {
    val context = LocalContext.current
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var label by remember { mutableStateOf(TextFieldValue("")) }
    var selectedCategory by remember { mutableStateOf("nouns") }
    var message by remember { mutableStateOf<String?>(null) }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) selectedImageUri = uri
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create New Card") },
                navigationIcon = {
                    IconButton(onClick = { (context as? Activity)?.finish() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Image picker
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .border(2.dp, Color.Gray, RoundedCornerShape(8.dp))
                    .background(Color(0xFFF1F1F1), RoundedCornerShape(8.dp))
                    .clickable { pickImage.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (selectedImageUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(selectedImageUri),
                        contentDescription = "Selected image",
                        modifier = Modifier.fillMaxSize().padding(8.dp)
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Add image", tint = Color.Gray)
                        Text("Choose Image", color = Color.Gray)
                    }
                }
            }

            // Label
            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Label (word or phrase)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Category picker
            var expanded by remember { mutableStateOf(false) }
            val categories = listOf("nouns", "verbs", "adjectives", "emotions", "social", "places", "food")

            Box(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { expanded = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(selectedCategory, color = Color.Black)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = {
                                selectedCategory = cat
                                expanded = false
                            }
                        )
                    }
                }
            }

            Button(
                onClick = {
                    if (selectedImageUri != null && label.text.isNotBlank()) {
                        saveCustomCard(context, selectedImageUri!!, label.text, selectedCategory)
                        message = "Card saved successfully!"
                        label = TextFieldValue("")
                        selectedImageUri = null
                    } else {
                        message = "Please select image and enter label"
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDFF0D8))
            ) {
                Text("Save Card", color = Color.Black)
            }

            message?.let {
                Text(it, color = if (it.contains("success", true)) Color(0xFF2E7D32) else Color.Red)
            }
        }
    }
}

suspend fun copyUriToFile(context: Context, uri: Uri, destFile: File) = withContext(Dispatchers.IO) {
    context.contentResolver.openInputStream(uri)?.use { input ->
        FileOutputStream(destFile).use { output ->
            input.copyTo(output)
        }
    }
}

fun saveCustomCard(context: Context, imageUri: Uri, label: String, folder: String) {
    val fileDir = context.filesDir
    val destFile = File(fileDir, "custom_${System.currentTimeMillis()}.png")

    try {
        val inputStream: InputStream? = context.contentResolver.openInputStream(imageUri)
        val outputStream = FileOutputStream(destFile)
        inputStream?.copyTo(outputStream)
        inputStream?.close()
        outputStream.close()

        val jsonFile = File(fileDir, "custom_cards.json")
        val jsonArray = if (jsonFile.exists()) {
            JSONArray(jsonFile.readText())
        } else JSONArray()

        val newCard = JSONObject().apply {
            put("fileName", destFile.name)
            put("label", label)
            put("path", destFile.toURI().toString())
            put("folder", folder)
        }

        jsonArray.put(newCard)
        jsonFile.writeText(jsonArray.toString())

    } catch (e: Exception) {
        e.printStackTrace()
    }
}
