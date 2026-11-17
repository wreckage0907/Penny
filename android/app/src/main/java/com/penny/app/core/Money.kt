package com.penny.app.core

import java.util.Locale

/**
 * Money display formatting, port of the Flutter app's `toStringAsFixed(2)`
 * usage (income_analysis.dart:244,628).
 *
 * Pure JVM (java.util only, no Android types) so the formatting rules are
 * unit-testable. Locale.US pins the '.' decimal separator, matching Dart on
 * every device locale.
 *
 * Known, deliberate divergence (recorded in migration/LEDGER.md): for
 * |value| >= 1e21 Dart's toStringAsFixed switches to exponential notation
 * ("1e+21") while Java's %f prints full digits. Unreachable for income and
 * expense amounts; revisited only if a slice ever renders such values.
 */
object Money {
    fun format(value: Double): String = String.format(Locale.US, "%.2f", value)
}