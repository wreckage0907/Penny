package com.penny.app.core

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Course list / learning path / lesson logic (the Pages/coursePage pages) on the fake store. */
class CourseFlowsTest {

    private val store = FakeCourseContent()
    private fun obj(json: String) = Json.parseToJsonElement(json).jsonObject

    @Test
    fun `subfolders come back unordered with an empty name, the page filters and sorts`() = runBlocking {
        val raw = store.subfolders()
        assertEquals("""{"title":["3_credit_and_debt_management","2_banking_and_accounts","1_personal_finance",""]}""", raw.toString())
        val modules = CourseFlows.modules(raw)
        assertEquals(listOf("1_personal_finance", "2_banking_and_accounts", "3_credit_and_debt_management"), modules)
        // Display names hit exactly the three learning_page background keys.
        assertEquals(CourseFlows.BACKGROUNDS, modules.map(CourseText::moduleDisplayName))
    }

    @Test
    fun `any malformed folder list leaves the page empty`() {
        assertEquals(emptyList<String>(), CourseFlows.modules(null))
        assertEquals(emptyList<String>(), CourseFlows.modules(obj("""{"other":[]}""")))
        assertEquals(emptyList<String>(), CourseFlows.modules(obj("""{"title":"1_a"}""")))
        assertEquals(emptyList<String>(), CourseFlows.modules(obj("""{"title":["1_a",5]}""")))
    }

    @Test
    fun `lesson files follow the prefix-index naming and failures show the literal text`() = runBlocking {
        val c = CourseFlows.lessonContent(store.file("1_personal_finance", CourseText.lessonFileName("1_personal_finance", 0)))
        assertTrue(c.startsWith("# Lesson 1: What is personal finance"))
        assertTrue(CourseFlows.lessonContent(store.file("3_credit_and_debt_management", "3_8")).contains("Lesson 8: Review"))
        assertEquals("Failed to load content", CourseFlows.lessonContent(runCatching { store.file("1_personal_finance", "1_9") }.getOrNull()))
        assertEquals("Failed to load content", CourseFlows.lessonContent(runCatching { store.file("9_x", "9_1") }.getOrNull()))
        assertEquals("Failed to load content", CourseFlows.lessonContent(obj("""{"content":null}""")))
    }

    @Test
    fun `spoken text is the 004 cleaner applied to the lesson`() = runBlocking {
        val md = CourseFlows.lessonContent(store.file("1_personal_finance", "1_1"))
        val spoken = LessonSpeech.spokenText(md)
        assertTrue(spoken.startsWith("Lesson 1: What is personal finance"))
        assertFalse(spoken.contains("#") || spoken.contains("**") || spoken.contains("https://"))
        assertEquals("en-US", LessonSpeech.LANGUAGE)
        assertEquals(0.8f, LessonSpeech.PITCH)
    }

    @Test
    fun `learning path circles and hit testing match the painter`() {
        val w = 400.0
        val h = LearningPath.HEIGHT
        assertEquals(
            listOf(200.0 to 1520.0, 100.0 to 1330.0, 200.0 to 1140.0, 300.0 to 950.0,
                200.0 to 760.0, 100.0 to 570.0, 200.0 to 380.0, 300.0 to 190.0),
            LearningPath.circles(w, h).map { it.x to it.y },
        )
        assertEquals(0, LearningPath.hit(200.0, 1520.0, w, h))
        assertEquals(0, LearningPath.hit(240.0, 1520.0, w, h)) // distance <= radius, inclusive
        assertNull(LearningPath.hit(240.01, 1520.0, w, h))
        assertEquals(7, LearningPath.hit(300.0, 190.0, w, h))
        assertEquals(LearningPath.LESSONS, LearningPath.circles(w, h).size)
        // The winding path is offset from the circles (160 - w/4, 40 + 3w/4, 10 + 3w/4).
        assertEquals(60.0, LearningPath.pathPoints(w, h)[1].x, 0.0)
        assertEquals(340.0, LearningPath.pathPoints(w, h)[3].x, 0.0)
        assertEquals(310.0, LearningPath.pathPoints(w, h)[7].x, 0.0)
    }
}
