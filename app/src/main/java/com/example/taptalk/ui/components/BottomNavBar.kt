package com.example.taptalk.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.taptalk.AccActivity
import com.example.taptalk.CreateCardActivity
import com.example.taptalk.FastSettingsActivity
import com.example.taptalk.KeyboardActivity
import com.example.taptalk.SettingsActivity
import com.example.taptalk.ui.theme.Bar

/**
 * A Composable function that displays a persistent bottom navigation bar.
 * This bar provides quick access to different screens within the application.
 *
 * It contains five icon buttons:
 * - **Quick Settings:** Navigates to [FastSettingsActivity].
 * - **Keyboard:** Navigates to [KeyboardActivity].
 * - **Home:** Navigates to [AccActivity] (the main screen).
 * - **Create:** Navigates to [CreateCardActivity].
 * - **Settings:** Navigates to [SettingsActivity].
 */
@Composable
fun BottomNavBar() {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(Bar)
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        IconButton(onClick = {
            context.startActivity(
                android.content.Intent(context, FastSettingsActivity::class.java)
            )
        }) {
            Icon(Icons.Default.Tune, contentDescription = "Quick Settings")
        }

        IconButton(onClick = {
            context.startActivity(
                android.content.Intent(context, KeyboardActivity::class.java)
            )
        }) {
            Icon(Icons.Default.Keyboard, contentDescription = "Keyboard")
        }

        IconButton(onClick = {
            context.startActivity(
                android.content.Intent(context, AccActivity::class.java)
            )
        }) {
            Icon(Icons.Default.Home, contentDescription = "Home")
        }
        IconButton(onClick = {
            context.startActivity(
                android.content.Intent(context, CreateCardActivity::class.java)
            )
        }) {
            Icon(Icons.Default.Add, contentDescription = "Create")
        }
        IconButton(onClick = {
            context.startActivity(
                android.content.Intent(context, SettingsActivity::class.java)
            ) }) {
            Icon(Icons.Default.Settings, contentDescription = "Settings")
        }
    }
}