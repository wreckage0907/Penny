package com.penny.app.core

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * Contract tests for fetchQuestions (mcqpage.dart:78-97) against canned
 * backend behaviour. Error-message goldens EXECUTED on Dart 3.14, including
 * the doubled 'Exception: ' prefix from the catch-all rewrap (:93-96).
 */
class McqServiceContractTest {

    private lateinit var server: MockWebServer
    private lateinit var service: McqService

    private val question = """{"question":"q","options":["a","b"],"ans":"A","hint":"h"}"""

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        service = McqService(server.url("/").toString().trimEnd('/'))
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun ok(body: String) = MockResponse().setResponseCode(200).setBody(body)

    @Test
    fun `requests the chapter path with num_question 5 and no headers`() = runBlocking<Unit> {
        server.enqueue(ok("""{"chapter":{"q1":$question}}"""))
        service.fetchQuestions("budgeting_and_expense_tracking")
        val req = server.takeRequest()
        assertEquals("GET", req.method)
        assertEquals("/generate_questions/budgeting_and_expense_tracking?num_question=5", req.path)
        assertNull(req.getHeader("Content-Type"))
    }

    @Test
    fun `num_question override reaches the wire even though backend ignores it`() = runBlocking<Unit> {
        // Backend always returns 10 (ledger quirk); the client still sends n.
        val ten = (1..10).joinToString(",") { """"q$it":$question""" }
        server.enqueue(ok("""{"chapter":{$ten}}"""))
        val chapter = service.fetchQuestions("setting_financial_goals", 7)
        assertEquals("/generate_questions/setting_financial_goals?num_question=7", server.takeRequest().path)
        assertEquals(10, chapter.questions.size)
    }

    @Test
    fun `non-200 is double-wrapped exactly like Dart`() = runBlocking<Unit> {
        server.enqueue(MockResponse().setResponseCode(404).setBody("nope"))
        assertEquals(
            "Failed to load questions: Exception: Failed to load questions: 404",
            assertThrows(McqApiException::class.java) {
                runBlocking { service.fetchQuestions("c") }
            }.message,
        )
        server.takeRequest()
    }

    @Test
    fun `non-object 200 body is double-wrapped Invalid response format`() = runBlocking<Unit> {
        server.enqueue(ok("[]"))
        assertEquals(
            "Failed to load questions: Exception: Invalid response format",
            assertThrows(McqApiException::class.java) {
                runBlocking { service.fetchQuestions("c") }
            }.message,
        )
        server.takeRequest()
    }

    @Test
    fun `cast and state failures are wrapped with their bare Dart text`() = runBlocking<Unit> {
        server.enqueue(ok("{}"))
        assertEquals(
            "Failed to load questions: Bad state: No element",
            assertThrows(McqApiException::class.java) {
                runBlocking { service.fetchQuestions("c") }
            }.message,
        )
        server.takeRequest()

        server.enqueue(ok("""{"chapter":{"q1":{"question":"q","options":["a"],"hint":"h"}}}"""))
        assertEquals(
            "Failed to load questions: type 'Null' is not a subtype of type 'String'",
            assertThrows(McqApiException::class.java) {
                runBlocking { service.fetchQuestions("c") }
            }.message,
        )
        server.takeRequest()
    }

    @Test
    fun `success preserves wire order of questions`() = runBlocking<Unit> {
        server.enqueue(ok("""{"chapter":{"q2":$question,"q1":$question}}"""))
        val chapter = service.fetchQuestions("c")
        assertEquals(listOf("q2", "q1"), chapter.questions.keys.toList())
        server.takeRequest()
    }

    @Test
    fun `invalid chapter 400 and generic 500 wrap with their status codes`() = runBlocking<Unit> {
        server.enqueue(MockResponse().setResponseCode(400).setBody("""{"detail":"Invalid chapter"}"""))
        assertEquals(
            "Failed to load questions: Exception: Failed to load questions: 400",
            assertThrows(McqApiException::class.java) {
                runBlocking { service.fetchQuestions("nope") }
            }.message,
        )
        server.takeRequest()
        server.enqueue(MockResponse().setResponseCode(500).setBody("""{"detail":"Failed to generate questions"}"""))
        assertEquals(
            "Failed to load questions: Exception: Failed to load questions: 500",
            assertThrows(McqApiException::class.java) {
                runBlocking { service.fetchQuestions("c") }
            }.message,
        )
        server.takeRequest()
    }

    @Test
    fun `empty question map parses at the client layer (crash is UI-side, carried forward)`() = runBlocking<Unit> {
        server.enqueue(ok("""{"Introduction to Personal Finance":{}}"""))
        assertEquals(0, service.fetchQuestions("c").questions.size)
        server.takeRequest()
    }

    @Test
    fun `nonconforming payload (3 options, lowercase ans) parses and never grades correct`() = runBlocking<Unit> {
        // Backend passes ans through unnormalised; Flutter compares letters
        // exactly, so 'b' means no option is ever green (mcqpage.dart:248).
        server.enqueue(
            ok("""{"Introduction to Personal Finance":{"question_1":{"question":"q","options":["A) a","B) b","C) c"],"ans":"b","hint":"h"}}}"""),
        )
        val q = service.fetchQuestions("c").questions.values.first()
        assertEquals(listOf("A) a", "B) b", "C) c"), q.options)
        assertEquals("b", q.answer)
        for (i in q.options.indices) {
            // The wire answer is a letter; 'b' never matches "A"+i, so
            // nothing is ever green (same contract v1 pinned via McqState).
            assertFalse(QuizText.isCorrect(q, i))
        }
        server.takeRequest()
    }

    @Test
    fun `raw UTF-8 payloads survive decoding (002 UTF-8 decision)`() = runBlocking<Unit> {
        server.enqueue(
            ok("""{"Introduction to Personal Finance":{"question_1":{"question":"What does a ₹ budget mean for café owners?","options":["A) ₹ plan","B) € plan","C) 3","D) 4"],"ans":"A","hint":"café hint"}}}"""),
        )
        val q = service.fetchQuestions("c").questions.values.first()
        assertEquals("What does a ₹ budget mean for café owners?", q.question)
        assertEquals("café hint", q.hint)
        server.takeRequest()
    }

    @Test
    fun `non-object 200 bodies all wrap Invalid response format`() = runBlocking<Unit> {
        for (body in listOf("[]", "null", "\"str\"", "3")) {
            server.enqueue(ok(body))
            assertEquals(
                "Failed to load questions: Exception: Invalid response format",
                assertThrows(McqApiException::class.java) {
                    runBlocking { service.fetchQuestions("c") }
                }.message,
            )
            server.takeRequest()
        }
    }

    @Test
    fun `malformed 200 body keeps single wrap (inner text diverges, recorded)`() = runBlocking<Unit> {
        // Dart FormatException vs kotlinx JsonDecodingException text differ
        // (accepted divergence, ledger); structure is a single wrap.
        server.enqueue(ok(""))
        val e = assertThrows(McqApiException::class.java) {
            runBlocking { service.fetchQuestions("c") }
        }
        assertTrue(e.message!!.startsWith("Failed to load questions: "))
        assertEquals(false, e.message!!.contains("Exception: Failed to load questions: Exception:"))
        server.takeRequest()
    }

    @Test
    fun `transport failures keep the wrap prefix (inner text diverges, recorded)`() = runBlocking<Unit> {
        // Dart's ClientException text vs OkHttp IOException text differ
        // (accepted divergence, ledger); the wrap structure is parity.
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST))
        val e = assertThrows(McqApiException::class.java) {
            runBlocking { service.fetchQuestions("c") }
        }
        assertTrue(e.message!!.startsWith("Failed to load questions: "))
        assertEquals(1, server.requestCount)
    }
}