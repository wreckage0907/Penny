package com.penny.app

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import com.penny.app.ui.PennyApp
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * v2.1 account recovery (frame 02): request → expired seed → new code →
 * reset → back to Sign In, and the new password works.
 */
class RecoveryE2eTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun theExpiredSeedCodeSaysSoAndAFreshCodeResetsThePassword() {
        val services = PennyServices.fakes()
        compose.setContent { PennyApp(services) }

        compose.onNodeWithTag("login-forgot").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("recovery-page") }

        compose.onNodeWithTag("recovery-email-input").performTextInput("demo@penny.app")
        compose.waitForIdle()
        compose.onNodeWithTag("recovery-send").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("recovery-code") }

        // The seeded 824109 is expired — frame 02's state.
        compose.onNodeWithTag("recovery-code-input").performTextInput("824109")
        compose.waitForIdle()
        compose.onNodeWithTag("recovery-password-input").performTextInput("newpassword")
        compose.waitForIdle()
        compose.onNodeWithTag("recovery-submit").performScrollTo().performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTextExists("This code expired. Request a new code, then try again.") }

        // Request a fresh code (the fake's is 123456) and save.
        compose.onNodeWithTag("recovery-resend").performClick()
        compose.onNodeWithTag("recovery-code-input").performTextClearance()
        compose.onNodeWithTag("recovery-code-input").performTextInput("123456")
        compose.waitForIdle()
        compose.onNodeWithTag("recovery-submit").performScrollTo().performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("recovery-done") }
        assertTrue(compose.onAllNodesWithTextExists("Password saved. Return to Sign In — existing sessions were signed out.", substring = true))

        compose.onNodeWithTag("recovery-back").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("login-page") }

        // The old password is dead; the new one signs in.
        compose.onNodeWithTag("login-id-input").performTextInput("demo@penny.app")
        compose.waitForIdle()
        compose.onNodeWithTag("login-password-input").performTextInput("penny123")
        compose.waitForIdle()
        compose.onNodeWithTag("login-submit").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTextExists("Could not sign in", substring = true) }
        compose.onNodeWithTag("login-password-input").performTextClearance()
        compose.onNodeWithTag("login-password-input").performTextInput("newpassword")
        compose.waitForIdle()
        compose.onNodeWithTag("login-submit").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("home-page") }
    }

    @Test
    fun anUnknownAccountNeverClaimsToSendACode() {
        val services = PennyServices.fakes()
        compose.setContent { PennyApp(services) }

        compose.onNodeWithTag("login-forgot").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("recovery-page") }
        compose.onNodeWithTag("recovery-email-input").performTextInput("who@penny.app")
        compose.waitForIdle()
        compose.onNodeWithTag("recovery-send").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTextExists("We couldn't find that account.", substring = true) }
        assertNull(services.auth.state.value as? com.penny.app.core.AuthState.SignedIn)
        assertNotNull(services.auth.state.value as? com.penny.app.core.AuthState.SignedOut)
    }
}
