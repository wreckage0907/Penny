package com.penny.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.penny.app.ui.PennyApp
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * v2.1 Home (frame 04 + S03): the month hero with reconciled money, top
 * spending, the learning shortcut, and the first-run empty state.
 */
class HomeE2eTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun demoHomeShowsTheHeroTopSpendingAndLearningShortcut() {
        compose.setContent { PennyApp(PennyServices.fakes(signedIn = true)) }
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("home-page") }
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("home-hero") }

        // "Good <daypart>, Demo" — first name only, whatever the clock says.
        assertTrue(compose.onAllNodesWithTextExists(", Demo", substring = true))
        compose.onNodeWithTag("home-hero-amount").assertTextEquals("+₹53,550")
        assertTrue(compose.onAllNodesWithTextExists("₹17,000 SIP recorded · ₹36,550 remains unallocated"))

        compose.onNodeWithTag("home-spend-0-name").assertTextEquals("Food & Dining")
        assertTrue(compose.onAllNodesWithTextExists("36% of spend · 6 items"))
        compose.onNodeWithTag("home-avatar-default").assertIsDisplayed()

        // See all → Budget (frame 04's link).
        compose.onNodeWithTag("home-see-all").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("budget-page") }

        // The dock's repeated Add opens Quick Log in thumb reach.
        compose.onNodeWithTag("nav-home").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("home-page") }
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("dock-0") }
    }

    @Test
    fun continueLearningOpensTheNextLesson() {
        compose.setContent { PennyApp(PennyServices.fakes(signedIn = true)) }
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("home-page") }
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("home-learn") }
        assertTrue(compose.onAllNodesWithTextExists("Lesson 3 of 8 · 2 complete", substring = true))
        compose.onNodeWithTag("home-learn-start").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("lesson-page") }
        assertTrue(compose.onAllNodesWithTextExists("Lesson 3 of 8", substring = true))
    }

    @Test
    fun aFreshAccountSeesTheFirstRunStateNotFakeNumbers() {
        val services = PennyServices.fakes()
        compose.setContent { PennyApp(services) }

        // Sign up a brand-new user with an empty month.
        compose.onNodeWithTag("login-signup-link").performClick()
        compose.onNodeWithTag("signup-name-input").performTextInput("Empty User")
        compose.onNodeWithTag("signup-username-input").performTextInput("empty")
        compose.onNodeWithTag("signup-email-input").performTextInput("empty@penny.app")
        compose.onNodeWithTag("signup-password-input").performTextInput("secret1")
        compose.onNodeWithTag("signup-submit").performScrollTo().performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("home-empty") }

        compose.onNodeWithTag("home-empty").assertIsDisplayed()
        assertTrue(compose.onAllNodesWithTextExists("No activity yet"))
        assertTrue(compose.onAllNodesWithTextExists("Empty is not an error", substring = true))
        compose.onNodeWithTag("home-empty-lesson").assertIsDisplayed()
        compose.onNodeWithTag("home-empty-add").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("budget-page") }
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("budget-empty") }
    }
}
