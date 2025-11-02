package com.example.taptalk.data

import androidx.room.*

/**
 * Data Access Object (DAO) for the [FastSettingsEntity].
 * This interface provides methods for interacting with the `fast_settings` table in the database.
 * Since fast settings are a singleton entity within the app, most operations target the row with a fixed ID of 1.
 */
@Dao
interface FastSettingsDao {
    @Query("SELECT * FROM fast_settings WHERE id = 1")
    suspend fun getSettings(): FastSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(settings: FastSettingsEntity)

    @Query("UPDATE fast_settings SET isSynced = :synced WHERE id = 1")
    suspend fun updateSyncStatus(synced: Boolean)
}
