package com.example.taptalk.viewmodel

import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.*
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RegisterViewModelTest {

    private val auth = mockk<FirebaseAuth>(relaxed = true)
    private val firestore = mockk<FirebaseFirestore>(relaxed = true)

    private val userCollection = mockk<CollectionReference>(relaxed = true)
    private val userDoc = mockk<DocumentReference>(relaxed = true)
    private val profileCollection = mockk<CollectionReference>(relaxed = true)
    private val profileDoc = mockk<DocumentReference>(relaxed = true)

    private lateinit var viewModel: RegisterViewModel
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)

        mockkStatic(FirebaseAuth::class)
        every { FirebaseAuth.getInstance() } returns auth

        mockkStatic(FirebaseFirestore::class)
        every { FirebaseFirestore.getInstance() } returns firestore

        every { firestore.collection("USERS") } returns userCollection
        every { userCollection.document(any()) } returns userDoc
        every { userDoc.collection("User_Data") } returns profileCollection
        every { profileCollection.document("Profile") } returns profileDoc

        viewModel = spyk(RegisterViewModel())

        every { viewModel.isValidEmail(any()) } returns false
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    // TEST 1  Password mismatch
    @Test
    fun `register returns error when passwords don't match`() = runTest {
        viewModel.register(
            name = "John",
            email = "test@test.com",
            password = "123456",
            repeatPassword = "654321",
            dob = "2000-01-01",
            pin = null,
            optPassword = true,
            optPin = false
        )

        assertEquals("Passwords do not match", viewModel.state.value.error)
    }

    // TEST 2  Invalid email
    @Test
    fun `register returns error when email invalid`() = runTest {
        viewModel.register(
            name = "John",
            email = "bad email",
            password = "123456",
            repeatPassword = "123456",
            dob = "2000-01-01",
            pin = null,
            optPassword = true,
            optPin = false
        )

        assertEquals("Please enter a valid email", viewModel.state.value.error)
    }

    // TEST 4  FirebaseAuth failure
    @Test
    fun `firebase auth failure returns error`() = runTest {
        val authTask = mockk<Task<AuthResult>>(relaxed = true)

        every { viewModel.isValidEmail("test@test.com") } returns true

        every { auth.createUserWithEmailAndPassword(any(), any()) } returns authTask

        every { authTask.addOnSuccessListener(any()) } returns authTask

        every { authTask.addOnFailureListener(any()) } answers {
            firstArg<OnFailureListener>().onFailure(RuntimeException("Auth error"))
            authTask
        }

        viewModel.register(
            name = "John",
            email = "test@test.com",
            password = "123456",
            repeatPassword = "123456",
            dob = "2000-01-01",
            pin = null,
            optPassword = true,
            optPin = false
        )

        advanceUntilIdle()

        assertEquals("Auth error", viewModel.state.value.error)
    }
}
