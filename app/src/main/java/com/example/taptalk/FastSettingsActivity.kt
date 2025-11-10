package com.example.taptalk

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.room.Room
import com.example.taptalk.data.*
import com.example.taptalk.ui.components.BottomNavBar
import com.example.taptalk.viewmodel.FastSettingsViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.SetOptions

/**
 * An activity that provides a user interface for quickly adjusting application settings.
 * This screen allows users to configure options such as sound volume, voice selection,
 * and AI support features.
 *
 * The activity sets up the main content view using Jetpack Compose, displaying the
 * [FastSettingsScreen] composable, which contains the actual UI elements for the settings.
 */
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

/**
 * Safely updates a single setting field in a user's "Fast_Settings" document in Firestore.
 *
 * This function attempts to update a specific key-value pair in the user's settings document.
 * If the document does not exist, it catches the exception and creates the document
 * with the provided key-value pair instead, ensuring the setting is saved either way.
 *
 * @param userId The unique identifier of the user whose setting is being updated.
 * @param key The name of the setting field to update (e.g., "volume", "aiSupport").
 * @param value The new value for the setting.
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

/**
 * A composable that creates a styled container box for a settings section.
 *
 * This box includes a title, an icon, and a designated content area. The box has a
 * specific background color, rounded corners, and a border. It's designed to
 * structure different settings categories in a visually consistent way.
 *
 * @param title The text to be displayed as the title of the section.
 * @param iconId The resource ID of the drawable to be used as the section's icon.
 * @param content A composable lambda that defines the content to be displayed
 *                within the main body of the section box. This lambda has a `ColumnScope`
 *                receiver, allowing content to be placed vertically.
 */
@Composable
fun SectionBox(title: String, iconId: Int, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .height(220.dp)
            .background(Color(0xFF6A1B9A), RoundedCornerShape(16.dp))
            .border(3.dp, Color.Black, RoundedCornerShape(16.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = iconId),
                contentDescription = "$title icon",
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = Color.White
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            content()
        }
    }
}

/**
 * A composable that displays a single voice option as a clickable card.
 *
 * This card shows an image and the name of the voice. It visually indicates
 * whether it is the currently selected option by displaying a border. Tapping on the
 * card triggers the provided `onClick` lambda.
 *
 * @param name The name of the voice to display.
 * @param imageRes The drawable resource ID for the voice's representative image.
 * @param selected A boolean indicating if this voice option is currently selected.
 * @param onClick A lambda function to be executed when the voice option is clicked.
 */
@Composable
fun VoiceOption(name: String, imageRes: Int, selected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .size(120.dp)
            .border(
                width = 4.dp,
                color = if (selected) Color.Black else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .background(Color.White, RoundedCornerShape(12.dp))
            .padding(8.dp)
    ) {
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = name,
            modifier = Modifier.size(70.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(name, fontSize = 18.sp, fontWeight = FontWeight.Medium)
    }
}

/**
 * A composable that displays a selectable card for an AI-related option.
 *
 * This card presents a binary choice (e.g., "Yes" or "No") with a corresponding
 * image and name. It highlights the selection with a border. Tapping the card
 * triggers the provided `onClick` lambda to update the selection state.
 *
 * @param name The text to display for the option (e.g., "Yes", "No").
 * @param imageRes The drawable resource ID for the image representing the option.
 * @param selected A boolean indicating whether this option is currently selected.
 * @param onClick A lambda function to be executed when the option is clicked.
 */
@Composable
fun AiOption(name: String, imageRes: Int, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(130.dp, 110.dp)
            .border(
                width = 4.dp,
                color = if (selected) Color.Black else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .background(Color.White, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = name,
                modifier = Modifier.size(70.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * A composable screen that provides a user interface for quickly configuring key app settings.
 *
 * This screen handles the presentation and logic for adjusting sound volume, selecting a text-to-speech
 * voice, and enabling or disabling AI support. It integrates with Firebase for user authentication
 * and for fetching and saving settings to Firestore. It also initializes a local Room database
 * and a `FastSettingsViewModel` to manage the settings data and business logic.
 *
 * The UI is structured into distinct sections for "SOUND", "VOICE", and "AI SUPPORT", each
 * presented within a `SectionBox`. User interactions, such as moving a slider or selecting
 * an option, trigger updates to both the local state and the remote Firestore database.
 * The screen ensures that a user is logged in before displaying the settings; otherwise, it
 * shows a "User not logged in" message.
 */
@Composable
fun FastSettingsScreen() {
    val context = LocalContext.current
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

    var volume by remember { mutableStateOf(viewModel.volume) }
    var selectedVoice by remember { mutableStateOf(viewModel.selectedVoice) }
    var aiSupport by remember { mutableStateOf(viewModel.aiSupport) }

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val firestore = FirebaseFirestore.getInstance()
        val snap = firestore.collection("USERS")
            .document(userId)
            .collection("Fast_Settings")
            .document("current")
            .get()
            .await()

        if (snap.exists()) {
            volume = (snap.getDouble("volume") ?: 50.0).toFloat()
            selectedVoice = snap.getString("selectedVoice") ?: "Kate"
            aiSupport = snap.getBoolean("aiSupport") ?: true
        }
    }

    Scaffold(bottomBar = { BottomNavBar() }) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.White)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            //SOUND
            SectionBox(title = "SOUND", iconId = R.drawable.sound) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
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
                        onValueChange = { volume = it },
                        onValueChangeFinished = {
                            coroutineScope.launch {
                                viewModel.volume = volume
                                viewModel.saveSettings()
                            }
                        },
                        valueRange = 0f..100f,
                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
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

            //VOICE
            SectionBox(title = "VOICE", iconId = R.drawable.voice) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("Kate", "Sami", "Josh", "Sabrina").forEach { voice ->
                        val imageRes = when (voice) {
                            "Kate" -> R.drawable.kate
                            "Sami" -> R.drawable.sami
                            "Josh" -> R.drawable.josh
                            else -> R.drawable.trisha
                        }
                        VoiceOption(
                            name = voice,
                            imageRes = imageRes,
                            selected = selectedVoice == voice,
                            onClick = {
                                selectedVoice = voice
                                coroutineScope.launch {
                                    viewModel.selectedVoice = voice
                                    viewModel.saveSettings()
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            //AI SUPPORT
            SectionBox(title = "AI SUPPORT", iconId = R.drawable.ai) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    AiOption("No", R.drawable.no, selected = !aiSupport) {
                        aiSupport = false
                        coroutineScope.launch {
                            safeUpdateSetting(userId, "aiSupport", false)
                        }
                    }

                    AiOption("Yes", R.drawable.yes, selected = aiSupport) {
                        aiSupport = true
                        coroutineScope.launch {
                            safeUpdateSetting(userId, "aiSupport", true)
                        }
                    }
                }
            }
        }
    }
}