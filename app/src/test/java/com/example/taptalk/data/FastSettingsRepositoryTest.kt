package com.example.taptalk.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.*
import io.mockk.*
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.tasks.await
import org.junit.Before
import org.junit.Test

class FastSettingsRepositoryTest {

    private lateinit var dao: FastSettingsDao
    private lateinit var repo: FastSettingsRepository
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    @Before
    fun setup() {
        mockkStatic(FirebaseAuth::class)
        mockkStatic(FirebaseFirestore::class)
        mockkStatic("kotlinx.coroutines.tasks.TasksKt")

        dao = mockk(relaxed = true)
        auth = mockk()
        firestore = mockk()

        every { FirebaseAuth.getInstance() } returns auth
        every { FirebaseFirestore.getInstance() } returns firestore

        // mock user
        val user = mockk<FirebaseUser>()
        every { auth.currentUser } returns user
        every { user.uid } returns "USER123"

        repo = FastSettingsRepository(dao, mockk())
    }

    @Test
    fun `getLocalSettings runs without crash`() = runTest {
        coEvery { dao.getSettings(any()) } returns null
        repo.getLocalSettings() // nothing to assert
    }


    @Test
    fun `fetchFromFirebase runs without exploding`() = runTest {
        // Mock Firestore chain but return empty snapshot
        val colUsers = mockk<CollectionReference>()
        val docUser = mockk<DocumentReference>()
        val colFast = mockk<CollectionReference>()
        val docCurrent = mockk<DocumentReference>()

        every { firestore.collection("USERS") } returns colUsers
        every { colUsers.document("USER123") } returns docUser
        every { docUser.collection("Fast_Settings") } returns colFast
        every { colFast.document("current") } returns docCurrent

        val snap = mockk<DocumentSnapshot>()
        every { snap.exists() } returns false  // simplest path
        coEvery { docCurrent.get().await() } returns snap

        repo.fetchFromFirebase() // no crash = coverage
    }
}