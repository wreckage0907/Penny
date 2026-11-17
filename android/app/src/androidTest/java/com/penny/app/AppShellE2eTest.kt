package com.penny.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.penny.app.core.FakeAuthService
import com.penny.app.ui.PennyApp
import org.junit.Rule
import org.junit.Test

/** 006: AuthWrapper routing + named routes on fake services. */
class AppShellE2eTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun signedOutLandsOnLoginThenDemoLoginReachesHomeAndRoutes() {
        val services = PennyServices(auth = FakeAuthService.demo())
        compose.setContent { PennyApp(services) }

        compose.onNodeWithTag("shell-login").assertIsDisplayed()
        compose.onNodeWithTag("shell-demo-login").performClick()
        compose.onNodeWithTag("shell-home").assertIsDisplayed()

        compose.onNodeWithTag("shell-route-/coursepage").performScrollTo().performClick()
        compose.onNodeWithTag("not-migrated-route").assertTextEquals("/coursepage — not migrated yet")
        compose.onNodeWithTag("not-migrated-back").performClick()
        compose.onNodeWithTag("shell-home").assertIsDisplayed()

        compose.onNodeWithTag("shell-sign-out").performScrollTo().performClick()
        compose.onNodeWithTag("shell-login").assertIsDisplayed()
    }

    @Test
    fun seededSignedInUserSkipsLogin() {
        val auth = FakeAuthService(
            mapOf(FakeAuthService.DEMO_EMAIL to FakeAuthService.DEMO_PASSWORD),
            signedInAs = FakeAuthService.DEMO_EMAIL,
        )
        compose.setContent { PennyApp(PennyServices(auth)) }
        compose.onNodeWithTag("shell-home").assertIsDisplayed()
    }
}
