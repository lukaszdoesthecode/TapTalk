package com.example.taptalk

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.taptalk.aac.data.AccCard
import com.example.taptalk.aac.data.loadAccCards
import com.example.taptalk.aac.data.loadCustomCards
import com.example.taptalk.aac.data.saveCategoryLocallyAndToFirebase
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.text.style.TextAlign

class CreateCategoryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                CreateCategoryScreen()
            }
        }
    }
}

@Composable
fun CreateCategoryScreen() {
    val context = LocalContext.current

    var name by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(Color(0xFFFFB347)) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var message by remember { mutableStateOf("") }

    val allCards = remember {
        loadAccCards(context) + loadCustomCards(context)
    }
    val selectedCards = remember { mutableStateListOf<AccCard>() }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) selectedImageUri = uri
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF7F4FF))
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Text("Create Custom Category", fontSize = 22.sp, fontWeight = FontWeight.Bold)

            // Name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Category name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Color picker row
            Text(
                text = "Pick a color",
                fontSize = 16.sp,
                modifier = Modifier.fillMaxWidth(),
            )

            val colors = listOf(
                Color(0xFFADD8E6),
                Color(0xFFD3D3D3),
                Color(0xFFFF6B6B),
                Color(0xFFFFB347),
                Color(0xFFFFC0CB),
                Color(0xFFFFF176),
                Color(0xFFB39DDB),
                Color(0xFF81C784),
                Color(0xFF90A4AE)
            )

            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                colors.forEach { col ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 6.dp)
                            .size(40.dp)
                            .background(col, RoundedCornerShape(20.dp))
                            .clickable { selectedColor = col }
                            .then(
                                if (col == selectedColor)
                                    Modifier.border(3.dp, Color.Black, RoundedCornerShape(20.dp))
                                else Modifier
                            )
                    )
                }
            }
            // Image picker
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(Color(0xFFD9D9D9), RoundedCornerShape(12.dp))
                    .clickable { pickImage.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (selectedImageUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(selectedImageUri),
                        contentDescription = "Selected category icon",
                        modifier = Modifier.fillMaxSize().padding(8.dp)
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.AddPhotoAlternate,
                            contentDescription = "Add image",
                            tint = Color.Gray,
                            modifier = Modifier.size(48.dp)
                        )
                        Text("Choose Icon", color = Color.Gray)
                    }
                }
            }

            Text("Choose words for this category", fontSize = 16.sp)


            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(12),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize().padding(bottom = 8.dp)
                ) {
                    items(allCards) { card ->
                        val isSelected = selectedCards.any { it.fileName == card.fileName }

                        val borderColor = if (isSelected) Color(0xFF388E3C) else borderColorFor(card.folder)
                        val bgColor = borderColor.copy(alpha = 0.2f)

                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .border(3.dp, borderColor, RoundedCornerShape(10.dp))
                                .background(bgColor, RoundedCornerShape(10.dp))
                                .clickable {
                                    if (isSelected) selectedCards.removeAll { it.fileName == card.fileName }
                                    else selectedCards.add(card)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(6.dp)
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter(Uri.parse(card.path)),
                                    contentDescription = card.label,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                                Text(
                                    text = card.label,
                                    fontSize = 12.sp,
                                    color = Color.Black,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    if (name.isBlank()) {
                        message = "Please enter category name"
                        return@Button
                    }
                    if (selectedCards.isEmpty()) {
                        message = "Please choose at least one word"
                        return@Button
                    }

                    val colorHex = String.format("#%06X", 0xFFFFFF and selectedColor.toArgb())
                    val fileNames = selectedCards.map { it.fileName }

                    saveCategoryLocallyAndToFirebase(
                        context = context,
                        imageUri = selectedImageUri,
                        name = name,
                        colorHex = colorHex,
                        selectedCardFileNames = fileNames
                    ) { msg ->
                        message = msg
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDFF0D8))
            ) {
                Text("Save Category", color = Color.Black, fontSize = 18.sp)
            }

            if (message.isNotEmpty()) {
                Text(
                    message,
                    color = if ("✅" in message || "success" in message.lowercase()) Color(0xFF388E3C) else Color.Red,
                    fontSize = 14.sp
                )
            }
        }
    }
}
