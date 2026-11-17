package com.penny.app.core

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Pure data logic of the expense dashboard (expense_dashboard.dart:127-189,
 * :1074, :1084, :1137), operating on the raw JSON that [ExpenseService]
 * returns (wire key order preserved — it drives legend, colours and lists).
 *
 * Dart reaches into `dynamic` JSON; a wrong shape throws a TypeError or
 * NoSuchMethodError. Those are always caught by the load methods (never
 * user-visible), so [DashboardShapeException] stands in for both — what
 * matters for parity is WHEN it throws and what state survives.
 * All goldens were produced by executing the verbatim Dart on Dart 3.14.
 */
object ExpenseAggregation {

    class DashboardShapeException(message: String) : RuntimeException(message)

    /**
     * getCategoriesForSelectedMonthAndYear (:171-179). Missing year, month
     * or `categories` key → empty. A present value of the wrong kind
     * (null, list, number, string) throws, as Dart's dynamic calls do.
     */
    fun categoriesFor(expenseData: JsonObject, year: Int, month: String): JsonObject {
        val yearKey = year.toString()
        if (!expenseData.containsKey(yearKey)) return EMPTY
        val yearObj = expenseData[yearKey].asObject("year")
        if (!yearObj.containsKey(month)) return EMPTY
        val monthObj = yearObj[month].asObject("month")
        if (!monthObj.containsKey("categories")) return EMPTY
        return monthObj["categories"].asObject("categories")
    }

    /**
     * Per-category totals (getSections :184-189, list tile :1137):
     * `fold(0.0, sum + (amount as num).toDouble())` in wire order.
     */
    fun categoryTotals(categories: JsonObject): List<Pair<String, Double>> =
        categories.map { (category, expenses) -> category to categoryTotal(expenses) }

    private fun categoryTotal(expenses: JsonElement?): Double {
        val list = expenses as? JsonArray ?: throw DashboardShapeException("expenses is not a List")
        var sum = 0.0
        for (expense in list) sum += amountOf(expense)
        return sum
    }

    /** `(expense['amount'] as num).toDouble()` — int and double both accepted. */
    private fun amountOf(expense: JsonElement): Double {
        val obj = expense as? JsonObject ?: throw DashboardShapeException("expense is not a Map")
        val amount = obj["amount"]
        if (amount !is JsonPrimitive || amount is JsonNull || amount.isString ||
            amount.content == "true" || amount.content == "false"
        ) {
            throw DashboardShapeException("amount is not a num")
        }
        // Dart int.toDouble and double literals both round-to-nearest, as does
        // parsing the literal directly (verified: 9007199254740993 → …992.0).
        return amount.content.toDouble()
    }

    /** Dashboard state written by `_loadExpenseData` (:146-161). */
    data class ExpensesState(val expenseData: JsonObject, val totalAmount: Double)

    /**
     * Successful fetch: `_calculateTotalAmount` (:163-169) resets the total
     * to 0, then adds each category's subtotal (subtotal FIRST, then
     * accumulate — `0.1+0.1 | 0.1+0.6` gives 0.8999999999999999, not the flat
     * sum's 0.9). If any shape check throws, the data is
     * wiped to {} ("No data") but the PARTIAL total accumulated so far stays.
     */
    fun onExpensesFetched(payload: JsonObject, year: Int, month: String): ExpensesState {
        var total = 0.0
        return try {
            for ((_, expenses) in categoriesFor(payload, year, month)) {
                total += categoryTotal(expenses)
            }
            ExpensesState(payload, total)
        } catch (e: DashboardShapeException) {
            ExpensesState(EMPTY, total)
        }
    }

    /** Failed fetch: data wiped, total NOT reset — the previous month's total stays. */
    fun onExpensesFetchFailed(previousTotal: Double): ExpensesState =
        ExpensesState(EMPTY, previousTotal)

    /**
     * `_loadIncomeData` (:127-144): `incomeData[year]?[month] ?? 0.0` assigned
     * to a `double` field. Only a JSON value Dart decodes as `double` survives
     * (decimal point, exponent, or beyond int64); an int (incl. the backend's
     * `0` for months without income), string, object, list or bad year shape
     * throws inside the try → 0.0. This is the int-income quirk (LEDGER,
     * PRESERVE) — here only income is zeroed, the pies stay.
     */
    fun incomeFor(incomeData: JsonObject, year: Int, month: String): Double {
        val yearObj = incomeData[year.toString()] as? JsonObject ?: return 0.0
        val value = yearObj[month]
        if (value !is JsonPrimitive || value is JsonNull || dartKind(value) != "double") return 0.0
        return value.content.toDouble()
    }

    /**
     * Income/expense bar weights (:1074, :1084): `(amount * 100).round()`.
     * Dart rounds half AWAY from zero (-12.5 → -13; Java Math.round gives
     * -12), clamps to the int64 range, and throws on NaN/Infinity.
     */
    fun barFlex(amount: Double): Long {
        val x = amount * 100
        if (x.isNaN() || x.isInfinite()) {
            throw UnsupportedOperationException("Unsupported operation: Infinity or NaN toInt")
        }
        if (x <= -9.223372036854775808E18) return Long.MIN_VALUE
        val magnitude = Math.round(Math.abs(x)) // half up on |x| == half away from zero
        return if (x < 0) -magnitude else magnitude
    }

    private fun JsonElement?.asObject(what: String): JsonObject =
        this as? JsonObject ?: throw DashboardShapeException("$what is not a Map")

    private val EMPTY = JsonObject(emptyMap())
}

/** Display constants and strings of the expense dashboard. */
object ExpenseDashboardText {

    /** monthNames (:75-88); the screen selects by English name. */
    val MONTH_NAMES = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December",
    )

    /** initState (:121): the "11"-style field initialiser is dead code. */
    fun initialMonth(month1Based: Int): String = MONTH_NAMES[month1Based - 1]

    /** Year dropdown (:748): currentYear-5 .. currentYear+5. */
    fun yearRange(currentYear: Int): List<Int> = List(11) { currentYear - 5 + it }

    /** `"\$${x.toStringAsFixed(2)}"` (:250, :911, :1019, :1050, :1174). */
    fun amountLabel(amount: Double): String = "\$" + Money.format(amount)

    /** Category tile subtitle (:1166). */
    fun categorySubtitle(month: String, year: Int): String = "$month, $year"

    /** Pie/legend palette (:31-38): Material teal, yellow, deepOrange, pink, blueAccent, blueGrey. */
    val SLICE_COLORS: List<Long> = listOf(
        0xFF009688, 0xFFFFEB3B, 0xFFFF5722, 0xFFE91E63, 0xFF448AFF, 0xFF607D8B,
    )

    /** `colors[i % colors.length]` (:194, :949-950); legend uses keys.indexOf == wire index. */
    fun sliceColor(index: Int): Long = SLICE_COLORS[index % SLICE_COLORS.size]

    enum class CategoryIcon {
        MONEY_BILL_WAVE, UTENSILS, CAR, CART_SHOPPING, FILM, CHAMPAGNE_GLASSES, GIFTS, TAGS,
    }

    private val ICONS = mapOf(
        "Utility Bills" to CategoryIcon.MONEY_BILL_WAVE,
        "Food" to CategoryIcon.UTENSILS,
        "Transport" to CategoryIcon.CAR,
        "Shopping" to CategoryIcon.CART_SHOPPING,
        "Entertainment" to CategoryIcon.FILM,
        "Holidays" to CategoryIcon.CHAMPAGNE_GLASSES,
        "Gifts" to CategoryIcon.GIFTS,
        "Miscellaneous" to CategoryIcon.TAGS,
    )

    /** icons map (:40-73) with the tags fallback (:1152-1155); exact, case-sensitive keys. */
    fun iconFor(category: String): CategoryIcon = ICONS[category] ?: CategoryIcon.TAGS
}
