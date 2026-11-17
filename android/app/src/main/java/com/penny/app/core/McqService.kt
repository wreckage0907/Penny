package com.penny.app.core

import java.io.IOException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Port of `fetchQuestions` (mcqpage.dart:78-97). GET
 * `{base}/generate_questions/{chapter}?num_question={n}` (n defaults to 5,
 * mcqpage.dart:66; the backend ignores it and returns 10). No headers.
 * Success iff 200 AND a JSON object; EVERY failure — status, format, cast,
 * transport — is rethrown wrapped as 'Failed to load questions: $e', with
 * Dart's string rendering of the inner error (doubled "Exception: " prefix
 * for inner Exceptions, bare text for TypeErrors/StateErrors).
 */
class McqService(
    private val baseUrl: String,
    // Parity with slice 002: Dart never retries.
    private val client: OkHttpClient = OkHttpClient.Builder()
        .retryOnConnectionFailure(false)
        .build(),
) {
    suspend fun fetchQuestions(chapter: String, numQuestions: Int = 5): McqChapter {
        try {
            val request = Request.Builder()
                .url("$baseUrl/generate_questions/$chapter?num_question=$numQuestions")
                .get()
                .build()
            val response = execute(request)
            val body = response.body?.string() ?: ""
            if (response.code != 200) {
                throw McqApiException("Failed to load questions: ${response.code}")
            }
            val element = Json.parseToJsonElement(body)
            if (element !is JsonObject) {
                throw McqApiException("Invalid response format")
            }
            return McqChapter.fromJson(element)
        } catch (e: Exception) {
            throw McqApiException("Failed to load questions: ${dartRender(e)}")
        }
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
                    // Leak window (as 002): cancellation between resume and
                    // body consumption discards the result; the read itself
                    // is no longer cancellable. Body is always consumed
                    // before any throw, so error paths never leak.
                    cont.resume(response)
                }
            })
        }
}