package com.example.taptalk.viewmodel

import com.example.taptalk.data.LoginRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val repo = mockk<LoginRepository>()
    private lateinit var viewModel: LoginViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = LoginViewModel(repo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `login with empty email or password sets error state`() = runTest {
        viewModel.login("", "")

        val state = viewModel.state.value
        assertEquals("E-mail and password required", state.error)
        assertEquals(false, state.success)
        assertEquals(false, state.isLoading)
    }

    @Test
    fun `successful login updates state to success`() = runTest {
        coEvery { repo.loginWithEmailPassword(any(), any()) } returns Result.success(Unit)

        viewModel.login("test@email.com", "password")
        advanceUntilIdle()

        val state = viewModel.state.value

        coVerify { repo.loginWithEmailPassword("test@email.com", "password") }

        assertEquals(true, state.success)
        assertEquals(null, state.error)
        assertEquals(false, state.isLoading)
    }

    @Test
    fun `failed login updates state with error message`() = runTest {
        coEvery { repo.loginWithEmailPassword(any(), any()) } returns Result.failure(
            RuntimeException("Login failed")
        )

        viewModel.login("test@email.com", "wrongpass")
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("Login failed", state.error)
        assertEquals(false, state.success)
        assertEquals(false, state.isLoading)
    }

    @Test
    fun `login sets loading state before repository returns`() = runTest {
        coEvery { repo.loginWithEmailPassword(any(), any()) } coAnswers {
            delay(100)
            Result.success(Unit)
        }

        viewModel.login("email", "password")

        val loadingState = viewModel.state.value
        assertEquals(true, loadingState.isLoading)
        assertEquals(null, loadingState.error)
        assertEquals(false, loadingState.success)

        advanceUntilIdle()

        val finalState = viewModel.state.value
        assertEquals(true, finalState.success)
    }
}
