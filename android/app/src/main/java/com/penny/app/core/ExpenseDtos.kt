package com.penny.app.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Port of the Flutter `Expense` request DTO
 * (expense_service.dart:5-27). Request-only: the Dart app has NO response
 * models — GETs return raw decoded maps and every cast lives in the screens.
 * Field order matches Dart's toJson map insertion order; kotlinx.serialization
 * emits doubles shortest-round-trip (12.5, 100.0), which is load-bearing:
 * PUT /expenses matches stored amounts with Python float == (Q12).
 */
@Serializable
data class Expense(
    val name: String,
    val amount: Double,
    val category: String,
    val year: Int,
    val month: String,
)

/** addIncome body (expense_service.dart:55-59). */
@Serializable
data class AddIncomeBody(
    val amount: Double,
    val year: Int,
    val month: String,
)

/** updateIncome body (expense_service.dart:75-77). */
@Serializable
data class UpdateIncomeBody(
    val amount: Double,
)

/** updateExpense body (expense_service.dart:116-123), snake_case keys. */
@Serializable
data class UpdateExpenseBody(
    @SerialName("old_name") val oldName: String,
    @SerialName("old_amount") val oldAmount: Double,
    @SerialName("old_category") val oldCategory: String,
    @SerialName("new_name") val newName: String,
    @SerialName("new_amount") val newAmount: Double,
    @SerialName("new_category") val newCategory: String,
)