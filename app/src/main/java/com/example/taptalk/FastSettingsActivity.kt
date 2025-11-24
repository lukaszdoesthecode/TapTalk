package com.example.taptalk

import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.room.Room
import com.example.taptalk.data.AppDatabase
import com.example.taptalk.data.FastSettingsRepository
import com.example.taptalk.ui.components.BottomNavBar
import com.example.taptalk.ui.theme.Bar
import com.example.taptalk.ui.theme.SoftGreen
import com.example.taptalk.ui.theme.SoftGreenBorder
import com.example.taptalk.viewmodel.FastSettingsViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Locale


class FastSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                FastSettingsScreen()
            }
        }
    }
}

// FIRESTORE HELPER

/**
 * Safely updates a single setting field in a user's "Fast_Settings" document in Firestore.
 */
suspend fun safeUpdateSetting(userId: String, key: String, value: Any) {
    val firestore = FirebaseFirestore.getInstance()
    val docRef = firestore.collection("USERS")
        .document(userId)
        .collection("Fast_Settings")
        .document("current")

    try {
        docRef.update(key, value).await()
    } catch (e: Exception) {
        docRef.set(mapOf(key to value), SetOptions.merge()).await()
    }
}

// UI BUILDING BLOCKS

@Composable
fun SectionBox(title: String, iconId: Int, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 210.dp)
            .background(SoftGreen, RoundedCornerShape(16.dp))
            .border(3.dp, SoftGreenBorder, RoundedCornerShape(16.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Image(
                painter = painterResource(id = iconId),
                contentDescription = "$title icon",
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Color.Black
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            content()
        }
    }
}

/**
 * Selectable card for AI Yes/No.
 */
@Composable
fun AiOption(
    name: String,
    displayText: String,
    imageRes: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .size(100.dp)
            .border(
                width = 3.dp,
                color = if (selected) Color.Black else Bar,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .background(Color.White, RoundedCornerShape(10.dp))
            .padding(6.dp)
    ) {
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = name,
            modifier = Modifier.size(55.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            displayText,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// MAIN SCREEN

@Composable
fun FastSettingsScreen() {
    val context = LocalContext.current

    // DB + Repo + ViewModel
    val db = remember {
        Room.databaseBuilder(context, AppDatabase::class.java, "tap_talk_db").build()
    }
    val repo = remember { FastSettingsRepository(db.fastSettingsDao(), db.historyDao()) }

    val userId = FirebaseAuth.getInstance().currentUser?.uid

    if (userId == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("User not logged in", color = Color.Red, fontSize = 20.sp)
        }
        return
    }

    val viewModel = remember { FastSettingsViewModel(repo, userId) }
    val coroutineScope = rememberCoroutineScope()

    // system volume
    val audioManager = remember {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    val maxSystemVolume = remember {
        audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    }

    // TTS + VOICES
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var voices by remember { mutableStateOf<List<Voice>>(emptyList()) }
    var voicesLoaded by rememberSaveable { mutableStateOf(false) }

    // LOCAL STATE
    var volume by remember { mutableFloatStateOf(viewModel.volume) }
    var selectedVoice by remember { mutableStateOf(viewModel.selectedVoice) }
    var aiSupport by remember { mutableStateOf(viewModel.aiSupport) }

    // INIT TTS + LOAD VOICES
    LaunchedEffect(Unit) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                val all = tts?.voices ?: emptySet()
                val englishOffline = all.filter { v ->
                    v.locale.language == "en" && !v.isNetworkConnectionRequired
                }.sortedBy { it.name }
                voices = englishOffline
                voicesLoaded = true

                val match = englishOffline.firstOrNull { it.name == selectedVoice }
                if (match != null) {
                    tts?.voice = match
                }
            }
        }
    }

    // DISPOSE TTS
    DisposableEffect(Unit) {
        onDispose {
            tts?.stop()
            tts?.shutdown()
        }
    }

    // LOAD SETTINGS FROM FIRESTORE
    LaunchedEffect(Unit) {
        val firestore = FirebaseFirestore.getInstance()
        val snap = firestore.collection("USERS")
            .document(userId)
            .collection("Fast_Settings")
            .document("current")
            .get()
            .await()

        if (snap.exists()) {
            val newVolume = (snap.getDouble("volume") ?: viewModel.volume.toDouble()).toFloat()
            val newVoice = snap.getString("selectedVoice") ?: viewModel.selectedVoice
            val newAi = snap.getBoolean("aiSupport") ?: viewModel.aiSupport

            volume = newVolume
            selectedVoice = newVoice
            aiSupport = newAi

            val targetVol = (maxSystemVolume * (newVolume / 100f)).toInt()
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
        }
    }

    Scaffold(bottomBar = { BottomNavBar() }) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.White)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // SOUND
            SectionBox(title = "SOUND", iconId = R.drawable.sound) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Quiet", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Image(
                            painter = painterResource(R.drawable.quiet),
                            contentDescription = "Quiet Icon",
                            modifier = Modifier.size(70.dp)
                        )
                    }

                    Slider(
                        value = volume,
                        onValueChange = { value ->
                            volume = value
                            val newVol = (maxSystemVolume * (value / 100f)).toInt()
                            audioManager.setStreamVolume(
                                AudioManager.STREAM_MUSIC,
                                newVol.coerceIn(0, maxSystemVolume),
                                0
                            )
                        },
                        onValueChangeFinished = {
                            coroutineScope.launch {
                                viewModel.volume = volume
                                viewModel.saveSettings()
                                safeUpdateSetting(userId, "volume", volume)
                            }
                        },
                        valueRange = 0f..100f,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.Black,
                            activeTrackColor = Color.Black,
                            inactiveTrackColor = Color.Gray
                        )
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Loud", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Image(
                            painter = painterResource(R.drawable.scream),
                            contentDescription = "Loud Icon",
                            modifier = Modifier.size(70.dp)
                        )
                    }
                }

                Text(
                    text = volume.toInt().toString(),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // VOICE
            SectionBox(title = "VOICE", iconId = R.drawable.voice) {

                if (!voicesLoaded) {
                    Text("Loading voices...", fontSize = 18.sp, fontWeight = FontWeight.Medium)
                } else if (voices.isEmpty()) {
                    Text(
                        "No offline English voices found.\nCheck system TTS settings.",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    var expanded by remember { mutableStateOf(false) }

                    Text(
                        text = "Selected voice:",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = selectedVoice.ifBlank { "None" },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(12.dp))

                    Button(onClick = { expanded = true }) {
                        Text("Choose system voice")
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        voices.forEach { v ->
                            DropdownMenuItem(
                                text = {
                                    Text("${v.locale.displayLanguage} – ${v.name}")
                                },
                                onClick = {
                                    expanded = false
                                    selectedVoice = v.name

                                    coroutineScope.launch {
                                        viewModel.selectedVoice = v.name
                                        viewModel.saveSettings()
                                        safeUpdateSetting(userId, "selectedVoice", v.name)
                                    }

                                    tts?.voice = v
                                    tts?.speak(
                                        "Voice chosen",
                                        TextToSpeech.QUEUE_FLUSH,
                                        null,
                                        "VOICE_CHOSEN"
                                    )
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // AI SUPPORT
            SectionBox(title = "AI SUPPORT", iconId = R.drawable.ai) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    AiOption(
                        name = "No",
                        displayText = "No",
                        imageRes = R.drawable.no,
                        selected = !aiSupport
                    ) {
                        aiSupport = false
                        coroutineScope.launch {
                            viewModel.aiSupport = false
                            viewModel.saveSettings()
                            safeUpdateSetting(userId, "aiSupport", false)
                        }
                    }

                    AiOption(
                        name = "Yes",
                        displayText = "Yes",
                        imageRes = R.drawable.yes,
                        selected = aiSupport
                    ) {
                        aiSupport = true
                        coroutineScope.launch {
                            viewModel.aiSupport = true
                            viewModel.saveSettings()
                            safeUpdateSetting(userId, "aiSupport", true)
                        }
                    }
                }
            }
        }
    }
}
