package com.penny.app.core

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** The fake questions backend mimics questionGen.py and shares McqService's message path. */
class FakeMcqBackendTest {

    private val api = FakeMcqBackend()

    @Test
    fun `every known chapter returns ten questions in wire order, num_question ignored`() = runBlocking {
        McqChapters.let { (0..3).map { i -> it.slug(i)!! } }.forEach { slug ->
            val chapter = api.fetchQuestions(slug, numQuestions = 5)
            assertEquals((1..10).map { "question_$it" }, chapter.questions.keys.toList())
        }
    }

    @Test
    fun `options arrive pre-prefixed and answers are letters`() = runBlocking {
        val q = api.fetchQuestions("introduction_to_personal_finance").questions.getValue("question_1")
        assertEquals("What is the main goal of a budget?", q.question)
        assertEquals(listOf("A) To spend more each month", "B) To plan how money is earned and spent",
            "C) To avoid paying taxes", "D) To borrow money easily"), q.options)
        assertEquals("B", q.answer)
        assertTrue(q.hint.isNotEmpty())
    }

    @Test
    fun `an unknown chapter is the backend 400, rendered exactly like the HTTP client`() {
        val e = assertThrows(McqApiException::class.java) { runBlocking { api.fetchQuestions("nope") } }
        assertEquals("Failed to load questions: Exception: Failed to load questions: 400", e.message)
        assertEquals(
            "Error: Exception: Failed to load questions: Exception: Failed to load questions: 400",
            PracticeText.errorText(e),
        )
    }

    @Test
    fun `practice constants and Dart RangeError texts (executed Dart 3_14)`() {
        assertEquals(listOf("Introduction to Personal Finance"), PracticeText.FINISHED)
        assertEquals(listOf("Insurance and Risk Management", "Real Estate and Home Management"), PracticeText.UNFINISHED)
        assertEquals("Chapter 1", PracticeText.APP_BAR_TITLE)
        assertEquals("RangeError (index): Index out of range: no indices are valid: 0", PracticeText.elementAtError(0, 0))
        assertEquals("RangeError (index): Index out of range: index should be less than 3: 3", PracticeText.elementAtError(3, 3))
    }
}
