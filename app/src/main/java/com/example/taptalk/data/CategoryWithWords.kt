package com.example.taptalk.data

import androidx.room.Embedded
import androidx.room.Relation

data class CategoryWithWords(
    @Embedded val category: UserCategoryEntity,
    @Relation(
        parentColumn = "name",
        entityColumn = "folder"
    )
    val words: List<CustomWordEntity>
)
