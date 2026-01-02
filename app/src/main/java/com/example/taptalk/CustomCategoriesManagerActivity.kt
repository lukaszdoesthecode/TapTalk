package com.example.taptalk

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.taptalk.data.AppDatabase
import com.example.taptalk.data.UserCategoryEntity
import com.example.taptalk.ui.components.BottomNavBar
import kotlinx.coroutines.launch
import com.example.taptalk.aac.data.scheduleCategorySync
import com.google.firebase.auth.FirebaseAuth

/**
 * An activity that provides a user interface for managing custom communication categories.
 * This screen allows users to view, edit, and delete their own created categories.
 * It serves as the entry point for the category management feature, setting up the
 * main composable screen `CustomCategoriesManagerScreen`.
 */
class CustomCategoriesManagerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                CustomCategoriesManagerScreen()
            }
        }
    }
}

/**
 * A Composable screen that allows users to manage their custom categories.
 *
 * This screen displays a list of all user-created categories. It provides options
 * to edit a category's name, color, and icon, delete a category, or manage the
 * words associated with it. The categories are fetched from the local Room database.
 * Any modifications (updates or deletions) trigger a background sync job.
 *
 * It features:
 * - A list of custom categories displayed in `CategoryItem` cards.
 * - A message indicating when no custom categories have been created.
 * - An `EditCategoryDialog` that appears when a user chooses to edit a category.
 * - A bottom navigation bar provided by `BottomNavBar`.
 */
@Composable
fun CustomCategoriesManagerScreen() {
    val context = LocalContext.current
    val db = remember { androidx.room.Room.databaseBuilder(context, AppDatabase::class.java, "tap_talk_db").build() }
    val scope = rememberCoroutineScope()

    var categories by remember { mutableStateOf<List<UserCategoryEntity>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf<UserCategoryEntity?>(null) }

    LaunchedEffect(Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        val local = db.userCategoryDao().getAll(uid)
        val unique = local.distinctBy { it.name.lowercase() } // remove duplicates by name
        categories = unique
    }

    Scaffold(bottomBar = { BottomNavBar() }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF7F4FF))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Manage Custom Categories",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            if (categories.isEmpty()) {
                Text(
                    "No custom categories found.",
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(categories.size) { index ->
                        val cat = categories[index]
                        CategoryItem(
                            category = cat,
                            onEdit = { selectedCategory = cat },
                            onDelete = {
                                scope.launch {
                                    db.userCategoryDao().delete(cat)
                                    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                                    categories = db.userCategoryDao().getAll(uid)
                                    scheduleCategorySync(context)
                                }
                            },
                            onManageWords = {
                                val intent = Intent(context, ManageCategoryWordsActivity::class.java)
                                intent.putExtra("categoryName", cat.name)
                                context.startActivity(intent)
                            }
                        )
                    }
                }
            }
        }

        if (selectedCategory != null) {
            EditCategoryDialog(
                category = selectedCategory!!,
                onDismiss = { selectedCategory = null },
                onSave = { updated ->
                    scope.launch {
                        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

                        val unsynced = updated.copy(
                            synced = false,
                            userId = uid
                        )

                        db.userCategoryDao().insertOrUpdate(unsynced)

                        categories = db.userCategoryDao().getAll(uid)
                        selectedCategory = null
                        scheduleCategorySync(context)
                    }
                }

            )
        }
    }
}

/**
 * A Composable that displays a single category item in a list.
 * It shows the category's image (or a placeholder), name, and word count.
 * It also provides interactive buttons for editing, deleting, and managing the words
 * within the category.
 *
 * @param category The [UserCategoryEntity] object containing the data for the category to be displayed.
 * @param onEdit A lambda function to be invoked when the user clicks the edit button.
 * @param onDelete A lambda function to be invoked when the user clicks the delete button.
 * @param onManageWords A lambda function to be invoked when the user clicks the "manage words" button.
 */
@Composable
fun CategoryItem(
    category: UserCategoryEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onManageWords: () -> Unit
) {
    val borderCol = try {
        Color(android.graphics.Color.parseColor(category.colorHex))
    } catch (_: Exception) {
        Color.LightGray
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, borderCol, RoundedCornerShape(10.dp))
            .background(Color.White)
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (category.imagePath != null) {
                Image(
                    painter = rememberAsyncImagePainter(Uri.parse(category.imagePath)),
                    contentDescription = category.name,
                    modifier = Modifier.size(70.dp).clip(RoundedCornerShape(8.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .background(Color(0xFFD9D9D9), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No\nIcon", textAlign = TextAlign.Center, color = Color.Gray, fontSize = 12.sp)
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(category.name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Words: ${category.cardFileNames.size}", color = Color.Gray, fontSize = 14.sp)
            }

            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF388E3C))
            }

            IconButton(onClick = onManageWords) {
                Icon(Icons.Default.MoreVert, contentDescription = "Manage words", tint = Color(0xFF1976D2))
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
            }
        }
    }
}

/**
 * A composable function that displays a dialog for editing a user-created category.
 *
 * This dialog allows the user to change the category's name, its associated icon image, and its
 * representative color. It pre-populates the fields with the existing category's data.
 * The user can save their changes, which triggers the `onSave` callback, or cancel the
 * operation, which triggers the `onDismiss` callback.
 *
 * @param category The [UserCategoryEntity] to be edited. Its properties are used to initialize the dialog's state.
 * @param onDismiss A lambda function to be invoked when the user dismisses the dialog (e.g., by pressing the "Cancel" button or clicking outside the dialog).
 * @param onSave A lambda function that is invoked when the user clicks the "Save" button. It receives the updated [UserCategoryEntity] with the new name, color, and image path.
 */
@Composable
fun EditCategoryDialog(
    category: UserCategoryEntity,
    onDismiss: () -> Unit,
    onSave: (UserCategoryEntity) -> Unit
) {
    var name by remember { mutableStateOf(category.name) }
    var selectedColor by remember {
        mutableStateOf(Color(android.graphics.Color.parseColor(category.colorHex)))
    }
    var selectedImageUri by remember { mutableStateOf<Uri?>(if (category.imagePath != null) Uri.parse(category.imagePath) else null) }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) selectedImageUri = uri
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Category") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category name") },
                    modifier = Modifier.fillMaxWidth()
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(Color(0xFFD9D9D9), RoundedCornerShape(8.dp))
                        .clickable { pickImage.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedImageUri != null) {
                        Image(
                            painter = rememberAsyncImagePainter(selectedImageUri),
                            contentDescription = "Category Icon",
                            modifier = Modifier.fillMaxSize().padding(8.dp)
                        )
                    } else {
                        Text("Tap to change icon", color = Color.Gray)
                    }
                }

                Text("Pick a color", fontSize = 16.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)

                val colors = listOf(
                    Color(0xFFADD8E6), Color(0xFFD3D3D3), Color(0xFFFF6B6B),
                    Color(0xFFFFB347), Color(0xFFFFC0CB), Color(0xFFFFF176),
                    Color(0xFFB39DDB), Color(0xFF81C784), Color(0xFF90A4AE)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    colors.forEach { col ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 6.dp)
                                .size(36.dp)
                                .background(col, RoundedCornerShape(18.dp))
                                .clickable { selectedColor = col }
                                .then(
                                    if (col == selectedColor)
                                        Modifier.border(3.dp, Color.Black, RoundedCornerShape(18.dp))
                                    else Modifier
                                )
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val updated = category.copy(
                    name = name,
                    colorHex = String.format("#%06X", 0xFFFFFF and selectedColor.toArgb()),
                    imagePath = selectedImageUri?.toString()
                )
                onSave(updated)
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
