package com.example.taptalk

import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.imageLoader
import com.example.taptalk.acc.AccUserSettings
import com.example.taptalk.ui.acc.AccScreen
import com.example.taptalk.ui.theme.TapTalkTheme
import com.example.taptalk.viewmodel.AccViewModel
import com.google.mlkit.nl.smartreply.SmartReply
import kotlinx.coroutines.launch
import java.util.Locale

private val TTS_VOICE_MAP = mapOf(
    "Kate" to listOf("female", "en-us-x-sfg", "en-gb-x-fis", "f1"),
    "Josh" to listOf("male", "en-us-x-tpd", "en-gb-x-rjs", "m1"),
    "Sabrina" to listOf("female", "child", "en-us-x-sfg#female_2", "en-in-x-cxx"),
    "Sami" to listOf("male", "child", "en-us-x-tpd#male_2", "en-in-x-ism")
)

class AccActivity : ComponentActivity() {

    private val viewModel: AccViewModel by viewModels()
    private var tts: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val smartReply = SmartReply.getClient()

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                val voice = pickTtsVoice(viewModel.uiState.value.settings.selectedVoice)
                if (voice != null) {
                    tts?.voice = voice
                }
            }
        }

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    val voice = pickTtsVoice(state.settings.selectedVoice)
                    if (voice != null) {
                        tts?.voice = voice
                    }
                }
            }
        }

        setContent {
            val uiState by viewModel.uiState.collectAsState()

            TapTalkTheme(
                darkMode = uiState.settings.darkMode,
                lowVision = uiState.settings.lowVisionMode
            ) {
                AccScreen(
                    state = uiState,
                    imageLoader = imageLoader,
                    smartReply = smartReply,
                    events = viewModel.events,
                    onSpeak = { sentence -> speak(sentence, uiState.settings) },
                    onToggleFavourite = { card -> viewModel.toggleFavourite(card) },
                    onRequestFavourites = { viewModel.refreshFavourites() }
                )
            }
        }
    }

    private fun speak(text: String, settings: AccUserSettings) {
        if (text.isBlank()) return

        if (settings.autoSpeak) {
            val voice = pickTtsVoice(settings.selectedVoice)
            if (voice != null) {
                tts?.voice = voice
            }
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ACC_UTTERANCE")
        }

        viewModel.onSentenceSpoken(text)
    }

    private fun pickTtsVoice(friendlyName: String): android.speech.tts.Voice? {
        val keywords = TTS_VOICE_MAP[friendlyName] ?: return null
        val allVoices = tts?.voices ?: return null

        return allVoices.firstOrNull { voice ->
            keywords.any { key -> voice.name.contains(key, ignoreCase = true) }
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
