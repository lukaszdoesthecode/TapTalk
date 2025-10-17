package com.example.taptalk

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    var volume by remember { mutableStateOf(65f) }
    var selectedVoice by remember { mutableStateOf("Kate") }
    var aiSupport by remember { mutableStateOf(true) }

    Scaffold(bottomBar = { BottomNavBar() }) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.White),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // SOUND
            SectionBox(title = "SOUND") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Quiet", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Slider(
                        value = volume,
                        onValueChange = { volume = it },
                        valueRange = 0f..100f,
                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                    )
                    Text("Loud", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = volume.toInt().toString(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            // VOICE
            SectionBox(title = "VOICE") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("Kate", "Sami", "Josh", "Sabrina").forEach { voice ->
                        VoiceOption(
                            name = voice,
                            selected = (selectedVoice == voice),
                            onClick = { selectedVoice = voice }
                        )
                    }
                }
            }

            // AI SUPPORT
            SectionBox(title = "AI SUPPORT") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    AiOption("No", selected = !aiSupport) { aiSupport = false }
                    AiOption("Yes", selected = aiSupport) { aiSupport = true }
                }
            }
        }
    }
}

@Composable
fun SectionBox(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF6A1B9A), RoundedCornerShape(8.dp))
            .border(2.dp, Color.Black, RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = Color.White
        )
        content()
    }
}

@Composable
fun VoiceOption(name: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .size(80.dp)
            .border(
                width = 3.dp,
                color = if (selected) Color.Black else Color.Transparent,
                shape = RoundedCornerShape(6.dp)
            )
            .clickable { onClick() }
            .background(Color.White, RoundedCornerShape(6.dp))
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color.LightGray, RoundedCornerShape(4.dp))
        )
        Text(name, fontSize = 14.sp)
    }
}

@Composable
fun AiOption(name: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(100.dp, 80.dp)
            .border(
                width = 3.dp,
                color = if (selected) Color.Black else Color.Transparent,
                shape = RoundedCornerShape(6.dp)
            )
            .clickable { onClick() }
            .background(Color.White, RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}