package com.penny.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.penny.app.core.AuthState
import com.penny.app.ui.PennyApp
import org.junit.Rule
import org.junit.Test

/** 012: Profile Settings + Edit Profile on the fake services. */
class ProfileE2eTest {

    @get:Rule
    val compose = createComposeRule()

    private fun open(pick: String? = "content://penny.test/picked.jpg"): PennyServices {
        val base = PennyServices.fakes(signedIn = true)
        val services = PennyServices(base.auth, base.users, base.prefs, base.profileImages, base.expenses,
            base.courses, base.speech, base.mcq, pickImage = { pick })
        compose.setContent { PennyApp(services) }
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("home-page") }
        compose.onNodeWithTag("home-nav-profile").performClick()
        compose.waitUntil(5_000) {
            runCatching { compose.onNodeWithTag("profile-fullname").assertTextEquals("Demo User") }.isSuccess
        }
        return services
    }

    private fun waitText(text: String) = compose.waitUntil(5_000) { compose.onAllNodesWithTextExists(text) }

    @Test
    fun settingsShowsIdentityAndSignOutReturnsToLogin() {
        open()
        compose.onNodeWithTag("profile-username").assertTextEquals("demo")
        compose.onNodeWithTag("profile-avatar-default").assertIsDisplayed()
        compose.onNodeWithTag("profile-sign-out").performScrollTo().performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("login-page") }
    }

    @Test
    fun changePasswordValidatesThenWorksForTheNextLogin() {
        val services = open()
        compose.onNodeWithTag("profile-change-password").performScrollTo().performClick()
        compose.onNodeWithTag("pw-current").performTextInput("penny123")
        compose.onNodeWithTag("pw-new").performTextInput("newpass1")
        compose.onNodeWithTag("pw-confirm").performTextInput("newpass2")
        compose.onNodeWithTag("pw-change").performClick()
        waitText("New passwords do not match")
        compose.onNodeWithTag("pw-confirm").performTextClearance()
        compose.onNodeWithTag("pw-confirm").performTextInput("newpass1")
        compose.onNodeWithTag("pw-change").performClick()
        waitText("Password changed successfully")
        check(kotlinx.coroutines.runBlocking {
            services.auth.signOut(); services.auth.login("demo@penny.app", "newpass1") != null
        })
    }

    @Test
    fun editProfilePrefillsSavesWithFourToastsAndUpdatesClaims() {
        val services = open()
        compose.onNodeWithTag("profile-edit").performScrollTo().performClick()
        compose.waitUntil(5_000) {
            runCatching { compose.onNodeWithTag("edit-first").assertTextEquals("Demo") }.isSuccess
        }
        compose.onNodeWithTag("edit-username").assertTextEquals("demo")
        compose.onNodeWithTag("edit-email").assertTextEquals("demo@penny.app")
        compose.onNodeWithTag("edit-first").performTextClearance()
        compose.onNodeWithTag("edit-first").performTextInput("Dee")
        compose.onNodeWithTag("edit-save").performScrollTo().performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("toast-warning") }
        listOf("success", "error", "info", "warning").forEach {
            compose.onNodeWithTag("toast-$it").assertTextEquals("Profile Updated Successfully")
        }
        check(kotlinx.coroutines.runBlocking { services.auth.customClaims()?.get("fullName") } == "Dee User")
    }

    @Test
    fun pictureUploadThenDelete() {
        open()
        compose.onNodeWithTag("profile-edit").performScrollTo().performClick()
        compose.onNodeWithTag("edit-avatar-default").assertIsDisplayed()
        compose.onNodeWithTag("edit-picture").performClick()
        waitText("Profile picture updated successfully")
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("edit-avatar-image") }
        compose.onNodeWithTag("edit-delete-picture").performClick()
        waitText("Profile picture deleted successfully")
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("edit-avatar-default") }
    }

    @Test
    fun deleteAccountRemovesTheAccountAndReturnsToLogin() {
        val services = open()
        compose.onNodeWithTag("profile-delete").performScrollTo().performClick()
        compose.onNodeWithTag("delete-account-confirm").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("login-page") }
        check(services.auth.state.value == AuthState.SignedOut)
        check(kotlinx.coroutines.runBlocking { services.users.getUser("demo") } == null)
        check(kotlinx.coroutines.runBlocking { services.auth.login("demo@penny.app", "penny123") } == null)
    }
}
