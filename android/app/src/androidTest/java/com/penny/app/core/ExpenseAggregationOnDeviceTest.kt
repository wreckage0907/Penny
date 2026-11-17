package com.penny.app.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 005 parity re-run on ART: Android's java.math.BigDecimal is a different
 * implementation from the JDK's, and Money.format now depends on its exact
 * double expansion. All goldens were emitted by executing Dart 3.14.
 */
class ExpenseAggregationOnDeviceTest {

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
    fun moneyFormatMatchesDartToStringAsFixedOnArt() {
        val failures = fixedGoldens.mapNotNull { (v, expected) ->
            val actual = Money.format(v)
            if (actual == expected) null else "$v: expected $expected got $actual"
        }
        assertEquals(failures.joinToString("\n"), 0, failures.size)
    }

    @Test
    fun aggregationMatchesDartOnArt() {
        val payload = Json.parseToJsonElement(
            """{"2026":{"November":{"categories":{"Food":[{"name":"a","amount":0.1},""" +
                """{"name":"b","amount":0.1}],"Transport":[{"name":"c","amount":0.1},{"name":"d","amount":0.6}]}}}}""",
        ).jsonObject
        val state = ExpenseAggregation.onExpensesFetched(payload, 2026, "November")
        assertEquals(0.8999999999999999, state.totalAmount, 0.0)
        assertEquals("\$0.90", ExpenseDashboardText.amountLabel(state.totalAmount))
        assertEquals(-13L, ExpenseAggregation.barFlex(-0.125))
        assertEquals(268L, ExpenseAggregation.barFlex(2.675))
        val income = Json.parseToJsonElement("""{"2026":{"November":5000}}""").jsonObject
        assertEquals(0.0, ExpenseAggregation.incomeFor(income, 2026, "November"), 0.0)
    }
}
