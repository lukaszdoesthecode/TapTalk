package com.example.taptalk.aac.data

import android.content.Context
import android.content.res.AssetManager
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import org.junit.Test

/**
 * Unit tests for verifying the functionality of loading and sorting AAC categories.
 *
 * This test class mocks the Android [Context] and [AssetManager] to simulate
 * reading category image files from the app's assets. It specifically ensures that:
 * - The `loadCategories` function correctly parses file names.
 * - The resulting list of categories is sorted in a specific, pre-defined order
 *   (e.g., "Home" before "Pronouns", "Pronouns" before "Verbs").
 */
class LoadCategoriesTest {

    @Test
    fun `loadCategories returns items in desired order`() {
        val context = mockk<Context>()
        val assets = mockk<AssetManager>()

        every { context.assets } returns assets

        every { assets.list("ACC_board/categories") } returns arrayOf(
            "verbs_category.png",
            "home_category.png",
            "pronouns_category.png"
        )

        val result = loadCategories(context)

        assertEquals(3, result.size)

        assertEquals("Home", result[0].label)
        assertEquals("Pronouns", result[1].label)
        assertEquals("Verbs", result[2].label)
    }
}
