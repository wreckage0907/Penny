package com.penny.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.penny.app.ui.BudgetScreen
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/**
 * v2.1 Budget (frames 05–08 + S01): the artboard's November dataset on the
 * screen — donut and reconciled summary, ranked categories, the category
 * sheet with edit/delete/undo, the 50/30/20 plan with the corrected SIP
 * illustration, Quick Log validation, and month stepping. Rendered directly
 * with a fixed date, like the v1 dashboard tests.
 */
class BudgetE2eTest {

    private val today = LocalDate.of(2026, 11, 25)

    @get:Rule
    val compose = createComposeRule()

    private fun boot() = PennyServices.fakes(signedIn = true, today = today).also { services ->
        compose.setContent { BudgetScreen(services, navigate = {}, today = today) }
        compose.waitUntil(5_000) { compose.onAllNodesWithTextExists("₹31,450") }
    }

    @Test
    fun theDemoMonthReconcilesLikeTheArtboard() {
        boot()
        compose.onNodeWithTag("budget-month").assertTextEquals("Nov 2026")
        compose.onNodeWithTag("budget-total").assertTextEquals("−₹31,450")
        assertTrue(compose.onAllNodesWithTextExists("+₹53,550"))
        assertTrue(compose.onAllNodesWithTextExists("₹17,000"))
        assertTrue(compose.onAllNodesWithTextExists("₹36,550"))
        assertTrue(compose.onAllNodesWithTextExists("18 ITEMS"))

        assertTrue(compose.onAllNodesWithTextExists("Food & Dining"))
        assertTrue(compose.onAllNodesWithTextExists("Rent & Utilities"))
        assertTrue(compose.onAllNodesWithTextExists("−₹11,200"))
        assertTrue(compose.onAllNodesWithTextExists("−₹11,200"))
        assertTrue(compose.onAllNodesWithTextExists("−₹9,500"))
        assertTrue(compose.onAllNodesWithTextExists("5 categories"))
    }

    @Test
    fun thePlanTabShowsTargetsRemaindersAndTheCorrectedSip() {
        boot()
        compose.onNodeWithTag("budget-tab-plan").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("plan-needs") }

        compose.onNodeWithTag("plan-needs-actual").assertTextEquals("−₹23,800")
        assertTrue(compose.onAllNodesWithTextExists("₹42,500 target"))
        assertTrue(compose.onAllNodesWithTextExists("56%"))
        assertTrue(compose.onAllNodesWithTextExists("₹18,700 budget remainder"))
        compose.onNodeWithTag("plan-wants-actual").assertTextEquals("−₹7,650")
        assertTrue(compose.onAllNodesWithTextExists("₹17,850 budget remainder"))
        compose.onNodeWithTag("plan-sip-actual").assertTextEquals("₹17,000 recorded")
        assertTrue(compose.onAllNodesWithTextExists("100% · goal met"))
        assertTrue(compose.onAllNodesWithTextExists("₹36,550 unallocated", substring = true))
        assertTrue(compose.onAllNodesWithTextExists("Unspent cash is not counted until you record an allocation."))

        // Frame 08's corrected illustrations: 7% ₹29.6L · 10% ₹35.1L · 12% ₹39.5L.
        assertTrue(compose.onAllNodesWithTextExists("₹17,000/month · ₹20.4L contributed", substring = true))
        compose.onNodeWithTag("sip-headline").assertTextEquals("₹39.5L")
        compose.onNodeWithTag("sip-rate-0").performScrollTo().performClick()
        compose.onNodeWithTag("sip-headline").assertTextEquals("₹29.6L")
        assertTrue(compose.onAllNodesWithTextExists("FD 7% → ₹29.6L", substring = true))
        assertTrue(compose.onAllNodesWithTextExists("12% exact → ₹39,49,764"))
        assertTrue(compose.onAllNodesWithTextExists("Returns are not guaranteed", substring = true))
    }

    @Test
    fun theCategorySheetListsTransactionsWithTheirMetadataAndQuickAdds() {
        val services = boot()
        compose.onNodeWithTag("budget-category-0").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("category-sheet") }

        compose.onNodeWithTag("category-title").assertTextEquals("Food & Dining")
        compose.onNodeWithTag("category-tx-0").assertIsDisplayed()
        assertTrue(compose.onAllNodesWithTextExists("BigBasket Monthly"))
        assertTrue(compose.onAllNodesWithTextExists("18 Nov · Groceries · UPI"))
        assertTrue(compose.onAllNodesWithTextExists("Swiggy Family Dinner"))
        assertTrue(compose.onAllNodesWithTextExists("6 transactions · Nov 2026 · 35.6% of spend"))

        // The sheet's quick add books straight into the open category.
        compose.onNodeWithTag("category-quick-amount-input").performTextInput("150")
        compose.onNodeWithTag("category-quick-add").performScrollTo().performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTextExists("Expense saved · Undo") }
        // …and undo takes it right back out.
        compose.onNodeWithTag("budget-toast-action").performClick()
        compose.waitUntil(5_000) { !compose.onAllNodesWithTextExists("−₹11,350") }
        assertTrue(compose.onAllNodesWithTextExists("−₹11,200"))
    }

    @Test
    fun deletingAsksFirstAndUndoes() {
        boot()
        compose.onNodeWithTag("budget-category-0").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("category-sheet") }

        compose.onNodeWithTag("category-delete-1").performClick() // Swiggy Family Dinner
        compose.onNodeWithTag("delete-tx-modal").assertIsDisplayed()
        assertTrue(compose.onAllNodesWithTextExists("Swiggy Family Dinner"))
        assertTrue(compose.onAllNodesWithTextExists("This updates Food & Dining, monthly totals, insights, and future AI answers."))
        compose.onNodeWithTag("delete-tx-cancel").performClick()
        compose.waitUntil(5_000) { !compose.onAllNodesWithTagExists("delete-tx-modal") }

        compose.onNodeWithTag("category-delete-1").performClick()
        compose.onNodeWithTag("delete-tx-confirm").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTextExists("Transaction deleted · Undo") }
        compose.onNodeWithTag("budget-toast-action").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTextExists("Swiggy Family Dinner") }
    }

    @Test
    fun quickLogValidatesBothRequiredFieldsThenSavesWithUndo() {
        val services = boot()
        compose.onNodeWithTag("dock-0").performClick() // Add Expense
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("quicklog-sheet") }

        // Empty submit points at the amount, then the category.
        compose.onNodeWithTag("quicklog-submit").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTextExists("Amount is required.") }
        compose.onNodeWithTag("quicklog-amount-input").performTextInput("999")
        compose.onNodeWithTag("quicklog-submit").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTextExists("Choose a category.") }

        compose.onNodeWithTag("quicklog-cat-0").performClick() // Food & Dining
        compose.onNodeWithTag("quicklog-merchant-input").performTextInput("Test Kettle")
        compose.onNodeWithTag("quicklog-submit").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTextExists("Expense saved · Undo") }
        compose.waitUntil(5_000) { compose.onAllNodesWithTextExists("₹32,449") } // 31,450 + 999

        compose.onNodeWithTag("budget-toast-action").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTextExists("₹31,450") }
    }

    @Test
    fun monthSteppingCrossesMonthsAndEmptyMonthsTellTheTruth() {
        boot()
        compose.onNodeWithTag("budget-prev").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTextExists("Nothing logged for Oct 2026") }
        compose.onNodeWithTag("budget-next").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTextExists("₹31,450") }
    }
}
