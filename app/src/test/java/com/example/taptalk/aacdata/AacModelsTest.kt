package com.example.taptalk.aacdata

import com.example.taptalk.aac.data.AccCard
import com.example.taptalk.aac.data.Category
import com.example.taptalk.aac.data.VerbForms
import org.junit.Assert.assertEquals
import org.junit.Test

class AccModelsTest {

    @Test
    fun `AccCard stores data correctly`() {
        val card = AccCard(
            fileName = "apple.png",
            label = "Apple",
            path = "/images/food/apple.png",
            folder = "Food"
        )

        assertEquals("apple.png", card.fileName)
        assertEquals("Apple", card.label)
        assertEquals("/images/food/apple.png", card.path)
        assertEquals("Food", card.folder)
    }

    @Test
    fun `VerbForms stores data and default negatives correctly`() {
        val verb = VerbForms(
            base = "eat",
            past = "ate",
            perfect = "eaten"
        )

        assertEquals("eat", verb.base)
        assertEquals("ate", verb.past)
        assertEquals("eaten", verb.perfect)
        assertEquals(emptyList<String>(), verb.negatives)
    }

    @Test
    fun `Category stores data correctly`() {
        val category = Category(
            label = "Animals",
            path = "/categories/animals"
        )

        assertEquals("Animals", category.label)
        assertEquals("/categories/animals", category.path)
    }
}
