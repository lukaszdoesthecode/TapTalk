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
import com.google.mlkit.nl.smartreply.*
import kotlinx.coroutines.tasks.await
import java.util.*

class KeyboardActivity : ComponentActivity() {
    private var tts: TextToSpeech? = null

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

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}

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

    LaunchedEffect(Unit) {
        try {
            val result = smartReply.suggestReplies(conversation).await()
            if (result.status == SmartReplySuggestionResult.STATUS_SUCCESS) {
                smartReplies = result.suggestions.map { it.text }.take(3)
            }
        } catch (e: Exception) {
            e.printStackTrace()
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

fun handleKeyPress(currentText: String, key: String): String {
    return when (key) {
        "SPACE" -> "$currentText "
        "⌫" -> if (currentText.isNotEmpty()) currentText.dropLast(1) else currentText
        else -> currentText + key
    }
}
