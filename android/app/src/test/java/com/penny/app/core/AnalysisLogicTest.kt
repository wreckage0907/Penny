package com.penny.app.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The Analysis tab (income_analysis.dart:86-210, 288-340). The goldens below
 * were emitted by executing the verbatim Dart on jsonDecode'd fixtures
 * (harness dart013/an.dart); doubles compared bit-for-bit.
 */
class AnalysisLogicTest {

    private data class AnalysisGolden(
        val label: String,
        val incomeJson: String,
        val expensesJson: String,
        val incomeValue: Double,
        val split: Triple<Double, Double, Double>,
        val categories: List<String>,
        val sections: List<List<Double>>,
        val totals: List<Double>,
    )

    private fun obj(json: String): JsonObject = Json.parseToJsonElement(json).jsonObject
    private fun bits(d: Double) = java.lang.Double.doubleToRawLongBits(d)

    private val analysisGoldens = listOf(
        AnalysisGolden("typical", "{\"2026\":{\"May\":50000.0}}", "{\"2026\":{\"May\":{\"categories\":{\"Food\":[{\"name\":\"a\",\"amount\":0.1},{\"name\":\"b\",\"amount\":0.2}],\"Gifts\":[{\"name\":\"g\",\"amount\":3.0}]}}}}", incomeValue = 50000.0, split = Triple(25000.0, 15000.0, 10000.0),
            categories = listOf("Food", "Gifts"), sections = listOf(listOf(33.33333333333333, 66.66666666666666), listOf(100.0)), totals = listOf(0.30000000000000004, 3.0)),
        AnalysisGolden("int income wipes expenses too", "{\"2026\":{\"May\":0}}", "{\"2026\":{\"May\":{\"categories\":{\"Food\":[{\"name\":\"a\",\"amount\":10.0}]}}}}", incomeValue = 0.0, split = Triple(0.0, 0.0, 0.0),
            categories = listOf(), sections = listOf(), totals = listOf()),
        AnalysisGolden("missing month income -> 0.0 keeps expenses", "{\"2026\":{}}", "{\"2026\":{\"May\":{\"categories\":{\"Food\":[{\"name\":\"a\",\"amount\":10.0}]}}}}", incomeValue = 0.0, split = Triple(0.0, 0.0, 0.0),
            categories = listOf("Food"), sections = listOf(listOf(100.0)), totals = listOf(10.0)),
        AnalysisGolden("no categories", "{\"2026\":{\"May\":1234.5}}", "{\"2026\":{\"May\":{}}}", incomeValue = 1234.5, split = Triple(617.25, 370.34999999999997, 246.9),
            categories = listOf(), sections = listOf(), totals = listOf()),
        AnalysisGolden("categories not a map wipes", "{\"2026\":{\"May\":100.0}}", "{\"2026\":{\"May\":{\"categories\":[]}}}", incomeValue = 0.0, split = Triple(0.0, 0.0, 0.0),
            categories = listOf(), sections = listOf(), totals = listOf()),
        AnalysisGolden("category value not list wipes", "{\"2026\":{\"May\":100.0}}", "{\"2026\":{\"May\":{\"categories\":{\"Food\":{\"x\":1}}}}}", incomeValue = 0.0, split = Triple(0.0, 0.0, 0.0),
            categories = listOf(), sections = listOf(), totals = listOf()),
        AnalysisGolden("empty list category", "{\"2026\":{\"May\":100.0}}", "{\"2026\":{\"May\":{\"categories\":{\"Food\":[]}}}}", incomeValue = 100.0, split = Triple(50.0, 30.0, 20.0),
            categories = listOf("Food"), sections = listOf(listOf()), totals = listOf(0.0)),
        AnalysisGolden("thirds", "{\"2026\":{\"May\":333.33}}", "{\"2026\":{\"May\":{\"categories\":{\"Food\":[{\"name\":\"a\",\"amount\":1.0},{\"name\":\"b\",\"amount\":1.0},{\"name\":\"c\",\"amount\":1.0}]}}}}", incomeValue = 333.33, split = Triple(166.665, 99.999, 66.666),
            categories = listOf("Food"), sections = listOf(listOf(33.33333333333333, 33.33333333333333, 33.33333333333333)), totals = listOf(3.0)),
        AnalysisGolden("odd split", "{\"2026\":{\"May\":0.1}}", "{\"2026\":{\"May\":{\"categories\":{\"Food\":[{\"name\":\"a\",\"amount\":0.7},{\"name\":\"b\",\"amount\":0.1},{\"name\":\"c\",\"amount\":0.2}]}}}}", incomeValue = 0.1, split = Triple(0.05, 0.03, 0.020000000000000004),
            categories = listOf("Food"), sections = listOf(listOf(70.0, 10.0, 20.0)), totals = listOf(1.0)),
    )

    @Test
    fun `load, split, sections and card totals match executed Dart`() {
        val failures = analysisGoldens.mapNotNull { g ->
            val s = AnalysisLogic.load(obj(g.incomeJson), obj(g.expensesJson), 2026, "May")
            val split = s.split
            val cats = s.expenses.keys.toList()
            when {
                bits(s.income) != bits(g.incomeValue) -> "${g.label}: income ${s.income}"
                listOf(split.essentials, split.wants, split.savings).map(::bits) !=
                    listOf(g.split.first, g.split.second, g.split.third).map(::bits) -> "${g.label}: split $split"
                cats != g.categories -> "${g.label}: categories $cats"
                cats.map { AnalysisLogic.sections(s.expenses.getValue(it)).map(::bits) } != g.sections.map { l -> l.map(::bits) } ->
                    "${g.label}: sections ${cats.map { AnalysisLogic.sections(s.expenses.getValue(it)) }}"
                cats.map { bits(AnalysisLogic.cardTotal(s.expenses.getValue(it))) } != g.totals.map(::bits) -> "${g.label}: totals"
                else -> null
            }
        }
        assertEquals(failures.joinToString("\n"), 0, failures.size)
    }

    @Test
    fun `the int-income TypeError wipes the expenses too (one try)`() {
        val s = AnalysisLogic.load(obj("""{"2026":{"May":0}}"""),
            obj("""{"2026":{"May":{"categories":{"Food":[{"name":"a","amount":10.0}]}}}}"""), 2026, "May")
        assertEquals(AnalysisLogic.EMPTY, s)
    }

    @Test
    fun `texts, palette, axis and maxY`() {
        assertEquals("No data available for June 2026", AnalysisLogic.emptyText("June", 2026))
        assertEquals(listOf(0xFFE91E63, 0xFF673AB7, 0xFFFF5722, 0xFF448AFF, 0xFF009688, 0xFFFFEB3B), AnalysisLogic.COLORS)
        assertEquals(0xFFE91E63, AnalysisLogic.color(6))
        assertEquals("\$17623", AnalysisLogic.axisLabel(17623.41683200001)) // toInt truncates
        val projections = InvestmentProjection.project(10_000.0)
        assertEquals(projections[3][5], AnalysisLogic.maxY(projections), 0.0) // Gold, year 5
        assertEquals(0.0, AnalysisLogic.maxY(InvestmentProjection.project(0.0)), 0.0)
    }

    @Test
    fun `section percentages divide THEN multiply (discriminating fixture, Dart-executed)`() {
        // dart013/disc.dart: (0.1 / 0.7999999999999999) * 100 = 12.500000000000004,
        // while 0.1 * 100 / 0.7999999999999999 = 12.500000000000002 (013 review note 1).
        val rows = listOf(0.1, 0.7).map { kotlinx.serialization.json.JsonObject(mapOf("amount" to kotlinx.serialization.json.JsonPrimitive(it))) }
        assertEquals(12.500000000000004, AnalysisLogic.sections(rows)[0], 0.0)
    }
}
