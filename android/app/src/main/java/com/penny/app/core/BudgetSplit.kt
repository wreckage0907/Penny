package com.penny.app.core

/**
 * Port of the 50/30/20 split from the Flutter app:
 * mobile/lib/Pages/expenseTracker/income_analysis.dart, `_calculateSplit` (lines 120-124).
 *
 * Pure arithmetic, no Android types, so it runs in fast JVM tests.
 * Behaviour is preserved exactly, including the lack of guards: zero and
 * negative incomes pass straight through, as they do in Dart.
 */
data class BudgetSplit(
    val essentials: Double,
    val wants: Double,
    val savings: Double,
) {
    companion object {
        fun of(income: Double): BudgetSplit = BudgetSplit(
            essentials = income * 0.5,
            wants = income * 0.3,
            savings = income * 0.2,
        )
    }
}
