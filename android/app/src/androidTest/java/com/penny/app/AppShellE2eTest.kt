package com.penny.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.penny.app.ui.PennyApp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** v2.1 shell: demo login reaches Home, and the four-tab round trip works. */
class AppShellE2eTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun signedOutLandsOnLoginThenDemoLoginReachesHomeAndRoundsTheTabs() {
        val services = PennyServices.fakes()
        compose.setContent { PennyApp(services) }

        compose.onNodeWithTag("login-page").assertIsDisplayed()
        compose.onNodeWithTag("login-id-input").performScrollTo().performTextInput("demo")
        compose.onNodeWithTag("login-password-input").performScrollTo().performTextInput("penny123")
        compose.onNodeWithTag("login-submit").performScrollTo().performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("home-page") }

        // The labelled bottom navigation (frame 04) reaches every tab.
        compose.onNodeWithTag("nav-budget").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("budget-page") }
        compose.onNodeWithTag("nav-learn").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("learn-page") }
        compose.onNodeWithTag("nav-home").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTextExists(", Demo", substring = true) }
        compose.onNodeWithTag("nav-profile").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("profile-page") }

        // Sign out from the profile tab (frame 14's ghost button).
        compose.onNodeWithTag("profile-sign-out").performScrollTo().performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("login-page") }
        assertEquals(
            com.penny.app.core.AuthState.SignedOut,
            services.auth.state.value,
        )
    }

    @Test
    fun seededSignedInUserSkipsLogin() {
        compose.setContent { PennyApp(PennyServices.fakes(signedIn = true)) }
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("home-page") }
    }
}
