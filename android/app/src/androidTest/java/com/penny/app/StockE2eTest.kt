package com.penny.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.penny.app.ui.PennyApp
import org.junit.Rule
import org.junit.Test

/** 014: the Market page (stock_profile.dart) with local-only trading on the fake market. */
class StockE2eTest {

    @get:Rule
    val compose = createComposeRule()

    private fun open() {
        compose.setContent { PennyApp(PennyServices.fakes(signedIn = true)) }
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("home-page") }
        compose.onNodeWithTag("home-tile-2").performScrollTo().performClick()
        compose.onNodeWithTag("stock-page").assertIsDisplayed()
    }

    private fun trade(kind: String, quantity: String) {
        compose.onNodeWithTag("stock-$kind").performScrollTo().performClick()
        compose.onNodeWithTag("trade-quantity").performTextInput(quantity)
        compose.onNodeWithTag("trade-confirm").performClick()
    }

    private fun holdings() = compose.onNodeWithTag("stock-holdings").performScrollTo()

    @Test
    fun hardCodedCardAndEmptyHoldings() {
        open()
        compose.onNodeWithTag("stock-header").assertTextEquals("AAPL - Apple INC.")
        compose.onNodeWithTag("stock-price").assertTextEquals("\$387")
        compose.onNodeWithTag("stock-open").assertTextEquals("\$133")
        holdings().assertTextEquals("Your Holdings: 0.0 shares")
    }

    @Test
    fun buyThenOversellThenSellToZero() {
        open()
        trade("buy", "2")
        compose.waitUntil(5_000) { compose.onAllNodesWithTextExists("Bought 2.0 shares of AAPL") }
        holdings().assertTextEquals("Your Holdings: 2.0 shares")
        trade("sell", "5")
        compose.waitUntil(5_000) { compose.onAllNodesWithTextExists("Not enough shares to sell") }
        trade("sell", "2")
        compose.waitUntil(5_000) { compose.onAllNodesWithTextExists("Sold 2.0 shares of AAPL") }
        holdings().assertTextEquals("Your Holdings: 0.0 shares")
    }

    @Test
    fun searchSelectsAnotherTickerAndRangesSwitch() {
        open()
        compose.onNodeWithTag("stock-search").performClick()
        compose.onNodeWithTag("search-query").performTextInput("nike")
        compose.onNodeWithTag("search-result-NKE").performClick()
        compose.onNodeWithTag("stock-header").assertTextEquals("NKE - Nike INC.")
        compose.onNodeWithTag("stock-range-1W").performScrollTo().performClick()
        compose.onNodeWithTag("stock-chart").assertIsDisplayed()
    }
}
