package com.penny.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.penny.app.ui.ExpenseTrackerScreen
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/** 009: the expense tracker dashboard (expense_dashboard.dart) on the fake backend. */
class ExpenseTrackerE2eTest {

    @get:Rule
    val compose = createComposeRule()

    private val today = LocalDate.of(2026, 5, 15)

    private fun open(): PennyServices {
        val base = PennyServices.fakes(signedIn = true)
        val services = PennyServices(
            base.auth, base.users, base.prefs, base.profileImages,
            expenses = FakeExpenseRegistry.demo(today)::forUser,
        )
        compose.setContent { ExpenseTrackerScreen(services, onBack = {}, today = today) }
        waitText("dash-expenses", "\$9851.24")
        return services
    }

    private fun waitText(tag: String, text: String) = compose.waitUntil(5_000) {
        runCatching { compose.onNodeWithTag(tag, useUnmergedTree = true).assertTextEquals(text) }.isSuccess
    }

    private fun waitAnyText(text: String) = compose.waitUntil(5_000) { compose.onAllNodesWithTextExists(text) }

    private fun pick(dropdown: String, option: String) {
        compose.onNodeWithTag(dropdown).performClick()
        compose.onNodeWithTag("$dropdown-$option").performClick()
    }

    @Test
    fun demoMonthShowsTotalsIncomeLegendAndCategories() {
        open()
        compose.onNodeWithTag("dash-total", useUnmergedTree = true).assertTextEquals("\$9851.24")
        compose.onNodeWithTag("dash-income").assertTextEquals("\$50000.00")
        compose.onNodeWithTag("dash-month-title").assertTextEquals("May")
        compose.onNodeWithTag("dash-bar-income").assertIsDisplayed()
        compose.onNodeWithTag("dash-category-total-0", useUnmergedTree = true).performScrollTo().assertTextEquals("\$4450.50")
        compose.onNodeWithTag("dash-category-total-4", useUnmergedTree = true).performScrollTo().assertTextEquals("\$650.00")
    }

    @Test
    fun switchingMonthsReloadsAndEmptyMonthsShowNoDataAndIntZeroIncome() {
        open()
        pick("dash-month", "April")
        waitText("dash-expenses", "\$9000.00")
        compose.onNodeWithTag("dash-income").assertTextEquals("\$48000.00")
        pick("dash-month", "June")
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("dash-no-data") }
        waitText("dash-expenses", "\$0.00")
        // June has no income entry → backend int 0 → Dart TypeError → 0.0.
        compose.onNodeWithTag("dash-income").assertTextEquals("\$0.00")
    }

    @Test
    fun addExpenseValidatesThenAdds() {
        open()
        compose.onNodeWithTag("dash-add").performScrollTo().performClick()
        compose.onNodeWithTag("add-submit").performClick()
        waitAnyText("Please fill all fields")

        compose.onNodeWithTag("add-name").performTextInput("Gift card")
        compose.onNodeWithTag("add-amount").performTextInput("abc")
        compose.onNodeWithTag("add-category").performClick()
        compose.onNodeWithTag("add-category-Gifts").performClick()
        compose.onNodeWithTag("add-submit").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("add-submit").assertIsDisplayed() // double.parse threw: dialog stays, no message

        compose.onNodeWithTag("add-amount").performTextClearance()
        compose.onNodeWithTag("add-amount").performTextInput("100")
        compose.onNodeWithTag("add-submit").performClick()
        waitAnyText("Expense added successfully")
        waitText("dash-expenses", "\$9951.24")
    }

    @Test
    fun editAndDeleteFromTheCategorySheet() {
        open()
        compose.onNodeWithTag("dash-category-0").performScrollTo().performClick()
        compose.onNodeWithTag("sheet-title", useUnmergedTree = true).assertTextEquals("Expenses for Food")
        compose.onNodeWithTag("sheet-edit-1").performClick()
        compose.onNodeWithTag("edit-amount").performTextClearance()
        compose.onNodeWithTag("edit-amount").performTextInput("1000")
        compose.onNodeWithTag("edit-save").performClick()
        waitAnyText("Expense updated successfully")
        waitText("dash-expenses", "\$9651.24")
        compose.onNodeWithTag("sheet-amount-1", useUnmergedTree = true).assertTextEquals("\$1000.00")

        compose.onNodeWithTag("sheet-delete-0").performClick()
        compose.onNodeWithTag("delete-confirm").performClick()
        waitAnyText("Expense Deleted Successfully")
        waitText("dash-expenses", "\$6400.74")
    }

    @Test
    fun incomeDialogRejectsGarbageAndShowsTheEnteredAmountNotTheSum() {
        val services = open()
        compose.onNodeWithTag("dash-income-button").performClick()
        compose.onNodeWithTag("income-amount").performTextInput("1,000")
        compose.onNodeWithTag("income-add").performClick()
        waitAnyText("Invalid amount entered")

        compose.onNodeWithTag("dash-income-button").performClick()
        compose.onNodeWithTag("income-amount").performTextInput("1000")
        compose.onNodeWithTag("income-add").performClick()
        // Flutter shows the entered amount; the backend ADDS (50000 + 1000).
        waitText("dash-income", "\$1000.00")
        compose.waitUntil(5_000) {
            kotlinx.coroutines.runBlocking { services.expenses("demo").getIncome(2026, "May").toString() } ==
                """{"2026":{"May":51000.0}}"""
        }
    }

    @Test
    fun analysisTabOpensTheAnalysisPage() {
        open()
        compose.onNodeWithTag("tracker-tab-analysis").performClick()
        compose.onNodeWithTag("analysis-page").assertIsDisplayed() // 013: migrated
    }
}
