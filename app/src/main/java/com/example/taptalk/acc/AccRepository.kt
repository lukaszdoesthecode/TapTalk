package com.example.taptalk.acc

import android.content.Context
import android.util.Log
import com.example.taptalk.data.AppDatabase
import com.example.taptalk.data.FastSettingsEntity
import com.example.taptalk.data.HistoryRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.mlkit.nl.smartreply.TextMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.nio.charset.Charset

class AccRepository(private val context: Context) {

    private val firestore = FirebaseFirestore.getInstance()
    private val historyRepository = HistoryRepository(context)
    private val database by lazy { AppDatabase.getInstance(context) }
    private val fastSettingsDao by lazy { database.fastSettingsDao() }

    suspend fun loadAllCards(): List<AccCard> = withContext(Dispatchers.IO) {
        loadAccCards() + loadCustomCards()
    }

    suspend fun loadCategories(): List<AccCard> = withContext(Dispatchers.IO) {
        loadCategoryCards()
    }

    suspend fun loadIrregularVerbs(): JSONObject = withContext(Dispatchers.IO) {
        loadJsonAsset("irregular_verbs.json")
    }

    suspend fun loadIrregularPlurals(): JSONObject = withContext(Dispatchers.IO) {
        loadJsonAsset("irregular_nouns.json")
    }

    suspend fun buildBaseConversation(cards: List<AccCard>): List<TextMessage> = withContext(Dispatchers.IO) {
        val messages = mutableListOf<TextMessage>()
        val history = historyRepository.getRecentSentences()
        history.forEach { entity ->
            messages.add(TextMessage.createForLocalUser(entity.sentence, entity.timestamp))
        }
        val baseTimestamp = System.currentTimeMillis()
        cards.forEachIndexed { index, card ->
            messages.add(TextMessage.createForLocalUser(card.label, baseTimestamp + index))
        }
        messages
    }

    suspend fun loadInitialSettings(userId: String?): AccUserSettings = withContext(Dispatchers.IO) {
        if (userId == null) {
            return@withContext AccUserSettings()
        }

        var local = fastSettingsDao.getSettings()
        var volume = local?.volume ?: 50f
        var selectedVoice = local?.selectedVoice ?: "Kate"
        var aiSupport = local?.aiSupport ?: true
        var autoSpeak = local?.autoSpeak ?: true
        var gridSize = local?.gridSize ?: "Medium"
        var darkMode = false
        var lowVisionMode = false
        var visibleLevels: List<String> = listOf("A1", "A2", "B1")

        try {
            val snapshot = firestore.collection("USERS")
                .document(userId)
                .collection("Fast_Settings")
                .document("current")
                .get()
                .await()

            if (snapshot.exists()) {
                volume = (snapshot.getDouble("volume") ?: volume.toDouble()).toFloat()
                selectedVoice = snapshot.getString("selectedVoice") ?: selectedVoice
                aiSupport = snapshot.getBoolean("aiSupport") ?: aiSupport
                autoSpeak = snapshot.getBoolean("autoSpeak") ?: autoSpeak
                gridSize = snapshot.getString("gridSize") ?: gridSize
                darkMode = snapshot.getBoolean("darkMode") ?: false
                lowVisionMode = snapshot.getBoolean("lowVisionMode") ?: false
                visibleLevels = (snapshot.get("visibleLevels") as? List<*>)
                    ?.filterIsInstance<String>()
                    ?.takeIf { it.isNotEmpty() }
                    ?: visibleLevels

                val entity = FastSettingsEntity(
                    volume = volume,
                    selectedVoice = selectedVoice,
                    aiSupport = aiSupport,
                    autoSpeak = autoSpeak,
                    isSynced = true,
                    gridSize = gridSize
                )
                fastSettingsDao.insertOrUpdate(entity)
                local = entity
            } else if (local == null) {
                fastSettingsDao.insertOrUpdate(FastSettingsEntity())
                local = fastSettingsDao.getSettings()
            }
        } catch (e: Exception) {
            Log.e("AccRepository", "Failed to load settings", e)
        }

        val stored = local ?: FastSettingsEntity()
        AccUserSettings(
            volume = stored.volume,
            selectedVoice = stored.selectedVoice,
            aiSupport = aiSupport,
            autoSpeak = autoSpeak,
            gridSize = stored.gridSize,
            visibleLevels = visibleLevels,
            darkMode = darkMode,
            lowVisionMode = lowVisionMode
        )
    }

    suspend fun refreshFavourites(userId: String?): List<AccCard> = withContext(Dispatchers.IO) {
        if (userId == null) return@withContext emptyList()
        return@withContext try {
            val snapshot = firestore.collection("USERS")
                .document(userId)
                .collection("Favourites")
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                val label = doc.getString("label") ?: return@mapNotNull null
                val path = doc.getString("path") ?: return@mapNotNull null
                val folder = doc.getString("folder") ?: "favourites"
                val fileName = doc.getString("fileName") ?: "$label.png"
                AccCard(fileName, label, path, folder)
            }
        } catch (e: Exception) {
            Log.e("AccRepository", "Failed to load favourites", e)
            emptyList()
        }
    }

    suspend fun toggleFavourite(userId: String?, card: AccCard): Boolean = withContext(Dispatchers.IO) {
        if (userId == null) return@withContext false
        return@withContext try {
            val favRef = firestore.collection("USERS")
                .document(userId)
                .collection("Favourites")
                .document(card.label)

            val doc = favRef.get().await()
            if (doc.exists()) {
                favRef.delete().await()
                false
            } else {
                val data = mapOf(
                    "label" to card.label,
                    "path" to card.path,
                    "folder" to card.folder,
                    "fileName" to card.fileName,
                    "timestamp" to Timestamp.now()
                )
                favRef.set(data).await()
                true
            }
        } catch (e: Exception) {
            Log.e("AccRepository", "Failed to toggle favourite", e)
            false
        }
    }

    suspend fun recordSentence(text: String) {
        withContext(Dispatchers.IO) {
            historyRepository.saveSentenceOffline(text)
            historyRepository.syncToFirebase()
        }
    }

    suspend fun syncHistory() {
        withContext(Dispatchers.IO) {
            historyRepository.syncToFirebase()
        }
    }

    private fun loadJsonAsset(fileName: String): JSONObject {
        val input = context.assets.open(fileName)
        val json = input.bufferedReader(Charset.forName("UTF-8")).use { it.readText() }
        return JSONObject(json)
    }

    private fun loadAccCards(): List<AccCard> {
        val assetManager = context.assets

        fun walk(path: String, topCategory: String? = null): List<AccCard> {
            val files = assetManager.list(path) ?: return emptyList()

            return files.flatMap { name ->
                val fullPath = if (path.isEmpty()) name else "$path/$name"
                val children = assetManager.list(fullPath) ?: emptyArray()

                if (children.isNotEmpty()) {
                    val cat = topCategory ?: name
                    walk(fullPath, cat)
                } else if (name.endsWith(".png") || name.endsWith(".jpg")) {
                    val category = topCategory ?: path.substringAfterLast("/")
                    listOf(
                        AccCard(
                            fileName = name,
                            label = normalizeFileName(name),
                            path = "file:///android_asset/$fullPath",
                            folder = category.lowercase()
                        )
                    )
                } else emptyList()
            }
        }

        return walk("ACC_board") + walk("categories")
    }

    private fun loadCustomCards(): List<AccCard> {
        val rootDir = File(context.filesDir, "Custom_Words")
        if (!rootDir.exists()) return emptyList()

        return rootDir.walkTopDown()
            .filter { it.isFile && (it.extension.equals("png", true) || it.extension.equals("jpg", true)) }
            .map { file ->
                val folderName = file.parentFile?.name ?: "custom"
                val label = file.nameWithoutExtension
                AccCard(
                    fileName = file.name,
                    label = label.replaceFirstChar { it.uppercase() },
                    path = file.absolutePath,
                    folder = folderName
                )
            }
            .toList()
    }

    private fun loadCategoryCards(): List<AccCard> {
        val files = context.assets.list("ACC_board/categories") ?: return emptyList()
        val desiredOrder = listOf(
            "home_category.png",
            "favourites_category.png",
            "custom_category.png",
            "emergency_category.png",
            "social_category.png",
            "questions_category.png",
            "pronouns_category.png",
            "verbs_category.png",
            "adjective_category.png",
            "adverb_category.png",
            "prepositions_category.png",
            "conjuction_category.png",
            "determiners_category.png",
            "negation_category.png"
        )

        val sortedFiles = desiredOrder.mapNotNull { orderName ->
            files.find { it.equals(orderName, ignoreCase = true) }
        }

        return sortedFiles.map { file ->
            AccCard(
                fileName = file,
                label = trimCategoryName(file),
                path = "file:///android_asset/ACC_board/categories/$file",
                folder = "categories"
            )
        }
    }

    private fun normalizeFileName(file: String): String {
        var name = file.substringBeforeLast('.')
        name = name.replace(Regex("_(A1|A2|B1|B2|C1|C2|EN|PL|DE|FR|ES)\\b"), "")
        name = name.replace(Regex("^[0-9]+[_-]?"), "")
        name = name.replace("_", " ").replace("-", " ")
        name = name.replace("\\s+".toRegex(), " ").trim()
        return name.split(" ").joinToString(" ") { word ->
            val lower = word.lowercase()
            if (lower in listOf("a", "an", "the", "and", "or", "of", "to", "in", "on", "at", "for")) lower
            else lower.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }.replaceFirstChar { it.titlecase() }
    }

    private fun trimCategoryName(fileName: String): String {
        var name = fileName.substringBeforeLast('.')
        name = name.replace("_cathegory", "", true)
            .replace("_category", "", true)
            .replace("_", " ")
            .trim()
        return name.replaceFirstChar { it.uppercase() }
    }
}
