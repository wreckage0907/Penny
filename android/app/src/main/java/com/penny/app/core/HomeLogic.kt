package com.penny.app.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

/**
 * Backend profile pictures: `GET /prof/{username}` → `{"url": <signed URL |
 * null>}` (backend/routers/profile.py:34-48; URLs expire after 1 hour, hence
 * Home's 50-minute refresh).
 */
interface ProfileImages {
    /** The signed URL, or null when the user has no picture. Throws on transport/5xx. */
    suspend fun imageUrl(username: String): String?

    /** `PUT /prof/{username}` (replace): true = 200. Throws on transport errors. */
    suspend fun upload(username: String, localImage: String): Boolean

    /**
     * `DELETE /prof/{username}`: 200 whether or not a picture existed
     * ("No profile picture found" is still a 200). Throws on transport errors.
     */
    suspend fun delete(username: String): Boolean
}

/** In-memory mimic: nobody has a picture until one is set or uploaded. */
class FakeProfileImages : ProfileImages {
    private val urls = mutableMapOf<String, String>()
    fun set(username: String, url: String) { urls[username] = url }
    override suspend fun imageUrl(username: String): String? = urls[username]
    override suspend fun upload(username: String, localImage: String): Boolean {
        urls[username] = localImage // the fake "signed URL" is the picked image itself
        return true
    }
    override suspend fun delete(username: String): Boolean {
        urls.remove(username)
        return true
    }
}

/** One Finance News card (home.dart:135-205). */
data class NewsCard(val title: String, val description: String, val imageUrl: String, val url: String?)

/** Pure logic of the Home page (Pages/home/home.dart). */
object HomeLogic {

    /**
     * _loadUserData (:73-101): claims win only when BOTH username and
     * fullName claims are present; otherwise both come from prefs.
     */
    fun identity(claims: Map<String, String>?, prefsUsername: String?, prefsFullName: String?): Pair<String?, String?> =
        if (claims != null && claims["username"] != null && claims["fullName"] != null) {
            claims["username"] to claims["fullName"]
        } else {
            prefsUsername to prefsFullName
        }

    /** `'WELCOME,\n$fullName'` (:241) — Dart prints a null name as "null". */
    fun greeting(fullName: String?): String = "WELCOME,\n${fullName ?: "null"}"

    /** v2.1 frame 04: "Good evening, Demo" — hour → daypart greeting. */
    fun daypart(hour: Int): String = when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else -> "Good evening"
    }

    /** "Good evening, Demo" — the first name only (v2.1, no shouting caps). */
    fun hello(fullName: String?, hour: Int): String =
        "${daypart(hour)}, ${(fullName ?: "there").trim().substringBefore(' ')}"

    /** Profile picture refresh period (:56-57). */
    const val PROFILE_REFRESH_MINUTES = 50L

    data class Tile(val title: String, val route: String)

    /** The 2×2 grid `data` list (:207-212) and its onTap routing (:289-360). */
    val TILES = listOf(
        Tile("Course\nPage", AppRoutes.COURSE_PAGE),
        Tile("Expense\nTracker", AppRoutes.BUDGET),
        Tile("Predict\nStocks", AppRoutes.STOCK_PROFILE),
        Tile("Practice", AppRoutes.PRACTICE),
    )

    /** The Course tile's progress is hard-coded: text "74%", ring 0.7 (:322-336). */
    const val COURSE_PROGRESS_LABEL = "74%"
    const val COURSE_PROGRESS = 0.7f

    /** `List.generate(20, …)` (:136-137): the carousel is ALWAYS 20 pages. */
    const val NEWS_PAGES = 20

    const val IMAGE_PLACEHOLDER = "https://via.placeholder.com/300x200"

    /** `json.decode(finance.json)['articles']` (:43-49). */
    fun articles(financeJson: String): JsonArray =
        Json.parseToJsonElement(financeJson).jsonObject["articles"] as JsonArray

    /**
     * Card i of the carousel. Dart indexes `articles[index]` for every one of
     * the 20 pages, so i >= articles.size is a RangeError at build (null here);
     * null fields fall back to "No title" / "No description" / placeholder.
     */
    fun newsCard(articles: JsonArray, index: Int): NewsCard? {
        val a = articles.getOrNull(index) as? JsonObject ?: return null
        fun str(key: String): String? = (a[key] as? JsonPrimitive)?.takeIf { it.isString }?.content
        return NewsCard(
            title = str("title") ?: "No title",
            description = str("description") ?: "No description",
            imageUrl = str("urlToImage") ?: IMAGE_PLACEHOLDER,
            url = str("url"),
        )
    }
}
