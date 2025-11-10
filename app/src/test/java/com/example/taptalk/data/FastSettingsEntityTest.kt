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
    fun defaultConstructor_providesExpectedValues() {
        val entity = FastSettingsEntity()

        // Verify all default values
        assertEquals(0, entity.id)
        assertEquals(50f, entity.volume)
        assertEquals("Kate", entity.selectedVoice)
        assertTrue(entity.aiSupport)
        assertTrue(entity.autoSpeak)
        assertTrue(entity.isSynced)
        assertEquals("Medium", entity.gridSize)
    }

    @Test
    fun customConstructor_overridesValuesCorrectly() {
        val custom = FastSettingsEntity(
            id = 10,
            volume = 80f,
            selectedVoice = "Alex",
            aiSupport = false,
            autoSpeak = false,
            isSynced = false,
            gridSize = "Large"
        )

        // Verify all assigned values
        assertEquals(10, custom.id)
        assertEquals(80f, custom.volume)
        assertEquals("Alex", custom.selectedVoice)
        assertEquals(false, custom.aiSupport)
        assertEquals(false, custom.autoSpeak)
        assertEquals(false, custom.isSynced)
        assertEquals("Large", custom.gridSize)
    }
}
