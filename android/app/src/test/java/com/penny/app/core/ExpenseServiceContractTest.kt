package com.penny.app.core

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

/**
 * Contract tests for the ported ExpenseService against canned backend
 * behaviour (MockWebServer — no Firebase creds needed). Goldens come from
 * expense_service.dart verbatim: verbs, paths, query quirks, bodies, the
 * exactly-200 rule, and error messages embedding the raw body.
 */
class ExpenseServiceContractTest {

    private lateinit var server: MockWebServer
    private lateinit var service: ExpenseService

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        // BACKEND_URL has no trailing slash (backend_url.dart); keep it that way.
        service = ExpenseService(server.url("/").toString().trimEnd('/'), "alice")
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun ok(body: String = "{}") = MockResponse().setResponseCode(200).setBody(body)

    @Test
    fun `addExpense posts the exact body to the expenses path`() = runBlocking<Unit> {
        server.enqueue(ok())
        service.addExpense(Expense("Lunch", 12.5, "Food", 2026, "November"))
        val req = server.takeRequest()
        assertEquals("POST", req.method)
        assertEquals("/expenses/alice", req.path)
        assertEquals(
            """{"name":"Lunch","amount":12.5,"category":"Food","year":2026,"month":"November"}""",
            req.body.readUtf8(),
        )
    }

    @Test
    fun `addIncome posts amount year month to the income path`() = runBlocking<Unit> {
        server.enqueue(ok())
        service.addIncome(1000.0, 2026, "November")
        val req = server.takeRequest()
        assertEquals("POST", req.method)
        assertEquals("/income/alice", req.path)
        assertEquals("""{"amount":1000.0,"year":2026,"month":"November"}""", req.body.readUtf8())
    }

    @Test
    fun `updateIncome puts year month in path and amount in body, backend 422 quirk`() = runBlocking<Unit> {
        // Backend declares amount as a QUERY param (expenseTracker.py:192-193);
        // Dart sends JSON body -> 422. The client must reproduce the request
        // and surface the failure verbatim.
        val detail = """{"detail":[{"loc":["query","amount"],"msg":"field required"}]}"""
        server.enqueue(MockResponse().setResponseCode(422).setBody(detail))
        val e = assertThrows(ExpenseApiException::class.java) {
            runBlocking { service.updateIncome(2026, "November", 500.0) }
        }
        assertEquals("Failed to update income: $detail", e.message)
        val req = server.takeRequest()
        assertEquals("PUT", req.method)
        assertEquals("/income/alice/2026/November", req.path)
        assertEquals("""{"amount":500.0}""", req.body.readUtf8())
        // F04: the 422 is a MISSING QUERY param — the body is sent but ignored.
        assertNull(req.requestUrl!!.queryParameter("amount"))
    }

    @Test
    fun `getIncome sends only non-null filters and keeps trailing question mark`() = runBlocking<Unit> {
        server.enqueue(ok("""{"2026":{"November":0}}"""))
        val unfiltered = service.getIncome()
        assertEquals("/income/alice?", server.takeRequest().path)
        // int 0 stays an int primitive — no coercion (the preserved quirk).
        val prim = unfiltered["2026"]!!.jsonObject["November"]!!.jsonPrimitive
        assertEquals("0", prim.content)
        assertEquals(0L, prim.longOrNull)

        server.enqueue(ok("{}"))
        service.getIncome(year = 2026)
        assertEquals("/income/alice?year=2026", server.takeRequest().path)

        server.enqueue(ok("{}"))
        service.getIncome(year = 2026, month = "November")
        assertEquals("/income/alice?year=2026&month=November", server.takeRequest().path)
    }

    @Test
    fun `getIncome and getExpenses send no headers`() = runBlocking<Unit> {
        server.enqueue(ok())
        service.getIncome(2026, "November")
        assertNull(server.takeRequest().getHeader("Content-Type"))

        server.enqueue(ok())
        service.getExpenses(2026, "November")
        assertNull(server.takeRequest().getHeader("Content-Type"))
    }

    @Test
    fun `updateExpense puts year month in query and six snake_case keys in body`() = runBlocking<Unit> {
        server.enqueue(ok())
        service.updateExpense("Lunch", 12.5, "Food", "Brunch", 15.0, "Food", 2026, "November")
        val req = server.takeRequest()
        assertEquals("PUT", req.method)
        assertEquals("/expenses/alice?year=2026&month=November", req.path)
        assertEquals(
            """{"old_name":"Lunch","old_amount":12.5,"old_category":"Food",""" +
                """"new_name":"Brunch","new_amount":15.0,"new_category":"Food"}""",
            req.body.readUtf8(),
        )
    }

    @Test
    fun `getExpenses drops month when year is null`() = runBlocking<Unit> {
        server.enqueue(ok())
        service.getExpenses(month = "November")
        assertEquals("/expenses/alice", server.takeRequest().path)

        server.enqueue(ok())
        service.getExpenses(year = 2026, month = "November")
        assertEquals("/expenses/alice?year=2026&month=November", server.takeRequest().path)
    }

    @Test
    fun `deleteExpense interpolates raw so ampersands inject query params`() = runBlocking<Unit> {
        server.enqueue(ok())
        service.deleteExpense(2026, "November", "Food", "a&b=c")
        val req = server.takeRequest()
        assertEquals("DELETE", req.method)
        assertEquals("/expenses/alice?year=2026&month=November&category=Food&name=a&b=c", req.path)
        // Server-side view: name truncated at the injected '&', exactly as Dart.
        assertEquals("a", req.requestUrl!!.queryParameter("name"))
        assertEquals("c", req.requestUrl!!.queryParameter("b"))
        assertNull(req.getHeader("Content-Type"))
    }

    @Test
    fun `only status 200 is success, everything else throws with the raw body`() = runBlocking<Unit> {
        for (code in listOf(201, 400, 404, 500)) {
            server.enqueue(MockResponse().setResponseCode(code).setBody("boom"))
            val e = assertThrows(ExpenseApiException::class.java) {
                runBlocking { service.addExpense(Expense("x", 1.0, "Food", 2026, "November")) }
            }
            assertEquals("Failed to add expense: boom", e.message)
            server.takeRequest()
        }
        // 204 carries no body on the wire (both clients see an empty string).
        server.enqueue(MockResponse().setResponseCode(204))
        val e = assertThrows(ExpenseApiException::class.java) {
            runBlocking { service.addIncome(1.0, 2026, "November") }
        }
        assertEquals("Failed to add income: ", e.message)
        server.takeRequest()
    }

    @Test
    fun `get failures embed the body for both getters`() = runBlocking<Unit> {
        server.enqueue(MockResponse().setResponseCode(500).setBody("kaboom"))
        assertEquals(
            "Failed to get income: kaboom",
            assertThrows(ExpenseApiException::class.java) {
                runBlocking { service.getIncome(2026, "November") }
            }.message,
        )
        server.takeRequest()
        server.enqueue(MockResponse().setResponseCode(404).setBody("nope"))
        assertEquals(
            "Failed to get expenses: nope",
            assertThrows(ExpenseApiException::class.java) {
                runBlocking { service.getExpenses(2026, "November") }
            }.message,
        )
        server.takeRequest()
    }

    @Test
    fun `non-object top level fails like Dart's downcast TypeError`() = runBlocking<Unit> {
        for (body in listOf("[]", "null", "3", "\"x\"")) {
            server.enqueue(ok(body))
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking { service.getIncome(2026, "November") }
            }
            server.takeRequest()
        }
    }

    @Test
    fun `invalid JSON fails like Dart jsonDecode`() = runBlocking<Unit> {
        server.enqueue(ok("{not json"))
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { service.getExpenses(2026, "November") }
        }
        server.takeRequest()
    }

    @Test
    fun `writes send the exact charset header the Dart http package sends`() = runBlocking<Unit> {
        // Verified with http 1.2.2 against a local server: the body setter
        // appends charset=utf-8 to the explicit Content-Type.
        server.enqueue(ok())
        service.addExpense(Expense("Lunch", 12.5, "Food", 2026, "November"))
        assertEquals("application/json; charset=utf-8", server.takeRequest().getHeader("Content-Type"))

        server.enqueue(ok())
        service.updateExpense("a", 1.0, "Food", "b", 2.0, "Food", 2026, "November")
        assertEquals("application/json; charset=utf-8", server.takeRequest().getHeader("Content-Type"))
    }

    @Test
    fun `income fixtures keep int zero, console ints and stored double zero distinct`() = runBlocking<Unit> {
        // F10/F11/F12 from the backend characterisation: the preserved quirk
        // hinges on 0 (int) vs 0.0 (double) vs absent key. No coercion here.
        server.enqueue(ok("""{"2024":{"February":0}}"""))
        val absentMonth = service.getIncome(2024, "February")
        assertEquals("0", absentMonth["2024"]!!.jsonObject["February"]!!.jsonPrimitive.content)

        server.enqueue(ok("""{"2024":{"January":900}}"""))
        val consoleInt = service.getIncome(2024, "January")
        assertEquals("900", consoleInt["2024"]!!.jsonObject["January"]!!.jsonPrimitive.content)

        server.enqueue(ok("""{"2024":{"March":0.0}}"""))
        val storedZero = service.getIncome(2024, "March")
        assertEquals("0.0", storedZero["2024"]!!.jsonObject["March"]!!.jsonPrimitive.content)

        // F07/F08: empty payloads pass through raw (caller's ?? 0.0 path).
        server.enqueue(ok("{}"))
        assertEquals(0, service.getIncome(2024, "February").size)
        server.enqueue(ok("""{"2025":{}}"""))
        assertEquals(0, service.getIncome(2025, "January")["2025"]!!.jsonObject.size)
    }

    @Test
    fun `expense fixtures pass console ints and category-less months through raw`() = runBlocking<Unit> {
        // F19/F20: the client must not crash or coerce; screens guard later.
        server.enqueue(ok("""{"2024":{"Jan":{"note":"x"}}}"""))
        val odd = service.getExpenses(2024, "Jan")
        assertEquals("x", odd["2024"]!!.jsonObject["Jan"]!!.jsonObject["note"]!!.jsonPrimitive.content)

        server.enqueue(ok("""{"2024":{"February":{"categories":{"Food":[{"name":"Coffee","amount":10}]}}}}"""))
        val entry = (
            service.getExpenses(2024, "February")["2024"]!!.jsonObject["February"]!!
                .jsonObject["categories"]!!.jsonObject["Food"] as JsonArray
            )[0].jsonObject
        assertEquals("10", entry["amount"]!!.jsonPrimitive.content)
        assertEquals(10L, entry["amount"]!!.jsonPrimitive.longOrNull)
    }

    @Test
    fun `error bodies are embedded verbatim, including non-JSON and wrapped 404s`() = runBlocking<Unit> {
        // F30 (text/plain 500), F02 (KeyError leak), F06/F22 (404 wrapped as 400).
        // Explicit (code, body, call, expected) tuples — never derive the call
        // from the expected message.
        val cases: List<Triple<Int, String, suspend () -> Unit>> = listOf(
            Triple(500, "Internal Server Error", { service.getExpenses(2024, "February") }),
            Triple(400, """{"detail":"'categories'"}""", { service.addExpense(Expense("x", 1.0, "Food", 2024, "February")) }),
            Triple(400, """{"detail":"404: Finance document not found."}""", { service.updateIncome(2024, "February", 1.0) }),
            Triple(400, """{"detail":"404: Expense not found."}""", { service.updateExpense("a", 1.0, "Food", "b", 2.0, "Food", 2024, "February") }),
        )
        val actions = listOf("get expenses", "add expense", "update income", "update expense")
        for ((i, case) in cases.withIndex()) {
            val (code, body, call) = case
            server.enqueue(MockResponse().setResponseCode(code).setBody(body))
            val e = assertThrows(ExpenseApiException::class.java) { runBlocking { call() } }
            assertEquals("Failed to ${actions[i]}: $body", e.message)
            server.takeRequest()
        }
    }

    @Test
    fun `getters also accept only status 200`() = runBlocking<Unit> {
        server.enqueue(MockResponse().setResponseCode(201).setBody("{}"))
        assertEquals(
            "Failed to get expenses: {}",
            assertThrows(ExpenseApiException::class.java) {
                runBlocking { service.getExpenses(2024, "February") }
            }.message,
        )
        server.takeRequest()
        server.enqueue(MockResponse().setResponseCode(204))
        assertEquals(
            "Failed to get income: ",
            assertThrows(ExpenseApiException::class.java) {
                runBlocking { service.getIncome(2024, "February") }
            }.message,
        )
        server.takeRequest()
    }

    @Test
    fun `status is checked before decoding, so non-object error bodies stay API errors`() = runBlocking<Unit> {
        server.enqueue(MockResponse().setResponseCode(400).setBody("[]"))
        assertEquals(
            "Failed to get income: []",
            assertThrows(ExpenseApiException::class.java) {
                runBlocking { service.getIncome(2024, "February") }
            }.message,
        )
        server.takeRequest()
    }

    @Test
    fun `getIncome month-only filter reaches the wire`() = runBlocking<Unit> {
        server.enqueue(ok())
        service.getIncome(month = "November")
        assertEquals("/income/alice?month=November", server.takeRequest().path)
    }

    @Test
    fun `getIncome query values use Dart encodeQueryComponent, not raw interpolation`() = runBlocking<Unit> {
        // Goldens executed against Dart 3.14 Uri.replace(queryParameters:).
        val cases = listOf(
            "Nov 5" to "/income/alice?year=2026&month=Nov+5",
            "a&b=c" to "/income/alice?year=2026&month=a%26b%3Dc",
            "a+b" to "/income/alice?year=2026&month=a%2Bb",
            "a#b" to "/income/alice?year=2026&month=a%23b",
            "100%" to "/income/alice?year=2026&month=100%25",
            "*~" to "/income/alice?year=2026&month=%2A~",
            "" to "/income/alice?year=2026&month",
        )
        for ((month, path) in cases) {
            server.enqueue(ok())
            service.getIncome(2026, month)
            assertEquals(path, server.takeRequest().path)
        }
    }

    @Test
    fun `request bodies escape quotes and control chars exactly like Dart jsonEncode`() = runBlocking<Unit> {
        server.enqueue(ok())
        service.addExpense(Expense("Café \"x\"\n", 12.5, "Food", 2026, "November"))
        assertEquals(
            "{\"name\":\"Café \\\"x\\\"\\n\",\"amount\":12.5,\"category\":\"Food\",\"year\":2026,\"month\":\"November\"}",
            server.takeRequest().body.readUtf8(),
        )
    }

    @Test
    fun `deleteExpense hash truncates the query at parse time, like Dart`() = runBlocking<Unit> {
        server.enqueue(ok())
        service.deleteExpense(2026, "November", "Food", "Cola #1")
        assertEquals(
            "/expenses/alice?year=2026&month=November&category=Food&name=Cola%20",
            server.takeRequest().path,
        )
    }

    @Test
    fun `year zero and negative years are sent, client side uses null-checks not truthiness`() = runBlocking<Unit> {
        server.enqueue(ok())
        service.getExpenses(year = 0)
        assertEquals("/expenses/alice?year=0", server.takeRequest().path)
        server.enqueue(ok())
        service.getExpenses(year = -1, month = "November")
        assertEquals("/expenses/alice?year=-1&month=November", server.takeRequest().path)
    }

    @Test
    fun `base url path prefix is kept and trailing slash doubles, as in Dart`() = runBlocking<Unit> {
        val prefixed = ExpenseService(server.url("/api").toString(), "alice")
        server.enqueue(ok())
        prefixed.addExpense(Expense("x", 1.0, "Food", 2026, "November"))
        assertEquals("/api/expenses/alice", server.takeRequest().path)

        val slashed = ExpenseService(server.url("/").toString(), "alice")
        server.enqueue(ok())
        slashed.addExpense(Expense("x", 1.0, "Food", 2026, "November"))
        assertEquals("//expenses/alice", server.takeRequest().path)
    }

    @Test
    fun `no retry on connection failure, because POST expenses appends server-side`() = runBlocking<Unit> {
        // Dart's http client never retries; OkHttp's default would resend the
        // buffered POST and duplicate the expense. Parity: exactly one attempt.
        server.enqueue(MockResponse().setSocketPolicy(okhttp3.mockwebserver.SocketPolicy.DISCONNECT_AFTER_REQUEST))
        assertThrows(java.io.IOException::class.java) {
            runBlocking { service.addExpense(Expense("x", 1.0, "Food", 2026, "November")) }
        }
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `deleteExpense spaces are percent-encoded at parse time, like Dart Uri parse`() = runBlocking<Unit> {
        // Verified against Dart 3.14: Uri.parse keeps raw spaces in the query
        // and emits %20 on the wire; OkHttp HttpUrl.parse does the same.
        server.enqueue(ok())
        service.deleteExpense(2026, "November", "Fast Food", "Cola Zero")
        val req = server.takeRequest()
        assertEquals(
            "/expenses/alice?year=2026&month=November&category=Fast%20Food&name=Cola%20Zero",
            req.path,
        )
        assertEquals("Cola Zero", req.requestUrl!!.queryParameter("name"))
    }

    @Test
    fun `expense payload keeps int and float primitives distinct`() = runBlocking<Unit> {
        val payload = """{"2026":{"November":{"categories":{"Food":[{"name":"Lunch","amount":12.5}]}}}}"""
        server.enqueue(ok(payload))
        val res = service.getExpenses(2026, "November")
        val entry = (
            res["2026"]!!.jsonObject["November"]!!.jsonObject["categories"]!!
                .jsonObject["Food"] as JsonArray
            )[0].jsonObject
        assertEquals("12.5", entry["amount"]!!.jsonPrimitive.content)
        assertNull(entry["amount"]!!.jsonPrimitive.longOrNull)
    }
}