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
import com.example.taptalk.viewmodel.FastSettingsViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

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

@Composable
fun FastSettingsScreen() {
    val context = LocalContext.current
    val db = remember {
        Room.databaseBuilder(context, AppDatabase::class.java, "tap_talk_db").build()
    }
    val repo = remember { FastSettingsRepository(db.fastSettingsDao(), db.historyDao()) }

    // 🔐 Get currently logged-in Firebase user
    val userId = FirebaseAuth.getInstance().currentUser?.uid

    if (userId == null) {
        // fallback if user somehow not logged in
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("User not logged in 🥺", color = Color.Red, fontSize = 20.sp)
        }
        return
    }

    val viewModel = remember { FastSettingsViewModel(repo, userId) }

    var volume by remember { mutableStateOf(viewModel.volume) }
    var selectedVoice by remember { mutableStateOf(viewModel.selectedVoice) }
    var aiSupport by remember { mutableStateOf(viewModel.aiSupport) }

    val coroutineScope = rememberCoroutineScope()

    // load last local settings
    LaunchedEffect(Unit) {
        val settings = db.fastSettingsDao().getSettings()
        settings?.let {
            volume = it.volume
            selectedVoice = it.selectedVoice
            aiSupport = it.aiSupport
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

            // 🎚 SOUND
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

            // 🗣 VOICE
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

            // 🤖 AI SUPPORT
            SectionBox(title = "AI SUPPORT", iconId = R.drawable.ai) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    AiOption("No", R.drawable.no, selected = !aiSupport) {
                        aiSupport = false
                        coroutineScope.launch {
                            viewModel.aiSupport = false
                            viewModel.saveSettings()
                        }
                    }
                    AiOption("Yes", R.drawable.yes, selected = aiSupport) {
                        aiSupport = true
                        coroutineScope.launch {
                            viewModel.aiSupport = true
                            viewModel.saveSettings()
                        }
                    }
                }
            }
        }
    }
}

// --- UI helpers from before ---

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
