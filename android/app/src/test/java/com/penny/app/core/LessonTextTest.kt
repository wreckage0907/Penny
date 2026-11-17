package com.penny.app.core

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Characterisation of `cleanMarkdownText` (lesson_page.dart:53-66), the TTS
 * cleaner. Every expected value below was produced by EXECUTING the verbatim
 * Dart function on Dart 3.14 and emitting Kotlin literals (no hand-typed
 * goldens): 90 markdown cases + 22 Dart-vs-Java regex/whitespace
 * discriminators (CR-as-line-start, U+0085, BOM, NBSP, U+001C, ...).
 */
class LessonTextTest {

    private data class Golden(val label: String, val input: String, val expected: String?)

    private val cleanGoldens = listOf(
        Golden("empty", "", ""),
        Golden("whitespace-only spaces", "   ", ""),
        Golden("whitespace-only mixed", " \n\t\r\n ", ""),
        Golden("h1", "# Heading", "Heading"),
        Golden("h2", "## Heading 2", "Heading 2"),
        Golden("h3", "### Heading 3", "Heading 3"),
        Golden("h4", "#### H4", "H4"),
        Golden("h5", "##### H5", "H5"),
        Golden("h6", "###### H6", "H6"),
        Golden("h6 + text", "###### deep header text", "deep header text"),
        Golden("hashes, no space (7)", "####### seven", "seven"),
        Golden("bare hashes no space", "###", "###"),
        Golden("bare hashes + trailing space", "### ", ""),
        Golden("hash no space word", "#Heading", "#Heading"),
        Golden("C# prose", "C# is fun", "Cis fun"),
        Golden("C# then space", "I like C# a lot", "I like Ca lot"),
        Golden("hash + digit (no ws)", "#5 item", "#5 item"),
        Golden("hash inside sentence", "a # b # c", "a b c"),
        Golden("hash + tab", "#\tTab header", "Tab header"),
        Golden("hash + newline (header then body)", "###\nHeader", "Header"),
        Golden("header + nbsp", "##\u00A0NBSP header", "NBSP header"),
        Golden("bold **", "**bold**", "bold"),
        Golden("italic *", "*italic*", "italic"),
        Golden("bold+italic ***", "***both***", "both"),
        Golden("4 stars", "****four****", "four"),
        Golden("underscore bold", "__bold__", "__bold__"),
        Golden("underscore italic", "_italic_", "_italic_"),
        Golden("strike", "~~strike~~", "~~strike~~"),
        Golden("lone stars spaced", "a * b * c", "a  b  c"),
        Golden("nested bold/italic", "**bold *nested italic* bold**", "bold nested italic bold"),
        Golden("escaped star", "literal \\* star", "literal \\ star"),
        Golden("inline code", "`code`", "code"),
        Golden("double backtick", "``x``", "x"),
        Golden("fenced code (no lang)", "```\nval x = 1\n```", "val x = 1"),
        Golden("fenced code (lang)", "```kotlin\nval x = 1\n```", "kotlin\nval x = 1"),
        Golden("4 backticks", "````x````", "x"),
        Golden("fence containing header", "```\n# not a header\n```", "not a header"),
        Golden("fence containing bold", "```\n**not bold**\n```", "not bold"),
        Golden("inline + fenced mixed", "Run `a` then:\n```\nb\n```", "Run a then:\n\nb"),
        Golden("markdown link", "[link](https://x.com)", "link"),
        Golden("link with title", "[link](https://x.com \"Title\")", "link"),
        Golden("two links", "[a](b) and [c](d)", "a and c"),
        Golden("link + trailing text", "see [docs](u) now", "see docs now"),
        Golden("ref-style link text only", "[no target]", "no target"),
        Golden("bare brackets", "a [b] c", "a b c"),
        Golden("image", "![alt](img.png)", "!alt"),
        Golden("image + link", "![alt](i.png) [t](u)", "!alt t"),
        Golden("prose parenthesis", "text (see chapter 2) more", "text  more"),
        Golden("math parens", "f(x) = y", "f = y"),
        Golden("nested parens", "a (b (c) d) e", "a  d) e"),
        Golden("double parens", "((x))", ")"),
        Golden("paren with quoted paren in title", "[t](u \"a(b)c\")", "tc\")"),
        Golden("unclosed paren", "text (unclosed", "text (unclosed"),
        Golden("paren spanning newline", "a (b\nc) d", "a (b\nc) d"),
        Golden("html bold", "<b>bold</b>", "<b>bold</b>"),
        Golden("html + entities", "<p>&amp; &nbsp;</p>", "<p>&amp; &nbsp;</p>"),
        Golden("html div block", "<div>\nhello\n</div>", "<div>\nhello\n</div>"),
        Golden("blockquote", "> quoted line", "> quoted line"),
        Golden("blockquote multi", "> one\n> two", "> one\n> two"),
        Golden("ordered list", "1. First\n2. Second", "1. First\n2. Second"),
        Golden("ordered list paren", "1) First\n2) Second", "1) First\n2) Second"),
        Golden("unordered dash", "- First\n- Second", "First\nSecond"),
        Golden("unordered star", "* First\n* Second", "First\n Second"),
        Golden("unordered plus", "+ First\n+ Second", "+ First\n+ Second"),
        Golden("indented dash list", "  - indented item", "indented item"),
        Golden("dash item no space", "-item", "-item"),
        Golden("dash inside line", "a - b", "a - b"),
        Golden("list after paragraph", "Intro\n   - bullet", "Intro\nbullet"),
        Golden("list preceded by blank lines", "\n\n- item", "item"),
        Golden("double blank then bullet mid", "a\n\n- b", "a\nb"),
        Golden("bullet alone then text", "- \n- x", "x"),
        Golden("lone dash", "-", "-"),
        Golden("table", "| a | b |\n|---|---|\n| 1 | 2 |", "| a | b |\n|---|---|\n| 1 | 2 |"),
        Golden("unicode/emoji", "\uD83D\uDE00 caf\u00E9 \u65E5\u672C\u8A9E", "\uD83D\uDE00 caf\u00E9 \u65E5\u672C\u8A9E"),
        Golden("emoji + header", "## \uD83D\uDE00 Emoji header", "\uD83D\uDE00 Emoji header"),
        Golden("emoji + bold", "**\uD83D\uDE00**", "\uD83D\uDE00"),
        Golden("CRLF header + body", "\r\n# H\r\n\r\ntext", "H\r\n\r\ntext"),
        Golden("CRLF list", "- a\r\n- b", "a\rb"),
        Golden("CRLF plain", "line1\r\nline2", "line1\r\nline2"),
        Golden("multiple spaces internal", "a  b", "a  b"),
        Golden("multiple newlines internal", "a\n\n\n\nb", "a\n\n\n\nb"),
        Golden("trailing spaces", "text with trailing   ", "text with trailing"),
        Golden("leading/trailing newlines", "\n\ntext\n\n", "text"),
        Golden("nbsp padding", "\u00A0text\u00A0", "text"),
        Golden("bom prefix", "\uFEFFbom text", "bom text"),
        Golden("zero-width space", "\u200Btext\u200B", "\u200Btext\u200B"),
        Golden("everything at once", "# **Bold** _hdr_ with [link](u) and `code`\n- item *one*\n1. ordered\n> quote\n![i](p.png)", "Bold _hdr_ with link and code\nitem one\n1. ordered\n> quote\n!i"),
        Golden("asterisk bullet then dash line", "* a\n- b", "a\nb"),
        Golden("header level then bold", "## **T**", "T"),
        Golden("heading with hash text", "# C# basics", "Cbasics"),
        Golden("CR alone is a line start", "a\r- b", "a\rb"),
        Golden("U+2028 is a line start", "a\u2028- b", "a\u2028b"),
        Golden("U+2029 is a line start", "a\u2029- b", "a\u2029b"),
        Golden("U+0085 is NOT a line start", "a\u0085- b", "a\u0085- b"),
        Golden("dot crosses U+0085", "(a\u0085b)", ""),
        Golden("dot stops at U+2028", "(a\u2028b)", "(a\u2028b)"),
        Golden("dot stops at CR", "(a\rb)", "(a\rb)"),
        Golden("U+0085 is not regex whitespace", "##\u0085x", "##\u0085x"),
        Golden("BOM is regex whitespace", "##\uFEFFx", "x"),
        Golden("nbsp after dash marker", "-\u00A0x", "x"),
        Golden("nbsp indent before marker", "\u00A0\u00A0- x", "x"),
        Golden("CRLF blank lines then bullet", "a\r\n\r\n- b", "a\rb"),
        Golden("trim strips U+0085", "\u0085text\u0085", "text"),
        Golden("U+180E is not whitespace", "\u180Etext", "\u180Etext"),
        Golden("VT after hash", "#\u000Bx", "x"),
        Golden("U+001C is not whitespace (Kotlin trim would strip)", "\u001Ctext\u001C", "\u001Ctext\u001C"),
        Golden("FS after hash is not whitespace", "#\u001Cx", "#\u001Cx"),
        Golden("star bullet after newline leaves space", "x\n* y", "x\n y"),
        Golden("ideographic space after star", "*\u3000x", "x"),
        Golden("nested CRLF list", "- a\r\n  - b\r\n", "a\rb"),
        Golden("ZWSP padding is kept", "\u200Btext\u200B", "\u200Btext\u200B"),
        Golden("U+2007 figure space trimmed", "\u2007text\u2007", "text"),
    )

    @Test
    fun `cleanMarkdownText matches executed Dart on every golden`() {
        val failures = cleanGoldens.mapNotNull { g ->
            val actual = LessonText.cleanMarkdownText(g.input)
            if (actual == g.expected) null else "${g.label}: expected <${g.expected}> got <$actual>"
        }
        assertEquals(failures.joinToString("\n"), 0, failures.size)
    }

    @Test
    fun `quirks are preserved, not fixed`() {
        // `#+\s` eats "C# " mid-sentence.
        assertEquals("Cbasics", LessonText.cleanMarkdownText("# C# basics"))
        // Stars are stripped BEFORE the list step, so `* ` bullets keep a space.
        assertEquals("First\n Second", LessonText.cleanMarkdownText("* First\n* Second"))
        // Any one-line parenthetical is dropped, lazily: nested parens leak `)`.
        assertEquals("a  d) e", LessonText.cleanMarkdownText("a (b (c) d) e"))
        // CRLF: Dart `^` fires between \r and \n, eating the \n.
        assertEquals("a\rb", LessonText.cleanMarkdownText("- a\r\n- b"))
    }

    @Test
    fun `golden table covers every case the Dart harness ran`() {
        assertEquals(112, cleanGoldens.size)
        assertEquals(cleanGoldens.size, cleanGoldens.map { it.label }.toSet().size)
    }
}
