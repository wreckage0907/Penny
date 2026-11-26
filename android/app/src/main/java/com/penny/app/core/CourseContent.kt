package com.penny.app.core

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Course content seam: `GET /subfolder` and `GET /get-file/{subfolder}/{file}`
 * (backend/routers/file.py). Raw JSON like the other clients. A missing file
 * is a backend 500 (blob download raises) → [CourseContentException].
 */
interface CourseContent {
    /** `{"title": [...]}` — a Python set: arbitrary order, may contain "". */
    suspend fun subfolders(): JsonObject

    /** `{"content": "<markdown>"}`. */
    suspend fun file(subfolder: String, fileName: String): JsonObject
}

class CourseContentException(message: String) : RuntimeException(message)

/**
 * Sample course content until the real content store is chosen. Folder names
 * use the `N_snake_case` convention the Flutter app parses; the three display
 * names match learning_page.dart's background-image keys. The title list is
 * deliberately unordered and includes "" (a blob named `content/`), like the
 * backend's set.
 */
class FakeCourseContent(
    private val modules: Map<String, List<String>> = SAMPLE,
) : CourseContent {

    override suspend fun subfolders(): JsonObject {
        val names = modules.keys.reversed() + ""
        return JsonObject(mapOf("title" to JsonArray(names.map { JsonPrimitive(it) })))
    }

    override suspend fun file(subfolder: String, fileName: String): JsonObject {
        val lessons = modules[subfolder] ?: throw CourseContentException("500")
        // v2.1: the Learn path asks for the unit's lesson titles in one call.
        if (fileName == "titles") {
            return JsonObject(
                mapOf("titles" to JsonArray(lessons.map { JsonPrimitive(it) })),
            )
        }
        val prefix = subfolder.split('_')[0]
        val index = fileName.removePrefix("${prefix}_").toIntOrNull()?.minus(1)
        val title = index?.let { lessons.getOrNull(it) } ?: throw CourseContentException("500")
        return JsonObject(mapOf("content" to JsonPrimitive(lessonMarkdown(title, index + 1))))
    }

    companion object {
        val SAMPLE: Map<String, List<String>> = linkedMapOf(
            "1_personal_finance" to listOf(
                "What is personal finance", "Needs vs wants", "The 50/30/20 budget", "Tracking expenses",
                "Emergency funds", "Saving goals", "Intro to investing", "Review",
            ),
            "2_banking_and_accounts" to listOf(
                "How banks work", "Savings accounts", "Current accounts", "Fixed deposits",
                "Interest explained", "Debit vs credit cards", "Online banking safety", "Review",
            ),
            "3_credit_and_debt_management" to listOf(
                "What is credit", "Credit scores", "Good vs bad debt", "Loans and EMIs",
                "Credit card interest", "Paying off debt", "Avoiding debt traps", "Review",
            ),
        )

        fun lessonMarkdown(title: String, number: Int): String = """
            # Lesson $number: $title

            Welcome to **$title**. This lesson is *sample content* served by the
            fake course store until the real content backend is chosen.

            ## Key ideas

            - Spend less than you earn.
            - Track every expense (see [the tracker](https://example.invalid/tracker)).
            * Review your budget every month.

            ## Try it

            Write down three `expenses` from this week and sort them into needs and wants.

            > Small steps, repeated, build strong habits.
        """.trimIndent()
    }
}

/** Course/lesson page decisions (course_page.dart:35-61, lesson_page.dart:37-51). */
object CourseFlows {

    /**
     * fetchFolderNames: `List<String>.from(data['title'])`, drop empties,
     * numeric-prefix sort (004). Any failure (missing/non-list title,
     * non-string entry, transport) → the list stays empty, loader off.
     */
    fun modules(response: JsonObject?): List<String> {
        val titles = response?.get("title") as? JsonArray ?: return emptyList()
        val names = titles.map { (it as? JsonPrimitive)?.takeIf { p -> p.isString }?.content ?: return emptyList() }
        return CourseText.sortModules(names)
    }

    const val FAILED_CONTENT = "Failed to load content"

    /** fetchFile: `data['content']` (String) or the literal failure text. */
    fun lessonContent(response: JsonObject?): String =
        (response?.get("content") as? JsonPrimitive)?.takeIf { it.isString }?.content ?: FAILED_CONTENT

    /** Background images exist for exactly three display names (learning_page.dart:17-21). */
    val BACKGROUNDS = listOf("Personal Finance", "Banking And Accounts", "Credit And Debt Management")
}

/**
 * The learning path CustomPainter geometry (learning_page.dart:77-182), in
 * logical pixels, with Dart's operation order. Circle i is lesson i + 1;
 * lesson 1 sits at the bottom (the page scrolls in reverse).
 */
object LearningPath {
    const val CIRCLE_RADIUS = 40.0
    const val HEIGHT = 1900.0
    const val LESSONS = 8

    data class Point(val x: Double, val y: Double)

    /** points2: the circle centres. */
    fun circles(w: Double, h: Double): List<Point> {
        val half = w / 2
        return listOf(
            Point(half, 4 * h / 5),
            Point(w / 4, ((4 * h / 5) + (3 * h / 5)) / 2),
            Point(half, 3 * h / 5),
            Point(3 * w / 4, ((3 * h / 5) + (2 * h / 5)) / 2),
            Point(half, 2 * h / 5),
            Point(w / 4, ((2 * h / 5) + (h / 5)) / 2),
            Point(half, h / 5),
            Point(3 * w / 4, h / 10),
        )
    }

    /** points1: the control points of the winding path (offset from the circles). */
    fun pathPoints(w: Double, h: Double): List<Point> {
        val half = w / 2
        return listOf(
            Point(half, 4 * h / 5),
            Point(160 - w / 4, ((4 * h / 5) + (3 * h / 5)) / 2),
            Point(half, 3 * h / 5),
            Point(40 + 3 * w / 4, ((3 * h / 5) + (2 * h / 5)) / 2),
            Point(half, 2 * h / 5),
            Point(160 - w / 4, ((2 * h / 5) + (h / 5)) / 2),
            Point(half, h / 5),
            Point(10 + 3 * w / 4, h / 10),
        )
    }

    /** The first circle whose centre is within the radius of the tap (:172-177). */
    fun hit(x: Double, y: Double, w: Double, h: Double): Int? =
        circles(w, h).indexOfFirst { Math.hypot(x - it.x, y - it.y) <= CIRCLE_RADIUS }.takeIf { it >= 0 }
}

/** Text-to-speech seam (flutter_tts). Android implementation lives in the UI layer. */
interface Speech {
    suspend fun speak(text: String, language: String, pitch: Float, rate: Double = 1.0)
    suspend fun stop()
    fun setOnComplete(onComplete: () -> Unit)

    /**
     * Stop and free the engine (review 010: flutter_tts is one app-wide
     * plugin, but a TextToSpeech per lesson must be shut down on dispose).
     * Synchronous so it can run directly in onDispose.
     */
    fun release()
}

/** toggleSpeak (lesson_page.dart:68-89): en-US, pitch 0.8, cleaned markdown. */
object LessonSpeech {
    const val LANGUAGE = "en-US"
    const val PITCH = 0.8f

    /** v2.1 narration speeds (frame 10's 1.0× button). */
    val RATES = listOf(0.8, 1.0, 1.2, 1.5)
    const val DEFAULT_RATE_INDEX = 1

    fun spokenText(markdown: String): String = LessonText.cleanMarkdownText(markdown)
}

/** v2.1: lesson titles for the Learn path (frame 09's stop cards). */
suspend fun CourseContent.lessonTitles(subfolder: String): List<String> {
    val response = try {
        file(subfolder, "titles")
    } catch (e: Exception) {
        return emptyList()
    }
    val titles = (response["titles"] as? kotlinx.serialization.json.JsonArray) ?: return emptyList()
    return titles.mapNotNull { (it as? kotlinx.serialization.json.JsonPrimitive)?.takeIf { p -> p.isString }?.content }
}
