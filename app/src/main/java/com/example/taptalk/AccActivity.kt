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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.lifecycleScope
import com.example.taptalk.data.HistoryRepository
import kotlinx.coroutines.launch
import java.io.File
import androidx.room.Room
import coil.imageLoader
import com.example.taptalk.data.AppDatabase
import com.example.taptalk.data.FastSettingsEntity
import com.example.taptalk.ui.theme.TapTalkTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

val TTS_VOICE_MAP = mapOf(
    "Kate" to listOf("female", "en-us-x-sfg", "en-gb-x-fis", "f1"),
    "Josh" to listOf("male", "en-us-x-tpd", "en-gb-x-rjs", "m1"),
    "Sabrina" to listOf("female", "child", "en-us-x-sfg#female_2", "en-in-x-cxx"),
    "Sami" to listOf("male", "child", "en-us-x-tpd#male_2", "en-in-x-ism")
)

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

private fun pickTtsVoice(
    tts: TextToSpeech?,
    friendlyName: String
): android.speech.tts.Voice? {
    val keywords = TTS_VOICE_MAP[friendlyName] ?: return null
    val allVoices = tts?.voices ?: return null

    return allVoices.firstOrNull { voice ->
        keywords.any { key -> voice.name.contains(key, ignoreCase = true) }
    }
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

// ---------- FAVOURITES FIREBASE ----------
fun toggleFavouriteInFirebase(context: Context, card: AccCard, userId: String?, onDone: (Boolean) -> Unit = {}) {
    if (userId == null) return

    val firestore = FirebaseFirestore.getInstance()
    val favRef = firestore.collection("USERS").document(userId).collection("Favourites")

    favRef.document(card.label).get()
        .addOnSuccessListener { doc ->
            if (doc.exists()) {
                // already favourite
                favRef.document(card.label).delete()
                onDone(false)
            } else {
                // not favourite
                val data = mapOf(
                    "label" to card.label,
                    "path" to card.path,
                    "folder" to card.folder,
                    "fileName" to card.fileName,
                    "timestamp" to com.google.firebase.Timestamp.now()
                )
                favRef.document(card.label).set(data)
                onDone(true)
            }
        }
}


fun getVerbForms(verb: String, json: JSONObject): VerbForms {
    specialVerbForms(verb)?.let { return it }

    val v = verb.lowercase()
    if (json.has(v)) {
        val entry = json.getJSONObject(v)
        val base = v
        val past = entry.optString("past", base + "ed")
        val perfect = entry.optString("perfect", past)
        val negativesJson = entry.optJSONArray("negatives")
        val negatives = mutableListOf<String>()
        negativesJson?.let { arr ->
            for (i in 0 until arr.length()) negatives.add(arr.getString(i))
        }
        return VerbForms(base, past, perfect, negatives)
    }

    return VerbForms(v, v + "ed", v + "ed")
}


fun borderColorFor(folder: String): Color {
    val f = folder.lowercase()
    return when {
        "adjective" in f   -> Color(0xFFADD8E6)
        "conjunction" in f -> Color(0xFFD3D3D3)
        "emergency" in f    -> Color(0xFFFF6B6B)
        "negation" in f    -> Color(0xFFFF6B6B)
        "noun" in f        -> Color(0xFFFFB347)
        "preposition" in f -> Color(0xFFFFC0CB)
        "pronoun" in f     -> Color(0xFFFFF176)
        "question" in f    -> Color(0xFFB39DDB)
        "social" in f      -> Color(0xFFFFC0CB)
        "verbs" in f        -> Color(0xFF81C784)
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

val smallGridOrder: List<String?> = listOf(
    "what_A1","I_A1","they_A1","come_A1","child_A1","place_A1","hand_A1", "good_A1","bad_A1",
    "how_A1","you_A1","be_A1","do_A1","man_A1","house_A1","bathroom_A1","thirsty_A2","hungry_A1",
    "why_A1","he_A1","have_A1","get_A1","woman_A1","school_A1","food_A1","happy_A1","sad_A1",
    "where_A1","she_A1","yes_A1","go_B1","friend_A1","job_A1","water_A1","tired_A1","calm_B1",
    "who_A1","we_A1","no_A1","hello_A1","good_morning_A1","bye_A1","thanks_A1","please_A1","to_A1",
    null,null,null,null,null,null,null,null,null
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

fun loadFavourites(context: Context, userId: String?, onLoaded: (List<AccCard>) -> Unit) {
    if (userId == null) {
        onLoaded(emptyList())
        return
    }

    val firestore = FirebaseFirestore.getInstance()
    firestore.collection("USERS").document(userId).collection("Favourites")
        .get()
        .addOnSuccessListener { snapshot ->
            val favs = snapshot.documents.mapNotNull { doc ->
                val label = doc.getString("label") ?: return@mapNotNull null
                val path = doc.getString("path") ?: return@mapNotNull null
                val folder = doc.getString("folder") ?: "favourites"
                val fileName = doc.getString("fileName") ?: "$label.png"
                AccCard(fileName, label, path, folder)
            }
            onLoaded(favs)
        }
}


fun loadCustomCards(context: Context): List<AccCard> {
    val rootDir = File(context.filesDir, "Custom_Words")
    if (!rootDir.exists()) return emptyList()

    val customCards = mutableListOf<AccCard>()

    rootDir.walkTopDown().forEach { file ->
        if (file.isFile && (file.extension == "jpg" || file.extension == "png")) {
            val folderName = file.parentFile?.name ?: "custom"
            val label = file.nameWithoutExtension
            customCards.add(
                AccCard(
                    fileName = file.name,
                    label = label.replaceFirstChar { it.uppercase() },
                    path = file.absolutePath,
                    folder = folderName
                )
            )
        }
    }

    return customCards
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

    val desiredOrder = listOf(
        "home_category.png",
        "favourites_category.png",
        "custom_category.png",
        "emergency_category.png",
        "social_category.png",
        "questions_category.png",
        "pronouns_category.png",
        "verbs_category.png",
        "adjective_category.png",
        "adverb_category.png",
        "prepositions_category.png",
        "conjuction_category.png",
        "determiners_category.png",
        "negation_category.png"
    )

    val sortedFiles = desiredOrder.mapNotNull { orderName ->
        files.find { it.equals(orderName, ignoreCase = true) }
    }

    return sortedFiles.map { file ->
        AccCard(
            fileName = file,
            label = trimCategoryName(file),
            path = "file:///android_asset/ACC_board/categories/$file",
            folder = "categories"
        )
    }
}

fun specialVerbForms(verb: String): VerbForms? {
    return when (verb.lowercase()) {
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
    private val baseConversation = mutableListOf<TextMessage>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var darkMode: Boolean = false
        var lowVisionMode: Boolean = false

        val smartReply = SmartReply.getClient()
        val historyRepo = HistoryRepository(this)
        val allCards = loadAccCards(this) + loadCustomCards(this)

        val baseConversation = mutableListOf<TextMessage>()
        lifecycleScope.launch {
            val history = historyRepo.getRecentSentences()
            history.forEach {
                baseConversation.add(TextMessage.createForLocalUser(it.sentence, it.timestamp))
            }
        }
        allCards.forEach { card ->
            baseConversation.add(TextMessage.createForLocalUser(card.label, System.currentTimeMillis()))
        }

        // Text-to-Speech
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US

                lifecycleScope.launch {
                    val db = Room.databaseBuilder(
                        this@AccActivity,
                        AppDatabase::class.java,
                        "tap_talk_db"
                    ).build()
                    val fastDao = db.fastSettingsDao()

                    val firestore = FirebaseFirestore.getInstance()
                    val userId = FirebaseAuth.getInstance().currentUser?.uid

                    // --- Load settings ---
                    var settings = fastDao.getSettings()
                    if (settings == null || !settings.isSynced) {
                        val snapshot = firestore.collection("USERS")
                            .document(userId ?: return@launch)
                            .collection("Fast_Settings")
                            .document("current")
                            .get()
                            .await()

                        settings = if (snapshot.exists()) {
                            FastSettingsEntity(
                                volume = (snapshot.getDouble("volume") ?: 50.0).toFloat(),
                                selectedVoice = snapshot.getString("selectedVoice") ?: "Kate",
                                aiSupport = snapshot.getBoolean("aiSupport") ?: true,
                                isSynced = true
                            )
                        } else {
                            FastSettingsEntity(
                                volume = 50f,
                                selectedVoice = "Kate",
                                aiSupport = true,
                                isSynced = true
                            )
                        }
                        fastDao.insertOrUpdate(settings)
                    }

                    kotlinx.coroutines.delay(500)

                    val fs = FirebaseFirestore.getInstance()
                    val uid = FirebaseAuth.getInstance().currentUser?.uid

                    if (uid != null) {
                        lifecycleScope.launch {
                            try {
                                val snap = firestore.collection("USERS")
                                    .document(uid)
                                    .collection("Fast_Settings")
                                    .document("current")
                                    .get()
                                    .await()

                                darkMode = snap.getBoolean("darkMode") ?: false
                                lowVisionMode = snap.getBoolean("lowVisionMode") ?: false
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }

                            setContent {
                                TapTalkTheme(
                                    darkMode = darkMode,
                                    lowVision = lowVisionMode
                                ) {
                                    AccScreen(
                                        imageLoader = imageLoader,
                                        smartReply = smartReply,
                                        baseConversation = baseConversation
                                    ) { speakOut(it) }
                                }
                            }
                        }
                    } else {
                        setContent {
                            TapTalkTheme {
                                AccScreen(
                                    imageLoader = imageLoader,
                                    smartReply = smartReply,
                                    baseConversation = baseConversation
                                ) { speakOut(it) }
                            }
                        }
                    }

                    var fireVoiceName: String? = null
                    var fireSpeed: Float? = null
                    var firePitch: Float? = null

                    if (uid != null) {
                        val snap = fs.collection("USERS").document(uid)
                            .collection("Fast_Settings").document("current").get().await()
                        if (snap.exists()) {
                            fireVoiceName = snap.getString("voiceName")
                            fireSpeed = (snap.getDouble("voiceSpeed") ?: Double.NaN)
                                .let { if (it.isNaN()) null else it.toFloat() }
                            firePitch = (snap.getDouble("voicePitch") ?: Double.NaN)
                                .let { if (it.isNaN()) null else it.toFloat() }
                        }
                    }

                    val local = VoicePrefs.read(this@AccActivity)
                    val voiceName = fireVoiceName ?: local.name
                    val speechRate = fireSpeed ?: local.speed ?: 1.0f
                    val pitch = firePitch ?: local.pitch ?: 1.0f

                    val voices = tts?.voices?.toList().orEmpty()
                    val exact = voices.firstOrNull { it.name == voiceName }

                    val finalVoice = exact ?: run {
                        val fallback = pickTtsVoice(tts, settings.selectedVoice)
                        fallback ?: voices.firstOrNull { it.locale?.language == "en" } ?: voices.firstOrNull()
                    }

                    if (finalVoice != null) {
                        tts?.voice = finalVoice
                        Log.d("ACC_TTS", "Using voice ${finalVoice.name} (speed=$speechRate, pitch=$pitch)")
                    } else {
                        Log.w("ACC_TTS", " No matching voice found, using engine default")
                    }

                    tts?.setSpeechRate(speechRate)
                    tts?.setPitch(pitch)
                }
            }
        }

        val imageLoader = coil.ImageLoader.Builder(this)
            .components { add(AssetUriFetcher.Factory(this@AccActivity)) }
            .crossfade(true)
            .respectCacheHeaders(false)
            .allowHardware(true)
            .build()

        setContent {
            TapTalkTheme(
                darkMode = darkMode,
                lowVision = lowVisionMode
            ) {
                AccScreen(
                    imageLoader = imageLoader,
                    smartReply = smartReply,
                    baseConversation = baseConversation
                ) { speakOut(it) }
            }

        }

        lifecycleScope.launch {
            HistoryRepository(this@AccActivity).syncToFirebase()
        }
    }


    private fun speakOut(text: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val firestore = FirebaseFirestore.getInstance()

        lifecycleScope.launch {
            var autoSpeak = true
            var smartReplyEnabled = true

            try {
                val snap = firestore.collection("USERS")
                    .document(userId ?: return@launch)
                    .collection("Fast_Settings")
                    .document("current")
                    .get()
                    .await()

                if (snap.exists()) {
                    autoSpeak = snap.getBoolean("autoSpeak") ?: true
                    smartReplyEnabled = snap.getBoolean("aiSupport") ?: true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // speak only if autoSpeak is ON
            if (autoSpeak) {
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ACC_UTTERANCE")
            }

            val repo = HistoryRepository(this@AccActivity)
            repo.saveSentenceOffline(text)
            repo.syncToFirebase()

            // add to SmartReply base conversation only if aiSupport is ON
            if (smartReplyEnabled) {
                baseConversation.add(TextMessage.createForLocalUser(text, System.currentTimeMillis()))
            }
        }
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
    baseConversation: List<TextMessage>,
    spacing: Dp = 6.dp,
    speak: (String) -> Unit
) {
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    var autoSpeak by remember { mutableStateOf(true) }
    var smartReplyEnabled by remember { mutableStateOf(true) }

    val irregularVerbJson = remember { loadJsonAsset(context, "irregular_verbs.json") }
    val irregularPluralJson = remember { loadJsonAsset(context, "irregular_nouns.json") }

    var showPopup by remember { mutableStateOf(false) }
    var selectedCard by remember { mutableStateOf<AccCard?>(null) }
    val chosen = remember { mutableStateListOf<AccCard>() }
    var suggestions by remember { mutableStateOf<List<AccCard>>(emptyList()) }

    var accCards by remember { mutableStateOf<List<AccCard>>(emptyList()) }
    LaunchedEffect(Unit) {
        accCards = withContext(kotlinx.coroutines.Dispatchers.IO) { loadAccCards(context) }
    }
    val accDict = remember(accCards) { accCards.associateBy { it.label.lowercase() } }


    LaunchedEffect(chosen.toList(), smartReplyEnabled) {
        val sentence = chosen.joinToString(" ") { it.label }

        if (sentence.isNotBlank()) {
            if (smartReplyEnabled) {
                val conversation = ArrayList<TextMessage>(baseConversation)
                conversation.add(TextMessage.createForLocalUser(sentence, System.currentTimeMillis()))
                val sorted = conversation.sortedBy { it.timestampMillis }

                smartReply.suggestReplies(sorted)
                    .addOnSuccessListener { result ->
                        if (result.status == SmartReplySuggestionResult.STATUS_SUCCESS) {
                            suggestions = result.suggestions.mapNotNull { accDict[it.text.lowercase()] }
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
        } else {
            suggestions = emptyList()
        }
    }

    var gridSize by remember { mutableStateOf("Medium") }
    LaunchedEffect(Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            val snap = FirebaseFirestore.getInstance()
                .collection("USERS").document(uid)
                .collection("Fast_Settings").document("current")
                .get()
                .await()

            autoSpeak = snap.getBoolean("autoSpeak") ?: true
            smartReplyEnabled = snap.getBoolean("aiSupport") ?: true

            val savedGrid = snap.getString("gridSize") ?: "Medium"
            gridSize = savedGrid
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
    val allCards = remember { builtInCards + customCards }

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

    val visibleCardsAll by remember(selectedCategory, allCards, favs.toList(), gridSize, visibleLevels) {
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
                selectedCategory.equals("favourites", ignoreCase = true) -> favs.map { it as AccCard? }

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

    // ===== UI =====
    Scaffold(bottomBar = { BottomNavBar() }) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            TopWhiteBar(
                chosen = chosen,
                spacing = spacing,
                modifier = Modifier.fillMaxWidth().height(80.dp),
                imageLoader = imageLoader,
                painterCache = mutableMapOf(), // not used here
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
                        card.folder.contains("noun", ignoreCase = true) -> {
                            val plural = suggestPlural(card.label, irregularPluralJson)
                            if (chosen.size < 14) chosen.add(card.copy(label = plural))
                        }

                        card.folder == "verbs" -> {
                            selectedCard = card
                            showPopup = true
                        }

                        card.label.equals("will", ignoreCase = true) -> {
                            selectedCard = card
                            showPopup = true
                        }

                        else -> if (chosen.size < 14) chosen.add(card)
                    }
                },
                modifier = Modifier.fillMaxWidth().weight(1f)
            )

            CategoryBar(
                imageLoader = imageLoader,
                onCategorySelected = { selectedCategory = it },
                modifier = Modifier.fillMaxWidth().height(90.dp)
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
}


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
                            val painter = rememberAsyncImagePainter(
                                model = ImageRequest.Builder(context)
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
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ← back button
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
            Icon(Icons.Default.ArrowBack, contentDescription = "Prev", tint = Color.Black)
        }

        // actual category tiles
        visibleCats.forEach { cat ->
            val label = cat.label.lowercase()
            val borderColor = when {
                label.contains("home") || label.contains("favourites") || label.contains("custom") -> Color.Black
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
                        if (label == "home") onCategorySelected(null)
                        else onCategorySelected(label)
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

        // → next button
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
            .background(Color(0xFFDFF0D8))
            .fillMaxWidth()
            .padding(horizontal = spacing, vertical = spacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategorySquareButton(
            icon = Icons.Default.ArrowBack,
            contentDesc = "Previous page",
            enabled = canGoPrev,
            onClick = { if (canGoPrev) onPrev() }
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                                modifier = Modifier.weight(1f).fillMaxWidth()
                            )
                            Text(card.label, fontSize = 12.sp, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }

        CategorySquareButton(
            icon = Icons.Default.ArrowForward,
            contentDesc = "Next page",
            enabled = canGoNext,
            onClick = { if (canGoNext) onNext() }
        )
    }
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
