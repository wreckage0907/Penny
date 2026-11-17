package com.penny.app.core

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

/**
 * Port of `Question` / `Chapter` (mcqpage.dart:12-46). Request-side of the
 * practice flow: the backend payload is `{chapterName: {key: {question,
 * options, ans, hint}}}`; Chapter takes the FIRST top-level value and keeps
 * wire order (the UI indexes `questions.values.elementAt(i)`).
 *
 * Dart's implicit dynamic→String casts throw TypeError on wrong kinds; the
 * messages are deterministic for JSON-derived types and user-visible (the
 * fetch catch wraps them into the on-screen error), so they are reproduced
 * verbatim — verified by executing Dart 3.14.
 */
data class McqQuestion(
    val question: String,
    val options: List<String>,
    val answer: String,
    val hint: String,
) {
    companion object {
        fun fromJson(json: JsonObject): McqQuestion = McqQuestion(
            question = dartString(json["question"]),
            options = dartStringList(json["options"]),
            answer = dartString(json["ans"]),
            hint = dartString(json["hint"]),
        )
    }
}

data class McqChapter(val questions: Map<String, McqQuestion>) {
    companion object {
        fun fromJson(json: JsonObject): McqChapter {
            val first = json.values.firstOrNull()
                ?: throw IllegalStateException("Bad state: No element")
            val map = first as? JsonObject
                ?: throw IllegalArgumentException(
                    "type '${dartKind(first)}' is not a subtype of type 'Map<String, dynamic>' in type cast",
                )
            return McqChapter(
                map.mapValues { (_, v) ->
                    // Implicit param cast in Dart Question.fromJson(value):
                    // no "in type cast" suffix (that's only for explicit `as`).
                    val q = v as? JsonObject
                        ?: throw IllegalArgumentException(
                            "type '${dartKind(v)}' is not a subtype of type 'Map<String, dynamic>'",
                        )
                    McqQuestion.fromJson(q)
                },
            )
        }
    }
}

/** Dart runtime type name of a JSON-decoded value, for cast-error text. */
internal fun dartKind(element: JsonElement?): String = when {
    element == null || element is kotlinx.serialization.json.JsonNull -> "Null"
    element is JsonArray -> "List<dynamic>"
    // Dart jsonDecode maps have runtime type _Map<…> (underscore) when they
    // appear as the SOURCE of a failed cast — verified by execution.
    element is JsonObject -> "_Map<String, dynamic>"
    element is JsonPrimitive && element.isString -> "String"
    element is JsonPrimitive && element.booleanOrNull != null -> "bool"
    // Dart yields int only for pure integer literals: the String extension
    // rejects exponent form (1e2), +1, 1.0 and >Long magnitudes, all of which
    // jsonDecode returns as double. kotlinx's longOrNull would accept 1e2.
    element is JsonPrimitive && element.content.toLongOrNull() != null -> "int"
    else -> "double"
}

private fun dartString(element: JsonElement?): String =
    (element as? JsonPrimitive)?.takeIf { it.isString }?.content
        ?: throw IllegalArgumentException("type '${dartKind(element)}' is not a subtype of type 'String'")

private fun dartStringList(element: JsonElement?): List<String> {
    val array = element as? JsonArray
        ?: throw IllegalArgumentException(
            "type '${dartKind(element)}' is not a subtype of type 'Iterable<dynamic>'",
        )
    return array.map { dartString(it) }
}