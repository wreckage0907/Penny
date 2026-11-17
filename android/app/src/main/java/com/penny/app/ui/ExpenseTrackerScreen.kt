package com.penny.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.penny.app.PennyServices
import com.penny.app.core.DashboardFlows
import com.penny.app.core.ExpenseAggregation
import com.penny.app.core.ExpenseApi
import com.penny.app.core.ExpenseDashboardText
import com.penny.app.core.ExpenseDashboardText.CategoryIcon
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.time.LocalDate

/** ExpenseTracker (expense_tracker.dart): app bar + Dashboard / Analysis tabs. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseTrackerScreen(services: PennyServices, onBack: () -> Unit, today: LocalDate = LocalDate.now()) {
    var tab by remember { mutableIntStateOf(0) }
    Scaffold(
        containerColor = PennyPalette.background,
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Expense Tracker", fontSize = 28.sp, fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("tracker-back")) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = PennyPalette.background),
                )
                TabRow(tab, containerColor = PennyPalette.background, contentColor = PennyPalette.text) {
                    listOf("Dashboard", "Analysis").forEachIndexed { i, title ->
                        Tab(
                            selected = tab == i, onClick = { tab = i },
                            text = { Text(title, fontSize = 16.sp) },
                            selectedContentColor = PennyPalette.text,
                            unselectedContentColor = PennyPalette.text.copy(alpha = 0.5f),
                            modifier = Modifier.testTag("tracker-tab-${title.lowercase()}"),
                        )
                    }
                }
            }
        },
    ) { inner ->
        Box(Modifier.padding(inner)) {
            if (tab == 0) {
                ExpenseDashboardTab(services, today)
            } else {
                Box(Modifier.fillMaxSize().testTag("analysis-not-migrated"), contentAlignment = Alignment.Center) {
                    Text("Analysis — not migrated yet")
                }
            }
        }
    }
}

private class ColoredSnack(override val message: String, val error: Boolean) : SnackbarVisuals {
    override val actionLabel: String? = null
    override val withDismissAction = false
    override val duration = SnackbarDuration.Short
}

private val ICONS: Map<CategoryIcon, ImageVector> = mapOf(
    CategoryIcon.MONEY_BILL_WAVE to Icons.Filled.Payments,
    CategoryIcon.UTENSILS to Icons.Filled.Restaurant,
    CategoryIcon.CAR to Icons.Filled.DirectionsCar,
    CategoryIcon.CART_SHOPPING to Icons.Filled.ShoppingCart,
    CategoryIcon.FILM to Icons.Filled.Movie,
    CategoryIcon.CHAMPAGNE_GLASSES to Icons.Filled.Celebration,
    CategoryIcon.GIFTS to Icons.Filled.CardGiftcard,
    CategoryIcon.TAGS to Icons.Filled.LocalOffer,
)

/** The category names offered by the add dialog: the icons map keys (:584). */
private val CATEGORIES = listOf(
    "Utility Bills", "Food", "Transport", "Shopping", "Entertainment", "Holidays", "Gifts", "Miscellaneous",
)

/** ExpenseDashboard (expense_dashboard.dart). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseDashboardTab(services: PennyServices, today: LocalDate) {
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var year by remember { mutableIntStateOf(today.year) }
    var month by remember { mutableStateOf(ExpenseDashboardText.initialMonth(today.monthValue)) }
    var api by remember { mutableStateOf<ExpenseApi?>(null) }
    var state by remember { mutableStateOf(ExpenseAggregation.ExpensesState(JsonObject(emptyMap()), 0.0)) }
    var income by remember { mutableStateOf(0.0) }
    var isLoading by remember { mutableStateOf(true) }
    var sheet by remember { mutableStateOf<Pair<String, List<JsonObject>>?>(null) }
    var addDialog by remember { mutableStateOf(false) }
    var incomeDialog by remember { mutableStateOf(false) }

    fun show(message: String, error: Boolean) {
        scope.launch { snackbar.showSnackbar(ColoredSnack(message, error)) }
    }

    // _loadExpenseData / _loadIncomeData: independent, uncancelled requests.
    fun loadExpenses() {
        scope.launch {
            isLoading = true
            val y = year
            val m = month
            state = try {
                ExpenseAggregation.onExpensesFetched(api!!.getExpenses(y, m), y, m)
            } catch (e: Exception) {
                ExpenseAggregation.onExpensesFetchFailed(state.totalAmount)
            }
            isLoading = false
        }
    }

    fun loadIncome() {
        scope.launch {
            val y = year
            val m = month
            income = try {
                ExpenseAggregation.incomeFor(api!!.getIncome(y, m), y, m)
            } catch (e: Exception) {
                0.0
            }
        }
    }

    LaunchedEffect(Unit) {
        // _loadUsername (:90-118): username claim, else prefs.
        val username = DashboardFlows.username(services.auth.customClaims(), services.prefs.username())
        api = username?.let(services.expenses)
        loadExpenses()
        loadIncome()
    }

    val categories = remember(state, year, month) {
        runCatching { ExpenseAggregation.categoriesFor(state.expenseData, year, month) }.getOrDefault(JsonObject(emptyMap()))
    }
    val totals = remember(categories) { runCatching { ExpenseAggregation.categoryTotals(categories) }.getOrDefault(emptyList()) }

    Scaffold(
        containerColor = PennyPalette.background,
        snackbarHost = {
            SnackbarHost(snackbar, Modifier.testTag("dash-snackbar")) { data ->
                val error = (data.visuals as? ColoredSnack)?.error ?: false
                Snackbar(data, containerColor = if (error) Color(0xFFF44336) else Color(0xFF4CAF50))
            }
        },
    ) { inner ->
        Column(
            Modifier.fillMaxSize().padding(inner).verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 5.dp).testTag("dash-page"),
        ) {
            DashCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
                    Text("Month", fontSize = 18.sp, color = PennyPalette.text)
                    Picker(month, ExpenseDashboardText.MONTH_NAMES, "dash-month", 130) { m ->
                        month = m; loadExpenses(); loadIncome()
                    }
                    Text("Year", fontSize = 18.sp, color = PennyPalette.text)
                    Picker(year.toString(), ExpenseDashboardText.yearRange(today.year).map { it.toString() }, "dash-year", 95) { y ->
                        year = y.toInt(); loadExpenses(); loadIncome()
                    }
                }
                Spacer(Modifier.height(10.dp))
                when {
                    isLoading -> CircularProgressIndicator(Modifier.testTag("dash-loading"))
                    categories.isEmpty() -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No data for this month and year", fontSize = 16.sp, fontWeight = FontWeight.Medium,
                            color = PennyPalette.text, modifier = Modifier.testTag("dash-no-data"))
                    }
                    else -> Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f).height(200.dp).testTag("dash-pie"), contentAlignment = Alignment.Center) {
                            RingPie(totals.map { it.second })
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Total", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PennyPalette.text)
                                Text(ExpenseDashboardText.amountLabel(state.totalAmount), fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold, color = PennyPalette.text, modifier = Modifier.testTag("dash-total"))
                            }
                        }
                        Spacer(Modifier.width(20.dp))
                        Column(Modifier.weight(1f)) {
                            totals.forEachIndexed { i, (category, _) ->
                                Row(Modifier.padding(vertical = 4.dp).testTag("dash-legend-$i"), verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(10.dp).background(Color(ExpenseDashboardText.sliceColor(i)), CircleShape))
                                    Spacer(Modifier.width(10.dp))
                                    Text(category, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = PennyPalette.text,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            DashCard {
                Text(month, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = PennyPalette.text,
                    modifier = Modifier.testTag("dash-month-title"))
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        TextButton({ incomeDialog = true }, Modifier.testTag("dash-income-button")) {
                            Text("Income", fontSize = 24.sp, fontWeight = FontWeight.Light, color = PennyPalette.text)
                        }
                        Text(ExpenseDashboardText.amountLabel(income), fontSize = 24.sp, fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF4CAF50), modifier = Modifier.testTag("dash-income"))
                    }
                    Text("/", fontSize = 28.sp, color = PennyPalette.text.copy(alpha = 0.8f))
                    Column {
                        TextButton({}, enabled = false) {
                            Text("Expenses", fontSize = 24.sp, fontWeight = FontWeight.Light, color = PennyPalette.text)
                        }
                        Text(ExpenseDashboardText.amountLabel(state.totalAmount), fontSize = 24.sp, fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFF44336), modifier = Modifier.testTag("dash-expenses"))
                    }
                }
                Spacer(Modifier.height(8.dp))
                FlexBars(income, state.totalAmount)
            }
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth().padding(start = 8.dp), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("Expenses", fontSize = 28.sp, fontWeight = FontWeight.SemiBold, color = PennyPalette.text)
                IconButton({ addDialog = true }, Modifier.testTag("dash-add"),
                    colors = IconButtonDefaults.iconButtonColors(containerColor = PennyPalette.card)) {
                    Icon(Icons.Filled.Add, "Add expense", tint = PennyPalette.text)
                }
            }
            Spacer(Modifier.height(8.dp))
            totals.forEachIndexed { i, (category, total) ->
                val expenses = (categories[category] as? JsonArray)?.mapNotNull { it as? JsonObject } ?: emptyList()
                Card(
                    Modifier.fillMaxWidth().padding(bottom = 15.dp).clickable { sheet = category to expenses }
                        .testTag("dash-category-$i"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PennyPalette.background),
                    elevation = CardDefaults.cardElevation(3.dp),
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(50.dp).background(PennyPalette.card, CircleShape), contentAlignment = Alignment.Center) {
                            Icon(ICONS.getValue(ExpenseDashboardText.iconFor(category)), null, tint = PennyPalette.text)
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(category, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = PennyPalette.text)
                            Text(ExpenseDashboardText.categorySubtitle(month, year), fontSize = 14.sp, color = PennyPalette.text)
                        }
                        Text(ExpenseDashboardText.amountLabel(total), fontSize = 20.sp, fontWeight = FontWeight.SemiBold,
                            color = PennyPalette.text, modifier = Modifier.testTag("dash-category-total-$i"))
                    }
                }
            }
        }
    }

    sheet?.let { (category, initial) ->
        ExpensesSheet(
            category, initial,
            onDismiss = { sheet = null },
            onSave = { expense, newName, newAmount, done ->
                scope.launch {
                    when (val r = DashboardFlows.saveEdit(api, (expense["name"] as? JsonPrimitive)?.content ?: "",
                        expense["amount"], category, newName, newAmount, year, month)) {
                        is DashboardFlows.Outcome.Done -> { done(); loadExpenses(); show(r.message, false) }
                        is DashboardFlows.Outcome.Failed -> show(r.message, true)
                        DashboardFlows.Outcome.Silent -> Unit
                    }
                }
            },
            onDelete = { expense, removed ->
                scope.launch {
                    val name = (expense["name"] as? JsonPrimitive)?.content ?: ""
                    when (val r = DashboardFlows.delete(api, year, month, category, name)) {
                        is DashboardFlows.Outcome.Done -> { removed(); loadExpenses(); show(r.message, false) }
                        is DashboardFlows.Outcome.Failed -> show(r.message, true)
                        DashboardFlows.Outcome.Silent -> Unit
                    }
                }
            },
        )
    }

    if (addDialog) {
        AddExpenseDialog(onDismiss = { addDialog = false }) { name, amount, category ->
            scope.launch {
                when (val r = DashboardFlows.addExpense(api, name, amount, category, year, month)) {
                    is DashboardFlows.Outcome.Done -> { addDialog = false; loadExpenses(); show(r.message, false) }
                    is DashboardFlows.Outcome.Failed -> show(r.message, true)
                    DashboardFlows.Outcome.Silent -> Unit
                }
            }
        }
    }

    if (incomeDialog) {
        IncomeDialog(onDismiss = { incomeDialog = false }) { text ->
            incomeDialog = false
            val parsed = DashboardFlows.parseIncome(text)
            if (parsed == null) {
                show(DashboardFlows.INVALID_INCOME, true)
            } else {
                // Not awaited in Flutter; a non-finite amount never leaves the client.
                if (parsed.isFinite()) scope.launch { runCatching { api!!.addIncome(parsed, year, month) } }
                income = parsed
            }
        }
    }
}

@Composable
private fun DashCard(content: @Composable () -> Unit) {
    Card(
        Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PennyPalette.background),
        elevation = CardDefaults.cardElevation(3.dp),
    ) { Column(Modifier.padding(16.dp)) { content() } }
}

@Composable
private fun Picker(value: String, options: List<String>, tag: String, widthDp: Int, onPick: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier.width(widthDp.dp).height(30.dp).border(1.dp, PennyPalette.text, RoundedCornerShape(35.dp))
                .clickable { open = true }.padding(start = 12.dp, end = 10.dp).testTag(tag),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(value, Modifier.weight(1f), fontSize = 16.sp, color = PennyPalette.text, maxLines = 1)
            Icon(Icons.Filled.KeyboardArrowDown, null, Modifier.size(14.dp), tint = PennyPalette.text)
        }
        DropdownMenu(open, { open = false }) {
            options.forEach { option ->
                DropdownMenuItem({ Text(option) }, { open = false; onPick(option) }, Modifier.testTag("$tag-$option"))
            }
        }
    }
}

/** fl_chart PieChart: sectionsSpace 3, centerSpaceRadius 70, radius 10, from 0° clockwise. */
@Composable
private fun RingPie(values: List<Double>) {
    Canvas(Modifier.size(160.dp)) {
        val sum = values.sum()
        if (!(sum > 0)) return@Canvas
        val ring = 10.dp.toPx()
        val radius = 70.dp.toPx() + ring / 2
        val gapDegrees = Math.toDegrees((3.dp.toPx() / radius).toDouble()).toFloat()
        var start = 0f
        values.forEachIndexed { i, v ->
            val sweep = (v / sum * 360).toFloat()
            if (sweep > 0f) {
                drawArc(
                    Color(ExpenseDashboardText.sliceColor(i)), start + gapDegrees / 2, (sweep - gapDegrees).coerceAtLeast(0.5f), false,
                    topLeft = Offset(center.x - radius, center.y - radius), size = Size(radius * 2, radius * 2),
                    style = Stroke(ring),
                )
            }
            start += sweep
        }
    }
}

/** Income vs expense bars (:1060-1095): Flexible flex = (x * 100).round(). */
@Composable
private fun FlexBars(income: Double, expenses: Double) {
    val flexes = runCatching { ExpenseAggregation.barFlex(income) to ExpenseAggregation.barFlex(expenses) }.getOrNull()
    if (flexes == null) {
        // Dart throws UnsupportedError in build (NaN/Infinity) → error widget.
        Text("Error", color = Color.Red, modifier = Modifier.testTag("dash-bars-error"))
        return
    }
    Row(Modifier.fillMaxWidth().height(10.dp).testTag("dash-bars")) {
        if (flexes.first > 0) Box(Modifier.weight(flexes.first.toFloat()).height(10.dp)
            .background(Color(0xFF4CAF50), RoundedCornerShape(6.dp)).testTag("dash-bar-income"))
        Spacer(Modifier.width(10.dp))
        if (flexes.second > 0) Box(Modifier.weight(flexes.second.toFloat()).height(10.dp)
            .background(Color(0xFFF44336), RoundedCornerShape(6.dp)).testTag("dash-bar-expense"))
    }
}

/**
 * _showExpensesForCategory (:205-263). Holds its OWN list like Flutter's sheet
 * (the captured `expenses` list): edits mutate the tapped entry, deletes
 * remove every entry with the same name AND amount, reloads don't refresh it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpensesSheet(
    category: String,
    initial: List<JsonObject>,
    onDismiss: () -> Unit,
    onSave: (JsonObject, String, Double, done: () -> Unit) -> Unit,
    onDelete: (JsonObject, removed: () -> Unit) -> Unit,
) {
    val items = remember(category) { mutableStateListOf<JsonObject>().apply { addAll(initial) } }
    var editing by remember { mutableStateOf<Int?>(null) }
    var deleting by remember { mutableStateOf<Int?>(null) }
    ModalBottomSheet(onDismiss, containerColor = PennyPalette.background) {
        Column(Modifier.padding(16.dp).testTag("sheet")) {
            Text("Expenses for $category", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = PennyPalette.text,
                modifier = Modifier.testTag("sheet-title"))
            Spacer(Modifier.height(16.dp))
            items.forEachIndexed { j, expense ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("sheet-item-$j"), verticalAlignment = Alignment.CenterVertically) {
                    Text((expense["name"] as? JsonPrimitive)?.content ?: "null", Modifier.weight(1f).testTag("sheet-name-$j"),
                        fontSize = 18.sp, color = PennyPalette.text)
                    Text(ExpenseDashboardText.amountLabel(amountOf(expense["amount"])), fontSize = 18.sp, color = PennyPalette.text,
                        modifier = Modifier.testTag("sheet-amount-$j"))
                    IconButton({ editing = j }, Modifier.testTag("sheet-edit-$j")) { Icon(Icons.Filled.Edit, "Edit", tint = PennyPalette.text) }
                    IconButton({ deleting = j }, Modifier.testTag("sheet-delete-$j")) { Icon(Icons.Filled.Delete, "Delete", tint = Color.Red) }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
    editing?.let { j ->
        val expense = items[j]
        EditDialog(expense, onDismiss = { editing = null }) { newName, newAmount ->
            onSave(expense, newName, newAmount) {
                editing = null
                items[j] = JsonObject(expense + mapOf("name" to JsonPrimitive(newName), "amount" to JsonPrimitive(newAmount)))
            }
        }
    }
    deleting?.let { j ->
        val expense = items[j]
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete Expense") },
            text = { Text("Are you sure you want to delete this expense?") },
            confirmButton = {
                TextButton({
                    deleting = null
                    onDelete(expense) { items.removeAll { it["name"] == expense["name"] && it["amount"] == expense["amount"] } }
                }, Modifier.testTag("delete-confirm")) { Text("Delete", color = Color.Red) }
            },
            dismissButton = { TextButton({ deleting = null }, Modifier.testTag("delete-cancel")) { Text("Cancel") } },
        )
    }
}

private fun amountOf(e: JsonElement?): Double = (e as? JsonPrimitive)?.content?.toDoubleOrNull() ?: Double.NaN

@Composable
private fun EditDialog(expense: JsonObject, onDismiss: () -> Unit, onSave: (String, Double) -> Unit) {
    var name by remember { mutableStateOf((expense["name"] as? JsonPrimitive)?.content ?: "") }
    var amountText by remember { mutableStateOf(DashboardFlows.amountText(expense["amount"])) }
    var newAmount by remember { mutableStateOf(amountOf(expense["amount"])) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Expense") },
        text = {
            Column {
                OutlinedTextField(name, { name = it }, Modifier.testTag("edit-name"), label = { Text("Expense Name") })
                OutlinedTextField(amountText, {
                    amountText = it
                    newAmount = DashboardFlows.editedAmount(it, newAmount)
                }, Modifier.testTag("edit-amount"), label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
        },
        confirmButton = { TextButton({ onSave(name, newAmount) }, Modifier.testTag("edit-save")) { Text("Save") } },
        dismissButton = { TextButton(onDismiss, Modifier.testTag("edit-cancel")) { Text("Cancel") } },
    )
}

@Composable
private fun AddExpenseDialog(onDismiss: () -> Unit, onAdd: (String, String, String?) -> Unit) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<String?>(null) }
    var open by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Expense") },
        text = {
            Column {
                OutlinedTextField(name, { name = it }, Modifier.testTag("add-name"), label = { Text("Expense Name") })
                OutlinedTextField(amount, { amount = it }, Modifier.testTag("add-amount"), label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                Spacer(Modifier.height(10.dp))
                Box {
                    Text(category ?: "Select a Category", Modifier.fillMaxWidth().clickable { open = true }
                        .border(1.dp, PennyPalette.text, RoundedCornerShape(4.dp)).padding(12.dp).testTag("add-category"))
                    DropdownMenu(open, { open = false }) {
                        CATEGORIES.forEach { c ->
                            DropdownMenuItem({ Text(c) }, { category = c; open = false }, Modifier.testTag("add-category-$c"))
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton({ onAdd(name, amount, category) }, Modifier.testTag("add-submit")) { Text("Add") } },
        dismissButton = { TextButton(onDismiss, Modifier.testTag("add-cancel")) { Text("Cancel") } },
    )
}

@Composable
private fun IncomeDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Income") },
        text = {
            OutlinedTextField(text, { text = it }, Modifier.testTag("income-amount"), label = { Text("Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        },
        confirmButton = { TextButton({ onAdd(text) }, Modifier.testTag("income-add")) { Text("Add") } },
        dismissButton = { TextButton(onDismiss, Modifier.testTag("income-cancel")) { Text("Cancel") } },
    )
}
