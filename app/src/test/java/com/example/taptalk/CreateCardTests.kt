package com.example.taptalk

import android.content.Context
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, shadows = [ShadowFirebaseApp::class])
class CreateCardTests {

    @Test
    fun testBorderColorForCategory_defaultCase() {
        assertEquals(Color.Black, borderColorForCategory("does_not_exist"))
    }

    @Test
    fun testSaveCard_userIdIsNull() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val uri = Uri.parse("content://test/image.jpg")
        var message = ""

        saveCardLocallyAndToFirebase(
            context = context,
            imageUri = uri,
            label = "testLabel",
            selectedCategory = "nouns",
            userId = null,
            onResult = { message = it }
        )

        assertEquals("Please sign in first!", message)
    }

    @Test
    fun testSaveCard_localSaveFails() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        var message = ""

        saveCardLocallyAndToFirebase(
            context = context,
            imageUri = Uri.parse("content://whatever"),
            label = "banana",
            selectedCategory = "verbs",
            userId = null,
            onResult = { message = it }
        )

        assertEquals("Please sign in first!", message)
    }}

