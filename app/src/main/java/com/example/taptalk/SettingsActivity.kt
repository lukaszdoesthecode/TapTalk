package com.example.taptalk

import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taptalk.data.AppDatabase
import com.example.taptalk.data.FastSettingsRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import android.content.Context
import androidx.work.*
import com.example.taptalk.data.FastSettingsEntity

/**
 * An activity that displays the application's settings screen.
 * This activity is built using Jetpack Compose and provides users with various options
 * to customize the app's behavior, including voice settings, interface preferences,
 * data synchronization, and account management.
 *
 * It hosts the [SettingsScreen] composable, which contains the main UI and logic
 * for handling user interactions with the settings.
 */
class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                SettingsScreen()
            }
        }
    }
}

/**
 * Selects an Android Text-to-Speech (TTS) voice based on a friendly name.
 *
 * This function attempts to find the best-matching voice installed on the device
 * for a given friendly name (e.g., "Kate", "Josh"). It searches for specific keywords
 * within the voice names provided by the TTS engine.
 *
 * The matching logic is as follows:
 * 1. It looks for a voice whose name contains any of the predefined keywords associated
 *    with the friendly name (e.g., "en-gb", "female" for "Kate").
 * 2. If no match is found, it falls back to the first available English voice.
 * 3. If no English voice is found, it falls back to the very first voice available on the device.
 * 4. If no voices are available at all, it returns null.
 *
 * @param tts The initialized `TextToSpeech` instance from which to get the available voices.
 * @param friendly The user-friendly name of the desired voice (e.g., "Kate", "Josh", "Sabrina", "Sami").
 * @return The best-matched `android.speech.tts.Voice` object, or `null` if the TTS engine has no voices.
 */
private fun pickDeviceVoiceExact(tts: TextToSpeech?, friendly: String): android.speech.tts.Voice? {
    val all = tts?.voices?.toList().orEmpty()
    if (all.isEmpty()) return null
    val wanted = when (friendly) {
        "Kate" -> listOf("en-gb", "en-gb-x-fis", "female")
        "Josh" -> listOf("en-us", "en-us-x-tpd", "male")
        "Sabrina" -> listOf("child", "en-in", "female")
        "Sami" -> listOf("child", "en-in", "male")
        else -> listOf("en")
    }
    return all.firstOrNull { v -> wanted.any { k -> v.name.contains(k, true) } }
        ?: all.firstOrNull { it.locale?.language == "en" }
        ?: all.first()
}

/**
 * Schedules a one-time background job using WorkManager to synchronize "fast" settings.
 *
 * "Fast" settings are critical settings (like auto-speak, AI support) that need to be
 * available quickly, even offline. This function enqueues a `FastSettingsSyncWorker`
 * which will sync the local "fast" settings with the remote Firebase Firestore database.
 *
 * The job is constrained to run only when the device has a network connection.
 * It uses a unique work name "FastSettingsSync" with a policy of `REPLACE`, meaning
 * that if a sync job is already pending, it will be replaced by this new one. This is
 * useful for ensuring only the latest settings changes are synced if the user changes
 * multiple settings quickly.
 *
 * @param context The application context, used to get an instance of WorkManager.
 */
fun scheduleFastSettingsSync(context: Context) {
    val request = OneTimeWorkRequestBuilder<com.example.taptalk.data.FastSettingsSyncWorker>()
        .setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        )
        .build()

    WorkManager.getInstance(context).enqueueUniqueWork(
        "FastSettingsSync",
        ExistingWorkPolicy.REPLACE,
        request
    )
}

/**
 * A composable that displays a distinct section within the settings screen.
 * It consists of a title and a card-like surface containing the section's content.
 *
 * @param title The text to be displayed as the title of the section.
 * @param content A composable lambda that defines the content to be placed inside the section's card.
 *                This lambda is invoked within a `ColumnScope`, allowing for vertical arrangement of its children.
 */
@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFF333333),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 2.dp,
            color = Color.White
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = content
            )
        }
    }
}

/**
 * A Composable that displays a setting item with a title and a toggle switch.
 *
 * This component arranges a text label and a switch in a horizontal row,
 * with the label on the left and the switch on the right. It is typically
 * used for boolean settings.
 *
 * @param title The text to display as the title for the setting.
 * @param checked The current state of the switch (true for on, false for off).
 * @param onCheckedChange A callback that is invoked when the user toggles the switch.
 *                        It provides the new boolean state of the switch.
 */
@Composable
fun SettingsSwitch(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/**
 * A composable that displays a styled button for use in the settings screen.
 *
 * This button spans the full width of its parent and has a distinct light green background color.
 * It's designed for actions within a settings section, such as "Preview voice" or "Log Out".
 *
 * @param title The text to display on the button.
 * @param onClick The lambda function to be executed when the button is clicked.
 */
@Composable
fun SettingsButton(title: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDFF0D8))
    ) {
        Text(title, color = Color.Black)
    }
}

/**
 * A composable that displays a slider with a title, allowing users to select a value within a specified range.
 * This is a common UI element in settings screens for adjusting parameters like volume, speed, or pitch.
 *
 * @param title The text label displayed above the slider to indicate its purpose.
 * @param value The current value of the slider. This should be managed by a state variable.
 * @param onValueChange A lambda function that is invoked when the user drags the slider. It receives the new float value.
 * @param range The inclusive range of possible values for the slider (e.g., 0.5f..2.0f).
 */
@Composable
fun SettingsSlider(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>
) {
    Column {
        Text(title)
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range
        )
    }
}

/**
 * A composable that displays a dropdown menu for a setting.
 * It shows a title, a button with the currently selected option, and a dropdown
 * menu with all available options.
 *
 * @param title The title text to display above the dropdown.
 * @param options A list of strings representing the choices available in the dropdown.
 * @param selected The currently selected option string. This is displayed on the button.
 * @param onSelect A lambda function that is invoked when a new option is selected from the dropdown.
 *                 It receives the selected option string as a parameter.
 */
@Composable
fun SettingsDropdown(title: String, options: List<String>, selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(title)
        Box {
            Button(
                onClick = { expanded = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(selected, color = Color.Black)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { opt ->
                    DropdownMenuItem(
                        text = { Text(opt) },
                        onClick = {
                            onSelect(opt)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

/**
 * A Composable function that displays the main settings screen for the application.
 *
 * This screen provides users with a comprehensive interface to customize their experience.
 * It is organized into several sections, including "Voice & Speech", "Interface", "Custom Words",
 * "Data Sync", "Account", and "About".
 *
 * Key features:
 * - **State Management**: Uses `remember` and `mutableStateOf` to manage the state of various settings
 *   like voice selection, pitch, speed, UI modes, and more.
 * - **Firebase Integration**: Fetches the user's current settings from Firestore upon composition and
 *   updates them in real-time as the user makes changes. The user is identified by their
 *   Firebase Auth UID.
 * - **Local Database Interaction**: Interacts with a local Room database via `FastSettingsRepository`
 *   to save settings that require quick, offline access and schedules a background sync.
 * - **Text-To-Speech (TTS)**: Initializes and manages a TTS engine to provide voice previews and
 *   handle speech-related settings. The TTS engine is properly shut down when the screen is disposed.
 * - **UI Composition**: Built with Jetpack Compose, using a `Scaffold` for the basic layout,
 *   a `Column` for scrollable content, and custom `SettingsSection` composables to group related settings.
 * - **User Actions**: Allows users to manage custom words, trigger manual data synchronization, log out,
 *   and delete their account.
 * - **Navigation**: Includes a `BottomNavBar` for app-wide navigation and handles navigation to other
 *   activities like `CustomWordsManagerActivity` and `LoginActivity`.
 */
@Composable
fun SettingsScreen() {
    val context = LocalContext.current

    val db = remember { AppDatabase.getInstance(context) }
    val fastRepo = remember { FastSettingsRepository(db.fastSettingsDao(), db.historyDao()) }
    val coroutineScope = rememberCoroutineScope()

    val ttsState = remember { mutableStateOf<TextToSpeech?>(null) }

    LaunchedEffect(Unit) {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                engine?.language = java.util.Locale.US
                ttsState.value = engine
            } else {
                android.util.Log.e("TTS", "Init failed: $status")
                ttsState.value = null
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            ttsState.value?.stop()
            ttsState.value?.shutdown()
        }
    }

    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    val userId = FirebaseAuth.getInstance().currentUser?.uid
    val firestore = remember { FirebaseFirestore.getInstance() }

    var selectedVoice by remember { mutableStateOf("Kate") }
    var voicePitch by remember { mutableStateOf(1f) }
    var voiceSpeed by remember { mutableStateOf(1f) }
    var autoSpeak by remember { mutableStateOf(true) }
    var smartReplyEnabled by remember { mutableStateOf(true) }
    var darkMode by remember { mutableStateOf(false) }
    var lowVisionMode by remember { mutableStateOf(false) }
    var gridSize by remember { mutableStateOf("Medium") }

    fun updateSetting(key: String, value: Any) {
        if (userId == null) return
        firestore.collection("USERS")
            .document(userId)
            .collection("Fast_Settings")
            .document("current")
            .update(key, value)
    }

    LaunchedEffect(userId) {
        if (userId != null) {
            firestore.collection("USERS")
                .document(userId)
                .collection("Fast_Settings")
                .document("current")
                .get()
                .addOnSuccessListener { doc ->
                    if (doc != null && doc.exists()) {
                        selectedVoice = doc.getString("selectedVoice") ?: "Kate"
                        voicePitch = (doc.getDouble("voicePitch") ?: 1.0).toFloat()
                        voiceSpeed = (doc.getDouble("voiceSpeed") ?: 1.0).toFloat()
                        autoSpeak = doc.getBoolean("autoSpeak") ?: true
                        smartReplyEnabled = doc.getBoolean("aiSupport") ?: true
                        darkMode = doc.getBoolean("darkMode") ?: false
                        lowVisionMode = doc.getBoolean("lowVisionMode") ?: false
                        gridSize = doc.getString("gridSize") ?: "Medium"
                    }
                }
        }
    }

    Scaffold(
        bottomBar = { BottomNavBar() },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .background(Color(0xFFF8F8F8))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            SettingsSection("Voice & Speech") {
                SettingsSwitch(
                    title = "Auto-speak selected words",
                    checked = autoSpeak,
                    onCheckedChange = {
                        autoSpeak = it
                        updateSetting("autoSpeak", it)

                        coroutineScope.launch {
                            val local = fastRepo.getLocalSettings()
                            val updated = (local ?: FastSettingsEntity(
                                volume = 50f,
                                selectedVoice = selectedVoice,
                                aiSupport = smartReplyEnabled,
                                autoSpeak = it
                            )).copy(autoSpeak = it)
                            fastRepo.saveLocalSettings(updated)
                            scheduleFastSettingsSync(context)
                        }
                    }
                )


                SettingsDropdown(
                    title = "Voice",
                    options = listOf("Kate","Josh","Sabrina","Sami"),
                    selected = selectedVoice,
                    onSelect = { v ->
                        selectedVoice = v
                        updateSetting("selectedVoice", v)

                        val tts = ttsState.value
                        val picked = pickDeviceVoiceExact(tts, v)
                        if (picked != null && tts != null) {
                            tts.voice = picked
                            tts.setSpeechRate(voiceSpeed)
                            tts.setPitch(voicePitch)

                            VoicePrefs.save(context, picked.name, voiceSpeed, voicePitch)
                            updateSetting("voiceName", picked.name)
                            updateSetting("voiceSpeed", voiceSpeed)
                            updateSetting("voicePitch", voicePitch)
                        }
                    }
                )

                SettingsSlider(
                    title = "Saved voice speed",
                    value = voiceSpeed,
                    onValueChange = {
                        voiceSpeed = it
                        updateSetting("voiceSpeed", it)
                    },
                    range = 0.5f..2f
                )

                SettingsButton("Preview voice") {
                    val tts = ttsState.value ?: return@SettingsButton
                    val picked = pickDeviceVoiceExact(tts, selectedVoice)
                    if (picked != null) tts.voice = picked

                    tts.setSpeechRate(voiceSpeed)
                    tts.setPitch(voicePitch)

                    VoicePrefs.save(context, picked?.name, voiceSpeed, voicePitch)
                    updateSetting("voiceName", picked?.name ?: "")
                    updateSetting("voiceSpeed", voiceSpeed)
                    updateSetting("voicePitch", voicePitch)

                    val message = "This is the preview for $selectedVoice."
                    tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, "preview")
                }



            }

            SettingsSection("Interface") {
                SettingsSwitch("Dark Mode", darkMode) {
                    darkMode = it
                    updateSetting("darkMode", it)
                }
                SettingsSwitch("Low Vision Mode", lowVisionMode) {
                    lowVisionMode = it
                    updateSetting("lowVisionMode", it)
                }
                SettingsDropdown(
                    title = "Grid Size",
                    options = listOf("Small", "Medium", "Large"),
                    selected = gridSize,
                    onSelect = {
                        gridSize = it
                        updateSetting("gridSize", it)
                    }
                )
                var visibleLevels by remember { mutableStateOf(listOf("A1", "A2", "B1")) }

                LaunchedEffect(userId) {
                    if (userId != null) {
                        firestore.collection("USERS").document(userId)
                            .collection("Fast_Settings").document("current")
                            .get()
                            .addOnSuccessListener { doc ->
                                if (doc.exists()) {
                                    val list = doc.get("visibleLevels") as? List<*>
                                    visibleLevels = list?.filterIsInstance<String>() ?: listOf("A1", "A2", "B1")
                                }
                            }
                    }
                }

                SettingsSection("Language Levels") {
                    val allLevels = listOf("A1", "A2", "B1")

                    allLevels.forEach { level ->
                        SettingsSwitch(
                            title = "Show $level words",
                            checked = visibleLevels.contains(level),
                            onCheckedChange = { checked ->
                                val newList = if (checked)
                                    visibleLevels + level
                                else
                                    visibleLevels - level
                                visibleLevels = newList
                                updateSetting("visibleLevels", newList)
                            }
                        )
                    }
                }

            }

            SettingsSwitch(
                title = "Enable Smart Replies",
                checked = smartReplyEnabled,
                onCheckedChange = {
                    smartReplyEnabled = it
                    updateSetting("aiSupport", it)

                    coroutineScope.launch {
                        val local = fastRepo.getLocalSettings()
                        val updated = (local ?: FastSettingsEntity(
                            volume = 50f,
                            selectedVoice = selectedVoice,
                            aiSupport = it
                        )).copy(aiSupport = it)

                        fastRepo.saveLocalSettings(updated)
                        scheduleFastSettingsSync(context)
                    }
                }
            )

            SettingsSection("Custom Words") {
                SettingsButton("Manage Custom Words") {
                    val intent = Intent(context, CustomWordsManagerActivity::class.java)
                    context.startActivity(intent)
                }
            }


            // === DATA SYNC ===
            SettingsSection("Data Sync") {
                var syncing by remember { mutableStateOf(false) }

                SettingsButton(
                    title = if (syncing) "Syncing..." else "Sync Now"
                ) {
                    syncing = true
                    coroutineScope.launch {
                        val repo = com.example.taptalk.data.HistoryRepository(context)
                        repo.syncToFirebase()
                        syncing = false
                        android.widget.Toast.makeText(
                            context,
                            "✅ Synced with Firebase ☁️",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            // === ACCOUNT ===
            SettingsSection("Account") {
                SettingsButton("Log Out") {
                    FirebaseAuth.getInstance().signOut()
                    android.widget.Toast.makeText(
                        context,
                        "👋 Logged out",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    context.startActivity(Intent(context, LoginActivity::class.java))
                }

                SettingsButton("Delete Account") {
                    val user = FirebaseAuth.getInstance().currentUser
                    user?.delete()?.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            if (userId != null) {
                                firestore.collection("USERS").document(userId).delete()
                            }
                            android.widget.Toast.makeText(
                                context,
                                "🗑️ Account deleted",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                            context.startActivity(Intent(context, LoginActivity::class.java))
                        } else {
                            android.widget.Toast.makeText(
                                context,
                                "❌ Failed to delete account",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }

            // === ABOUT ===
            SettingsSection("About") {
                Text("TapTalk AAC App", fontSize = 16.sp)
                Text("Version 1.0", color = Color.Gray)
                SettingsButton("Contact Developer") {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "message/rfc822"
                        putExtra(Intent.EXTRA_EMAIL, arrayOf("developer@taptalk.app"))
                        putExtra(Intent.EXTRA_SUBJECT, "Feedback on TapTalk AAC App")
                    }
                    context.startActivity(Intent.createChooser(intent, "Contact Developer"))
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
