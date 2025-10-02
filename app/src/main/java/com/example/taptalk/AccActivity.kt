package com.example.taptalk

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.Options
import okio.buffer
import okio.source
import org.burnoutcrew.reorderable.*
import java.util.Locale
import android.speech.tts.TextToSpeech


class AssetUriFetcher(
    private val context: Context,
    private val uri: Uri
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        Log.d("ACC_DEBUG", "FETCHER CALLED for: $uri")

        val assetPath = uri.toString().removePrefix("file:///android_asset/")
        Log.d("ACC_DEBUG", "Asset path resolved to: $assetPath")

        val input = context.assets.open(assetPath)

        val bmp = BitmapFactory.decodeStream(context.assets.open(assetPath))
        Log.d("ACC_DEBUG", "Bitmap decoded successfully? ${bmp != null}")

        return SourceResult(
            source = ImageSource(input.source().buffer(), context),
            mimeType = "image/png",
            dataSource = DataSource.DISK
        )
    }

    class Factory(private val context: Context) : Fetcher.Factory<Uri> {
        override fun create(
            data: Uri,
            options: Options,
            imageLoader: ImageLoader
        ): Fetcher? {
            return if (data.toString().startsWith("file:///android_asset/")) {
                Log.d("ACC_DEBUG", "Factory accepted URI: $data")
                AssetUriFetcher(context, data)
            } else {
                Log.d("ACC_DEBUG", "Factory ignored URI: $data")
                null
            }
        }
    }
}

data class AccCard(
    val fileName: String,
    val label: String,
    val path: String,
    val folder: String
)

fun borderColorFor(folder: String): Color = when (folder.lowercase()) {
    "adjectives" -> Color.Blue
    "conjuctions" -> Color.LightGray
    "negation" -> Color.Red
    "nouns" -> Color(0xFFFFA500)
    "preposition" -> Color(0xFFFFC0CB)
    "pronouns" -> Color.Yellow
    "questions" -> Color(0xFF800080)
    "social" -> Color(0xFFFFC0CB)
    "verbs" -> Color.Green
    else -> Color.Black
}

val gridOrder: List<String?> = listOf(
    "what_A1", "I_A1", "we_A1", "have_A1", "know_A1", "child_A1", "place_A1", "time_A1", "hand_A1", "good_A1", "bad_A1",
    "how_A1", "you_A1", "they_A1", "come_A1", "make_A1", "man_A1", "world_A1", "year_A1", "thing_A1", "thirsty_A2", "hungry_A1",
    "why_A1", "he_A1", "be_A1", "do_A1", "say_A1", "woman_A1", "house_A1", "month_A1", "bathroom_A1", "happy_A1", "sad_A1",
    "where_A1", "she_A1", "yes_A1", "get_A1", "see_A1", "person_A1", "school_A1", "life_A1", "food_A1", "tired_A1", "calm_B1",
    "who_A1", "it_A1.png", "no_A1", "go_B1", "create_A1", "friend_A1", "job_A1", "family_A1", "water_A1", "long_A1", "pretty_A1",
    "when_A1.png", "a", "an", "the", "hello_A1", "good_morning_A1", "bye_A1", "thanks_A1", "please_A1", "to_A1", "of_A1",
    "and_A1", "or_A1", null, null, null, null, null, null, null, null, null
)

fun loadAccCards(context: Context): List<AccCard> {
    val assetManager = context.assets

    fun walk(path: String): List<AccCard> {
        val files = assetManager.list(path) ?: return emptyList()
        Log.d("ACC_DEBUG", "Listing in: $path -> ${files.joinToString()}")

        return files.flatMap { name ->
            val fullPath = if (path.isEmpty()) name else "$path/$name"
            val children = assetManager.list(fullPath) ?: emptyArray()

            if (children.isNotEmpty()) {
                walk(fullPath)
            } else if (name.endsWith(".png") || name.endsWith(".jpg")) {
                Log.d("ACC_DEBUG", "Found image: $fullPath")
                val folderName = path.substringAfterLast("/")
                listOf(
                    AccCard(
                        fileName = name,
                        label = normalizeFileName(name),
                        path = "file:///android_asset/$fullPath",
                        folder = folderName.lowercase()
                    )
                )
            } else emptyList()
        }
    }

    return walk("ACC_board")
}

fun normalizeFileName(file: String): String {
    var name = file.substringBeforeLast('.')
    name = name.replace(Regex("_(A1|A2|B1|B2|C1|C2|EN|PL|DE|FR|ES)\\b"), "")
    name = name.replace(Regex("^[0-9]+[_-]?"), "")
    name = name.replace("_", " ").replace("-", " ")
    name = name.replace("\\s+".toRegex(), " ").trim()
    return name.split(" ").joinToString(" ") { word ->
        val lower = word.lowercase(Locale.getDefault())
        if (lower in listOf("a","an","the","and","or","of","to","in","on","at","for")) lower
        else lower.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }.replaceFirstChar { it.titlecase(Locale.getDefault()) }
}

class AccActivity : ComponentActivity() {
    private var tts: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TTS
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.US)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("ACC_DEBUG", "TTS: Language not supported")
                }
            } else {
                Log.e("ACC_DEBUG", "TTS: Initialization failed")
            }
        }

        val imageLoader = ImageLoader.Builder(this)
            .components { add(AssetUriFetcher.Factory(this@AccActivity)) }
            .build()

        setContent {
            MaterialTheme {
                AccScreen(imageLoader, speak = { text -> speakOut(text) })
            }
        }
    }

    private fun speakOut(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ACC_UTTERANCE")
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}

// ------------------ UI ------------------
@Composable
fun AccScreen(
    imageLoader: ImageLoader,
    spacing: Dp = 6.dp,
    speak: (String) -> Unit
) {
    var chosen = remember { mutableStateListOf<AccCard>() }

    Scaffold(bottomBar = { BottomNavBar() }) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TopWhiteBar(
                chosen = chosen,
                spacing = spacing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                imageLoader = imageLoader,
                onReorder = { from, to -> chosen.add(to, chosen.removeAt(from)) },
                onRemove = { idx -> chosen.removeAt(idx) },
                onClearAll = { chosen.clear() },
                onPlayStop = {
                    val sentence = chosen.joinToString(" ") { it.label }
                    if (sentence.isNotBlank()) speak(sentence)
                },
                onSpeakWord = { word -> speak(word) }
            )

            GreenBar(
                labels = listOf("Q0", "Q1", "Q2", "Q3", "Q4", "Q5"),
                spacing = spacing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp)
            )

            AACBoardGrid(imageLoader) { card ->
                chosen.add(card)
            }
        }
    }
}

@Composable
fun AACBoardGrid(imageLoader: ImageLoader, onCardClick: (AccCard) -> Unit) {
    val context = LocalContext.current
    val cards = remember { loadAccCards(context) }
        .associateBy { it.fileName.substringBeforeLast('.').lowercase() }

    val rows = 7
    val cols = 11

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for (row in 0 until rows) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (col in 0 until cols) {
                    val idx = row * cols + col
                    val fileName = gridOrder[idx]?.lowercase()
                    val card = if (fileName != null) cards[fileName] else null

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .border(
                                2.dp,
                                borderColorFor(card?.folder ?: ""),
                                RoundedCornerShape(4.dp)
                            )
                            .background(Color(0xFFD9D9D9), RoundedCornerShape(4.dp))
                            .clickable(enabled = card != null) {
                                if (card != null) onCardClick(card)
                            }
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (card != null) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter(
                                        model = Uri.parse(card.path),
                                        imageLoader = imageLoader
                                    ),
                                    contentDescription = card.label,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                )
                                Text(
                                    card.label,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ------------------ TOP BAR ------------------
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TopWhiteBar(
    chosen: List<AccCard>,
    modifier: Modifier = Modifier,
    spacing: Dp = 6.dp,
    imageLoader: ImageLoader,
    onReorder: (Int, Int) -> Unit = { _, _ -> },
    onRemove: (Int) -> Unit = {},
    onClearAll: () -> Unit = {},
    onPlayStop: () -> Unit = {},
    onSpeakWord: (String) -> Unit = {}
) {
    val state = rememberReorderableLazyListState(
        onMove = { from, to -> onReorder(from.index, to.index) }
    )

    Row(
        modifier = modifier
            .background(Color.White)
            .padding(horizontal = spacing, vertical = spacing / 2),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LazyRow(
            state = state.listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .reorderable(state)
                .detectReorderAfterLongPress(state),
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(chosen.size, key = { it }) { index ->
                val card = chosen[index]
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(1f)
                        .detectReorder(state)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(
                                2.dp,
                                borderColorFor(card.folder),
                                RoundedCornerShape(4.dp)
                            )
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFD9D9D9))
                            .combinedClickable(
                                onClick = { onSpeakWord(card.label) },
                                onLongClick = { onRemove(index) }
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(
                                model = Uri.parse(card.path),
                                imageLoader = imageLoader
                            ),
                            contentDescription = card.label,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        )
                        Text(card.label, fontSize = 12.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        }

        IconButton(onClick = onClearAll) {
            Icon(Icons.Default.Delete, contentDescription = "Clear all")
        }

        IconButton(onClick = onPlayStop) {
            Icon(Icons.Default.PlayArrow, contentDescription = "Play/Stop")
        }
    }
}



// ------------------ GREEN BAR ------------------
@Composable
fun GreenBar(labels: List<String>, spacing: Dp, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.background(Color(0xFFDFF0D8)).fillMaxWidth().padding(vertical = spacing),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(spacing), verticalAlignment = Alignment.CenterVertically) {
            labels.forEach { label ->
                Box(modifier = Modifier.size(50.dp)) {
                    AccTile(text = label, borderColor = Color.Black, modifier = Modifier.fillMaxSize(), onClick = {}, onLongClick = {})
                }
            }
        }
    }
}

// ------------------ TILE + NAV ------------------
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AccTile(text: String, borderColor: Color, modifier: Modifier = Modifier, onClick: () -> Unit, onLongClick: () -> Unit) {
    Box(
        modifier = modifier.border(2.dp, borderColor, RoundedCornerShape(4.dp)).clip(RoundedCornerShape(4.dp))
            .background(Color.LightGray).combinedClickable(onClick = onClick, onLongClick = onLongClick),
        contentAlignment = Alignment.Center
    ) { Text(text, fontSize = 12.sp) }
}

@Composable
fun BottomNavBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(Color(0xFFDFF0D8))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        IconButton(onClick = { }) { Icon(Icons.Default.Tune, contentDescription = "Quick Settings") }
        IconButton(onClick = { }) { Icon(Icons.Default.Keyboard, contentDescription = "Keyboard") }
        IconButton(onClick = { }) { Icon(Icons.Default.Home, contentDescription = "Home") }
        IconButton(onClick = { }) { Icon(Icons.Default.Add, contentDescription = "Create") }
        IconButton(onClick = { }) { Icon(Icons.Default.Settings, contentDescription = "Settings") }
    }
}

