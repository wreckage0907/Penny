package com.penny.app.core

/**
 * v2.1 profile-screen decisions: the dirty-form back guard (S05), the
 * delete-account impact summary (S02), and CSV export (frame 14 row).
 * All pure; all new behaviour by the redesign.
 */
object ProfileExtras {

    /** A staged photo change (frame 15): none / replaced / removed. */
    sealed interface PhotoStage {
        data object Unchanged : PhotoStage
        data class Replaced(val url: String) : PhotoStage
        data object Removed : PhotoStage
    }

    data class ProfileDraft(
        val fullName: String,
        val email: String,
        val phone: String,
        val photo: PhotoStage = PhotoStage.Unchanged,
    )

    /** "Unsaved changes" (frame 15 navbar side): any edit vs the saved doc. */
    fun isDirty(original: ProfileDraft, draft: ProfileDraft): Boolean = original != draft

    /** Field-level validation before Save; first failure wins (top field first). */
    sealed interface SaveIssue {
        data object NameRequired : SaveIssue
        data object EmailRequired : SaveIssue
        data object EmailInvalid : SaveIssue
    }

    fun validate(draft: ProfileDraft): SaveIssue? = when {
        draft.fullName.isBlank() -> SaveIssue.NameRequired
        draft.email.isBlank() -> SaveIssue.EmailRequired
        !FakeAuthService.isValidEmail(draft.email) -> SaveIssue.EmailInvalid
        else -> null
    }

    /**
     * The S02 bullet list: exact, user-scoped numbers instead of a vague
     * warning. `unitCount` is finished learning units.
     */
    fun deleteImpact(
        transactionCount: Int,
        monthLabel: String,
        coins: Int,
        streakDays: Int,
        badgeCount: Int,
        unitCount: Int,
        hasPhoto: Boolean,
        aiThreadCount: Int,
    ): List<String> = buildList {
        add("$transactionCount $monthLabel transactions and all other finance data")
        add("Course progress, streaks, badges, and $coins coins")
        if (hasPhoto) add("Profile details and photo") else add("Profile details")
        if (aiThreadCount > 0) add("Penny AI threads")
        if (unitCount == 0 && streakDays == 0 && badgeCount == 0) {
            // Nothing learning-shaped exists yet; keep the list honest.
            removeAt(1)
        }
    }

    /** One CSV row per expense, header first (frame 14 "Export CSV · 18 rows"). */
    fun csv(rows: List<CsvExpense>): String = buildString {
        appendLine("Date,Category,Merchant,Payment,Amount")
        rows.forEach { row ->
            appendLine(
                listOf(
                    row.date, row.category, row.merchant, row.payment, amountColumn(row.amount),
                ).joinToString(",") { escape(it) },
            )
        }
    }

    /** CSV money: plain number, two decimals, no currency or grouping. */
    internal fun amountColumn(amount: Double): String =
        java.math.BigDecimal(amount).setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()

    data class CsvExpense(
        val date: String,
        val category: String,
        val merchant: String,
        val payment: String,
        val amount: Double,
    )

    /** RFC-4180-ish: quote when a comma, quote or newline appears. */
    internal fun escape(field: String): String =
        if (field.any { it == ',' || it == '"' || it == '\n' }) "\"${field.replace("\"", "\"\"")}\"" else field
}
