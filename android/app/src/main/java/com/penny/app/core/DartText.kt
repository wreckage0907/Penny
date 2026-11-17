package com.penny.app.core

import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.math.RoundingMode

/**
 * Dart-runtime string semantics that the course/lesson ports depend on.
 * Every set and rule here was derived by EXECUTING Dart 3.14 exhaustively
 * over all BMP code units (and all code points for case mapping), because
 * the Kotlin/Java defaults differ in ways that change output:
 *
 * - Dart `RegExp` `\s` (ECMAScript) includes U+00A0, U+1680, U+2000-200A,
 *   U+2028/2029, U+202F, U+205F, U+3000 and U+FEFF; Java's `\s` is ASCII-only.
 * - Dart `String.trim` / `int.parse` whitespace = that set plus U+0085;
 *   Kotlin `trim()` skips U+0085/U+FEFF but trims other chars Dart keeps.
 * - Dart `.` stops only at `\n \r U+2028 U+2029`; Java's also stops at U+0085.
 * - Dart multiline `^` fires after each of `\n \r U+2028 U+2029` individually
 *   (so BETWEEN the `\r` and `\n` of CRLF); Java treats CRLF as one terminator.
 * - Dart case mapping is the simple 1:1 mapping per code point (no `ß`→`SS`,
 *   no final sigma); Kotlin's `uppercase()` / `lowercase()` use full mapping.
 */
internal object DartText {

    /** Regex character-class body equal to Dart `RegExp(r'\s')`. */
    const val REGEX_WHITESPACE =
        "\\t\\n\\u000B\\f\\r \\u00A0\\u1680\\u2000-\\u200A\\u2028\\u2029\\u202F\\u205F\\u3000\\uFEFF"

    /** Regex character-class body equal to the code units Dart `.` refuses. */
    const val REGEX_LINE_TERMINATORS = "\\n\\r\\u2028\\u2029"

    /** Dart `String.trim` whitespace (same set `int.parse` strips). */
    fun isTrimWhitespace(c: Char): Boolean = when (c) {
        '\t', '\n', '\u000B', '\u000C', '\r', ' ', '\u0085', ' ', ' ',
        ' ', ' ', ' ', ' ', '　', '﻿' -> true
        in ' '..' ' -> true
        else -> false
    }

    /** Dart `String.trim()`. */
    fun trim(s: String): String = s.trim(::isTrimWhitespace)

    /**
     * Dart `String.toUpperCase()`: simple per-code-point mapping. Lone
     * surrogates (e.g. after `this[0]` splits a pair) map to themselves.
     * Known divergence: code points whose case pair is newer than Dart's
     * tables (749 on JDK 17, e.g. Georgian Mtavruli) are left unchanged by
     * Dart but mapped here; ASCII and Latin-1 are identical.
     */
    fun toUpperCase(s: String): String = mapCodePoints(s, Character::toUpperCase)

    /** Dart `String.toLowerCase()` — see [toUpperCase]. */
    fun toLowerCase(s: String): String = mapCodePoints(s, Character::toLowerCase)

    private inline fun mapCodePoints(s: String, f: (Int) -> Int): String {
        val out = StringBuilder(s.length)
        var i = 0
        while (i < s.length) {
            val cp = s.codePointAt(i)
            out.appendCodePoint(f(cp))
            i += Character.charCount(cp)
        }
        return out.toString()
    }

    private val TWO_POW_63: BigInteger = BigInteger.ONE.shiftLeft(63)
    private val TWO_POW_64: BigInteger = BigInteger.ONE.shiftLeft(64)
    private val HEX = Regex("[0-9a-fA-F]+")
    private val DECIMAL = Regex("[0-9]+")

    /**
     * Dart `int.tryParse(source)` with no radix (Dart int is 64-bit):
     * trim, optional `+`/`-`, then either `0x`/`0X` + hex digits — an
     * unsigned 64-bit magnitude wrapped to signed (`0xFFFFFFFFFFFFFFFF` is
     * -1), negative magnitudes capped at 2^63 — or ASCII decimal digits that
     * must fit a signed 64-bit int. Anything else is null.
     */
    fun intTryParse(source: String): Long? {
        var s = trim(source)
        var negative = false
        if (s.startsWith('+') || s.startsWith('-')) {
            negative = s[0] == '-'
            s = s.substring(1)
        }
        if (s.startsWith("0x") || s.startsWith("0X")) {
            val digits = s.substring(2)
            if (!HEX.matches(digits)) return null
            val magnitude = BigInteger(digits, 16)
            return if (negative) {
                if (magnitude > TWO_POW_63) null else magnitude.negate().toLong()
            } else {
                if (magnitude >= TWO_POW_64) null else magnitude.toLong()
            }
        }
        // Guard: Kotlin's toLongOrNull would accept non-ASCII digits (٣, １).
        if (!DECIMAL.matches(s)) return null
        return (if (negative) "-$s" else s).toLongOrNull()
    }

    /**
     * Shortest decimal digits that round-trip to [abs] (finite, > 0), as
     * (digits, decimal exponent of the first digit). Among equally short
     * candidates the closest to the exact value wins, ties to an even last
     * digit (ECMAScript rule, which Dart follows). Both floor and ceiling are
     * tried at each precision: the round-trip interval is asymmetric at powers
     * of two (verified on every 2^k, k = 70..1023, slice 006).
     */
    fun shortestDigits(abs: Double): Pair<String, Int> {
        val exact = BigDecimal(abs)
        for (precision in 1..17) {
            val best = listOf(RoundingMode.FLOOR, RoundingMode.CEILING)
                .map { exact.round(MathContext(precision, it)) }
                .distinct()
                .filter { it.toDouble() == abs }
                .minWithOrNull(
                    compareBy<BigDecimal> { (it - exact).abs() }
                        .thenBy { it.stripTrailingZeros().unscaledValue().testBit(0) },
                ) ?: continue
            val shortest = best.stripTrailingZeros()
            val digits = shortest.unscaledValue().toString()
            return digits to (digits.length - 1 - shortest.scale())
        }
        error("unreachable: 17 digits always round-trip")
    }

    /**
     * Dart `double.toString()`: shortest round-trip digits; plain decimal for
     * 1e-6 <= |x| < 1e21 (integers keep ".0"), exponential otherwise
     * ("1e-7", "1.5e+21"); "NaN", "Infinity", "-0.0" as Dart prints them.
     */
    fun doubleToString(value: Double): String {
        if (value.isNaN()) return "NaN"
        if (value.isInfinite()) return if (value > 0) "Infinity" else "-Infinity"
        if (value == 0.0) return if (1.0 / value < 0) "-0.0" else "0.0"
        val sign = if (value < 0) "-" else ""
        val abs = Math.abs(value)
        val (digits, exp) = shortestDigits(abs)
        if (abs >= 1e-6 && abs < 1e21) {
            val body = when {
                exp >= digits.length - 1 -> digits + "0".repeat(exp - digits.length + 1) + ".0"
                exp >= 0 -> digits.substring(0, exp + 1) + "." + digits.substring(exp + 1)
                else -> "0." + "0".repeat(-exp - 1) + digits
            }
            return sign + body
        }
        val mantissa = if (digits.length == 1) digits else digits[0] + "." + digits.substring(1)
        return sign + mantissa + "e" + (if (exp >= 0) "+" else "-") + Math.abs(exp)
    }

    private val DOUBLE_LITERAL = Regex("[+-]?(NaN|Infinity|([0-9]+\\.?[0-9]*|\\.[0-9]+)([eE][+-]?[0-9]+)?)")

    /**
     * Dart `double.tryParse(source)`: Dart-trimmed; optional sign; `NaN`
     * (even "-NaN"), `Infinity`, or ASCII decimal with optional fraction
     * (".5", "5." ok) and exponent; overflow → ±Infinity. No hex, no
     * grouping, no non-ASCII digits.
     */
    fun doubleTryParse(source: String): Double? {
        val s = trim(source)
        if (!DOUBLE_LITERAL.matches(s)) return null
        val negative = s.startsWith('-')
        val body = s.trimStart('+', '-')
        return when (body) {
            "NaN" -> Double.NaN
            "Infinity" -> if (negative) Double.NEGATIVE_INFINITY else Double.POSITIVE_INFINITY
            else -> s.toDouble()
        }
    }

    /** Dart `RangeError` text for `""[0]` — user-visible via the error widget. */
    const val EMPTY_INDEX_RANGE_ERROR =
        "RangeError (index): Invalid value: Valid value range is empty: 0"
}
