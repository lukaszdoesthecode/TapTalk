package com.example.taptalk.aac.data

import junit.framework.TestCase.assertEquals
import org.junit.Test

class TrimCategoryNameTest {

    @Test
    fun `trimCategoryName cleans file names correctly`() {
        assertEquals("Nouns", trimCategoryName("nouns_category.png"))
        assertEquals("Home", trimCategoryName("home_cathegory.jpg"))
        assertEquals("My label", trimCategoryName("my_label.png"))
    }
}
