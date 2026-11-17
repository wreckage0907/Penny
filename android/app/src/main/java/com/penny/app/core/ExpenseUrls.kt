package com.penny.app.core

/**
 * URL construction ported verbatim from Flutter `expense_service.dart`
 * (mobile/lib/Pages/expenseTracker/expense_service.dart). Pure string logic,
 * no Android or HTTP types, so the quirks are JVM-testable:
 *  - `expenses` drops `month` when `year` is null (:133-138).
 *  - `income` with no filters keeps a trailing '?' (:91, Dart
 *    `Uri.replace(queryParameters: {})` with an empty map).
 *  - update/delete queries are raw interpolation (:114, :156); encoding and
 *    '&' splitting happen at parse time, exactly like Dart `Uri.parse`.
 */
object ExpenseUrls {

    fun addExpense(baseUrl: String, userId: String): String =
        "$baseUrl/expenses/$userId"

    fun addIncome(baseUrl: String, userId: String): String =
        "$baseUrl/income/$userId"

    fun updateIncome(baseUrl: String, userId: String, year: Int, month: String): String =
        "$baseUrl/income/$userId/$year/$month"

    fun updateExpense(baseUrl: String, userId: String, year: Int, month: String): String =
        "$baseUrl/expenses/$userId?year=$year&month=$month"

    fun income(baseUrl: String, userId: String, year: Int?, month: String?): String {
        val params = buildList {
            if (year != null) add("year=${encodeQueryComponent(year.toString())}")
            if (month != null) {
                // Dart's _makeQuery omits '=' for empty values.
                add(if (month.isEmpty()) "month" else "month=${encodeQueryComponent(month)}")
            }
        }
        // Dart replaces the query with an empty map when both are null,
        // which leaves a trailing '?' on the wire.
        return "$baseUrl/income/$userId?" + params.joinToString("&")
    }

    /**
     * Dart `Uri.encodeQueryComponent` (used by Uri.replace(queryParameters:),
     * expense_service.dart:91) — NOT the raw-interpolation path of :114/:156.
     * Verified against Dart 3.14: space→'+', '&'→%26, '#'→%23, '+'→%2B,
     * '%'→%25, '*'→%2A, '~' stays raw. Java's URLEncoder differs only on
     * '*' and '~', corrected here.
     */
    private fun encodeQueryComponent(value: String): String =
        java.net.URLEncoder.encode(value, "UTF-8")
            .replace("*", "%2A")
            .replace("%7E", "~")

    fun expenses(baseUrl: String, userId: String, year: Int?, month: String?): String {
        var url = "$baseUrl/expenses/$userId"
        if (year != null) {
            url += "?year=$year"
            if (month != null) {
                url += "&month=$month"
            }
        }
        return url
    }

    fun deleteExpense(
        baseUrl: String,
        userId: String,
        year: Int,
        month: String,
        category: String,
        name: String,
    ): String =
        "$baseUrl/expenses/$userId?year=$year&month=$month&category=$category&name=$name"
}