package com.example.taptalk.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class HistoryDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: HistoryDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        dao = db.historyDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `insert and getAll returns stored history items`() = runBlocking {
        val item1 = HistoryEntity(
            sentence = "Hello",
            timestamp = 1L,
            userId = "user1"
        )

        val item2 = HistoryEntity(
            sentence = "World",
            timestamp = 2L,
            userId = "user1"
        )

        dao.insert(item1)
        dao.insert(item2)

        val result = dao.getAll("user1")

        assertEquals(2, result.size)
        assertEquals("Hello", result[0].sentence)
        assertEquals("World", result[1].sentence)
    }

    @Test
    fun `getRecent returns latest 15 items`() = runBlocking {
        // insert 20 items
        for (i in 1..20) {
            dao.insert(
                HistoryEntity(
                    sentence = "Msg $i",
                    timestamp = i.toLong(),
                    userId = "user1"
                )
            )
        }

        val recent = dao.getRecent("user1")

        assertEquals(15, recent.size)
        assertEquals("Msg 20", recent[0].sentence) // newest
        assertEquals("Msg 6", recent.last().sentence) // oldest of the returned
    }

    @Test
    fun `deleteAll removes all user history`() = runBlocking {
        dao.insert(
            HistoryEntity(
                sentence = "DeleteMe",
                timestamp = 1L,
                userId = "user1"
            )
        )

        dao.deleteAll("user1")

        val result = dao.getAll("user1")
        assertTrue(result.isEmpty())
    }
}
