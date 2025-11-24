package com.example.taptalk

import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.taptalk.data.AppDatabase
import com.example.taptalk.data.FastSettingsEntity
import com.example.taptalk.data.HistoryRepository
import com.example.taptalk.ui.components.BottomNavBar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.mlkit.nl.smartreply.SmartReply
import com.google.mlkit.nl.smartreply.SmartReplySuggestionResult
import com.google.mlkit.nl.smartreply.TextMessage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Locale
import androidx.room.Room
import com.example.taptalk.ui.theme.Bar

// SUGGESTIONS

fun generateKeyboardSuggestions(userText: String): List<String> {
    val text = userText.lowercase()
    return when {
        text.contains("hungry") || text.contains("eat") ->
            listOf("Let's eat!", "I want food", "What’s for lunch?")
        text.contains("tired") || text.contains("sleep") ->
            listOf("I need rest", "So sleepy...", "Let’s nap")
        text.contains("happy") ->
            listOf("Yay!", "That’s great!", "I’m so happy")
        text.contains("sad") ->
            listOf("I feel down", "I need a hug", "Not feeling good")
        text.contains("angry") ->
            listOf("I’m mad", "That’s annoying!", "I need a break")
        text.contains("help") ->
            listOf("Please help me", "Call someone", "I need assistance")
        text.contains("hello") || text.contains("hi") ->
            listOf("Hi there!", "Hey!", "How are you?")
        text.contains("thanks") ->
            listOf("You’re welcome!", "No problem!", "Anytime!")
        else -> listOf("Yes", "No", "Maybe", "Let’s go!")
    }
}

// ACTIVITY

class KeyboardActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tts = TextToSpeech(this, this)

        setContent {
            MaterialTheme {
                Scaffold(
                    bottomBar = { BottomNavBar() },
                    containerColor = Color(0xFFEFEFEF)
                ) { innerPadding ->

                    KeyboardScreen(
                        speak = { text -> speakOut(text) },
                        openTtsSettings = {
                            try {
                                startActivity(Intent("com.android.settings.TTS_SETTINGS"))
                            } catch (_: Exception) { }
                        },
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                            .background(Color(0xFFEFEFEF))
                    )
                }
            }
        }

        lifecycleScope.launch {
            HistoryRepository(this@KeyboardActivity).syncToFirebase()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            lifecycleScope.launch {
                val db = Room.databaseBuilder(
                    this@KeyboardActivity,
                    AppDatabase::class.java,
                    "tap_talk_db"
                ).build()
                val fastDao = db.fastSettingsDao()

                val firestore = FirebaseFirestore.getInstance()
                val userId = FirebaseAuth.getInstance().currentUser?.uid

                val local = fastDao.getSettings()

                var voiceName = local?.selectedVoice ?: "Kate"
                var speechRate = local?.voiceSpeed ?: 1.0f
                var pitch = local?.voicePitch ?: 1.0f

                applyVoiceSettings(tts, voiceName, speechRate, pitch)
                tts?.language = Locale.US

                lifecycleScope.launch {
                    runCatching {
                        val snap = firestore.collection("USERS")
                            .document(userId ?: return@launch)
                            .collection("Fast_Settings")
                            .document("current")
                            .get()
                            .await()

                        if (snap.exists()) {
                            voiceName = snap.getString("selectedVoice") ?: voiceName
                            speechRate = (snap.getDouble("voiceSpeed")
                                ?: speechRate.toDouble()).toFloat()
                            pitch = (snap.getDouble("voicePitch")
                                ?: pitch.toDouble()).toFloat()

                            val updated = local?.copy(
                                selectedVoice = voiceName,
                                voiceSpeed = speechRate,
                                voicePitch = pitch,
                                aiSupport = snap.getBoolean("aiSupport") ?: local.aiSupport,
                                gridSize = snap.getString("gridSize") ?: local.gridSize,
                                isSynced = true
                            ) ?: FastSettingsEntity(
                                volume = 50f,
                                selectedVoice = voiceName,
                                voiceSpeed = speechRate,
                                voicePitch = pitch,
                                aiSupport = snap.getBoolean("aiSupport") ?: true,
                                gridSize = snap.getString("gridSize") ?: "Medium",
                                isSynced = true
                            )

                            fastDao.insertOrUpdate(updated)
                            applyVoiceSettings(tts, voiceName, speechRate, pitch)
                        }
                    }
                }

            }
        }
    }

    private fun speakOut(text: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val firestore = FirebaseFirestore.getInstance()
        val repo = HistoryRepository(this)

        lifecycleScope.launch {
            var autoSpeak = true

            try {
                val snap = firestore.collection("USERS")
                    .document(userId ?: return@launch)
                    .collection("Fast_Settings")
                    .document("current")
                    .get()
                    .await()

                if (snap.exists()) {
                    autoSpeak = snap.getBoolean("autoSpeak") ?: true
                }
            } catch (_: Exception) { }

            if (autoSpeak) {
                tts?.speak(
                    text,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "KEYBOARD_UTTERANCE"
                )
            }

            repo.saveSentenceOffline(text)
            repo.syncToFirebase()
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}

// KEY INPUT

fun handleKeyPress(currentText: String, key: String): String =
    when (key) {
        "SPACE" -> "$currentText "
        "⌫" -> if (currentText.isNotEmpty()) currentText.dropLast(1) else currentText
        else -> currentText + key
    }

// UI: ROW OF KEYS

@Composable
fun KeyboardRowExact(
    keys: List<String>,
    keyHeight: Dp,
    onKeyClick: (String) -> Unit
) {
    val spacing = 6.dp
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val horizontalPadding = 16.dp
    val usableWidth = screenWidth - horizontalPadding * 2 - spacing * (keys.size - 1)

    val unitWidths = keys.map {
        when (it) {
            "SPACE" -> 3f
            "COPY", "PASTE" -> 1.6f
            else -> 1f
        }
    }

    val totalUnits = unitWidths.sum()
    val unitWidth = usableWidth / totalUnits

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding),
        Arrangement.spacedBy(spacing),
        Alignment.CenterVertically
    ) {
        keys.forEachIndexed { i, k ->
            val width = unitWidth * unitWidths[i]
            Box(
                Modifier
                    .width(width)
                    .height(keyHeight + 35.dp)
                    .background(Color(0xFFD9D9D9), RoundedCornerShape(6.dp))
                    .border(3.dp, Color.Black, RoundedCornerShape(6.dp))
                    .clickable { onKeyClick(k) },
                contentAlignment = Alignment.Center
            ) {
                Text(k, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            }
        }
    }
}

// UI: WHOLE SCREEN

@Composable
fun KeyboardScreen(
    speak: (String) -> Unit,
    openTtsSettings: () -> Unit,
    modifier: Modifier = Modifier
) {

    val firestore = FirebaseFirestore.getInstance()
    val userId = FirebaseAuth.getInstance().currentUser?.uid
    var smartRepliesEnabled by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val snap = firestore.collection("USERS")
            .document(userId ?: return@LaunchedEffect)
            .collection("Fast_Settings")
            .document("current")
            .get()
            .await()

        smartRepliesEnabled = snap.getBoolean("aiSupport") ?: true
    }

    var text by remember { mutableStateOf("") }
    val keyHeight =
        if (LocalConfiguration.current.screenWidthDp < 700) 48.dp else 56.dp
    val clipboard = LocalClipboardManager.current

    var smartReplies by remember { mutableStateOf(listOf<String>()) }
    val smartReply = remember { SmartReply.getClient() }

    val conversation = remember {
        listOf(
            TextMessage.createForRemoteUser(
                "Hey, how are you?",
                System.currentTimeMillis() - 60000,
                "user1"
            ),
            TextMessage.createForLocalUser(
                "I'm fine, how about you?",
                System.currentTimeMillis() - 30000
            ),
            TextMessage.createForRemoteUser(
                "Doing good! Want to meet later?",
                System.currentTimeMillis() - 10000,
                "user1"
            )
        )
    }

    LaunchedEffect(text) {
        if (text.isNotBlank()) {
            try {
                val conv = conversation + TextMessage.createForLocalUser(
                    text,
                    System.currentTimeMillis()
                )
                val result = smartReply.suggestReplies(conv).await()
                if (result.status == SmartReplySuggestionResult.STATUS_SUCCESS) {
                    val replies = result.suggestions.map { it.text }
                        .filter { it.lowercase() !in listOf("nice", "ok", "okay", "sure") }

                    smartReplies = if (replies.isNotEmpty()) replies.take(3)
                    else generateKeyboardSuggestions(text)
                } else {
                    smartReplies = generateKeyboardSuggestions(text)
                }
            } catch (e: Exception) {
                smartReplies = generateKeyboardSuggestions(text)
            }
        } else {
            smartReplies = emptyList()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFEFEFEF))
    ) {

        //  TEXT BAR + SPEAK
        Row(
            Modifier
                .fillMaxWidth()
                .height(90.dp)
                .background(Color.White)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text.ifBlank { "Tap to type or build a sentence..." },
                modifier = Modifier
                    .padding(end = 8.dp)
                    .weight(1f),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            IconButton(onClick = { text = "" }) {
                Icon(Icons.Default.Delete, contentDescription = "Clear all", tint = Color.Black)
            }
            IconButton(onClick = { if (text.isNotBlank()) speak(text) }) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Speak", tint = Color.Black)
            }
            IconButton(onClick = { openTtsSettings() }) {
                Icon(Icons.Default.Settings, contentDescription = "TTS Settings", tint = Color.Black)
            }
        }

        // SMART REPLIES
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Bar)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (smartRepliesEnabled) {
                val suggestions = smartReplies.ifEmpty { listOf("Okay", "Sure!", "Sounds good") }
                suggestions.forEach { word ->
                    Box(
                        modifier = Modifier
                            .height(60.dp)
                            .width(100.dp)
                            .border(2.dp, Color.Black, RoundedCornerShape(6.dp))
                            .background(Color(0xFFD9D9D9), RoundedCornerShape(6.dp))
                            .clickable { text += if (text.isBlank()) word else " $word" },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            word,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }

        // KEYBOARD
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 12.dp)
                .padding(bottom = 8.dp),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            fun press(k: String) {
                text = when (k) {
                    "COPY" -> {
                        clipboard.setText(AnnotatedString(text))
                        text
                    }
                    "PASTE" -> clipboard.getText()?.text?.let { text + it } ?: text
                    else -> handleKeyPress(text, k)
                }
            }

            KeyboardRowExact("1234567890".map { it.toString() }, keyHeight, ::press)
            KeyboardRowExact("QWERTYUIOP".map { it.toString() }, keyHeight, ::press)
            KeyboardRowExact("ASDFGHJKL".map { it.toString() }, keyHeight, ::press)
            KeyboardRowExact(
                listOf("↑") + "ZXCVBNM".map { it.toString() } + listOf(".", "⌫"),
                keyHeight,
                ::press
            )
            KeyboardRowExact(
                listOf("COPY", "PASTE", "SPACE", ",", "!", "?"),
                keyHeight,
                ::press
            )
        }
    }
}
