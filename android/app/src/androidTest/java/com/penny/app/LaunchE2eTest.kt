package com.penny.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Rule
import org.junit.Test

/** 006: the real activity boots into the AuthWrapper (signed out → login). */
class LaunchE2eTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun coldLaunchShowsLogin() {
        compose.onNodeWithTag("login-page").assertIsDisplayed()
    }
}
