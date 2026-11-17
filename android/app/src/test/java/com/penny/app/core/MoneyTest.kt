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
        // 0.125 is exactly representable; Dart rounds away from zero, Java
        // HALF_UP agrees here. (Non-tie decimals like 0.135 are not exact
        // binary ties and round by their true double value in both.)
        assertEquals("0.13", Money.format(0.125))
        assertEquals("0.12", Money.format(0.124))
    }

    @Test
    fun `negative values and negative zero keep their sign`() {
        assertEquals("-1159.27", Money.format(-1_159.2740743000002))
        assertEquals("-0.00", Money.format(-0.0))
        assertEquals("0.00", Money.format(0.0))
    }
}