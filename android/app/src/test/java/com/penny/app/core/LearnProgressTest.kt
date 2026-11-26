package com.penny.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * LearnRules (frames 09–12): idempotent lesson rewards, the boss gate,
 * locked answers with saved sessions, coins settled once per unit, the
 * badge at 4/5, and the streak.
 */
class LearnProgressTest {

    private val unit = "1_personal_finance"
    private val today = LocalDate.of(2026, 11, 26)

    private fun completed(learn: LearnState, count: Int) =
        (0 until count).fold(learn) { state, i -> LearnRules.completeLesson(state, unit, i, today) }

    @Test
    fun `a lesson completion pays once and touches the streak once a day`() {
        var state = LearnState(coins = 240)
        state = LearnRules.completeLesson(state, unit, 0, today)
        assertEquals(240 + LearnRules.LESSON_COINS, state.coins)
        assertEquals(setOf(LessonKey(unit, 0)), state.completed)
        assertEquals(1, state.streakDays)
        assertEquals(today, state.lastActiveDay)
        // Review never duplicates.
        state = LearnRules.completeLesson(state, unit, 0, today)
        assertEquals(240 + LearnRules.LESSON_COINS, state.coins)
        assertEquals(1, state.streakDays)
        // A second lesson the same day keeps the streak, pays again (new lesson).
        state = LearnRules.completeLesson(state, unit, 1, today)
        assertEquals(240 + 2 * LearnRules.LESSON_COINS, state.coins)
        assertEquals(1, state.streakDays)
    }

    @Test
    fun `the streak bridges yesterday and restarts after a gap`() {
        // Same day: no change.
        var state = LearnRules.onActivity(LearnState(streakDays = 5, lastActiveDay = today), today)
        assertEquals(5, state.streakDays)
        // Yesterday's activity continues the streak.
        state = LearnRules.onActivity(LearnState(streakDays = 5, lastActiveDay = today.minusDays(1)), today)
        assertEquals(6, state.streakDays)
        // A gap restarts at 1.
        state = LearnRules.onActivity(LearnState(streakDays = 5, lastActiveDay = today.minusDays(3)), today)
        assertEquals(1, state.streakDays)
        // First ever activity starts at 1.
        state = LearnRules.onActivity(LearnState(streakDays = 5, lastActiveDay = null), today)
        assertEquals(1, state.streakDays)
    }

    @Test
    fun `the boss quiz unlocks only after all eight lessons`() {
        var state = LearnState()
        assertFalse(LearnRules.quizUnlocked(state, unit))
        state = completed(state, 7)
        assertFalse(LearnRules.quizUnlocked(state, unit))
        state = completed(state, 8)
        assertTrue(LearnRules.quizUnlocked(state, unit))
        assertEquals("8 of 8 lessons complete", LearnRules.progressText(state, unit))
        assertEquals(100, LearnRules.progressShare(state, unit))
    }

    @Test
    fun `lesson statuses follow the path in order`() {
        var state = LearnState()
        assertEquals(LearnRules.LessonStatus.CURRENT, LearnRules.lessonStatus(state, unit, 0))
        assertEquals(LearnRules.LessonStatus.LOCKED, LearnRules.lessonStatus(state, unit, 1))
        state = completed(state, 2)
        assertEquals(LearnRules.LessonStatus.COMPLETED, LearnRules.lessonStatus(state, unit, 0))
        assertEquals(LearnRules.LessonStatus.COMPLETED, LearnRules.lessonStatus(state, unit, 1))
        assertEquals(LearnRules.LessonStatus.CURRENT, LearnRules.lessonStatus(state, unit, 2))
        assertEquals(LearnRules.LessonStatus.LOCKED, LearnRules.lessonStatus(state, unit, 3))
    }

    @Test
    fun `answers lock in order and a saved session resumes where it stopped`() {
        var session = LearnRules.startQuiz(LearnState(), unit, 5).savedSession!!
        assertEquals(0, session.currentIndex)
        session = LearnRules.answer(session, 0, 1, correct = true)
        assertEquals(1, session.currentIndex)
        // Second tap and out-of-order answers are ignored.
        assertEquals(session, LearnRules.answer(session, 0, 2, correct = false))
        assertEquals(session, LearnRules.answer(session, 2, 0, correct = true))
        session = LearnRules.answer(session, 1, 3, correct = false)
        assertEquals(2, session.currentIndex)
        assertEquals(true, session.answerOf(0)!!.correct)
        assertEquals(false, session.answerOf(1)!!.correct)
        // A saved partial session resumes; a new quiz resets to Question 1.
        val state = LearnState(savedSession = session)
        assertEquals(session, LearnRules.resumeQuiz(state, unit, 5))
        val fresh = LearnRules.startQuiz(state, unit, 5)
        assertEquals(0, fresh.savedSession!!.currentIndex)
        assertNull(fresh.savedSession!!.answerOf(0))
    }

    @Test
    fun `finishing pays once per unit, keeps the best score, and badging needs four`() {
        var state = completed(LearnState(coins = 240), 8)
        val answers = listOf(
            QuizAnswer(0, 0, true), QuizAnswer(1, 0, true), QuizAnswer(2, 0, true), QuizAnswer(3, 0, false), QuizAnswer(4, 0, true),
        )
        val session = QuizSession(unit, 5, answers)
        val outcome = LearnRules.finish(state, unit, session)
        assertEquals(4, outcome.correct)
        assertEquals(60, outcome.coinsAwarded) // 4 × 15, settled once
        assertTrue(outcome.badgeEarned)
        assertEquals(4, outcome.bestScore)
        // 240 seeded + 8 lessons × 25 + 4 × 15 = 500.
        assertEquals(500, outcome.newBalance)
        state = LearnRules.apply(state, unit, session, outcome, today)
        assertEquals(500, state.coins)
        assertTrue(unit in state.quizRewarded)
        assertTrue(unit in state.badges)
        assertNull(state.savedSession)

        // A retake is practice only: no coins, no second badge, best kept.
        val retake = LearnRules.finish(state, unit, session.copy(answers = answers.map { it.copy(correct = true) }))
        assertEquals(0, retake.coinsAwarded)
        assertFalse(retake.badgeEarned)
        assertEquals(5, retake.bestScore)
        assertEquals(500, retake.newBalance)

        // Three correct never badged a fresh unit.
        val newUnit = LearnRules.finish(LearnState(), "2_banking", QuizSession("2_banking", 5, answers.take(3)))
        assertFalse(newUnit.badgeEarned)
        assertEquals(45, newUnit.coinsAwarded)
    }

    @Test
    fun `the emergency-fund lesson quotes the user's own Needs spend`() {
        val target = LearnRules.emergencyTarget("Emergency funds", 23_800.0)
        assertTrue(target!!.contains("₹71,400"))
        assertTrue(target.contains("₹1,42,800"))
        assertTrue(target.contains("₹23,800"))
        assertNull(LearnRules.emergencyTarget("Saving goals", 23_800.0))
    }

    @Test
    fun `the fake course store serves all eight titles in one call`() = kotlinx.coroutines.runBlocking {
        val titles = FakeCourseContent().lessonTitles("1_personal_finance")
        assertEquals(8, titles.size)
        assertEquals("What is personal finance", titles.first())
        assertTrue(FakeCourseContent().lessonTitles("nope").isEmpty())
    }
}
