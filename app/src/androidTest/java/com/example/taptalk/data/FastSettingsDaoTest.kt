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
 * Tests for the [FastSettingsDao] interface.
 * These tests verify basic CRUD functionality and correct update behavior.
 */
@RunWith(AndroidJUnit4::class)
class FastSettingsDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: FastSettingsDao

    @Before
    fun setUp() {
        // Build an in-memory database that will be destroyed after each test
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.fastSettingsDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertAndRetrieveSettings() = runBlocking {
        // Insert a sample settings entity
        val entity = FastSettingsEntity(
            id = 1,
            volume = 70f,
            selectedVoice = "Ava",
            aiSupport = true,
            autoSpeak = true,
            isSynced = false,
            gridSize = "Medium"
        )

        dao.insertOrUpdate(entity)

        // Retrieve the same entity
        val result = dao.getSettings()

        // Verify that data is persisted correctly
        assertNotNull(result)
        assertEquals("Ava", result?.selectedVoice)
        assertEquals(70f, result?.volume)
        assertEquals(false, result?.isSynced)
    }

    @Test
    fun updateSyncStatus_changesIsSyncedFlag() = runBlocking {
        // Insert a settings entity with isSynced = false
        val entity = FastSettingsEntity(id = 1, isSynced = false)
        dao.insertOrUpdate(entity)

        // Update the sync flag to true
        dao.updateSyncStatus(true)

        // Retrieve updated entity
        val updated = dao.getSettings()

        // Verify that the flag has been updated
        assertNotNull(updated)
        assertEquals(true, updated?.isSynced)
    }
}
