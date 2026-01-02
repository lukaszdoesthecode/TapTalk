package com.example.taptalk.ui.components

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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.taptalk.aac.data.AccCard
import com.example.taptalk.borderColorFor
import org.burnoutcrew.reorderable.detectReorder
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable

/**
 * A Composable that displays a top bar containing a sequence of chosen AAC (Augmentative and Alternative Communication) cards.
 * This bar allows users to see their selected cards, reorder them via drag-and-drop,
 * remove individual cards, clear the entire selection, and initiate text-to-speech for the sequence.
 *
 * @param chosen The list of [AccCard]s that have been selected to be displayed in the bar.
 * @param modifier The modifier to be applied to the top bar's root [Row].
 * @param spacing The spacing to be applied between the cards and around the content.
 * @param imageLoader The Coil [ImageLoader] used to load the card images asynchronously.
 * @param painterCache A mutable map to cache painters. Note: This parameter is currently unused in the implementation but is kept for potential future optimizations.
 * @param onReorder A callback function invoked when a card is dragged and dropped to a new position. It provides the 'from' and 'to' indices.
 * @param onRemove A callback function invoked when a card is long-pressed, signaling its removal. It provides the index of the card to remove.
 * @param onClearAll A callback function invoked when the 'clear all' button is clicked, to remove all cards from the bar.
 * @param onPlayStop A callback function invoked when the 'play/stop' button is clicked, to speak the sequence of card labels.
 * @param onSpeakWord A callback function invoked when a single card is clicked, to speak its label individually.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TopWhiteBar(
    chosen: List<AccCard>,
    modifier: Modifier = Modifier,
    spacing: Dp = 6.dp,
    imageLoader: ImageLoader,
    painterCache: MutableMap<String, Painter>,
    onReorder: (Int, Int) -> Unit = { _, _ -> },
    onRemove: (Int) -> Unit = {},
    onClearAll: () -> Unit = {},
    onPlayStop: () -> Unit = {},
    onSpeakWord: (String) -> Unit = {}
) {
    val state = rememberReorderableLazyListState(
        onMove = { from, to -> onReorder(from.index, to.index) }
    )
    Row(
        modifier = modifier
            .background(Color.White)
            .padding(horizontal = spacing, vertical = spacing / 2),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LazyRow(
            state = state.listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .reorderable(state)
                .detectReorderAfterLongPress(state),
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(chosen.size, key = { it }) { index ->
                val card = chosen[index]
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(1f)
                        .detectReorder(state)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(2.dp, borderColorFor(card.folder), RoundedCornerShape(4.dp))
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFD9D9D9))
                            .combinedClickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = LocalIndication.current,
                                onClick = { onSpeakWord(card.label) },
                                onLongClick = { onRemove(index) }
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val painter = rememberAsyncImagePainter(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(Uri.parse(card.path))
                                .crossfade(true)
                                .build(),
                            imageLoader = imageLoader
                        )

                        Image(
                            painter = painter,
                            contentDescription = card.label,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        )
                        Text(card.label, fontSize = 12.sp, textAlign = TextAlign.Center)

                    }
                }
            }
        }
        IconButton(onClick = onClearAll) {
            Icon(Icons.Default.Delete, contentDescription = "Clear all")
        }
        IconButton(onClick = onPlayStop) {
            Icon(Icons.Default.PlayArrow, contentDescription = "Play/Stop")
        }
    }
}