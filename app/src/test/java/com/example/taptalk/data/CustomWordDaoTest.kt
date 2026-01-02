package com.example.taptalk.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class CustomWordDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: CustomWordDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        dao = db.customWordDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `insert and getAll works`() = runBlocking {
        val word = CustomWordEntity(
            label = "Hello",
            folder = "greetings",
            imagePath = "/tmp/hello.jpg",
            synced = false,
            userId = "user1"
        )

        dao.insertOrUpdate(word)

        val result = dao.getAll("user1")
        assertEquals(1, result.size)
        assertEquals("Hello", result[0].label)
    }

    @Test
    fun `delete removes only the selected word`() = runBlocking {
        val w1 = CustomWordEntity("A", "f", null, false, "user1")
        val w2 = CustomWordEntity("B", "f", null, false, "user1")

        dao.insertOrUpdate(w1)
        dao.insertOrUpdate(w2)

        dao.delete(w1)

        val result = dao.getAll("user1")
        assertEquals(1, result.size)
        assertEquals("B", result[0].label)
    }

    @Test
    fun `deleteAll removes all words for user`() = runBlocking {
        dao.insertOrUpdate(CustomWordEntity("X", "f", null, false, "user1"))
        dao.insertOrUpdate(CustomWordEntity("Y", "f", null, false, "user1"))

        dao.deleteAll("user1")

        val result = dao.getAll("user1")
        assertEquals(0, result.size)
    }
}
