package com.penny.app.core

import java.time.LocalDate

/**
 * v2.1 learning journey state and rules (artboard frames 09–12): a gated
 * path of 8 lessons per unit, idempotent lesson rewards, a boss quiz that
 * unlocks only after every lesson, per-question locking with a saved
 * (resumable) session, coins settled once per unit, badges, and a streak.
 *
 * New behaviour — the Flutter app had no progress, coins, scoring, or
 * gating (LEDGER 003: "Grading = reveal-only"). The maintainer approved
 * implementing it fully for the redesign.
 */
data class LessonKey(val unit: String, val index: Int)

data class QuizAnswer(val questionIndex: Int, val optionIndex: Int, val correct: Boolean)

data class QuizSession(
    val unit: String,
    val total: Int,
    val answers: List<QuizAnswer> = emptyList(),
) {
    /** 0-based index of the first unanswered question. */
    val currentIndex: Int get() = answers.size

    fun answerOf(questionIndex: Int): QuizAnswer? = answers.firstOrNull { it.questionIndex == questionIndex }
}

data class LearnState(
    val coins: Int = 0,
    val streakDays: Int = 0,
    val completed: Set<LessonKey> = emptySet(),
    /** unit → best number of correct answers. */
    val quizBest: Map<String, Int> = emptyMap(),
    /** Units whose quiz reward has been settled (retakes never re-award). */
    val quizRewarded: Set<String> = emptySet(),
    val savedSession: QuizSession? = null,
    val badges: Set<String> = emptySet(),
    val lastActiveDay: LocalDate? = null,
    /** The most recent finished run, for the Results screen and answer review. */
    val lastOutcome: QuizOutcome? = null,
    val lastSession: QuizSession? = null,
)

/** What finishing a quiz returns, for the Results screen (frame 12). */
data class QuizOutcome(
    val correct: Int,
    val total: Int,
    val coinsAwarded: Int,
    val badgeEarned: Boolean,
    val bestScore: Int,
    val newBalance: Int,
)

object LearnRules {
    const val LESSONS_PER_UNIT = 8
    const val QUIZ_QUESTIONS = 5
    const val LESSON_COINS = 25
    const val QUESTION_COINS = 15
    /** "4/5 correct earns the badge" (frame 09 boss card). */
    const val BADGE_THRESHOLD = 4

    /**
     * Mark a lesson complete: +[LESSON_COINS] and a streak touch on FIRST
     * completion only ("Review never duplicates rewards", frame 10).
     */
    fun completeLesson(state: LearnState, unit: String, index: Int, today: LocalDate): LearnState {
        val key = LessonKey(unit, index)
        if (key in state.completed) return state
        return onActivity(
            state.copy(
                completed = state.completed + key,
                coins = state.coins + LESSON_COINS,
                // A completed unit-1 lesson while a saved unit-1 session
                // exists would make the gate retro-inconsistent; the boss
                // gate is re-evaluated live, so just drop the stale session.
                savedSession = state.savedSession?.takeIf { it.unit != unit },
            ),
            today,
        )
    }

    fun completedCount(state: LearnState, unit: String): Int =
        state.completed.count { it.unit == unit }

    /** "The Boss Quiz unlocks after all 8 lessons" (frame 09). */
    fun quizUnlocked(state: LearnState, unit: String): Boolean =
        completedCount(state, unit) == LESSONS_PER_UNIT

    /** Start a NEW quiz at Question 1 — never reuse the saved session. */
    fun startQuiz(state: LearnState, unit: String, total: Int): LearnState =
        state.copy(savedSession = state.savedSession?.takeIf { it.unit != unit } ?: QuizSession(unit, total))

    /**
     * "Resumed saved session" (frame 11): the saved one if it is for this
     * unit, otherwise a fresh session at Question 1.
     */
    fun resumeQuiz(state: LearnState, unit: String, total: Int): QuizSession =
        state.savedSession?.takeIf { it.unit == unit } ?: QuizSession(unit, total)

    /** Answer: locked in ("Answer locked", frame 11); second taps ignored. */
    fun answer(session: QuizSession, questionIndex: Int, optionIndex: Int, correct: Boolean): QuizSession {
        if (session.answerOf(questionIndex) != null || questionIndex != session.currentIndex) return session
        return session.copy(answers = session.answers + QuizAnswer(questionIndex, optionIndex, correct))
    }

    /** Finish: coins settled once per unit, best score kept, badge at 4+. */
    fun finish(state: LearnState, unit: String, session: QuizSession): QuizOutcome {
        val correct = session.answers.count { it.correct }
        val rewardable = unit !in state.quizRewarded
        val coins = if (rewardable) correct * QUESTION_COINS else 0
        val best = maxOf(state.quizBest[unit] ?: 0, correct)
        return QuizOutcome(
            correct = correct,
            total = session.total,
            coinsAwarded = coins,
            badgeEarned = correct >= BADGE_THRESHOLD && unit !in state.badges,
            bestScore = best,
            newBalance = state.coins + coins,
        )
    }

    /** The persisted state after [finish] (the session is consumed). */
    fun apply(
        state: LearnState,
        unit: String,
        session: QuizSession,
        outcome: QuizOutcome,
        today: LocalDate,
    ): LearnState =
        onActivity(
            state.copy(
                coins = state.coins + outcome.coinsAwarded,
                quizBest = state.quizBest + (unit to outcome.bestScore),
                quizRewarded = state.quizRewarded + unit,
                badges = if (outcome.badgeEarned) state.badges + unit else state.badges,
                savedSession = null,
            ),
            today,
        )

    /** Streak: same day = no change, yesterday = +1, a gap = restart at 1. */
    fun onActivity(state: LearnState, today: LocalDate): LearnState = when (state.lastActiveDay) {
        null -> state.copy(streakDays = 1, lastActiveDay = today)
        today -> state
        today.minusDays(1) -> state.copy(streakDays = state.streakDays + 1, lastActiveDay = today)
        else -> state.copy(streakDays = 1, lastActiveDay = today)
    }

    /** "2 of 8 lessons complete · 25%" (frame 09 progress strip). */
    fun progressText(state: LearnState, unit: String): String =
        "${completedCount(state, unit)} of $LESSONS_PER_UNIT lessons complete"

    fun progressShare(state: LearnState, unit: String): Int =
        completedCount(state, unit) * 100 / LESSONS_PER_UNIT

    /** Lesson display state for one stop on the path (frame 09). */
    enum class LessonStatus { COMPLETED, CURRENT, LOCKED }
    fun lessonStatus(state: LearnState, unit: String, index: Int): LessonStatus {
        if (LessonKey(unit, index) in state.completed) return LessonStatus.COMPLETED
        val next = state.completed.count { it.unit == unit } // lessons complete in order
        return if (index == next) LessonStatus.CURRENT else LessonStatus.LOCKED
    }

    /**
     * Frame 10's gold notice, grounded in the user's own budget: an
     * emergency-fund lesson prints 3–6 months of the month's Needs spend.
     * Null for every other lesson.
     */
    fun emergencyTarget(lessonTitle: String, needsActual: Double): String? {
        if (!lessonTitle.lowercase().contains("emergency")) return null
        return "Your target: 3–6 months of core Needs = ${Inr.plain(needsActual * 3)}–${Inr.plain(needsActual * 6)}, " +
            "based on monthly Needs of ${Inr.plain(needsActual)}."
    }
}

/**
 * The chapter slug table (mcqpage.dart:58-63), kept from the v1 practice
 * flow: the boss quiz of learning unit i draws its questions from chapter
 * i. Out-of-range units have no quiz (null).
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

/** v2.1 quiz display helpers. */
object QuizText {
    /** The fake backend prefixes options with "A) " — the letter chip shows it instead. */
    fun optionText(raw: String, index: Int): String {
        val prefix = "${'A' + index}) "
        return if (raw.startsWith(prefix)) raw.removePrefix(prefix) else raw
    }

    /** Correctness: the wire answer is the option letter. */
    fun isCorrect(question: McqQuestion, optionIndex: Int): Boolean =
        question.answer == "${'A' + optionIndex}"
}
