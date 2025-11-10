package com.example.taptalk.aacdata

import com.example.taptalk.aac.data.AccCard
import org.junit.Assert.*
import org.junit.Test

class AacModelsTest {

    @Test
    fun accCard_createsProperly_andEqualityWorks() {
        val card1 = AccCard("hello.mp3", "Hello", "/storage/hello.mp3", "Greetings")
        val card2 = card1.copy()

        assertEquals(card1, card2)
        assertEquals("Hello", card1.label)
        assertEquals("/storage/hello.mp3", card1.path)
    }

    @Test
    fun accCard_copyUpdatesFieldCorrectly() {
        val original = AccCard("dog.png", "Dog", "/files/dog.png", "Animals")
        val updated = original.copy(label = "Doggo")

        assertEquals("Doggo", updated.label)
        assertNotEquals(original, updated)
    }
}
