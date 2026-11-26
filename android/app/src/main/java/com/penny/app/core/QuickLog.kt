package com.penny.app.core

/**
 * v2.1 Quick Log decisions (artboard frames 06, 07, S01). "Generic Add never
 * chooses a category silently" — amount and category are both required, and
 * validation failures point at the offending field.
 *
 * New behaviour (the Flutter dialogs validated nothing and crashed on
 * `double.parse` — DashboardFlows keeps that old contract for its tests).
 */
object QuickLog {
    /** Frame 06's payment choices. */
    val PAYMENTS = listOf("UPI", "Card", "Cash")

    sealed interface Result {
        /** Ready to save. */
        data class Valid(
            val amount: Double,
            val category: String,
            val merchant: String,
            val day: Int,
            val payment: String,
        ) : Result

        data object AmountRequired : Result
        data object AmountInvalid : Result
        data object CategoryRequired : Result
    }

    /**
     * Amount must parse as a positive number ("0.00" placeholder never is),
     * category must be a known budget category, merchant/note is optional
     * (blank kept as ""), payment must be one of [PAYMENTS] (UPI default).
     */
    fun parse(
        amountText: String,
        category: String?,
        merchant: String,
        day: Int,
        payment: String = "UPI",
    ): Result {
        val text = amountText.trim()
        if (text.isEmpty()) return Result.AmountRequired
        val amount = text.toDoubleOrNull() ?: return Result.AmountInvalid
        if (amount <= 0.0 || amount.isNaN() || amount.isInfinite()) return Result.AmountInvalid
        if (category == null || category !in BudgetCategories.ALL) return Result.CategoryRequired
        if (payment !in PAYMENTS) return Result.CategoryRequired // unknown payment = form bug, not user error
        return Result.Valid(amount, category, merchant.trim(), day, payment)
    }

    /** Income variant (S03 "Add Monthly Income"): only the amount matters. */
    sealed interface IncomeResult {
        data class Valid(val amount: Double) : IncomeResult
        data object AmountRequired : IncomeResult
        data object AmountInvalid : IncomeResult
    }

    fun parseIncome(amountText: String): IncomeResult {
        val text = amountText.trim()
        if (text.isEmpty()) return IncomeResult.AmountRequired
        val amount = text.toDoubleOrNull() ?: return IncomeResult.AmountInvalid
        if (amount <= 0.0 || amount.isNaN() || amount.isInfinite()) return IncomeResult.AmountInvalid
        return IncomeResult.Valid(amount)
    }
}
