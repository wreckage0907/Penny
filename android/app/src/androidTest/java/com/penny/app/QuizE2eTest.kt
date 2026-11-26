package com.penny.app

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.penny.app.core.LessonKey
import com.penny.app.core.LearnRules
import com.penny.app.core.LearnState
import com.penny.app.core.QuizAnswer
import com.penny.app.core.QuizSession
import com.penny.app.ui.PennyApp
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * v2.1 Boss quiz (frames 11–12): locked answers with a saved session, the
 * 4/5 badge, coins settled once, results, review and retake.
 */
class QuizE2eTest {

    @get:Rule
    val compose = createComposeRule()

    private val unit = "1_personal_finance"

    /** A journey with all eight lessons done, so the boss is unlocked. */
    private fun unlockedState(saved: QuizSession? = null) = LearnState(
        coins = 240,
        streakDays = 5,
        completed = (0 until 8).map { LessonKey(unit, it) }.toSet(),
        savedSession = saved,
    )

    private fun boot(saved: QuizSession? = null): PennyServices {
        val services = PennyServices.fakes(signedIn = true)
        runBlocking { services.learn.set(unlockedState(saved)) }
        compose.setContent { PennyApp(services) }
        compose.onNodeWithTag("nav-learn").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("learn-boss") }
        compose.onNodeWithTag("boss-start").performScrollTo().performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("quiz-page") }
        return services
    }

    @Test
    fun aFullQuizRunScoresBadgesAndSettlesOnce() {
        val services = boot()
        compose.waitUntil(5_000) { compose.onAllNodesWithTextExists("What is the main goal of a budget?") }

        // Fake backend answers: B, C, D, A, B. Miss Q3 on purpose → 4/5.
        val picks = listOf(1, 2, 1, 0, 1)
        picks.forEachIndexed { index, option ->
            compose.onNodeWithTag("quiz-option-$option").performClick()
            compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("quiz-explanation") }
            if (index == 4) {
                compose.onNodeWithTag("quiz-finish").performClick()
            } else {
                compose.onNodeWithTag("quiz-next").performClick()
            }
        }
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("results-page") }

        compose.onNodeWithTag("results-score").assertTextEquals("4 / 5")
        compose.onNodeWithTag("results-reward").assertTextEquals("+60 ◈ · 4 × 15")
        assertTrue(compose.onAllNodesWithTextExists("Personal Finance Badge earned"))
        assertTrue(compose.onAllNodesWithTextExists("Reward settled once. New balance: 300 ◈.", substring = true))

        // Retakes are practice only — the second run awards nothing.
        compose.onNodeWithTag("results-retake").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTextExists("What is the main goal of a budget?") }
        listOf(1, 2, 3, 0, 1).forEachIndexed { index, option ->
            compose.onNodeWithTag("quiz-option-$option").performClick()
            if (index == 4) compose.onNodeWithTag("quiz-finish").performClick()
            else compose.onNodeWithTag("quiz-next").performClick()
        }
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("results-page") }
        compose.onNodeWithTag("results-reward").assertTextEquals("+0 ◈ · 5 × 15") // no duplicate coins
        compose.onNodeWithTag("results-score").assertTextEquals("5 / 5")
    }

    @Test
    fun aSavedSessionResumesWhereItStopped() {
        // Two answers already locked in (Q1 correct, Q2 correct) — resumes at Q3.
        boot(
            saved = QuizSession(
                unit, 5,
                listOf(QuizAnswer(0, 1, true), QuizAnswer(1, 2, true)),
            ),
        )
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("quiz-resumed") }
        assertTrue(compose.onAllNodesWithTextExists("Resumed saved session. A new quiz starts at Question 1.", substring = true))
        // The progress row shows two answered segments and we're on Question 3.
        assertTrue(compose.onAllNodesWithTextExists("QUESTION 3", substring = true)) // the eyebrow uppercases
    }

    @Test
    fun answerReviewShowsTheLockedRunWithoutCoins() {
        boot()
        listOf(1, 2, 3, 0, 1).forEachIndexed { index, option ->
            compose.onNodeWithTag("quiz-option-$option").performClick()
            if (index == 4) compose.onNodeWithTag("quiz-finish").performClick()
            else compose.onNodeWithTag("quiz-next").performClick()
        }
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("results-page") }
        compose.onNodeWithTag("results-review").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTextExists("Review", substring = true) }
        // Everything is locked in review: options already answered, no dock reward.
        assertTrue(compose.onAllNodesWithTextExists("+15 ◈ ✓"))
        compose.onNodeWithTag("quiz-done").performClick()
    }

    @Test
    fun theBossStaysLockedUntilEveryLessonIsComplete() {
        val services = PennyServices.fakes(signedIn = true)
        compose.setContent { PennyApp(services) }
        compose.onNodeWithTag("nav-learn").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("learn-boss") }
        assertTrue(!compose.onAllNodesWithTagExists("boss-start"))
        assertEquals(8, LearnRules.LESSONS_PER_UNIT)
    }
}
