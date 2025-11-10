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
class HistoryDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: HistoryDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        )
            .allowMainThreadQueries() // OK w testach
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

        // rosnąco po timestamp
        assertTrue(all.zipWithNext().all { (first, second) ->
            first.timestamp <= second.timestamp
        })

        // opcjonalnie: sprawdź, że zdania się zgadzają
        assertEquals(listOf("A", "B", "C"), all.map { it.sentence })
    }

    @Test
    fun getRecent_shouldReturn15LatestItemsInDescendingOrder() {
        // Wstaw 20 rekordów z rosnącym timestamp
        (1..20).forEach { i ->
            dao.insert(
                HistoryEntity(
                    sentence = "Sentence $i",
                    timestamp = i.toLong()
                )
            )
        }

        val recent = dao.getRecent()

        // powinno być dokładnie 15 rekordów
        assertEquals(15, recent.size)

        // malejąco po timestamp
        assertTrue(recent.zipWithNext().all { (first, second) ->
            first.timestamp >= second.timestamp
        })

        // najnowszy (20) na górze, najstarszy w tej piętnastce (6) na dole
        assertEquals(20L, recent.first().timestamp)
        assertEquals(6L, recent.last().timestamp)
    }
}
