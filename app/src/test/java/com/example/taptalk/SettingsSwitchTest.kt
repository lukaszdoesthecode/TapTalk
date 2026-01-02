package com.example.taptalk

import org.junit.Assert.*
import org.junit.Test

class SettingsSwitchTest {

    @Test
    fun switchCallbackFires() {
        var changedTo: Boolean? = null

        val callback = { newValue: Boolean ->
            changedTo = newValue
        }

        callback(true)
        assertEquals(true, changedTo)

        callback(false)
        assertEquals(false, changedTo)
    }
}
