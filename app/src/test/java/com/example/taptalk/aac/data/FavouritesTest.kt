package com.example.taptalk.aac.data

import android.content.Context
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.*
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for managing user favourites functionality within the application.
 *
 * This test class verifies the interactions with Firebase Firestore for operations related to
 * "Favourites", specifically testing the logic for toggling a favourite status (adding/removing)
 * and retrieving the list of favourite AAC cards for a specific user.
 *
 */
class FavouritesTest {

    private lateinit var context: Context
    private lateinit var firestore: FirebaseFirestore
    private lateinit var users: CollectionReference
    private lateinit var userDoc: DocumentReference
    private lateinit var favs: CollectionReference


    @Before
    fun setup() {
        context = mockk(relaxed = true)

        firestore = mockk()
        users = mockk()
        userDoc = mockk()
        favs = mockk()

        // Static FirebaseFirestore.getInstance()
        mockkStatic(FirebaseFirestore::class)
        every { FirebaseFirestore.getInstance() } returns firestore

        every { firestore.collection("USERS") } returns users
        every { users.document(any()) } returns userDoc
        every { userDoc.collection("Favourites") } returns favs
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    /**
     * Verifies that [toggleFavouriteInFirebase] correctly executes the provided callback.
     *
     * This test mocks the Firestore interactions to simulate a scenario where a favourite
     * document does not exist (triggering an addition). It ensures that:
     * 1. The document reference is retrieved correctly.
     * 2. The `get()` task's success listener is invoked.
     * 3. The final callback lambda is executed with the expected boolean result (true, indicating added).
     */
    @Test
    fun `toggleFavouriteInFirebase runs callback`() {
        val card = AccCard(
            fileName = "x.png",
            label = "TestLabel",
            path = "path/to",
            folder = "folder"
        )

        val favDoc = mockk<DocumentReference>()
        every { favs.document("TestLabel") } returns favDoc

        val docSnap = mockk<DocumentSnapshot>()
        every { docSnap.exists() } returns false

        val getTask = mockk<Task<DocumentSnapshot>>()

        every { favDoc.get() } returns getTask

        every { getTask.addOnSuccessListener(any()) } answers {
            val listener = arg<OnSuccessListener<DocumentSnapshot>>(0)
            listener.onSuccess(docSnap)
            getTask
        }

        every { favDoc.set(any()) } returns mockk(relaxed = true)
        every { favDoc.delete() } returns mockk(relaxed = true)

        var result: Boolean? = null

        toggleFavouriteInFirebase(context, card, "USER123") { isFav ->
            result = isFav
        }

        assert(result == true)
    }

    /**
     * Verifies that [loadFavourites] successfully retrieves a list of favourite cards from Firestore.
     *
     * This test mocks the Firestore query result to return a single document snapshot containing
     * valid `AccCard` data. It asserts that the callback function receives a list containing exactly
     * one card with the expected label ("Hello"), ensuring the data mapping from Firestore fields
     * to the [AccCard] object is correct.
     */
    @Test
    fun `loadFavourites returns list`() {
        val doc = mockk<DocumentSnapshot>()
        every { doc.getString("label") } returns "Hello"
        every { doc.getString("path") } returns "path/to"
        every { doc.getString("folder") } returns "folder"
        every { doc.getString("fileName") } returns "Hello.png"

        val snapshot = mockk<QuerySnapshot>()
        every { snapshot.documents } returns listOf(doc)

        val getTask = mockk<Task<QuerySnapshot>>()
        every { favs.get() } returns getTask

        every { getTask.addOnSuccessListener(any()) } answers {
            val listener = arg<OnSuccessListener<QuerySnapshot>>(0)
            listener.onSuccess(snapshot)
            getTask
        }

        var loaded: List<AccCard>? = null

        loadFavourites(context, "USER123") { list ->
            loaded = list
        }

        val out = loaded!!
        assert(out.size == 1)
        assert(out[0].label == "Hello")
    }
}