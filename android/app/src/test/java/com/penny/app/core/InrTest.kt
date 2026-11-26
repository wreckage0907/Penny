package com.penny.app.core

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Inr (v2.1 money display): en-IN grouping, artboard signs, lakh forms,
 * and the two share formats the frames use.
 */
class InrTest {

    @Test
    fun `plain amounts group en-IN and keep fractions only when present`() {
        assertEquals("₹85,000", Inr.plain(85_000.0))
        assertEquals("₹53,550", Inr.plain(53_550.0))
        assertEquals("₹12,34,567", Inr.plain(1_234_567.0))
        assertEquals("₹1,234.50", Inr.plain(1_234.5))
        assertEquals("₹100", Inr.plain(100.0))
        assertEquals("₹0", Inr.plain(0.0))
        assertEquals("−₹31,450", Inr.plain(-31_450.0))
        assertEquals("₹39,49,764", Inr.plain(3_949_764.0))
    }

    @Test
    fun `signed amounts carry the artboard plus and minus`() {
        assertEquals("+₹53,550", Inr.signed(53_550.0))
        assertEquals("−₹31,450", Inr.signed(-31_450.0))
        assertEquals("₹0", Inr.signed(0.0))
        assertEquals("+₹85,000", Inr.signed(85_000.0))
    }

    @Test
    fun `spending rows always show the minus`() {
        assertEquals("−₹11,200", Inr.spent(11_200.0))
        assertEquals("−₹200", Inr.spent(200.0))
    }

    @Test
    fun `lakh short forms match the artboard chart`() {
        assertEquals("₹29.6L", Inr.lakh(2_959_500.0))
        assertEquals("₹35.1L", Inr.lakh(3_511_382.0))
        assertEquals("₹39.5L", Inr.lakh(3_949_764.0))
        assertEquals("₹20.4L", Inr.lakh(2_040_000.0))
    }

    @Test
    fun `shares round half away with the two frame formats`() {
        assertEquals("63%", Inr.share0(53_550.0, 85_000.0))
        assertEquals("37%", Inr.share0(31_450.0, 85_000.0))
        assertEquals("0%", Inr.share0(0.0, 85_000.0))
        assertEquals("0%", Inr.share0(50.0, 0.0)) // no denominator → honest zero
        assertEquals("35.6%", Inr.share1(11_200.0, 31_450.0))
        assertEquals("30.2%", Inr.share1(9_500.0, 31_450.0))
        assertEquals("17.3%", Inr.share1(5_450.0, 31_450.0))
        assertEquals("9.9%", Inr.share1(3_100.0, 31_450.0))
        assertEquals("7.0%", Inr.share1(2_200.0, 31_450.0))
    }

    @Test
    fun `indian grouping is last-3 then pairs`() {
        assertEquals("123", Inr.indianGroup("123"))
        assertEquals("1,234", Inr.indianGroup("1234"))
        assertEquals("12,34,567", Inr.indianGroup("1234567"))
        assertEquals("1,00,00,000", Inr.indianGroup("10000000"))
    }
}
