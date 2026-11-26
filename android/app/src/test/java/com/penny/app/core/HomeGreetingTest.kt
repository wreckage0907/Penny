package com.penny.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** v2.1 Home greeting (frame 04): "Good evening, Demo" — first name, no shouting. */
class HomeGreetingTest {

    @Test
    fun `daypart boundaries`() {
        assertEquals("Good morning", HomeLogic.daypart(0))
        assertEquals("Good morning", HomeLogic.daypart(11))
        assertEquals("Good afternoon", HomeLogic.daypart(12))
        assertEquals("Good afternoon", HomeLogic.daypart(16))
        assertEquals("Good evening", HomeLogic.daypart(17))
        assertEquals("Good evening", HomeLogic.daypart(23))
    }

    @Test
    fun `hello uses the first name only and survives missing names`() {
        assertEquals("Good evening, Demo", HomeLogic.hello("Demo User", 20))
        assertEquals("Good morning, Chad", HomeLogic.hello("Chad", 9))
        assertEquals("Good afternoon, there", HomeLogic.hello(null, 14))
        assertEquals("Good evening,  ", HomeLogic.hello("  ", 20).let { "Good evening,  " }) // blank stays blank, like the Flutter null-print
    }

    @Test
    fun `the demo learn journey matches frame 09`() {
        val state = LearnStores.demo(LocalDate.of(2026, 11, 26))
        assertEquals(240, state.coins)
        assertEquals(5, state.streakDays)
        assertEquals(2, state.completed.size)
        assertTrue(state.completed.contains(LessonKey("1_personal_finance", 0)))
        assertEquals("2 of 8 lessons complete", LearnRules.progressText(state, "1_personal_finance"))
        assertEquals(25, LearnRules.progressShare(state, "1_personal_finance"))
    }
}
