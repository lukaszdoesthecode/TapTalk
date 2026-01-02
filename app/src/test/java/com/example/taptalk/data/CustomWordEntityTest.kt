package com.example.taptalk.data

import org.junit.Assert.assertEquals
import org.junit.Test

class CustomWordEntityTest {

    @Test
    fun `entity should store fields correctly`() {
        val entity = CustomWordEntity(
            label = "apple",
            folder = "fruits",
            imagePath = "/images/apple.png",
            synced = true,
            userId = "user123"
        )

        assertEquals("apple", entity.label)
        assertEquals("fruits", entity.folder)
        assertEquals("/images/apple.png", entity.imagePath)
        assertEquals(true, entity.synced)
        assertEquals("user123", entity.userId)
    }

    @Test
    fun `default values should be applied`() {
        val entity = CustomWordEntity(
            label = "dog",
            folder = "animals"
        )

        assertEquals("dog", entity.label)
        assertEquals("animals", entity.folder)
        assertEquals(null, entity.imagePath)
        assertEquals(false, entity.synced)
        assertEquals("", entity.userId)
    }
}
