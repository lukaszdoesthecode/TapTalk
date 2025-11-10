package com.example.taptalk
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.graphics.Color
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.Options
import okio.buffer
import okio.source
import java.util.Locale
import android.speech.tts.TextToSpeech
import com.google.mlkit.nl.smartreply.*
import org.json.JSONObject
import androidx.lifecycle.lifecycleScope
import com.example.taptalk.data.HistoryRepository
import kotlinx.coroutines.launch
import java.io.File
import androidx.room.Room
import coil.imageLoader
import com.example.taptalk.aac.data.AccCard
import com.example.taptalk.aac.data.VerbForms
import com.example.taptalk.aac.data.loadAccCards
import com.example.taptalk.aac.data.loadCustomCards
import com.example.taptalk.data.AppDatabase
import com.example.taptalk.data.FastSettingsEntity
import com.example.taptalk.ui.screen.AccScreen
import com.example.taptalk.ui.theme.TapTalkTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * A map that associates user-friendly voice names (e.g., "Kate", "Josh") with a list of
 * technical keywords used to identify specific Text-to-Speech (TTS) voices on the device.
 *
 * This mapping provides a fallback mechanism to find a suitable voice when the exact
 * voice name stored in preferences is not available on the current device. The `pickTtsVoice`
 * function uses the keywords to search for a voice that matches characteristics like gender,
 * accent, or internal engine identifiers.
 *
 * The keys of the map are friendly names presented to the user in settings.
 * The values are lists of strings, where each string is a keyword or identifier.
 * The search for a matching voice is case-insensitive.
 *
 * Example:
 * - "Kate" is associated with female voices, including specific US and GB English variants.
 * - "Sabrina" is associated with female child voices.
 */
val TTS_VOICE_MAP = mapOf(
    "Kate" to listOf("female", "en-us-x-sfg", "en-gb-x-fis", "f1"),
    "Josh" to listOf("male", "en-us-x-tpd", "en-gb-x-rjs", "m1"),
    "Sabrina" to listOf("female", "child", "en-us-x-sfg#female_2", "en-in-x-cxx"),
    "Sami" to listOf("male", "child", "en-us-x-tpd#male_2", "en-in-x-ism")
)

/**
 * A custom [Fetcher] for Coil that handles URIs for Android assets.
 *
 * This class is designed to load images from the `assets` directory of the application
 * using URIs that start with "file:///android_asset/". It decodes the asset path
 * from the URI, opens an [InputStream] to the asset, and returns it as a [SourceResult]
 * for Coil to process.
 *
 * @property context The application [Context] used to access the assets.
 * @property uri The [Uri] of the asset to fetch, e.g., "file:///android_asset/images/my_image.png".
 */
class AssetUriFetcher(
    private val context: Context,
    private val uri: Uri
) : Fetcher {

    /**
     * Fetches an image from the Android assets folder.
     *
     * This method handles URIs with the scheme `file:///android_asset/`.
     * It strips the prefix to get the relative asset path, opens an
     * [InputStream] to the asset, and returns it as a [SourceResult]
     * for Coil to process.
     *
     * @return A [FetchResult] containing the image data as a [SourceResult].
     */
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

    /**
     * A factory for creating [AssetUriFetcher] instances.
     *
     * This factory checks if a given [Uri] points to an Android asset
     * (i.e., starts with "file:///android_asset/"). If it does, it creates
     * an [AssetUriFetcher] to handle fetching the image from the assets folder.
     * Otherwise, it returns null, allowing Coil to delegate to another fetcher.
     */
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

/**
 * Selects a TTS voice based on a user-friendly name.
 *
 * This function attempts to find the best available TTS voice that matches the characteristics
 * associated with a given `friendlyName`. It uses the `TTS_VOICE_MAP` to look up a list
 * of keywords (e.g., "female", "child", "en-us-x-sfg") for the friendly name. It then
 * iterates through the available voices on the device and returns the first one whose
 * name contains any of the associated keywords.
 *
 * @param tts The initialized [TextToSpeech] instance from which to get available voices.
 * @param friendlyName The user-friendly name of the desired voice (e.g., "Kate", "Sami").
 * @return The best-matching [android.speech.tts.Voice] object, or `null` if no suitable voice is found
 *         or if the TTS service/voices are unavailable.
 */
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

/**
 * Selects an appropriate icon asset URI for a given negative verb form.
 *
 * This function takes a string containing a negative contraction (e.g., "don't", "won't")
 * and determines the correct tense (present, past, future, perfect) to select a
 * corresponding icon. The icon paths are prefixed with the Android asset directory path.
 *
 * @param neg The string containing the negative verb form. The function is case-insensitive.
 * @return A `String` representing the full asset URI (e.g., "file:///android_asset/tenses/negative_present.png")
 *         for the corresponding tense icon. Defaults to the present tense icon if no match is found.
 */
fun negativeIconFor(neg: String): String {
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

/**
 * Retrieves the base, past, perfect, and negative forms of a given verb.
 *
 * This function first checks for special hardcoded verbs (like "be", "have", "will")
 * using [specialVerbForms]. If not found, it queries a provided [JSONObject] for
 * irregular verb forms. If the verb is not in the JSON data, it assumes a regular
 * verb and forms the past and perfect tenses by appending "ed".
 *
 * @param verb The base form of the verb to look up (e.g., "go", "walk").
 * @param json A [JSONObject] where keys are lowercase base verbs and values are objects
 *             containing "past", "perfect", and "negatives" forms.
 * @return A [VerbForms] data class containing the different forms of the verb.
 */
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

/**
 * Determines the border color for an AAC (Augmentative and Alternative Communication) card
 * based on its folder name, which typically corresponds to a grammatical category.
 *
 * This function implements a color-coding system (e.g., the Fitzgerald Key) to visually
 * distinguish different typesof words. For example, verbs are green, nouns are orange, etc.
 * The folder name is matched case-insensitively against keywords for different
 * parts of speech.
 *
 * @param folder The name of the folder containing the card's image, used to infer its grammatical category.
 * @return A [Color] object representing the appropriate border color. Defaults to [Color.Black]
 *         if no specific category is matched.
 */
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

// ---------------- UTIL: GRID ORDER ----------------
val largeGridOrder: List<String?> = listOf(
    "what_A1","I_A1","we_A1","have_A1","know_A1", "eat_A1", "child_A1","place_A1","time_A1","hand_A1","good_A1","bad_A1", "boring_A1",
    "how_A1","you_A1","they_A1","come_A1","make_A1", "drink_A1","man_A1","world_A1","year_A1","thing_A1","thirsty_A2","hungry_A1", "wrong_A1",
    "why_A1","he_A1","be_A1","do_A1","say_A1", "think_A1","woman_A1","house_A1","week_A1","bathroom_A1","happy_A1","sad_A1", "funny_A1",
    "where_A1","she_A1","yes_A1","get_A1","see_A1", "study_A2", "person_A1","school_A1","life_A1","food_A1","tired_A1","calm_B1", "polite_A2",
    "who_A1","it_A1","no_A1","go_B1","create_A1", "will_A1", "friend_A1","job_A1","family_A1","water_A1","beautiful_A1","pretty_A1", "brilliant_A2",
    "when_A1","my_A1","your_A1","hello_A1","good_morning_A1","bye_A1","thanks_A1","please_A1","sorry_A1","great_A1","and_A1", "or_A1", "because_A1",
    "whose_A1", "a_A1", "an_A1", "the_A1", "toothache_A2", "backache_B1", "headache_A1", "ache_B1", "dizzy_A2", "sore_B1", "to_A1", "of_A1", "from_A1",
    null,null,null,null,null,null,null,null,null,null,null
)

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

/**
 * Provides hardcoded `VerbForms` for common irregular verbs like "be", "have", and "will".
 * These verbs have unique conjugations and negative forms that don't follow standard rules.
 * This function serves as a quick lookup before attempting to generate forms from a larger JSON file
 * or applying default rules.
 *
 * @param verb The base form of the verb to look up (case-insensitive).
 * @return A [VerbForms] object containing the past, perfect, and negative forms if the verb
 * is a special case (e.g., "be", "have", "will"). Returns `null` if the verb is not
 * found in the hardcoded list, indicating that regular processing should be used.
 */
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

/**
 * Normalizes a file name string into a human-readable label.
 *
 * This function performs several transformations:
 * 1. Removes the file extension.
 * 2. Strips CEFR level codes (e.g., _A1, _B2) and language codes (e.g., _EN, _PL).
 * 3. Removes any leading numbers followed by an optional underscore or hyphen.
 * 4. Replaces underscores and hyphens with spaces.
 * 5. Collapses multiple spaces into a single space and trims whitespace.
 * 6. Converts the string to Title Case, but keeps common articles and prepositions
 *    (e.g., "a", "the", "of") in lowercase, except for the very first word.
 *
 * Example: "123-good_morning_A1.png" becomes "Good Morning".
 * Example: "a_cup_of_tea.png" becomes "A Cup of Tea".
 *
 * @param file The raw file name string.
 * @return A cleaned and title-cased version of the name.
 */
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

/**
 * Generates a plural form for a given noun.
 *
 * This function first checks a provided JSON object for a manually defined plural form.
 * If not found, it applies a set of common English pluralization rules:
 * - Nouns ending in a consonant followed by "y" are changed to "ies" (e.g., "story" -> "stories").
 * - Nouns ending in "s", "x", "z", "ch", or "sh" get "es" appended (e.g., "box" -> "boxes").
 * - All other nouns get a simple "s" appended (e.g., "cat" -> "cats").
 * The comparison is case-insensitive, but the output is based on the lowercase version of the input noun.
 *
 * @param noun The singular noun to be pluralized.
 * @param json A [JSONObject] containing a mapping of lowercase singular nouns to their
 *             irregular plural forms. This is checked first.
 * @return The suggested plural form of the noun as a [String].
 */
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

                    // Load settings
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


    /**
     * Handles the logic for speaking a given text string and saving it to history.
     *
     * This function retrieves the user's "autoSpeak" and "aiSupport" settings from Firestore.
     * If "autoSpeak" is enabled, it uses the Text-to-Speech engine to voice the provided text.
     * It always saves the sentence to the local history database and triggers a sync with Firebase.
     * If "aiSupport" is enabled, the text is also added to the `baseConversation` list,
     * which is used to generate smart replies.
     *
     * @param text The string of text to be spoken and logged.
     */
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

            if (autoSpeak) {
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ACC_UTTERANCE")
            }

            val repo = HistoryRepository(this@AccActivity)
            repo.saveSentenceOffline(text)
            repo.syncToFirebase()

            if (smartReplyEnabled) {
                baseConversation.add(TextMessage.createForLocalUser(text, System.currentTimeMillis()))
            }
        }
    }

    /**
     * Called when the activity is being destroyed.
     * This method stops and shuts down the Text-to-Speech (TTS) engine
     * to release its resources and prevent memory leaks. It then calls the
     * superclass's implementation of onDestroy to complete the activity's
     * lifecycle cleanup.
     */
    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
