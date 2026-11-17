package com.penny.app.core

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

/**
 * The expense dashboard's dialogs and their messages
 * (expense_dashboard.dart:265-735), independent of Compose. Message texts are
 * Dart's: `$e` of an `Exception('Failed to …: body')` renders with an
 * "Exception: " prefix, so the SnackBar shows the doubled
 * "Failed to add expense: Exception: Failed to add expense: <body>".
 */
object DashboardFlows {

    /**
     * _loadUsername (expense_dashboard.dart:90-112): the `username` claim if
     * present, else the prefs username — unlike Home, fullName is irrelevant.
     * No username → no service, and every load fails into its catch.
     */
    fun username(claims: Map<String, String>?, prefsUsername: String?): String? =
        claims?.get("username") ?: prefsUsername

    /** Dart `'$e'` for the errors these flows can see. */
    fun dartError(e: Throwable): String = when (e) {
        is ExpenseApiException -> "Exception: ${e.message}"
        is DartTypeError -> e.message ?: ""
        else -> e.toString()
    }

    /** Dart TypeError stand-in (implicit casts of dynamic JSON). */
    class DartTypeError(message: String) : RuntimeException(message)

    sealed interface Outcome {
        /** Close the dialog, reload, SnackBar/toast [message]. */
        data class Done(val message: String) : Outcome
        /** Keep the dialog open, red SnackBar [message]. */
        data class Failed(val message: String) : Outcome
        /** Nothing visible happens (Dart threw outside any try). */
        data object Silent : Outcome
    }

    /** jsonEncode refuses non-finite doubles before any request is sent. */
    private fun encodeError(amount: Double): String? =
        if (amount.isFinite()) null
        else "Converting object to an encodable object failed: ${DartText.doubleToString(amount)}"

    /**
     * "Add" (:597-645): empty field → "Please fill all fields";
     * `double.parse` runs OUTSIDE the try, so an unparseable amount throws
     * silently; NaN/Infinity parse but fail jsonEncode inside the try.
     */
    suspend fun addExpense(
        api: ExpenseApi?,
        name: String,
        amountText: String,
        category: String?,
        year: Int,
        month: String,
    ): Outcome {
        if (name.isEmpty() || amountText.isEmpty() || category == null) {
            return Outcome.Failed("Please fill all fields")
        }
        val amount = DartText.doubleTryParse(amountText) ?: return Outcome.Silent
        return try {
            encodeError(amount)?.let { throw IllegalStateException(it) }
            requireApi(api).addExpense(Expense(name, amount, category, year, month))
            Outcome.Done("Expense added successfully")
        } catch (e: IllegalStateException) {
            Outcome.Failed("Failed to add expense: ${e.message}")
        } catch (e: Exception) {
            Outcome.Failed("Failed to add expense: ${dartError(e)}")
        }
    }

    /** Edit amount field (:296-298): `double.tryParse(value) ?? newAmount`. */
    fun editedAmount(text: String, previous: Double): Double = DartText.doubleTryParse(text) ?: previous

    /** Edit amount prefill (:300): Dart `expense['amount'].toString()`. */
    fun amountText(amount: JsonElement?): String = when {
        amount == null || amount is JsonNull -> "null"
        amount is JsonPrimitive && dartKind(amount) == "int" -> amount.content
        amount is JsonPrimitive && dartKind(amount) == "double" -> DartText.doubleToString(amount.content.toDouble())
        amount is JsonPrimitive -> amount.content
        else -> amount.toString()
    }

    /**
     * "Save" (:318-350): `oldAmount: expense['amount']` is a `double`
     * parameter, so an int amount is a TypeError inside the try.
     */
    suspend fun saveEdit(
        api: ExpenseApi?,
        oldName: String,
        oldAmount: JsonElement?,
        category: String,
        newName: String,
        newAmount: Double,
        year: Int,
        month: String,
    ): Outcome = try {
        val old = (oldAmount as? JsonPrimitive)?.takeIf { it !is JsonNull && dartKind(it) == "double" }
            ?.content?.toDouble()
            ?: throw DartTypeError("type '${dartKind(oldAmount)}' is not a subtype of type 'double'")
        encodeError(newAmount)?.let { throw IllegalStateException(it) }
        requireApi(api).updateExpense(oldName, old, category, newName, newAmount, category, year, month)
        Outcome.Done("Expense updated successfully")
    } catch (e: IllegalStateException) {
        Outcome.Failed("Failed to update expense: ${e.message}")
    } catch (e: Exception) {
        Outcome.Failed("Failed to update expense: ${dartError(e)}")
    }

    /** Delete (:410-433): success toast, failure red SnackBar. */
    suspend fun delete(api: ExpenseApi?, year: Int, month: String, category: String, name: String): Outcome = try {
        requireApi(api).deleteExpense(year, month, category, name)
        Outcome.Done("Expense Deleted Successfully")
    } catch (e: Exception) {
        Outcome.Failed("Failed to delete expense: ${dartError(e)}")
    }

    /**
     * Add income (:715-735): `double.tryParse`; null → "Invalid amount
     * entered". Otherwise `addIncome` is fired WITHOUT await (errors are lost;
     * a non-finite amount never reaches the backend — jsonEncode throws) and
     * the display is set to the ENTERED amount although the backend adds.
     * Returns the new displayed income, or null after the SnackBar case.
     */
    fun parseIncome(text: String): Double? = DartText.doubleTryParse(text)

    const val INVALID_INCOME = "Invalid amount entered"

    /** `_expenseService!` with no username → Dart null-check error inside the try. */
    private fun requireApi(api: ExpenseApi?): ExpenseApi =
        api ?: throw DartTypeError("Null check operator used on a null value")
}
