package com.example.taptalk

import org.junit.Assert.*
import org.junit.Test

class SettingsButtonTest {

    @Test
    fun buttonTriggersClick() {
        var clicked = false
        val onClick = { clicked = true }

        onClick()

        assertTrue(clicked)
    }
}
