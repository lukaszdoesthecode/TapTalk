package com.example.taptalk.viewmodel

import com.example.taptalk.data.FastSettingsEntity
import com.example.taptalk.data.FastSettingsRepository
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class FastSettingsViewModelTest {

    private val repo = mockk<FastSettingsRepository>(relaxed = true)
    private val firestore = mockk<FirebaseFirestore>(relaxed = true)
    private val collectionRef = mockk<CollectionReference>(relaxed = true)
    private val documentRef = mockk<DocumentReference>(relaxed = true)

    private lateinit var viewModel: FastSettingsViewModel

    @Before
    fun setup() {
        viewModel = FastSettingsViewModel(repo, "user123")

        mockkStatic(FirebaseFirestore::class)
        every { FirebaseFirestore.getInstance() } returns firestore

        every { firestore.collection(any()) } returns collectionRef
        every { collectionRef.document(any()) } returns documentRef
        every { documentRef.collection(any()) } returns collectionRef
        every { collectionRef.document(any()) } returns documentRef

        every { documentRef.update(any<Map<String, Any>>()) } returns mockk(relaxed = true)
    }

    @Test
    fun `saveSettings should save local settings correctly`() = runTest {
        viewModel.volume = 40f
        viewModel.selectedVoice = "John"
        viewModel.aiSupport = false

        viewModel.saveSettings()

        coVerify {
            repo.saveLocalSettings(
                FastSettingsEntity(
                    id = 0,
                    volume = 40f,
                    selectedVoice = "John",
                    aiSupport = false
                )
            )
        }
    }

    @Test
    fun `saveSettings should call firestore update`() = runTest {
        viewModel.saveSettings()

        verify {
            documentRef.update(
                match<Map<String, Any>> {
                    it["volume"] == 50f &&
                            it["selectedVoice"] == "Kate" &&
                            it["aiSupport"] == true
                }
            )
        }
    }
}
