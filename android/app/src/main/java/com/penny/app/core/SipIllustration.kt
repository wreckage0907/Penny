package com.penny.app.core

/**
 * v2.1 SIP illustration (artboard frame 08 "10-Year SIP Illustration"):
 * beginning-of-month contributions, monthly compounding, three illustrative
 * annual rates. Dataset check from the artboard: ₹17,000/month for 10 years
 * → 7% ₹29.6L · 10% ₹35.1L · 12% ₹39.5L (exact ₹39,49,764), contributions
 * ₹20.4L.
 *
 * New behaviour (the Flutter analysis screen's 5-year/4-type projection is
 * superseded by this); "returns are not guaranteed" is the screen's copy.
 */
object SipIllustration {
    /** (label, annual rate) in the artboard's segmented-control order. */
    val RATES: List<Pair<String, Double>> = listOf(
        "FD 7%" to 0.07,
        "Hybrid 10%" to 0.10,
        "Index 12%" to 0.12,
    )
    const val DEFAULT_RATE_INDEX = 2 // "Index 12%" preselected (frame 08)

    /** Future value of `monthly` contributed at the START of each month. */
    fun futureValue(monthly: Double, months: Int, annualRate: Double): Double {
        val i = annualRate / 12.0
        if (i == 0.0) return contributions(monthly, months)
        return monthly * ((Math.pow(1.0 + i, months.toDouble()) - 1.0) / i) * (1.0 + i)
    }

    /** Total money put in (frame 08: "₹17,000/month · ₹20.4L contributed"). */
    fun contributions(monthly: Double, months: Int): Double = monthly * months
}
