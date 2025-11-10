package com.example.taptalk.aac.data

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Toggles the favourite status of an [AccCard] in Firebase Firestore for a specific user.
 *
 * If the card is already marked as a favourite, it will be removed. If it's not a favourite,
 * it will be added. The operation is performed in the user's "Favourites" sub-collection,
 * using the card's label as the document ID.
 *
 * The function is asynchronous. The `onDone` callback is invoked upon completion.
 *
 * @param context The application context. Currently unused but kept for potential future use (e.g., showing Toasts).
 * @param card The [AccCard] to add or remove from favourites.
 * @param userId The unique ID of the current user. If null, the function does nothing.
 * @param onDone A callback function that is invoked after the operation completes. It receives a Boolean:
 *               `true` if the card was added to favourites, `false` if it was removed.
 */
fun toggleFavouriteInFirebase(context: Context, card: AccCard, userId: String?, onDone: (Boolean) -> Unit = {}) {
    if (userId == null) return

    val firestore = FirebaseFirestore.getInstance()
    val favRef = firestore.collection("USERS").document(userId).collection("Favourites")

    favRef.document(card.label).get()
        .addOnSuccessListener { doc ->
            if (doc.exists()) {
                // already favourite
                favRef.document(card.label).delete()
                onDone(false)
            } else {
                // not favourite
                val data = mapOf(
                    "label" to card.label,
                    "path" to card.path,
                    "folder" to card.folder,
                    "fileName" to card.fileName,
                    "timestamp" to com.google.firebase.Timestamp.now()
                )
                favRef.document(card.label).set(data)
                onDone(true)
            }
        }
}

/**
 * Loads the user's favourite AAC cards from Firestore.
 *
 * This function fetches the "Favourites" sub-collection for a given user ID.
 * It maps the retrieved documents to a list of [AccCard] objects and passes this list
 * to the `onLoaded` callback. If the `userId` is null, it immediately calls `onLoaded`
 * with an empty list.
 *
 * @param context The application context. (Note: This parameter is not currently used but is kept for future-proofing and consistency).
 * @param userId The unique ID of the user whose favourites are to be loaded.
 * @param onLoaded A callback function that will be invoked with the list of loaded [AccCard]s.
 */
fun loadFavourites(context: Context, userId: String?, onLoaded: (List<AccCard>) -> Unit) {
    if (userId == null) {
        onLoaded(emptyList())
        return
    }

    val firestore = FirebaseFirestore.getInstance()
    firestore.collection("USERS").document(userId).collection("Favourites")
        .get()
        .addOnSuccessListener { snapshot ->
            val favs = snapshot.documents.mapNotNull { doc ->
                val label = doc.getString("label") ?: return@mapNotNull null
                val path = doc.getString("path") ?: return@mapNotNull null
                val folder = doc.getString("folder") ?: "favourites"
                val fileName = doc.getString("fileName") ?: "$label.png"
                AccCard(fileName, label, path, folder)
            }
            onLoaded(favs)
        }
}