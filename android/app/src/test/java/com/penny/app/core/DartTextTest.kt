package com.penny.app.core

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Locks [DartText] to Dart 3.14 runtime semantics. The character sets were
 * derived by executing Dart over ALL 65,536 BMP code units (RegExp `\s`,
 * `.`, multiline `^`, `String.trim`, `int.tryParse`), so these tests assert
 * exhaustively, not by sampling. int.tryParse goldens were emitted by Dart.
 */
class DartTextTest {

    /** Executed: RegExp(r'\s').hasMatch — note U+0085 is NOT included. */
    private val dartRegexWhitespace: Set<Int> =
        setOf(0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x20, 0xA0, 0x1680) +
            (0x2000..0x200A) + setOf(0x2028, 0x2029, 0x202F, 0x205F, 0x3000, 0xFEFF)

    /** Executed: String.trim() and int.tryParse padding — adds U+0085. */
    private val dartTrimWhitespace: Set<Int> = dartRegexWhitespace + 0x85

    /** Executed: code units RegExp('.') refuses / multiline `^` breaks after. */
    private val dartLineTerminators: Set<Int> = setOf(0x0A, 0x0D, 0x2028, 0x2029)

    private val intGoldens = listOf(
        "1" to 1L,
        "2" to 2L,
        "10" to 10L,
        "02" to 2L,
        "0" to 0L,
        "00" to 0L,
        "-3" to -3L,
        "+5" to 5L,
        " 7" to 7L,
        "7 " to 7L,
        "1.5" to null,
        "0x10" to 16L,
        "1e3" to null,
        "99999999999999999999" to null,
        "9223372036854775807" to 9223372036854775807L,
        "9223372036854775808" to null,
        "" to null,
        "abc" to null,
        "\u0661\u0662" to null,
        "\uFF11" to null,
        "Infinity" to null,
        "NaN" to null,
        "--3" to null,
        "+-3" to null,
        "1_000" to null,
        "\u00A07" to 7L,
        "0X10" to 16L,
        "-0x10" to -16L,
        "+0x10" to 16L,
        "0x" to null,
        "0xg" to null,
        "0x7FFFFFFFFFFFFFFF" to 9223372036854775807L,
        "0x8000000000000000" to Long.MIN_VALUE,
        "0xFFFFFFFFFFFFFFFF" to -1L,
        "0x10000000000000000" to null,
        "-0x8000000000000000" to Long.MIN_VALUE,
        "-0x8000000000000001" to null,
        "-9223372036854775808" to Long.MIN_VALUE,
        "-9223372036854775809" to null,
        "-0" to 0L,
        "+0" to 0L,
        "0b1" to null,
        "0o7" to null,
        "\t7\n" to 7L,
        "\u00857" to 7L,
        "\uFEFF7" to 7L,
        "\u200B7" to null,
        "7\u3000" to 7L,
        "00000000000000000000001" to 1L,
        "+" to null,
        "-" to null,
        " " to null,
        "0x_1" to null,
        "\u0663" to null,
        "-0xFFFFFFFFFFFFFFFF" to null,
        "+0xFFFFFFFFFFFFFFFF" to -1L,
        "0x00000000000000000001" to 1L,
        "0x0FFFFFFFFFFFFFFFF" to -1L,
        "-0x7FFFFFFFFFFFFFFF" to -9223372036854775807L,
        "-0x1" to -1L,
        "0x-1" to null,
        "0x+1" to null,
        "- 1" to null,
        "+ 1" to null,
        "0xa" to 10L,
        "0Xf" to 15L,
        "x10" to null,
        "00x10" to null,
        "1 2" to null,
        "\uD835\uDFCE" to null,
        "-00" to 0L,
        "0x 1" to null,
        "-0xFFFFFFFFFFFFFFFE" to null,
        "\u001C7" to null,
    )

    @Test
    fun `regex whitespace class equals Dart backslash-s over the whole BMP`() {
        val re = Regex("[${DartText.REGEX_WHITESPACE}]")
        val actual = (0..0xFFFF).filter { re.matches(it.toChar().toString()) }.toSet()
        assertEquals(dartRegexWhitespace, actual)
    }

    @Test
    fun `trim whitespace equals Dart String trim over the whole BMP`() {
        val actual = (0..0xFFFF).filter { DartText.isTrimWhitespace(it.toChar()) }.toSet()
        assertEquals(dartTrimWhitespace, actual)
        assertEquals("a", DartText.trim("\u0085﻿ a　 "))
        // Kotlin trim() strips U+001C..U+001F; Dart does not.
        assertEquals("\u001Ca\u001F", DartText.trim("\u001Ca\u001F"))
    }

    @Test
    fun `line terminator class equals Dart dot and multiline caret over the BMP`() {
        val re = Regex("[${DartText.REGEX_LINE_TERMINATORS}]")
        val actual = (0..0xFFFF).filter { re.matches(it.toChar().toString()) }.toSet()
        assertEquals(dartLineTerminators, actual)
    }

    @Test
    fun `intTryParse matches executed Dart int tryParse`() {
        val failures = intGoldens.mapNotNull { (input, expected) ->
            val actual = DartText.intTryParse(input)
            if (actual == expected) null else "<$input>: expected $expected got $actual"
        }
        assertEquals(failures.joinToString("\n"), 0, failures.size)
    }

    @Test
    fun `case mapping is simple per code point and keeps lone surrogates`() {
        assertEquals("ß", DartText.toUpperCase("ß"))
        assertEquals("ﬁ", DartText.toUpperCase("ﬁ"))
        assertEquals("οδοσ", DartText.toLowerCase("ΟΔΟΣ"))
        assertEquals("\uD801", DartText.toUpperCase("\uD801"))
        assertEquals("𐐨", DartText.toLowerCase("𐐀"))
        assertEquals("Ǆ", DartText.toUpperCase("ǆ"))
    }
}
