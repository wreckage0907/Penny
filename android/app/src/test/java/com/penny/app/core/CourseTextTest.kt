package com.penny.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Characterisation of the course-flow text logic (course_page.dart:11-15,
 * :42-49, :63-66; learning_page.dart:29-30). Golden tables were emitted as
 * Kotlin literals by EXECUTING the verbatim Dart on Dart 3.14; `null`
 * expected = Dart threw RangeError.
 */
class CourseTextTest {

    private data class Golden(val label: String, val input: String, val expected: String?)

    private val capitalizeGoldens = listOf(
        Golden("empty", "", null /* throws RangeError */),
        Golden("single lower", "a", "A"),
        Golden("single upper", "A", "A"),
        Golden("word", "abc", "Abc"),
        Golden("already capital", "Already", "Already"),
        Golden("all caps", "ABC", "Abc"),
        Golden("two words", "Hello World", "Hello world"),
        Golden("leading space", " hello", " hello"),
        Golden("leading newline", "\nhello", "\nhello"),
        Golden("leading tab", "\thello", "\thello"),
        Golden("digit first", "1st", "1st"),
        Golden("digit only", "42", "42"),
        Golden("underscore first", "_x", "_x"),
        Golden("accented lower", "\u00E9cole", "\u00C9cole"),
        Golden("accented upper", "\u00C9COLE", "\u00C9cole"),
        Golden("german sharp s", "\u00DF", "\u00DF"),
        Golden("german word", "\u00DFeta", "\u00DFeta"),
        Golden("ligature fi", "\uFB01le", "\uFB01le"),
        Golden("turkish dotted I", "\u0130stanbul", "\u0130stanbul"),
        Golden("turkish dotless i", "\u0131rmak", "Irmak"),
        Golden("dz digraph", "\u01C6ungla", "\u01C4ungla"),
        Golden("emoji first (surrogate pair)", "\uD83D\uDE00abc", "\uD83D\uDE00abc"),
        Golden("emoji only", "\uD83D\uDE00", "\uD83D\uDE00"),
        Golden("emoji in middle", "a\uD83D\uDE00b", "A\uD83D\uDE00b"),
        Golden("cyrillic", "\u043F\u0440\u0438\u0432\u0435\u0442", "\u041F\u0440\u0438\u0432\u0435\u0442"),
        Golden("greek", "\u03B1\u03B2\u03B3", "\u0391\u03B2\u03B3"),
        Golden("combining accent first", "e\u0301cole", "E\u0301cole"),
        Golden("nbsp first", "\u00A0x", "\u00A0x"),
        Golden("mixed case word", "pErSoNaL", "Personal"),
        Golden("with digits after", "a1B2", "A1b2"),
        Golden("final sigma not applied", "a\u039F\u0394\u039F\u03A3", "A\u03BF\u03B4\u03BF\u03C3"),
        Golden("sigma first", "\u03C3\u039F\u0394\u039F\u03A3", "\u03A3\u03BF\u03B4\u03BF\u03C3"),
        Golden("sharp s then X", "\u00DFX", "\u00DFx"),
        Golden("ligature then X", "\uFB01X", "\uFB01x"),
        Golden("dotted capital I lowercases to plain i", "a\u0130", "Ai"),
        Golden("Deseret small first stays (surrogate split)", "\uD801\uDC28x", "\uD801\uDC28x"),
        Golden("Deseret capital in tail lowercases", "x\uD801\uDC00", "X\uD801\uDC28"),
        Golden("titlecase dz", "\u01C5X", "\u01C4x"),
    )

    private val moduleNameGoldens = listOf(
        Golden("typical", "1_personal_finance", "Personal Finance"),
        Golden("zero-padded", "01_banking_and_accounts", "Banking And Accounts"),
        Golden("drop-first only", "abc", ""),
        Golden("numeric only", "1", ""),
        Golden("empty", "", ""),
        Golden("leading underscore", "_x", "X"),
        Golden("trailing underscore", "1_personal_", null /* throws RangeError */),
        Golden("double underscore", "1_a__b", null /* throws RangeError */),
        Golden("all caps word", "1_ABC", "Abc"),
        Golden("mixed case", "1_pErSoNaL", "Personal"),
        Golden("three parts", "12_credit_and_debt_management", "Credit And Debt Management"),
        Golden("emoji part", "1_\uD83D\uDE00", "\uD83D\uDE00"),
        Golden("accented part", "1_\u00E9cole", "\u00C9cole"),
        Golden("spaces in part", "1_ personal finance", " personal finance"),
        Golden("hyphenated", "1_credit-and-debt", "Credit-and-debt"),
    )

    private val sortGoldens = listOf(
        listOf("10_c", "2_b", "1_a", "02_d") to
            listOf("1_a", "2_b", "02_d", "10_c"),
        listOf("2_second", "personal_finance", "1_first") to
            listOf("personal_finance", "1_first", "2_second"),
        listOf("2_b", "abc_x", "1_a") to
            listOf("abc_x", "1_a", "2_b"),
        listOf("2_b", "_leading", "1_a") to
            listOf("_leading", "1_a", "2_b"),
        listOf("1_a", "-3_minus", "2_b") to
            listOf("-3_minus", "1_a", "2_b"),
        listOf("+5_e", " 7_f", "02_g", "1_a") to
            listOf("1_a", "02_g", "+5_e", " 7_f"),
        listOf("1_a", "1.5_b", "0x10_c") to
            listOf("1.5_b", "1_a", "0x10_c"),
        listOf("1_a", "99999999999999999999_b", "2_c") to
            listOf("99999999999999999999_b", "1_a", "2_c"),
        listOf("1_c", "1_a", "1_b") to
            listOf("1_c", "1_a", "1_b"),
        listOf("2_b", "02_a") to
            listOf("2_b", "02_a"),
        listOf("", "2_b", "", "1_a", "") to
            listOf("1_a", "2_b"),
        listOf("3000000000_big", "10_j", "-1_z", "0_c", "abc_x", "1_a", "2_b", "2_a") to
            listOf("-1_z", "0_c", "abc_x", "1_a", "2_b", "2_a", "10_j", "3000000000_big"),
        listOf("12_credit_and_debt_management", "1_personal_finance", "10_stocks", "2_banking_and_accounts") to
            listOf("1_personal_finance", "2_banking_and_accounts", "10_stocks", "12_credit_and_debt_management"),
        emptyList<String>() to
            emptyList<String>(),
        listOf("") to
            emptyList<String>(),
        listOf("1_g00", "2_g01", "3_g02", "1_g03", "2_g04", "3_g05", "1_g06", "2_g07", "3_g08", "1_g09", "2_g10", "3_g11", "1_g12", "2_g13", "3_g14", "1_g15", "2_g16", "3_g17", "1_g18", "2_g19", "3_g20", "1_g21", "2_g22", "3_g23", "1_g24", "2_g25", "3_g26", "1_g27", "2_g28", "3_g29", "1_g30", "2_g31", "3_g32") to
            listOf("1_g00", "1_g03", "1_g06", "1_g09", "1_g12", "1_g15", "1_g18", "1_g21", "1_g24", "1_g27", "1_g30", "2_g01", "2_g04", "2_g07", "2_g10", "2_g13", "2_g16", "2_g19", "2_g22", "2_g25", "2_g28", "2_g31", "3_g02", "3_g05", "3_g08", "3_g11", "3_g14", "3_g17", "3_g20", "3_g23", "3_g26", "3_g29", "3_g32"),
    )

    private fun check(goldens: List<Golden>, f: (String) -> String) {
        val failures = goldens.mapNotNull { g ->
            // `null` golden = Dart threw RangeError: only our RangeError stand-in counts.
            val actual = try {
                f(g.input)
            } catch (e: IndexOutOfBoundsException) {
                if (e.message == DartText.EMPTY_INDEX_RANGE_ERROR) null else "<wrong error: ${e.message}>"
            }
            if (actual == g.expected) null else "${g.label}: expected <${g.expected}> got <$actual>"
        }
        assertEquals(failures.joinToString("\n"), 0, failures.size)
    }

    @Test
    fun `capitalize matches executed Dart`() = check(capitalizeGoldens, CourseText::capitalize)

    @Test
    fun `module display names match executed Dart`() =
        check(moduleNameGoldens, CourseText::moduleDisplayName)

    @Test
    fun `empty capitalize throws with Dart RangeError text`() {
        val e = assertThrows(IndexOutOfBoundsException::class.java) { CourseText.capitalize("") }
        assertEquals("RangeError (index): Invalid value: Valid value range is empty: 0", e.message)
        // getModuleNames inherits it for any empty `_` part.
        assertThrows(IndexOutOfBoundsException::class.java) { CourseText.moduleDisplayName("1_a__b") }
        assertThrows(IndexOutOfBoundsException::class.java) { CourseText.moduleDisplayName("1_x_") }
    }

    @Test
    fun `simple case mapping, not Kotlin full mapping`() {
        // Kotlin "ß".uppercase() == "SS" and "ΟΔΟΣ".lowercase() ends in final ς.
        assertEquals("ßx", CourseText.capitalize("ßX"))
        assertEquals("Aοδοσ", CourseText.capitalize("aΟΔΟΣ"))
        // Dart lowercases İ to plain i (Kotlin lowercase() gives i + U+0307).
        assertEquals("Ai", CourseText.capitalize("aİ"))
    }

    @Test
    fun `sortModules drops empties and orders by numeric prefix like Dart`() {
        val failures = sortGoldens.mapNotNull { (input, expected) ->
            val actual = CourseText.sortModules(input)
            if (actual == expected) null else "$input: expected $expected got $actual"
        }
        assertEquals(failures.joinToString("\n"), 0, failures.size)
    }

    @Test
    fun `ties keep input order, matching Dart insertion sort up to 33 items`() {
        val ties = List(33) { "1_${it.toString().padStart(3, '0')}" }
        assertEquals(ties, CourseText.sortModules(ties))
        // Backend /subfolder returns a Python set (hash-randomised order), so
        // the input order itself is not deterministic; see LEDGER 004.
    }

    @Test
    fun `sort keys use Dart int tryParse, missing prefix is 0`() {
        assertEquals(12L, CourseText.moduleSortKey("12_credit_and_debt_management"))
        assertEquals(0L, CourseText.moduleSortKey("personal_finance"))
        assertEquals(16L, CourseText.moduleSortKey("0x10_hex"))
        assertEquals(0L, CourseText.moduleSortKey(""))
        assertEquals(3_000_000_000L, CourseText.moduleSortKey("3000000000_big"))
    }

    @Test
    fun `lesson title and file name use first underscore part and 1-based index`() {
        assertEquals("Lesson 1", CourseText.lessonTitle(0))
        assertEquals("Lesson 8", CourseText.lessonTitle(7))
        assertEquals("1_1", CourseText.lessonFileName("1_personal_finance", 0))
        assertEquals("02_8", CourseText.lessonFileName("02_banking_and_accounts", 7))
        assertEquals("personal_3", CourseText.lessonFileName("personal", 2))
        assertEquals("_1", CourseText.lessonFileName("_x", 0))
    }
}
