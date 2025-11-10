package com.example.taptalk.aac.data

/**
 * Represents a single Augmentative and Alternative Communication (AAC) card.
 *
 * This data class holds all the necessary information to display and locate an AAC card,
 * which typically consists of an image and a spoken label.
 *
 * @property fileName The name of the image file associated with the card (e.g., "apple.png").
 * @property label The text label displayed on the card and spoken when the card is activated (e.g., "Apple").
 * @property path The full path to the image file within the application's assets or storage.
 * @property folder The name of the category or folder this card belongs to.
 */
data class AccCard(
    val fileName: String,
    val label: String,
    val path: String,
    val folder: String
)

/**
 * Represents the different forms of a verb.
 *
 * This data class holds the base form, past tense, and perfect tense of a verb.
 * It also includes a list of negative forms for the verb.
 *
 * @property base The base form of the verb (e.g., "go", "eat").
 * @property past The simple past tense form of the verb (e.g., "went", "ate").
 * @property perfect The past participle/perfect tense form of the verb (e.g., "gone", "eaten").
 * @property negatives A list of negative forms associated with the verb (e.g., "don't", "can't"). Defaults to an empty list.
 */
data class VerbForms(
    val base: String,
    val past: String,
    val perfect: String,
    val negatives: List<String> = emptyList()
)

/**
 * Represents a single category in the AAC system.
 * Categories are used to group related AAC cards, such as "Food", "Places", or "Actions".
 *
 * @property label The display name of the category (e.g., "Food").
 * @property path The file system path to the directory containing the cards for this category.
 */
data class Category(
    val label: String,
    val path: String
)