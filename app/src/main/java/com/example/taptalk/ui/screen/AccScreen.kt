package com.example.taptalk.ui.screen

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import com.example.taptalk.aac.data.AccCard
import com.example.taptalk.aac.data.loadAccCards
import com.example.taptalk.aac.data.loadCustomCards
import com.example.taptalk.aac.data.loadFavourites
import com.example.taptalk.aac.data.loadJsonAsset
import com.example.taptalk.getVerbForms
import com.example.taptalk.gridOrder
import com.example.taptalk.negativeIconFor
import com.example.taptalk.smallGridOrder
import com.example.taptalk.suggestPlural
import com.example.taptalk.ui.components.AACBoardGrid
import com.example.taptalk.ui.components.BottomNavBar
import com.example.taptalk.ui.components.CategoryBar
import com.example.taptalk.ui.components.GreenBar
import com.example.taptalk.ui.components.TopWhiteBar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.mlkit.nl.smartreply.SmartReplyGenerator
import com.google.mlkit.nl.smartreply.SmartReplySuggestionResult
import com.google.mlkit.nl.smartreply.TextMessage
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import androidx.room.Room
import com.example.taptalk.data.AppDatabase
import com.example.taptalk.data.UserCategoryEntity
import com.example.taptalk.largeGridOrder

/**
 * Generates a list of suggested AAC cards based on keywords found in the user's text.
 *
 * This function serves as a fallback or supplement to the ML Kit Smart Reply feature. It performs
 * simple keyword matching on the input text to provide contextually relevant card suggestions.
 * For example, if the user's text contains "hungry", it suggests cards like "eat", "food", and "water".
 *
 * @param userText The text composed by the user, which will be scanned for keywords.
 * @param accDict A map where keys are lowercase card labels and values are the corresponding [AccCard] objects.
 *                This is used to efficiently look up card data for the suggested labels.
 * @return A list of unique [AccCard] objects that are relevant to the keywords found in the `userText`.
 *         Returns an empty list if no keywords are matched.
 */
fun generateFallbackSuggestions(userText: String, accDict: Map<String, AccCard>): List<AccCard> {
    val text = userText.lowercase()
    val matches = mutableListOf<String>()

    when {
        "hungry" in text || "food" in text -> matches += listOf("eat", "food", "water", "drink")
        "tired" in text || "sleep" in text -> matches += listOf("sleep", "bed", "rest")
        "happy" in text || "good" in text -> matches += listOf("smile", "fun", "yes")
        "sad" in text || "bad" in text -> matches += listOf("cry", "help", "no")
        "thirsty" in text -> matches += listOf("drink", "water", "cup")
        "help" in text -> matches += listOf("doctor", "emergency", "please")
        "hello" in text || "hi" in text -> matches += listOf("how", "are", "you")
        "thank" in text -> matches += listOf("welcome", "bye")
        "go" in text -> matches += listOf("walk", "come", "run")
    }

    return matches.mapNotNull { accDict[it.lowercase()] }.distinct()
}


/**
 * The main screen for the Augmentative and Alternative Communication (AAC) feature.
 *
 * This composable function orchestrates the entire AAC interface, including the sentence construction
 * bar, the smart reply suggestion bar, the main grid of communication cards, and the category
 * selection bar. It manages the state for the current sentence, card selections, grid size,
 * AI-powered suggestions, and grammatical variations (like verb tenses and noun plurals).
 *
 * @param imageLoader The Coil [ImageLoader] used to asynchronously load card images from assets.
 * @param smartReply The [SmartReplyGenerator] instance from ML Kit used to generate contextual
 *   suggestions based on the constructed sentence.
 * @param baseConversation A list of [TextMessage]s that provide a basic context for the
 *   [smartReply] generator to improve initial suggestions.
 * @param spacing The [Dp] value for spacing between elements, defaulting to 6.dp.
 * @param speak A lambda function `(String) -> Unit` that is invoked to speak a given sentence
 *   or word using a Text-To-Speech engine.
 */
@Composable
fun AccScreen(
    imageLoader: ImageLoader,
    smartReply: SmartReplyGenerator,
    baseConversation: List<TextMessage>,
    spacing: Dp = 6.dp,
    speak: (String) -> Unit
) {

    var showCategoryLetterPopup by remember { mutableStateOf(false) }
    var selectedCategoryLetter by remember { mutableStateOf<Char?>(null) }
    var longPressedCategory by remember { mutableStateOf<String?>(null) }

    var selectedCategory by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    var autoSpeak by remember { mutableStateOf(true) }
    var smartReplyEnabled by remember { mutableStateOf(true) }

    val irregularVerbJson = remember { loadJsonAsset(context, "irregular_verbs.json") }
    val irregularPluralJson = remember { loadJsonAsset(context, "irregular_nouns.json") }

    var showPopup by remember { mutableStateOf(false) }
    var selectedCard by remember { mutableStateOf<AccCard?>(null) }
    var popupType by remember { mutableStateOf("verb") }

    val chosen = remember { mutableStateListOf<AccCard>() }
    var suggestions by remember { mutableStateOf<List<AccCard>>(emptyList()) }

    var accCards by remember { mutableStateOf<List<AccCard>>(emptyList()) }
    LaunchedEffect(Unit) {
        accCards = withContext(kotlinx.coroutines.Dispatchers.IO) { loadAccCards(context) }
    }
    val accDict = remember(accCards) { accCards.associateBy { it.label.lowercase() } }


    LaunchedEffect(chosen.toList(), smartReplyEnabled) {
        val sentence = chosen.joinToString(" ") { it.label }

        if (!smartReplyEnabled) {
            suggestions = emptyList()
            return@LaunchedEffect
        }

        if (sentence.isBlank()) {
            suggestions = emptyList()
            return@LaunchedEffect
        }

        smartReply.suggestReplies(
            listOf(TextMessage.createForLocalUser(sentence, System.currentTimeMillis()))
        ).addOnSuccessListener { result ->
            if (result.status == SmartReplySuggestionResult.STATUS_SUCCESS) {
                val replies = result.suggestions.map { it.text }
                val smartReplies = replies.filter {
                    it.lowercase() !in listOf("nice", "ok", "okay", "thanks")
                }

                val customSuggestions = generateFallbackSuggestions(sentence, accDict)

                suggestions = (smartReplies.mapNotNull { accDict[it.lowercase()] } + customSuggestions)
                    .distinct()
            } else {
                suggestions = emptyList()
            }
        }.addOnFailureListener {
            suggestions = emptyList()
        }
    }


        var gridSize by remember { mutableStateOf("Medium") }
    LaunchedEffect(Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            FirebaseFirestore.getInstance()
                .collection("USERS")
                .document(uid)
                .collection("Fast_Settings")
                .document("current")
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null && snapshot.exists()) {
                        autoSpeak = snapshot.getBoolean("autoSpeak") ?: true
                        smartReplyEnabled = snapshot.getBoolean("aiSupport") ?: true
                        gridSize = snapshot.getString("gridSize") ?: "Medium"
                    }
                }
        }
    }

    val (rows, cols) = when (gridSize) {
        "Small" -> 5 to 9
        "Large" -> 7 to 13
        else -> 6 to 11
    }
    val perPage = rows * cols

    val builtInCards = remember { loadAccCards(context) }
    val customCards = remember { loadCustomCards(context) }
    val allCards = remember { (builtInCards + customCards).distinctBy { it.fileName } }

    var userCategories by remember { mutableStateOf<List<UserCategoryEntity>>(emptyList()) }

    LaunchedEffect(Unit) {
        val db = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "tap_talk_db"
        ).build()
        userCategories = db.userCategoryDao().getAll()
    }

    val favs = remember { mutableStateListOf<AccCard>() }
    LaunchedEffect(selectedCategory) {
        if (selectedCategory.equals("favourites", ignoreCase = true)) {
            loadFavourites(
                context,
                FirebaseAuth.getInstance().currentUser?.uid
            ) { list ->
                favs.clear(); favs.addAll(list)
            }
        }
    }

    var visibleLevels by remember { mutableStateOf(listOf("A1", "A2", "B1")) }

    LaunchedEffect(Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            val snap = FirebaseFirestore.getInstance()
                .collection("USERS").document(uid)
                .collection("Fast_Settings").document("current")
                .get().await()
            val list = snap.get("visibleLevels") as? List<*>
            visibleLevels = list?.filterIsInstance<String>() ?: listOf("A1", "A2", "B1")
        }
    }

    val visibleCardsAll by remember(selectedCategory, allCards, favs.toList(), gridSize, visibleLevels, userCategories) {
        derivedStateOf {
            fun levelOf(fileName: String): String? =
                Regex("_(A1|A2|B1|B2|C1|C2)").find(fileName)?.groupValues?.get(1)

            fun levelPass(fileName: String): Boolean {
                val lvl = levelOf(fileName)
                return (lvl == null) || visibleLevels.contains(lvl)
            }

            fun keepSlotsWithLevels(list: List<AccCard>): List<AccCard?> =
                list.map { card -> if (levelPass(card.fileName)) card else null }

            when {
                selectedCategory.equals("favourites", ignoreCase = true) -> favs.map { it }

                userCategories.any { it.name.equals(selectedCategory, ignoreCase = true) } -> {
                    val cat = userCategories.first { it.name.equals(selectedCategory, ignoreCase = true) }
                    val set = cat.cardFileNames.toSet()
                    val inCat = allCards.filter { set.contains(it.fileName) }
                    keepSlotsWithLevels(inCat)
                }

                selectedCategory.equals("custom", ignoreCase = true) -> {
                    val customs = loadCustomCards(context)
                    keepSlotsWithLevels(customs)
                }

                !selectedCategory.isNullOrBlank() -> {

                    var inCat = allCards.filter {
                        it.folder.startsWith(selectedCategory!!, ignoreCase = true)
                    }

                    selectedCategoryLetter?.let { letter ->
                        inCat = inCat.filter { card ->
                            card.label.startsWith(letter, ignoreCase = true)
                        }
                    }

                    keepSlotsWithLevels(inCat)
                }


                else -> {
                    val order = when (gridSize) {
                        "Small" -> smallGridOrder
                        "Large" ->  largeGridOrder
                        else -> gridOrder
                    }

                    val byBase = allCards.associateBy { it.fileName.substringBeforeLast('.').lowercase() }

                    val curated: List<AccCard?> = order.map { key ->
                        if (key == null) return@map null
                        val card = byBase[key.lowercase()]
                        if (card != null && levelPass(card.fileName)) card else null
                    }

                    curated
                }
            }
        }
    }
    var page by remember { mutableIntStateOf(0) }

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
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            TopWhiteBar(
                chosen = chosen,
                spacing = spacing,
                modifier = Modifier.fillMaxWidth().height(80.dp),
                imageLoader = imageLoader,
                painterCache = mutableMapOf(),
                onReorder = { from, to -> chosen.add(to, chosen.removeAt(from)) },
                onRemove = { idx -> chosen.removeAt(idx) },
                onClearAll = { chosen.clear() },
                onPlayStop = {
                    val sentence = chosen.joinToString(" ") { it.label }
                    if (sentence.isNotBlank()) speak(sentence)
                },
                onSpeakWord = { word -> speak(word) }
            )

            GreenBar(
                cards = suggestions,
                spacing = spacing,
                imageLoader = imageLoader,
                onSuggestionClick = { card -> if (chosen.size < 14) chosen.add(card) },
                page = page,
                pageCount = pageCount,
                onPrev = { if (page > 0) page-- },
                onNext = { if (page < pageCount - 1) page++ },
                modifier = Modifier.fillMaxWidth().height(90.dp)
            )

            AACBoardGrid(
                imageLoader = imageLoader,
                painterCache = mutableMapOf(),
                rows = rows,
                cols = cols,
                gridSize = gridSize,
                visibleCards = pageSlice,
                favs = favs,
                onCardClick = { card -> if (chosen.size < 14) chosen.add(card) },
                onCardLongPress = { card ->

                    when {

                        card.folder.equals("nouns", ignoreCase = true) -> {
                            selectedCard = card
                            popupType = "noun"
                            showPopup = true
                        }

                        card.folder.equals("verbs", ignoreCase = true) -> {
                            selectedCard = card
                            popupType = "verb"
                            showPopup = true
                        }

                        card.label.equals("will", ignoreCase = true) -> {
                            selectedCard = card
                            popupType = "verb"
                            showPopup = true
                        }

                        else -> if (chosen.size < 14) chosen.add(card)
                    }
                },
                modifier = Modifier.fillMaxWidth().weight(1f)
            )

            CategoryBar(
                imageLoader = imageLoader,
                onCategorySelected = {
                    selectedCategoryLetter = null
                    selectedCategory = it
                },
                onCategoryLongPress = { cat ->

                    // BLOCK long-press for these
                    val blocked = listOf(
                        "home", "favourites", "custom", "+ new"
                    )

                    val isUserCategory = userCategories.any { it.name.equals(cat, ignoreCase = true) }

                    if (cat.lowercase() in blocked || isUserCategory) {
                        return@CategoryBar
                    }

                    longPressedCategory = cat
                    showCategoryLetterPopup = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
            )
        }
    }

    if (showPopup && selectedCard != null && selectedCard!!.folder == "verbs") {
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
                    val baseVerb = selectedCard!!.label.lowercase()
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
                        add("Present" to forms.base)
                    }

                    Text(
                        text = "Choose tense for: ${selectedCard!!.label}",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.wrapContentWidth()
                    ) {
                        mainForms.forEach { (tenseName, label) ->
                            val imagePath = "file:///android_asset/tenses/${tenseName.lowercase()}.png"
                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .border(2.dp, Color(0xFFFFA500), RoundedCornerShape(8.dp))
                                    .background(Color(0xFFD1D1D1), RoundedCornerShape(8.dp))
                                    .clickable {
                                        chosen.add(selectedCard!!.copy(label = label))
                                        showPopup = false
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize().padding(4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    Image(
                                        painter = rememberAsyncImagePainter(Uri.parse(imagePath), imageLoader),
                                        contentDescription = tenseName,
                                        modifier = Modifier.weight(1f).fillMaxWidth()
                                    )
                                    Text(label, fontSize = 12.sp, color = Color.Black, textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }

                    if (presentVariants.isNotEmpty()) {
                        Text("Present Forms", style = MaterialTheme.typography.titleMedium)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.wrapContentWidth()
                        ) {
                            presentVariants.forEach { form ->
                                Box(
                                    modifier = Modifier
                                        .size(90.dp)
                                        .border(2.dp, Color(0xFF81C784), RoundedCornerShape(8.dp))
                                        .background(Color(0xFFD1D1D1), RoundedCornerShape(8.dp))
                                        .clickable {
                                            chosen.add(selectedCard!!.copy(label = form))
                                            showPopup = false
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(form, fontSize = 16.sp, color = Color.Black, textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }

                    if (forms.negatives.isNotEmpty()) {
                        Text("Negatives", style = MaterialTheme.typography.titleMedium)

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.wrapContentWidth()
                        ) {
                            forms.negatives.forEach { neg ->
                                val negPath = negativeIconFor(neg)
                                Box(
                                    modifier = Modifier
                                        .size(90.dp)
                                        .border(2.dp, Color.Red, RoundedCornerShape(8.dp))
                                        .background(Color(0xFFD1D1D1), RoundedCornerShape(8.dp))
                                        .clickable {
                                            chosen.add(selectedCard!!.copy(label = neg))
                                            showPopup = false
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize().padding(4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        Image(
                                            painter = rememberAsyncImagePainter(Uri.parse(negPath), imageLoader),
                                            contentDescription = "Negative",
                                            modifier = Modifier.weight(1f).fillMaxWidth()
                                        )
                                        Text(neg, fontSize = 12.sp, color = Color.Black, textAlign = TextAlign.Center)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .border(2.dp, Color.Red, RoundedCornerShape(8.dp))
                            .background(Color.LightGray, RoundedCornerShape(8.dp))
                            .clickable { showPopup = false },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(
                                    Uri.parse("file:///android_asset/tenses/negative_present.png"),
                                    imageLoader
                                ),
                                contentDescription = "Cancel",
                                modifier = Modifier.weight(1f).fillMaxWidth()
                            )
                            Text("Cancel", fontSize = 12.sp, color = Color.Red, textAlign = TextAlign.Center)
                        }
                    }
                }
            }        }
    }

    if (showPopup && selectedCard != null && popupType == "noun") {
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

                    val singular = selectedCard!!.label
                    val plural = suggestPlural(singular, irregularPluralJson)

                    Text(
                        text = "Choose form:",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        // Singular
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .border(2.dp, Color(0xFF81C784), RoundedCornerShape(10.dp))
                                .background(Color(0xFFE8F5E9), RoundedCornerShape(10.dp))
                                .clickable {
                                    chosen.add(selectedCard!!)
                                    showPopup = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Image(
                                    painter = rememberAsyncImagePainter(Uri.parse(selectedCard!!.path), imageLoader),
                                    contentDescription = "Singular",
                                    modifier = Modifier.size(50.dp)
                                )
                                Text(singular, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Plural
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .border(2.dp, Color(0xFFFFA726), RoundedCornerShape(10.dp))
                                .background(Color(0xFFFFF3E0), RoundedCornerShape(10.dp))
                                .clickable {
                                    chosen.add(selectedCard!!.copy(label = plural))
                                    showPopup = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Image(
                                    painter = rememberAsyncImagePainter(Uri.parse(selectedCard!!.path), imageLoader),
                                    contentDescription = "Plural",
                                    modifier = Modifier.size(50.dp)
                                )
                                Text(plural, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .border(2.dp, Color.Red, RoundedCornerShape(8.dp))
                            .background(Color.LightGray, RoundedCornerShape(8.dp))
                            .clickable { showPopup = false },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(
                                    Uri.parse("file:///android_asset/tenses/negative_present.png"),
                                    imageLoader
                                ),
                                contentDescription = "Cancel",
                                modifier = Modifier.weight(1f).fillMaxWidth()
                            )
                            Text("Cancel", fontSize = 12.sp, color = Color.Red, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
    }

    if (showCategoryLetterPopup && longPressedCategory != null) {
        Dialog(onDismissRequest = { showCategoryLetterPopup = false }) {

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .border(4.dp, Color.Black, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    Text(
                        text = "Filter by letter",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )

                    // All cards inside the long-pressed category
                    val cardsInCategory = allCards.filter {
                        it.folder.startsWith(longPressedCategory!!, ignoreCase = true)
                    }

                    // Letters that actually exist in that category
                    val availableLetters = cardsInCategory
                        .mapNotNull { it.label.firstOrNull()?.uppercaseChar() }
                        .toSet()

                    val letters = ('A'..'Z').toList()

                    for (row in letters.chunked(6)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Spacer(Modifier.weight(1f))

                            row.forEach { letter ->
                                val enabled = availableLetters.contains(letter)

                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .border(
                                            width = 3.dp,
                                            color = if (enabled) Color(0xFF81C784) else Color.LightGray,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .background(
                                            color = if (enabled) Color(0xFFE8F5E9) else Color(0xFFE0E0E0),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable(enabled = enabled) {
                                            selectedCategory = longPressedCategory
                                            selectedCategoryLetter = letter
                                            showCategoryLetterPopup = false
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = letter.toString(),
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (enabled) Color.Black else Color.Gray
                                    )
                                }
                            }

                            Spacer(Modifier.weight(1f))
                        }
                    }

                    Text(
                        text = "Show all",
                        color = Color.Blue,
                        modifier = Modifier.clickable {
                            selectedCategoryLetter = null
                            selectedCategory = longPressedCategory
                            showCategoryLetterPopup = false
                        }
                    )

                    Text(
                        text = "Cancel",
                        color = Color.Red,
                        modifier = Modifier.clickable {
                            showCategoryLetterPopup = false
                        }
                    )
                }
            }
        }
    }
}