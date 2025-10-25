package com.example.taptalk
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.Options
import okio.buffer
import okio.source
import org.burnoutcrew.reorderable.*
import java.util.Locale
import android.speech.tts.TextToSpeech
import com.google.mlkit.nl.smartreply.*
import com.google.mlkit.nl.smartreply.SmartReplyGenerator
import androidx.compose.ui.window.Dialog
import org.json.JSONObject
import java.nio.charset.Charset
import coil.request.ImageRequest
import androidx.compose.ui.graphics.painter.Painter

class AssetUriFetcher(
    private val context: Context,
    private val uri: Uri
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        val assetPath = uri.toString().removePrefix("file:///android_asset/")
        val input = context.assets.open(assetPath)
        BitmapFactory.decodeStream(context.assets.open(assetPath))
        return SourceResult(
            source = ImageSource(input.source().buffer(), context),
            mimeType = "image/png",
            dataSource = DataSource.DISK
        )
    }

    class Factory(private val context: Context) : Fetcher.Factory<Uri> {
        override fun create(
            data: Uri,
            options: Options,
            imageLoader: ImageLoader
        ): Fetcher? {
            return if (data.toString().startsWith("file:///android_asset/")) {
                AssetUriFetcher(context, data)
            } else null
        }
    }
}

// ---------------- DATA ----------------
data class AccCard(
    val fileName: String,
    val label: String,
    val path: String,
    val folder: String
)

data class VerbForms(
    val base: String,
    val past: String,
    val perfect: String,
    val negatives: List<String> = emptyList()
)

fun loadJsonAsset(context: Context, fileName: String): JSONObject {
    val input = context.assets.open(fileName)
    val json = input.bufferedReader(Charset.forName("UTF-8")).use { it.readText() }
    return JSONObject(json)
}

private fun negativeIconFor(neg: String): String {
    val n = neg.lowercase()
    val base = "file:///android_asset/tenses/"

    return when {
        // future
        "won't" in n || "will not" in n -> base + "negative_future.png"

        // perfect
        "haven" in n || "hasn" in n || "hadn" in n -> base + "negative_perfect.png"

        // past
        "didn" in n || "wasn" in n || "weren" in n -> base + "negative_past.png"

        // present
        "don'" in n || "doesn" in n || "isn" in n || "aren" in n -> base + "negative_present.png"

        else -> base + "negative_present.png"
    }
}

fun getVerbForms(verb: String, json: JSONObject): VerbForms {
    val v = verb.lowercase()

    return if (json.has(v)) {
        val entry = json.getJSONObject(v)
        val base = v
        val past = entry.optString("past", base + "ed")
        val perfect = entry.optString("perfect", past)
        val negativesJson = entry.optJSONArray("negatives")
        val negatives = mutableListOf<String>()
        negativesJson?.let { arr ->
            for (i in 0 until arr.length()) negatives.add(arr.getString(i))
        }
        VerbForms(base, past, perfect, negatives)
    } else {
        VerbForms(v, v + "ed", v + "ed")
    }
}

fun borderColorFor(folder: String): Color {
    val f = folder.lowercase()
    return when {
        "adjective" in f   -> Color(0xFFADD8E6)
        "conjunction" in f -> Color(0xFFD3D3D3)
        "negation" in f    -> Color(0xFFFF6B6B)
        "noun" in f        -> Color(0xFFFFB347)
        "preposition" in f -> Color(0xFFFFC0CB)
        "pronoun" in f     -> Color(0xFFFFF176)
        "question" in f    -> Color(0xFFB39DDB)
        "social" in f      -> Color(0xFFFFC0CB)
        "verb" in f        -> Color(0xFF81C784)
        "determiner" in f  -> Color(0xFF90A4AE)
        else               -> Color.Black
    }
}


val gridOrder: List<String?> = listOf(
    "what_A1","I_A1","we_A1","have_A1","know_A1","child_A1","place_A1","time_A1","hand_A1","good_A1","bad_A1",
    "how_A1","you_A1","they_A1","come_A1","make_A1","man_A1","world_A1","year_A1","thing_A1","thirsty_A2","hungry_A1",
    "why_A1","he_A1","be_A1","do_A1","say_A1","woman_A1","house_A1","week_A1","bathroom_A1","happy_A1","sad_A1",
    "where_A1","she_A1","yes_A1","get_A1","see_A1","person_A1","school_A1","life_A1","food_A1","tired_A1","calm_B1",
    "who_A1","it_A1","no_A1","go_B1","create_A1","friend_A1","job_A1","family_A1","water_A1","and_A1","or_A1",
    "when_A1","a_A1","an_A1","the_A1","hello_A1","good_morning_A1","bye_A1","thanks_A1","please_A1","to_A1","of_A1",
    null,null,null,null,null,null,null,null,null,null,null
)

fun loadAccCards(context: Context): List<AccCard> {
    val assetManager = context.assets

    fun walk(path: String, topCategory: String? = null): List<AccCard> {
        val files = assetManager.list(path) ?: return emptyList()

        return files.flatMap { name ->
            val fullPath = if (path.isEmpty()) name else "$path/$name"
            val children = assetManager.list(fullPath) ?: emptyArray()

            if (children.isNotEmpty()) {
                val cat = topCategory ?: name
                walk(fullPath, cat)
            }
            else if (name.endsWith(".png") || name.endsWith(".jpg")) {
                val category = topCategory ?: path.substringAfterLast("/")
                listOf(
                    AccCard(
                        fileName = name,
                        label = normalizeFileName(name),
                        path = "file:///android_asset/$fullPath",
                        folder = category.lowercase()
                    )
                )
            }
            else emptyList()
        }
    }

    return walk("ACC_board") + walk("categories")
}

// ---------- Category buttons ---------------
fun trimCategoryName(fileName: String): String {
    var name = fileName.substringBeforeLast('.')
    name = name.replace("_cathegory","",true)
        .replace("_category","",true)
        .replace("_"," ")
        .trim()
    return name.replaceFirstChar { it.uppercase() }
}

fun loadCategories(context: Context): List<AccCard> {
    val files = context.assets.list("ACC_board/categories") ?: return emptyList()
    return files.filter { it.endsWith(".png") }.map { file ->
        AccCard(
            fileName = file,
            label = trimCategoryName(file),
            path = "file:///android_asset/ACC_board/categories/$file",
            folder = "categories"
        )
    }
}

fun normalizeFileName(file: String): String {
    var name = file.substringBeforeLast('.')
    name = name.replace(Regex("_(A1|A2|B1|B2|C1|C2|EN|PL|DE|FR|ES)\\b"), "")
    name = name.replace(Regex("^[0-9]+[_-]?"), "")
    name = name.replace("_", " ").replace("-", " ")
    name = name.replace("\\s+".toRegex(), " ").trim()
    return name.split(" ").joinToString(" ") { word ->
        val lower = word.lowercase(Locale.getDefault())
        if (lower in listOf("a","an","the","and","or","of","to","in","on","at","for")) lower
        else lower.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }.replaceFirstChar { it.titlecase(Locale.getDefault()) }

}

// ---------------- UTIL: AUTO PLURAL GENERATOR ----------------
fun suggestPlural(noun: String, json: JSONObject): String {
    val lower = noun.lowercase()
    if (json.has(lower)) return json.getString(lower)

    return when {
        lower.endsWith("y") && lower.length > 1 &&
                !"aeiou".contains(lower[lower.length - 2]) ->
            lower.dropLast(1) + "ies"

        lower.endsWith("s") || lower.endsWith("x") || lower.endsWith("z")
                || lower.endsWith("ch") || lower.endsWith("sh") ->
            lower + "es"

        else -> lower + "s"
    }
}

// ---------------- ACTIVITY ----------------
class AccActivity : ComponentActivity() {
    private var tts: TextToSpeech? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val smartReply = SmartReply.getClient()

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.setLanguage(Locale.US)
            }
        }

        val imageLoader = ImageLoader.Builder(this)
            .components { add(AssetUriFetcher.Factory(this@AccActivity)) }
            .crossfade(true)
            .respectCacheHeaders(false)
            .allowHardware(true)
            .build()

        setContent {
            MaterialTheme {
                AccScreen(imageLoader, smartReply) { speakOut(it) }
            }
        }
    }

    private fun speakOut(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ACC_UTTERANCE")
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}

// ---------------- UI ----------------
@Composable
fun AccScreen(
    imageLoader: ImageLoader,
    smartReply: SmartReplyGenerator,
    spacing: Dp = 6.dp,
    speak: (String) -> Unit
) {

    var selectedCategory by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current

    val irregularVerbJson = remember {
        loadJsonAsset(context, "irregular_verbs.json")
    }

    val irregularPluralJson = remember {
        loadJsonAsset(context, "irregular_nouns.json")
    }


    var showPopup by remember { mutableStateOf(false) }
    var selectedCard by remember { mutableStateOf<AccCard?>(null) }

    val chosen = remember { mutableStateListOf<AccCard>() }
    var suggestions by remember { mutableStateOf<List<AccCard>>(emptyList()) }

    val accCards = remember { loadAccCards(context) }
    val accDict = remember { accCards.associateBy { it.label.lowercase() } }

    val painterCache = remember { mutableStateMapOf<String, Painter>() }

    LaunchedEffect(chosen.toList()) {
        val sentence = chosen.joinToString(" ") { it.label }
        if (sentence.isNotBlank()) {
            val conversation = ArrayList<TextMessage>()
            conversation.add(
                TextMessage.createForLocalUser(sentence, System.currentTimeMillis())
            )

            smartReply.suggestReplies(conversation)
                .addOnSuccessListener { result ->
                    if (result.status == SmartReplySuggestionResult.STATUS_SUCCESS) {
                        suggestions = result.suggestions
                            .mapNotNull { accDict[it.text.lowercase()] }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("ACC_DEBUG", "SmartReply failed", e)
                }
        } else {
            suggestions = emptyList()
        }
    }

    // ---- Layout ----
    Scaffold(bottomBar = { BottomNavBar() }) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // --- Top sentence bar ---
            TopWhiteBar(
                chosen = chosen,
                spacing = spacing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                imageLoader = imageLoader,
                painterCache = painterCache,
                onReorder = { from, to -> chosen.add(to, chosen.removeAt(from)) },
                onRemove = { idx -> chosen.removeAt(idx) },
                onClearAll = { chosen.clear() },
                onPlayStop = {
                    val sentence = chosen.joinToString(" ") { it.label }
                    if (sentence.isNotBlank()) speak(sentence)
                },
                onSpeakWord = { word -> speak(word) }
            )

            // --- Smart suggestions ---
            GreenBar(
                cards = suggestions,
                spacing = spacing,
                imageLoader = imageLoader,
                onSuggestionClick = { card ->
                    if (chosen.size < 14) chosen.add(card)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
            )

            // --- Grid of cards ---
            AACBoardGrid(
                imageLoader = imageLoader,
                painterCache = painterCache,
                selectedCategory = selectedCategory,
                onCardClick = { card ->
                    if (chosen.size < 14) {
                        chosen.add(card)
                    }
                },
                onCardLongPress = { card ->
                    when {
                        card.folder.contains("noun", ignoreCase = true) -> {
                            val pluralLabel = suggestPlural(card.label, irregularPluralJson)
                            if (chosen.size < 14) {
                                chosen.add(card.copy(label = pluralLabel))
                            }
                        }

                        card.folder == "verbs" -> {
                            selectedCard = card
                            showPopup = true
                        }

                        else -> {
                            if (chosen.size < 14) chosen.add(card)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )


            // --- Categories ---
            CategoryBar(
                imageLoader = imageLoader,
                onCategorySelected = { selectedCategory = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
            )

        }
    }

// ---- Popup Dialog ----
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
                                    Uri.parse("file:///android_asset/tenses/negative_present.png"), // or your cancel icon
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
}

@SuppressLint("RememberReturnType")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AACBoardGrid(
    imageLoader: ImageLoader,
    painterCache: Map<String, Painter>,
    selectedCategory: String?,
    onCardClick: (AccCard) -> Unit,
    onCardLongPress: (AccCard) -> Unit,
    modifier: Modifier = Modifier
)
{
    val context = LocalContext.current
    val allCards = remember { loadAccCards(context) }

    val rows = 6
    val cols = 11
    val perPage = rows * cols

    val visibleCardsAll by remember(selectedCategory) {
        mutableStateOf(
            if (!selectedCategory.isNullOrBlank()) {
                allCards.filter {
                    it.folder.equals(selectedCategory, ignoreCase = true) ||
                            it.folder.equals(selectedCategory + "s", ignoreCase = true)
                }
            } else {
                val cardsMap = allCards.associateBy { it.fileName.substringBeforeLast('.').lowercase() }
                gridOrder.mapNotNull { key -> key?.let { cardsMap[it.lowercase()] } }
            }
        )
    }

    var page by remember { mutableStateOf(0) }
    val pageCount = (visibleCardsAll.size + perPage - 1) / perPage
    val visibleCards = visibleCardsAll.drop(page * perPage).take(perPage)

    Column(
        modifier = modifier.padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // ---- GRID ----
        for (row in 0 until rows) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (col in 0 until cols) {
                    val idx = row * cols + col
                    val card = visibleCards.getOrNull(idx)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .border(
                                2.dp,
                                borderColorFor(card?.folder ?: ""),
                                RoundedCornerShape(4.dp)
                            )
                            .background(Color(0xFFD9D9D9), RoundedCornerShape(4.dp))
                            .combinedClickable(
                                enabled = card != null,
                                onClick = { card?.let(onCardClick) },
                                onLongClick = { card?.let(onCardLongPress) }
                            )
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (card != null) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {

                                val context = LocalContext.current

                                val painter = rememberAsyncImagePainter(
                                    model = ImageRequest.Builder(context)
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


                                Text(
                                    text = card.label,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }

        // ---- PAGE NAVIGATION ----
        if (pageCount > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { if (page > 0) page-- },
                    enabled = page > 0
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Prev page",
                        tint = if (page > 0) Color.Black else Color.Gray
                    )
                }

                Text(
                    text = "Page ${page + 1} / $pageCount",
                    color = Color.Black,
                    fontSize = 16.sp
                )

                IconButton(
                    onClick = { if (page < pageCount - 1) page++ },
                    enabled = page < pageCount - 1
                ) {
                    Icon(
                        Icons.Default.ArrowForward,
                        contentDescription = "Next page",
                        tint = if (page < pageCount - 1) Color.Black else Color.Gray
                    )
                }
            }
        }
    }
}


// --------------- CATEGORY BAR ---------------
@Composable
fun CategoryBar(
    imageLoader: ImageLoader,
    onCategorySelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val categories = remember { loadCategories(context) }

    val visibleCount = 9
    val maxStart = (categories.size - visibleCount).coerceAtLeast(0)
    var startIndex by remember { mutableStateOf(0) }

    val endIndex = (startIndex + visibleCount).coerceAtMost(categories.size)
    val visibleCats = categories.subList(startIndex, endIndex)

    Row(
        modifier = modifier
            .background(Color(0xFFEFEFEF))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until 11) {
            when (i) {
                0 -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .border(2.dp, Color.Black, RoundedCornerShape(4.dp))
                            .background(Color.LightGray, RoundedCornerShape(4.dp))
                            .clickable {
                                startIndex = (startIndex - visibleCount).coerceAtLeast(0)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Prev", tint = Color.Black)
                    }
                }
                10 -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .border(2.dp, Color.Black, RoundedCornerShape(4.dp))
                            .background(Color.LightGray, RoundedCornerShape(4.dp))
                            .clickable {
                                startIndex = (startIndex + visibleCount).coerceAtMost(maxStart)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "Next", tint = Color.Black)
                    }
                }
                else -> {
                    val catIndex = i - 1
                    if (catIndex < visibleCats.size) {
                        val cat = visibleCats[catIndex]
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .border(2.dp, Color.Black, RoundedCornerShape(4.dp))
                                .background(Color(0xFFD9D9D9), RoundedCornerShape(4.dp))
                                .clickable { onCategorySelected(cat.label.lowercase()) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(2.dp),
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
                }
            }
        }
    }
}

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

// ---------------- GREEN BAR ----------------
@Composable
fun GreenBar(
    cards: List<AccCard>,
    spacing: Dp,
    modifier: Modifier = Modifier,
    imageLoader: ImageLoader,
    onSuggestionClick: (AccCard) -> Unit
) {
    Row(
        modifier = modifier
            .background(Color(0xFFDFF0D8))
            .fillMaxWidth()
            .padding(vertical = spacing),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
            cards.forEach { card ->
                Box(
                    modifier = Modifier
                        .height(70.dp)
                        .width(80.dp)
                        .border(2.dp, borderColorFor(card.folder), RoundedCornerShape(4.dp))
                        .background(Color(0xFFD9D9D9), RoundedCornerShape(4.dp))
                        .clickable { onSuggestionClick(card) },
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
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        )
                        Text(
                            text = card.label,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

// ---------------- TILE ----------------
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AccTile(text: String, borderColor: Color, modifier: Modifier = Modifier, onClick: () -> Unit, onLongClick: () -> Unit) {
    Box(
        modifier = modifier
            .border(2.dp, borderColor, RoundedCornerShape(4.dp))
            .clip(RoundedCornerShape(4.dp))
            .background(Color.LightGray)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        contentAlignment = Alignment.Center
    ) { Text(text, fontSize = 12.sp) }
}

@Composable
fun BottomNavBar() {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(Color(0xFFDFF0D8))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        IconButton(onClick = {
            context.startActivity(
                android.content.Intent(context, FastSettingsActivity::class.java)
            )
        }) {
            Icon(Icons.Default.Tune, contentDescription = "Quick Settings")
        }

        IconButton(onClick = {
            context.startActivity(
                android.content.Intent(context, KeyboardActivity::class.java)
            )
        }) {
            Icon(Icons.Default.Keyboard, contentDescription = "Keyboard")
        }

        IconButton(onClick = {
            context.startActivity(
                android.content.Intent(context, AccActivity::class.java)
            )
        }) {
            Icon(Icons.Default.Home, contentDescription = "Home")
        }
        IconButton(onClick = {
            context.startActivity(
                android.content.Intent(context, CreateCardActivity::class.java)
            )
        }) {
            Icon(Icons.Default.Add, contentDescription = "Create")
        }
        IconButton(onClick = {
            context.startActivity(
                android.content.Intent(context, SettingsActivity::class.java)
            ) }) {
            Icon(Icons.Default.Settings, contentDescription = "Settings")
        }
    }
}
