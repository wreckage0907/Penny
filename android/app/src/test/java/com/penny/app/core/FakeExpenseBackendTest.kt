package com.penny.app.core

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * The fake mimics backend/routers/expenseTracker.py (quirks verified in-process
 * in slice 002, LEDGER "Backend quirks"). Wire shapes are asserted as JSON text.
 */
class FakeExpenseBackendTest {

    private fun run(block: suspend FakeExpenseBackend.() -> Unit) = runBlocking { FakeExpenseBackend().block() }

    @Test
    fun `no documents read as empty objects`() = run {
        assertEquals("{}", getExpenses(2026, "May").toString())
        assertEquals("{}", getIncome(2026, "May").toString())
    }

    @Test
    fun `amounts are stored as floats and keep insertion order`() = run {
        addExpense(Expense("b", 5.0, "Food", 2026, "May"))
        addExpense(Expense("a", 2.5, "Transport", 2026, "May"))
        addExpense(Expense("c", 1.0, "Food", 2026, "May"))
        assertEquals(
            """{"2026":{"May":{"categories":{"Food":[{"name":"b","amount":5.0},{"name":"c","amount":1.0}],"Transport":[{"name":"a","amount":2.5}]}}}}""",
            getExpenses(2026, "May").toString(),
        )
    }

    @Test
    fun `filters follow python truthiness`() = run {
        addExpense(Expense("x", 1.0, "Food", 2026, "May"))
        addExpense(Expense("y", 2.0, "Food", 2025, "June"))
        assertEquals(setOf("2026", "2025"), getExpenses(0, "May").keys) // year 0 → unfiltered
        assertEquals(setOf("2026", "2025"), getExpenses(null, "May").keys) // month without year ignored
        assertEquals("""{"2026":{"May":{"categories":{"Food":[{"name":"x","amount":1.0}]}}}}""", getExpenses(2026, "").toString())
        assertEquals("""{"2026":{"June":{}}}""", getExpenses(2026, "June").toString())
        assertEquals("""{"2027":{}}""", getExpenses(2027, null).toString())
    }

    @Test
    fun `income adds, and a missing month reports int 0 (int-income trigger)`() = run {
        addIncome(1000.0, 2026, "May")
        addIncome(250.5, 2026, "May")
        assertEquals("""{"2026":{"May":1250.5}}""", getIncome(2026, "May").toString())
        val missing = getIncome(2026, "June")
        assertEquals("""{"2026":{"June":0}}""", missing.toString())
        // The 005 parity rule: int income → 0.0, double survives.
        assertEquals(0.0, ExpenseAggregation.incomeFor(missing, 2026, "June"), 0.0)
        assertEquals(1250.5, ExpenseAggregation.incomeFor(getIncome(2026, "May"), 2026, "May"), 0.0)
        assertEquals("""{"2027":{}}""", getIncome(2027, null).toString())
        updateIncome(2026, "May", 1.0) // PUT also ADDS
        assertEquals(JsonPrimitive(1251.5), (getIncome(2026, "May")["2026"] as JsonObject)["May"])
    }

    @Test
    fun `update income without a finances document is the wrapped 400`() = run {
        val e = assertThrows(ExpenseApiException::class.java) { runBlocking { updateIncome(2026, "May", 1.0) } }
        assertEquals("Failed to update income: {\"detail\":\"404: Finance document not found.\"}", e.message)
    }

    @Test
    fun `delete removes every same-name expense and cascades empty keys`() = run {
        addExpense(Expense("x", 1.0, "Food", 2026, "May"))
        addExpense(Expense("x", 9.0, "Food", 2026, "May"))
        addExpense(Expense("y", 2.0, "Gifts", 2026, "May"))
        deleteExpense(2026, "May", "Food", "x")
        assertEquals("""{"2026":{"May":{"categories":{"Gifts":[{"name":"y","amount":2.0}]}}}}""", getExpenses(2026, "May").toString())
        deleteExpense(2026, "May", "Gifts", "y")
        assertEquals("{}", getExpenses(null, null).toString())
        // Unknown NAME in an existing category is a lying 200.
        addExpense(Expense("z", 3.0, "Food", 2026, "May"))
        deleteExpense(2026, "May", "Food", "nope")
        assertEquals(1, ((getExpenses(2026, "May")["2026"] as JsonObject)["May"] as JsonObject).size)
    }

    @Test
    fun `delete misses are wrapped 404-as-400 bodies`() = run {
        fun msg(block: suspend () -> Unit) =
            assertThrows(ExpenseApiException::class.java) { runBlocking { block() } }.message
        assertEquals("Failed to delete expense: {\"detail\":\"404: Expense document not found.\"}",
            msg { deleteExpense(2026, "May", "Food", "x") })
        addExpense(Expense("x", 1.0, "Food", 2026, "May"))
        assertEquals("Failed to delete expense: {\"detail\":\"404: Year not found.\"}", msg { deleteExpense(2025, "May", "Food", "x") })
        assertEquals("Failed to delete expense: {\"detail\":\"404: Month not found.\"}", msg { deleteExpense(2026, "June", "Food", "x") })
        assertEquals("Failed to delete expense: {\"detail\":\"404: Category not found.\"}", msg { deleteExpense(2026, "May", "Gifts", "x") })
    }

    @Test
    fun `update matches name AND amount, first hit, may move category`() = run {
        addExpense(Expense("x", 1.0, "Food", 2026, "May"))
        addExpense(Expense("x", 1.0, "Food", 2026, "May"))
        updateExpense("x", 1.0, "Food", "x2", 4.0, "Food", 2026, "May")
        assertEquals("""{"2026":{"May":{"categories":{"Food":[{"name":"x2","amount":4.0},{"name":"x","amount":1.0}]}}}}""",
            getExpenses(2026, "May").toString())
        updateExpense("x", 1.0, "Food", "moved", 1.0, "Gifts", 2026, "May")
        updateExpense("x2", 4.0, "Food", "moved2", 4.0, "Gifts", 2026, "May") // Food empties → removed
        assertEquals("""{"2026":{"May":{"categories":{"Gifts":[{"name":"moved","amount":1.0},{"name":"moved2","amount":4.0}]}}}}""",
            getExpenses(2026, "May").toString())
        val e = assertThrows(ExpenseApiException::class.java) {
            runBlocking { updateExpense("moved", 9.0, "Gifts", "n", 1.0, "Gifts", 2026, "May") }
        }
        assertEquals("Failed to update expense: {\"detail\":\"404: Expense not found.\"}", e.message)
    }
}
