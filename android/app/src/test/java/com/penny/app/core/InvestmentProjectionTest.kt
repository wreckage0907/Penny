package com.penny.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Characterisation tests for the Flutter `calculateInvestmentValues`
 * (income_analysis.dart:135-155). Goldens are hand-derived from the Dart
 * algorithm and independently recomputed bit-for-bit (IEEE-754 binary64,
 * `last * (1 + rate)` per step): series start at savings, then 5 iterative
 * compound steps. All value assertions are exact (0.0 tolerance) — the
 * iterative op order differs from closed-form pow by ~1e-12, so tolerances
 * would hide exactly the drift these tests exist to catch.
 */
class InvestmentProjectionTest {

    @Test
    fun `four series of six points, each starting at savings`() {
        val projections = InvestmentProjection.project(10_000.0)
        assertEquals(4, projections.size)
        for (series in projections) {
            assertEquals(6, series.size)
            assertEquals(10_000.0, series.first(), 0.0)
        }
    }

    @Test
    fun `types and rates match the Dart lists verbatim, dead tail included`() {
        // income_analysis.dart:135-141. The tail (indices 4-6) is never used
        // by the loop, but it is part of the spec — including the duplicate
        // 0.10 at index 4. Display labels are user-visible in Flutter.
        assertEquals(
            listOf("Fixed Deposits", "Mutual Funds", "Stocks", "Gold"),
            InvestmentProjection.TYPES,
        )
        assertEquals(
            listOf(0.03, 0.06, 0.10, 0.12, 0.10, 0.075, 0.065),
            InvestmentProjection.ANNUAL_RETURNS,
        )
    }

    @Test
    fun `fixed deposits full series golden at 3 percent`() {
        val fd = InvestmentProjection.project(10_000.0)[0]
        assertEquals(
            listOf(10_000.0, 10_300.0, 10_609.0, 10_927.27, 11_255.0881, 11_592.740743),
            fd,
        )
    }

    @Test
    fun `final values after 5 years for mutual funds, stocks and gold`() {
        val projections = InvestmentProjection.project(10_000.0)
        // Exact iterative products; the pretty decimals 16105.1 / 17623.416832
        // do NOT round-trip to these doubles.
        assertEquals(13_382.255776, projections[1].last(), 0.0)      // 6%
        assertEquals(16_105.100000000008, projections[2].last(), 0.0) // 10%
        assertEquals(17_623.41683200001, projections[3].last(), 0.0)  // 12%
    }

    @Test
    fun `operation order is locked by raw bits at a discriminating point`() {
        // Mutual funds (6%), year 2, savings 10000:
        //   iterative (Dart): 11236.0               = 0x40C5F20000000000
        //   closed-form pow:  11236.000000000002    = 0x40C5F20000000001
        // A refactor to Math.pow or BigDecimal fails here. (Stocks year 2 was
        // considered and rejected: iterative and pow agree there.)
        val mfYear2 = InvestmentProjection.project(10_000.0)[1][2]
        assertEquals(11_236.0, mfYear2, 0.0)
        assertEquals(
            0x40C5F20000000000L,
            java.lang.Double.doubleToRawLongBits(mfYear2),
        )
    }

    @Test
    fun `zero savings stay zero in every series`() {
        for (series in InvestmentProjection.project(0.0)) {
            for (value in series) assertEquals(0.0, value, 0.0)
        }
    }

    @Test
    fun `negative savings compound unguarded, as in Dart`() {
        val fd = InvestmentProjection.project(-1_000.0)[0]
        assertEquals(
            listOf(-1_000.0, -1_030.0, -1_060.9, -1_092.727, -1_125.50881, -1_159.2740743000002),
            fd,
        )
    }

    @Test
    fun `NaN and infinity propagate with no guards, as in Dart`() {
        for (series in InvestmentProjection.project(Double.NaN)) {
            for (value in series) assertTrue(value.isNaN())
        }
        for (series in InvestmentProjection.project(Double.POSITIVE_INFINITY)) {
            for (value in series) assertEquals(Double.POSITIVE_INFINITY, value, 0.0)
        }
    }

    @Test
    fun `only the first four of the seven rates are used, as in Dart`() {
        // The Dart list has 7 entries; the loop iterates investmentTypes.length (4).
        assertEquals(7, InvestmentProjection.ANNUAL_RETURNS.size)
        assertEquals(4, InvestmentProjection.project(1.0).size)
    }

    @Test
    fun `growth is iterative, matching Dart float operation order exactly`() {
        val savings = 12_345.67
        val projections = InvestmentProjection.project(savings)
        for (i in projections.indices) {
            var expected = savings
            for (year in 1..InvestmentProjection.YEARS) {
                expected *= (1 + InvestmentProjection.ANNUAL_RETURNS[i])
            }
            // Consistency check on op order; the Dart-anchored proof is the
            // raw-bits golden above, not this loop.
            assertEquals(expected, projections[i].last(), 0.0)
        }
    }

    @Test
    fun `split-to-projection chain reproduces the e2e goldens headlessly`() {
        // The same chain MainActivity renders: income 50000 -> savings 10000
        // -> 5-year finals. Covers the integration in milliseconds on the JVM
        // (the pre-commit hook runs only unit tests).
        val savings = BudgetSplit.of(50_000.0).savings
        val finals = InvestmentProjection.project(savings).map { it.last() }
        assertEquals(
            listOf(11_592.740743, 13_382.255776, 16_105.100000000008, 17_623.41683200001),
            finals,
        )
    }
}