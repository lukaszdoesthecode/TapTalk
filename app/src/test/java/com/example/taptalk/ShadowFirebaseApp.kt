package com.example.taptalk

import android.content.Context
import com.google.firebase.FirebaseApp
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements

@Implements(FirebaseApp::class)
class ShadowFirebaseApp {

    @Implementation
    fun initializeApp(context: Context?): FirebaseApp? {
        return null
    }

    companion object {
        @JvmStatic
        @Implementation
        fun initializeApp(context: Context?, options: Any?): FirebaseApp? {
            return null
        }
    }
}
