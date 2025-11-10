package com.example.taptalk

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
import androidx.compose.material.icons.filled.*
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
import com.example.taptalk.ui.components.BottomNavBar
import com.google.mlkit.nl.smartreply.*
import kotlinx.coroutines.tasks.await
import java.util.*

fun generateKeyboardSuggestions(userText: String): List<String> {
    val text = userText.lowercase()
    return when {
        text.contains("hungry") || text.contains("eat") -> listOf("Let's eat!", "I want food", "What’s for lunch?")
        text.contains("tired") || text.contains("sleep") -> listOf("I need rest", "So sleepy...", "Let’s nap")
        text.contains("happy") -> listOf("Yay!", "That’s great!", "I’m so happy")
        text.contains("sad") -> listOf("I feel down", "I need a hug", "Not feeling good")
        text.contains("angry") -> listOf("I’m mad", "That’s annoying!", "I need a break")
        text.contains("help") -> listOf("Please help me", "Call someone", "I need assistance")
        text.contains("hello") || text.contains("hi") -> listOf("Hi there!", "Hey!", "How are you?")
        text.contains("thanks") -> listOf("You’re welcome!", "No problem!", "Anytime!")
        else -> listOf("Yes", "No", "Maybe", "Let’s go!")
    }
}


/**
 * The main activity for the keyboard screen of the TapTalk application.
 *
 * This activity sets up the user interface for a custom keyboard designed for
 * accessibility. It initializes the Android Text-to-Speech (TTS) engine
 * to provide auditory feedback for the typed text. The UI is built using
 * Jetpack Compose and features a `KeyboardScreen` composable.
 *
 * The activity manages the lifecycle of the TTS engine, initializing it in `onCreate`
 * and shutting it down in `onDestroy` to prevent memory leaks and ensure proper
 * resource management.
 *
 * @see ComponentActivity
 * @see TextToSpeech
 * @see KeyboardScreen
 */
class KeyboardActivity : ComponentActivity() {
    private var tts: TextToSpeech? = null

    /**
     * Called when the activity is first created.
     *
     * This method initializes the activity, including:
     * - Setting up the Text-To-Speech (TTS) engine and setting its language to US English upon successful initialization.
     * - Setting the content view of the activity using Jetpack Compose.
     * - Building the UI with a `Scaffold` that includes a bottom navigation bar (`BottomNavBar`) and the main `KeyboardScreen`.
     * - Passing a lambda function to `KeyboardScreen` to handle the speech synthesis of typed text.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in [onSaveInstanceState].  <b><i>Note: Otherwise it is null.</i></b>
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tts = TextToSpeech(this) {
            if (it == TextToSpeech.SUCCESS) tts?.language = Locale.US
        }

        setContent {
            MaterialTheme {
                Scaffold(
                    bottomBar = { BottomNavBar() },
                    containerColor = Color(0xFFEFEFEF)
                ) { innerPadding ->
                    KeyboardScreen(
                        speak = { text ->
                            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "KEYBOARD_UTTERANCE")
                        },
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                            .background(Color(0xFFEFEFEF))
                    )
                }
            }
        }
    }

    /**
     * Called when the activity is being destroyed.
     * This is the final call that the activity will receive.
     * It is used here to release resources, specifically to stop and shut down the
     * Text-to-Speech (TTS) engine to prevent memory leaks.
     */
    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}

/**
 * Processes a key press and updates the current text accordingly.
 *
 * This function handles standard character appends, as well as special keys like
 * "SPACE" for adding a space and "⌫" for backspace/deleting the last character.
 *
 * @param currentText The current string of text before the key press.
 * @param key The string representing the key that was pressed. This can be a character,
 *            "SPACE", or "⌫".
 * @return The updated string after processing the key press.
 */
fun handleKeyPress(currentText: String, key: String): String {
    return when (key) {
        "SPACE" -> "$currentText "
        "⌫" -> if (currentText.isNotEmpty()) currentText.dropLast(1) else currentText
        else -> currentText + key
    }
}

/**
 * A composable function that renders a single row of a keyboard with exact spacing and sizing.
 *
 * This function arranges a list of keys in a `Row`, calculating their widths dynamically
 * based on the available screen width. Special keys like "SPACE", "COPY", and "PASTE"
 * are given larger relative widths. The keys are color-coded based on their type
 * (e.g., numbers, vowels, consonants, punctuation, special actions) to improve usability.
 *
 * @param keys A list of strings, where each string represents a key to be displayed in the row.
 * @param keyHeight The base height for each key. The actual height is slightly larger to accommodate padding and borders.
 * @param onKeyClick A lambda function that is invoked when a key is clicked, passing the string representation of the key.
 */
@Composable
fun KeyboardRowExact(keys: List<String>, keyHeight: Dp, onKeyClick: (String) -> Unit) {
    val spacing = 6.dp
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val horizontalPadding = 16.dp
    val totalSpacing = spacing * (keys.size - 1)
    val usableWidth = screenWidth - horizontalPadding * 2 - totalSpacing

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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        keys.forEachIndexed { index, key ->
            val width = unitWidth * unitWidths[index]
            val borderColor = when {
                key in "1234567890" -> Color(0xFF3B82F6)
                key.uppercase() in setOf("A", "E", "I", "O", "U") -> Color(0xFFFFA500)
                key in listOf("↑", "⌫", "SPACE", "COPY", "PASTE") -> Color(0xFF9F58E3)
                key in listOf(".", ",", "!", "?") -> Color.Black
                else -> Color(0xFF0B8B3A)
            }

            Box(
                modifier = Modifier
                    .width(width)
                    .height(keyHeight + 35.dp)
                    .background(Color(0xFFD9D9D9), RoundedCornerShape(6.dp))
                    .border(3.dp, borderColor, RoundedCornerShape(6.dp))
                    .clickable { onKeyClick(key) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    key,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }
    }
}

/**
 * A Composable function that displays a full-screen custom keyboard interface.
 *
 * This screen provides a complete Augmentative and Alternative Communication (AAC) keyboard,
 * allowing users to construct sentences by tapping keys. It includes a text display area,
 * smart reply suggestions, and a QWERTY-style keyboard with additional function keys
 * like space, backspace, copy, and paste. The composed text can be spoken aloud
 * using the provided text-to-speech (TTS) function.
 *
 * Features:
 * - **Text Display**: Shows the currently typed text or a placeholder prompt.
 * - **Action Bar**: Contains buttons to clear the text and to trigger text-to-speech.
 * - **Smart Replies**: Utilizes ML Kit's Smart Reply to suggest context-aware responses
 *   based on a predefined conversation history. Falls back to default suggestions if ML Kit fails.
 * - **Custom Keyboard**: A five-row keyboard with numbers, letters, and special function keys.
 *
 * @param speak A lambda function `(String) -> Unit` that is invoked to speak the given text aloud.
 *              This is typically connected to a TextToSpeech engine.
 * @param modifier The modifier to be applied to the root Column of the screen.
 */
@Composable
fun KeyboardScreen(speak: (String) -> Unit, modifier: Modifier = Modifier) {
    var text by remember { mutableStateOf("") }
    val configuration = LocalConfiguration.current
    val keyHeight = if (configuration.screenWidthDp < 700) 48.dp else 56.dp
    val clipboardManager = LocalClipboardManager.current

    var smartReplies by remember { mutableStateOf(listOf<String>()) }
    val smartReply = remember { SmartReply.getClient() }

    val conversation = listOf(
        TextMessage.createForRemoteUser("Hey, how are you?", System.currentTimeMillis() - 60000, "user1"),
        TextMessage.createForLocalUser("I'm fine, how about you?", System.currentTimeMillis() - 30000),
        TextMessage.createForRemoteUser("Doing good! Want to meet later?", System.currentTimeMillis() - 10000, "user1")
    )

    LaunchedEffect(text) {
        if (text.isNotBlank()) {
            try {
                val result = smartReply.suggestReplies(conversation).await()
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .background(Color.White)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (text.isBlank()) "Tap to type or build a sentence..." else text,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .weight(1f)
            )
            IconButton(onClick = { text = "" }) {
                Icon(Icons.Default.Delete, contentDescription = "Clear all", tint = Color.Black)
            }
            IconButton(onClick = { if (text.isNotBlank()) speak(text) }) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Speak", tint = Color.Black)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFDFF0D8))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val suggestions = if (smartReplies.isNotEmpty()) smartReplies else listOf("Okay", "Sure!", "Sounds good")
            suggestions.forEach { word ->
                Box(
                    modifier = Modifier
                        .height(60.dp)
                        .width(100.dp)
                        .border(2.dp, Color.Black, RoundedCornerShape(6.dp))
                        .background(Color(0xFFD9D9D9), RoundedCornerShape(6.dp))
                        .clickable { text += " $word" },
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
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 12.dp)
                .padding(bottom = 8.dp),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            fun handleKeyClick(key: String) {
                when (key) {
                    "COPY" -> clipboardManager.setText(AnnotatedString(text))
                    "PASTE" -> clipboardManager.getText()?.let { pasted -> text += pasted.text }
                    else -> text = handleKeyPress(text, key)
                }
            }

            KeyboardRowExact("1234567890".map { it.toString() }, keyHeight, ::handleKeyClick)
            KeyboardRowExact("QWERTYUIOP".map { it.toString() }, keyHeight, ::handleKeyClick)
            KeyboardRowExact("ASDFGHJKL".map { it.toString() }, keyHeight, ::handleKeyClick)
            KeyboardRowExact(listOf("↑") + "ZXCVBNM".map { it.toString() } + listOf(".", "⌫"), keyHeight, ::handleKeyClick)
            KeyboardRowExact(listOf("COPY", "PASTE", "SPACE", ",", "!", "?"), keyHeight, ::handleKeyClick)
        }
    }
}