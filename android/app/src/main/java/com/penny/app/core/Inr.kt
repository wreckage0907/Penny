package com.penny.app.core

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * v2.1 money display (penny-v21-artboard "Money" rule: INR/en-IN, explicit
 * +/−, tabular figures). Replaces the Flutter `\$x.toStringAsFixed(2)`
 * labels ([Money] stays for the Dart parity surfaces it still serves).
 *
 * Artboard examples: "₹85,000", "+₹53,550", "−₹31,450", "₹39,49,764",
 * "₹29.6L". Indian grouping (last 3 digits, then pairs), no decimals for
 * whole rupees, two for fractions. The minus sign is U+2212 as in the
 * artboard, not the ASCII hyphen.
 */
object Inr {
    const val MINUS = "−" // U+2212 MINUS SIGN
    const val PLUS = "+"
    const val SYMBOL = "₹"

    /** "₹85,000" / "₹1,23,456.50"; non-finite prints like Dart ("NaN", …). */
    fun plain(amount: Double): String {
        if (amount.isNaN()) return "NaN"
        if (amount == Double.POSITIVE_INFINITY) return "Infinity"
        if (amount == Double.NEGATIVE_INFINITY) return "-Infinity"
        val scaled = BigDecimal(amount).setScale(2, RoundingMode.HALF_UP)
        val negative = scaled.signum() < 0
        val abs = scaled.abs()
        val whole = abs.setScale(0, RoundingMode.DOWN).toPlainString()
        val fraction = abs.subtract(abs.setScale(0, RoundingMode.DOWN))
        val text = SYMBOL + indianGroup(whole) +
            (if (fraction.signum() == 0) "" else fraction.toPlainString().removePrefix("0"))
        return if (negative) MINUS + text else text
    }

    /** "+₹53,550" for positive, "−₹31,450" for negative, "₹0" for zero. */
    fun signed(amount: Double): String = when {
        amount > 0.0 -> PLUS + plain(amount)
        amount < 0.0 -> plain(amount) // plain already prefixes −
        else -> plain(0.0)
    }

    /** A spending row: stored positive, rendered with the artboard's − ("−₹11,200"). */
    fun spent(amount: Double): String = MINUS + plain(amount)

    /** "₹29.6L" — lakh with exactly one decimal ("₹20.4L", "₹39.5L"). */
    fun lakh(amount: Double): String = SYMBOL + oneDecimal(amount / 100_000.0) + "L"

    /** "63% of inflow" style: integer share, half away from zero. */
    fun share0(numerator: Double, denominator: Double): String =
        if (denominator <= 0.0) "0%" else BigDecimal(numerator / denominator * 100.0)
            .setScale(0, RoundingMode.HALF_UP).toPlainString() + "%"

    /** "35.6%" style: one decimal kept even when zero ("7.0%"). */
    fun share1(numerator: Double, denominator: Double): String =
        if (denominator <= 0.0) "0.0%" else BigDecimal(numerator / denominator * 100.0)
            .setScale(1, RoundingMode.HALF_UP).toPlainString() + "%"

    private fun oneDecimal(value: Double): String =
        BigDecimal(value).setScale(1, RoundingMode.HALF_UP).toPlainString()

    /** "1234567" → "12,34,567" (en-IN: last 3, then groups of 2). */
    internal fun indianGroup(digits: String): String {
        if (digits.length <= 3) return digits
        val groups = mutableListOf<String>()
        var rest = digits
        groups.add(rest.takeLast(3))
        rest = rest.dropLast(3)
        while (rest.isNotEmpty()) {
            groups.add(rest.takeLast(2))
            rest = rest.dropLast(2)
        }
        return groups.reversed().joinToString(",")
    }
}
