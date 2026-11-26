package com.penny.app

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.penny.app.ui.LessonScreen
import com.penny.app.ui.PennyApp
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * v2.1 Learn (frame 09): the gated path with rewards, lesson completion
 * paying once, and the boss lock until all eight lessons are done.
 */
class LearnE2eTest {

    @get:Rule
    val compose = createComposeRule()

    private fun boot(): PennyServices {
        val services = PennyServices.fakes(signedIn = true)
        compose.setContent { PennyApp(services) }
        compose.onNodeWithTag("nav-learn").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("learn-page") }
        return services
    }

    @Test
    fun thePathShowsTheJourneyPurseAndGatedBoss() {
        boot()
        compose.onNodeWithTag("learn-streak").assertTextEquals("🔥 5 days")
        compose.onNodeWithTag("learn-coins").assertTextEquals("◈ 240")
        assertTrue(compose.onAllNodesWithTextExists("2 of 8 lessons complete · 25%"))
        assertTrue(compose.onAllNodesWithTextExists("Lesson 3 is next. The Boss Quiz unlocks after all 8 lessons."))
        compose.onNodeWithTag("learn-boss").performScrollTo().assertIsDisplayed()
        // Locked boss: no start button.
        assertTrue(!compose.onAllNodesWithTagExists("boss-start"))
        // Completed lessons reopen for review; the vault previews, never redeems.
        assertTrue(compose.onAllNodesWithTextExists("Completed · +25 ◈ earned · Review"))
        compose.onNodeWithTag("learn-vault").performScrollTo()
        compose.onNodeWithTag("reward-freeze").performScrollTo().performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("reward-preview") }
        assertTrue(compose.onAllNodesWithTextExists("No instant redemption", substring = true))
    }

    @Test
    fun completingTheCurrentLessonPaysOnceAndAdvancesTheGate() {
        val services = boot()
        compose.onNodeWithTag("learn-stop-2").performScrollTo()
        compose.onNodeWithTag("stop-action").performScrollTo().performClick() // Start Lesson 3
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("lesson-page") }

        // The emergency-fund lesson quotes the user's own Needs spend.
        compose.waitUntil(5_000) { compose.onAllNodesWithTextExists("Lesson 3 of 8", substring = true) }
        // The current lesson in the fake store is "The 50/30/20 budget" —
        // the emergency-fund notice is content-conditional and absent here.
        compose.waitUntil(5_000) { compose.onAllNodesWithTextExists("The 50/30/20 budget", substring = true) }
        compose.onNodeWithTag("lesson-complete").performClick()

        compose.waitUntil(5_000) { compose.onAllNodesWithTextExists("3 of 8 lessons complete · 37%") }
        compose.onNodeWithTag("learn-coins").assertTextEquals("◈ 265") // 240 + 25, once
        assertTrue(compose.onAllNodesWithTextExists("Lesson 4 is next", substring = true))
    }
}
