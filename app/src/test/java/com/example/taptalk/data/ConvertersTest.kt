package com.example.taptalk.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun `fromStringList converts list to string`() {
        val list = listOf("apple", "banana", "cherry")
        val result = converters.fromStringList(list)
        assertEquals("apple|banana|cherry", result)
    }

    @Test
    fun `fromStringList handles null`() {
        val result = converters.fromStringList(null)
        assertEquals(null, result)
    }

    @Test
    fun `toStringList converts string to list`() {
        val input = "apple|banana|cherry"
        val result = converters.toStringList(input)
        assertEquals(listOf("apple", "banana", "cherry"), result)
    }

    @Test
    fun `toStringList handles empty string`() {
        val result = converters.toStringList("")
        assertEquals(emptyList<String>(), result)
    }
}
