package com.example.taptalk.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserCategoryDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: UserCategoryDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            androidx.test.core.app.ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()

        dao = db.userCategoryDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun insertAndRead() = runBlocking {
        val uid = "userA"

        val entity = UserCategoryEntity(
            name = "TestCat",
            colorHex = "#123456",
            imagePath = null,
            cardFileNames = listOf("x.png"),
            userId = uid
        )

        dao.insertOrUpdate(entity)

        val all = dao.getAll(uid)
        assertEquals(1, all.size)
        assertEquals("TestCat", all[0].name)
    }
}
