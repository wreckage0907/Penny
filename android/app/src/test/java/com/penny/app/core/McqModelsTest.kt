package com.penny.app.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Characterisation tests for Chapter/Question.fromJson
 * (mcqpage.dart:12-46). Goldens EXECUTED on Dart 3.14: cast-failure messages
 * are deterministic for JSON-derived types and are user-visible (the fetch
 * catch wraps them into 'Error: …' on screen), so they are parity surface.
 * Wire order is preserved (Dart jsonDecode map is insertion-ordered; the UI
 * indexes questions.values.elementAt(i)).
 */
class McqModelsTest {

    private fun obj(s: String) = Json.parseToJsonElement(s) as JsonObject

    private val good = """{"question":"q","options":["a","b"],"ans":"A","hint":"h"}"""

    @Test
    fun `question parses the four Dart keys verbatim`() {
        val q = McqQuestion.fromJson(obj(good))
        assertEquals("q", q.question)
        assertEquals(listOf("a", "b"), q.options)
        assertEquals("A", q.answer)
        assertEquals("h", q.hint)
    }

    @Test
    fun `chapter takes the FIRST top-level value and preserves wire order`() {
        val chapter = McqChapter.fromJson(
            obj("""{"chapter":{"q2":$good,"q1":$good}}"""),
        )
        assertEquals(listOf("q2", "q1"), chapter.questions.keys.toList())
    }

    @Test
    fun `empty top level fails with Dart StateError text`() {
        val e = assertThrows(IllegalStateException::class.java) { McqChapter.fromJson(obj("{}")) }
        assertEquals("Bad state: No element", e.message)
    }

    @Test
    fun `non-map first value fails with Dart cast text`() {
        val e = assertThrows(IllegalArgumentException::class.java) {
            McqChapter.fromJson(obj("""{"c":3}"""))
        }
        assertEquals("type 'int' is not a subtype of type 'Map<String, dynamic>' in type cast", e.message)
    }

    @Test
    fun `missing or null string fields fail with Dart Null cast text`() {
        for (payload in listOf(
            """{"question":"q","options":["a"],"hint":"h"}""",          // missing ans
            """{"question":null,"options":["a"],"ans":"A","hint":"h"}""",
            """{"question":"q","options":["a"],"ans":"A"}""",          // missing hint
        )) {
            val e = assertThrows(IllegalArgumentException::class.java) {
                McqQuestion.fromJson(obj(payload))
            }
            assertEquals("type 'Null' is not a subtype of type 'String'", e.message)
        }
    }

    @Test
    fun `int answer fails with Dart int cast text`() {
        val e = assertThrows(IllegalArgumentException::class.java) {
            McqQuestion.fromJson(obj("""{"question":"q","options":["a"],"ans":0,"hint":"h"}"""))
        }
        assertEquals("type 'int' is not a subtype of type 'String'", e.message)
    }

    @Test
    fun `non-string option fails with Dart element cast text`() {
        val e = assertThrows(IllegalArgumentException::class.java) {
            McqQuestion.fromJson(obj("""{"question":"q","options":["a",2],"ans":"A","hint":"h"}"""))
        }
        assertEquals("type 'int' is not a subtype of type 'String'", e.message)
    }

    @Test
    fun `non-list options fails with Dart Iterable cast text`() {
        val e = assertThrows(IllegalArgumentException::class.java) {
            McqQuestion.fromJson(obj("""{"question":"q","options":"a","ans":"A","hint":"h"}"""))
        }
        assertEquals("type 'String' is not a subtype of type 'Iterable<dynamic>'", e.message)
    }

    @Test
    fun `non-map question entry fails with Dart implicit param cast text`() {
        val e = assertThrows(IllegalArgumentException::class.java) {
            McqChapter.fromJson(obj("""{"chapter":{"q1":"nope"}}"""))
        }
        assertEquals("type 'String' is not a subtype of type 'Map<String, dynamic>'", e.message)
    }

    @Test
    fun `empty options list decodes fine (unanswerable question, not an error)`() {
        val q = McqQuestion.fromJson(obj("""{"question":"q","options":[],"ans":"A","hint":"h"}"""))
        assertEquals(emptyList<String>(), q.options)
    }

    @Test
    fun `only the first top-level chapter survives, extra leading keys break it`() {
        val two = McqChapter.fromJson(obj("""{"c1":{"q1":$good},"c2":{"q9":$good}}"""))
        assertEquals(listOf("q1"), two.questions.keys.toList())
        val e = assertThrows(IllegalArgumentException::class.java) {
            McqChapter.fromJson(obj("""{"count":10,"c1":{"q1":$good}}"""))
        }
        assertEquals("type 'int' is not a subtype of type 'Map<String, dynamic>' in type cast", e.message)
    }

    @Test
    fun `duplicate question keys keep last value at first position, like Dart jsonDecode`() {
        val chapter = McqChapter.fromJson(
            obj("""{"c":{"q1":$good,"q2":$good,"q1":{"question":"second","options":["z"],"ans":"B","hint":"h2"}}}"""),
        )
        assertEquals(listOf("q1", "q2"), chapter.questions.keys.toList())
        assertEquals("second", chapter.questions["q1"]!!.question)
        assertEquals("B", chapter.questions["q1"]!!.answer)
    }

    @Test
    fun `object sources print Dart's underscored _Map runtime type`() {
        assertEquals(
            "type '_Map<String, dynamic>' is not a subtype of type 'String'",
            assertThrows(IllegalArgumentException::class.java) {
                McqQuestion.fromJson(obj("""{"question":{"k":1},"options":["a"],"ans":"A","hint":"h"}"""))
            }.message,
        )
        assertEquals(
            "type '_Map<String, dynamic>' is not a subtype of type 'Iterable<dynamic>'",
            assertThrows(IllegalArgumentException::class.java) {
                McqQuestion.fromJson(obj("""{"question":"q","options":{"0":"a"},"ans":"A","hint":"h"}"""))
            }.message,
        )
        assertEquals(
            "type '_Map<String, dynamic>' is not a subtype of type 'String'",
            assertThrows(IllegalArgumentException::class.java) {
                McqQuestion.fromJson(obj("""{"question":"q","options":[{"k":1}],"ans":"A","hint":"h"}"""))
            }.message,
        )
    }

    @Test
    fun `exponent-form integers are double in Dart, not int`() {
        assertEquals(
            "type 'double' is not a subtype of type 'String'",
            assertThrows(IllegalArgumentException::class.java) {
                McqQuestion.fromJson(obj("""{"question":"q","options":["a"],"ans":1e2,"hint":"h"}"""))
            }.message,
        )
    }

    @Test
    fun `every JSON kind as first chapter value gets its Dart cast text`() {
        val cases = listOf(
            """{"c":[1]}""" to "type 'List<dynamic>' is not a subtype of type 'Map<String, dynamic>' in type cast",
            """{"c":"x"}""" to "type 'String' is not a subtype of type 'Map<String, dynamic>' in type cast",
            """{"c":true}""" to "type 'bool' is not a subtype of type 'Map<String, dynamic>' in type cast",
            """{"c":null}""" to "type 'Null' is not a subtype of type 'Map<String, dynamic>' in type cast",
        )
        for ((payload, message) in cases) {
            assertEquals(
                message,
                assertThrows(IllegalArgumentException::class.java) { McqChapter.fromJson(obj(payload)) }.message,
            )
        }
    }

    @Test
    fun `null options and null option elements fail with Null cast text`() {
        assertEquals(
            "type 'Null' is not a subtype of type 'Iterable<dynamic>'",
            assertThrows(IllegalArgumentException::class.java) {
                McqQuestion.fromJson(obj("""{"question":"q","options":null,"ans":"A","hint":"h"}"""))
            }.message,
        )
        assertEquals(
            "type 'Null' is not a subtype of type 'String'",
            assertThrows(IllegalArgumentException::class.java) {
                McqQuestion.fromJson(obj("""{"question":"q","options":["a",null],"ans":"A","hint":"h"}"""))
            }.message,
        )
    }

    @Test
    fun `duplicate top-level chapter keys keep last value at first position`() {
        val chapter = McqChapter.fromJson(
            obj("""{"c1":{"q1":$good},"c2":{"q2":$good},"c1":{"q7":$good}}"""),
        )
        assertEquals(listOf("q7"), chapter.questions.keys.toList())
    }

    @Test
    fun `double and bool kinds map to their Dart type names`() {
        assertEquals(
            "type 'double' is not a subtype of type 'String'",
            assertThrows(IllegalArgumentException::class.java) {
                McqQuestion.fromJson(obj("""{"question":"q","options":["a"],"ans":1.5,"hint":"h"}"""))
            }.message,
        )
        assertEquals(
            "type 'bool' is not a subtype of type 'String'",
            assertThrows(IllegalArgumentException::class.java) {
                McqQuestion.fromJson(obj("""{"question":true,"options":["a"],"ans":"A","hint":"h"}"""))
            }.message,
        )
    }
}