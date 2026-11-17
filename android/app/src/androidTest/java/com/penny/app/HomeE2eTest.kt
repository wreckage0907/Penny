package com.penny.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import com.penny.app.core.FakeProfileImages
import com.penny.app.ui.PennyApp
import org.junit.Rule
import org.junit.Test

/** 008: Home (home.dart) on the fake services. */
class HomeE2eTest {

    @get:Rule
    val compose = createComposeRule()

    private fun signedIn(images: FakeProfileImages = FakeProfileImages()): PennyServices {
        val base = PennyServices.fakes(signedIn = true)
        val services = PennyServices(base.auth, base.users, base.prefs, images)
        compose.setContent { PennyApp(services) }
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("home-page") }
        return services
    }

    @Test
    fun greetingFromClaimsDefaultAvatarAndHardCodedCourseProgress() {
        signedIn()
        compose.waitUntil(5_000) {
            runCatching { compose.onNodeWithTag("home-greeting").assertTextEquals("WELCOME,\nDemo User") }.isSuccess
        }
        compose.onNodeWithTag("home-avatar-default").assertIsDisplayed()
        compose.onNodeWithTag("home-course-progress", useUnmergedTree = true).assertTextEquals("74%")
        compose.onNodeWithTag("home-nav-home").assertIsNotEnabled()
    }

    @Test
    fun profileImageUrlSwitchesToTheImageAvatar() {
        signedIn(FakeProfileImages().apply { set("demo", "https://example.invalid/demo.jpg") })
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("home-avatar-image") }
    }

    @Test
    fun tilesAndBottomBarOpenTheirFlutterRoutes() {
        signedIn()
        // 009: the Expense Tracker tile opens the real tracker.
        compose.onNodeWithTag("home-tile-1").performScrollTo().performClick()
        compose.onNodeWithTag("tracker-tab-dashboard").assertIsDisplayed()
        compose.onNodeWithTag("tracker-back").performClick()
        // 011: the Practice tile opens the real practice list.
        compose.onNodeWithTag("home-tile-3").performScrollTo().performClick()
        compose.onNodeWithTag("practice-page").assertIsDisplayed()
        compose.onNodeWithTag("back").performClick()
        // 014: the Predict Stocks tile opens the Market page.
        compose.onNodeWithTag("home-tile-2").performScrollTo().performClick()
        compose.onNodeWithTag("stock-page").assertIsDisplayed()
        compose.onNodeWithTag("back").performClick()
        compose.onNodeWithTag("home-nav-chat").performClick()
        compose.onNodeWithTag("not-migrated-route").assertTextEquals("/chatbot — not migrated yet")
    }

    @Test
    fun financeNewsShowsBundledArticlesAndSwipes() {
        signedIn()
        compose.onNodeWithTag("home-news-pager").performScrollTo()
        // Clickable cards merge their children; read the title node itself.
        compose.onNodeWithTag("news-title-0", useUnmergedTree = true)
            .assertTextEquals("Bajaj Freedom CNG and petrol mileage explained! - BikeWale")
        compose.onNodeWithTag("home-news-pager").performTouchInput { swipeLeft() }
        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("news-title-1", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
