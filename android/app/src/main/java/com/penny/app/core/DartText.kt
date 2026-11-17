package com.penny.app.core

import java.math.BigInteger

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

    /** Dart `RangeError` text for `""[0]` — user-visible via the error widget. */
    const val EMPTY_INDEX_RANGE_ERROR =
        "RangeError (index): Invalid value: Valid value range is empty: 0"
}
