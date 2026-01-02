package com.example.taptalk

import org.junit.Assert.*
import org.junit.Test

class SettingsDropdownTest {

    @Test
    fun dropdownOnSelectUpdatesValue() {
        var selected = "Medium"
        val onSelect = { newValue: String -> selected = newValue }

        onSelect("Large")

        assertEquals("Large", selected)
    }
}
