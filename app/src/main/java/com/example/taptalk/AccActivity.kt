package com.example.taptalk

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.Options
import com.example.taptalk.aac.data.VerbForms
import com.example.taptalk.aac.data.loadAccCards
import com.example.taptalk.aac.data.loadCustomCards
import com.example.taptalk.data.AppDatabase
import com.example.taptalk.data.FastSettingsEntity
import com.example.taptalk.data.HistoryRepository
import com.example.taptalk.ui.screen.AccScreen
import com.example.taptalk.ui.theme.TapTalkTheme
import com.example.taptalk.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.mlkit.nl.smartreply.SmartReply
import com.google.mlkit.nl.smartreply.TextMessage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import okio.buffer
import okio.source
import org.json.JSONObject
import java.util.Locale

// ASSET FETCHER

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

// HELPER

fun negativeIconFor(neg: String): String {
    val n = neg.lowercase()
    val base = "file:///android_asset/tenses/"

    return when {
        "won't" in n || "will not" in n -> base + "negative_future.png"
        "haven" in n || "hasn" in n || "hadn" in n -> base + "negative_perfect.png"
        "didn" in n || "wasn" in n || "weren" in n -> base + "negative_past.png"
        "don'" in n || "doesn" in n || "isn" in n || "aren" in n -> base + "negative_present.png"
        else -> base + "negative_present.png"
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
        "pronoun" in f     -> Pronoun
        "adjective" in f   -> Adjective
        "conjunction" in f -> Conjunction
        "emergency" in f   -> Emergency
        "negation" in f    -> Negation
        "noun" in f        -> Noun
        "preposition" in f -> Proposition
        "question" in f    -> Question
        "social" in f      -> Social
        "verbs" in f       -> Verb
        "determiner" in f  -> Determiner
        else               -> Color.Black
    }
}


val largeGridOrder: List<String?> = listOf(
    "what_A1","I_A1","it_A1","have_A1","know_A1", "eat_A1", "child_A1","place_A1","time_A1","hand_A1","good_A1","bad_A1", "boring_A1",
    "how_A1","you_A1","we_A1","come_A1","make_A1", "drink_A1","man_A1","world_A1","year_A1","thing_A1","thirsty_A2","hungry_A1", "wrong_A1",
    "why_A1","he_A1","they_A1","do_A1","say_A1", "think_A1","woman_A1","house_A1","week_A1","bathroom_A1","happy_A1","sad_A1", "funny_A1",
    "where_A1","she_A1","be_A1","get_A1","see_A1", "study_A2", "person_A1","school_A1","life_A1","food_A1","tired_A1","calm_B1", "polite_A2",
    "who_A1","my_A1","your_A1","go_B1","create_A1", "will_A1", "friend_A1","job_A1","family_A1","water_A1","beautiful_A1","pretty_A1", "brilliant_A2",
    "when_A1","yes_A1","no_A1","hello_A1","good_morning_A1","bye_A1","thanks_A1","please_A1","sorry_A1","great_A1","and_A1", "or_A1", "because_A1",
    "whose_A1", "a_A1", "an_A1", "the_A1", "toothache_A2", "backache_B1", "headache_A1", "ache_B1", "dizzy_A2", "sore_B1", "to_A1", "of_A1", "from_A1",
    null,null,null,null,null,null,null,null,null,null,null
)

val gridOrder: List<String?> = listOf(
    "what_A1","I_A1","it_A1","have_A1","know_A1","child_A1","place_A1","time_A1","hand_A1","good_A1","bad_A1",
    "how_A1","you_A1","we_A1","come_A1","make_A1","man_A1","world_A1","year_A1","thing_A1","thirsty_A2","hungry_A1",
    "why_A1","he_A1","they_A1","do_A1","say_A1","woman_A1","house_A1","week_A1","bathroom_A1","happy_A1","sad_A1",
    "where_A1","she_A1","be_A1","get_A1","see_A1","person_A1","school_A1","life_A1","food_A1","tired_A1","calm_B1",
    "who_A1","yes_A1","no_A1","go_B1","create_A1","friend_A1","job_A1","family_A1","water_A1","and_A1","or_A1",
    "when_A1","a_A1","an_A1","the_A1","hello_A1","good_morning_A1","bye_A1","thanks_A1","please_A1","to_A1","of_A1",
    null,null,null,null,null,null,null,null,null,null,null
)

val smallGridOrder: List<String?> = listOf(
    "what_A1","I_A1","we_A1","come_A1","child_A1","place_A1","hand_A1", "good_A1","bad_A1",
    "how_A1","you_A1","they_A1","do_A1","man_A1","house_A1","bathroom_A1","thirsty_A2","hungry_A1",
    "why_A1","he_A1","be_A1","get_A1","woman_A1","school_A1","food_A1","happy_A1","sad_A1",
    "where_A1","she_A1","have_A1","go_B1","friend_A1","job_A1","water_A1","tired_A1","calm_B1",
    "who_A1","yes_A1","no_A1","hello_A1","good_morning_A1","bye_A1","thanks_A1","please_A1","to_A1",
    null,null,null,null,null,null,null,null,null
)

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

fun applyVoiceSettings(tts: TextToSpeech?, voiceName: String, rate: Float, pitch: Float) {
    val voices = tts?.voices ?: return
    val match = voices.firstOrNull { it.name == voiceName }
        ?: voices.firstOrNull { it.locale?.language == "en" }
        ?: voices.firstOrNull()

    tts.voice = match
    tts.setSpeechRate(rate)
    tts.setPitch(pitch)
}

fun suggestPlural(noun: String, json: JSONObject): String {
    val lower = noun.lowercase()
    val plural = if (json.has(lower)) {
        json.getString(lower)
    } else {
        when {
            lower.endsWith("y") && lower.length > 1 &&
                    !"aeiou".contains(lower[lower.length - 2]) ->
                lower.dropLast(1) + "ies"

            lower.endsWith("s") || lower.endsWith("x") || lower.endsWith("z")
                    || lower.endsWith("ch") || lower.endsWith("sh") ->
                lower + "es"

            else -> lower + "s"
        }
    }

    return plural.replaceFirstChar { it.uppercase() }
}

// ACC ACTIVITY

class AccActivity : ComponentActivity() {

    private var tts: TextToSpeech? = null
    private val baseConversation = mutableListOf<TextMessage>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val smartReply = SmartReply.getClient()
        val historyRepo = HistoryRepository(this)
        val allCards = loadAccCards(this) + loadCustomCards(this)

        lifecycleScope.launch {
            val history = historyRepo.getRecentSentences()
            history.forEach {
                baseConversation.add(TextMessage.createForLocalUser(it.sentence, it.timestamp))
            }
        }
        allCards.forEach { card ->
            baseConversation.add(TextMessage.createForLocalUser(card.label, System.currentTimeMillis()))
        }

        // TTS init
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                lifecycleScope.launch {
                    val firestore = FirebaseFirestore.getInstance()
                    val userId = FirebaseAuth.getInstance().currentUser?.uid

                    val db = Room.databaseBuilder(
                        this@AccActivity,
                        AppDatabase::class.java,
                        "tap_talk_db"
                    ).build()
                    val fastDao = db.fastSettingsDao()

                    val local = fastDao.getSettings()

                    var voiceName = local?.selectedVoice ?: "Kate"
                    var speechRate = local?.voiceSpeed ?: 1.0f
                    var pitch = local?.voicePitch ?: 1.0f

                    applyVoiceSettings(tts, voiceName, speechRate, pitch)
                    tts?.language = Locale.US

                    lifecycleScope.launch {
                        runCatching {
                            val snap = firestore.collection("USERS")
                                .document(userId ?: return@runCatching)
                                .collection("Fast_Settings")
                                .document("current")
                                .get()
                                .await()

                            if (snap.exists()) {
                                val newVoice = snap.getString("selectedVoice") ?: voiceName
                                val newRate = (snap.getDouble("voiceSpeed") ?: speechRate.toDouble()).toFloat()
                                val newPitch = (snap.getDouble("voicePitch") ?: pitch.toDouble()).toFloat()

                                val updated = fastDao.getSettings()?.copy(
                                    selectedVoice = newVoice,
                                    voiceSpeed = newRate,
                                    voicePitch = newPitch,
                                    aiSupport = snap.getBoolean("aiSupport")
                                        ?: local?.aiSupport ?: true,
                                    gridSize = snap.getString("gridSize")
                                        ?: local?.gridSize ?: "Medium",
                                    isSynced = true
                                ) ?: FastSettingsEntity(
                                    volume = 50f,
                                    selectedVoice = newVoice,
                                    voiceSpeed = newRate,
                                    voicePitch = newPitch,
                                    aiSupport = true,
                                    gridSize = "Medium",
                                    isSynced = true
                                )

                                fastDao.insertOrUpdate(updated)
                                applyVoiceSettings(tts, newVoice, newRate, newPitch)
                            }
                        }
                    }

                    val imageLoader = ImageLoader.Builder(this@AccActivity)
                        .components { add(AssetUriFetcher.Factory(this@AccActivity)) }
                        .build()

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

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
