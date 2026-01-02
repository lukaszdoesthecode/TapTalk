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
class FastSettingsDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: FastSettingsDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        dao = db.fastSettingsDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `insert and getSettings returns stored settings`() = runBlocking {
        val settings = FastSettingsEntity(
            id = 0,
            isSynced = false,
            userId = "user1"
        )

        dao.insertOrUpdate(settings)

        val result = dao.getSettings("user1")

        assertNotNull(result)
    }

    @Test
    fun `updateSyncStatus updates the synced field`() = runBlocking {
        val settings = FastSettingsEntity(
            id = 0,
            isSynced = false,
            userId = "user1"
        )

        dao.insertOrUpdate(settings)

        dao.updateSyncStatus("user1", true)

        val result = dao.getSettings("user1")
        assertEquals(true, result?.isSynced)
    }

    @Test
    fun `deleteAll removes all settings for user`() = runBlocking {
        dao.insertOrUpdate(
            FastSettingsEntity(
                id = 0,
                userId = "user1"
            )
        )

        dao.deleteAll("user1")

        val result = dao.getSettings("user1")
        assertNull(result)
    }
}
