package com.example.taptalk.ui.components

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import com.example.taptalk.aac.data.AccCard
import com.example.taptalk.borderColorFor
import com.example.taptalk.ui.theme.Bar

/**
 * A Composable that displays a horizontal bar with a green background,
 * showing a paginated list of suggestion cards (`AccCard`). It includes
 * buttons to navigate between pages of these suggestions.
 *
 * This component is typically used as a "suggestion bar" in an AAC (Augmentative and Alternative Communication) application.
 *
 * @param cards The list of `AccCard`s to display on the current page.
 * @param spacing The spacing to apply between the cards and around the content of the bar.
 * @param modifier The modifier to be applied to the `Row` container.
 * @param imageLoader The `ImageLoader` from Coil to be used for loading card images asynchronously.
 * @param onSuggestionClick A lambda that is invoked when a suggestion card is clicked. It receives the clicked `AccCard`.
 * @param page The current page number (0-indexed).
 * @param pageCount The total number of pages available.
 * @param onPrev A lambda that is invoked when the "previous page" button is clicked.
 * @param onNext A lambda that is invoked when the "next page" button is clicked.
 */
@Composable
fun GreenBar(
    cards: List<AccCard>,
    spacing: Dp,
    modifier: Modifier = Modifier,
    imageLoader: ImageLoader,
    onSuggestionClick: (AccCard) -> Unit,
    page: Int,
    pageCount: Int,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    val canGoPrev = page > 0
    val canGoNext = page < pageCount - 1

    Row(
        modifier = modifier
            .background(Bar)
            .fillMaxWidth()
            .padding(horizontal = spacing, vertical = spacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Previous Button
        CategorySquareButton(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            contentDesc = "Previous page",
            enabled = cards.isNotEmpty() && canGoPrev,
            onClick = { if (canGoPrev) onPrev() }
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            if (cards.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    cards.forEach { card ->
                        Box(
                            modifier = Modifier
                                .height(80.dp)
                                .width(80.dp)
                                .border(2.dp, borderColorFor(card.folder), RoundedCornerShape(4.dp))
                                .background(Color(0xFFD9D9D9), RoundedCornerShape(4.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = LocalIndication.current
                                ) { onSuggestionClick(card) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(2.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter(
                                        model = Uri.parse(card.path),
                                        imageLoader = imageLoader
                                    ),
                                    contentDescription = card.label,
                                    modifier = Modifier.weight(1f).fillMaxWidth()
                                )
                                Text(
                                    card.label,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            } else {
                Spacer(Modifier.height(80.dp))
            }
        }

        // Next Button
        CategorySquareButton(
            icon = Icons.AutoMirrored.Filled.ArrowForward,
            contentDesc = "Next page",
            enabled = cards.isNotEmpty() && canGoNext,
            onClick = { if (canGoNext) onNext() }
        )
    }
}
