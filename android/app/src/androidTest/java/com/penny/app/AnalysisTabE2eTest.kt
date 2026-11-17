package com.penny.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.penny.app.core.Expense
import com.penny.app.ui.ExpenseTrackerScreen
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/** 013: the Analysis tab (income_analysis.dart) on the fake backend. */
class AnalysisTabE2eTest {

    @get:Rule
    val compose = createComposeRule()

    private val today = LocalDate.of(2026, 5, 15)

    private fun open(): FakeExpenseRegistry {
        val registry = FakeExpenseRegistry.demo(today)
        val base = PennyServices.fakes(signedIn = true)
        val services = PennyServices(base.auth, base.users, base.prefs, base.profileImages, expenses = registry::forUser)
        compose.setContent { ExpenseTrackerScreen(services, onBack = {}, today = today) }
        compose.onNodeWithTag("tracker-tab-analysis").performClick()
        return registry
    }

    private fun waitText(tag: String, text: String) = compose.waitUntil(5_000) {
        runCatching { compose.onNodeWithTag(tag, useUnmergedTree = true).assertTextEquals(text) }.isSuccess
    }

    private fun pick(dropdown: String, option: String) {
        compose.onNodeWithTag(dropdown).performScrollTo().performClick()
        compose.onNodeWithTag("$dropdown-$option").performClick()
    }

    @Test
    fun splitAndFirstCategoryForTheDemoMonth() {
        open()
        waitText("analysis-essentials", "\$25000.00")
        compose.onNodeWithTag("analysis-wants").assertTextEquals("\$15000.00")
        compose.onNodeWithTag("analysis-savings").assertTextEquals("\$10000.00")
        compose.onNodeWithTag("analysis-chart").assertIsDisplayed()
        compose.onNodeWithTag("analysis-card-title-0", useUnmergedTree = true).performScrollTo().assertTextEquals("Food")
        compose.onNodeWithTag("analysis-card-total-0", useUnmergedTree = true).assertTextEquals("\$4450.50")
    }

    @Test
    fun aMonthWithExpensesButNoIncomeEntryShowsNoData() {
        val registry = open()
        // June has an expense but no income entry → backend int 0 → TypeError → wiped.
        kotlinx.coroutines.runBlocking { registry.forUser("demo").addExpense(Expense("Lunch", 12.0, "Food", 2026, "June")) }
        pick("analysis-month", "June")
        waitText("analysis-empty-text", "No data available for June 2026")
        compose.onNodeWithTag("analysis-essentials").assertTextEquals("\$0.00")
    }

    @Test
    fun previousMonthHasItsOwnIncomeAndCategory() {
        open()
        pick("analysis-month", "April")
        waitText("analysis-savings", "\$9600.00")
        compose.onNodeWithTag("analysis-card-title-0", useUnmergedTree = true).performScrollTo().assertTextEquals("Miscellaneous")
    }
}
