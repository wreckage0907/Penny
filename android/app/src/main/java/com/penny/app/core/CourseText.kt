package com.penny.app.core

/**
 * Pure text logic of the course flow:
 * course_page.dart:11-15 (capitalize), :42-49 (filter + numeric-prefix sort),
 * :63-66 (getModuleNames); learning_page.dart:29-30 (lesson title/file name).
 */
object CourseText {

    /**
     * The `capitalize()` extension (course_page.dart:11-15):
     * `this[0].toUpperCase() + substring(1).toLowerCase()`. `this[0]` is a
     * UTF-16 code unit, so a leading surrogate pair is never uppercased.
     * Empty input throws like Dart's RangeError (index).
     */
    fun capitalize(s: String): String {
        if (s.isEmpty()) throw IndexOutOfBoundsException(DartText.EMPTY_INDEX_RANGE_ERROR)
        return DartText.toUpperCase(s.substring(0, 1)) + DartText.toLowerCase(s.substring(1))
    }

    /**
     * `getModuleNames` (course_page.dart:63-66): drop the first `_` part,
     * capitalize the rest, join with spaces. An empty part (`1_a__b`,
     * trailing `_`) throws — in Flutter that kills the list item's build.
     */
    fun moduleDisplayName(folder: String): String =
        folder.split('_').drop(1).joinToString(" ") { capitalize(it) }

    /** Sort key (course_page.dart:46-47): `int.tryParse(split('_')[0]) ?? 0`. */
    fun moduleSortKey(folder: String): Long =
        DartText.intTryParse(folder.split('_')[0]) ?: 0L

    /**
     * fetchFolderNames list shaping (course_page.dart:42-49): drop empty
     * names, then order by [moduleSortKey]. Ties keep input order (stable);
     * Dart's List.sort is identical up to 33 elements (insertion sort) and
     * unstable beyond — see LEDGER 004 for why that divergence is accepted.
     */
    fun sortModules(titles: List<String>): List<String> =
        titles.filter { it.isNotEmpty() }.sortedBy(::moduleSortKey)

    /** LessonPage title (learning_page.dart:29). */
    fun lessonTitle(index: Int): String = "Lesson ${index + 1}"

    /** LessonPage fileName (learning_page.dart:30): first `_` part + 1-based index. */
    fun lessonFileName(subfolder: String, index: Int): String =
        "${subfolder.split('_')[0]}_${index + 1}"
}

/**
 * `cleanMarkdownText` (lesson_page.dart:53-66): the text-to-speech cleaner.
 * Five global regex replacements in Dart order, then Dart trim. Quirks kept:
 * `#+\s` also eats `C# ` mid-sentence; stars go before the list-marker step,
 * so `* item` bullets leave a space; any `(…)` on one line is deleted (not
 * just link targets), lazily, so nested parens leave a `)`; `_`, `~`, `>`,
 * `+`, `1.` and HTML survive. Regexes use Dart's character sets
 * ([DartText]) because Java's `\s`, `.` and multiline `^` differ.
 */
object LessonText {
    private const val WS = "[${DartText.REGEX_WHITESPACE}]"
    private val HEADING = Regex("#+$WS")
    private val EMPHASIS = Regex("\\*{1,2}")

    /** Dart `^` with multiLine: start of input or after any single terminator. */
    private val LIST_MARKER =
        Regex("(?:^|(?<=[${DartText.REGEX_LINE_TERMINATORS}]))$WS*[-*]$WS")
    private val CODE = Regex("`{1,3}")
    private val LINK_PARTS = Regex("\\[|\\]|\\([^${DartText.REGEX_LINE_TERMINATORS}]*?\\)")

    fun cleanMarkdownText(markdownText: String): String {
        var text = markdownText
        text = HEADING.replace(text, "")
        text = EMPHASIS.replace(text, "")
        text = LIST_MARKER.replace(text, "")
        text = CODE.replace(text, "")
        text = LINK_PARTS.replace(text, "")
        return DartText.trim(text)
    }
}
