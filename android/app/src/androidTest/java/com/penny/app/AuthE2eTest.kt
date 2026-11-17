package com.penny.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.penny.app.ui.PennyApp
import org.junit.Rule
import org.junit.Test

/** 007: login / sign-up / onboarding / phone flows on the fake services. */
class AuthE2eTest {

    @get:Rule
    val compose = createComposeRule()

    private fun start(): PennyServices = PennyServices.fakes().also { s -> compose.setContent { PennyApp(s) } }

    private fun type(tag: String, text: String) = compose.onNodeWithTag(tag).performScrollTo().performTextInput(text)
    private fun tap(tag: String) = compose.onNodeWithTag(tag).performScrollTo().performClick()

    // Dialog content is not inside a scroll container.
    private fun dialogType(tag: String, text: String) = compose.onNodeWithTag(tag).performTextInput(text)
    private fun dialogTap(tag: String) = compose.onNodeWithTag(tag).performClick()

    @Test
    fun usernameLoginReachesHome() {
        start()
        compose.onNodeWithTag("login-page-title").assertIsDisplayed()
        type("login-username", "demo")
        type("login-password", "penny123")
        tap("login-submit")
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("home-page") }
    }

    @Test
    fun wrongPasswordIsSilent() {
        val services = start()
        type("login-username", "demo")
        type("login-password", "nope")
        tap("login-submit")
        compose.waitForIdle()
        // Silent failure = no navigation, no session, no prefs (login.dart:185-203).
        // (Asserting on-screen visibility raced the IME animation intermittently.)
        check(!compose.onAllNodesWithTagExists("home-page"))
        compose.onNodeWithTag("login-submit").assertExists()
        check(services.auth.state.value == com.penny.app.core.AuthState.SignedOut)
        check(kotlinx.coroutines.runBlocking { services.prefs.username() } == null)
    }

    @Test
    fun signUpOnboardThenLogBackInWithTheNewUsername() {
        val services = start()
        tap("login-signup-link")
        type("signup-email", "new@penny.app")
        type("signup-password", "secret1")
        tap("signup-submit")
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("onboarding-page") }
        type("onb-username", "newbie")
        type("onb-first", "New")
        type("onb-last", "Bie")
        type("onb-email", "new@penny.app")
        type("onb-phone", "+1 555 0199")
        tap("onb-submit")
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("home-page") }

        compose.onNodeWithTag("home-nav-profile").performClick()
        compose.onNodeWithTag("profile-sign-out").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("login-page") }
        type("login-username", "newbie")
        type("login-password", "secret1")
        tap("login-submit")
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("home-page") }
        check(kotlinx.coroutines.runBlocking { services.prefs.fullName() } == "New Bie")
        compose.onNodeWithTag("home-greeting").assertTextEquals("WELCOME,\nNew Bie")
    }

    @Test
    fun onboardingWithoutUsernameShowsDartError() {
        start()
        tap("login-signup-link")
        type("signup-email", "x@penny.app")
        type("signup-password", "secret1")
        tap("signup-submit")
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("onboarding-page") }
        tap("onb-submit")
        compose.waitUntil(5_000) {
            compose.onAllNodesWithTextExists(
                "An error occurred: Exception: Failed to post user data: {\"detail\":\"Not Found\"}",
            )
        }
    }

    @Test
    fun phoneSignInAlwaysGoesToOnboarding() {
        start()
        tap("social-phone")
        dialogType("phone-dialog-input", "+1 555 0100")
        dialogTap("phone-dialog-submit")
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("code-dialog-input") }
        dialogType("code-dialog-input", "123456")
        dialogTap("code-dialog-submit")
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("onboarding-page") }
        compose.onNodeWithText("Let's Get Started!").assertIsDisplayed()
    }
}
