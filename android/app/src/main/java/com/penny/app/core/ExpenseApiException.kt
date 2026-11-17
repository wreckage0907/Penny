package com.penny.app.core

/**
 * Failure port of `throw Exception('Failed to <action>: ${response.body}')`
 * (expense_service.dart:43,63,81,97,127,145,160). The message embeds the raw
 * response body verbatim — callers surface it in snackbars (Q17), so the
 * format is part of the parity surface.
 */
class ExpenseApiException(message: String) : Exception(message)