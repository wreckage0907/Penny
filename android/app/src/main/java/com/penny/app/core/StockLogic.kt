package com.penny.app.core

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Market data seam: `GET /stocks/live?ticker=` and `GET /stocks/historical?
 * ticker=&start_date=&end_date=` (backend/routers/livestock.py, yfinance).
 * Both return `{"YYYY-MM-DD HH:MM:SS": {"Open","High","Low","Close","Volume",
 * …}}` in chronological order; an unknown ticker is an empty object.
 */
interface StockMarket {
    suspend fun live(ticker: String): JsonObject
    suspend fun historical(ticker: String, startDate: String, endDate: String): JsonObject
}

/**
 * Deterministic synthetic market (the real data is yfinance): a seeded random
 * walk per ticker — minute bars from 09:30 for today's "live" session, daily
 * weekday bars for historical ranges. Unknown tickers → `{}` like yfinance.
 */
class FakeStockMarket(private val today: () -> LocalDate = { LocalDate.now() }) : StockMarket {

    private fun walk(ticker: String, n: Int, salt: Long): List<Double> {
        var seed = ticker.fold(1125899906842597L) { h, c -> 31 * h + c.code } xor salt
        var price = 100.0 + (Math.floorMod(seed, 400L))
        return List(n) {
            seed = seed * 6364136223846793005L + 1442695040888963407L
            price *= 1 + ((seed ushr 33) % 2001 - 1000) / 100_000.0
            Math.round(price * 100) / 100.0
        }
    }

    private fun bars(ticker: String, stamps: List<LocalDateTime>, salt: Long): JsonObject {
        if (ticker !in StockLogic.TICKERS) return JsonObject(emptyMap())
        val closes = walk(ticker, stamps.size, salt)
        return JsonObject(stamps.zip(closes).associate { (t, c) ->
            t.format(STAMP) to JsonObject(mapOf(
                "Open" to JsonPrimitive(c), "High" to JsonPrimitive(c), "Low" to JsonPrimitive(c),
                "Close" to JsonPrimitive(c), "Volume" to JsonPrimitive(1000), "Dividends" to JsonPrimitive(0.0),
                "Stock Splits" to JsonPrimitive(0.0),
            ))
        })
    }

    override suspend fun live(ticker: String): JsonObject {
        val open = today().atTime(9, 30)
        return bars(ticker, List(390) { open.plusMinutes(it.toLong()) }, salt = today().toEpochDay())
    }

    override suspend fun historical(ticker: String, startDate: String, endDate: String): JsonObject {
        val start = LocalDate.parse(startDate)
        val end = LocalDate.parse(endDate)
        // yfinance `end` is exclusive; weekends have no bars.
        val days = generateSequence(start) { it.plusDays(1) }.takeWhile { it < end }
            .filter { it.dayOfWeek.value <= 5 }.map { it.atStartOfDay() }.toList()
        return bars(ticker, days, salt = start.toEpochDay())
    }

    private companion object {
        val STAMP: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    }
}

/** Pure logic of the stock page (Pages/stocks/stock_profile.dart). */
object StockLogic {
    /** stockName (:83-94) and the names from stockDetails (:21-82). */
    val TICKERS = listOf("AAPL", "DIS", "SBUX", "NKE", "^NSEI", "TSLA", "INTC", "AMZN", "GOOGL", "PFE")
    val NAMES = mapOf(
        "AAPL" to "Apple INC.", "DIS" to "The Walt Disney Company", "SBUX" to "Starbucks Corporation",
        "NKE" to "Nike INC.", "^NSEI" to "NIFTY 50", "TSLA" to "Tesla INC.", "INTC" to "Intel Corporation",
        "AMZN" to "Amazon.com INC.", "GOOGL" to "Alphabet INC.", "PFE" to "Pfizer INC.",
    )

    /** Every price on the page is hard-coded (:166, :241, :466, :477-479); stockDetails' numbers are unused. */
    const val PRICE = "\$387"
    const val OPEN = "\$133"
    const val HIGH = "\$129"
    const val LOW = "\$333"

    val RANGES = listOf("1D", "1W", "1M", "3M", "6M", "1Y", "2Y", "3Y")
    private val RANGE_DAYS = mapOf("1W" to 7L, "1M" to 30L, "3M" to 90L, "6M" to 180L, "1Y" to 365L, "2Y" to 730L, "3Y" to 1095L)

    fun header(ticker: String): String = "$ticker - ${NAMES[ticker]}"

    /** Dialog quantity: `double.tryParse(value) ?? 0`. */
    fun quantity(text: String): Double = DartText.doubleTryParse(text) ?: 0.0

    /** 'Your Holdings: ${userHoldings[s] ?? 0} shares' — executed Dart prints the 0 as "0.0". */
    fun holdingsText(holdings: Map<String, Double>, ticker: String): String =
        "Your Holdings: ${DartText.doubleToString(holdings[ticker] ?: 0.0)} shares"

    data class Trade(val holdings: Map<String, Double>, val message: String, val error: Boolean)

    /** _buyStock (:218-225): no validation — 0, negatives and NaN all "buy". */
    fun buy(holdings: Map<String, Double>, ticker: String, quantity: Double): Trade {
        val next = holdings + (ticker to ((holdings[ticker] ?: 0.0) + quantity))
        return Trade(next, "Bought ${DartText.doubleToString(quantity)} shares of $ticker", error = false)
    }

    /**
     * Sell (:290-316): allowed iff `quantity <= available` (so negatives sell
     * "up", NaN fails); a holding of exactly 0 is removed.
     */
    fun sell(holdings: Map<String, Double>, ticker: String, quantity: Double): Trade {
        val available = holdings[ticker] ?: 0.0
        if (!(quantity <= available)) return Trade(holdings, "Not enough shares to sell", error = true)
        val left = (holdings[ticker] ?: 0.0) - quantity
        val next = if (left == 0.0) holdings - ticker else holdings + (ticker to left)
        return Trade(next, "Sold ${DartText.doubleToString(quantity)} shares of $ticker", error = false)
    }

    /** StockSearch (:690-700): case-insensitive ticker OR name contains; empty → all. */
    fun search(query: String): List<String> =
        if (query.isEmpty()) TICKERS
        else TICKERS.filter { it.lowercase().contains(query.lowercase()) || NAMES.getValue(it).lowercase().contains(query.lowercase()) }

    /**
     * getHistoricalStockData dates (:318-370): `DateTime.now().subtract(Duration(days: n))`
     * (n × 24 h, so DST can shift the date) → ISO date; unknown ranges use 1 day.
     */
    fun dateRange(range: String, now: ZonedDateTime): Pair<String, String> {
        val days = RANGE_DAYS[range] ?: 1L
        val start = now.minusHours(24 * days)
        return start.toLocalDate().toString() to now.toLocalDate().toString()
    }

    data class Point(val x: Double, val y: Double)

    /**
     * Parse a payload into chart points (:129-141): key → local DateTime →
     * epoch ms; `value["Close"]` as a Dart double. Any failure (int Close,
     * bad key) throws inside the try → the OLD points stay (null here).
     */
    fun points(json: JsonObject, zone: ZoneId = ZoneId.systemDefault()): List<Point>? = try {
        json.map { (key, value) ->
            val t = LocalDateTime.parse(key.replace(' ', 'T')).atZone(zone).toInstant().toEpochMilli().toDouble()
            val close = (value as JsonObject)["Close"] as JsonPrimitive
            if (dartKind(close) != "double") throw IllegalStateException("type 'int' is not a subtype of type 'double'")
            Point(t, close.content.toDouble())
        }
    } catch (e: Exception) {
        null
    }

    /** Chart bounds (:560-571): first/last x (not min/max), reduce min/max y; 0 when empty. */
    data class Bounds(val minX: Double, val maxX: Double, val minY: Double, val maxY: Double)

    fun bounds(points: List<Point>): Bounds =
        if (points.isEmpty()) Bounds(0.0, 0.0, 0.0, 0.0)
        else Bounds(points.first().x, points.last().x,
            points.map { it.y }.reduce { a, b -> if (a < b) a else b }, points.map { it.y }.reduce { a, b -> if (a > b) a else b })
}
