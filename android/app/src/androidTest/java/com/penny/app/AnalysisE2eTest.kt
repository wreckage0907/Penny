package com.penny.app

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * E2E for migration slice 1: the ported budget math, rendered on a real
 * device/emulator. Expected strings come from the same goldens as the JVM
 * tests (income 50000 -> savings 10000 -> 5-year projections).
 * Since 006 the app launches into the auth shell, so the screen is rendered
 * directly (same theme, same assertions).
 */
class AnalysisE2eTest {

    @get:Rule
    val compose = createComposeRule()

    @Before
    fun render() {
        compose.setContent { MaterialTheme { AnalysisScreen(sampleIncome = 50_000.0) } }
    }

    @Test
    fun splitAndProjectionShowForSampleIncome() {
        compose.onNodeWithTag("analysis-title").assertTextEquals("Penny — Analysis")
        compose.onNodeWithTag("income").assertTextEquals("Income: 50000.00")
        compose.onNodeWithTag("essentials").assertTextEquals("Essentials (50%): 25000.00")
        compose.onNodeWithTag("wants").assertTextEquals("Wants (30%): 15000.00")
        compose.onNodeWithTag("savings").assertTextEquals("Savings (20%): 10000.00")
        compose.onNodeWithTag("projection-title").assertTextEquals("5-year projection of savings")
        compose.onNodeWithTag("projection-0").assertTextEquals("Fixed Deposits: 11592.74")
        compose.onNodeWithTag("projection-1").assertTextEquals("Mutual Funds: 13382.26")
        compose.onNodeWithTag("projection-2").assertTextEquals("Stocks: 16105.10")
        compose.onNodeWithTag("projection-3").assertTextEquals("Gold: 17623.42")
    }
}
