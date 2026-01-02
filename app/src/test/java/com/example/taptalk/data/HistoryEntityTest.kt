package com.example.taptalk.data

import org.junit.Assert.assertEquals
import org.junit.Test

class HistoryEntityTest {

    @Test
    fun `entity should store fields correctly`() {
        val now = 123456789L

        val entity = HistoryEntity(
            id = 5,
            sentence = "hello world",
            timestamp = now,
            userId = "user123"
        )

        assertEquals(5, entity.id)
        assertEquals("hello world", entity.sentence)
        assertEquals(now, entity.timestamp)
        assertEquals("user123", entity.userId)
    }

    @Test
    fun `default values should be applied`() {
        val entity = HistoryEntity(sentence = "test")

        assertEquals(0, entity.id)

        assert(entity.timestamp > 0)

        assertEquals("", entity.userId)
    }
}
