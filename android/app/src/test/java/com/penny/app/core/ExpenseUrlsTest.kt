package com.penny.app.core

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Characterisation tests for the URL construction in Flutter
 * `expense_service.dart` (mobile/lib/Pages/expenseTracker/expense_service.dart).
 * Goldens are the exact strings Dart builds, quirks included:
 *  - getExpenses drops `month` when `year` is null (:133-138, string concat).
 *  - getIncome with no filters keeps a trailing '?' (:91,
 *    Uri.replace(queryParameters: {}) with an empty map).
 *  - update/delete queries are raw interpolation, unencoded (:114, :156):
 *    an '&' inside a name injects extra query params, exactly as Dart.
 * Encoding of spaces/unicode happens at parse time (Dart Uri.parse /
 * OkHttp HttpUrl.parse alike), so builders stay raw strings.
 */
class ExpenseUrlsTest {

    private val base = "https://penny.example.com"
    private val user = "alice"

    @Test
    fun `addExpense posts to expenses path with no query`() {
        assertEquals("$base/expenses/$user", ExpenseUrls.addExpense(base, user))
    }

    @Test
    fun `addIncome posts to income path with no query`() {
        assertEquals("$base/income/$user", ExpenseUrls.addIncome(base, user))
    }

    @Test
    fun `updateIncome puts year and month into the path`() {
        assertEquals(
            "$base/income/$user/2026/May",
            ExpenseUrls.updateIncome(base, user, 2026, "May"),
        )
    }

    @Test
    fun `updateExpense puts year and month into the query`() {
        assertEquals(
            "$base/expenses/$user?year=2026&month=May",
            ExpenseUrls.updateExpense(base, user, 2026, "May"),
        )
    }

    @Test
    fun `getIncome with no filters keeps the trailing question mark`() {
        assertEquals("$base/income/$user?", ExpenseUrls.income(base, user, null, null))
    }

    @Test
    fun `getIncome includes only the non-null filters, year first`() {
        assertEquals("$base/income/$user?year=2026", ExpenseUrls.income(base, user, 2026, null))
        assertEquals("$base/income/$user?month=May", ExpenseUrls.income(base, user, null, "May"))
        assertEquals(
            "$base/income/$user?year=2026&month=May",
            ExpenseUrls.income(base, user, 2026, "May"),
        )
    }

    @Test
    fun `getIncome query values use Dart encodeQueryComponent semantics`() {
        // Executed against Dart 3.14: Uri.replace(queryParameters:) encodes
        // values with encodeQueryComponent — unlike the raw :114/:156 sites.
        assertEquals("$base/income/$user?month=Nov+5", ExpenseUrls.income(base, user, null, "Nov 5"))
        assertEquals("$base/income/$user?month=a%26b%3Dc", ExpenseUrls.income(base, user, null, "a&b=c"))
        assertEquals("$base/income/$user?month=a%2Bb", ExpenseUrls.income(base, user, null, "a+b"))
        assertEquals("$base/income/$user?month=a%23b", ExpenseUrls.income(base, user, null, "a#b"))
        assertEquals("$base/income/$user?month=100%25", ExpenseUrls.income(base, user, null, "100%"))
        assertEquals("$base/income/$user?month=%2A~", ExpenseUrls.income(base, user, null, "*~"))
        // Empty values emit the key without '=' (Dart _makeQuery).
        assertEquals("$base/income/$user?year=2026&month", ExpenseUrls.income(base, user, 2026, ""))
    }

    @Test
    fun `getExpenses with no filters has no query at all`() {
        assertEquals("$base/expenses/$user", ExpenseUrls.expenses(base, user, null, null))
    }

    @Test
    fun `getExpenses drops month when year is null, as in Dart`() {
        // expense_service.dart:133-138 — month is only appended inside the
        // year != null branch. A month-only filter silently fetches everything.
        assertEquals("$base/expenses/$user", ExpenseUrls.expenses(base, user, null, "May"))
    }

    @Test
    fun `getExpenses year-only and year-plus-month`() {
        assertEquals("$base/expenses/$user?year=2026", ExpenseUrls.expenses(base, user, 2026, null))
        assertEquals(
            "$base/expenses/$user?year=2026&month=May",
            ExpenseUrls.expenses(base, user, 2026, "May"),
        )
    }

    @Test
    fun `deleteExpense interpolates raw, so ampersands inject params`() {
        // expense_service.dart:156 — no encoding; Dart Uri.parse then splits
        // on the injected '&'. The builder must not "helpfully" encode.
        assertEquals(
            "$base/expenses/$user?year=2026&month=May&category=Food&name=a&b=c",
            ExpenseUrls.deleteExpense(base, user, 2026, "May", "Food", "a&b=c"),
        )
    }

    @Test
    fun `deleteExpense keeps spaces raw for the parse stage`() {
        assertEquals(
            "$base/expenses/$user?year=2026&month=May&category=Fast Food&name=Cola Zero",
            ExpenseUrls.deleteExpense(base, user, 2026, "May", "Fast Food", "Cola Zero"),
        )
    }
}