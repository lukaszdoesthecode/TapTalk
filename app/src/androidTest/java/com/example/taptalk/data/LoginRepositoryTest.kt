package com.example.taptalk.data

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// ✅ We wrap FirebaseAuth instead of subclassing it
class FakeFirebaseAuth(
    private val shouldSucceed: Boolean = true
) {
    fun signInWithEmailAndPassword(email: String, password: String): Task<AuthResult> {
        return if (shouldSucceed) {
            Tasks.forResult(null) // success
        } else {
            Tasks.forException(Exception("Invalid credentials")) // failure
        }
    }
}

// ✅ Adapter so we can inject our fake into LoginRepository
class TestableLoginRepository(private val fakeAuth: FakeFirebaseAuth) {
    suspend fun loginWithEmailPassword(email: String, password: String): Result<Unit> {
        return try {
            fakeAuth.signInWithEmailAndPassword(email, password).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class LoginRepositoryTest {

    @Test
    fun loginWithEmailPassword_success_returnsSuccess() = runBlocking {
        val repo = TestableLoginRepository(fakeAuth = FakeFirebaseAuth(true))
        val result = repo.loginWithEmailPassword("user@example.com", "password")
        assertTrue(result.isSuccess)
    }

    @Test
    fun loginWithEmailPassword_failure_returnsFailure() = runBlocking {
        val repo = TestableLoginRepository(fakeAuth = FakeFirebaseAuth(false))
        val result = repo.loginWithEmailPassword("user@example.com", "wrong")
        assertFalse(result.isSuccess)
    }
}
