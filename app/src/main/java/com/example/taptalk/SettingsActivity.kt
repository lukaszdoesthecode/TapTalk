package com.example.taptalk

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // --- States ---
    var darkMode by remember { mutableStateOf(false) }
    var autoSpeak by remember { mutableStateOf(true) }
    var voicePitch by remember { mutableStateOf(1f) }
    var voiceSpeed by remember { mutableStateOf(1f) }
    var smartReplyEnabled by remember { mutableStateOf(true) }
    var showLabels by remember { mutableStateOf(true) }
    var gridSize by remember { mutableStateOf("Medium") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontSize = 22.sp) },
                navigationIcon = {
                    IconButton(onClick = { (context as? ComponentActivity)?.finish() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
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
            // --- Section: Voice & Speech ---
            SettingsSection("Voice & Speech") {
                SettingsSwitch(
                    title = "Auto-speak selected words",
                    checked = autoSpeak,
                    onCheckedChange = { autoSpeak = it }
                )
                SettingsSlider(
                    title = "Voice Pitch",
                    value = voicePitch,
                    onValueChange = { voicePitch = it },
                    range = 0.5f..2f
                )
                SettingsSlider(
                    title = "Voice Speed",
                    value = voiceSpeed,
                    onValueChange = { voiceSpeed = it },
                    range = 0.5f..2f
                )
            }

            // --- Section: Interface ---
            SettingsSection("Interface") {
                SettingsSwitch("Dark Mode", darkMode) { darkMode = it }
                SettingsSwitch("Show labels under icons", showLabels) { showLabels = it }
                SettingsDropdown(
                    title = "Grid Size",
                    options = listOf("Small", "Medium", "Large"),
                    selected = gridSize,
                    onSelect = { gridSize = it }
                )
            }

            // --- Section: Smart Replies ---
            SettingsSection("Smart Replies") {
                SettingsSwitch("Enable Smart Replies", smartReplyEnabled) { smartReplyEnabled = it }
            }

            // --- Section: Backup ---
            SettingsSection("Backup") {
                SettingsButton("Export custom cards") { /* TODO */ }
                SettingsButton("Import from file") { /* TODO */ }
            }

            // --- Section: About ---
            SettingsSection("About") {
                Text("TapTalk AAC App", fontSize = 16.sp)
                Text("Version 1.0", color = Color.Gray)
                SettingsButton("Contact Developer") {
                    // TODO: open mail intent
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

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

@Composable
fun SettingsSlider(title: String, value: Float, onValueChange: (Float) -> Unit, range: ClosedFloatingPointRange<Float>) {
    Column {
        Text(title)
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range
        )
    }
}

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
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
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
