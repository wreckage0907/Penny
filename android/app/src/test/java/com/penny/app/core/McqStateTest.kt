package com.penny.app.core

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Characterisation tests for the MCQ page's pure state logic
 * (mcqpage.dart:99-114, :167, :216-220, :245-263). There is NO score
 * computation in the Flutter page: "grading" is per-option display state,
 * the bar is progress, and nextQuestion is a no-op on the last question.
 */
class McqStateTest {

    @Test
    fun `option letters are charCode 65 plus index, past Z included`() {
        assertEquals('A', McqState.optionLetter(0))
        assertEquals('D', McqState.optionLetter(3))
        assertEquals('Z', McqState.optionLetter(25))
        // Dart String.fromCharCode(65+26) — verified, no wraparound guard.
        assertEquals('[', McqState.optionLetter(26))
    }

    @Test
    fun `zero-length quiz no-ops next and labels divide by zero as strings`() {
        val s = McqState(index = 0, selectedAnswer = "A", isChecked = true)
        assertEquals(s, s.next(0))
        assertEquals("1/0", McqState.progressLabel(0, 0))
        assertEquals("Question 1/0", McqState.questionLabel(0, 0))
    }

    @Test
    fun `progress is (index + 1) divided by length as double`() {
        assertEquals(0.1, McqState.progress(0, 10), 0.0)
        assertEquals(1.0, McqState.progress(9, 10), 0.0)
        assertEquals(0.3333333333333333, McqState.progress(0, 3), 0.0)
        assertEquals(0.6666666666666666, McqState.progress(1, 3), 0.0)
        assertEquals(0.14285714285714285, McqState.progress(0, 7), 0.0)
        // Dart double division by zero is Infinity, not an exception
        // (unreachable in the UI, which throws at elementAt(0) first).
        assertEquals(Double.POSITIVE_INFINITY, McqState.progress(0, 0), 0.0)
    }

    @Test
    fun `counter labels are plain interpolation`() {
        assertEquals("1/10", McqState.progressLabel(0, 10))
        assertEquals("10/10", McqState.progressLabel(9, 10))
        assertEquals("Question 1/10", McqState.questionLabel(0, 10))
        assertEquals("Question 10/10", McqState.questionLabel(9, 10))
    }

    @Test
    fun `chapter slug table mirrors Dart and backend, out of range is null`() {
        assertEquals("introduction_to_personal_finance", McqChapters.slug(0))
        assertEquals("setting_financial_goals", McqChapters.slug(1))
        assertEquals("budgeting_and_expense_tracking", McqChapters.slug(2))
        assertEquals("online_and_mobile_banking", McqChapters.slug(3))
        assertEquals(null, McqChapters.slug(4))
        assertEquals(null, McqChapters.slug(-1))
    }

    @Test
    fun `selecting locks the answer and marks checked`() {
        val s0 = McqState()
        val s1 = s0.select("B")
        assertEquals("B", s1.selectedAnswer)
        assertEquals(true, s1.isChecked)
        // UI disables further taps once checked (mcqpage.dart:268); the pure
        // state mirrors that: select on a checked state is a no-op.
        assertEquals(s1, s1.select("C"))
    }

    @Test
    fun `next advances and resets selection, but is a no-op on the last question`() {
        val s = McqState(index = 0).select("A")
        val s1 = s.next(10)
        assertEquals(1, s1.index)
        assertEquals(null, s1.selectedAnswer)
        assertEquals(false, s1.isChecked)
        val last = McqState(index = 9).select("D")
        assertEquals(last, last.next(10))
    }

    @Test
    fun `selected-and-correct reveals green only, stale selections stay neutral`() {
        // Correct branch precedes selected-wrong (:254 before :258).
        assertEquals(McqOptionState.CORRECT, McqState.optionState("C", "C", "C", true))
        assertEquals(McqOptionState.NEUTRAL, McqState.optionState("Z", "A", "C", true))
        assertEquals(McqOptionState.WRONG_SELECTED, McqState.optionState("D", "D", "C", true))
    }

    @Test
    fun `ten-step walk and single-question dead end match Dart state goldens`() {
        var s = McqState()
        repeat(10) {
            s = s.select("A")
            if (it < 9) s = s.next(10)
        }
        assertEquals(9, s.index)
        assertEquals("A", s.selectedAnswer)
        assertEquals(true, s.isChecked)
        assertEquals(1.0, McqState.progress(s.index, 10), 0.0)
        assertEquals(s, s.next(10))

        var single = McqState().select("B")
        repeat(3) { single = single.next(1) }
        assertEquals(0, single.index)
        assertEquals("B", single.selectedAnswer)
        assertEquals(true, single.isChecked)

        val skipped = McqState().next(3)
        assertEquals(1, skipped.index)
        assertEquals(null, skipped.selectedAnswer)
        assertEquals(false, skipped.isChecked)
        assertEquals(0.6666666666666666, McqState.progress(skipped.index, 3), 0.0)
    }

    @Test
    fun `option display state matches the checked colouring table`() {
        // mcqpage.dart:253-263 — colours only once checked; correct always
        // green, selected-wrong red, everything else neutral.
        assertEquals(McqOptionState.NEUTRAL, McqState.optionState("A", "A", "B", false))
        assertEquals(McqOptionState.NEUTRAL, McqState.optionState("B", "A", "B", false))
        assertEquals(McqOptionState.CORRECT, McqState.optionState("A", "A", "A", true))
        assertEquals(McqOptionState.CORRECT, McqState.optionState(null, "B", "B", true))
        assertEquals(McqOptionState.WRONG_SELECTED, McqState.optionState("A", "A", "B", true))
        assertEquals(McqOptionState.NEUTRAL, McqState.optionState("C", "A", "B", true))
        assertEquals(McqOptionState.NEUTRAL, McqState.optionState(null, "A", "B", true))
    }

    @Test
    fun `answer comparison is exact string equality on letters, case sensitive`() {
        // mcqpage.dart:248 — optionLetter == currentQuestion.answer, no trim.
        assertEquals(McqOptionState.CORRECT, McqState.optionState(null, "A", "A", true))
        assertEquals(McqOptionState.NEUTRAL, McqState.optionState(null, "A", "a", true))
        assertEquals(McqOptionState.NEUTRAL, McqState.optionState(null, "A", "A ", true))
    }
}