package com.penny.app.core

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

/** Home page logic (Pages/home/home.dart). */
class HomeLogicTest {

    private val financeJson = File("src/main/assets/finance.json").readText()

    @Test
    fun `claims win only when both username and fullName are present`() {
        val both = mapOf("username" to "demo", "fullName" to "Demo User")
        assertEquals("demo" to "Demo User", HomeLogic.identity(both, "p", "Prefs Name"))
        // One claim missing → BOTH come from prefs (home.dart:80-92).
        assertEquals("p" to "Prefs Name", HomeLogic.identity(mapOf("username" to "demo"), "p", "Prefs Name"))
        assertEquals("p" to null, HomeLogic.identity(emptyMap(), "p", null))
        assertEquals(null to null, HomeLogic.identity(null, null, null))
    }

    @Test
    fun `greeting interpolates like Dart, null included`() {
        assertEquals("WELCOME,\nDemo User", HomeLogic.greeting("Demo User"))
        assertEquals("WELCOME,\nnull", HomeLogic.greeting(null))
    }

    @Test
    fun `grid tiles keep Flutter titles, order and routes`() {
        assertEquals(
            listOf("Course\nPage" to "/coursepage", "Expense\nTracker" to "/budget",
                "Predict\nStocks" to "/stockprofile", "Practice" to "/practice"),
            HomeLogic.TILES.map { it.title to it.route },
        )
        assertEquals("74%", HomeLogic.COURSE_PROGRESS_LABEL) // hard-coded, not computed
        assertEquals(0.7f, HomeLogic.COURSE_PROGRESS)
    }

    @Test
    fun `the bundled finance json fills exactly the 20 carousel pages`() {
        val articles = HomeLogic.articles(financeJson)
        assertEquals(HomeLogic.NEWS_PAGES, articles.size)
        val raw = Json.parseToJsonElement(financeJson).jsonObject["articles"] as JsonArray
        (0 until HomeLogic.NEWS_PAGES).forEach { i ->
            val card = HomeLogic.newsCard(articles, i)!!
            val a = raw[i].jsonObject
            assertEquals(a["title"]!!.jsonPrimitive.content, card.title)
            assertEquals(a["urlToImage"]!!.jsonPrimitive.content, card.imageUrl)
            assertEquals(a["url"]!!.jsonPrimitive.content, card.url)
        }
        assertEquals("Bajaj Freedom CNG and petrol mileage explained! - BikeWale", HomeLogic.newsCard(articles, 0)!!.title)
        // Page 21 would be a Dart RangeError at build.
        assertNull(HomeLogic.newsCard(articles, 20))
    }

    @Test
    fun `null article fields fall back like Dart`() {
        val articles = Json.parseToJsonElement("""[{"title":null,"url":"u"},{}]""") as JsonArray
        assertEquals(NewsCard("No title", "No description", HomeLogic.IMAGE_PLACEHOLDER, "u"),
            HomeLogic.newsCard(articles, 0))
        assertEquals(NewsCard("No title", "No description", HomeLogic.IMAGE_PLACEHOLDER, null),
            HomeLogic.newsCard(articles, 1))
    }

    @Test
    fun `fake profile images mimic url-or-null`() = runBlocking {
        val images = FakeProfileImages()
        assertNull(images.imageUrl("demo"))
        images.set("demo", "https://example.invalid/demo.jpg")
        assertEquals("https://example.invalid/demo.jpg", images.imageUrl("demo"))
    }

    @Test
    fun `sign-out clears auth and the username prefs`() = runBlocking {
        val auth = FakeAuthService.demo()
        val prefs = InMemoryUserPrefs()
        auth.login(FakeAuthService.DEMO_EMAIL, FakeAuthService.DEMO_PASSWORD)
        prefs.saveUsername("demo"); prefs.saveFullName("Demo User")
        AuthFlows.signOut(auth, prefs)
        assertEquals(AuthState.SignedOut, auth.state.value)
        assertNull(prefs.username()); assertNull(prefs.fullName())
    }
}
