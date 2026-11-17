package com.penny.app.core

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/** Stock page logic (stock_profile.dart). Texts/sequences executed on Dart 3.14 (dart014/st.dart, dst.dart). */
class StockLogicTest {

    @Test
    fun `holdings and trade texts follow the executed Dart sequence`() {
        var h = emptyMap<String, Double>()
        assertEquals("Your Holdings: 0.0 shares", StockLogic.holdingsText(h, "AAPL")) // `?? 0` prints 0.0
        fun buy(t: String, q: Double): String = StockLogic.buy(h, t, q).also { h = it.holdings }.message
        fun sell(t: String, q: Double): String = StockLogic.sell(h, t, q).also { h = it.holdings }.message
        assertEquals("Bought 2.0 shares of AAPL", buy("AAPL", 2.0))
        assertEquals("Bought 0.5 shares of AAPL", buy("AAPL", 0.5))
        assertEquals("Sold 2.5 shares of AAPL", sell("AAPL", 2.5))
        assertEquals("Your Holdings: 0.0 shares", StockLogic.holdingsText(h, "AAPL"))
        assertEquals(false, h.containsKey("AAPL")) // exactly 0 is removed
        buy("AAPL", 0.1); buy("AAPL", 0.2)
        assertEquals("Your Holdings: 0.30000000000000004 shares", StockLogic.holdingsText(h, "AAPL"))
        assertEquals("Sold 0.30000000000000004 shares of AAPL", sell("AAPL", 0.30000000000000004))
        assertEquals("Not enough shares to sell", sell("TSLA", 1.0))
        assertEquals("Sold -3.0 shares of TSLA", sell("TSLA", -3.0)) // negative sells "up"
        assertEquals("Your Holdings: 3.0 shares", StockLogic.holdingsText(h, "TSLA"))
        assertEquals("Bought -1.0 shares of NKE", buy("NKE", -1.0))
        assertEquals("Bought 1e+21 shares of DIS", buy("DIS", 1e21))
        assertEquals("Bought NaN shares of SBUX", buy("SBUX", Double.NaN))
        assertEquals("Not enough shares to sell", sell("SBUX", Double.NaN))
        assertEquals(true, StockLogic.sell(h, "SBUX", Double.NaN).error)
        assertEquals(0.0, StockLogic.quantity("abc"), 0.0)
    }

    @Test
    fun `hard-coded prices and search`() {
        assertEquals(listOf("\$387", "\$133", "\$129", "\$333"), listOf(StockLogic.PRICE, StockLogic.OPEN, StockLogic.HIGH, StockLogic.LOW))
        assertEquals("AAPL - Apple INC.", StockLogic.header("AAPL"))
        assertEquals(StockLogic.TICKERS, StockLogic.search(""))
        assertEquals(listOf("NKE"), StockLogic.search("nike"))
        // Executed Dart (dart014/se.dart): every "… INC." name matches.
        assertEquals(listOf("AAPL", "NKE", "TSLA", "AMZN", "GOOGL", "PFE"), StockLogic.search("inc"))
        assertEquals(listOf("SBUX", "INTC"), StockLogic.search("corp"))
        assertEquals(listOf("^NSEI"), StockLogic.search("^n"))
    }

    @Test
    fun `date ranges subtract 24-hour days, so DST shifts the start (executed under America New_York)`() {
        val ny = ZoneId.of("America/New_York")
        val now = ZonedDateTime.of(2026, 3, 9, 0, 30, 0, 0, ny)
        assertEquals("2026-03-07" to "2026-03-09", StockLogic.dateRange("1D", now))
        assertEquals("2026-03-01" to "2026-03-09", StockLogic.dateRange("1W", now))
        assertEquals("2026-02-06" to "2026-03-09", StockLogic.dateRange("1M", now))
        assertEquals("2026-11-01", StockLogic.dateRange("1D", ZonedDateTime.of(2026, 11, 2, 23, 30, 0, 0, ny)).first)
        assertEquals("2023-11-11", StockLogic.dateRange("3Y", ZonedDateTime.of(2026, 11, 10, 12, 0, 0, 0, ny)).first)
    }

    @Test
    fun `points keep wire order, int Close aborts the update, bounds use first and last x`() {
        val utc = ZoneId.of("UTC")
        val ok = Json.parseToJsonElement(
            """{"2026-05-15 09:30:00":{"Close":10.5},"2026-05-15 09:31:00":{"Close":9.0},"2026-05-15 09:29:00":{"Close":12.0}}""",
        ).jsonObject
        val pts = StockLogic.points(ok, utc)!!
        assertEquals(listOf(10.5, 9.0, 12.0), pts.map { it.y })
        val b = StockLogic.bounds(pts)
        assertEquals(pts.first().x, b.minX, 0.0)
        assertEquals(pts.last().x, b.maxX, 0.0) // not the max x
        assertEquals(9.0, b.minY, 0.0)
        assertEquals(12.0, b.maxY, 0.0)
        assertNull(StockLogic.points(Json.parseToJsonElement("""{"2026-05-15 09:30:00":{"Close":10}}""").jsonObject, utc))
        assertEquals(StockLogic.Bounds(0.0, 0.0, 0.0, 0.0), StockLogic.bounds(emptyList()))
    }

    @Test
    fun `fake market is deterministic, chronological, and empty for unknown tickers`() = runBlocking {
        val market = FakeStockMarket { LocalDate.of(2026, 5, 15) }
        val live = market.live("AAPL")
        assertEquals(390, live.size)
        assertEquals("2026-05-15 09:30:00", live.keys.first())
        assertEquals(live, market.live("AAPL"))
        assertEquals("{}", market.live("NOPE").toString())
        val hist = market.historical("TSLA", "2026-05-01", "2026-05-15") // end exclusive, weekdays only
        assertEquals(10, hist.size)
        assertEquals(10, StockLogic.points(hist)!!.size)
    }
}
