package com.example.taptalk.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a user-created category in the local database.
 * This entity is used by Room to create the `user_categories` table.
 *
 * @property name The unique name of the category, serving as the primary key.
 * @property colorHex The hexadecimal string representation of the category's color (e.g., "#FFFFFF").
 * @property imagePath The local file path to the category's cover image, if one is set. Can be null.
 * @property cardFileNames A list of filenames corresponding to the cards belonging to this category.
 * @property synced A boolean flag indicating whether this category has been synced with a remote data source. Defaults to `false`.
 */
@Entity(tableName = "user_categories")
data class UserCategoryEntity(
    @PrimaryKey val name: String,
    val colorHex: String,
    val imagePath: String?,
    val cardFileNames: List<String>,
    val synced: Boolean = false
)