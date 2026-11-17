package com.penny.app.core

/**
 * Failure port for the MCQ fetch (mcqpage.dart:88,91,95). Dart renders
 * `Exception` with an "Exception: " prefix in string interpolation, and the
 * fetch catch wraps EVERY inner failure into
 * 'Failed to load questions: $e' — producing doubled prefixes for inner
 * Exceptions. [dartRender] reproduces that rendering.
 */
class McqApiException(message: String) : Exception(message)

internal fun dartRender(e: Exception): String =
    if (e is McqApiException) "Exception: ${e.message}" else e.message ?: e.toString()