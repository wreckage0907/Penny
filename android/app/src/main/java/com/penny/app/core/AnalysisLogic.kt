package com.penny.app.core

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * The Analysis tab's data logic (income_analysis.dart:86-180, 207-210,
 * 288-340). Income and expenses load in ONE try: an int income (the backend's
 * `0` for a month without income) is a Dart TypeError that zeroes the income
 * AND wipes the expenses — the analysis-side blast radius of the int-income
 * quirk (LEDGER, PRESERVE). Goldens were emitted by executing the verbatim Dart.
 */
object AnalysisLogic {

    /** Income + raw expense rows per category (`{name, amount}` are read lazily by the page). */
    data class State(val income: Double, val expenses: Map<String, List<JsonObject>>) {
        /** _calculateSplit: 0.5 / 0.3 / 0.2 (the 001 core). */
        val split: BudgetSplit get() = BudgetSplit.of(income)
    }

    val EMPTY = State(0.0, emptyMap())

    private class Wipe : RuntimeException()

    /**
     * `incomeData[y]?[m] ?? 0.0`, then `(expenseData[y]?[m]?['categories'] as
     * Map?)?.map((k, v) => (v as List).cast<Map>())`; any TypeError → EMPTY.
     */
    fun load(incomeData: JsonObject, expenseData: JsonObject, year: Int, month: String): State = try {
        val income = when (val y = incomeData[year.toString()]) {
            null, JsonNull -> 0.0
            is JsonObject -> when (val v = y[month]) {
                null, JsonNull -> 0.0
                is JsonPrimitive -> if (dartKind(v) == "double") v.content.toDouble() else throw Wipe()
                else -> throw Wipe()
            }
            else -> throw Wipe()
        }
        val categories = when (val y = expenseData[year.toString()]) {
            null, JsonNull -> null
            is JsonObject -> when (val m = y[month]) {
                null, JsonNull -> null
                is JsonObject -> when (val c = m["categories"]) {
                    null, JsonNull -> null
                    is JsonObject -> c
                    else -> throw Wipe()
                }
                else -> throw Wipe()
            }
            else -> throw Wipe()
        }
        val expenses = categories?.mapValues { (_, v) ->
            // `.cast<Map<String, dynamic>>()` is lazy: element types are only checked on use.
            (v as? JsonArray ?: throw Wipe()).map { it as? JsonObject ?: JsonObject(emptyMap()) }
        } ?: emptyMap()
        State(income, expenses)
    } catch (e: Wipe) {
        EMPTY
    }

    private fun amountOf(e: JsonObject): Double? =
        (e["amount"] as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content?.toDoubleOrNull()

    /** getSections (:157-180): `amount / total * 100` per expense, only if the total is finite and > 0. */
    fun sections(expenses: List<JsonObject>): List<Double> {
        val amounts = expenses.map { amountOf(it) ?: 0.0 }
        var total = 0.0
        for (a in amounts) total += a
        if (!(total.isFinite() && total > 0)) return emptyList()
        return amounts.map { (it / total) * 100 }
    }

    /** Card total (:207-210): `fold(0.0, sum + (amount as double? ?? 0.0))`. */
    fun cardTotal(expenses: List<JsonObject>): Double {
        var sum = 0.0
        for (e in expenses) sum += amountOf(e) ?: 0.0
        return sum
    }

    /** "No data available for $selectedMonth $selectedYear" (:194). */
    fun emptyText(month: String, year: Int): String = "No data available for $month $year"

    /** This page's own palette (:126-133): pink, deepPurple, deepOrange, blueAccent, teal, yellow. */
    val COLORS: List<Long> = listOf(0xFFE91E63, 0xFF673AB7, 0xFFFF5722, 0xFF448AFF, 0xFF009688, 0xFFFFEB3B)

    fun color(index: Int): Long = COLORS[index % COLORS.size]

    /** BarChart maxY (:296-297): the largest projected value of any type and year. */
    fun maxY(projections: List<List<Double>>): Double = projections.flatten().reduce { a, b -> if (a > b) a else b }

    /** Left axis labels: `'\$${value.toInt()}'` (Dart toInt truncates toward zero). */
    fun axisLabel(value: Double): String = "\$" + value.toLong()
}
