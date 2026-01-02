package com.example.taptalk

import androidx.work.OneTimeWorkRequestBuilder
import org.junit.Test

class ScheduleSyncTest {

    @Test
    fun buildWorkRequestDoesNotCrash() {
        OneTimeWorkRequestBuilder<com.example.taptalk.data.FastSettingsSyncWorker>()
            .setConstraints(
                androidx.work.Constraints.Builder()
                    .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                    .build()
            )
            .build()
    }
}
