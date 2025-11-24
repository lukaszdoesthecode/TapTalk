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
import androidx.room.Room
import com.example.taptalk.data.AppDatabase
import com.example.taptalk.data.FastSettingsRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import android.content.Context
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.work.*
import com.example.taptalk.data.FastSettingsEntity
import com.example.taptalk.ui.components.BottomNavBar
import com.google.firebase.auth.EmailAuthProvider
import kotlinx.coroutines.withContext
import android.speech.tts.Voice
import kotlinx.coroutines.tasks.await


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

private fun clearLocalCustomCards(context: Context) {
    val dir = context.getDir("custom_cards", Context.MODE_PRIVATE)
    if (dir.exists()) {
        dir.deleteRecursively()
    }
}

private suspend fun clearLocalUserData(context: Context) {
    val db = AppDatabase.getDatabase(context)

    db.fastSettingsDao().deleteAll()
    db.historyDao().deleteAll()
    db.userCategoryDao().deleteAll()

    clearLocalCustomCards(context)
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

    var fastRepo by remember { mutableStateOf<FastSettingsRepository?>(null) }

    LaunchedEffect(Unit) {
        withContext(kotlinx.coroutines.Dispatchers.IO) {
            val db = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "tap_talk_db"
            ).build()
            fastRepo = FastSettingsRepository(db.fastSettingsDao(), db.historyDao())
        }
    }

    val coroutineScope = rememberCoroutineScope()

    val ttsState = remember { mutableStateOf<TextToSpeech?>(null) }


    val voicesState = remember { mutableStateOf<List<Voice>>(emptyList()) }
    val voices = voicesState.value.map { it.name }


    LaunchedEffect(Unit) {
        var engine: TextToSpeech? = null

        engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsState.value = engine
                engine?.language = java.util.Locale.US

                voicesState.value = engine?.voices?.toList() ?: emptyList()
            } else {
                android.util.Log.e("TTS", "TTS init failed: $status")
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

    var voicePitch by remember { mutableFloatStateOf(1f) }
    var voiceSpeed by remember { mutableFloatStateOf(1f) }
    var smartReplyEnabled by remember { mutableStateOf(true) }
    var darkMode by remember { mutableStateOf(false) }
    var lowVisionMode by remember { mutableStateOf(false) }
    var gridSize by remember { mutableStateOf("Medium") }

    fun updateSetting(key: String, value: Any) {
        if (userId == null) return
        val ref = firestore.collection("USERS")
            .document(userId)
            .collection("Fast_Settings")
            .document("current")

        ref.set(mapOf(key to value), com.google.firebase.firestore.SetOptions.merge())
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

                val ttsEngine = ttsState.value

                //  States loaded from DB/Firestore
                var selectedVoice by remember { mutableStateOf<String?>(null) }

                // Load settings (pitch, speed, selectedVoice)
                LaunchedEffect(userId) {
                    if (userId != null) {
                        val doc = firestore.collection("USERS")
                            .document(userId)
                            .collection("Fast_Settings")
                            .document("current")
                            .get()
                            .await()

                        selectedVoice = doc.getString("selectedVoice")
                        voicePitch = (doc.getDouble("voicePitch") ?: 1.0).toFloat()
                        voiceSpeed = (doc.getDouble("voiceSpeed") ?: 1.0).toFloat()
                    }
                }

                // Apply voice once settings + voices are available
                LaunchedEffect(voicesState.value, selectedVoice) {
                    if (!selectedVoice.isNullOrEmpty() && voicesState.value.isNotEmpty()) {
                        val v = voicesState.value.firstOrNull { it.name == selectedVoice }
                            ?: voicesState.value.first()

                        selectedVoice = v.name
                        ttsEngine?.voice = v

                        // apply saved pitch/speed here after voice applied
                        ttsEngine?.setPitch(voicePitch)
                        ttsEngine?.setSpeechRate(voiceSpeed)
                    }
                }

                if (!selectedVoice.isNullOrEmpty()) {
                    SettingsDropdown(
                        title = "Voice",
                        options = voicesState.value.map { it.name },
                        selected = selectedVoice!!,
                        onSelect = { v ->

                            selectedVoice = v
                            updateSetting("selectedVoice", v)

                            val voiceObj = voicesState.value.firstOrNull { it.name == v }
                            ttsEngine?.voice = voiceObj

                            // Reset to saved defaults for new voice
                            voicePitch = 1f
                            voiceSpeed = 1f
                            updateSetting("voicePitch", 1f)
                            updateSetting("voiceSpeed", 1f)
                        }
                    )
                }

                // Pitch
                SettingsSlider(
                    title = "Pitch",
                    value = voicePitch,
                    onValueChange = {
                        voicePitch = it
                        ttsEngine?.setPitch(it)
                        updateSetting("voicePitch", it)
                    },
                    range = 0.5f..2f
                )

                // Speed
                SettingsSlider(
                    title = "Speed",
                    value = voiceSpeed,
                    onValueChange = {
                        voiceSpeed = it
                        ttsEngine?.setSpeechRate(it)
                        updateSetting("voiceSpeed", it)
                    },
                    range = 0.5f..2f
                )

                SettingsButton("Preview voice") {
                    ttsEngine?.speak(
                        "This is a preview of the selected voice.",
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "preview"
                    )
                }

                SettingsButton("Manage Voices") {
                    val intent = Intent("com.android.settings.TTS_SETTINGS")
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(
                            context,
                            "Unable to open TTS settings on this device",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            SettingsSection("Interface") {

                SettingsDropdown(
                    title = "Grid Size",
                    options = listOf("Small", "Medium", "Large"),
                    selected = gridSize,
                    onSelect = { newSize ->
                        gridSize = newSize
                        updateSetting("gridSize", newSize)

                        coroutineScope.launch {
                            val local = fastRepo?.getLocalSettings()
                            val updated = (local ?: FastSettingsEntity(
                                volume = 50f,
                                aiSupport = smartReplyEnabled,
                                gridSize = newSize
                            )).copy(gridSize = newSize)

                            fastRepo?.saveLocalSettings(updated)
                            scheduleFastSettingsSync(context)
                        }
                    }
                )

                var visibleLevels by remember { mutableStateOf(listOf("A1", "A2", "B1")) }

                LaunchedEffect(userId) {
                    if (userId != null) {
                        firestore.collection("USERS").document(userId)
                            .collection("Fast_Settings").document("current")
                            .addSnapshotListener { snapshot, _ ->
                                if (snapshot != null && snapshot.exists()) {
                                    val list = snapshot.get("visibleLevels") as? List<*>
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
                        val local = fastRepo?.getLocalSettings()
                        val updated = (local ?: FastSettingsEntity(
                            volume = 50f,
                            aiSupport = it
                        )).copy(aiSupport = it)

                        fastRepo?.saveLocalSettings(updated)
                        scheduleFastSettingsSync(context)
                    }
                }
            )

            SettingsSection("Custom Content") {
                SettingsButton("Manage Custom Words") {
                    val intent = Intent(context, CustomWordsManagerActivity::class.java)
                    context.startActivity(intent)
                }

                SettingsButton("Manage Custom Categories") {
                    val intent = Intent(context, CustomCategoriesManagerActivity::class.java)
                    context.startActivity(intent)
                }
            }


            // DATA SYNC
            SettingsSection("Data Sync") {
                var syncing by remember { mutableStateOf(false) }

                SettingsButton(
                    title = if (syncing) "Syncing..." else "Sync Now"
                ) {
                    syncing = true
                    coroutineScope.launch {
                        val repo = com.example.taptalk.data.HistoryRepository(context)
                        repo.syncToFirebase()

                        withContext(kotlinx.coroutines.Dispatchers.IO) {
                            com.example.taptalk.aac.data.CategoryRepository(context).restoreFromFirebase()
                            com.example.taptalk.aac.data.CustomWordsRepository(context).restoreFromFirebase()
                        }

                        syncing = false
                        android.widget.Toast.makeText(
                            context,
                            "☁️ Synced & Custom Items Restored!",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()


                        syncing = false
                        android.widget.Toast.makeText(
                            context,
                            "☁️ Synced & Restored!",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            // ACCOUNT
            SettingsSection("Account") {
                SettingsButton("Log Out") {
                    coroutineScope.launch {
                        clearLocalUserData(context)

                        FirebaseAuth.getInstance().signOut()

                        android.widget.Toast.makeText(
                            context,
                            "👋 Logged out and cleared local data",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()

                        val intent = Intent(context, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        context.startActivity(intent)
                    }
                }

                var showPasswordDialog by remember { mutableStateOf(false) }
                var password by remember { mutableStateOf("") }

                SettingsButton("Delete Account") {
                    showPasswordDialog = true
                }

                if (showPasswordDialog) {
                    AlertDialog(
                        onDismissRequest = { showPasswordDialog = false },
                        title = { Text("Confirm Deletion") },
                        text = {
                            Column {
                                Text("Please enter your password to confirm account deletion:")
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = password,
                                    onValueChange = { password = it },
                                    label = { Text("Password") },
                                    singleLine = true,
                                    visualTransformation = PasswordVisualTransformation()
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                val user = FirebaseAuth.getInstance().currentUser
                                if (user != null && user.email != null) {
                                    val credential =
                                        EmailAuthProvider.getCredential(user.email!!, password)

                                    user.reauthenticate(credential)
                                        .addOnCompleteListener { reauthTask ->
                                            if (reauthTask.isSuccessful) {
                                                user.delete().addOnCompleteListener { task ->
                                                    if (task.isSuccessful) {
                                                        val userId =
                                                            FirebaseAuth.getInstance().currentUser?.uid
                                                        if (userId != null) {
                                                            FirebaseFirestore.getInstance()
                                                                .collection("USERS")
                                                                .document(userId)
                                                                .delete()
                                                        }
                                                        android.widget.Toast.makeText(
                                                            context,
                                                            "🗑️ Account deleted",
                                                            android.widget.Toast.LENGTH_SHORT
                                                        ).show()
                                                        context.startActivity(
                                                            Intent(
                                                                context,
                                                                LoginActivity::class.java
                                                            )
                                                        )
                                                    } else {
                                                        android.widget.Toast.makeText(
                                                            context,
                                                            "❌ Failed to delete account",
                                                            android.widget.Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                }
                                            } else {
                                                android.widget.Toast.makeText(
                                                    context,
                                                    "🔐 Wrong password — try again",
                                                    android.widget.Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                }
                                showPasswordDialog = false
                            }) {
                                Text("Delete", color = Color.Red)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showPasswordDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }

            // ABOUT
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
