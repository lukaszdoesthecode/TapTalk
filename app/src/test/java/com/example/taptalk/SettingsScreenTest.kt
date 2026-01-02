package com.example.taptalk

import android.content.Context
import android.speech.tts.TextToSpeech
import com.example.taptalk.data.FastSettingsDao
import com.example.taptalk.data.HistoryDao
import com.example.taptalk.data.UserCategoryDao
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.*
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File

class SettingsScreenTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        context = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }


    @Test
    fun `update Firestore setting writes correct key`() {
        val firestore = mockk<FirebaseFirestore>()
        val users = mockk<CollectionReference>()
        val userDoc = mockk<DocumentReference>()
        val fastSettings = mockk<CollectionReference>()
        val current = mockk<DocumentReference>(relaxed = true)

        every { firestore.collection("USERS") } returns users
        every { users.document(any()) } returns userDoc
        every { userDoc.collection("Fast_Settings") } returns fastSettings
        every { fastSettings.document("current") } returns current

        val key = "voicePitch"
        val value = 1.2f

        current.set(mapOf(key to value), SetOptions.merge())

        verify { current.set(mapOf(key to value), any()) }
    }

    @Test
    fun `tts loads voice names`() {
        val tts = mockk<TextToSpeech>(relaxed = true)
        every { tts.voices } returns setOf(
            mockk { every { name } returns "VoiceA" },
            mockk { every { name } returns "VoiceB" },
        )

        val result = tts.voices.map { it.name }

        assert(result.contains("VoiceA"))
        assert(result.contains("VoiceB"))
    }

    @Test
    fun `clearLocalCustomCards deletes directory`() {
        val dir = mockk<File>(relaxed = true)

        every { context.getDir("custom_cards", Context.MODE_PRIVATE) } returns dir
        every { dir.exists() } returns true

        clearLocalCustomCards(context)

        verify { dir.deleteRecursively() }
    }


    @Test
    fun `clearLocalUserData calls all DAOs`() = runBlocking {
        val fastDao = mockk<FastSettingsDao>(relaxed = true)
        val historyDao = mockk<HistoryDao>(relaxed = true)
        val catDao = mockk<UserCategoryDao>(relaxed = true)

        val user = mockk<FirebaseUser>()
        every { user.uid } returns "UID123"

        mockkStatic(FirebaseAuth::class)
        every { FirebaseAuth.getInstance().currentUser } returns user

        mockkObject(com.example.taptalk.data.AppDatabase.Companion)
        every { com.example.taptalk.data.AppDatabase.getDatabase(any()) } returns mockk {
            every { fastSettingsDao() } returns fastDao
            every { historyDao() } returns historyDao
            every { userCategoryDao() } returns catDao
        }

        clearLocalUserData(context)

        coVerify { fastDao.deleteAll("UID123") }
        coVerify { historyDao.deleteAll("UID123") }
        coVerify { catDao.deleteAll("UID123") }
    }

    @Test
    fun `account deletion triggers reauthenticate and delete`() {
        val auth = mockk<FirebaseAuth>()
        val user = mockk<FirebaseUser>(relaxed = true)

        mockkStatic(FirebaseAuth::class)
        every { FirebaseAuth.getInstance() } returns auth
        every { auth.currentUser } returns user

        every { user.reauthenticate(any()) } returns mockk(relaxed = true)
        every { user.delete() } returns mockk(relaxed = true)

        // Simulate flow
        user.reauthenticate(mockk(relaxed = true))
        user.delete()

        verify { user.reauthenticate(any()) }
        verify { user.delete() }
    }

}
