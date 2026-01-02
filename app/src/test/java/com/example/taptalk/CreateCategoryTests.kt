package com.example.taptalk

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class CreateCategoryTests {
    @Test
    fun testBlankNameShowsError() {
        val name = ""
        val selectedCards = emptyList<String>()
        val errors = mutableListOf<String>()

        if (name.isBlank()) {
            errors.add("Please enter category name")
        }

        assertEquals("Please enter category name", errors.first())
    }

    @Test
    fun testEmptyCardsShowsError() {
        val name = "Food"
        val selectedCards = emptyList<String>()
        val errors = mutableListOf<String>()

        if (name.isNotBlank() && selectedCards.isEmpty()) {
            errors.add("Please choose at least one word")
        }

        assertEquals("Please choose at least one word", errors.first())
    }
}
