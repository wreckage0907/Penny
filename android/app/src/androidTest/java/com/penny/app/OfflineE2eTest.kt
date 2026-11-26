package com.penny.app

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.penny.app.ui.BudgetScreen
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/**
 * v2.1 offline state library (S04): cached data with a retry, never an
 * error screen — driven by the Connectivity fake directly, plus one
 * activity-level check of the broadcast lever the suite uses.
 */
class OfflineE2eTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun offlineShowsCachedDataWithRetryAndARefreshError() {
        val services = PennyServices.fakes(signedIn = true, today = LocalDate.of(2026, 11, 25))
        compose.setContent { BudgetScreen(services, navigate = {}, today = LocalDate.of(2026, 11, 25)) }
        compose.waitUntil(5_000) { compose.onAllNodesWithTextExists("₹31,450") }

        services.connectivity.setOnline(false)
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("budget-offline") }
        assertTrue(compose.onAllNodesWithTextExists("Offline. Showing 18 saved transactions from Nov 2026", substring = true))
        // Cached data remains visible — no error screen.
        assertTrue(compose.onAllNodesWithTextExists("₹31,450"))

        // A retry while offline raises the honest refresh error, data intact.
        compose.onNodeWithTag("budget-retry").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("budget-refresh-error") }
        assertTrue(compose.onAllNodesWithTextExists("Couldn't refresh Nov. Your saved data is unchanged.", substring = true))

        // Reconnecting auto-refreshes: both banners clear themselves.
        services.connectivity.setOnline(true)
        compose.waitUntil(5_000) { !compose.onAllNodesWithTagExists("budget-refresh-error") }
        compose.waitUntil(5_000) { !compose.onAllNodesWithTagExists("budget-offline") }
        assertFalse(compose.onAllNodesWithTextExists("Offline. Showing"))
    }
}
