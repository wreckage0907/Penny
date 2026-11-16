package com.penny.app.core

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Characterisation tests for the Flutter `_calculateSplit`
 * (income_analysis.dart:120-124). Expected values are derived from the Dart
 * source, quirks included — this is a parity port, not a redesign.
 *
 * Every product here is a single correctly-rounded IEEE-754 multiply, so all
 * assertions use 0.0 tolerance with exact literals (verified bit-for-bit
 * against the Dart operation sequence).
 */
class BudgetSplitTest {

    @Test
    fun `50-30-20 split of a round income`() {
        val split = BudgetSplit.of(50_000.0)
        assertEquals(25_000.0, split.essentials, 0.0)
        assertEquals(15_000.0, split.wants, 0.0)
        assertEquals(10_000.0, split.savings, 0.0)
    }

    @Test
    fun `zero income gives all zeros, like the Flutter error path`() {
        // On any fetch error the Dart code sets incomeAmount = 0.0 and recalculates.
        val split = BudgetSplit.of(0.0)
        assertEquals(0.0, split.essentials, 0.0)
        assertEquals(0.0, split.wants, 0.0)
        assertEquals(0.0, split.savings, 0.0)
    }

    @Test
    fun `fractional income keeps full double precision`() {
        val split = BudgetSplit.of(33_333.33)
        assertEquals(16_666.665, split.essentials, 0.0)
        assertEquals(9_999.999, split.wants, 0.0)
        // 33333.33 * 0.2 is 6666.666000000001, not 6666.666 — the pretty
        // decimal is 1 ulp off the true product.
        assertEquals(6_666.666000000001, split.savings, 0.0)
    }

    @Test
    fun `negative income passes through unguarded, as in Dart`() {
        val split = BudgetSplit.of(-100.0)
        assertEquals(-50.0, split.essentials, 0.0)
        assertEquals(-30.0, split.wants, 0.0)
        assertEquals(-20.0, split.savings, 0.0)
    }

    @Test
    fun `parts sum back to the income within float tolerance`() {
        val income = 12_345.67
        val split = BudgetSplit.of(income)
        assertEquals(income, split.essentials + split.wants + split.savings, 1e-9)
    }

    @Test
    fun `negative zero keeps its sign bit, as in Dart`() {
        // JUnit's double assertEquals treats -0.0 == 0.0, so assert raw bits.
        val split = BudgetSplit.of(-0.0)
        assertEquals(
            0x8000000000000000UL.toLong(),
            java.lang.Double.doubleToRawLongBits(split.essentials),
        )
        assertEquals(
            0x8000000000000000UL.toLong(),
            java.lang.Double.doubleToRawLongBits(split.wants),
        )
        assertEquals(
            0x8000000000000000UL.toLong(),
            java.lang.Double.doubleToRawLongBits(split.savings),
        )
    }

    @Test
    fun `subnormal income underflows to zero with no normalisation`() {
        // Double.MIN_VALUE * 0.5 / 0.3 / 0.2 all underflow to +0.0 in Dart;
        // pins that the port adds no clamping or rounding of its own.
        val split = BudgetSplit.of(Double.MIN_VALUE)
        assertEquals(0.0, split.essentials, 0.0)
        assertEquals(0.0, split.wants, 0.0)
        assertEquals(0.0, split.savings, 0.0)
    }
}