package com.penny.app.core

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Characterisation tests for Money.format — the native stand-in for Dart's
 * toStringAsFixed(2). Goldens are the exact strings the Analysis screen (and
 * its e2e) renders for the slice-001 sample income chain.
 */
class MoneyTest {

    @Test
    fun `formats the slice-001 goldens exactly as the e2e expects`() {
        assertEquals("50000.00", Money.format(50_000.0))
        assertEquals("25000.00", Money.format(25_000.0))
        assertEquals("15000.00", Money.format(15_000.0))
        assertEquals("10000.00", Money.format(10_000.0))
        assertEquals("11592.74", Money.format(11_592.740743))
        assertEquals("13382.26", Money.format(13_382.255776))
        assertEquals("16105.10", Money.format(16_105.100000000008))
        assertEquals("17623.42", Money.format(17_623.41683200001))
    }

    @Test
    fun `rounds half up like Dart for exact binary ties`() {
        // 0.125 is exactly representable; Dart rounds ties away from zero.
        assertEquals("0.13", Money.format(0.125))
        assertEquals("0.12", Money.format(0.124))
    }

    @Test
    fun `negative values and negative zero keep their sign`() {
        assertEquals("-1159.27", Money.format(-1_159.2740743000002))
        assertEquals("-0.00", Money.format(-0.0))
        assertEquals("0.00", Money.format(0.0))
    }

    private val fixedGoldens = listOf(
        1.005 to "1.00",
        2.675 to "2.67",
        1.115 to "1.11",
        0.125 to "0.13",
        0.375 to "0.38",
        1.045 to "1.04",
        8.345 to "8.35",
        10.005 to "10.01",
        100.005 to "100.00",
        1234.565 to "1234.57",
        0.005 to "0.01",
        0.015 to "0.01",
        0.025 to "0.03",
        0.035 to "0.04",
        0.045 to "0.04",
        999999.995 to "999999.99",
        0.30000000000000004 to "0.30",
        -1.005 to "-1.00",
        -0.125 to "-0.13",
        -0.005 to "-0.01",
        -0.001 to "-0.00",
        -0.0 to "-0.00",
        0.0 to "0.00",
        1000000000000000.1 to "1000000000000000.13",
        9.995 to "9.99",
        19.995 to "20.00",
        0.995 to "0.99",
        100000000000000000000.0 to "100000000000000000000.00",
        123456789012345680.0 to "123456789012345680.00",
        5e-324 to "0.00",
        -5e-324 to "-0.00",
        0.8999999999999999 to "0.90",
        Double.NaN to "NaN",
        Double.POSITIVE_INFINITY to "Infinity",
        Double.NEGATIVE_INFINITY to "-Infinity",
        50000.0 to "50000.00",
        11592.740743 to "11592.74",
        16105.100000000008 to "16105.10",
        4.35 to "4.35",
        0.285 to "0.28",
        1.275 to "1.27",
        33.335 to "33.34",
    )

    @Test
    fun `rounds the exact binary value like Dart toStringAsFixed`() {
        // Emitted by executing Dart 3.14. Java's %.2f (the 001 implementation)
        // rounded the shortest decimal repr: 1.005 -> "1.01", Dart "1.00".
        val failures = fixedGoldens.mapNotNull { (v, expected) ->
            val actual = Money.format(v)
            if (actual == expected) null else "$v: expected $expected got $actual"
        }
        assertEquals(failures.joinToString("\n"), 0, failures.size)
    }
}
