package com.example.taptalk.ui.components

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.room.Room
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import com.example.taptalk.CreateCategoryActivity
import com.example.taptalk.aac.data.loadCategories
import com.example.taptalk.borderColorFor
import com.example.taptalk.data.AppDatabase
import com.example.taptalk.ui.theme.White
import com.google.firebase.auth.FirebaseAuth

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
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun CategoryBar(
    imageLoader: ImageLoader,
    onCategorySelected: (String?) -> Unit,
    onCategoryLongPress: (String) -> Unit,
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
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        userCats = db.userCategoryDao().getAll(uid)

        val userAsCategories = userCats.map { uc ->
            com.example.taptalk.aac.data.Category(
                label = uc.name,
                path = uc.imagePath?.let { "file://$it" }
                    ?: "file:///android_asset/icons/custom_folder.png"
            )
        }

        val addCategoryTile = com.example.taptalk.aac.data.Category(
            label = "+ New",
            path = "file:///android_asset/ACC_board/categories/new.png"
        )

        categories = (builtIn + userAsCategories)
            .distinctBy { it.label.lowercase() }
            .plus(addCategoryTile)
    }

    val visibleCount = 9
    val maxStart = (categories.size - visibleCount).coerceAtLeast(0)
    var startIndex by remember { mutableStateOf(0) }

    val visibleCats = categories.subList(
        startIndex,
        (startIndex + visibleCount).coerceAtMost(categories.size)
    )

    Row(
        modifier = modifier
            .height(100.dp)
            .background(White)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        val canGoPrev = startIndex > 0

        // Left button
        CategorySquareButton(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            contentDesc = "Previous categories",
            enabled = canGoPrev
        ) {
            if (canGoPrev) {
                startIndex = (startIndex - visibleCount).coerceAtLeast(0)
            }
        }

        // Category tile
        visibleCats.forEach { cat ->
            val labelLower = cat.label.lowercase()
            val isAdd = cat.label == "+ New"

            val userColorHex =
                userCats.find { it.name.equals(cat.label, ignoreCase = true) }?.colorHex
            val userColor = userColorHex?.let {
                Color(android.graphics.Color.parseColor(it))
            }

            val borderColor = when {
                isAdd -> Color.Black
                userColor != null -> userColor
                else -> borderColorFor(labelLower)
            }

            val bgColor = borderColor.copy(alpha = 0.25f)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(80.dp)
                    .border(4.dp, borderColor, RoundedCornerShape(10.dp))
                    .background(bgColor, RoundedCornerShape(10.dp))
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = LocalIndication.current,
                        onClick = {
                            if (isAdd) {
                                context.startActivity(
                                    android.content.Intent(context, CreateCategoryActivity::class.java)
                                )
                            } else {
                                if (cat.label.equals("Home", ignoreCase = true)) {
                                    onCategorySelected(null)
                                } else {
                                    onCategorySelected(cat.label)
                                }
                            }
                        },
                        onLongClick = {
                            if (!isAdd) {
                                onCategoryLongPress(cat.label)
                            }
                        }
                    ),
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

        val canGoNext = startIndex < maxStart

        // Right button
        CategorySquareButton(
            icon = Icons.AutoMirrored.Filled.ArrowForward,
            contentDesc = "Next categories",
            enabled = canGoNext
        ) {
            if (canGoNext) {
                startIndex = (startIndex + visibleCount).coerceAtMost(maxStart)
            }
        }
    }

}