package com.penny.app.ui

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import com.penny.app.PennyServices
import com.penny.app.R
import com.penny.app.core.CourseFlows
import com.penny.app.core.CourseText
import com.penny.app.core.LearningPath
import com.penny.app.core.LessonSpeech
import com.penny.app.core.Speech
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BackBar(title: @Composable () -> Unit, onBack: () -> Unit, actions: @Composable () -> Unit = {}) {
    TopAppBar(
        title = title,
        navigationIcon = {
            IconButton(onClick = onBack, modifier = Modifier.testTag("back")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = PennyPalette.text)
            }
        },
        actions = { actions() },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = PennyPalette.background),
    )
}

/** CoursePage (course_page.dart): banner, "Modules", one button per folder. */
@Composable
fun CourseListScreen(services: PennyServices, onBack: () -> Unit, openModule: (String) -> Unit) {
    var modules by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        modules = CourseFlows.modules(runCatching { services.courses.subfolders() }.getOrNull())
        isLoading = false
    }
    Scaffold(containerColor = PennyPalette.background, topBar = { BackBar({}, onBack) }) { inner ->
        Column(Modifier.fillMaxSize().padding(inner).padding(20.dp).testTag("course-page")) {
            Image(painterResource(R.drawable.modules1), null, Modifier.fillMaxWidth().height(250.dp), contentScale = ContentScale.Crop)
            Spacer(Modifier.height(20.dp))
            Text("Modules", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = PennyPalette.text)
            Spacer(Modifier.height(20.dp))
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(15.dp)) {
                val count = if (isLoading) 5 else modules.size // Skeletonizer shows 5 placeholders
                repeat(count) { i ->
                    val folder = modules.getOrNull(i)
                    // getModuleNames throws RangeError for an empty `_` part → that tile errors.
                    val label = if (isLoading) "Module Name" else runCatching { CourseText.moduleDisplayName(folder!!) }.getOrNull()
                    Button(
                        onClick = { if (folder != null) openModule(folder) },
                        enabled = !isLoading && label != null,
                        modifier = Modifier.fillMaxWidth().height(70.dp).testTag(if (isLoading) "module-skeleton-$i" else "module-$i"),
                        shape = RoundedCornerShape(15.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PennyPalette.button,
                            disabledContainerColor = PennyPalette.button.copy(alpha = 0.3f)),
                    ) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.PlayCircle, null, Modifier.size(32.dp), tint = PennyPalette.background)
                            Spacer(Modifier.width(8.dp))
                            Text(label ?: "Error", fontSize = 30.sp, fontWeight = FontWeight.SemiBold,
                                color = PennyPalette.background, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.testTag("module-name-$i"))
                        }
                    }
                }
            }
        }
    }
}

private val BACKGROUND_IMAGES = mapOf(
    "Personal Finance" to R.drawable.background1,
    "Banking And Accounts" to R.drawable.background2,
    "Credit And Debt Management" to R.drawable.background3,
)

private val PATH_COLOR = Color(0xFF8C4A4A) // rgb(140, 74, 74)
private val CIRCLE_FILL = Color(0xFFE7CECE) // rgb(231, 206, 206)

/** LearningPage (learning_page.dart): 8-node winding path over the module background. */
@Composable
fun LearningScreen(subfolder: String, onBack: () -> Unit, openLesson: (Int) -> Unit) {
    val moduleName = runCatching { CourseText.moduleDisplayName(subfolder) }.getOrDefault("")
    Scaffold(
        containerColor = PennyPalette.background,
        topBar = { BackBar({ Text(moduleName, modifier = Modifier.testTag("learning-title")) }, onBack) },
    ) { inner ->
        val background = BACKGROUND_IMAGES[moduleName]
        if (background == null) {
            // Flutter passes null into DecorationImage → TypeError at build → error widget.
            Box(Modifier.fillMaxSize().padding(inner).testTag("learning-error"), contentAlignment = Alignment.Center) {
                Text("Error", color = Color.Red)
            }
            return@Scaffold
        }
        val measurer = rememberTextMeasurer()
        val density = LocalDensity.current
        BoxWithConstraints(Modifier.fillMaxSize().padding(inner)) {
            val widthDp = maxWidth.value.toDouble()
            Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState(), reverseScrolling = true)) {
                Box(Modifier.fillMaxWidth().height(LearningPath.HEIGHT.dp).testTag("learning-path")) {
                    Image(painterResource(background), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    Canvas(
                        Modifier.fillMaxSize().pointerInput(widthDp) {
                            detectTapGestures { tap ->
                                val x = with(density) { tap.x.toDp().value.toDouble() }
                                val y = with(density) { tap.y.toDp().value.toDouble() }
                                LearningPath.hit(x, y, widthDp, LearningPath.HEIGHT)?.let(openLesson)
                            }
                        },
                    ) {
                        val px = density.density
                        fun o(p: LearningPath.Point) = Offset((p.x * px).toFloat(), (p.y * px).toFloat())
                        val pts = LearningPath.pathPoints(widthDp, LearningPath.HEIGHT)
                        val path = Path().apply {
                            moveTo(o(pts[0]).x, o(pts[0]).y)
                            for (i in 0 until pts.size - 1) {
                                val c = o(pts[i])
                                val next = o(pts[i + 1])
                                quadraticTo(c.x, c.y, (c.x + next.x) / 2, (c.y + next.y) / 2)
                            }
                            val a = o(pts[pts.size - 2])
                            val b = o(pts[pts.size - 1])
                            quadraticTo(a.x, a.y, b.x, b.y)
                        }
                        drawPath(path, PATH_COLOR, style = Stroke(12 * px))
                        val r = (LearningPath.CIRCLE_RADIUS * px).toFloat()
                        LearningPath.circles(widthDp, LearningPath.HEIGHT).forEachIndexed { i, p ->
                            drawCircle(CIRCLE_FILL, r, o(p))
                            drawCircle(PATH_COLOR, r, o(p), style = Stroke(12 * px))
                            val label = measurer.measure("${i + 1}", TextStyle(fontSize = 30.sp, color = PennyPalette.text))
                            drawText(label, topLeft = o(p) - Offset(label.size.width / 2f, label.size.height / 2f))
                        }
                    }
                }
            }
        }
    }
}

/** LessonPage (lesson_page.dart): markdown body + text-to-speech toggle. */
@Composable
fun LessonScreen(services: PennyServices, subfolder: String, index: Int, onBack: () -> Unit) {
    val context = LocalContext.current
    val speech = remember { services.speech?.invoke(context) ?: AndroidSpeech(context) }
    val scope = rememberCoroutineScope()
    var content by remember { mutableStateOf<String?>(null) } // `late String content`
    var isSpeaking by remember { mutableStateOf(false) }
    var fetches by remember { mutableIntStateOf(0) }
    var loaded by remember { mutableStateOf<String?>(null) }

    // FutureBuilder(future: fetchFile()) is created in build: every setState
    // (including toggling speech) starts a NEW fetch and shows the loader.
    LaunchedEffect(fetches) {
        loaded = null
        loaded = CourseFlows.lessonContent(
            runCatching { services.courses.file(subfolder, CourseText.lessonFileName(subfolder, index)) }.getOrNull(),
        )
        content = loaded
    }
    // dispose() → flutterTts.stop(); called directly: the composition's
    // coroutine scope is already cancelled here.
    DisposableEffect(Unit) { onDispose { speech.release() } }

    Scaffold(
        containerColor = PennyPalette.background,
        topBar = {
            BackBar(
                { Text(CourseText.lessonTitle(index), fontWeight = FontWeight.Bold, color = PennyPalette.text,
                    modifier = Modifier.testTag("lesson-title")) },
                onBack,
            ) {
                IconButton(
                    onClick = {
                        val text = content ?: return@IconButton // LateInitializationError, swallowed
                        scope.launch {
                            if (isSpeaking) {
                                speech.stop()
                                isSpeaking = false
                            } else {
                                speech.speak(LessonSpeech.spokenText(text), LessonSpeech.LANGUAGE, LessonSpeech.PITCH)
                                isSpeaking = true
                                speech.setOnComplete { isSpeaking = false } // after speak, like Flutter
                            }
                            fetches++
                        }
                    },
                    modifier = Modifier.testTag("lesson-speak"),
                ) {
                    Icon(if (isSpeaking) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        if (isSpeaking) "Stop reading" else "Read aloud", tint = PennyPalette.text)
                }
            }
        },
    ) { inner ->
        val markdown = loaded
        if (markdown == null) {
            Box(Modifier.fillMaxSize().padding(inner).testTag("lesson-loading"), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PennyPalette.text)
            }
        } else {
            Column(Modifier.fillMaxSize().padding(inner).verticalScroll(rememberScrollState()).padding(16.dp)
                .testTag("lesson-content")) {
                Markdown(markdown, colors = lessonMarkdownColors(), typography = lessonMarkdownTypography())
            }
        }
    }
}

/** MarkdownStyleSheet (lesson_page.dart:133-180): h1 28 / h2 24 / h3 20 bold, body 16 × 1.5. */
@Composable
private fun lessonMarkdownTypography() = markdownTypography(
    h1 = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, color = PennyPalette.text),
    h2 = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, color = PennyPalette.text),
    h3 = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PennyPalette.text),
    text = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, color = PennyPalette.text),
    paragraph = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, color = PennyPalette.text),
    bullet = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, color = PennyPalette.text),
    list = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, color = PennyPalette.text),
    quote = TextStyle(fontSize = 16.sp, fontStyle = FontStyle.Italic, color = PennyPalette.text.copy(alpha = 0.7f)),
    code = TextStyle(fontSize = 14.sp, fontFamily = FontFamily.Monospace, color = Color(0xDD000000)),
)

@Composable
private fun lessonMarkdownColors() = markdownColor(
    text = PennyPalette.text,
    codeBackground = Color(0xFFEEEEEE), // Colors.grey[200]
    inlineCodeBackground = Color(0xFFEEEEEE),
)

/** flutter_tts stand-in over android.speech.tts. */
class AndroidSpeech(context: Context) : Speech {
    @Volatile private var onComplete: () -> Unit = {}
    private val lock = Any()
    private var ready = false
    private var released = false
    private val pending = mutableListOf<() -> Unit>()

    // The init callback runs on a binder thread; `ready`/`pending` are guarded
    // so an utterance queued during init is never lost (review 010 note 2).
    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        val queued: List<() -> Unit> = synchronized(lock) {
            ready = status == TextToSpeech.SUCCESS && !released
            val toRun = if (ready) pending.toList() else emptyList()
            pending.clear()
            toRun
        }
        queued.forEach { it() }
    }.apply {
        setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) = onComplete()
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) = onComplete()
        })
    }

    override suspend fun speak(text: String, language: String, pitch: Float) {
        val run = {
            tts.language = Locale.forLanguageTag(language)
            tts.setPitch(pitch)
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "penny-lesson")
            Unit
        }
        val now = synchronized(lock) {
            if (released) return
            if (!ready) pending += run
            ready
        }
        if (now) run()
    }

    override suspend fun stop() {
        tts.stop()
    }

    override fun setOnComplete(onComplete: () -> Unit) {
        this.onComplete = onComplete
    }

    override fun release() {
        synchronized(lock) {
            released = true
            pending.clear()
        }
        tts.stop()
        tts.shutdown()
    }
}
