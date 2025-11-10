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
 * Integration tests for [FastSettingsRepository].
 *
 * These tests verify that repository correctly interacts with
 * the local database through [FastSettingsDao].
 */
@RunWith(AndroidJUnit4::class)
class FastSettingsRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var fastSettingsDao: FastSettingsDao
    private lateinit var repository: FastSettingsRepository

    @Before
    fun setUp() {
        // Create in-memory database
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        fastSettingsDao = db.fastSettingsDao()
        repository = FastSettingsRepository(fastSettingsDao, db.historyDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun saveLocalSettings_savesToDatabaseAndMarksUnsynced() = runBlocking {
        val settings = FastSettingsEntity(
            id = 1,
            selectedVoice = "Emma",
            isSynced = true // intentionally true
        )

        // Save settings using repository
        repository.saveLocalSettings(settings)

        // Retrieve from DAO
        val stored = fastSettingsDao.getSettings()

        assertNotNull(stored)
        assertEquals("Emma", stored?.selectedVoice)
        // Should be marked unsynced (false) by repository
        assertEquals(false, stored?.isSynced)
    }

    @Test
    fun getLocalSettings_returnsPreviouslySavedSettings() = runBlocking {
        val settings = FastSettingsEntity(
            id = 1,
            selectedVoice = "Liam",
            isSynced = false
        )

        fastSettingsDao.insertOrUpdate(settings)

        val result = repository.getLocalSettings()

        assertNotNull(result)
        assertEquals("Liam", result?.selectedVoice)
        assertEquals(false, result?.isSynced)
    }
}
