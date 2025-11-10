package com.example.taptalk.data

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for the [HistoryEntity] data class.
 *
 * Since this is a data holder, we’re verifying:
 * - field assignment
 * - default timestamp generation
 * - equals() / hashCode() / toString() contract
 */
class HistoryEntityTest {

    @Test
    fun createEntity_shouldStoreProvidedValues() {
        val entity = HistoryEntity(
            id = 5,
            sentence = "Hello world",
            timestamp = 123456L
        )

        assertEquals(5, entity.id)
        assertEquals("Hello world", entity.sentence)
        assertEquals(123456L, entity.timestamp)
    }

    @Test
    fun defaultTimestamp_shouldBeRecent() {
        val before = System.currentTimeMillis()
        val entity = HistoryEntity(sentence = "Auto timestamp")
        val after = System.currentTimeMillis()

        // Timestamp should be between before and after
        assertTrue(entity.timestamp in before..after)
    }

    @Test
    fun equalsAndHashCode_shouldBeConsistent() {
        val e1 = HistoryEntity(id = 1, sentence = "Same", timestamp = 1000L)
        val e2 = HistoryEntity(id = 1, sentence = "Same", timestamp = 1000L)
        val e3 = HistoryEntity(id = 2, sentence = "Diff", timestamp = 2000L)

        assertEquals(e1, e2)
        assertEquals(e1.hashCode(), e2.hashCode())
        assertNotEquals(e1, e3)
    }

    @Test
    fun toString_shouldContainFieldValues() {
        val entity = HistoryEntity(id = 42, sentence = "Test", timestamp = 999L)
        val string = entity.toString()

        assertTrue(string.contains("42"))
        assertTrue(string.contains("Test"))
        assertTrue(string.contains("999"))
    }
}
