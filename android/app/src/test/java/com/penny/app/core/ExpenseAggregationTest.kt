package com.penny.app.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Characterisation of the expense dashboard's data logic
 * (expense_dashboard.dart:127-189, :1074, :1084). Every golden table below
 * was EMITTED as Kotlin literals by executing the verbatim Dart methods on
 * Dart 3.14 against jsonDecode'd fixtures (harness: dart005/dash.dart).
 * Doubles are compared bit-for-bit.
 */
class ExpenseAggregationTest {

    private data class LoadGolden(
        val label: String,
        val json: String,
        val total: Double,
        val wiped: Boolean,
        val categories: List<String>,
        val values: List<Double>,
    )

    private data class IncomeGolden(val label: String, val json: String, val income: Double)

    private fun obj(json: String): JsonObject = Json.parseToJsonElement(json).jsonObject

    private fun bits(d: Double) = java.lang.Double.doubleToRawLongBits(d)

    private val loadGoldens = listOf(
        LoadGolden("plan golden: per-category subtotal then accumulate", "{\"2026\":{\"November\":{\"categories\":{\"Food\":[{\"name\":\"a\",\"amount\":0.1},{\"name\":\"b\",\"amount\":0.1}],\"Transport\":[{\"name\":\"c\",\"amount\":0.1},{\"name\":\"d\",\"amount\":0.6}]}}}}", total = 0.8999999999999999, wiped = false,
            categories = listOf("Food", "Transport"), values = listOf(0.2, 0.7)),
        LoadGolden("ints and doubles mix (as num)", "{\"2026\":{\"November\":{\"categories\":{\"Food\":[{\"name\":\"a\",\"amount\":5},{\"name\":\"b\",\"amount\":2.5}]}}}}", total = 7.5, wiped = false,
            categories = listOf("Food"), values = listOf(7.5)),
        LoadGolden("empty categories object", "{\"2026\":{\"November\":{\"categories\":{}}}}", total = 0.0, wiped = false,
            categories = listOf(), values = listOf()),
        LoadGolden("category with empty list", "{\"2026\":{\"November\":{\"categories\":{\"Food\":[],\"Gifts\":[{\"name\":\"g\",\"amount\":3}]}}}}", total = 3.0, wiped = false,
            categories = listOf("Food", "Gifts"), values = listOf(0.0, 3.0)),
        LoadGolden("year absent", "{\"2025\":{\"November\":{\"categories\":{\"Food\":[{\"amount\":1}]}}}}", total = 0.0, wiped = false,
            categories = listOf(), values = listOf()),
        LoadGolden("month absent", "{\"2026\":{\"October\":{\"categories\":{\"Food\":[{\"amount\":1}]}}}}", total = 0.0, wiped = false,
            categories = listOf(), values = listOf()),
        LoadGolden("categories key absent", "{\"2026\":{\"November\":{\"other\":1}}}", total = 0.0, wiped = false,
            categories = listOf(), values = listOf()),
        LoadGolden("top-level empty", "{}", total = 0.0, wiped = false,
            categories = listOf(), values = listOf()),
        LoadGolden("year value is int", "{\"2026\":5}", total = 0.0, wiped = true,
            categories = listOf(), values = listOf()),
        LoadGolden("year value is list", "{\"2026\":[]}", total = 0.0, wiped = true,
            categories = listOf(), values = listOf()),
        LoadGolden("year value is string", "{\"2026\":\"x\"}", total = 0.0, wiped = true,
            categories = listOf(), values = listOf()),
        LoadGolden("year value is null", "{\"2026\":null}", total = 0.0, wiped = true,
            categories = listOf(), values = listOf()),
        LoadGolden("month value is list", "{\"2026\":{\"November\":[1]}}", total = 0.0, wiped = true,
            categories = listOf(), values = listOf()),
        LoadGolden("month value is null", "{\"2026\":{\"November\":null}}", total = 0.0, wiped = true,
            categories = listOf(), values = listOf()),
        LoadGolden("categories is list", "{\"2026\":{\"November\":{\"categories\":[]}}}", total = 0.0, wiped = true,
            categories = listOf(), values = listOf()),
        LoadGolden("categories is null", "{\"2026\":{\"November\":{\"categories\":null}}}", total = 0.0, wiped = true,
            categories = listOf(), values = listOf()),
        LoadGolden("category value not list", "{\"2026\":{\"November\":{\"categories\":{\"Food\":{\"a\":1}}}}}", total = 0.0, wiped = true,
            categories = listOf(), values = listOf()),
        LoadGolden("element not object", "{\"2026\":{\"November\":{\"categories\":{\"Food\":[5]}}}}", total = 0.0, wiped = true,
            categories = listOf(), values = listOf()),
        LoadGolden("element is list", "{\"2026\":{\"November\":{\"categories\":{\"Food\":[[1]]}}}}", total = 0.0, wiped = true,
            categories = listOf(), values = listOf()),
        LoadGolden("amount is string", "{\"2026\":{\"November\":{\"categories\":{\"Food\":[{\"name\":\"a\",\"amount\":\"5\"}]}}}}", total = 0.0, wiped = true,
            categories = listOf(), values = listOf()),
        LoadGolden("amount is null", "{\"2026\":{\"November\":{\"categories\":{\"Food\":[{\"name\":\"a\",\"amount\":null}]}}}}", total = 0.0, wiped = true,
            categories = listOf(), values = listOf()),
        LoadGolden("amount missing", "{\"2026\":{\"November\":{\"categories\":{\"Food\":[{\"name\":\"a\"}]}}}}", total = 0.0, wiped = true,
            categories = listOf(), values = listOf()),
        LoadGolden("amount is bool", "{\"2026\":{\"November\":{\"categories\":{\"Food\":[{\"name\":\"a\",\"amount\":true}]}}}}", total = 0.0, wiped = true,
            categories = listOf(), values = listOf()),
        LoadGolden("partial: second category fails after first summed", "{\"2026\":{\"November\":{\"categories\":{\"Food\":[{\"amount\":1.5},{\"amount\":2.25}],\"Bad\":[{\"amount\":null}],\"Late\":[{\"amount\":100}]}}}}", total = 3.75, wiped = true,
            categories = listOf(), values = listOf()),
        LoadGolden("partial: failure mid-list drops whole category", "{\"2026\":{\"November\":{\"categories\":{\"Food\":[{\"amount\":1}],\"Mid\":[{\"amount\":7},{\"amount\":\"x\"}]}}}}", total = 1.0, wiped = true,
            categories = listOf(), values = listOf()),
        LoadGolden("big int beyond 2^53 rounds on toDouble", "{\"2026\":{\"November\":{\"categories\":{\"Food\":[{\"amount\":9007199254740993}]}}}}", total = 9007199254740992.0, wiped = false,
            categories = listOf("Food"), values = listOf(9007199254740992.0)),
        LoadGolden("exponent and negative amounts", "{\"2026\":{\"November\":{\"categories\":{\"Food\":[{\"amount\":1e2},{\"amount\":-0.5},{\"amount\":-1E-1}]}}}}", total = 99.4, wiped = false,
            categories = listOf("Food"), values = listOf(99.4)),
        LoadGolden("seven categories wrap the six colours", "{\"2026\":{\"November\":{\"categories\":{\"Utility Bills\":[{\"amount\":1}],\"Food\":[{\"amount\":2}],\"Transport\":[{\"amount\":3}],\"Shopping\":[{\"amount\":4}],\"Entertainment\":[{\"amount\":5}],\"Holidays\":[{\"amount\":6}],\"Gifts\":[{\"amount\":7}]}}}}", total = 28.0, wiped = false,
            categories = listOf("Utility Bills", "Food", "Transport", "Shopping", "Entertainment", "Holidays", "Gifts"), values = listOf(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0)),
        LoadGolden("float order within a category", "{\"2026\":{\"November\":{\"categories\":{\"Food\":[{\"amount\":1e16},{\"amount\":1},{\"amount\":1}]}}}}", total = 10000000000000000.0, wiped = false,
            categories = listOf("Food"), values = listOf(10000000000000000.0)),
        LoadGolden("float order across categories", "{\"2026\":{\"November\":{\"categories\":{\"A\":[{\"amount\":1e16}],\"B\":[{\"amount\":1}],\"C\":[{\"amount\":1}]}}}}", total = 10000000000000000.0, wiped = false,
            categories = listOf("A", "B", "C"), values = listOf(10000000000000000.0, 1.0, 1.0)),
        LoadGolden("unicode category names keep wire order", "{\"2026\":{\"November\":{\"categories\":{\"Zeta\":[{\"amount\":1}],\"Caf\u00E9 \u20B9\":[{\"amount\":2}],\"Alpha\":[{\"amount\":3}]}}}}", total = 6.0, wiped = false,
            categories = listOf("Zeta", "Caf\u00E9 \u20B9", "Alpha"), values = listOf(1.0, 2.0, 3.0)),
    )

    private val incomeGoldens = listOf(
        IncomeGolden("double value", "{\"2026\":{\"November\":5000.0}}", 5000.0),
        IncomeGolden("int value throws -> 0.0 (int-income quirk)", "{\"2026\":{\"November\":5000}}", 0.0),
        IncomeGolden("int zero (backend no-entry) -> 0.0", "{\"2026\":{\"November\":0}}", 0.0),
        IncomeGolden("fractional", "{\"2026\":{\"November\":1234.56}}", 1234.56),
        IncomeGolden("exponent literal is double", "{\"2026\":{\"November\":5e3}}", 5000.0),
        IncomeGolden("huge int literal is double in Dart", "{\"2026\":{\"November\":99999999999999999999}}", 100000000000000000000.0),
        IncomeGolden("year absent", "{}", 0.0),
        IncomeGolden("month absent", "{\"2026\":{\"October\":10.5}}", 0.0),
        IncomeGolden("month null", "{\"2026\":{\"November\":null}}", 0.0),
        IncomeGolden("year null", "{\"2026\":null}", 0.0),
        IncomeGolden("year is list", "{\"2026\":[1]}", 0.0),
        IncomeGolden("year is int", "{\"2026\":7}", 0.0),
        IncomeGolden("value is string", "{\"2026\":{\"November\":\"5000.0\"}}", 0.0),
        IncomeGolden("value is object", "{\"2026\":{\"November\":{\"a\":1.0}}}", 0.0),
        IncomeGolden("negative double", "{\"2026\":{\"November\":-12.5}}", -12.5),
        IncomeGolden("negative zero", "{\"2026\":{\"November\":-0.0}}", -0.0),
    )

    private val flexGoldens = listOf(
        0.0 to 0L,
        -0.0 to 0L,
        0.004 to 0L,
        0.005 to 1L,
        0.015 to 2L,
        0.125 to 13L,
        -0.125 to -13L,
        0.0049999999999999 to 0L,
        1.005 to 100L,
        2.675 to 268L,
        12.345 to 1235L,
        -12.345 to -1235L,
        0.49999999999999994 to 50L,
        5000.0 to 500000L,
        92233720368547760.0 to 9223372036854775807L,
        92233720368547760.0 to 9223372036854775807L,
        100000000000000000.0 to 9223372036854775807L,
        10000000000000000000.0 to 9223372036854775807L,
        -10000000000000000000.0 to Long.MIN_VALUE,
        Double.NaN to null,
        Double.POSITIVE_INFINITY to null,
        Double.NEGATIVE_INFINITY to null,
        0.30000000000000004 to 30L,
        1e-320 to 0L,
    )

    @Test
    fun `expense load matches executed Dart on every fixture`() {
        val failures = loadGoldens.mapNotNull { g ->
            val payload = obj(g.json)
            val state = ExpenseAggregation.onExpensesFetched(payload, 2026, "November")
            val categories = ExpenseAggregation.categoriesFor(state.expenseData, 2026, "November")
            val totals = ExpenseAggregation.categoryTotals(categories)
            val actualWiped = state.expenseData.isEmpty() && payload.isNotEmpty()
            when {
                bits(state.totalAmount) != bits(g.total) -> "${g.label}: total ${state.totalAmount} != ${g.total}"
                actualWiped != g.wiped -> "${g.label}: wiped $actualWiped != ${g.wiped}"
                totals.map { it.first } != g.categories -> "${g.label}: categories ${totals.map { it.first }}"
                totals.map { bits(it.second) } != g.values.map { bits(it) } ->
                    "${g.label}: values ${totals.map { it.second }} != ${g.values}"
                else -> null
            }
        }
        assertEquals(failures.joinToString("\n"), 0, failures.size)
    }

    @Test
    fun `subtotal-then-accumulate order is locked against a flat sum`() {
        val g = loadGoldens.first { it.label.startsWith("plan golden") }
        assertEquals(0.8999999999999999, g.total, 0.0)
        // The flat left-to-right sum differs (Dart-executed: 0.1+0.1+0.1+0.6 == 0.9).
        assertEquals(0.9, 0.1 + 0.1 + 0.1 + 0.6, 0.0)
    }

    @Test
    fun `fetch failure wipes data but keeps the previous total`() {
        // Dart: _loadExpenseData catch sets expenseData = {} and never touches totalAmount.
        val state = ExpenseAggregation.onExpensesFetchFailed(previousTotal = 42.0)
        assertEquals(42.0, state.totalAmount, 0.0)
        assertEquals(0, state.expenseData.size)
    }

    @Test
    fun `malformed shapes throw from categoriesFor and categoryTotals like Dart`() {
        assertThrows(ExpenseAggregation.DashboardShapeException::class.java) {
            ExpenseAggregation.categoriesFor(obj("""{"2026":null}"""), 2026, "November")
        }
        assertThrows(ExpenseAggregation.DashboardShapeException::class.java) {
            ExpenseAggregation.categoryTotals(obj("""{"Food":[{"amount":"5"}]}"""))
        }
    }

    @Test
    fun `income matches executed Dart, int income zeroes`() {
        val failures = incomeGoldens.mapNotNull { g ->
            val actual = ExpenseAggregation.incomeFor(obj(g.json), 2026, "November")
            if (bits(actual) == bits(g.income)) null else "${g.label}: $actual != ${g.income}"
        }
        assertEquals(failures.joinToString("\n"), 0, failures.size)
    }

    @Test
    fun `bar flex rounds half away from zero, clamps, throws on non-finite`() {
        val failures = flexGoldens.mapNotNull { (amount, expected) ->
            val actual = try {
                ExpenseAggregation.barFlex(amount)
            } catch (e: UnsupportedOperationException) {
                if (e.message == "Unsupported operation: Infinity or NaN toInt") null else -999L
            }
            if (actual == expected) null else "$amount: $actual != $expected"
        }
        assertEquals(failures.joinToString("\n"), 0, failures.size)
    }

    @Test
    fun `display constants and strings`() {
        assertEquals("November", ExpenseDashboardText.initialMonth(11))
        assertEquals("January", ExpenseDashboardText.initialMonth(1))
        assertEquals((2021..2031).toList(), ExpenseDashboardText.yearRange(2026))
        assertEquals("\$1.00", ExpenseDashboardText.amountLabel(1.005))
        assertEquals("\$-0.00", ExpenseDashboardText.amountLabel(-0.0))
        assertEquals("November, 2026", ExpenseDashboardText.categorySubtitle("November", 2026))
        assertEquals(0xFF009688, ExpenseDashboardText.sliceColor(0))
        assertEquals(0xFF607D8B, ExpenseDashboardText.sliceColor(5))
        assertEquals(0xFF009688, ExpenseDashboardText.sliceColor(6))
        assertEquals(ExpenseDashboardText.CategoryIcon.UTENSILS, ExpenseDashboardText.iconFor("Food"))
        assertEquals(ExpenseDashboardText.CategoryIcon.TAGS, ExpenseDashboardText.iconFor("food"))
        assertEquals(ExpenseDashboardText.CategoryIcon.TAGS, ExpenseDashboardText.iconFor("Rent"))
    }
}
