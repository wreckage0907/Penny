package com.penny.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.penny.app.core.AuthFlows
import com.penny.app.ui.PennyApp
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * v2.1 auth (frames 01–03): inline login errors, sign-up-with-profile
 * (username taken checked live), and the social/OTP completion path.
 */
class AuthE2eTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun wrongPasswordShowsAnInlineErrorAndStaysPut() {
        val services = PennyServices.fakes()
        compose.setContent { PennyApp(services) }

        compose.onNodeWithTag("login-id-input").performTextInput("demo")
        compose.waitForIdle()
        compose.onNodeWithTag("login-password-input").performTextInput("nope42")
        compose.waitForIdle()
        compose.onNodeWithTag("login-submit").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTextExists("Could not sign in", substring = true) }
        compose.onNodeWithTag("login-error").assertIsDisplayed()
        assertEquals(com.penny.app.core.AuthState.SignedOut, services.auth.state.value)
    }

    @Test
    fun signUpCreatesTheProfileAndLandsHomeWithTheFirstNameGreeting() {
        val services = PennyServices.fakes()
        compose.setContent { PennyApp(services) }

        compose.onNodeWithTag("login-signup-link").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("signup-page") }
        compose.onNodeWithTag("signup-name-input").performTextInput("New Bie")
        compose.waitForIdle()
        compose.onNodeWithTag("signup-username-input").performTextInput("newbie")
        compose.waitForIdle()
        compose.onNodeWithTag("signup-email-input").performTextInput("new@bie.io")
        compose.waitForIdle()
        compose.onNodeWithTag("signup-password-input").performTextInput("secret1")
        compose.waitForIdle()
        compose.onNodeWithTag("signup-submit").performScrollTo().performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("home-page") }
        assertTrue(compose.onAllNodesWithTextExists(", New", substring = true))
        runBlocking { assertEquals("newbie", services.prefs.username()) }
    }

    @Test
    fun aTakenUsernameIsCaughtBeforeAnAccountIsStranded() {
        val services = PennyServices.fakes()
        compose.setContent { PennyApp(services) }

        compose.onNodeWithTag("login-signup-link").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("signup-page") }
        compose.onNodeWithTag("signup-name-input").performTextInput("Demo Two")
        compose.waitForIdle()
        compose.onNodeWithTag("signup-username-input").performTextInput("demo")
        compose.waitForIdle()
        compose.onNodeWithTag("signup-email-input").performTextInput("d2@penny.app")
        compose.waitForIdle()
        compose.onNodeWithTag("signup-password-input").performTextInput("secret1")
        compose.waitForIdle()
        compose.onNodeWithTag("signup-submit").performScrollTo().performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTextExists("That username is already taken", substring = true) }
        // Still on the sign-up page, no auth account was created.
        compose.onNodeWithTag("signup-page").assertIsDisplayed()
        assertEquals(com.penny.app.core.AuthState.SignedOut, services.auth.state.value)
    }

    @Test
    fun phoneOtpSignsInAndCompletesTheProfileOnboarding() {
        val services = PennyServices.fakes()
        compose.setContent { PennyApp(services) }

        compose.onNodeWithTag("social-phone-otp").performClick()
        compose.onNodeWithTag("phone-input-input").performTextInput("1 555 0100")
        compose.waitForIdle()
        compose.onNodeWithTag("phone-submit").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("code-dialog") }
        compose.onNodeWithTag("code-input-input").performTextInput(com.penny.app.core.FakeAuthService.FAKE_SMS_CODE)
        compose.waitForIdle()
        compose.onNodeWithTag("code-submit").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("onboarding-page") }
        compose.onNodeWithTag("onb-username-input").performTextInput("phonie")
        compose.waitForIdle()
        compose.onNodeWithTag("onb-first-input").performTextInput("Phone")
        compose.waitForIdle()
        compose.onNodeWithTag("onb-last-input").performTextInput("User")
        compose.waitForIdle()
        compose.onNodeWithTag("onb-email-input").performTextInput("phone@penny.app")
        compose.waitForIdle()
        compose.onNodeWithTag("onb-submit").performScrollTo().performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("home-page") }
        runBlocking { assertEquals("phonie", services.prefs.username()) }
    }

    @Test
    fun googleContinuesFirstRunIntoOnboarding() {
        val services = PennyServices.fakes()
        compose.setContent { PennyApp(services) }

        compose.onNodeWithTag("social-google").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("onboarding-page") }
    }
}
