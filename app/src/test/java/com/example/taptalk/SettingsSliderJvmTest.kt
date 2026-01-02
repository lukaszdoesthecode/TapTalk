package com.example.taptalk

import androidx.compose.runtime.mutableStateOf
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsSliderJvmTest {

    @Test
    fun sliderCallbackUpdatesValue() {
        val state = mutableStateOf(1f)

        val callback = { v: Float -> state.value = v }

        callback(1.75f)

        assertEquals(1.75f, state.value)
    }
}
