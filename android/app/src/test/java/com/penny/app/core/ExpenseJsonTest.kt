package com.penny.app.core

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Request-body goldens: exact compact JSON in Dart map insertion order
 * (expense_service.dart:20-26,55-59,75-77,116-123). Doubles must serialise
 * shortest-round-trip with '.0' on wholes (Q12: PUT /expenses matches stored
 * amounts with Python float ==, so lossy formatting breaks updates).
 * Spelling note: inside [1e-3, 1e7) kotlinx matches Dart byte-for-byte;
 * outside it Java uses 1.0E7-style where Dart writes 10000000.0 / 1e-7.
 * Same IEEE value, Pydantic parses both identically — accepted divergence.
 */
class ExpenseJsonTest {

    @Test
    fun `expense body matches Dart toJson key order and double spelling`() {
        assertEquals(
            """{"name":"Lunch","amount":12.5,"category":"Food","year":2026,"month":"November"}""",
            Json.encodeToString(Expense.serializer(), Expense("Lunch", 12.5, "Food", 2026, "November")),
        )
    }

    @Test
    fun `whole doubles keep the decimal point`() {
        assertEquals(
            """{"name":"Cola","amount":100.0,"category":"Food","year":2026,"month":"November"}""",
            Json.encodeToString(Expense.serializer(), Expense("Cola", 100.0, "Food", 2026, "November")),
        )
    }

    @Test
    fun `addIncome body order is amount, year, month`() {
        assertEquals(
            """{"amount":1000.0,"year":2026,"month":"November"}""",
            Json.encodeToString(AddIncomeBody.serializer(), AddIncomeBody(1000.0, 2026, "November")),
        )
    }

    @Test
    fun `updateIncome body is amount only`() {
        assertEquals(
            """{"amount":500.0}""",
            Json.encodeToString(UpdateIncomeBody.serializer(), UpdateIncomeBody(500.0)),
        )
    }

    @Test
    fun `updateExpense body uses snake_case keys in Dart order`() {
        assertEquals(
            """{"old_name":"Lunch","old_amount":12.5,"old_category":"Food",""" +
                """"new_name":"Brunch","new_amount":15.0,"new_category":"Food"}""",
            Json.encodeToString(
                UpdateExpenseBody.serializer(),
                UpdateExpenseBody("Lunch", 12.5, "Food", "Brunch", 15.0, "Food"),
            ),
        )
    }

    @Test
    fun `doubles serialise shortest-round-trip, not fixed decimals`() {
        assertEquals(
            """{"amount":0.3333333333333333,"year":2026,"month":"November"}""",
            Json.encodeToString(AddIncomeBody.serializer(), AddIncomeBody(1.0 / 3.0, 2026, "November")),
        )
        assertEquals(
            """{"amount":0.1,"year":2026,"month":"November"}""",
            Json.encodeToString(AddIncomeBody.serializer(), AddIncomeBody(0.1, 2026, "November")),
        )
    }
}