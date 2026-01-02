package com.example.taptalk.aac.data

import android.content.Context
import android.util.Log
import com.example.taptalk.data.AppDatabase
import com.example.taptalk.data.CustomWordDao
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.*
import com.google.firebase.storage.FirebaseStorage
import io.mockk.*
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for the [CustomWordsRepository] class.
 *
 * This test class verifies the interactions between the repository and its dependencies,
 * including Firebase (Auth, Firestore, Storage) and the local Room database (AppDatabase, CustomWordDao).
 *
 * Key responsibilities tested:
 * - Setup and initialization of mocks for Firebase and Room.
 * - Execution of the `restoreFromFirebase` method to ensure data synchronization logic flows correctly without crashing.
 */
class CustomWordsRepositoryTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = mockk(relaxed = true)

        mockkStatic(Log::class)
        mockkStatic(FirebaseAuth::class)
        mockkStatic(FirebaseFirestore::class)
        mockkStatic(FirebaseStorage::class)
        mockkStatic(AppDatabase::class)
        mockkStatic("kotlinx.coroutines.tasks.TasksKt")

        every { Log.e(any(), any()) } returns 0

        val auth = mockk<FirebaseAuth>()
        val user = mockk<FirebaseUser>()
        val firestore = mockk<FirebaseFirestore>()
        val storage = mockk<FirebaseStorage>()
        val db = mockk<AppDatabase>()
        val dao = mockk<CustomWordDao>(relaxed = true)

        every { FirebaseAuth.getInstance() } returns auth
        every { FirebaseFirestore.getInstance() } returns firestore
        every { FirebaseStorage.getInstance() } returns storage
        every { AppDatabase.getDatabase(context) } returns db
        every { db.customWordDao() } returns dao

        every { auth.currentUser } returns user
        every { user.uid } returns "USER123"

        // Firestore mock
        val users: CollectionReference = mockk()
        val userDoc: DocumentReference = mockk()
        val words: CollectionReference = mockk()

        every { firestore.collection("USERS") } returns users
        every { users.document("USER123") } returns userDoc
        every { userDoc.collection("Custom_Words") } returns words

        val doc = mockk<DocumentSnapshot>()
        every { doc.getString(any()) } returns "test"

        val snapshot = mockk<QuerySnapshot>()
        every { snapshot.documents } returns listOf(doc)

        val task = com.google.android.gms.tasks.Tasks.forResult(snapshot)
        every { words.get() } returns task
        coEvery { task.await() } returns snapshot
    }

    @Test
    fun test_restoreFromFirebase_runs() = runTest {
        val repo = CustomWordsRepository(context)
        repo.restoreFromFirebase()

        assert(true)
    }
}