package com.penny.app.core

import java.io.IOException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Port of Flutter `ExpenseService` (expense_service.dart:29-163).
 *
 * Parity boundary (characterisation spec, slice 002): the Dart service is an
 * untyped HTTP wrapper. It builds URLs/bodies, accepts ONLY status 200, and
 * returns raw decoded JSON for GETs. Every cast, null-coalesce, aggregation
 * and error-swallow lives in the screens — so this client returns [JsonObject]
 * and never coerces (the int-income TypeError quirk stays in the callers).
 *
 * Preserved quirks: success iff exactly 200 (201/204 fail); error messages
 * embed the raw body verbatim; writes send Content-Type json, GET/DELETE send
 * no headers; no retries, no caching; month is the English month name chosen
 * by callers (Q19); doubles serialise shortest-round-trip (Q12).
 */
class ExpenseService(
    private val baseUrl: String,
    private val userId: String,
    // Dart's http client never retries; OkHttp would resend buffered POSTs
    // after a connection failure, and POST /expenses APPENDS server-side
    // (duplicate expense). Parity: retries off. Timeouts keep OkHttp defaults
    // (maintainer decision 2026-11-17, explicit ledger divergence from Dart's
    // hang-forever). Body decoding is UTF-8 (maintainer decision: fix Dart's
    // latin1 mojibake, explicit ledger divergence).
    private val client: OkHttpClient = OkHttpClient.Builder()
        .retryOnConnectionFailure(false)
        .build(),
) : ExpenseApi {
    private val json = Json

    override suspend fun addExpense(expense: Expense) {
        write(
            "POST",
            ExpenseUrls.addExpense(baseUrl, userId),
            json.encodeToString(Expense.serializer(), expense),
            "add expense",
        )
    }

    override suspend fun addIncome(amount: Double, year: Int, month: String) {
        write(
            "POST",
            ExpenseUrls.addIncome(baseUrl, userId),
            json.encodeToString(AddIncomeBody.serializer(), AddIncomeBody(amount, year, month)),
            "add income",
        )
    }

    override suspend fun updateIncome(year: Int, month: String, amount: Double) {
        write(
            "PUT",
            ExpenseUrls.updateIncome(baseUrl, userId, year, month),
            json.encodeToString(UpdateIncomeBody.serializer(), UpdateIncomeBody(amount)),
            "update income",
        )
    }

    override suspend fun getIncome(year: Int?, month: String?): JsonObject =
        read(ExpenseUrls.income(baseUrl, userId, year, month), "get income")

    override suspend fun updateExpense(
        oldName: String,
        oldAmount: Double,
        oldCategory: String,
        newName: String,
        newAmount: Double,
        newCategory: String,
        year: Int,
        month: String,
    ) {
        write(
            "PUT",
            ExpenseUrls.updateExpense(baseUrl, userId, year, month),
            json.encodeToString(
                UpdateExpenseBody.serializer(),
                UpdateExpenseBody(oldName, oldAmount, oldCategory, newName, newAmount, newCategory),
            ),
            "update expense",
        )
    }

    override suspend fun getExpenses(year: Int?, month: String?): JsonObject =
        read(ExpenseUrls.expenses(baseUrl, userId, year, month), "get expenses")

    override suspend fun deleteExpense(year: Int, month: String, category: String, name: String) {
        val request = Request.Builder()
            .url(ExpenseUrls.deleteExpense(baseUrl, userId, year, month, category, name))
            .delete()
            .build()
        val response = execute(request)
        val body = response.body?.string() ?: ""
        if (response.code != 200) {
            throw ExpenseApiException("Failed to delete expense: $body")
        }
    }

    private suspend fun write(method: String, url: String, body: String, action: String) {
        val request = Request.Builder()
            .url(url)
            .method(method, body.toRequestBody(JSON_MEDIA))
            .build()
        val response = execute(request)
        val responseBody = response.body?.string() ?: ""
        if (response.code != 200) {
            throw ExpenseApiException("Failed to $action: $responseBody")
        }
    }

    private suspend fun read(url: String, action: String): JsonObject {
        val request = Request.Builder().url(url).get().build()
        val response = execute(request)
        val body = response.body?.string() ?: ""
        if (response.code != 200) {
            throw ExpenseApiException("Failed to $action: $body")
        }
        // Dart jsonDecode + implicit downcast to Map<String, dynamic>: a
        // non-object top level is a TypeError there; a failure here too.
        // (Exception type differs, callers swallow both identically.)
        //
        // CARRY-FORWARD for the analysis/dashboard slices: the PRESERVED
        // int-income quirk must be reproduced with primitive.content /
        // longOrNull (int => type failure), NEVER JsonPrimitive.double,
        // which parses "0" to 0.0 silently and would make the quirk vanish.
        //
        // Known tolerance divergence (unreachable via this backend, which
        // 500s on non-finite floats before serialising): kotlinx's lexer
        // accepts NaN/Infinity literals where Dart jsonDecode throws.
        return Json.parseToJsonElement(body) as? JsonObject
            ?: throw IllegalArgumentException("Response is not a JSON object: $body")
    }

    private suspend fun execute(request: Request): Response =
        suspendCancellableCoroutine { cont ->
            val call = client.newCall(request)
            cont.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    cont.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    // Leak window: cancellation between resume and body
                    // consumption leaves the response unclosed until GC;
                    // call.cancel() via invokeOnCancellation masks most cases.
                    cont.resume(response)
                }
            })
        }

    private companion object {
        // Verified against http 1.2.2 on the wire: the package appends
        // charset=utf-8 to the explicit 'application/json' header.
        val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
    }
}