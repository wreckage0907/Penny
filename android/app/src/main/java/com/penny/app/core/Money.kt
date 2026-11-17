package com.penny.app.core

import java.math.BigDecimal
import java.math.MathContext
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
 * For |value| >= 1e21 Dart returns `toStringAsExponential()`: the SHORTEST
 * digits that round-trip, as "d.ddde+NN" ("1e+21", "1.1805916207174113e+21");
 * reproduced in [exponential] (closed in 006; 5,965 Dart-emitted goldens).
 */
object Money {
    fun format(value: Double): String {
        if (value.isNaN()) return "NaN"
        if (value == Double.POSITIVE_INFINITY) return "Infinity"
        if (value == Double.NEGATIVE_INFINITY) return "-Infinity"
        if (Math.abs(value) >= 1e21) return exponential(value)
        val digits = BigDecimal(value).setScale(2, RoundingMode.HALF_UP).toPlainString()
        val negative = value < 0.0 || (value == 0.0 && 1.0 / value < 0.0)
        return if (negative && !digits.startsWith("-")) "-$digits" else digits
    }

    /**
     * Dart/JS `toStringAsExponential()` with no fraction digits: the shortest
     * decimal that round-trips to [value]; among equally short candidates the
     * closest to the exact value, ties to an even last digit (ECMAScript rule;
     * no double in the 5,965-golden set reaches an exact tie). Both the floor
     * and ceiling candidates are checked at each precision because the
     * round-trip interval is asymmetric at powers of two.
     */
    internal fun exponential(value: Double): String {
        val exact = BigDecimal(Math.abs(value))
        var best: BigDecimal? = null
        for (precision in 1..17) {
            val candidates = listOf(RoundingMode.FLOOR, RoundingMode.CEILING)
                .map { exact.round(MathContext(precision, it)) }
                .distinct()
                .filter { it.toDouble() == Math.abs(value) }
            if (candidates.isEmpty()) continue
            best = candidates.minWithOrNull(
                compareBy<BigDecimal> { (it - exact).abs() }
                    .thenBy { it.stripTrailingZeros().unscaledValue().testBit(0) },
            )
            break
        }
        val shortest = best!!.stripTrailingZeros()
        val digits = shortest.unscaledValue().toString()
        val exponent = digits.length - 1 - shortest.scale()
        val mantissa = if (digits.length == 1) digits else digits[0] + "." + digits.substring(1)
        return (if (value < 0) "-" else "") + mantissa + "e+" + exponent
    }
}
