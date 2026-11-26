@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.penny.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.penny.app.PennyServices
import com.penny.app.core.Allocations
import com.penny.app.core.BudgetCategories
import com.penny.app.core.DashboardFlows
import com.penny.app.core.Expense
import com.penny.app.core.ExpenseAggregation
import com.penny.app.core.ExpenseDashboardText
import com.penny.app.core.Inr
import com.penny.app.core.PlanMath
import com.penny.app.core.PlanMonth
import com.penny.app.core.PlanSummary
import com.penny.app.core.QuickLog
import com.penny.app.core.SipIllustration
import com.penny.app.core.TxDetail
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.time.LocalDate
import java.time.LocalTime

/**
 * v2.1 Budget (artboard frames 05–08 + state frames S01/S04): one screen,
 * two segments — Spending (donut, categories) and the 50/30/20 Plan
 * (targets, reconciled SIP, illustration). Quick Log (06) is the single
 * generic entry point; category sheets (07) carry edit/delete with the
 * delete confirmation and Undo toast (S01). Offline shows cached data with
 * retry (S04) instead of an error screen.
 *
 * Supersedes the v1 dashboard + analysis screens and their quirks (ledger):
 * int income no longer wipes anything, money prints en-IN, the analysis
 * pager/projection screens are replaced by the Plan tab.
 */
private data class QuickLogOpen(val presetCategory: String?, val edit: TxRow?)

private data class MonthData(
    val plan: PlanSummary,
    val ranked: List<Pair<String, Pair<Double, Int>>>,
    val txCount: Int,
)

private data class TxRow(
    val name: String,
    val amount: Double,
    val category: String,
    val detail: TxDetail,
)

@Composable
fun BudgetScreen(
    services: PennyServices,
    navigate: (String) -> Unit,
    initialTab: String? = null,
    openAdd: Boolean = false,
    openIncome: Boolean = false,
    today: LocalDate = LocalDate.now(),
) {
    val scope = rememberCoroutineScope()
    var year by remember { mutableStateOf(today.year) }
    var month by remember { mutableStateOf(ExpenseDashboardText.initialMonth(today.monthValue)) }
    var tab by remember { mutableStateOf(if (initialTab == "plan") 1 else 0) }
    var data by remember { mutableStateOf<MonthData?>(null) }
    var refreshError by remember { mutableStateOf(false) }
    var quickLog by remember { mutableStateOf<QuickLogOpen?>(null) }
    var incomeOpen by remember { mutableStateOf(openIncome) }
    var categoryOpen by remember { mutableStateOf<String?>(null) }
    var deleteTarget by remember { mutableStateOf<TxRow?>(null) }
    var toast by remember { mutableStateOf<Pair<String, (() -> Unit)?>?>(null) }
    val online by services.connectivity.online.collectAsState()

    var user by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        user = DashboardFlows.username(services.auth.customClaims(), services.prefs.username())
    }

    // Offline never blocks the read: the store IS the cache (S04). A failed
    // refresh only raises the banner; the saved data stays on screen.
    suspend fun reload() {
        val u = user ?: return
        val api = services.expenses(u)
        val expenses = try {
            api.getExpenses(year, month)
        } catch (e: Exception) {
            refreshError = true
            return
        }
        val income = try {
            api.getIncome(year, month)
        } catch (e: Exception) {
            JsonObject(emptyMap())
        }
        val categories = PlanMath.monthCategories(expenses, year, month)
        val allocations = services.allocations.forMonth(u, year, month)
        val monthData = PlanMonth(
            income = PlanMath.income(income, year, month),
            categories = categories,
            sipRecorded = Allocations.sipTotal(allocations),
            goalAllocated = Allocations.goalTotal(allocations),
        )
        data = MonthData(
            plan = PlanMath.summary(monthData),
            ranked = PlanMath.rankedCategories(monthData),
            txCount = categories.values.sumOf { it.second },
        )
        refreshError = !services.connectivity.online.value
    }

    LaunchedEffect(user, year, month) { reload() }
    LaunchedEffect(Unit) { if (openAdd) quickLog = QuickLogOpen(null, null) }

    fun toastNow(message: String, undo: (() -> Unit)?) {
        toast = message to undo
    }

    Box(Modifier.fillMaxSize().tag("budget-page")) {
        Column(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp)
                    .padding(top = 5.dp, bottom = 24.dp),
            ) {
                Row(
                    Modifier.padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Budget", style = PennyType.phoneTitle, modifier = Modifier.weight(1f).tag("budget-title"))
                    IconBtn(
                        Icons.AutoMirrored.Filled.ArrowBack, "Previous month",
                        { val (y, m) = PlanMath.shift(year, month, -1); year = y; month = m; data = null },
                        tag = "budget-prev",
                    )
                    PillBtn(PlanMath.monthLabel(year, month), {}, Modifier.padding(horizontal = 4.dp), tag = "budget-month")
                    IconBtn(
                        Icons.Filled.ChevronRight, "Next month",
                        { val (y, m) = PlanMath.shift(year, month, 1); year = y; month = m; data = null },
                        tag = "budget-next",
                    )
                }
                if (!online) {
                    val cached = data
                    OfflineBanner(
                        txCount = cached?.txCount ?: 0,
                        label = PlanMath.monthLabel(year, month),
                        onRetry = { scope.launch { reload() } },
                    )
                }
                Segment(
                    selected = tab,
                    onSelect = { tab = it },
                )
                val current = data
                if (current == null) {
                    Box(Modifier.padding(top = 14.dp)) { LoadingSummary() }
                } else if (current.txCount == 0) {
                    EmptyMonth(
                        label = PlanMath.monthLabel(year, month),
                        onAdd = { quickLog = QuickLogOpen(null, null) },
                        onIncome = { incomeOpen = true },
                    )
                } else if (tab == 0) {
                    SpendingTab(current, { categoryOpen = it })
                } else {
                    PlanTab(current)
                }
                if (refreshError) {
                    Notice(
                        "Couldn't refresh ${month.take(3)}. Your saved data is unchanged. Check your connection and Retry.",
                        Modifier.padding(top = 14.dp), NoticeKind.NEG,
                        tag = "budget-refresh-error",
                    )
                }
            }
            Dock("Add Expense" to { quickLog = QuickLogOpen(null, null) })
            BottomNavBar(PennyTab.BUDGET, { t -> navigate(tabRoute(t)) })
        }

        // Sheets and modals --------------------------------------------------
        quickLog?.let { open ->
            QuickLogSheet(
                services = services,
                year = year,
                month = month,
                presetCategory = open.presetCategory,
                editRow = open.edit,
                user = user ?: "",
                onDismiss = { quickLog = null },
                onSaved = { message, undo ->
                    quickLog = null
                    toastNow(message, undo)
                    scope.launch { reload() }
                },
            )
        }
        categoryOpen?.let { category ->
            CategorySheet(
                services = services,
                category = category,
                year = year,
                month = month,
                user = user ?: "",
                onDismiss = { categoryOpen = null },
                onEdit = { row -> categoryOpen = null; quickLog = QuickLogOpen(null, row) },
                onDelete = { row -> deleteTarget = row },
                onQuickAdd = { message, undo ->
                    toastNow(message, undo)
                    scope.launch { reload() }
                },
            )
        }
        deleteTarget?.let { row ->
            DeleteTransactionModal(
                row = row,
                monthLabel = PlanMath.monthLabel(year, month),
                onCancel = { deleteTarget = null },
                onConfirm = {
                    deleteTarget = null
                    scope.launch {
                        val u = user ?: return@launch
                        try {
                            services.expenses(u).deleteExpense(year, month, row.category, row.name)
                            toastNow("Transaction deleted · Undo") {
                                scope.launch {
                                    try {
                                        services.expenses(u).addExpense(
                                            Expense(row.name, row.amount, row.category, year, month),
                                        )
                                        services.meta.put(u, year, month, row.category, row.name, row.detail)
                                        reload()
                                    } catch (e: Exception) { }
                                }
                            }
                            reload()
                        } catch (e: Exception) {
                            toastNow("Could not delete: ${e.message}", null)
                        }
                    }
                },
            )
        }
        if (incomeOpen) {
            IncomeSheet(
                year = year,
                month = month,
                onDismiss = { incomeOpen = false },
                onSave = { amount ->
                    scope.launch {
                        val u = user ?: return@launch
                        services.expenses(u).addIncome(amount, year, month)
                        incomeOpen = false
                        toastNow("Income saved · adds to ${PlanMath.monthLabel(year, month)}", null)
                        reload()
                    }
                },
            )
        }
        toast?.let { (message, undo) ->
            ToastBar(
                message,
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 16.dp, end = 16.dp, bottom = 92.dp),
                actionLabel = if (undo != null) "Undo" else null,
                onAction = undo?.let { action -> { action(); scope.launch { reload() } } },
                tag = "budget-toast",
            )
            LaunchedEffect(message) {
                kotlinx.coroutines.delay(5000)
                if (toast?.first == message) toast = null
            }
        }
    }
}

// ── segmented control ──────────────────────────────────────────────────────

@Composable
private fun Segment(selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier
            .padding(top = 14.dp)
            .clip(PennyShape.field)
            .background(PennyPalette.sand)
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        listOf("Spending", "50/30/20 Plan").forEachIndexed { i, label ->
            Box(
                Modifier
                    .tag(if (i == 0) "budget-tab-spending" else "budget-tab-plan")
                    .weight(1f)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(11.dp))
                    .background(if (selected == i) PennyPalette.surface else Color.Transparent)
                    .defaultMinSize(minHeight = 48.dp)
                    .clickable { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    color = if (selected == i) PennyPalette.ink else PennyPalette.muted,
                    style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.W800),
                )
            }
        }
    }
}

// ── S04 offline + loading ─────────────────────────────────────────────────

@Composable
private fun OfflineBanner(txCount: Int, label: String, onRetry: () -> Unit) {
    Column(Modifier.padding(top = 14.dp).tag("budget-offline")) {
        Notice(
            "Offline. Showing $txCount saved transactions from $label at ${LocalTime.now().toString().take(5)}. " +
                "New entries stay on this device until you reconnect.",
            Modifier, NoticeKind.GOLD, icon = Icons.Filled.Wifi,
            boldParts = listOf("Offline."),
        )
        TextLink("Retry Connection", onRetry, Modifier.padding(top = 2.dp), tag = "budget-retry")
    }
}

@Composable
private fun LoadingSummary() {
    CardBox {
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Skeleton(Modifier.size(110.dp).clip(androidx.compose.foundation.shape.CircleShape))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Skeleton(Modifier.fillMaxWidth().height(22.dp))
                Skeleton(Modifier.fillMaxWidth().height(14.dp))
                Skeleton(Modifier.fillMaxWidth(0.7f).height(14.dp))
            }
        }
        Hint("Loading latest figures… Cached data remains visible.", Modifier.padding(top = 12.dp))
    }
}

// ── empty month ───────────────────────────────────────────────────────────

@Composable
private fun EmptyMonth(label: String, onAdd: () -> Unit, onIncome: () -> Unit) {
    CardBox(Modifier.padding(top = 14.dp), soft = true, tag = "budget-empty") {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Nothing logged for $label", style = PennyType.section)
            Text(
                "Add an expense or your monthly income and the plan, categories, and donut build themselves.",
                color = PennyPalette.muted,
                style = PennyType.secondaryPlain,
                modifier = Modifier.padding(top = 8.dp, bottom = 14.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PennyButton("Add Expense", onAdd, Modifier.weight(1f), tag = "budget-empty-add")
                PennyButton("Add Income", onIncome, Modifier.weight(1f), kind = BtnKind.SECONDARY, tag = "budget-empty-income")
            }
        }
    }
}

// ── 05 · Spending tab ─────────────────────────────────────────────────────

@Composable
private fun SpendingTab(data: MonthData, onCategory: (String) -> Unit) {
    CardBox(Modifier.padding(top = 14.dp), tag = "budget-summary") {
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Donut(
                segments = data.ranked.mapNotNull { (category, total) ->
                    BudgetCategories.COLORS[category]?.let { color ->
                        Color(color) to total.first / data.plan.spent
                    }
                },
                centerAmount = Inr.plain(data.plan.spent),
                centerLabel = "${data.txCount} items",
                modifier = Modifier.tag("budget-donut"),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    Inr.spent(data.plan.spent),
                    color = PennyPalette.neg,
                    style = PennyType.amountL.merge(MoneyStyle),
                    modifier = Modifier.tag("budget-total"),
                )
                SumLine("Net cash retained", Inr.signed(data.plan.retained), PennyPalette.pos)
                SumLine("Recorded SIP", Inr.plain(data.plan.sipRecorded), PennyPalette.ink)
                SumLine("Unallocated", Inr.plain(data.plan.unallocated), PennyPalette.ink)
            }
        }
    }
    SectionHead("By Category") { Tag("${data.ranked.size} categories") }
    CardBox(tag = "budget-categories") {
        Column {
            data.ranked.forEachIndexed { i, (category, total) ->
                SpendRow(
                    name = category,
                    meta = "${total.second} transactions",
                    amount = Inr.spent(total.first),
                    dot = Color(BudgetCategories.COLORS[category] ?: 0xFFA35555),
                    onClick = { onCategory(category) },
                    tag = "budget-category-$i",
                    trailing = {
                        Icon(
                            Icons.Filled.ChevronRight,
                            contentDescription = null,
                            tint = PennyPalette.muted,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                )
                if (i < data.ranked.lastIndex) {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(PennyPalette.ink.copy(alpha = 0.08f)))
                }
            }
        }
    }
}

@Composable
private fun SumLine(label: String, value: String, valueColor: Color) {
    Row(Modifier.padding(top = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = PennyPalette.muted, style = PennyType.meta, modifier = Modifier.weight(1f))
        Text(value, color = valueColor, style = PennyType.metaBold.merge(MoneyStyle))
    }
}

@Composable
private fun Donut(
    segments: List<Pair<Color, Double>>,
    centerAmount: String,
    centerLabel: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier.size(124.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(124.dp)) {
            val stroke = 14.dp.toPx()
            val inset = stroke / 2
            val rect = androidx.compose.ui.geometry.Rect(inset, inset, size.width - inset, size.height - inset)
            drawCircle(color = PennyPalette.sand, radius = (size.width - stroke) / 2, style = Stroke(stroke))
            var start = -90f
            segments.forEach { (color, fraction) ->
                val sweep = (fraction * 360.0).toFloat()
                drawArc(
                    color = color,
                    startAngle = start,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = Stroke(stroke, cap = StrokeCap.Butt),
                    topLeft = rect.topLeft,
                    size = androidx.compose.ui.geometry.Size(rect.width, rect.height),
                )
                start += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(centerAmount, style = PennyType.section.merge(MoneyStyle))
            Text(
                centerLabel.uppercase(),
                color = PennyPalette.muted,
                style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.W800, letterSpacing = 0.6.sp),
                modifier = Modifier.padding(top = 3.dp),
            )
        }
    }
}

// ── 08 · 50/30/20 Plan tab ────────────────────────────────────────────────

@Composable
private fun PlanTab(data: MonthData) {
    val plan = data.plan
    SectionHead("Actual vs Target") {
        Text("of ${Inr.plain(plan.income)}", color = PennyPalette.muted, style = PennyType.meta)
    }
    TargetCard(
        title = "Needs · ${plan.needs.sharePercent}%",
        targetLabel = "${Inr.plain(plan.needs.target)} target",
        actual = Inr.spent(plan.needs.actual),
        actualColor = PennyPalette.neg,
        meta = "Food + Rent/Utilities + Transport",
        used = Inr.share0(plan.needs.actual, plan.needs.target),
        progress = plan.needs.usedShare,
        color = PennyPalette.brand,
        remainder = "${Inr.plain(plan.needs.remainder)} budget remainder",
        tag = "plan-needs",
    )
    TargetCard(
        title = "Wants · ${plan.wants.sharePercent}%",
        targetLabel = "${Inr.plain(plan.wants.target)} target",
        actual = Inr.spent(plan.wants.actual),
        actualColor = PennyPalette.neg,
        meta = "Shopping + Entertainment",
        used = Inr.share0(plan.wants.actual, plan.wants.target),
        progress = plan.wants.usedShare,
        color = PennyPalette.c2,
        remainder = "${Inr.plain(plan.wants.remainder)} budget remainder",
        tag = "plan-wants",
    )
    TargetCard(
        title = "Actual SIP · 20%",
        targetLabel = "${Inr.plain(plan.sipGoal)} goal",
        actual = "${Inr.plain(plan.sipRecorded)} recorded",
        actualColor = PennyPalette.pos,
        meta = "Completed allocation only",
        used = if (plan.sipGoal > 0 && plan.sipRecorded >= plan.sipGoal) "100% · goal met" else Inr.share0(plan.sipRecorded, plan.sipGoal),
        progress = if (plan.sipGoal > 0) plan.sipRecorded / plan.sipGoal else 0.0,
        color = PennyPalette.pos,
        remainder = "Unspent cash is not counted until you record an allocation.",
        tag = "plan-sip",
    )
    Notice(
        "${Inr.plain(plan.unallocated)} unallocated. ${Inr.plain(plan.needs.remainder)} Needs remainder + " +
            "${Inr.plain(plan.wants.remainder)} Wants remainder.",
        Modifier.padding(top = 12.dp), NoticeKind.POS,
        icon = Icons.Filled.Check,
        boldParts = listOf("${Inr.plain(plan.unallocated)} unallocated."),
        tag = "plan-unallocated",
    )
    SipCard(plan)
}

@Composable
private fun TargetCard(
    title: String,
    targetLabel: String,
    actual: String,
    actualColor: Color,
    meta: String,
    used: String,
    progress: Double,
    color: Color,
    remainder: String,
    tag: String,
) {
    CardBox(Modifier.padding(top = 10.dp), tag = tag) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = PennyType.secondary, modifier = Modifier.weight(1f))
                Text(targetLabel, color = PennyPalette.muted, style = PennyType.meta)
            }
            Text(
                actual,
                color = actualColor,
                style = TextStyle(fontSize = 19.sp, fontWeight = FontWeight.W800).merge(MoneyStyle),
                modifier = Modifier.padding(top = 7.dp).tag("$tag-actual"),
            )
            Row(Modifier.padding(top = 7.dp)) {
                Text(meta, color = PennyPalette.muted, style = PennyType.meta, modifier = Modifier.weight(1f))
                Text(used, color = PennyPalette.muted, style = PennyType.metaBold)
            }
            Track(progress, Modifier.padding(top = 7.dp), color = color, height = 7)
            Text(remainder, color = if (remainder.endsWith("remainder")) PennyPalette.pos else PennyPalette.muted, style = PennyType.meta, modifier = Modifier.padding(top = 7.dp))
        }
    }
}

@Composable
private fun SipCard(plan: PlanSummary) {
    var rateIndex by remember { mutableStateOf(SipIllustration.DEFAULT_RATE_INDEX) }
    CardBox(Modifier.padding(top = 14.dp), tag = "plan-sip-card") {
        Column {
            Text("10-Year SIP Illustration", style = PennyType.section)
            Hint(
                "${Inr.plain(plan.sipRecorded)}/month · " +
                    "${Inr.lakh(SipIllustration.contributions(plan.sipRecorded, 120))} contributed · beginning-of-month contributions",
                Modifier.padding(top = 7.dp),
            )
            Row(
                Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SipIllustration.RATES.forEachIndexed { i, (label, _) ->
                    PillBtn(
                        label, { rateIndex = i }, Modifier.weight(1f),
                        selected = i == rateIndex,
                        tag = "sip-rate-$i",
                    )
                }
            }
            val (_, rate) = SipIllustration.RATES[rateIndex]
            val values = (0..10).map { y ->
                SipIllustration.futureValue(plan.sipRecorded, y * 12, rate)
            }
            SipChart(values, Modifier.padding(top = 10.dp).fillMaxWidth().height(160.dp).tag("sip-chart"))
            Row(
                Modifier.padding(top = 10.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    SipIllustration.RATES.forEach { (label, r) ->
                        Text(
                            "$label → ${Inr.lakh(SipIllustration.futureValue(plan.sipRecorded, 120, r))}",
                            color = PennyPalette.muted,
                            style = PennyType.meta,
                        )
                    }
                    Text(
                        "12% exact → ${Inr.plain(SipIllustration.futureValue(plan.sipRecorded, 120, 0.12))}",
                        color = PennyPalette.muted,
                        style = PennyType.meta,
                    )
                }
                Text(
                    Inr.lakh(values[10]),
                    style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.W800).merge(MoneyStyle),
                    modifier = Modifier.tag("sip-headline"),
                )
            }
            Notice(
                "Illustrative annual-return assumptions, compounded monthly. Returns are not guaranteed; " +
                    "taxes, fees, and inflation are excluded.",
                Modifier.padding(top = 12.dp), NoticeKind.GOLD,
                boldParts = listOf("not guaranteed"),
                tag = "sip-caution",
            )
        }
    }
}

@Composable
private fun SipChart(values: List<Double>, modifier: Modifier) {
    Canvas(modifier) {
        val maxValue = (values.last() / 100_000.0).let { Math.ceil(it / 10.0) * 10.0 } // axis in lakhs, rounded to 10L
        val axisMax = maxValue * 100_000.0
        val left = 46f
        val bottom = size.height - 18f
        val top = 16f
        val right = size.width - 8f
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.rgb(98, 94, 104)
            textSize = 10f * 2
            isAntiAlias = true
        }
        // gridlines + y labels (₹40L / ₹30L / ₹20L / ₹0)
        for (i in 0..3) {
            val y = bottom - (bottom - top) * i / 3
            drawLine(
                color = PennyPalette.ink.copy(alpha = 0.12f),
                start = Offset(left, y),
                end = Offset(right, y),
                strokeWidth = 1f,
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(3f, 4f)),
            )
            drawContext.canvas.nativeCanvas.drawText(
                if (i == 0) "₹0" else "₹${(maxValue * i / 3).toInt()}L",
                4f,
                y + 4f,
                paint,
            )
        }
        drawContext.canvas.nativeCanvas.drawText("Y0", left, size.height - 4f, paint)
        drawContext.canvas.nativeCanvas.drawText("Y5", (left + right) / 2, size.height - 4f, paint)
        drawContext.canvas.nativeCanvas.drawText("Y10", right - 24f, size.height - 4f, paint)
        val path = Path()
        values.forEachIndexed { year, value ->
            val x = left + (right - left) * year / (values.size - 1)
            val y = bottom - (bottom - top) * (value / axisMax).toFloat()
            if (year == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, PennyPalette.brand, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
    }
}

// ── 06 · Quick Log sheet ──────────────────────────────────────────────────

@Composable
private fun QuickLogSheet(
    services: PennyServices,
    year: Int,
    month: String,
    presetCategory: String?,
    editRow: TxRow?,
    user: String,
    onDismiss: () -> Unit,
    onSaved: (String, (() -> Unit)?) -> Unit,
) {
    var amount by remember { mutableStateOf(editRow?.let { Inr.plain(it.amount).removePrefix(Inr.SYMBOL) } ?: "") }
    var category by remember { mutableStateOf(editRow?.category ?: presetCategory) }
    var merchant by remember { mutableStateOf(editRow?.name ?: "") }
    var day by remember { mutableStateOf((editRow?.detail?.day ?: LocalDate.now().dayOfMonth).toString()) }
    var payment by remember { mutableStateOf(editRow?.detail?.payment ?: "UPI") }
    var amountError by remember { mutableStateOf<String?>(null) }
    var categoryError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = PennyShape.sheet,
        containerColor = PennyPalette.surface,
        scrimColor = PennyPalette.ink.copy(alpha = 0.40f),
        modifier = Modifier.testTag("quicklog-sheet"),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    if (editRow != null) "Edit Expense" else "Quick Log",
                    style = PennyType.sheetTitle,
                    modifier = Modifier.weight(1f).tag("quicklog-title"),
                )
                IconBtn(Icons.Filled.Close, "Close Quick Log", onDismiss, tag = "quicklog-close")
            }
            if (editRow == null) {
                Text(
                    "Add the essentials now. You can edit the details later.",
                    color = PennyPalette.muted,
                    style = PennyType.secondaryPlain,
                )
            }
            Field(
                "Amount · required", amount, { amount = it; amountError = null },
                prefix = Inr.SYMBOL, keyboardDecimal = true, placeholder = "0.00",
                error = amountError, tag = "quicklog-amount",
            )
            Column(Modifier.padding(top = 14.dp)) {
                Text(
                    "Category · required",
                    color = PennyPalette.ink,
                    style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.W800),
                    modifier = Modifier.padding(start = 2.dp, bottom = 7.dp).tag("quicklog-category-label"),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BudgetCategories.ALL.take(3).forEachIndexed { i, c ->
                        PillBtn(c, { category = c; categoryError = null }, Modifier.weight(1f), selected = category == c, tag = "quicklog-cat-$i")
                    }
                }
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BudgetCategories.ALL.drop(3).forEachIndexed { i, c ->
                        PillBtn(c, { category = c; categoryError = null }, Modifier.weight(1f), selected = category == c, tag = "quicklog-cat-${i + 3}")
                    }
                }
                if (categoryError != null) {
                    Text(categoryError!!, color = PennyPalette.neg, style = TextStyle(fontSize = 12.sp), modifier = Modifier.padding(start = 2.dp, top = 6.dp).tag("quicklog-category-error"))
                } else {
                    Hint(
                        if (presetCategory != null)
                            "Opened from $presetCategory — preselected, never silent."
                        else
                            "Generic Add never chooses a category silently. Food is preselected only when opened from Food & Dining.",
                        Modifier.padding(top = 7.dp),
                    )
                }
            }
            Field(
                "Merchant or note · optional", merchant, { merchant = it },
                placeholder = "e.g. BigBasket", tag = "quicklog-merchant",
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Field(
                    "Date", day, { day = it }, keyboardDecimal = true,
                    modifier = Modifier.weight(1f), tag = "quicklog-date",
                )
                Column(Modifier.weight(1f).padding(top = 14.dp)) {
                    Text(
                        "Payment",
                        color = PennyPalette.ink,
                        style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.W800),
                        modifier = Modifier.padding(start = 2.dp, bottom = 7.dp),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        QuickLog.PAYMENTS.forEach { p ->
                            PillBtn(p, { payment = p }, Modifier.weight(1f), selected = payment == p, tag = "quicklog-pay-$p")
                        }
                    }
                }
            }
            PennyButton(
                if (editRow != null) "Save Changes" else "Add Expense",
                {
                    val dayNumber = day.trim().toIntOrNull()?.coerceIn(1, 31) ?: LocalDate.now().dayOfMonth
                    when (val result = QuickLog.parse(amount, category, merchant, dayNumber, payment)) {
                        is QuickLog.Result.Valid -> {
                            scope.launch {
                                val api = services.expenses(user)
                                if (editRow != null) {
                                    try {
                                        api.updateExpense(
                                            editRow.name, editRow.amount, editRow.category,
                                            result.merchant.ifBlank { editRow.name }, result.amount, result.category,
                                            year, month,
                                        )
                                        services.meta.put(
                                            user, year, month, result.category,
                                            result.merchant.ifBlank { editRow.name },
                                            TxDetail(result.day, editRow.detail.subcategory, result.payment),
                                        )
                                        onSaved("Expense updated", null)
                                    } catch (e: Exception) {
                                        onSaved("Could not save: ${e.message}", null)
                                    }
                                } else {
                                    val name = result.merchant.ifBlank { "${result.category} expense" }
                                    try {
                                        api.addExpense(Expense(name, result.amount, result.category, year, month))
                                        services.meta.put(
                                            user, year, month, result.category, name,
                                            TxDetail(result.day, null, result.payment),
                                        )
                                        onSaved("Expense saved · Undo") {
                                            scope.launch {
                                                try {
                                                    api.deleteExpense(year, month, result.category, name)
                                                } catch (e: Exception) { }
                                            }
                                        }
                                    } catch (e: Exception) {
                                        onSaved("Could not save: ${e.message}", null)
                                    }
                                }
                            }
                        }
                        QuickLog.Result.AmountRequired -> amountError = "Amount is required."
                        QuickLog.Result.AmountInvalid -> amountError = "Enter a positive amount, e.g. 350.00."
                        QuickLog.Result.CategoryRequired -> categoryError = "Choose a category."
                    }
                },
                Modifier.padding(top = 12.dp),
                tag = "quicklog-submit",
            )
            Hint(
                "Validation appears beside Amount or Category. Success returns to Budget with Expense saved · Undo.",
                Modifier.padding(top = 7.dp),
                boldParts = listOf("Expense saved · Undo"),
            )
        }
    }
}

// ── 07 · Category sheet ───────────────────────────────────────────────────

@Composable
private fun CategorySheet(
    services: PennyServices,
    category: String,
    year: Int,
    month: String,
    user: String,
    onDismiss: () -> Unit,
    onEdit: (TxRow) -> Unit,
    onDelete: (TxRow) -> Unit,
    onQuickAdd: (String, (() -> Unit)?) -> Unit,
) {
    var rows by remember { mutableStateOf<List<TxRow>>(emptyList()) }
    var total by remember { mutableStateOf(0.0) }
    var monthSpend by remember { mutableStateOf(0.0) }
    var quickAmount by remember { mutableStateOf("") }
    var quickError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(category) {
        val api = services.expenses(user)
        val expenses = try {
            api.getExpenses(year, month)
        } catch (e: Exception) {
            kotlinx.serialization.json.JsonObject(emptyMap())
        }
        val categories = ExpenseAggregation.categoriesFor(expenses, year, month)
        val list = (categories[category] as? JsonArray)?.mapNotNull { el ->
            (el as? JsonObject)?.let { obj ->
                val name = (obj["name"] as? JsonPrimitive)?.content ?: return@mapNotNull null
                val amount = (obj["amount"] as? JsonPrimitive)?.content?.toDoubleOrNull() ?: return@mapNotNull null
                TxRow(name, amount, category, services.meta.detail(user, year, month, category, name))
            }
        } ?: emptyList()
        rows = list
        total = list.sumOf { it.amount }
        monthSpend = ExpenseAggregation.categoryTotals(categories).sumOf { it.second }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = PennyShape.sheet,
        containerColor = PennyPalette.surface,
        scrimColor = PennyPalette.ink.copy(alpha = 0.40f),
        modifier = Modifier.testTag("category-sheet"),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(11.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp)).background(Color(BudgetCategories.COLORS[category] ?: 0xFFA35555)))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(category, style = PennyType.sheetTitle, modifier = Modifier.tag("category-title"))
                        Text(
                            Inr.spent(total),
                            color = PennyPalette.neg,
                            style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.W800).merge(MoneyStyle),
                        )
                    }
                    Text(
                        "${rows.size} transactions · ${PlanMath.monthLabel(year, month)} · ${Inr.share1(total, monthSpend)} of spend",
                        color = PennyPalette.muted,
                        style = PennyType.meta,
                    )
                }
                IconBtn(Icons.Filled.Close, "Close category sheet", onDismiss, tag = "category-close")
            }
            Column(
                Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
            ) {
                rows.forEachIndexed { i, row ->
                    CategoryRow(row, month.take(3), onEdit, onDelete, i)
                }
            }
            // Footer: quick add into this category (artboard's "Add to Food").
            Row(
                Modifier.padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Field(
                    "Quick $category expense", quickAmount, { quickAmount = it; quickError = null },
                    prefix = Inr.SYMBOL, keyboardDecimal = true, placeholder = "Amount",
                    modifier = Modifier.weight(1f), error = quickError, tag = "category-quick-amount",
                )
                PennyButton(
                    "Add to ${category.take(4)}",
                    {
                        val parsed = QuickLog.parse(quickAmount, category, "", LocalDate.now().dayOfMonth, "UPI")
                        when (parsed) {
                            is QuickLog.Result.Valid -> {
                                scope.launch {
                                    val name = "${category} expense ${rows.size + 1}"
                                    services.expenses(user).addExpense(Expense(name, parsed.amount, category, year, month))
                                    services.meta.put(user, year, month, category, name, TxDetail(parsed.day, null, "UPI"))
                                    // Keep the open sheet in sync with the store.
                                    rows = rows + TxRow(name, parsed.amount, category, TxDetail(parsed.day, null, "UPI"))
                                    total += parsed.amount
                                    quickAmount = ""
                                    onQuickAdd("Expense saved · Undo") {
                                        scope.launch {
                                            try {
                                                services.expenses(user).deleteExpense(year, month, category, name)
                                            } catch (e: Exception) { }
                                        }
                                    }
                                }
                            }
                            else -> quickError = "Enter a positive amount."
                        }
                    },
                    Modifier.padding(top = 14.dp).tag("category-quick-add"),
                )
            }
            Hint("All ${rows.size} rows sum to ${Inr.plain(total)}. Long lists scroll here while Quick Log stays fixed.")
        }
    }
}

@Composable
private fun CategoryRow(row: TxRow, monthShort: String, onEdit: (TxRow) -> Unit, onDelete: (TxRow) -> Unit, index: Int) {
    val date = row.detail.day?.let { d -> "${d.toString().padStart(2, '0')} $monthShort" } ?: monthShort
    val middle = listOfNotNull(date, row.detail.subcategory, row.detail.payment).joinToString(" · ")
    Row(
        Modifier
            .tag("category-tx-$index")
            .fillMaxWidth()
            .defaultMinSize(minHeight = 70.dp)
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(row.name, style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.W800))
            Text(middle, color = PennyPalette.muted, style = TextStyle(fontSize = 11.sp), modifier = Modifier.padding(top = 3.dp))
        }
        Text(row.amount.let { Inr.spent(it) }, color = PennyPalette.neg, style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.W800).merge(MoneyStyle))
        IconBtn(Icons.Filled.Edit, "Edit ${row.name}", { onEdit(row) }, tag = "category-edit-$index")
        IconBtn(
            Icons.Filled.Delete, "Delete ${row.name}", { onDelete(row) },
            tint = PennyPalette.neg, tag = "category-delete-$index",
        )
    }
}

// ── S01 · delete confirmation ─────────────────────────────────────────────

@Composable
private fun DeleteTransactionModal(row: TxRow, monthLabel: String, onCancel: () -> Unit, onConfirm: () -> Unit) {
    ConfirmModal(
        "Delete this transaction?",
        onDismiss = onCancel,
        headerIcon = Icons.Filled.Delete,
        tag = "delete-tx-modal",
    ) {
        Text(
            "${row.name}\n${Inr.spent(row.amount)} · $monthLabel · ${row.detail.payment}",
            color = PennyPalette.muted,
            style = TextStyle(fontSize = 13.sp, lineHeight = 20.sp),
            modifier = Modifier.padding(top = 10.dp),
        )
        Text(
            "This updates ${row.category}, monthly totals, insights, and future AI answers.",
            color = PennyPalette.muted,
            style = TextStyle(fontSize = 13.sp, lineHeight = 20.sp),
            modifier = Modifier.padding(top = 10.dp),
        )
        Row(Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PennyButton("Cancel", onCancel, Modifier.weight(1f), kind = BtnKind.SECONDARY, tag = "delete-tx-cancel")
            PennyButton("Delete Transaction", onConfirm, Modifier.weight(1f), kind = BtnKind.DANGER, tag = "delete-tx-confirm")
        }
        Hint(
            "After deletion, show Transaction deleted · Undo until the change is permanently committed.",
            Modifier.padding(top = 10.dp),
            boldParts = listOf("Transaction deleted · Undo"),
        )
    }
}

// ── income sheet ───────────────────────────────────────────────────────────

@Composable
private fun IncomeSheet(year: Int, month: String, onDismiss: () -> Unit, onSave: (Double) -> Unit) {
    var amount by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = PennyShape.sheet,
        containerColor = PennyPalette.surface,
        scrimColor = PennyPalette.ink.copy(alpha = 0.40f),
        modifier = Modifier.testTag("income-sheet"),
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(bottom = 16.dp)) {
            Text("Add Monthly Income", style = PennyType.sheetTitle, modifier = Modifier.tag("income-title"))
            Text(
                "Adds to ${PlanMath.monthLabel(year, month)}'s income.",
                color = PennyPalette.muted,
                style = PennyType.secondaryPlain,
            )
            Field(
                "Amount", amount, { amount = it; error = null },
                prefix = Inr.SYMBOL, keyboardDecimal = true, placeholder = "0.00", error = error,
                tag = "income-amount",
            )
            PennyButton(
                "Save Income",
                {
                    when (val parsed = QuickLog.parseIncome(amount)) {
                        is QuickLog.IncomeResult.Valid -> onSave(parsed.amount)
                        QuickLog.IncomeResult.AmountRequired -> error = "Amount is required."
                        QuickLog.IncomeResult.AmountInvalid -> error = "Enter a positive amount, e.g. 85000."
                    }
                },
                Modifier.padding(top = 12.dp),
                tag = "income-save",
            )
        }
    }
}
