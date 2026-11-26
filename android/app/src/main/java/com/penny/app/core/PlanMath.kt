package com.penny.app.core

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * v2.1 budget vocabulary and month arithmetic (artboard frames 04–08, S03/S04).
 *
 * New behaviour by the maintainer-approved redesign — NOT Flutter parity:
 * the old dashboard/analysis screens and their quirks (int-income wipe,
 * "\$x.toStringAsFixed(2)", subtotal-then-accumulate) are superseded by this
 * model. The wire format is unchanged; income extraction here is LENIENT
 * (the backend's int `0` for a missing month is simply income 0 — the
 * int-income "quirk" no longer wipes anything because there is no Dart
 * `double` field to blow up).
 */
object BudgetCategories {
    const val FOOD = "Food & Dining"
    const val RENT = "Rent & Utilities"
    const val SHOPPING = "Shopping"
    const val TRANSPORT = "Transport & Metro"
    const val ENTERTAINMENT = "Entertainment"

    /** Wire/display order of the Quick Log choice grid. */
    val ALL = listOf(FOOD, RENT, SHOPPING, TRANSPORT, ENTERTAINMENT)

    /** 50/30/20 buckets (plan frame 08): Needs = food + rent + transport. */
    val NEEDS = setOf(FOOD, RENT, TRANSPORT)

    /** Wants = shopping + entertainment. */
    val WANTS = setOf(SHOPPING, ENTERTAINMENT)

    /** Artboard c1..c5, keyed by category. */
    val COLORS: Map<String, Long> = mapOf(
        FOOD to 0xFFA35555,
        SHOPPING to 0xFFC87D55,
        RENT to 0xFF5B7065,
        TRANSPORT to 0xFF4A6B82,
        ENTERTAINMENT to 0xFF7D5BA6,
    )
}

/** One month of budget inputs, already reduced from the wire JSON. */
data class PlanMonth(
    val income: Double,
    /** Wire category → (total, item count), insertion order. */
    val categories: Map<String, Pair<Double, Int>>,
    /** SIP-kind allocations recorded for the month (frame 08: 20% goal). */
    val sipRecorded: Double,
    /** Non-SIP goal allocations (e.g. a Penny AI emergency-fund move). */
    val goalAllocated: Double,
)

data class BucketSummary(
    val label: String,
    val sharePercent: Int,
    val actual: Double,
    val target: Double,
    val remainder: Double,
    /** actual / target, 0 when there is no target. */
    val usedShare: Double,
)

data class PlanSummary(
    val income: Double,
    val spent: Double,
    /** income − spent ("net cash retained", frame 04). */
    val retained: Double,
    val needs: BucketSummary,
    val wants: BucketSummary,
    val sipRecorded: Double,
    val sipGoal: Double,
    val goalAllocated: Double,
    /** retained − sipRecorded − goalAllocated (frame 04: "remains unallocated"). */
    val unallocated: Double,
)

/** Pure v2.1 plan decisions. */
object PlanMath {

    /** Per-category totals from the dashboard's month JSON, in wire order. */
    fun monthCategories(expenseData: JsonObject, year: Int, month: String): Map<String, Pair<Double, Int>> =
        ExpenseAggregation.categoryTotals(ExpenseAggregation.categoriesFor(expenseData, year, month))
            .associate { (category, total) -> category to (total to itemCount(expenseData, year, month, category)) }

    private fun itemCount(expenseData: JsonObject, year: Int, month: String, category: String): Int =
        ((ExpenseAggregation.categoriesFor(expenseData, year, month)[category])
            as? kotlinx.serialization.json.JsonArray)?.size ?: 0

    /**
     * Lenient income for the month: int `0` (the backend's missing-month
     * value), doubles, and absent keys all read as plain numbers.
     */
    fun income(incomeData: JsonObject, year: Int, month: String): Double {
        val yearObj = incomeData[year.toString()] as? JsonObject ?: return 0.0
        val value = yearObj[month] as? JsonPrimitive ?: return 0.0
        return value.content.toDoubleOrNull() ?: 0.0
    }

    /** The single source of every number on Home and both Budget tabs. */
    fun summary(month: PlanMonth): PlanSummary {
        val split = BudgetSplit.of(month.income)
        val spent = month.categories.values.sumOf { it.first }
        val retained = month.income - spent
        val needsActual = month.categories.filterKeys { it in BudgetCategories.NEEDS }.values.sumOf { it.first }
        val wantsActual = month.categories.filterKeys { it in BudgetCategories.WANTS }.values.sumOf { it.first }
        return PlanSummary(
            income = month.income,
            spent = spent,
            retained = retained,
            needs = bucket("Needs", 50, needsActual, split.essentials),
            wants = bucket("Wants", 30, wantsActual, split.wants),
            sipRecorded = month.sipRecorded,
            sipGoal = split.savings,
            goalAllocated = month.goalAllocated,
            unallocated = retained - month.sipRecorded - month.goalAllocated,
        )
    }

    private fun bucket(label: String, sharePercent: Int, actual: Double, target: Double) = BucketSummary(
        label = label,
        sharePercent = sharePercent,
        actual = actual,
        target = target,
        remainder = target - actual,
        usedShare = if (target > 0.0) actual / target else 0.0,
    )

    /** Frame S03: "No activity yet" — no income AND no expenses. */
    fun isEmpty(summary: PlanSummary): Boolean =
        summary.income == 0.0 && summary.spent == 0.0

    /** Categories for the Spending tab, largest first (frame 05 order). */
    fun rankedCategories(month: PlanMonth): List<Pair<String, Pair<Double, Int>>> =
        month.categories.entries.sortedByDescending { it.value.first }.map { it.key to it.value }

    /** Prev/next month stepper (frame 05's 48dp arrows): wraps years. */
    fun shift(year: Int, month: String, delta: Int): Pair<Int, String> {
        val names = ExpenseDashboardText.MONTH_NAMES
        val index = names.indexOf(month).let { if (it < 0) 0 else it } + delta
        val yearShift = Math.floorDiv(index, names.size)
        val wrapped = ((index % names.size) + names.size) % names.size
        return (year + yearShift) to names[wrapped]
    }

    /** "Nov 2026" — the month pill and every state banner. */
    fun monthLabel(year: Int, month: String): String =
        "${month.take(3)} $year"
}
