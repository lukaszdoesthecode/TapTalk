package com.example.taptalk.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the [FastSettingsEntity] data class.
 * These tests verify that default values and property assignments work as expected.
 */
class FastSettingsEntityTest {

    @Test
    fun customConstructor_overridesValuesCorrectly() {
        val custom = FastSettingsEntity(
            id = 10,
            volume = 80f,
            selectedVoice = "Alex",
            aiSupport = false,
            isSynced = false,
            gridSize = "Large"
        )

        // Verify all assigned values
        assertEquals(10, custom.id)
        assertEquals(80f, custom.volume)
        assertEquals("Alex", custom.selectedVoice)
        assertEquals(false, custom.aiSupport)
        assertEquals(false, custom.isSynced)
        assertEquals("Large", custom.gridSize)
    }
}
