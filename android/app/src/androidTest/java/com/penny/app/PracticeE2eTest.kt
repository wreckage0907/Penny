package com.penny.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.penny.app.core.FakeMcqBackend
import com.penny.app.ui.PennyApp
import org.junit.Rule
import org.junit.Test

/** 011: practice list → MCQ page (mcqpage.dart) on the fake questions backend. */
class PracticeE2eTest {

    @get:Rule
    val compose = createComposeRule()

    private fun open(mcq: FakeMcqBackend = FakeMcqBackend()) {
        val base = PennyServices.fakes(signedIn = true)
        val services = PennyServices(base.auth, base.users, base.prefs, base.profileImages, base.expenses,
            base.courses, base.speech, mcq = mcq)
        compose.setContent { PennyApp(services) }
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("home-page") }
        compose.onNodeWithTag("home-tile-3").performScrollTo().performClick()
        compose.onNodeWithTag("practice-page").assertIsDisplayed()
    }

    private fun label() = compose.onNodeWithTag("mcq-progress-label", useUnmergedTree = true)

    @Test
    fun unfinishedModulesDoNothingAndTheFinishedOneOpensChapterOne() {
        open()
        compose.onNodeWithTag("practice-unfinished-0").performClick()
        compose.onNodeWithTag("practice-page").assertIsDisplayed()
        compose.onNodeWithTag("practice-finished-0").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("mcq-page") }
        compose.onNodeWithTag("mcq-title", useUnmergedTree = true).assertTextEquals("Chapter 1")
        label().assertTextEquals("1/10")
        compose.onNodeWithTag("mcq-question", useUnmergedTree = true).assertTextEquals("What is the main goal of a budget?")
    }

    @Test
    fun wrongAnswerRevealsBothAndLocksTheQuestion() {
        open()
        compose.onNodeWithTag("practice-finished-0").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("mcq-page") }
        compose.onNodeWithTag("mcq-option-0").performClick() // A, answer is B
        compose.onNodeWithTag("mcq-wrong-0", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("mcq-correct-1", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("mcq-option-2").performClick() // one-shot: ignored
        check(!compose.onAllNodesWithTagExists("mcq-wrong-2"))
    }

    @Test
    fun nextAdvancesEvenUnansweredAndNoOpsOnTheLastQuestion() {
        open()
        compose.onNodeWithTag("practice-finished-0").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("mcq-page") }
        repeat(9) { compose.onNodeWithTag("mcq-next").performClick() }
        label().assertTextEquals("10/10")
        compose.onNodeWithTag("mcq-next").performClick()
        label().assertTextEquals("10/10") // no results screen in Flutter
    }

    @Test
    fun hintShowsTheQuestionsHint() {
        open()
        compose.onNodeWithTag("practice-finished-0").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("mcq-page") }
        compose.onNodeWithTag("mcq-hint").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTextExists("Think about planning ahead.") }
    }

    @Test
    fun fetchFailureShowsTheDoubleWrappedDartError() {
        open(FakeMcqBackend(chapters = emptyMap()))
        compose.onNodeWithTag("practice-finished-0").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("mcq-error") }
        compose.onNodeWithTag("mcq-error-text").assertTextEquals(
            "Error: Exception: Failed to load questions: Exception: Failed to load questions: 400",
        )
    }

    @Test
    fun emptyChapterIsTheDartRangeError() {
        open(FakeMcqBackend(mapOf("introduction_to_personal_finance" to ("Intro" to emptyList()))))
        compose.onNodeWithTag("practice-finished-0").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("mcq-error") }
        compose.onNodeWithTag("mcq-error-text").assertTextEquals(
            "RangeError (index): Index out of range: no indices are valid: 0",
        )
    }
}
