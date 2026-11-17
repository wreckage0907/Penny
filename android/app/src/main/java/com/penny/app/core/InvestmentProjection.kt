package com.penny.app.core

/**
 * Port of `calculateInvestmentValues` from the Flutter app:
 * mobile/lib/Pages/expenseTracker/income_analysis.dart (lines 135-155).
 *
 * Quirks preserved from the Dart original on purpose:
 *  - ANNUAL_RETURNS holds 7 rates, but only the first 4 are used (one per type).
 *  - Each series starts at the savings amount (year 0) and grows for 5 years: 6 points.
 *  - Growth is iterative (last * (1 + rate)), not a closed-form pow, so the
 *    floating-point operation order matches Dart bit-for-bit.
 */
object InvestmentProjection {
    val TYPES = listOf("Fixed Deposits", "Mutual Funds", "Stocks", "Gold")
    val ANNUAL_RETURNS = listOf(0.03, 0.06, 0.10, 0.12, 0.10, 0.075, 0.065)
    const val YEARS = 5

    fun project(savings: Double): List<List<Double>> =
        TYPES.indices.map { i ->
            val values = mutableListOf(savings)
            repeat(YEARS) {
                values.add(values.last() * (1 + ANNUAL_RETURNS[i]))
            }
            values
        }
}
