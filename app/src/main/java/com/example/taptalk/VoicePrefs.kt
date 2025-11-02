package com.example.taptalk

import android.content.Context
import androidx.core.content.edit

/**
 * Manages the storage and retrieval of user-selected voice preferences for Text-To-Speech (TTS).
 *
 * This object uses [android.content.SharedPreferences] to persist the voice name,
 * speech rate (speed), and pitch settings across application sessions.
 */
object VoicePrefs {
    private const val FILE = "fast_voice"
    private const val KEY_NAME = "voiceName"
    private const val KEY_SPEED = "voiceSpeed"
    private const val KEY_PITCH = "voicePitch"

    /**
     * Saves the selected voice preferences to SharedPreferences.
     *
     * @param context The application context.
     * @param voiceName The name of the selected voice, or null if none is selected.
     * @param speed The selected speech rate.
     * @param pitch The selected speech pitch.
     */
    fun save(context: Context, voiceName: String?, speed: Float, pitch: Float) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit {
                putString(KEY_NAME, voiceName)
                    .putFloat(KEY_SPEED, speed)
                    .putFloat(KEY_PITCH, pitch)
            }
    }

    /**
     * Represents the stored voice preferences.
     *
     * @property name The name of the selected voice engine.
     * @property speed The speech rate for the text-to-speech engine.
     * @property pitch The speech pitch for the text-to-speech engine.
     */
    data class Data(val name: String?, val speed: Float?, val pitch: Float?)

    /**
     * Reads the saved voice preferences from SharedPreferences.
     *
     * This function retrieves the voice name, speed, and pitch settings. If a value for speed
     * or pitch has not been previously saved, its corresponding property in the returned Data object
     * will be null.
     *
     * @param context The context used to access SharedPreferences.
     * @return A [Data] object containing the loaded voice preferences.
     */
    fun read(context: Context): Data {
        val sp = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        return Data(
            sp.getString(KEY_NAME, null),
            if (sp.contains(KEY_SPEED)) sp.getFloat(KEY_SPEED, 1f) else null,
            if (sp.contains(KEY_PITCH)) sp.getFloat(KEY_PITCH, 1f) else null
        )
    }
}
