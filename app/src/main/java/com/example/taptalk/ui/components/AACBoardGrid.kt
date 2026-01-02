package com.example.taptalk.ui.components

import android.annotation.SuppressLint
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.taptalk.aac.data.AccCard
import com.example.taptalk.aac.data.toggleFavouriteInFirebase
import com.example.taptalk.borderColorFor
import com.google.firebase.auth.FirebaseAuth
import com.example.taptalk.ui.theme.*

/**
 * A composable that displays a grid of AAC (Augmentative and Alternative Communication) cards.
 * A composable that displays a grid of AAC (Augmentative and Alternative Communication) cards.
 *
 * This grid is configurable by the number of rows and columns. It handles displaying AAC cards,
 * showing empty slots, and processing user interactions like single clicks, long presses, and
 * double-clicks to toggle favorites. Each card displays an image and a label, with a border
 * color determined by its category. Favorite cards are indicated with a heart icon.
 *
 * @param imageLoader The Coil [ImageLoader] to use for asynchronously loading card images.
 * @param painterCache A map to cache painters, though it appears unused in the current implementation.
 * @param rows The number of rows in the grid.
 * @param cols The number of columns in the grid.
 * @param gridSize A string ("Small", "Medium", "Large") that determines the font size of the card labels.
 * @param visibleCards A list of [AccCard]s to be displayed in the grid. Null values represent empty slots.
 * @param favs A list of the user's favorite [AccCard]s, used to display the favorite indicator.
 * @param onCardClick A lambda function to be invoked when a card is single-clicked.
 * @param onCardLongPress A lambda function to be invoked when a card is long-pressed.
 * @param modifier The [Modifier] to be applied to the grid container.
 */
@SuppressLint("RememberReturnType")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AACBoardGrid(
    imageLoader: ImageLoader,
    painterCache: Map<String, Painter>,
    rows: Int,
    cols: Int,
    gridSize: String,
    visibleCards: List<AccCard?>,
    favs: List<AccCard>,
    onCardClick: (AccCard) -> Unit,
    onCardLongPress: (AccCard) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier.padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for (row in 0 until rows) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (col in 0 until cols) {
                    val idx = row * cols + col
                    val card = visibleCards.getOrNull(idx)

                    if (card == null) {
                        // EMPTY SLOT
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .border(2.dp, BlankTileBorder, RoundedCornerShape(10.dp))
                                .background(BlankTile, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(" ", color = Color.Gray, fontSize = 10.sp)
                        }
                        continue
                    }

                    val borderColor = borderColorFor(card.folder)
                    val bgColor = borderColor.copy(alpha = 0.2f)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .border(2.dp, borderColor, RoundedCornerShape(10.dp))
                            .background(bgColor, RoundedCornerShape(10.dp))
                            .combinedClickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = LocalIndication.current,
                                onClick = { onCardClick(card) },
                                onLongClick = { onCardLongPress(card) },
                                onDoubleClick = {
                                    toggleFavouriteInFirebase(
                                        context,
                                        card,
                                        FirebaseAuth.getInstance().currentUser?.uid
                                    ) { added ->
                                        android.widget.Toast.makeText(
                                            context,
                                            if (added) "❤️ Added to Favourites" else "💔 Removed from Favourites",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            )
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {

                            if (gridSize != "Large") {
                                // NORMAL MODE
                                val painter = rememberAsyncImagePainter(
                                    model = ImageRequest.Builder(context)
                                        .data(Uri.parse(card.path))
                                        .crossfade(true)
                                        .build(),
                                    imageLoader = imageLoader
                                )

                                val imageHeight = when (gridSize) {
                                    "Small" -> 0.50f
                                    "Medium" -> 0.60f
                                    else -> 0f   // Large mode
                                }

                                if (imageHeight > 0f) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .fillMaxHeight(imageHeight)
                                            .clip(RoundedCornerShape(6.dp))
                                    ) {
                                        Image(
                                            painter = painter,
                                            contentDescription = card.label,
                                            modifier = Modifier.fillMaxSize()
                                        )

                                        if (favs.any { it.label == card.label }) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(4.dp)
                                                    .background(
                                                        Color.White.copy(alpha = 0.7f),
                                                        RoundedCornerShape(50)
                                                    )
                                                    .padding(2.dp)
                                            ) {
                                                Text("❤️", fontSize = 14.sp)
                                            }
                                        }
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(36.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    BoxWithConstraints(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val maxWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
                                        val label = card.label
                                        val isMultiWord = label.contains(" ")

                                        val baseSize = 1.sp

                                        // For single words
                                        val finalSize = if (!isMultiWord) {
                                            var chosen = baseSize
                                            for (size in 18 downTo 10) {
                                                val textWidth = size * label.length * 0.55f
                                                if (textWidth <= maxWidthPx) {
                                                    chosen = size.sp
                                                    break
                                                }
                                            }
                                            chosen
                                        } else {
                                            16.sp  // multi-word
                                        }

                                        Text(
                                            text = label,
                                            fontSize = finalSize,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black,
                                            textAlign = TextAlign.Center,
                                            maxLines = if (isMultiWord) 2 else 1,
                                            softWrap = isMultiWord,
                                        )
                                    }
                                }

                            } else {
                                // LARGE MODE
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = card.label,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        color = Color.Black,
                                        maxLines = 2
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}