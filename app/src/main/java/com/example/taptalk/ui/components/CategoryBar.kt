package com.example.taptalk.ui.components

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.room.Room
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import com.example.taptalk.CreateCategoryActivity
import com.example.taptalk.aac.data.loadCategories
import com.example.taptalk.borderColorFor
import com.example.taptalk.data.AppDatabase

/**
 * A Composable that displays a horizontal bar of categories.
 *
 * This bar includes built-in categories, user-created categories, and an "add new" button.
 * It supports pagination to scroll through categories if they don't all fit on the screen.
 * Selecting a category triggers the [onCategorySelected] callback. Selecting the "Home"
 * category passes `null`, while other categories pass their label. The "+ New" button
 * launches the `CreateCategoryActivity`.
 *
 * The categories are loaded from both built-in assets and the local Room database.
 * Each category tile displays an image and a label, with a border color that is either
 * user-defined or determined by a hashing function.
 *
 * @param imageLoader The Coil [ImageLoader] used for asynchronously loading category images.
 * @param onCategorySelected A callback lambda that is invoked when a category is selected.
 *                           It receives the category label as a [String], or `null` if the
 *                           "Home" category is selected.
 * @param modifier The [Modifier] to be applied to the category bar row.
 */
@Composable
fun CategoryBar(
    imageLoader: ImageLoader,
    onCategorySelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var categories by remember { mutableStateOf<List<com.example.taptalk.aac.data.Category>>(emptyList()) }
    var userCats by remember { mutableStateOf(emptyList<com.example.taptalk.data.UserCategoryEntity>()) }


    LaunchedEffect(Unit) {
        val builtIn = loadCategories(context).map {
            com.example.taptalk.aac.data.Category(
                label = it.label,
                path = it.path
            )
        }

        val db = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "tap_talk_db"
        ).build()
        userCats = db.userCategoryDao().getAll()

        val userAsCategories = userCats.map { uc ->
            com.example.taptalk.aac.data.Category(
                label = uc.name,
                path = uc.imagePath?.let { "file://$it" } ?: "file:///android_asset/icons/custom_folder.png"
            )
        }

        val addCategoryTile = com.example.taptalk.aac.data.Category(
            label = "+ New",
            path = "file:///android_asset/icons/add_category.png"
        )

        categories = (builtIn + userAsCategories)
            .distinctBy { it.label.lowercase() } + listOf(addCategoryTile)
    }

    val visibleCount = 9
    val maxStart = (categories.size - visibleCount).coerceAtLeast(0)
    var startIndex by remember { mutableStateOf(0) }

    val endIndex = (startIndex + visibleCount).coerceAtMost(categories.size)
    val visibleCats = categories.subList(startIndex, endIndex)

    Row(
        modifier = modifier
            .background(Color(0xFFEFEFEF))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .border(3.dp, Color.Black, RoundedCornerShape(8.dp))
                .background(Color.LightGray, RoundedCornerShape(8.dp))
                .clickable {
                    startIndex = (startIndex - visibleCount).coerceAtLeast(0)
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Black)
        }

        visibleCats.forEach { cat ->
            val label = cat.label.lowercase()
            val isAddButton = cat.label == "+ New"

            val userCatColorHex = userCats.find { it.name.equals(cat.label, ignoreCase = true) }?.colorHex
            val userCatColor = userCatColorHex?.let { Color(android.graphics.Color.parseColor(it)) }

            val borderColor = when {
                isAddButton -> Color.Black
                userCatColor != null -> userCatColor
                else -> borderColorFor(label)
            }
            val bgColor = borderColor.copy(alpha = 0.25f)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .border(4.dp, borderColor, RoundedCornerShape(10.dp))
                    .background(bgColor, RoundedCornerShape(10.dp))
                    .clickable {
                        if (isAddButton) {
                            context.startActivity(
                                android.content.Intent(context, CreateCategoryActivity::class.java)
                            )
                        } else {
                            if (label == "home") onCategorySelected(null)
                            else onCategorySelected(cat.label)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(
                            model = Uri.parse(cat.path),
                            imageLoader = imageLoader
                        ),
                        contentDescription = cat.label,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                    )
                    Text(
                        text = cat.label,
                        fontSize = 12.sp,
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .border(3.dp, Color.Black, RoundedCornerShape(8.dp))
                .background(Color.LightGray, RoundedCornerShape(8.dp))
                .clickable {
                    startIndex = (startIndex + visibleCount).coerceAtMost(maxStart)
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.ArrowForward, contentDescription = "Next", tint = Color.Black)
        }
    }
    }



@Composable
fun CategorySquareButton(
    icon: ImageVector,
    contentDesc: String,
    enabled: Boolean,
    width: Dp = 80.dp,
    height: Dp = 70.dp,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(4.dp)
    val borderColor = if (enabled) Color.Black else Color.Gray
    val bg = if (enabled) Color(0xFFD9D9D9) else Color(0xFFE8E8E8)

    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .border(2.dp, borderColor, shape)
            .background(bg, shape)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDesc, tint = if (enabled) Color.Black else Color.Gray)
    }
}