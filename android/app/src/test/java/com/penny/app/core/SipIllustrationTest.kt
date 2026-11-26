package com.penny.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * SipIllustration (frame 08): beginning-of-month contributions, monthly
 * compounding. The closed form is cross-checked against an independent
 * month-by-month simulation, and the three artboard headline values
 * (7% ₹29.6L · 10% ₹35.1L · 12% ₹39.5L, contributions ₹20.4L) are pinned.
 */
class SipIllustrationTest {

    /** Independent algorithm: contribute, then compound, month by month. */
    private fun simulate(monthly: Double, months: Int, annualRate: Double): Double {
        val i = annualRate / 12.0
        var balance = 0.0
        repeat(months) { balance = (balance + monthly) * (1.0 + i) }
        return balance
    }

    @Test
    fun `closed form equals the month-by-month simulation for the three rates`() {
        for (rate in listOf(0.07, 0.10, 0.12)) {
            val closed = SipIllustration.futureValue(17_000.0, 120, rate)
            val iterative = simulate(17_000.0, 120, rate)
            assertTrue(
                "closed=$closed iterative=$iterative rate=$rate",
                Math.abs(closed - iterative) <= Math.abs(iterative) * 1e-9,
            )
        }
    }

    @Test
    fun `the artboard dataset - 17000 for 10 years at the three rates`() {
        assertEquals("₹20.4L", Inr.lakh(SipIllustration.contributions(17_000.0, 120)))
        assertEquals("₹29.6L", Inr.lakh(SipIllustration.futureValue(17_000.0, 120, 0.07)))
        assertEquals("₹35.1L", Inr.lakh(SipIllustration.futureValue(17_000.0, 120, 0.10)))
        assertEquals("₹39.5L", Inr.lakh(SipIllustration.futureValue(17_000.0, 120, 0.12)))
        // Frame 08's exact footnote: 12% exact → ₹39,49,764.
        assertEquals("₹39,49,764", Inr.plain(Math.round(SipIllustration.futureValue(17_000.0, 120, 0.12)).toDouble()))
        assertEquals(2_040_000.0, SipIllustration.contributions(17_000.0, 120), 0.0)
    }

    @Test
    fun `a zero rate returns the contributions untouched`() {
        assertEquals(2_040_000.0, SipIllustration.futureValue(17_000.0, 120, 0.0), 0.0)
        assertEquals(0.0, SipIllustration.futureValue(17_000.0, 0, 0.12), 0.0)
        assertEquals(17_000.0, SipIllustration.futureValue(17_000.0, 1, 0.0), 0.0)
    }

    @Test
    fun `one month at 12 percent compounds the first contribution once`() {
        assertEquals(17_000.0 * 1.01, SipIllustration.futureValue(17_000.0, 1, 0.12), 1e-9)
    }
}
