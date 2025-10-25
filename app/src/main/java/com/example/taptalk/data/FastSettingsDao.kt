package com.example.taptalk.data

import androidx.room.*

@Dao
interface FastSettingsDao {
    @Query("SELECT * FROM fast_settings WHERE id = 1")
    suspend fun getSettings(): FastSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(settings: FastSettingsEntity)

    @Query("UPDATE fast_settings SET isSynced = :synced WHERE id = 1")
    suspend fun updateSyncStatus(synced: Boolean)
}
