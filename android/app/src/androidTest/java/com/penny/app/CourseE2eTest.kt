package com.penny.app

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.penny.app.core.FakeCourseContent
import com.penny.app.core.Speech
import com.penny.app.ui.PennyApp
import org.junit.Rule
import org.junit.Test

/** 010: course list → learning path → lesson + text-to-speech, on the fake store. */
class CourseE2eTest {

    @get:Rule
    val compose = createComposeRule()

    private class RecordingSpeech : Speech {
        val calls = mutableListOf<String>()
        override suspend fun speak(text: String, language: String, pitch: Float) { calls += "speak[$language,$pitch]:$text" }
        override suspend fun stop() { calls += "stop" }
        override fun setOnComplete(onComplete: () -> Unit) = Unit
    }

    private fun open(content: FakeCourseContent = FakeCourseContent(), speech: RecordingSpeech = RecordingSpeech()): RecordingSpeech {
        val base = PennyServices.fakes(signedIn = true)
        val services = PennyServices(base.auth, base.users, base.prefs, base.profileImages, base.expenses,
            courses = content, speech = { speech })
        compose.setContent { PennyApp(services) }
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("home-page") }
        compose.onNodeWithTag("home-tile-0").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("module-0") }
        return speech
    }

    private fun name(i: Int) = compose.onNodeWithTag("module-name-$i", useUnmergedTree = true)

    @Test
    fun modulesAreFilteredSortedAndNamed() {
        open()
        name(0).assertTextEquals("Personal Finance")
        name(1).assertTextEquals("Banking And Accounts")
        name(2).assertTextEquals("Credit And Debt Management")
        check(!compose.onAllNodesWithTagExists("module-3")) // the "" folder is dropped
    }

    @Test
    fun pathCircleOpensTheLessonAndSpeechReadsTheCleanedText() {
        val speech = open()
        compose.onNodeWithTag("module-0").performClick()
        compose.onNodeWithTag("learning-title").assertTextEquals("Personal Finance")
        // Lesson 1's circle is at (w/2, 4h/5) of the 1900-dp path, i.e. 380 dp above
        // its bottom edge. Touch coordinates are relative to the node's VISIBLE
        // bounds, whose bottom is the path's bottom (reverse scroll starts there).
        compose.onNodeWithTag("learning-path").performTouchInput { click(Offset(width / 2f, height - 380.dp.toPx())) }
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("lesson-content") }
        compose.onNodeWithTag("lesson-title", useUnmergedTree = true).assertTextEquals("Lesson 1")
        compose.waitUntil(5_000) { compose.onAllNodesWithTextExists("Lesson 1: What is personal finance") }

        compose.onNodeWithTag("lesson-speak").performClick()
        compose.waitUntil(5_000) { speech.calls.isNotEmpty() }
        check(speech.calls[0].startsWith("speak[en-US,0.8]:Lesson 1: What is personal finance")) { speech.calls[0] }
        check(!speech.calls[0].contains("**"))
        // Toggling re-fetches (FutureBuilder in build); the second tap stops.
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("lesson-content") }
        compose.onNodeWithTag("lesson-speak").performClick()
        compose.waitUntil(5_000) { speech.calls.lastOrNull() == "stop" }
    }

    @Test
    fun moduleWithoutABackgroundImageErrorsLikeFlutter() {
        open(FakeCourseContent(FakeCourseContent.SAMPLE + ("4_savings_tips" to listOf("A"))))
        compose.onNodeWithTag("module-3").performClick()
        compose.onNodeWithTag("learning-title").assertTextEquals("Savings Tips")
        compose.onNodeWithTag("learning-error").assertIsDisplayed()
    }
}
