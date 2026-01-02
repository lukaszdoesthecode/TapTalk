package com.example.taptalk.aac.data

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import org.junit.Test
import java.io.File

/**
 * Unit tests for the custom card loading functionality.
 *
 * This test class verifies that `loadCustomCards` correctly traverses the application's
 * internal storage directory structure (specifically the "Custom_Words" folder) to identify
 * and parse valid image files (JPG and PNG) into card objects.
 *
 * It mocks the Android `Context` and creates a temporary file system to simulate:
 * - The existence of category folders (e.g., "animals").
 * - The presence of image files with supported extensions.
 * - The correct extraction of metadata (labels, folder names, filenames).
 */
class LoadCustomCardsTest {

    @Test
    fun `loadCustomCards loads jpg and png files`() {
        val tempDir = createTempDir()
        val customRoot = File(tempDir, "Custom_Words")
        customRoot.mkdirs()

        val animalsFolder = File(customRoot, "animals")
        animalsFolder.mkdirs()

        val dog = File(animalsFolder, "dog.png")
        val cat = File(animalsFolder, "cat.jpg")
        dog.writeText("fake")
        cat.writeText("fake")

        val context = mockk<Context>()
        every { context.filesDir } returns tempDir

        val result = loadCustomCards(context)

        assertEquals(2, result.size)

        val first = result.first()

        assertEquals("Dog", first.label)
        assertEquals("animals", first.folder)
        assertEquals("dog.png", first.fileName)
    }
}
