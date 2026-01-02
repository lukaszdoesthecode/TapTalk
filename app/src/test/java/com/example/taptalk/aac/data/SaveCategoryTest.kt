package com.example.taptalk.aac.data

import android.content.ContentResolver
import android.content.Context
import android.content.res.AssetManager
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.*
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Test
import java.io.File

class AccFunctionsTest {

    // 1 USER NOT LOGGED IN
    @Test
    fun `saveCategory returns error when user not logged in`() {
        mockkStatic(FirebaseAuth::class)

        val mockAuth = mockk<FirebaseAuth>()
        every { FirebaseAuth.getInstance() } returns mockAuth
        every { mockAuth.currentUser } returns null

        val ctx = mockk<Context>(relaxed = true)
        var msg = ""
        saveCategoryLocallyAndToFirebase(ctx, null, "Cat", "#fff", emptyList()) { msg = it }

        assertEquals("User not logged in.", msg)
    }

    // 2 LOCAL IMAGE SAVING
    @Test
    fun `saveCategory saves image locally`() {
        val temp = createTempDir()
        val ctx = mockk<Context>(relaxed = true)
        every { ctx.filesDir } returns temp

        val bytes = "abc".toByteArray()
        val uri = mockk<Uri>()
        val resolver = mockk<ContentResolver>()
        every { ctx.contentResolver } returns resolver
        every { resolver.openInputStream(uri) } returns bytes.inputStream()

        mockFirebaseUser("123")

        saveCategoryLocallyAndToFirebase(ctx, uri, "Dog", "#000", emptyList()) {}

        val file = File(temp, "Custom_Categories/Dog.jpg")
        assertTrue(file.exists())
        assertEquals("abc", file.readText())
    }

    // 3 loadCustomCards loads PNG/JPG
    @Test
    fun `loadCustomCards loads jpg and png`() {
        val temp = createTempDir()
        val root = File(temp, "Custom_Words")
        val folder = File(root, "animals")
        folder.mkdirs()

        File(folder, "dog.png").writeText("x")
        File(folder, "cat.jpg").writeText("x")

        val ctx = mockk<Context>()
        every { ctx.filesDir } returns temp

        val list = loadCustomCards(ctx)

        assertEquals(2, list.size)
        assertEquals("Dog", list[0].label)
        assertEquals("animals", list[0].folder)
    }

    // 4 trimCategoryName
    @Test
    fun `trimCategoryName edge cases`() {
        assertEquals("Test", trimCategoryName("test.PNG"))
        assertEquals("Hello world", trimCategoryName("hello_world_category.png"))
        assertEquals("Abc", trimCategoryName("abc_cathegory.JPG"))
        assertEquals("Empty", trimCategoryName(" empty .jpg"))
    }

    // 5 loadAccCards nested assets
    @Test
    fun `loadAccCards loads nested asset structure`() {
        val ctx = mockk<Context>()
        val assets = mockk<AssetManager>()
        every { ctx.assets } returns assets

        every { assets.list("ACC_board") } returns arrayOf("actions")
        every { assets.list("ACC_board/actions") } returns arrayOf("run.png")
        every { assets.list("ACC_board/actions/run.png") } returns null

        every { assets.list("categories") } returns arrayOf("things")
        every { assets.list("categories/things") } returns arrayOf("car.jpg")
        every { assets.list("categories/things/car.jpg") } returns null

        val list = loadAccCards(ctx)

        assertEquals(2, list.size)
        assertEquals("Run", list[0].label)
        assertEquals("things", list[1].folder)
    }

    // 6 loadAccCards ignores non-image files
    @Test
    fun `loadAccCards ignores txt files`() {
        val ctx = mockk<Context>()
        val assets = mockk<AssetManager>()
        every { ctx.assets } returns assets

        every { assets.list("ACC_board") } returns arrayOf("actions")
        every { assets.list("ACC_board/actions") } returns arrayOf("note.txt")
        every { assets.list("ACC_board/actions/note.txt") } returns null

        every { assets.list("categories") } returns emptyArray()

        val result = loadAccCards(ctx)
        assertTrue(result.isEmpty())
    }

    // 7 loadAccCards handles empty folders
    @Test
    fun `loadAccCards handles empty folders`() {
        val ctx = mockk<Context>()
        val assets = mockk<AssetManager>()
        every { ctx.assets } returns assets

        every { assets.list("ACC_board") } returns arrayOf("emptyFolder")
        every { assets.list("ACC_board/emptyFolder") } returns emptyArray<String>()

        every { assets.list("categories") } returns emptyArray()

        assertTrue(loadAccCards(ctx).isEmpty())
    }

    // 8 loadCustomCards ignores non-image files
    @Test
    fun `loadCustomCards ignores txt files`() {
        val temp = createTempDir()
        val root = File(temp, "Custom_Words")
        root.mkdirs()

        val f = File(root, "animals")
        f.mkdirs()
        File(f, "note.txt").writeText("nope")

        val ctx = mockk<Context>()
        every { ctx.filesDir } returns temp

        assertTrue(loadCustomCards(ctx).isEmpty())
    }

    // 9 loadCustomCards returns empty when folder missing
    @Test
    fun `loadCustomCards returns empty when folder missing`() {
        val temp = createTempDir()
        val ctx = mockk<Context>()
        every { ctx.filesDir } returns temp

        assertTrue(loadCustomCards(ctx).isEmpty())
    }

    // 10 loadAccCards preserves folder names
    @Test
    fun `loadAccCards preserves original folder names`() {
        val ctx = mockk<Context>()
        val assets = mockk<AssetManager>()
        every { ctx.assets } returns assets

        every { assets.list("ACC_board") } returns arrayOf("MyFolder")
        every { assets.list("ACC_board/MyFolder") } returns arrayOf("pic.png")
        every { assets.list("ACC_board/MyFolder/pic.png") } returns null

        every { assets.list("categories") } returns emptyArray()

        val r = loadAccCards(ctx)
        assertEquals("myfolder", r[0].folder)
    }

    // 11 saveCategory handles local failure
    @Test
    fun `saveCategory handles local file failure`() {
        val ctx = mockk<Context>(relaxed = true)
        mockFirebaseUser("123")

        val uri = mockk<Uri>()
        val resolver = mockk<ContentResolver>()
        every { ctx.contentResolver } returns resolver
        every { resolver.openInputStream(uri) } throws RuntimeException("NO INPUT")

        every { ctx.filesDir } returns createTempDir()

        var msg = ""
        saveCategoryLocallyAndToFirebase(
            ctx, uri, "Fail", "#000", emptyList()
        ) { msg = it }

        assertTrue(msg.contains("Failed to save category icon locally"))
    }

    // HELPER
    private fun mockFirebaseUser(uid: String) {
        mockkStatic(FirebaseAuth::class)

        val mockAuth = mockk<FirebaseAuth>()
        val mockUser = mockk<FirebaseUser>()
        every { FirebaseAuth.getInstance() } returns mockAuth
        every { mockAuth.currentUser } returns mockUser
        every { mockUser.uid } returns uid
    }
}
