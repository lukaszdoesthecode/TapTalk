package com.example.taptalk.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation tests for the AppDatabase class.
 * These tests verify that the database and its DAOs
 * are correctly created and can perform basic CRUD operations.
 */
@RunWith(AndroidJUnit4::class)
class AppDatabaseTest {

    private lateinit var db: AppDatabase
    private lateinit var fastSettingsDao: FastSettingsDao
    private lateinit var historyDao: HistoryDao

    @Before
    fun setUp() {
        // Create an in-memory version of the database.
        // It does not persist data on the device and is destroyed after each test.
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        // Retrieve DAO instances from the database
        fastSettingsDao = db.fastSettingsDao()
        historyDao = db.historyDao()
    }

    @After
    fun tearDown() {
        // Close the database after each test to free up resources
        db.close()
    }

    @Test
    fun testDatabaseCreation() {
        // Verify that the database and its DAOs are properly initialized
        assertNotNull(db)
        assertNotNull(fastSettingsDao)
        assertNotNull(historyDao)
    }

    @Test
    fun testInsertAndReadFastSettingsEntity() = runBlocking {
        // Create a sample entity to insert
        val entity = FastSettingsEntity(
            id = 1,
            volume = 75f,
            selectedVoice = "Alex",
            aiSupport = true,
            autoSpeak = false,
            isSynced = false,
            gridSize = "Large"
        )

        // Insert or update settings
        fastSettingsDao.insertOrUpdate(entity)

        // Retrieve the settings
        val settings = fastSettingsDao.getSettings()

        // Verify that the retrieved entity matches what was inserted
        assertNotNull(settings)
        assertEquals(75f, settings?.volume)
        assertEquals("Alex", settings?.selectedVoice)
        assertEquals(false, settings?.autoSpeak)
        assertEquals("Large", settings?.gridSize)
    }

    @Test
    fun testInsertAndReadHistoryEntity() = runBlocking {
        // Create a sample history record
        val entity = HistoryEntity(
            sentence = "Hello world",
            timestamp = System.currentTimeMillis()
        )

        // Insert directly through the DAO (you’ll need this method in HistoryDao)
        historyDao.insert(entity)

        // Retrieve all records
        val all = historyDao.getAll()

        // Check that the inserted record exists
        assertEquals(1, all.size)
        assertEquals("Hello world", all.first().sentence)
    }
}