package com.penny.app

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.penny.app.ui.PennyApp
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * v2.1 Profile (frames 14–15 + S02/S05): saved-state banner, achievements,
 * password change sheet, CSV export, the unsaved-changes guard, staged
 * photo removal, and the delete-account modal with reauthentication.
 */
class ProfileE2eTest {

    @get:Rule
    val compose = createComposeRule()

    private fun boot(pickImage: (suspend () -> String?)? = null): PennyServices {
        val services = PennyServices.fakes(signedIn = true, pickImage = pickImage)
        compose.setContent { PennyApp(services) }
        compose.onNodeWithTag("nav-profile").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("profile-page") }
        return services
    }

    @Test
    fun theProfileShowsIdentityAchievementsAndHonestPreferences() {
        boot()
        compose.onNodeWithTag("profile-fullname").assertTextEquals("Demo User")
        assertTrue(compose.onAllNodesWithTextExists("@demo · demo@penny.app"))
        compose.onNodeWithTag("profile-streak").assertTextEquals("🔥 5-day streak")
        compose.onNodeWithTag("profile-coins").assertTextEquals("◈ 240 coins")
        compose.onNodeWithTag("profile-badges").assertTextEquals("★ 0 badges")
        compose.onNodeWithTag("profile-currency").assertTextEquals("INR · ₹")
        compose.onNodeWithTag("pref-currency").performScrollTo()
        assertTrue(compose.onAllNodesWithTextExists("INR (₹) · display only"))
        compose.onNodeWithTag("profile-export").performScrollTo()
        assertTrue(compose.onAllNodesWithTextExists("18 rows", substring = true))
    }

    @Test
    fun changePasswordValidatesMismatchThenSucceeds() {
        val services = boot()
        compose.onNodeWithTag("profile-change-password").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("pw-sheet") }

        compose.onNodeWithTag("pw-current-input").performTextInput("penny123")
        compose.onNodeWithTag("pw-new-input").performTextInput("newpass1")
        compose.onNodeWithTag("pw-confirm-input").performTextInput("newpass2")
        compose.onNodeWithTag("pw-change").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTextExists("New passwords do not match") }

        compose.onNodeWithTag("pw-confirm-input").performTextClearance()
        compose.onNodeWithTag("pw-confirm-input").performTextInput("newpass1")
        compose.onNodeWithTag("pw-change").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTextExists("Password changed successfully", substring = true) }
        runBlocking { assertTrue(services.auth.login("demo@penny.app", "newpass1") != null) }
    }

    @Test
    fun exportCsvShowsTheMonthExactly() {
        boot()
        compose.onNodeWithTag("profile-export").performScrollTo().performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("export-modal") }
        compose.onNodeWithTag("export-csv").assertExists()
        assertTrue(compose.onAllNodesWithTextExists("Date,Category,Merchant,Payment,Amount", substring = true))
        assertTrue(compose.onAllNodesWithTextExists("18 rows", substring = true))
    }

    @Test
    fun editingAsksBeforeDiscardingAndConfirmsTheSave() {
        val services = boot()
        compose.onNodeWithTag("profile-edit").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("edit-page") }
        // The prefill loads async — wait for it, then edit (typing alone appends).
        compose.waitUntil(5_000) { compose.onAllNodesWithTextExists("Demo User") }
        compose.onNodeWithTag("edit-name-input").performTextClearance()
        compose.onNodeWithTag("edit-name-input").performTextInput("Demo User Jr.")
        assertTrue(compose.onAllNodesWithTextExists("Unsaved changes"))
        compose.onNodeWithTag("back").performClick() // the guard opens, nothing is lost silently
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("unsaved-modal") }
        compose.onNodeWithTag("unsaved-keep").performClick()
        compose.waitUntil(5_000) { !compose.onAllNodesWithTagExists("unsaved-modal") }

        compose.onNodeWithTag("edit-save").performScrollTo().performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("profile-saved-banner") }
        assertTrue(compose.onAllNodesWithTextExists("Profile saved. Your changes are up to date."))
        compose.onNodeWithTag("profile-fullname").assertTextEquals("Demo User Jr.")
        runBlocking { assertEquals("Demo User Jr.", services.prefs.fullName()) }
    }

    @Test
    fun stagedPhotoRemovalIsReversibleAndSavesOnlyOnDemand() {
        boot(pickImage = { "content://picked/1" })
        compose.onNodeWithTag("profile-edit").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("edit-page") }

        compose.onNodeWithTag("edit-picture").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("edit-avatar-image") }
        compose.onNodeWithTag("edit-remove-picture").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTextExists("Photo will be removed only when you save.", substring = true) }
        compose.onNodeWithTag("edit-undo-remove").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("edit-avatar-image") } // back to the picked photo
    }

    @Test
    fun deleteAccountListsTheImpactRequiresThePasswordAndCascades() {
        val services = boot()
        compose.onNodeWithTag("profile-delete").performScrollTo().performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("delete-account-modal") }

        assertTrue(compose.onAllNodesWithTextExists("18 Nov transactions and all other finance data", substring = true))
        assertTrue(compose.onAllNodesWithTextExists("Course progress, streaks, badges, and 240 coins", substring = true))

        compose.onNodeWithTag("delete-password-input").performTextInput("wrongpass")
        compose.onNodeWithTag("delete-account-confirm").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTextExists("That password doesn't match. Try again.") }

        compose.onNodeWithTag("delete-password-input").performTextClearance()
        compose.onNodeWithTag("delete-password-input").performTextInput("penny123")
        compose.onNodeWithTag("delete-account-confirm").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("login-page") }
        runBlocking {
            assertEquals(com.penny.app.core.AuthState.SignedOut, services.auth.state.value)
            assertNull(services.users.getUser("demo"))
        }
    }

    @Test
    fun toolsReachTheMarketPracticeScreen() {
        boot()
        compose.onNodeWithTag("tools-stocks").performScrollTo().performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("stock-page") }
    }
}
