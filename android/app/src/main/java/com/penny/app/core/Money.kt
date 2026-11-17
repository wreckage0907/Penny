package com.penny.app.core

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Money display formatting: port of Dart's `toStringAsFixed(2)`
 * (income_analysis.dart:244,628; expense_dashboard.dart:250,911,1019,1050,1174).
 *
 * Dart rounds the double's EXACT binary value, ties away from zero, so
 * 1.005 (really 1.00499999…) prints "1.00". Java's `%.2f` rounds the
 * shortest decimal repr instead and prints "1.01" — the original 001
 * implementation used it and diverged on ordinary amounts (2.675, 0.015,
 * 999999.995…); fixed in 005. `BigDecimal(double)` is exact.
 *
 * Negative values that round to zero keep their sign ("-0.00"), like Dart.
 * NaN/±Infinity print as Dart does.
 *
 * Known, deliberate divergence (recorded in migration/LEDGER.md): for
 * |value| >= 1e21 Dart switches to exponential notation ("1e+21") while this
 * prints full digits. Unreachable for income and expense amounts.
 */
object Money {
    fun format(value: Double): String {
        if (value.isNaN()) return "NaN"
        if (value == Double.POSITIVE_INFINITY) return "Infinity"
        if (value == Double.NEGATIVE_INFINITY) return "-Infinity"
        val digits = BigDecimal(value).setScale(2, RoundingMode.HALF_UP).toPlainString()
        val negative = value < 0.0 || (value == 0.0 && 1.0 / value < 0.0)
        return if (negative && !digits.startsWith("-")) "-$digits" else digits
    }
}
