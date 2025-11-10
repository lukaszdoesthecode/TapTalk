package com.example.taptalk.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

@RunWith(AndroidJUnit4::class)
class HistoryDatabaseTest {

    private lateinit var db: HistoryDatabase
    private lateinit var dao: HistoryDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            HistoryDatabase::class.java
        )
            .allowMainThreadQueries() // fine for tests
            .build()

        dao = db.historyDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun insertAndGetAll_shouldReturnItemsInAscendingOrder() {
        val entries = listOf(
            HistoryEntity(sentence = "A", timestamp = 1L),
            HistoryEntity(sentence = "B", timestamp = 2L),
            HistoryEntity(sentence = "C", timestamp = 3L)
        )

        entries.forEach { dao.insert(it) }

        val all = dao.getAll()
        assertEquals(3, all.size)

        // ascending by timestamp
        assertTrue(all.zipWithNext().all { (first, second) ->
            first.timestamp <= second.timestamp
        })

        // check content
        assertEquals(listOf("A", "B", "C"), all.map { it.sentence })
    }

    @Test
    fun getRecent_shouldReturn15LatestItemsInDescendingOrder() {
        // insert 20 records with increasing timestamps
        (1..20).forEach { i ->
            dao.insert(
                HistoryEntity(
                    sentence = "Sentence $i",
                    timestamp = i.toLong()
                )
            )
        }

        val recent = dao.getRecent()

        // expect 15 latest
        assertEquals(15, recent.size)

        // descending order by timestamp
        assertTrue(recent.zipWithNext().all { (first, second) ->
            first.timestamp >= second.timestamp
        })

        // newest (20) at top, oldest in that range (6) at bottom
        assertEquals(20L, recent.first().timestamp)
        assertEquals(6L, recent.last().timestamp)
    }
}
