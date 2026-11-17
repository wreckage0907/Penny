package com.penny.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.penny.app.PennyServices
import com.penny.app.core.AnalysisLogic
import com.penny.app.core.DashboardFlows
import com.penny.app.core.ExpenseAggregation
import com.penny.app.core.ExpenseApi
import com.penny.app.core.ExpenseDashboardText
import com.penny.app.core.InvestmentProjection
import com.penny.app.core.Money
import kotlinx.coroutines.launch
import java.time.LocalDate

private val ESSENTIALS = Color(0xFF2196F3) // Colors.blue
private val WANTS = Color(0xFFF44336) // Colors.red
private val SAVINGS = Color(0xFF4CAF50) // Colors.green

/** IncomeAnalysis (income_analysis.dart): projections, 50/30/20 split, per-category pies. */
@Composable
fun AnalysisTab(services: PennyServices, today: LocalDate) {
    val scope = rememberCoroutineScope()
    var year by remember { mutableIntStateOf(today.year) }
    var month by remember { mutableStateOf(ExpenseDashboardText.initialMonth(today.monthValue)) }
    var api by remember { mutableStateOf<ExpenseApi?>(null) }
    var state by remember { mutableStateOf(AnalysisLogic.EMPTY) }
    var isLoading by remember { mutableStateOf(true) }

    // _loadIncomeData: income THEN expenses inside one try; any failure → empty.
    fun load() {
        scope.launch {
            isLoading = true
            val y = year
            val m = month
            state = try {
                val income = api!!.getIncome(y, m)
                val expenses = api!!.getExpenses(y, m)
                AnalysisLogic.load(income, expenses, y, m)
            } catch (e: Exception) {
                AnalysisLogic.EMPTY
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        val username = DashboardFlows.username(services.auth.customClaims(), services.prefs.username())
        api = username?.let(services.expenses)
        load()
    }

    val split = state.split
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).testTag("analysis-page")) {
        AnalysisCard {
            Text("Investment Returns Over the Years", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                color = PennyPalette.text, modifier = Modifier.testTag("analysis-chart-title"))
            Spacer(Modifier.height(16.dp))
            InvestmentChart(InvestmentProjection.project(split.savings))
            Spacer(Modifier.height(16.dp))
            Legend(InvestmentProjection.TYPES)
        }
        Spacer(Modifier.height(10.dp))
        AnalysisCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
                Text("Month", fontSize = 18.sp, color = PennyPalette.text)
                Picker(month, ExpenseDashboardText.MONTH_NAMES, "analysis-month", 130) { month = it; load() }
                Text("Year", fontSize = 18.sp, color = PennyPalette.text)
                Picker(year.toString(), ExpenseDashboardText.yearRange(today.year).map { it.toString() }, "analysis-year", 95) {
                    year = it.toInt(); load()
                }
            }
            Spacer(Modifier.height(10.dp))
            if (isLoading) {
                CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally).testTag("analysis-loading"))
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
                    SplitColumn("Essentials", split.essentials, ESSENTIALS, "analysis-essentials")
                    Text("/", fontSize = 28.sp, color = PennyPalette.text.copy(alpha = 0.8f))
                    SplitColumn("Wants", split.wants, WANTS, "analysis-wants")
                    Text("/", fontSize = 28.sp, color = PennyPalette.text.copy(alpha = 0.8f))
                    SplitColumn("Savings", split.savings, SAVINGS, "analysis-savings")
                }
            }
            Spacer(Modifier.height(15.dp))
            SplitBars(split.essentials, split.wants, split.savings)
        }
        Spacer(Modifier.height(10.dp))
        val categories = state.expenses.keys.toList()
        val pages = if (categories.isEmpty()) 1 else categories.size
        val pager = rememberPagerState { pages }
        HorizontalPager(pager, Modifier.fillMaxWidth().height(250.dp).testTag("analysis-pager")) { page ->
            if (categories.isEmpty()) {
                AnalysisCard(Modifier.fillMaxSize().testTag("analysis-empty")) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(AnalysisLogic.emptyText(month, year), fontSize = 18.sp, color = PennyPalette.text,
                            modifier = Modifier.testTag("analysis-empty-text"))
                    }
                }
            } else {
                CategoryCard(page, categories[page], state.expenses.getValue(categories[page]))
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            repeat(pages) { i ->
                Box(Modifier.padding(horizontal = 3.dp).size(5.dp).background(
                    if (pager.currentPage == i) PennyPalette.button else PennyPalette.card, CircleShape))
            }
        }
    }
}

@Composable
private fun AnalysisCard(modifier: Modifier = Modifier.fillMaxWidth(), content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Card(modifier, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = PennyPalette.background),
        elevation = CardDefaults.cardElevation(1.5.dp)) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun SplitColumn(title: String, amount: Double, color: Color, tag: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, fontSize = 18.sp, color = PennyPalette.text)
        Text("\$" + Money.format(amount), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = color,
            modifier = Modifier.testTag(tag))
    }
}

/** Three Flexible bars: flex = (x * 100).round(); flex 0 collapses (no width). */
@Composable
private fun SplitBars(essentials: Double, wants: Double, savings: Double) {
    val flexes = runCatching { listOf(essentials, wants, savings).map(ExpenseAggregation::barFlex) }.getOrNull() ?: return
    Row(Modifier.fillMaxWidth().height(10.dp).testTag("analysis-bars")) {
        listOf(ESSENTIALS, WANTS, SAVINGS).forEachIndexed { i, color ->
            if (i > 0) Spacer(Modifier.width(10.dp))
            if (flexes[i] > 0) Box(Modifier.weight(flexes[i].toFloat()).height(10.dp).background(color, RoundedCornerShape(6.dp)))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Legend(types: List<String>) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        types.forEachIndexed { i, type ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.testTag("analysis-legend-$i")) {
                Box(Modifier.size(8.dp).background(Color(AnalysisLogic.color(i))))
                Spacer(Modifier.width(4.dp))
                Text(type, fontSize = 12.sp, color = PennyPalette.text)
            }
        }
    }
}

/** fl_chart BarChart: 6 year groups × 4 rods (width 8), maxY = largest value, "$n" left labels. */
@Composable
private fun InvestmentChart(values: List<List<Double>>) {
    val maxY = AnalysisLogic.maxY(values)
    val measurer = rememberTextMeasurer()
    Canvas(Modifier.fillMaxWidth().height(300.dp).testTag("analysis-chart")) {
        val left = 40.dp.toPx()
        val bottom = size.height - 20.dp.toPx()
        val labelStyle = TextStyle(fontSize = 10.sp, color = PennyPalette.text)
        if (maxY > 0) {
            listOf(0.0, maxY / 2, maxY).forEach { v ->
                val y = (bottom - (v / maxY) * bottom).toFloat()
                val label = measurer.measure(AnalysisLogic.axisLabel(v), labelStyle)
                drawText(label, topLeft = Offset(0f, (y - label.size.height / 2f).coerceAtLeast(0f)))
            }
        }
        val groups = InvestmentProjection.YEARS + 1
        val slot = (size.width - left) / groups
        val rod = 8.dp.toPx()
        for (year in 0 until groups) {
            val groupWidth = rod * values.size
            val x0 = left + slot * year + (slot - groupWidth) / 2
            values.forEachIndexed { type, series ->
                val h = if (maxY > 0) (series[year] / maxY * bottom).toFloat() else 0f
                drawRect(Color(AnalysisLogic.color(type)), Offset(x0 + rod * type, bottom - h), Size(rod, h))
            }
            val label = measurer.measure("$year", labelStyle)
            drawText(label, topLeft = Offset(x0 + groupWidth / 2 - label.size.width / 2f, bottom + 4.dp.toPx()))
        }
    }
}

@Composable
private fun CategoryCard(index: Int, category: String, expenses: List<kotlinx.serialization.json.JsonObject>) {
    val sections = AnalysisLogic.sections(expenses)
    AnalysisCard(Modifier.fillMaxSize().padding(horizontal = 4.dp).testTag("analysis-card-$index")) {
        Row(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(200.dp)) {
                    val ring = 10.dp.toPx()
                    val radius = 80.dp.toPx() + ring / 2
                    val gap = Math.toDegrees((3.dp.toPx() / radius).toDouble()).toFloat()
                    var start = 0f
                    sections.forEachIndexed { i, pct ->
                        val sweep = (pct / 100 * 360).toFloat()
                        drawArc(Color(AnalysisLogic.color(i)), start + gap / 2, (sweep - gap).coerceAtLeast(0.5f), false,
                            Offset(center.x - radius, center.y - radius), Size(radius * 2, radius * 2), style = Stroke(ring))
                        start += sweep
                    }
                }
            }
            Spacer(Modifier.width(20.dp))
            Column {
                Text(category, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PennyPalette.text,
                    modifier = Modifier.testTag("analysis-card-title-$index"))
                Spacer(Modifier.height(8.dp))
                Text("\$" + Money.format(AnalysisLogic.cardTotal(expenses)), fontSize = 18.sp, color = PennyPalette.text,
                    modifier = Modifier.testTag("analysis-card-total-$index"))
                Spacer(Modifier.height(16.dp))
                expenses.forEachIndexed { i, e ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                        Box(Modifier.size(10.dp).background(Color(AnalysisLogic.color(i))))
                        Spacer(Modifier.width(10.dp))
                        // '${expense['name']}' — Dart interpolation prints a missing name as "null".
                        Text((e["name"] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: "null",
                            fontSize = 14.sp, color = PennyPalette.text)
                    }
                }
            }
        }
    }
}
