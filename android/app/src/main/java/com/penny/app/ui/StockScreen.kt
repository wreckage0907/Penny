package com.penny.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.penny.app.PennyServices
import com.penny.app.core.ProfileFlows
import com.penny.app.core.StockLogic
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

private val GREEN = Color(0xFF4CAF50)
private val RED = Color(0xFFF44336)

/** StockProfile (stock_profile.dart): local-only trading over a market chart. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockScreen(services: PennyServices, onBack: () -> Unit, now: () -> ZonedDateTime = { ZonedDateTime.now() }) {
    val scope = rememberCoroutineScope()
    val toasts = remember { mutableStateListOf<Pair<ProfileFlows.Toast, String>>() }
    var ticker by remember { mutableStateOf("AAPL") }
    var range by remember { mutableStateOf("1D") }
    // Every range tap (even on the selected one) re-runs the fetch/timer in Flutter (014 review note 1).
    var rangeTaps by remember { mutableStateOf(0) }
    var points by remember { mutableStateOf<List<StockLogic.Point>>(emptyList()) }
    var holdings by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var searching by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf<String?>(null) } // "buy" | "sell"

    // Failures only print in Flutter: the previous points stay.
    fun loadLive() = scope.launch {
        runCatching { StockLogic.points(services.stocks.live(ticker)) }.getOrNull()?.let { points = it }
    }
    fun loadHistorical() = scope.launch {
        val (start, end) = StockLogic.dateRange(range, now())
        runCatching { StockLogic.points(services.stocks.historical(ticker, start, end)) }.getOrNull()?.let { points = it }
    }

    // initState: live data now; while the range is 1D, refresh every minute.
    LaunchedEffect(range, rangeTaps) {
        if (range == "1D") {
            loadLive()
            while (true) {
                delay(60_000)
                loadLive()
            }
        } else {
            loadHistorical()
        }
    }

    if (searching) {
        StockSearch(onClose = { picked ->
            searching = false
            if (!picked.isNullOrEmpty()) {
                ticker = picked
                loadLive() // always LIVE data, whatever range is selected (quirk)
            }
        })
        return
    }

    Scaffold(
        containerColor = PennyPalette.background,
        topBar = {
            TopAppBar(
                title = { Text("Market", modifier = Modifier.testTag("stock-title")) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = PennyPalette.text)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PennyPalette.background),
            )
        },
    ) { inner ->
        Box(Modifier.fillMaxSize().padding(inner)) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).testTag("stock-page"),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    Modifier.padding(16.dp).fillMaxWidth().height(45.dp).background(Color.Black.copy(alpha = 0.38f), RoundedCornerShape(25.dp))
                        .clickable { searching = true }.testTag("stock-search"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Spacer(Modifier.width(20.dp))
                    Icon(Icons.Filled.Search, null, tint = Color.White)
                    Spacer(Modifier.width(10.dp))
                    Text("Search stocks", color = Color.White, fontSize = 18.sp)
                }
                Card(Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = PennyPalette.card)) {
                    Column(Modifier.padding(16.dp)) {
                        Text(StockLogic.header(ticker), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PennyPalette.text,
                            modifier = Modifier.testTag("stock-header"))
                        Spacer(Modifier.height(5.dp))
                        Text(StockLogic.PRICE, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = PennyPalette.text,
                            modifier = Modifier.testTag("stock-price"))
                        Spacer(Modifier.height(5.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            InfoItem("Open", StockLogic.OPEN)
                            InfoItem("High", StockLogic.HIGH)
                            InfoItem("Low", StockLogic.LOW)
                        }
                    }
                }
                Spacer(Modifier.height(15.dp))
                Text(StockLogic.holdingsText(holdings, ticker), fontSize = 18.sp, color = PennyPalette.text,
                    modifier = Modifier.testTag("stock-holdings"))
                Spacer(Modifier.height(15.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StockLogic.RANGES.forEach { r ->
                        Text(r, Modifier.clickable { range = r; rangeTaps++ }.testTag("stock-range-$r"), fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold, color = if (r == range) PennyPalette.button else PennyPalette.text)
                    }
                }
                LineChart(points)
                Spacer(Modifier.height(15.dp))
                Row(Modifier.fillMaxWidth().padding(bottom = 24.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Button({ dialog = "buy" }, Modifier.testTag("stock-buy"),
                        colors = ButtonDefaults.buttonColors(containerColor = GREEN.copy(alpha = 0.9f))) { Text("BUY", fontSize = 18.sp) }
                    Button({ dialog = "sell" }, Modifier.testTag("stock-sell"),
                        colors = ButtonDefaults.buttonColors(containerColor = RED.copy(alpha = 0.9f))) { Text("SELL", fontSize = 18.sp) }
                }
            }
            ToastHost(toasts, Modifier.align(Alignment.BottomCenter))
        }
    }

    dialog?.let { kind ->
        TradeDialog(kind, ticker, holdings[ticker] ?: 0.0, onDismiss = { dialog = null }) { quantity ->
            dialog = null
            val trade = if (kind == "buy") StockLogic.buy(holdings, ticker, quantity) else StockLogic.sell(holdings, ticker, quantity)
            holdings = trade.holdings
            toasts += (if (trade.error) ProfileFlows.Toast.ERROR else ProfileFlows.Toast.SUCCESS) to trade.message
        }
    }
}

@Composable
private fun InfoItem(label: String, value: String) {
    Column {
        Text(label, fontSize = 14.sp, color = PennyPalette.text.copy(alpha = 0.7f))
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = PennyPalette.text,
            modifier = Modifier.testTag("stock-${label.lowercase()}"))
    }
}

/** fl_chart LineChart: green line, no dots/axes/grid; x from first to last point, y from min to max. */
@Composable
private fun LineChart(points: List<StockLogic.Point>) {
    val bounds = StockLogic.bounds(points)
    Canvas(Modifier.fillMaxWidth().height(350.dp).padding(16.dp).testTag("stock-chart")) {
        if (points.size < 2) return@Canvas
        val dx = (bounds.maxX - bounds.minX).takeIf { it != 0.0 } ?: 1.0
        val dy = (bounds.maxY - bounds.minY).takeIf { it != 0.0 } ?: 1.0
        fun o(p: StockLogic.Point) = Offset(
            ((p.x - bounds.minX) / dx * size.width).toFloat(),
            (size.height - (p.y - bounds.minY) / dy * size.height).toFloat(),
        )
        val path = Path().apply {
            moveTo(o(points[0]).x, o(points[0]).y)
            points.drop(1).forEach { lineTo(o(it).x, o(it).y) }
        }
        drawPath(path, GREEN, style = Stroke(2.dp.toPx()))
    }
}

@Composable
private fun TradeDialog(kind: String, ticker: String, available: Double, onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    var quantity by remember { mutableStateOf(0.0) }
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (kind == "buy") "Buy $ticker" else "Sell $ticker") },
        text = {
            Column {
                Text("Current Price: ${StockLogic.PRICE}")
                if (kind == "sell") Text("Available: ${com.penny.app.core.DartText.doubleToString(available)}",
                    modifier = Modifier.testTag("trade-available"))
                OutlinedTextField(text, { text = it; quantity = StockLogic.quantity(it) }, Modifier.testTag("trade-quantity"),
                    label = { Text("Quantity") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
        },
        confirmButton = {
            TextButton({ onConfirm(quantity) }, Modifier.testTag("trade-confirm")) { Text(if (kind == "buy") "Buy" else "Sell") }
        },
        dismissButton = { TextButton(onDismiss, Modifier.testTag("trade-cancel")) { Text("Cancel") } },
    )
}

/** StockSearch (SearchDelegate): query field + filtered list; back/close returns ''. */
@Composable
private fun StockSearch(onClose: (String?) -> Unit) {
    var query by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().background(PennyPalette.background).testTag("stock-search-page")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton({ onClose("") }, Modifier.testTag("search-back")) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            TextField(query, { query = it }, Modifier.weight(1f).testTag("search-query"), singleLine = true,
                placeholder = { Text("Search") })
            IconButton({ query = "" }, Modifier.testTag("search-clear")) { Icon(Icons.Filled.Clear, "Clear") }
        }
        StockLogic.search(query).forEach { t ->
            Column(Modifier.fillMaxWidth().clickable { onClose(t) }.padding(16.dp).testTag("search-result-$t")) {
                Text(t, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PennyPalette.text)
                Text(StockLogic.NAMES.getValue(t), color = PennyPalette.text.copy(alpha = 0.7f))
            }
        }
    }
}
