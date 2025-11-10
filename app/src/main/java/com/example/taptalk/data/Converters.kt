package com.example.taptalk.data

import androidx.room.TypeConverter

/**
 * Type converters to allow Room to reference complex data types.
 *
 * This class provides methods to convert between a `List<String>` and its `String`
 * representation, enabling the storage of string lists in the database.
 */
class Converters {

    @TypeConverter
    fun fromStringList(list: List<String>?): String? {
        return list?.joinToString("|")
    }

    @TypeConverter
    fun toStringList(data: String?): List<String> {
        if (data.isNullOrEmpty()) return emptyList()
        return data.split("|")
    }
}
