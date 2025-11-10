package com.example.taptalk.aac.data

import android.content.Context
import org.json.JSONObject
import java.nio.charset.Charset

/**
 * Loads a JSON file from the app's assets folder and parses it into a JSONObject.
 *
 * This is a utility function for reading raw JSON data stored in the `assets` directory.
 * It opens the specified file, reads its content as a UTF-8 string, and then
 * constructs a [JSONObject] from that string.
 *
 * @param context The [Context] used to access the application's assets.
 * @param fileName The name of the JSON file to load from the assets folder (e.g., "data.json").
 * @return A [JSONObject] representing the content of the loaded file.
 * @throws java.io.IOException if the file cannot be opened or read.
 * @throws org.json.JSONException if the file content is not a valid JSON format.
 */
fun loadJsonAsset(context: Context, fileName: String): JSONObject {
    val input = context.assets.open(fileName)
    val json = input.bufferedReader(Charset.forName("UTF-8")).use { it.readText() }
    return JSONObject(json)
}