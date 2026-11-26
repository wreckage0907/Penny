package com.penny.app.ui

import android.content.Context
import android.speech.tts.UtteranceProgressListener
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikepenz.markdown.m3.Markdown
import com.penny.app.PennyServices
import com.penny.app.core.CourseFlows
import com.penny.app.core.CourseText
import com.penny.app.core.DashboardFlows
import com.penny.app.core.ExpenseDashboardText
import com.penny.app.core.LearnRules
import com.penny.app.core.McqChapters
import com.penny.app.core.McqQuestion
import com.penny.app.core.PlanMath
import com.penny.app.core.PlanMonth
import com.penny.app.core.QuizSession
import com.penny.app.core.QuizText
import com.penny.app.core.Speech
import com.penny.app.core.LessonSpeech
import com.penny.app.core.lessonTitles
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * v2.1 learning journey (artboard frames 09–12): the gated path with
 * rewards, the lesson reader with TTS, the boss quiz with saved sessions
 * and scoring, and results. Replaces the v1 course list / learning path /
 * lesson / practice screens; AndroidSpeech moves here from CourseScreens.
 */

// ── 09 · Learn path ────────────────────────────────────────────────────────

@Composable
fun LearnScreen(
    services: PennyServices,
    navigate: (String) -> Unit,
    initialUnit: String? = null,
    tab: PennyTab = PennyTab.LEARN,
) {
    var units by remember { mutableStateOf<List<String>>(emptyList()) }
    var titles by remember { mutableStateOf<Map<String, List<String>>>(emptyMap()) }
    var selected by remember { mutableStateOf<String?>(null) }
    var learn by remember { mutableStateOf(services.learn.state.value) }
    var rewardPreview by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(services.learn.state) { services.learn.state.collect { learn = it } }

    LaunchedEffect(Unit) {
        val list = CourseFlows.modules(services.courses.subfolders())
        units = list
        selected = initialUnit?.takeIf { it in list } ?: list.firstOrNull()
        list.forEach { unit -> titles = titles + (unit to services.courses.lessonTitles(unit)) }
    }

    val unit = selected ?: units.firstOrNull() ?: ""
    val lessonNames = titles[unit].orEmpty()
    val completed = LearnRules.completedCount(learn, unit)
    val unlocked = LearnRules.quizUnlocked(learn, unit)

    Column(Modifier.fillMaxSize().tag("learn-page")) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(top = 5.dp, bottom = 24.dp),
        ) {
            Row(Modifier.padding(top = 4.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Learn", style = PennyType.phoneTitle, modifier = Modifier.weight(1f).tag("learn-title"))
                Tag("🔥 ${learn.streakDays} days", kind = TagKind.GOLD, tag = "learn-streak")
                Tag("◈ ${learn.coins}", Modifier.padding(start = 8.dp), tag = "learn-coins")
            }
            Row(
                Modifier
                    .padding(bottom = 10.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                units.forEachIndexed { i, u ->
                    PillBtn(
                        "Unit ${i + 1} · ${CourseText.moduleDisplayName(u)}",
                        { selected = u },
                        selected = u == unit,
                        tag = "learn-unit-$i",
                    )
                }
            }
            CardBox(Modifier.padding(top = 2.dp), tag = "learn-progress") {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "${LearnRules.progressText(learn, unit)} · ${LearnRules.progressShare(learn, unit)}%",
                                style = PennyType.secondary,
                            )
                            Track(completed.toDouble() / LearnRules.LESSONS_PER_UNIT, Modifier.padding(top = 8.dp))
                        }
                        Tag("Boss · up to 75 ◈", kind = TagKind.GOLD, tag = "learn-boss-tag")
                    }
                    Hint(
                        if (unlocked) "All lessons complete — the Boss Quiz is unlocked."
                        else "Lesson ${completed + 1} is next. The Boss Quiz unlocks after all ${LearnRules.LESSONS_PER_UNIT} lessons.",
                        Modifier.padding(top = 8.dp),
                    )
                }
            }
            Box(
                Modifier
                    .padding(top = 12.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFFFBF7F4), Color(0xFFF5ECE7))))
                    .padding(16.dp)
                    .tag("learn-path"),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    (0 until LearnRules.LESSONS_PER_UNIT).forEach { i ->
                        val status = LearnRules.lessonStatus(learn, unit, i)
                        val title = lessonNames.getOrNull(i) ?: "Lesson ${i + 1}"
                        val medal = when (status) {
                            LearnRules.LessonStatus.COMPLETED -> MedalKind.DONE
                            LearnRules.LessonStatus.CURRENT -> MedalKind.CURRENT
                            LearnRules.LessonStatus.LOCKED -> MedalKind.LOCKED
                        }
                        Stop(
                            medal = medal,
                            right = i % 2 == 1,
                            card = {
                                StopCard(
                                    status = status,
                                    title = "${i + 1} · $title",
                                    meta = when (status) {
                                        LearnRules.LessonStatus.COMPLETED -> "Completed · +${LearnRules.LESSON_COINS} ◈ earned · Review"
                                        LearnRules.LessonStatus.CURRENT -> "4 min · TTS · +${LearnRules.LESSON_COINS} ◈ on first completion"
                                        LearnRules.LessonStatus.LOCKED -> "Complete Lesson $completed to unlock"
                                    },
                                    current = status == LearnRules.LessonStatus.CURRENT,
                                    actionLabel = "Start Lesson ${i + 1}",
                                    onOpen = { navigate("lesson/$unit/$i") },
                                )
                            },
                            tag = "learn-stop-$i",
                        )
                    }
                    Stop(
                        medal = if (unit in learn.badges) MedalKind.BOSS_DONE else MedalKind.BOSS,
                        right = false,
                        card = {
                            StopCard(
                                status = if (unlocked) LearnRules.LessonStatus.CURRENT else LearnRules.LessonStatus.LOCKED,
                                title = "Chapter Boss Quiz",
                                meta = if (unlocked) "4/5 correct earns the badge · up to 75 ◈ total"
                                else "Locked · complete all ${LearnRules.LESSONS_PER_UNIT} lessons",
                                current = unlocked,
                                actionLabel = "Start Boss Quiz",
                                onOpen = if (unlocked) ({ navigate("quiz/$unit") }) else null,
                            )
                        },
                        tag = "learn-boss",
                    )
                }
            }
            SectionHead("Rewards Vault") {
                Text("${learn.coins} ◈ available", color = PennyPalette.gold, style = PennyType.metaBold)
            }
            CardBox(tag = "learn-vault") {
                Column {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        RewardPill("🛡️ Streak Freeze · 100 ◈", Modifier.weight(1f)) { rewardPreview = "freeze" }
                        RewardPill("📊 Tax & SIP Sheet · 150 ◈", Modifier.weight(1f)) { rewardPreview = "sheet" }
                    }
                    Hint("Tap to preview the reward, cost, and effect before spending coins. No instant redemption.")
                }
            }
        }
        BottomNavBar(tab, { t -> navigate(tabRoute(t)) })
    }

    rewardPreview?.let { which ->
        val (name, cost, effect) = when (which) {
            "freeze" -> Triple("🛡️ Streak Freeze", 100, "Protects your streak for one missed day. One freeze is consumed automatically the first day you're inactive.")
            else -> Triple("📊 Tax & SIP Sheet", 150, "A quick-reference sheet of the tax brackets and SIP formulas from the Tax & SIPs unit.")
        }
        ConfirmModal(
            name, onDismiss = { rewardPreview = null },
            headerIcon = Icons.Filled.Star, headerTint = PennyPalette.gold, headerBg = PennyPalette.goldBg,
            tag = "reward-preview",
        ) {
            Text("$cost ◈", style = TextStyle(fontSize = 23.sp, fontWeight = FontWeight.W800), modifier = Modifier.padding(top = 8.dp))
            Text(effect, color = PennyPalette.muted, style = PennyType.secondaryPlain, modifier = Modifier.padding(top = 8.dp))
            Hint("Preview only — redemption ships with the real rewards backend.", Modifier.padding(top = 10.dp))
            PennyButton("Got it", { rewardPreview = null }, Modifier.padding(top = 12.dp), tag = "reward-close")
        }
    }
}

@Composable
private fun RewardPill(label: String, modifier: Modifier = Modifier, onPreview: () -> Unit) {
    PillBtn(label, onPreview, modifier, goldy = true, tag = "reward-${label.take(1)}")
}

private enum class MedalKind { DONE, CURRENT, LOCKED, BOSS, BOSS_DONE }

@Composable
private fun Stop(medal: MedalKind, right: Boolean, card: @Composable () -> Unit, tag: String) {
    Row(
        Modifier.fillMaxWidth().defaultMinSize(minHeight = 86.dp).tag(tag),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (right) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) { card() }
            Medal(medal)
        } else {
            Medal(medal)
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) { card() }
        }
    }
}

@Composable
private fun Medal(kind: MedalKind) {
    val size = if (kind == MedalKind.CURRENT) 60.dp else 48.dp
    val shape = if (kind == MedalKind.BOSS || kind == MedalKind.BOSS_DONE) RoundedCornerShape(16.dp) else CircleShape
    val fill: Color = when (kind) {
        MedalKind.DONE -> PennyPalette.pos
        MedalKind.CURRENT -> PennyPalette.brand
        MedalKind.LOCKED -> Color(0xFFE3DAD3)
        MedalKind.BOSS, MedalKind.BOSS_DONE -> Color.Transparent
    }
    val bg: Brush = when (kind) {
        MedalKind.BOSS, MedalKind.BOSS_DONE ->
            Brush.linearGradient(listOf(Color(0xFFC88A36), Color(0xFF9A6B2F)))
        else -> Brush.linearGradient(listOf(fill, fill)) // uniform = solid
    }
    val tint = when (kind) {
        MedalKind.DONE -> Color.White
        MedalKind.CURRENT -> PennyPalette.bg
        MedalKind.LOCKED -> PennyPalette.muted
        MedalKind.BOSS, MedalKind.BOSS_DONE -> Color.White
    }
    Box(
        Modifier
            .size(size)
            .clip(shape)
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        when (kind) {
            MedalKind.DONE -> Icon(Icons.Filled.Check, contentDescription = null, tint = tint)
            MedalKind.CURRENT -> Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = tint)
            MedalKind.LOCKED -> Icon(Icons.Filled.Lock, contentDescription = null, tint = tint)
            MedalKind.BOSS, MedalKind.BOSS_DONE -> Icon(Icons.Filled.Star, contentDescription = null, tint = tint)
        }
    }
}

@Composable
private fun StopCard(
    status: LearnRules.LessonStatus,
    title: String,
    meta: String,
    current: Boolean,
    actionLabel: String,
    onOpen: (() -> Unit)?,
) {
    Column(
        Modifier
            .defaultMinSize(minWidth = 218.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (status == LearnRules.LessonStatus.LOCKED) PennyPalette.sand else PennyPalette.surface)
            .border(
                width = if (current) 2.dp else 1.dp,
                color = if (current) PennyPalette.brand else PennyPalette.ink.copy(alpha = 0.10f),
                shape = RoundedCornerShape(14.dp),
            )
            .padding(horizontal = 11.dp, vertical = 10.dp),
    ) {
        Text(title, style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.W800, lineHeight = 18.sp))
        Text(meta, color = PennyPalette.muted, style = TextStyle(fontSize = 11.sp, lineHeight = 16.sp), modifier = Modifier.padding(top = 4.dp))
        if (current && onOpen != null) {
            PennyButton(actionLabel, onOpen, Modifier.padding(top = 9.dp), small = true, tag = "stop-action")
        }
    }
}

// ── 10 · Lesson reader + TTS ──────────────────────────────────────────────

/** Android TextToSpeech behind the [Speech] seam (moved from CourseScreens). */
class AndroidSpeech(context: Context) : Speech {
    private var tts: android.speech.tts.TextToSpeech? = null
    private var ready = false
    private val pending = ArrayDeque<Triple<String, String, Double>>()
    private var onComplete: () -> Unit = {}

    init {
        tts = android.speech.tts.TextToSpeech(context) { status ->
            ready = status == android.speech.tts.TextToSpeech.SUCCESS
            if (ready) drain()
        }
    }

    override suspend fun speak(text: String, language: String, pitch: Float, rate: Double) {
        enqueue(text, language, pitch, rate)
    }

    private fun enqueue(text: String, language: String, pitch: Float, rate: Double) {
        val engine = tts ?: return
        if (!ready) {
            pending.add(Triple(text, language, rate))
            return
        }
        engine.language = java.util.Locale.forLanguageTag(language)
        engine.setPitch(pitch)
        engine.setSpeechRate(rate.toFloat())
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) { onComplete() }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) { onComplete() }
        })
        engine.speak(text, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "lesson")
    }

    private fun drain() {
        while (pending.isNotEmpty()) {
            val (text, language, rate) = pending.removeFirst()
            enqueue(text, language, LessonSpeech.PITCH, rate)
        }
    }

    override suspend fun stop() { tts?.stop() }

    override fun setOnComplete(onComplete: () -> Unit) { this.onComplete = onComplete }

    override fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ready = false
    }
}

@Composable
fun LessonScreen(
    services: PennyServices,
    unit: String,
    index: Int,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var content by remember { mutableStateOf<String?>(null) }
    var speaking by remember { mutableStateOf(false) }
    var rateIndex by remember { mutableStateOf(LessonSpeech.DEFAULT_RATE_INDEX) }
    var needs by remember { mutableStateOf(0.0) }
    var learn by remember { mutableStateOf(services.learn.state.value) }
    LaunchedEffect(services.learn.state) { services.learn.state.collect { learn = it } }
    var speech by remember { mutableStateOf<Speech?>(null) }
    DisposableEffect(Unit) {
        onDispose { speech?.release() }
    }

    LaunchedEffect(unit, index) {
        val today = LocalDate.now()
        val month = ExpenseDashboardText.initialMonth(today.monthValue)
        val user = DashboardFlows.username(services.auth.customClaims(), services.prefs.username()) ?: ""
        val expenses = try {
            services.expenses(user).getExpenses(today.year, month)
        } catch (e: Exception) {
            kotlinx.serialization.json.JsonObject(emptyMap())
        }
        needs = PlanMath.summary(
            PlanMonth(0.0, PlanMath.monthCategories(expenses, today.year, month), 0.0, 0.0),
        ).needs.actual
        content = try {
            val prefix = unit.split('_')[0]
            CourseFlows.lessonContent(services.courses.file(unit, "${prefix}_${index + 1}"))
        } catch (e: Exception) {
            CourseFlows.FAILED_CONTENT
        }
    }

    val title = content?.lineSequence()?.firstOrNull { it.startsWith("# ") }?.removePrefix("# ")
        ?: "Lesson ${index + 1}"
    val completedCount = LearnRules.completedCount(learn, unit)

    Column(Modifier.fillMaxSize().tag("lesson-page")) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(top = 5.dp, bottom = 12.dp),
        ) {
            NavBar(
                "Lesson ${index + 1} of ${LearnRules.LESSONS_PER_UNIT}",
                onBack, side = title, tag = "lesson-nav",
            )
            Track(completedCount.toDouble() / LearnRules.LESSONS_PER_UNIT, tag = "lesson-progress")
            Row(
                Modifier
                    .padding(top = 14.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Brush.linearGradient(listOf(PennyPalette.heroInk, Color(0xFF4A3B41))))
                    .padding(10.dp)
                    .tag("lesson-audio"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                IconBtn(
                    if (speaking) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                    if (speaking) "Stop lesson" else "Play lesson",
                    {
                        val engine = speech
                            ?: (services.speech?.invoke(context) ?: AndroidSpeech(context)).also { speech = it }
                        scope.launch {
                            if (speaking) {
                                engine.stop()
                                speaking = false
                            } else {
                                engine.setOnComplete { speaking = false }
                                engine.speak(
                                    LessonSpeech.spokenText(content ?: ""),
                                    LessonSpeech.LANGUAGE,
                                    LessonSpeech.PITCH,
                                    LessonSpeech.RATES[rateIndex],
                                )
                                speaking = true
                            }
                        }
                    },
                    tint = Color(0xFFF0D7D3),
                    bg = PennyPalette.blush.copy(alpha = 0.16f),
                    tag = "lesson-play",
                )
                Column(Modifier.weight(1f)) {
                    Text("LISTEN · PENNY READS THIS", color = PennyPalette.bg.copy(alpha = 0.84f), style = PennyType.eyebrow)
                    Text(
                        "Narration at ${LessonSpeech.RATES[rateIndex]}×",
                        color = PennyPalette.bg.copy(alpha = 0.84f),
                        style = TextStyle(fontSize = 11.sp),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                PillBtn(
                    String.format("%.1f×", LessonSpeech.RATES[rateIndex]),
                    { rateIndex = (rateIndex + 1) % LessonSpeech.RATES.size },
                    tag = "lesson-speed",
                )
            }
            Text(
                title,
                style = TextStyle(fontSize = 25.sp, fontWeight = FontWeight.W800, lineHeight = 31.sp),
                modifier = Modifier.padding(top = 18.dp, bottom = 12.dp).tag("lesson-title"),
            )
            LearnRules.emergencyTarget(title, needs)?.let { target ->
                Notice(target, Modifier, NoticeKind.GOLD, boldParts = listOf("Your target:"), tag = "lesson-target")
                Spacer(Modifier.height(14.dp))
            }
            Box(Modifier.tag("lesson-content")) {
                Markdown(content ?: "")
            }
        }
        Column(Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
            Text(
                "First completion only · Review never duplicates rewards",
                color = PennyPalette.muted,
                style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.W800),
                modifier = Modifier.padding(bottom = 8.dp).align(Alignment.CenterHorizontally),
            )
            PennyButton(
                "Mark Lesson Complete · +${LearnRules.LESSON_COINS} ◈",
                {
                    scope.launch {
                        services.learn.set(
                            LearnRules.completeLesson(services.learn.state.value, unit, index, LocalDate.now()),
                        )
                        onBack()
                    }
                },
                tag = "lesson-complete",
            )
        }
    }
}

// ── 11 · Boss quiz ────────────────────────────────────────────────────────

@Composable
fun QuizScreen(
    services: PennyServices,
    unit: String,
    review: Boolean = false,
    onBack: () -> Unit,
    navigate: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var questions by remember { mutableStateOf<List<McqQuestion>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var session by remember { mutableStateOf<QuizSession?>(null) }
    var cursor by remember { mutableStateOf(0) }
    var resumedNotice by remember { mutableStateOf(false) }
    val unitLabel = CourseText.moduleDisplayName(unit)

    LaunchedEffect(unit, review) {
        val units = CourseFlows.modules(services.courses.subfolders())
        val slug = McqChapters.slug(units.indexOf(unit).coerceIn(0, 3)) ?: return@LaunchedEffect
        try {
            val chapter = services.mcq.fetchQuestions(slug)
            questions = chapter.questions.values.take(LearnRules.QUIZ_QUESTIONS).toList()
            session = if (review) {
                services.learn.state.value.lastSession
            } else {
                // A saved PARTIAL session for this unit resumes where it left off.
                val saved = services.learn.state.value.savedSession
                if (saved?.unit == unit && saved.answers.size < saved.total) {
                    resumedNotice = true
                    saved
                } else {
                    LearnRules.startQuiz(services.learn.state.value, unit, LearnRules.QUIZ_QUESTIONS).savedSession
                }
            }
            cursor = session?.currentIndex ?: 0
        } catch (e: Exception) {
            error = e.message
        }
    }

    val answered = session?.answers?.size ?: 0

    Column(Modifier.fillMaxSize().tag("quiz-page")) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(top = 5.dp, bottom = 12.dp),
        ) {
            NavBar(
                if (review) "$unitLabel · Review" else "$unitLabel · Quiz",
                onBack,
                side = "${(cursor + 1).coerceAtMost(session?.total ?: LearnRules.QUIZ_QUESTIONS)} / ${session?.total ?: LearnRules.QUIZ_QUESTIONS}",
                tag = "quiz-nav",
            )
            if (resumedNotice) {
                Notice(
                    "Resumed saved session. A new quiz starts at Question 1. Retakes never duplicate coin rewards.",
                    boldParts = listOf("Resumed saved session."),
                    tag = "quiz-resumed",
                )
            }
            Row(Modifier.padding(vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                repeat(session?.total ?: LearnRules.QUIZ_QUESTIONS) { i ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(PennyShape.pill)
                            .background(if (i < answered) PennyPalette.brand else PennyPalette.sand),
                    )
                }
            }
            val qs = questions
            when {
                error != null -> Notice("Couldn't load questions ($error). Your progress is safe.", Modifier, NoticeKind.NEG, tag = "quiz-error")
                qs == null || session == null -> Skeleton(Modifier.fillMaxWidth().height(320.dp))
                else -> {
                    val qIndex = cursor.coerceIn(0, qs.lastIndex)
                    QuestionCard(
                        question = qs[qIndex],
                        index = qIndex,
                        answered = session!!.answerOf(qIndex),
                        review = review,
                        onAnswer = { optionIndex ->
                            val updated = LearnRules.answer(session!!, qIndex, optionIndex, QuizText.isCorrect(qs[qIndex], optionIndex))
                            session = updated
                            scope.launch {
                                services.learn.set(services.learn.state.value.copy(savedSession = updated))
                            }
                        },
                    )
                }
            }
            Hint("Answer locked. Your progress is saved when you continue.", Modifier.padding(top = 4.dp))
        }
        Box(Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
            val s = session
            val onLast = s != null && cursor >= s.total - 1
            val currentAnswered = s?.answerOf(cursor) != null
            when {
                review -> PennyButton("Done", onBack, tag = "quiz-done")
                s == null -> Skeleton(Modifier.fillMaxWidth().height(48.dp))
                !currentAnswered -> PennyButton(
                    "Answer to continue", {}, kind = BtnKind.SECONDARY, tag = "quiz-next",
                )
                onLast -> PennyButton(
                    "Finish Quiz",
                    {
                        scope.launch {
                            val state = services.learn.state.value
                            val outcome = LearnRules.finish(state, unit, s)
                            services.learn.set(
                                LearnRules.apply(state, unit, s, outcome, LocalDate.now())
                                    .copy(lastOutcome = outcome, lastSession = s),
                            )
                            navigate("results/$unit")
                        }
                    },
                    tag = "quiz-finish",
                )
                else -> PennyButton(
                    "Next Question",
                    { cursor = session?.currentIndex ?: cursor },
                    tag = "quiz-next",
                )
            }
        }
    }
}

@Composable
private fun QuestionCard(
    question: McqQuestion,
    index: Int,
    answered: com.penny.app.core.QuizAnswer?,
    review: Boolean,
    onAnswer: (Int) -> Unit,
) {
    CardBox(tag = "quiz-card") {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Eyebrow("Question ${index + 1}")
                Spacer(Modifier.weight(1f))
                Tag("Up to 75 ◈ total", kind = TagKind.GOLD, tag = "quiz-reward-tag")
            }
            Text(
                question.question,
                style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.W800, lineHeight = 26.sp),
                modifier = Modifier.padding(top = 12.dp, bottom = 16.dp).tag("quiz-question"),
            )
            question.options.forEachIndexed { i, raw ->
                val correct = QuizText.isCorrect(question, i)
                val picked = answered?.optionIndex == i
                val state = when {
                    answered != null && picked && correct -> OptionState.OK
                    answered != null && picked && !correct -> OptionState.BAD
                    answered != null && correct -> OptionState.OK_REVEALED
                    else -> OptionState.PLAIN
                }
                OptionRow(
                    letter = "${'A' + i}",
                    text = QuizText.optionText(raw, i),
                    state = state,
                    enabled = answered == null && !review,
                    onClick = { onAnswer(i) },
                    tag = "quiz-option-$i",
                )
            }
            if (answered != null) {
                Notice(question.hint, Modifier.padding(top = 12.dp), NoticeKind.GOLD, tag = "quiz-explanation")
            }
        }
    }
}

private enum class OptionState { PLAIN, OK, BAD, OK_REVEALED }

@Composable
private fun OptionRow(
    letter: String,
    text: String,
    state: OptionState,
    enabled: Boolean,
    onClick: () -> Unit,
    tag: String,
) {
    val (border, bg, fg) = when (state) {
        OptionState.PLAIN -> Triple(PennyPalette.ink.copy(alpha = 0.10f), PennyPalette.bg, PennyPalette.muted)
        OptionState.OK, OptionState.OK_REVEALED -> Triple(PennyPalette.pos, PennyPalette.posBg, Color(0xFF22573D))
        OptionState.BAD -> Triple(PennyPalette.neg.copy(alpha = 0.45f), PennyPalette.negBg, Color(0xFF8A3232))
    }
    Row(
        Modifier
            .padding(bottom = 10.dp)
            .tag(tag)
            .fillMaxWidth()
            .defaultMinSize(minHeight = 58.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Box(
            Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    when (state) {
                        OptionState.OK -> PennyPalette.pos
                        OptionState.BAD -> PennyPalette.neg
                        else -> PennyPalette.sand
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                letter,
                color = if (state == OptionState.PLAIN) PennyPalette.muted else Color.White,
                style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.W800),
            )
        }
        Text(text, color = fg, style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.W700, lineHeight = 20.sp), modifier = Modifier.weight(1f))
        when (state) {
            OptionState.OK -> Text("+15 ◈ ✓", color = fg, style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.W900))
            OptionState.BAD -> Text("✕ Wrong", color = fg, style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.W900))
            else -> {}
        }
    }
}

// ── 12 · Results ──────────────────────────────────────────────────────────

@Composable
fun ResultsScreen(
    services: PennyServices,
    unit: String,
    onBack: () -> Unit,
    navigate: (String) -> Unit,
) {
    var learn by remember { mutableStateOf(services.learn.state.value) }
    LaunchedEffect(services.learn.state) { services.learn.state.collect { learn = it } }
    var units by remember { mutableStateOf<List<String>>(emptyList()) }
    LaunchedEffect(Unit) { units = CourseFlows.modules(services.courses.subfolders()) }

    val outcome = learn.lastOutcome
    val unitLabel = CourseText.moduleDisplayName(unit)
    val badgeEarned = unit in learn.badges

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(top = 5.dp, bottom = 24.dp)
            .tag("results-page"),
    ) {
        NavBar("Chapter Results", onBack, tag = "results-nav")
        Box(
            Modifier
                .clip(RoundedCornerShape(22.dp))
                .background(PennyPalette.soft)
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Box(
                    Modifier
                        .size(78.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFFC88A36), Color(0xFF9A6B2F))))
                        .tag("results-badge"),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(38.dp))
                }
                Eyebrow("Chapter Boss complete", Modifier.padding(top = 12.dp))
                Text(
                    "${outcome?.correct ?: 0} / ${outcome?.total ?: LearnRules.QUIZ_QUESTIONS}",
                    style = TextStyle(fontSize = 48.sp, fontWeight = FontWeight.W900, letterSpacing = (-1.6).sp),
                    modifier = Modifier.padding(top = 8.dp).tag("results-score"),
                )
                Text(if (badgeEarned) "$unitLabel Badge earned" else "$unitLabel quiz complete", style = PennyType.section)
                Text(
                    "You answered ${outcome?.correct ?: 0} questions correctly. Your best score is saved.",
                    color = PennyPalette.muted,
                    style = PennyType.secondaryPlain,
                    modifier = Modifier.padding(top = 6.dp),
                )
                Tag(
                    "+${outcome?.coinsAwarded ?: 0} ◈ · ${outcome?.correct ?: 0} × ${LearnRules.QUESTION_COINS}",
                    Modifier.padding(top = 10.dp), kind = TagKind.GOLD, tag = "results-reward",
                )
            }
        }
        Notice(
            "Reward settled once. New balance: ${learn.coins} ◈. Future retakes are for practice and do not duplicate coins.",
            Modifier.padding(top = 16.dp), NoticeKind.POS,
            icon = Icons.Filled.Check,
            boldParts = listOf("Reward settled once."),
            tag = "results-settled",
        )
        SectionHead("What happens next?")
        ListGroup {
            ListRow("Review answers", onClick = { navigate("quiz/$unit?review=true") }, value = "${outcome?.total ?: LearnRules.QUIZ_QUESTIONS} questions", tag = "results-review")
            ListDivider()
            val nextUnit = units.getOrNull(units.indexOf(unit) + 1)
            if (nextUnit != null) {
                ListRow(
                    "Continue to Unit ${units.indexOf(nextUnit) + 1}",
                    onClick = { navigate("learn?unit=${nextUnit}") },
                    value = CourseText.moduleDisplayName(nextUnit),
                    tag = "results-next-unit",
                )
                ListDivider()
            }
            ListRow("Retake for practice", onClick = { navigate("quiz/$unit") }, value = "No new coins", tag = "results-retake")
        }
        PennyButton("Back to Learn", { onBack() }, Modifier.padding(top = 18.dp), tag = "results-back")
    }
}
