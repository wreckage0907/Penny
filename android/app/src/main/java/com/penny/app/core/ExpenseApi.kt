package com.penny.app.core

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

/**
 * The expense/income seam (expense_service.dart). [ExpenseService] is the
 * HTTP implementation (002); [FakeExpenseBackend] mimics the FastAPI router
 * in memory until the stack is chosen. Failures throw [ExpenseApiException]
 * with the client's message: "Failed to <action>: <response body>".
 */
interface ExpenseApi {
    suspend fun addExpense(expense: Expense)
    suspend fun addIncome(amount: Double, year: Int, month: String)
    suspend fun updateIncome(year: Int, month: String, amount: Double)
    suspend fun getIncome(year: Int? = null, month: String? = null): JsonObject
    suspend fun updateExpense(
        oldName: String, oldAmount: Double, oldCategory: String,
        newName: String, newAmount: Double, newCategory: String,
        year: Int, month: String,
    )
    suspend fun getExpenses(year: Int? = null, month: String? = null): JsonObject
    suspend fun deleteExpense(year: Int, month: String, category: String, name: String)
}

/**
 * In-memory mimic of backend/routers/expenseTracker.py for ONE user, with the
 * verified quirks (LEDGER "Backend quirks"): amounts stored as floats; POST
 * income ADDS; GET income reports int 0 for months without an income entry
 * (the int-income quirk trigger); DELETE removes every expense with the name
 * (amount ignored) and cascades empty category → month → year keys away;
 * UPDATE matches name AND amount, first hit only, may move categories; every
 * 404 is re-raised as 400 {"detail":"404: …"}; `year = 0` / empty month are
 * treated as absent filters (Python truthiness); key order = insertion order.
 */
class FakeExpenseBackend : ExpenseApi {
    // year → month → category → list of (name, amount)
    private val expenses = linkedMapOf<String, LinkedHashMap<String, LinkedHashMap<String, MutableList<Pair<String, Double>>>>>()
    private var expensesDocExists = false
    // year → month → income
    private val finances = linkedMapOf<String, LinkedHashMap<String, Double>>()
    private var financesDocExists = false

    private fun fail(action: String, detail: String): Nothing =
        throw ExpenseApiException("Failed to $action: {\"detail\":\"${detail.replace("\"", "\\\"")}\"}")

    override suspend fun addExpense(expense: Expense) {
        expensesDocExists = true
        expenses.getOrPut(expense.year.toString()) { linkedMapOf() }
            .getOrPut(expense.month) { linkedMapOf() }
            .getOrPut(expense.category) { mutableListOf() }
            .add(expense.name to expense.amount)
    }

    override suspend fun addIncome(amount: Double, year: Int, month: String) {
        financesDocExists = true
        val months = finances.getOrPut(year.toString()) { linkedMapOf() }
        months[month] = (months[month] ?: 0.0) + amount
    }

    override suspend fun updateIncome(year: Int, month: String, amount: Double) {
        if (!financesDocExists) fail("update income", "404: Finance document not found.")
        addIncome(amount, year, month)
    }

    /**
     * get_income: optional year (truthy) then month filter; each month maps to
     * `month_data.get("income", 0)` — a filtered month that does not exist is
     * `{}` there, so it reports int 0, everything stored reports a float.
     */
    override suspend fun getIncome(year: Int?, month: String?): JsonObject {
        if (!financesDocExists) return JsonObject(emptyMap())
        fun months(m: Map<String, Double>) = JsonObject(m.mapValues { (_, v) -> JsonPrimitive(v) })
        if (year == null || year == 0) return JsonObject(finances.mapValues { (_, m) -> months(m) })
        val stored = finances[year.toString()] ?: emptyMap()
        if (month.isNullOrEmpty()) return JsonObject(mapOf(year.toString() to months(stored)))
        val value: JsonElement = stored[month]?.let { JsonPrimitive(it) } ?: JsonPrimitive(0)
        return JsonObject(mapOf(year.toString() to JsonObject(mapOf(month to value))))
    }

    override suspend fun updateExpense(
        oldName: String, oldAmount: Double, oldCategory: String,
        newName: String, newAmount: Double, newCategory: String,
        year: Int, month: String,
    ) {
        val action = "update expense"
        if (!expensesDocExists) fail(action, "404: Expense document not found.")
        val months = expenses[year.toString()]?.takeIf { it.isNotEmpty() } ?: fail(action, "404: Year not found.")
        val categories = months[month]?.takeIf { it.isNotEmpty() } ?: fail(action, "404: Month not found.")
        val old = categories[oldCategory] ?: mutableListOf()
        val i = old.indexOfFirst { it.first == oldName && it.second == oldAmount }
        if (i < 0) fail(action, "404: Expense not found.")
        if (oldCategory == newCategory) {
            old[i] = newName to newAmount
        } else {
            old.removeAt(i)
            categories.getOrPut(newCategory) { mutableListOf() }.add(newName to newAmount)
        }
        if (old.isEmpty()) categories.remove(oldCategory) else categories[oldCategory] = old
    }

    override suspend fun getExpenses(year: Int?, month: String?): JsonObject {
        if (!expensesDocExists) return JsonObject(emptyMap())
        if (year == null || year == 0) return expensesJson(expenses)
        val months = expenses[year.toString()]
        if (month.isNullOrEmpty()) {
            return JsonObject(mapOf(year.toString() to (months?.let { monthsJson(it) } ?: JsonObject(emptyMap()))))
        }
        val monthJson = months?.get(month)?.let { monthJson(it) } ?: JsonObject(emptyMap())
        return JsonObject(mapOf(year.toString() to JsonObject(mapOf(month to monthJson))))
    }

    override suspend fun deleteExpense(year: Int, month: String, category: String, name: String) {
        val action = "delete expense"
        if (!expensesDocExists) fail(action, "404: Expense document not found.")
        val months = expenses[year.toString()]?.takeIf { it.isNotEmpty() } ?: fail(action, "404: Year not found.")
        val categories = months[month]?.takeIf { it.isNotEmpty() } ?: fail(action, "404: Month not found.")
        val list = categories[category]?.takeIf { it.isNotEmpty() } ?: fail(action, "404: Category not found.")
        val kept = list.filter { it.first != name }.toMutableList()
        if (kept.isEmpty()) categories.remove(category) else categories[category] = kept
        if (categories.isEmpty()) months.remove(month)
        if (months.isEmpty()) expenses.remove(year.toString())
    }

    private fun expensesJson(years: Map<String, Map<String, Map<String, List<Pair<String, Double>>>>>) =
        JsonObject(years.mapValues { (_, m) -> monthsJson(m) })

    private fun monthsJson(months: Map<String, Map<String, List<Pair<String, Double>>>>) =
        JsonObject(months.mapValues { (_, c) -> monthJson(c) })

    private fun monthJson(categories: Map<String, List<Pair<String, Double>>>) = buildJsonObject {
        put("categories", JsonObject(categories.mapValues { (_, list) ->
            JsonArray(list.map { (n, a) -> JsonObject(mapOf("name" to JsonPrimitive(n), "amount" to JsonPrimitive(a))) })
        }))
    }
}
