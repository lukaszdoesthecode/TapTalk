package com.example.taptalk

import android.os.Looper
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import io.mockk.*
import junit.framework.TestCase.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class SettingsActivityRoboTest {

    @Test
    fun activity_startsSuccessfully() {

        mockkStatic(FirebaseApp::class)
        val fakeApp = mockk<FirebaseApp>(relaxed = true)
        every { FirebaseApp.initializeApp(any()) } returns fakeApp
        every { FirebaseApp.getInstance() } returns fakeApp

        mockkStatic(FirebaseAuth::class)
        every { FirebaseAuth.getInstance() } returns mockk(relaxed = true)

        mockkStatic(FirebaseFirestore::class)

        val firestore = mockk<FirebaseFirestore>(relaxed = true)
        val users = mockk<CollectionReference>(relaxed = true)
        val userDoc = mockk<DocumentReference>(relaxed = true)
        val fastSettings = mockk<CollectionReference>(relaxed = true)
        val current = mockk<DocumentReference>(relaxed = true)

        every { FirebaseFirestore.getInstance() } returns firestore
        every { firestore.collection("USERS") } returns users
        every { users.document(any()) } returns userDoc
        every { userDoc.collection("Fast_Settings") } returns fastSettings
        every { fastSettings.document("current") } returns current

        every { current.addSnapshotListener(any()) } returns mockk(relaxed = true)
        every { current.get() } returns mockk(relaxed = true)

        val controller = Robolectric.buildActivity(SettingsActivity::class.java)
        controller.setup()

        shadowOf(Looper.getMainLooper()).idle()

        val activity = controller.get()

        assertNotNull(activity)
    }
}
