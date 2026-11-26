package com.penny.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * QuickLog (frame 06): amount and category are both required, failures
 * name the offending field, and payments are one of UPI/Card/Cash.
 */
class QuickLogTest {

    @Test
    fun `a valid entry parses with a trimmed merchant`() {
        val r = QuickLog.parse("350", BudgetCategories.FOOD, "  BigBasket ", 25, "UPI")
        assertEquals(QuickLog.Result.Valid(350.0, BudgetCategories.FOOD, "BigBasket", 25, "UPI"), r)
        assertEquals(QuickLog.Result.Valid(12.5, BudgetCategories.RENT, "", 3, "Cash"), QuickLog.parse("12.5 ", BudgetCategories.RENT, "  ", 3, "Cash"))
    }

    @Test
    fun `amount failures are split into required and invalid`() {
        assertEquals(QuickLog.Result.AmountRequired, QuickLog.parse("", BudgetCategories.FOOD, "x", 1))
        assertEquals(QuickLog.Result.AmountRequired, QuickLog.parse("   ", BudgetCategories.FOOD, "x", 1))
        assertEquals(QuickLog.Result.AmountInvalid, QuickLog.parse("abc", BudgetCategories.FOOD, "x", 1))
        assertEquals(QuickLog.Result.AmountInvalid, QuickLog.parse("0", BudgetCategories.FOOD, "x", 1))
        assertEquals(QuickLog.Result.AmountInvalid, QuickLog.parse("-5", BudgetCategories.FOOD, "x", 1))
        assertEquals(QuickLog.Result.AmountInvalid, QuickLog.parse("1,000", BudgetCategories.FOOD, "x", 1))
    }

    @Test
    fun `the category must be one of the five budget categories`() {
        assertEquals(QuickLog.Result.CategoryRequired, QuickLog.parse("350", null, "x", 1))
        assertEquals(QuickLog.Result.CategoryRequired, QuickLog.parse("350", "Misc", "x", 1))
        assertEquals(QuickLog.Result.CategoryRequired, QuickLog.parse("350", "Food", "x", 1)) // v1 name, not v2.1
    }

    @Test
    fun `an unknown payment reads as a form bug, not a user error`() {
        // Documented quirk: the parse contract collapses unknown payments into
        // the category branch so the UI never shows a payment error.
        assertEquals(QuickLog.Result.CategoryRequired, QuickLog.parse("350", BudgetCategories.FOOD, "x", 1, "Bitcoin"))
    }

    @Test
    fun `income parsing shares the amount rules`() {
        assertEquals(QuickLog.IncomeResult.Valid(85_000.0), QuickLog.parseIncome("85000"))
        assertEquals(QuickLog.IncomeResult.Valid(12.5), QuickLog.parseIncome(" 12.5 "))
        assertEquals(QuickLog.IncomeResult.AmountRequired, QuickLog.parseIncome(""))
        assertEquals(QuickLog.IncomeResult.AmountInvalid, QuickLog.parseIncome("1,000"))
        assertEquals(QuickLog.IncomeResult.AmountInvalid, QuickLog.parseIncome("0"))
    }
}
