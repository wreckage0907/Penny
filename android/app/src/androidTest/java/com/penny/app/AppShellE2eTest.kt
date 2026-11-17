package com.penny.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.penny.app.ui.PennyApp
import org.junit.Rule
import org.junit.Test

/** 006: AuthWrapper routing + named routes on fake services. */
class AppShellE2eTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun signedOutLandsOnLoginThenDemoLoginReachesHomeAndRoutes() {
        val services = PennyServices.fakes()
        compose.setContent { PennyApp(services) }

        // 007 replaced the temporary shell login with the real LoginPage.
        compose.onNodeWithTag("login-page").assertIsDisplayed()
        compose.onNodeWithTag("login-username").performScrollTo().performTextInput("demo")
        compose.onNodeWithTag("login-password").performScrollTo().performTextInput("penny123")
        compose.onNodeWithTag("login-submit").performScrollTo().performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("home-page") }

        // A still-unmigrated route renders the placeholder and comes back.
        compose.onNodeWithTag("home-nav-chat").performClick()
        compose.onNodeWithTag("not-migrated-route").assertTextEquals("/chatbot — not migrated yet")
        compose.onNodeWithTag("not-migrated-back").performClick()
        compose.onNodeWithTag("home-page").assertIsDisplayed()

        compose.onNodeWithTag("home-nav-profile").performClick()
        compose.onNodeWithTag("profile-sign-out").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("login-page") }
    }

    @Test
    fun seededSignedInUserSkipsLogin() {
        compose.setContent { PennyApp(PennyServices.fakes(signedIn = true)) }
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("home-page") }
    }
}
