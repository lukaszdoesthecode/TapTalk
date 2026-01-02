package com.example.taptalk.aac.data

import android.content.Context
import android.content.res.AssetManager
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import org.junit.Test

/**
 * This test class verifies that AAC (Augmentative and Alternative Communication) cards are correctly
 * loaded and parsed from the application's assets folder. It mocks the Android [Context] and [AssetManager]
 * to simulate a file structure containing images nested within category directories (e.g., "ACC_board/actions"
 * and "categories/things").
 *
 * It asserts that:
 * - The correct number of cards are identified.
 * - File names, generated labels, file paths, and folder categories are correctly extracted and formatted.
 */
class LoadAccCardsTest {

    @Test
    fun `loadAccCards returns correct cards from nested assets`() {
        val context = mockk<Context>()
        val assets = mockk<AssetManager>()

        every { context.assets } returns assets

        every { assets.list("ACC_board") } returns arrayOf("actions")
        every { assets.list("ACC_board/actions") } returns arrayOf("run.png")
        every { assets.list("ACC_board/actions/run.png") } returns null

        every { assets.list("categories") } returns arrayOf("things")
        every { assets.list("categories/things") } returns arrayOf("car.jpg")
        every { assets.list("categories/things/car.jpg") } returns null

        val result = loadAccCards(context)

        assertEquals(2, result.size)

        val first = result[0]
        assertEquals("run.png", first.fileName)
        assertEquals("Run", first.label)
        assertEquals("file:///android_asset/ACC_board/actions/run.png", first.path)
        assertEquals("actions", first.folder)

        val second = result[1]
        assertEquals("car.jpg", second.fileName)
        assertEquals("Car", second.label)
        assertEquals("file:///android_asset/categories/things/car.jpg", second.path)
        assertEquals("things", second.folder)
    }
}
