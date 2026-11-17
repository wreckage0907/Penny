package com.penny.app.core

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test

/** Dashboard dialogs (expense_dashboard.dart:265-735) and their Dart messages. */
class DashboardFlowsTest {

    private val api = FakeExpenseBackend()

    @Test
    fun `username is the claim if present, else prefs`() {
        assertEquals("c", DashboardFlows.username(mapOf("username" to "c"), "p"))
        assertEquals("p", DashboardFlows.username(mapOf("fullName" to "x"), "p"))
        assertEquals(null, DashboardFlows.username(null, null))
    }

    @Test
    fun `add validates, parses outside the try, and reports Dart texts`() = runBlocking {
        assertEquals(DashboardFlows.Outcome.Failed("Please fill all fields"),
            DashboardFlows.addExpense(api, "", "5", "Food", 2026, "May"))
        assertEquals(DashboardFlows.Outcome.Failed("Please fill all fields"),
            DashboardFlows.addExpense(api, "x", "5", null, 2026, "May"))
        // double.parse throws outside any try → nothing visible.
        assertEquals(DashboardFlows.Outcome.Silent, DashboardFlows.addExpense(api, "x", "abc", "Food", 2026, "May"))
        assertEquals(
            DashboardFlows.Outcome.Failed("Failed to add expense: Converting object to an encodable object failed: NaN"),
            DashboardFlows.addExpense(api, "x", "NaN", "Food", 2026, "May"),
        )
        assertEquals(
            DashboardFlows.Outcome.Failed("Failed to add expense: Converting object to an encodable object failed: -Infinity"),
            DashboardFlows.addExpense(api, "x", "-Infinity", "Food", 2026, "May"),
        )
        assertEquals(DashboardFlows.Outcome.Done("Expense added successfully"),
            DashboardFlows.addExpense(api, "x", " 1e2 ", "Food", 2026, "May"))
        assertEquals("""{"2026":{"May":{"categories":{"Food":[{"name":"x","amount":100.0}]}}}}""",
            api.getExpenses(2026, "May").toString())
    }

    @Test
    fun `no username means every call fails the null check inside the try`() = runBlocking {
        assertEquals(
            DashboardFlows.Outcome.Failed("Failed to add expense: Null check operator used on a null value"),
            DashboardFlows.addExpense(null, "x", "5", "Food", 2026, "May"),
        )
    }

    @Test
    fun `edit keeps the last valid amount and prefills Dart toString`() {
        assertEquals(12.5, DashboardFlows.editedAmount("12.5", 3.0), 0.0)
        assertEquals(3.0, DashboardFlows.editedAmount("12.5x", 3.0), 0.0)
        assertEquals("5.0", DashboardFlows.amountText(JsonPrimitive(5.0)))
        assertEquals("5", DashboardFlows.amountText(JsonPrimitive(5)))
        assertEquals("1e+21", DashboardFlows.amountText(JsonPrimitive(1e21)))
        assertEquals("null", DashboardFlows.amountText(JsonNull))
    }

    @Test
    fun `save edit reports the int-amount TypeError and the doubled prefix`() = runBlocking {
        api.addExpense(Expense("x", 1.0, "Food", 2026, "May"))
        assertEquals(
            DashboardFlows.Outcome.Failed("Failed to update expense: type 'int' is not a subtype of type 'double'"),
            DashboardFlows.saveEdit(api, "x", JsonPrimitive(1), "Food", "y", 2.0, 2026, "May"),
        )
        assertEquals(
            DashboardFlows.Outcome.Failed(
                "Failed to update expense: Exception: Failed to update expense: {\"detail\":\"404: Expense not found.\"}",
            ),
            DashboardFlows.saveEdit(api, "x", JsonPrimitive(9.0), "Food", "y", 2.0, 2026, "May"),
        )
        assertEquals(DashboardFlows.Outcome.Done("Expense updated successfully"),
            DashboardFlows.saveEdit(api, "x", JsonPrimitive(1.0), "Food", "y", 2.0, 2026, "May"))
    }

    @Test
    fun `delete success and failure texts`() = runBlocking {
        api.addExpense(Expense("x", 1.0, "Food", 2026, "May"))
        assertEquals(
            DashboardFlows.Outcome.Failed(
                "Failed to delete expense: Exception: Failed to delete expense: {\"detail\":\"404: Category not found.\"}",
            ),
            DashboardFlows.delete(api, 2026, "May", "Gifts", "x"),
        )
        assertEquals(DashboardFlows.Outcome.Done("Expense Deleted Successfully"),
            DashboardFlows.delete(api, 2026, "May", "Food", "x"))
    }

    @Test
    fun `income parses with Dart tryParse`() {
        assertEquals(1000.0, DashboardFlows.parseIncome("1000")!!, 0.0)
        assertEquals(null, DashboardFlows.parseIncome("1,000"))
        assertEquals(true, DashboardFlows.parseIncome("NaN")!!.isNaN())
    }
}
