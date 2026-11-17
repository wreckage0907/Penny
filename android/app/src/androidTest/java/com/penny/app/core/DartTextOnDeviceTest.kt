package com.penny.app.core

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The 004 text ports re-run on ART. On-device `java.util.regex` is backed by
 * ICU (not the JDK engine the JVM tests use), and `Character` case mapping
 * follows ICU's Unicode version, so the Dart-semantics regexes (lookbehind
 * line starts, explicit `\uXXXX` whitespace classes) are proven here too.
 * Goldens are a discriminating subset of the Dart-executed JVM tables.
 */
class DartTextOnDeviceTest {

    private data class Golden(val label: String, val input: String, val expected: String)

    private val cleanGoldens = listOf(
        Golden("header + nbsp", "##\u00A0NBSP header", "NBSP header"),
        Golden("nested parens", "a (b (c) d) e", "a  d) e"),
        Golden("paren spanning newline", "a (b\nc) d", "a (b\nc) d"),
        Golden("unordered star", "* First\n* Second", "First\n Second"),
        Golden("CRLF list", "- a\r\n- b", "a\rb"),
        Golden("bom prefix", "\uFEFFbom text", "bom text"),
        Golden("everything at once", "# **Bold** _hdr_ with [link](u) and `code`\n- item *one*\n1. ordered\n> quote\n![i](p.png)", "Bold _hdr_ with link and code\nitem one\n1. ordered\n> quote\n!i"),
        Golden("heading with hash text", "# C# basics", "Cbasics"),
        Golden("CR alone is a line start", "a\r- b", "a\rb"),
        Golden("U+2028 is a line start", "a\u2028- b", "a\u2028b"),
        Golden("U+0085 is NOT a line start", "a\u0085- b", "a\u0085- b"),
        Golden("dot crosses U+0085", "(a\u0085b)", ""),
        Golden("dot stops at U+2028", "(a\u2028b)", "(a\u2028b)"),
        Golden("U+0085 is not regex whitespace", "##\u0085x", "##\u0085x"),
        Golden("BOM is regex whitespace", "##\uFEFFx", "x"),
        Golden("trim strips U+0085", "\u0085text\u0085", "text"),
        Golden("U+001C is not whitespace (Kotlin trim would strip)", "\u001Ctext\u001C", "\u001Ctext\u001C"),
        Golden("ideographic space after star", "*\u3000x", "x"),
        Golden("nested CRLF list", "- a\r\n  - b\r\n", "a\rb"),
    )

    @Test
    fun cleanMarkdownTextMatchesDartOnArt() {
        val failures = cleanGoldens.mapNotNull { g ->
            val actual = LessonText.cleanMarkdownText(g.input)
            if (actual == g.expected) null else "${g.label}: expected <${g.expected}> got <$actual>"
        }
        assertEquals(failures.joinToString("\n"), 0, failures.size)
    }

    @Test
    fun regexClassesMatchDartSetsOnArt() {
        val ws = Regex("[${DartText.REGEX_WHITESPACE}]")
        val expectedWs = setOf(0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x20, 0xA0, 0x1680) +
            (0x2000..0x200A) + setOf(0x2028, 0x2029, 0x202F, 0x205F, 0x3000, 0xFEFF)
        assertEquals(expectedWs, (0..0xFFFF).filter { ws.matches(it.toChar().toString()) }.toSet())
        val lt = Regex("[${DartText.REGEX_LINE_TERMINATORS}]")
        assertEquals(
            setOf(0x0A, 0x0D, 0x2028, 0x2029),
            (0..0xFFFF).filter { lt.matches(it.toChar().toString()) }.toSet(),
        )
    }

    @Test
    fun courseTextMatchesDartOnArt() {
        assertEquals("Credit And Debt Management", CourseText.moduleDisplayName("12_credit_and_debt_management"))
        assertEquals("ßx", CourseText.capitalize("ßX"))
        assertEquals("Aοδοσ", CourseText.capitalize("aΟΔΟΣ"))
        assertEquals("Ai", CourseText.capitalize("aİ"))
        assertEquals("École", CourseText.capitalize("ÉCOLE"))
        assertEquals(
            listOf("personal_finance", "1_a", "2_b", "0x10_c"),
            CourseText.sortModules(listOf("0x10_c", "", "2_b", "personal_finance", "1_a")),
        )
        assertEquals(-1L, DartText.intTryParse("0xFFFFFFFFFFFFFFFF"))
        assertEquals(null, DartText.intTryParse("٣"))
    }
}
