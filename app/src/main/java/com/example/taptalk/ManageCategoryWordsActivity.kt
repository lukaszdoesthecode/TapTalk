package com.example.taptalk

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.example.taptalk.aac.data.AccCard
import com.example.taptalk.aac.data.loadAccCards
import com.example.taptalk.data.AppDatabase
import com.example.taptalk.data.UserCategoryEntity
import kotlinx.coroutines.launch

/**
 * Activity that lets the user manage (add/remove) words inside a custom category.
 * Opens when clicking “Manage Words” from the CustomCategoriesManagerActivity.
 */
class ManageCategoryWordsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val categoryName = intent.getStringExtra("categoryName")
        setContent {
            MaterialTheme {
                ManageCategoryWordsScreen(categoryName)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageCategoryWordsScreen(categoryName: String?) {
    val context = LocalContext.current
    val db = remember {
        androidx.room.Room.databaseBuilder(context, AppDatabase::class.java, "tap_talk_db").build()
    }
    val scope = rememberCoroutineScope()

    var category by remember { mutableStateOf<UserCategoryEntity?>(null) }
    var allCards by remember { mutableStateOf<List<AccCard>>(emptyList()) }
    var selected by remember { mutableStateOf<MutableSet<String>>(mutableSetOf()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val cat = db.userCategoryDao().getAll().find { it.name == categoryName }
        category = cat
        if (cat != null) {
            selected = cat.cardFileNames.toMutableSet()
        }
        allCards = loadAccCards(context)
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Words") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = { (context as? ComponentActivity)?.finish() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF6A1B9A))
                    }
                },
                actions = {
                    if (category != null) {
                        Button(
                            onClick = {
                                scope.launch {
                                    val updated = category!!.copy(
                                        cardFileNames = selected.toList(),
                                        synced = false
                                    )
                                    db.userCategoryDao().insertOrUpdate(updated)
                                    (context as? ComponentActivity)?.finish()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF6A1B9A),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("Save", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color(0xFFF8F8F8))
                    .padding(12.dp)
            ) {
                Text(
                    "Tap words to add or remove from this category",
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(8.dp)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(allCards) { card ->
                        val isSelected = selected.contains(card.fileName)
                        WordCardItem(card, isSelected) {
                            selected = selected.toMutableSet().apply {
                                if (isSelected) remove(card.fileName) else add(card.fileName)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WordCardItem(card: AccCard, isSelected: Boolean, onClick: () -> Unit) {
    val borderColor = if (isSelected) Color(0xFF81C784) else Color.LightGray
    val backgroundColor = if (isSelected) Color(0xFFE8F5E9) else Color.White

    Box(
        modifier = Modifier
            .background(backgroundColor, RoundedCornerShape(10.dp))
            .border(2.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = rememberAsyncImagePainter(card.path),
                contentDescription = card.label,
                modifier = Modifier
                    .size(70.dp)
                    .padding(bottom = 4.dp)
            )
            Text(
                text = card.label,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) Color(0xFF2E7D32) else Color.Black,
                fontSize = 14.sp
            )
        }
    }
}
