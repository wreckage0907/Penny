package com.penny.app.core

/**
 * Pure state logic of the MCQ page (mcqpage.dart:99-114, :167, :216-220,
 * :245-263). There is NO scoring in Flutter: selection is one-shot, the bar
 * is progress, nextQuestion no-ops on the last question (no results screen).
 */
data class McqState(
    val index: Int = 0,
    val selectedAnswer: String? = null,
    val isChecked: Boolean = false,
) {
    /** selectAnswer (:99-104); taps are disabled once checked (:268). */
    fun select(letter: String): McqState =
        if (isChecked) this else copy(selectedAnswer = letter, isChecked = true)

    /** nextQuestion (:106-114): advance + reset, or no-op on the last. */
    fun next(length: Int): McqState =
        if (index < length - 1) copy(index = index + 1, selectedAnswer = null, isChecked = false)
        else this

    companion object {
        /** String.fromCharCode(65 + i) (:219) — no wraparound guard. */
        fun optionLetter(i: Int): Char = (65 + i).toChar()

        /** LinearPercentIndicator percent (:167). */
        fun progress(index: Int, length: Int): Double = (index + 1).toDouble() / length

        /** Counter strings (:170, :191) — plain interpolation, no padding. */
        fun progressLabel(index: Int, length: Int): String = "${index + 1}/$length"

        fun questionLabel(index: Int, length: Int): String = "Question ${index + 1}/$length"

        /** buildOptionButton colouring table (:253-263). */
        fun optionState(
            selected: String?,
            letter: String,
            answer: String,
            checked: Boolean,
        ): McqOptionState = when {
            !checked -> McqOptionState.NEUTRAL
            letter == answer -> McqOptionState.CORRECT
            selected == letter -> McqOptionState.WRONG_SELECTED
            else -> McqOptionState.NEUTRAL
        }
    }
}

enum class McqOptionState { NEUTRAL, CORRECT, WRONG_SELECTED }

/**
 * The chapter slug table (mcqpage.dart:58-63). Untyped lookup: out-of-range
 * index yields null (Dart then dies in initState assigning null to a String
 * field — the UI slice reproduces that failure, core just returns null).
 * Order mirrors backend/questionGen.py:26-31 exactly.
 */
object McqChapters {
    private val slugs = mapOf(
        0 to "introduction_to_personal_finance",
        1 to "setting_financial_goals",
        2 to "budgeting_and_expense_tracking",
        3 to "online_and_mobile_banking",
    )

    fun slug(index: Int): String? = slugs[index]
}