package com.example.taptalk.ui.acc

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.imageLoader
import com.example.taptalk.acc.AccCard
import com.example.taptalk.acc.AccEvent
import com.example.taptalk.acc.AccUiState
import com.example.taptalk.acc.VerbForms
import com.example.taptalk.ui.BottomNavBar
import com.google.mlkit.nl.smartreply.SmartReplyGenerator
import com.google.mlkit.nl.smartreply.SmartReplySuggestionResult
import com.google.mlkit.nl.smartreply.TextMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import org.json.JSONObject
import java.util.Locale

@Composable
fun AccScreen(
    state: AccUiState,
    imageLoader: ImageLoader,
    smartReply: SmartReplyGenerator,
    events: Flow<AccEvent>,
    onSpeak: (String) -> Unit,
    onToggleFavourite: (AccCard) -> Unit,
    onRequestFavourites: () -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        events.collectLatest { event ->
            when (event) {
                is AccEvent.FavouriteToggled -> {
                    val message = if (event.added) "❤️ Added to Favourites" else "💔 Removed from Favourites"
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
                is AccEvent.Error -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    if (state.userId == null) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text("User not logged in", color = Color.Red, fontSize = 20.sp)
            }
        }
        return
    }

    if (state.isLoading) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                CircularProgressIndicator()
            }
        }
        return
    }

    val irregularVerbJson = state.irregularVerbs
    val irregularPluralJson = state.irregularPlurals
    val baseConversation = state.baseConversation
    val settings = state.settings

    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var showPopup by remember { mutableStateOf(false) }
    var selectedCard by remember { mutableStateOf<AccCard?>(null) }
    val chosen = remember { mutableStateListOf<AccCard>() }
    var suggestions by remember { mutableStateOf<List<AccCard>>(emptyList()) }

    val accDict = remember(state.allCards) { state.allCards.associateBy { it.label.lowercase(Locale.getDefault()) } }

    LaunchedEffect(chosen.toList(), settings.aiSupport, baseConversation) {
        val sentence = chosen.joinToString(" ") { it.label }

        if (sentence.isNotBlank() && settings.aiSupport) {
            val conversation = ArrayList<TextMessage>(baseConversation)
            conversation.add(TextMessage.createForLocalUser(sentence, System.currentTimeMillis()))
            val sorted = conversation.sortedBy { it.timestampMillis }

            smartReply.suggestReplies(sorted)
                .addOnSuccessListener { result ->
                    if (result.status == SmartReplySuggestionResult.STATUS_SUCCESS) {
                        suggestions = result.suggestions.mapNotNull { accDict[it.text.lowercase(Locale.getDefault())] }
                    } else {
                        suggestions = emptyList()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("ACC_DEBUG", "SmartReply failed", e)
                    suggestions = emptyList()
                }
        } else {
            suggestions = emptyList()
        }
    }

    LaunchedEffect(selectedCategory) {
        if (selectedCategory.equals("favourites", ignoreCase = true)) {
            onRequestFavourites()
        }
    }

    val gridSize = settings.gridSize
    val (rows, cols) = when (gridSize) {
        "Small" -> 5 to 9
        "Large" -> 7 to 13
        else -> 6 to 11
    }
    val perPage = rows * cols

    var visibleLevels by remember { mutableStateOf(settings.visibleLevels) }
    LaunchedEffect(settings.visibleLevels) {
        visibleLevels = settings.visibleLevels
    }

    val allCards = state.allCards
    val favourites = state.favourites
    val categories = state.categories

    val visibleCardsAll = remember(selectedCategory, allCards, favourites, gridSize, visibleLevels) {
        val levelRegex = Regex("_(A1|A2|B1|B2|C1|C2)")
        fun levelPass(fileName: String): Boolean {
            val match = levelRegex.find(fileName)
            val lvl = match?.groupValues?.getOrNull(1)
            return lvl == null || visibleLevels.contains(lvl)
        }

        fun keepSlotsWithLevels(list: List<AccCard>): List<AccCard?> =
            list.map { card -> if (levelPass(card.fileName)) card else null }

        when {
            selectedCategory.equals("favourites", ignoreCase = true) -> favourites.map { it as AccCard? }
            selectedCategory.equals("custom", ignoreCase = true) -> {
                val customs = allCards.filter { it.folder.startsWith("custom", ignoreCase = true) }
                keepSlotsWithLevels(customs)
            }
            !selectedCategory.isNullOrBlank() -> {
                val inCat = allCards.filter { it.folder.startsWith(selectedCategory!!, ignoreCase = true) }
                keepSlotsWithLevels(inCat)
            }
            else -> {
                val order = when (gridSize) {
                    "Small" -> smallGridOrder
                    else -> gridOrder
                }

                val byBase = allCards.associateBy { it.fileName.substringBeforeLast('.').lowercase(Locale.getDefault()) }
                order.map { key ->
                    if (key == null) return@map null
                    val card = byBase[key.lowercase(Locale.getDefault())]
                    if (card != null && levelPass(card.fileName)) card else null
                }
            }
        }
    }

    var page by remember { mutableStateOf(0) }

    LaunchedEffect(selectedCategory, gridSize, visibleLevels) {
        page = 0
    }

    val nonNullCount = visibleCardsAll.count { it != null }
    val pageCount = if (nonNullCount == 0) 1 else (nonNullCount + perPage - 1) / perPage
    LaunchedEffect(visibleCardsAll.size, perPage) {
        if (page >= pageCount) page = (pageCount - 1).coerceAtLeast(0)
    }
    val pageSlice = remember(page, visibleCardsAll, perPage) {
        visibleCardsAll.drop(page * perPage).take(perPage)
    }

    Scaffold(bottomBar = { BottomNavBar() }) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TopWhiteBar(
                chosen = chosen,
                spacing = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                imageLoader = imageLoader,
                onRemove = { idx -> chosen.removeAt(idx) },
                onClearAll = { chosen.clear() },
                onPlayStop = {
                    val sentence = chosen.joinToString(" ") { it.label }
                    if (sentence.isNotBlank()) onSpeak(sentence)
                },
                onSpeakWord = { word -> onSpeak(word) }
            )

            GreenBar(
                cards = suggestions,
                spacing = 6.dp,
                imageLoader = imageLoader,
                onSuggestionClick = { card -> if (chosen.size < 14) chosen.add(card) },
                page = page,
                pageCount = pageCount,
                onPrev = { if (page > 0) page-- },
                onNext = { if (page < pageCount - 1) page++ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
            )

            AACBoardGrid(
                imageLoader = imageLoader,
                rows = rows,
                cols = cols,
                gridSize = gridSize,
                visibleCards = pageSlice,
                favs = favourites,
                onCardClick = { card -> if (chosen.size < 14) chosen.add(card) },
                onCardLongPress = { card ->
                    when {
                        card.folder.contains("noun", ignoreCase = true) && irregularPluralJson != null -> {
                            val plural = suggestPlural(card.label, irregularPluralJson)
                            if (chosen.size < 14) chosen.add(card.copy(label = plural))
                        }

                        card.folder == "verbs" && irregularVerbJson != null -> {
                            selectedCard = card
                            showPopup = true
                        }

                        card.label.equals("will", ignoreCase = true) && irregularVerbJson != null -> {
                            selectedCard = card
                            showPopup = true
                        }

                        else -> if (chosen.size < 14) chosen.add(card)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            CategoryBar(
                categories = categories,
                imageLoader = imageLoader,
                onCategorySelected = { selectedCategory = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
            )
        }
    }

    if (showPopup && selectedCard != null && irregularVerbJson != null) {
        Dialog(onDismissRequest = { showPopup = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .border(4.dp, Color.Black, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val baseVerb = selectedCard!!.label.lowercase(Locale.getDefault())
                    val forms = getVerbForms(baseVerb, irregularVerbJson)

                    val presentVariants = when (baseVerb) {
                        "be" -> listOf("am", "is", "are")
                        "have" -> listOf("have", "has")
                        else -> emptyList()
                    }

                    val mainForms = buildList {
                        add("Past" to forms.past)
                        if (!forms.perfect.equals(forms.past, ignoreCase = true)) {
                            add("Perfect" to forms.perfect)
                        }
                    }

                    Text("Verb forms for ${selectedCard!!.label}", fontSize = 22.sp, color = Color.Black)

                    mainForms.forEach { (label, value) ->
                        VerbFormRow(
                            title = label,
                            value = value,
                            onSpeak = onSpeak,
                            iconPath = null
                        )
                    }

                    if (presentVariants.isNotEmpty()) {
                        presentVariants.forEach { variant ->
                            VerbFormRow(
                                title = variant,
                                value = variant,
                                onSpeak = onSpeak,
                                iconPath = null
                            )
                        }
                    }

                    if (forms.negatives.isNotEmpty()) {
                        Text("Negatives", fontSize = 20.sp, color = Color.Black)
                        forms.negatives.forEach { neg ->
                            VerbFormRow(
                                title = neg,
                                value = neg,
                                onSpeak = onSpeak,
                                iconPath = negativeIconFor(neg)
                            )
                        }
                    }
                }
            }
        }
    }
}

private val gridOrder: List<String?> = listOf(
    "what_A1", "I_A1", "we_A1", "have_A1", "know_A1", "child_A1", "place_A1", "time_A1", "hand_A1", "good_A1", "bad_A1",
    "how_A1", "you_A1", "they_A1", "come_A1", "make_A1", "man_A1", "world_A1", "year_A1", "thing_A1", "thirsty_A2", "hungry_A1",
    "why_A1", "he_A1", "be_A1", "do_A1", "say_A1", "woman_A1", "house_A1", "week_A1", "bathroom_A1", "happy_A1", "sad_A1",
    "where_A1", "she_A1", "yes_A1", "get_A1", "see_A1", "person_A1", "school_A1", "life_A1", "food_A1", "tired_A1", "calm_B1",
    "who_A1", "it_A1", "no_A1", "go_B1", "create_A1", "friend_A1", "job_A1", "family_A1", "water_A1", "and_A1", "or_A1",
    "when_A1", "a_A1", "an_A1", "the_A1", "hello_A1", "good_morning_A1", "bye_A1", "thanks_A1", "please_A1", "to_A1", "of_A1",
    null, null, null, null, null, null, null, null, null, null, null
)

private val smallGridOrder: List<String?> = listOf(
    "what_A1", "I_A1", "they_A1", "come_A1", "child_A1", "place_A1", "hand_A1", "good_A1", "bad_A1",
    "how_A1", "you_A1", "be_A1", "do_A1", "man_A1", "house_A1", "bathroom_A1", "thirsty_A2", "hungry_A1",
    "why_A1", "he_A1", "have_A1", "get_A1", "woman_A1", "school_A1", "food_A1", "happy_A1", "sad_A1",
    "where_A1", "she_A1", "yes_A1", "go_B1", "friend_A1", "job_A1", "water_A1", "tired_A1", "calm_B1",
    "who_A1", "we_A1", "no_A1", "hello_A1", "good_morning_A1", "bye_A1", "thanks_A1", "please_A1", "to_A1",
    null, null, null, null, null, null, null, null, null
)

private fun borderColorFor(folder: String): Color {
    val f = folder.lowercase(Locale.getDefault())
    return when {
        "adjective" in f -> Color(0xFFADD8E6)
        "conjunction" in f -> Color(0xFFD3D3D3)
        "emergency" in f -> Color(0xFFFF6B6B)
        "negation" in f -> Color(0xFFFF6B6B)
        "noun" in f -> Color(0xFFFFB347)
        "preposition" in f -> Color(0xFFFFC0CB)
        "pronoun" in f -> Color(0xFFFFF176)
        "question" in f -> Color(0xFFB39DDB)
        "social" in f -> Color(0xFFFFC0CB)
        "verbs" in f -> Color(0xFF81C784)
        "determiner" in f -> Color(0xFF90A4AE)
        else -> Color.Black
    }
}

private fun negativeIconFor(neg: String): String {
    val n = neg.lowercase(Locale.getDefault())
    val base = "file:///android_asset/tenses/"

    return when {
        "won't" in n || "will not" in n -> base + "negative_future.png"
        "haven" in n || "hasn" in n || "hadn" in n -> base + "negative_perfect.png"
        "didn" in n || "wasn" in n || "weren" in n -> base + "negative_past.png"
        "don'" in n || "doesn" in n || "isn" in n || "aren" in n -> base + "negative_present.png"
        else -> base + "negative_present.png"
    }
}

private fun suggestPlural(noun: String, json: JSONObject): String {
    val lower = noun.lowercase(Locale.getDefault())
    if (json.has(lower)) return json.getString(lower)

    return when {
        lower.endsWith("y") && lower.length > 1 &&
            !"aeiou".contains(lower[lower.length - 2]) ->
            lower.dropLast(1) + "ies"

        lower.endsWith("s") || lower.endsWith("x") || lower.endsWith("z") ||
            lower.endsWith("ch") || lower.endsWith("sh") ->
            lower + "es"

        else -> lower + "s"
    }
}

private fun getVerbForms(verb: String, json: JSONObject): VerbForms {
    specialVerbForms(verb)?.let { return it }

    val v = verb.lowercase(Locale.getDefault())
    if (json.has(v)) {
        val entry = json.getJSONObject(v)
        val past = entry.optString("past", v + "ed")
        val perfect = entry.optString("perfect", past)
        val negativesJson = entry.optJSONArray("negatives")
        val negatives = mutableListOf<String>()
        negativesJson?.let { arr ->
            for (i in 0 until arr.length()) negatives.add(arr.getString(i))
        }
        return VerbForms(v, past, perfect, negatives)
    }

    return VerbForms(v, v + "ed", v + "ed")
}

private fun specialVerbForms(verb: String): VerbForms? {
    return when (verb.lowercase(Locale.getDefault())) {
        "be" -> VerbForms(
            base = "be",
            past = "was/were",
            perfect = "been",
            negatives = listOf("am not", "is not", "are not", "was not", "were not")
        )
        "have" -> VerbForms(
            base = "have",
            past = "had",
            perfect = "had",
            negatives = listOf("don’t have", "doesn’t have", "didn’t have")
        )
        "will" -> VerbForms(
            base = "will",
            past = "would",
            perfect = "would have",
            negatives = listOf("won’t", "will not")
        )
        else -> null
    }
}

@Composable
private fun VerbFormRow(title: String, value: String, onSpeak: (String) -> Unit, iconPath: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, Color.Black, RoundedCornerShape(10.dp))
            .background(Color(0xFFF9F9F9), RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 18.sp, color = Color.Black)
            Text(value, fontSize = 20.sp, color = Color.DarkGray)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (iconPath != null) {
                val painter = rememberAsyncImagePainter(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(Uri.parse(iconPath))
                        .build(),
                    imageLoader = LocalContext.current.imageLoader
                )
                Image(
                    painter = painter,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            IconButton(onClick = { onSpeak(value) }) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Speak")
            }
        }
    }
}

@Composable
private fun TopWhiteBar(
    chosen: List<AccCard>,
    spacing: Dp,
    modifier: Modifier,
    imageLoader: ImageLoader,
    onRemove: (Int) -> Unit,
    onClearAll: () -> Unit,
    onPlayStop: () -> Unit,
    onSpeakWord: (String) -> Unit
) {
    Surface(
        modifier = modifier.background(Color.White),
        shadowElevation = 4.dp,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onPlayStop) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Speak all")
            }
            LazyRow(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalArrangement = Arrangement.spacedBy(spacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(chosen.size) { index ->
                    val card = chosen[index]
                    Column(
                        modifier = Modifier
                            .width(80.dp)
                            .height(70.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFF3F3F3))
                            .clickable { onSpeakWord(card.label) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
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
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Text(
                            card.label,
                            maxLines = 1,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                    IconButton(onClick = { onRemove(index) }) {
                        Icon(Icons.Default.Close, contentDescription = "Remove word")
                    }
                }
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onClearAll) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear")
                }
                IconButton(onClick = { /* reserved for future actions */ }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
            }
        }
    }
}

@Composable
private fun GreenBar(
    cards: List<AccCard>,
    spacing: Dp,
    imageLoader: ImageLoader,
    onSuggestionClick: (AccCard) -> Unit,
    page: Int,
    pageCount: Int,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier
) {
    Surface(
        modifier = modifier.background(Color(0xFFE8F5E9)),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onPrev, enabled = page > 0) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Previous")
            }
            LazyRow(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalArrangement = Arrangement.spacedBy(spacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(cards.size) { index ->
                    val card = cards[index]
                    SuggestionChip(card = card, imageLoader = imageLoader, onClick = { onSuggestionClick(card) })
                }
            }
            IconButton(onClick = onNext, enabled = page < pageCount - 1) {
                Icon(Icons.Default.ArrowForward, contentDescription = "Next")
            }
        }
    }
}

@Composable
private fun SuggestionChip(card: AccCard, imageLoader: ImageLoader, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(80.dp)
            .height(70.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .clickable { onClick() }
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
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
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
        )
        Text(card.label, maxLines = 1, fontSize = 12.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun AACBoardGrid(
    imageLoader: ImageLoader,
    rows: Int,
    cols: Int,
    gridSize: String,
    visibleCards: List<AccCard?>,
    favs: List<AccCard>,
    onCardClick: (AccCard) -> Unit,
    onCardLongPress: (AccCard) -> Unit,
    modifier: Modifier = Modifier
) {
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
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .border(2.dp, Color(0xFFBDBDBD), RoundedCornerShape(10.dp))
                                .background(Color(0xFFF3F3F3), RoundedCornerShape(10.dp)),
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
                                onClick = { onCardClick(card) },
                                onLongClick = { onCardLongPress(card) },
                                onDoubleClick = {
                                    onToggleFavourite(card)
                                }
                            )
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val painter = rememberAsyncImagePainter(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(Uri.parse(card.path))
                                    .crossfade(true)
                                    .build(),
                                imageLoader = imageLoader
                            )

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
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
                                            .background(Color.White.copy(alpha = 0.7f), RoundedCornerShape(50))
                                            .padding(2.dp)
                                    ) {
                                        Text("❤️", fontSize = 14.sp)
                                    }
                                }
                            }

                            Text(
                                text = card.label,
                                fontSize = when (gridSize) {
                                    "Small" -> 14.sp
                                    "Large" -> 10.sp
                                    else -> 12.sp
                                },
                                textAlign = TextAlign.Center,
                                color = Color.Black
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryBar(
    categories: List<AccCard>,
    imageLoader: ImageLoader,
    onCategorySelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier.background(Color.White)) {
        LazyRow(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item {
                CategoryChip(
                    label = "Home",
                    imageLoader = imageLoader,
                    path = "file:///android_asset/ACC_board/home.png",
                    onClick = { onCategorySelected(null) }
                )
            }
            items(categories.size) { index ->
                val card = categories[index]
                CategoryChip(
                    label = card.label,
                    imageLoader = imageLoader,
                    path = card.path,
                    onClick = { onCategorySelected(card.label.lowercase(Locale.getDefault())) }
                )
            }
        }
    }
}

@Composable
private fun CategoryChip(label: String, imageLoader: ImageLoader, path: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(80.dp)
            .height(70.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFF3F3F3))
            .clickable { onClick() }
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val painter = rememberAsyncImagePainter(
            model = ImageRequest.Builder(LocalContext.current)
                .data(Uri.parse(path))
                .crossfade(true)
                .build(),
            imageLoader = imageLoader
        )
        Image(
            painter = painter,
            contentDescription = label,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
        )
        Text(label, maxLines = 1, fontSize = 12.sp, textAlign = TextAlign.Center)
    }
}
