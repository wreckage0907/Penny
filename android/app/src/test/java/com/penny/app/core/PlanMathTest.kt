package com.penny.app.core

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * PlanMath (v2.1 budget model): every number on Home and both Budget tabs,
 * checked against the artboard's own "Dataset check" — income ₹85,000,
 * spend ₹31,450 across five categories, SIP ₹17,000 → retained ₹53,550,
 * unallocated ₹36,550, Needs/Wants/SIP targets of ₹42,500/₹25,500/₹17,000.
 */
class PlanMathTest {

    private val demoCategories = mapOf(
        BudgetCategories.FOOD to (11_200.0 to 6),
        BudgetCategories.RENT to (9_500.0 to 3),
        BudgetCategories.SHOPPING to (5_450.0 to 2),
        BudgetCategories.TRANSPORT to (3_100.0 to 4),
        BudgetCategories.ENTERTAINMENT to (2_200.0 to 3),
    )

    private fun demoMonth() = PlanMonth(
        income = 85_000.0,
        categories = demoCategories,
        sipRecorded = 17_000.0,
        goalAllocated = 0.0,
    )

    @Test
    fun `the artboard dataset reconciles exactly`() {
        val s = PlanMath.summary(demoMonth())
        assertEquals(31_450.0, s.spent, 0.0)
        assertEquals(53_550.0, s.retained, 0.0)
        assertEquals(23_800.0, s.needs.actual, 0.0) // Food + Rent + Transport
        assertEquals(42_500.0, s.needs.target, 0.0)
        assertEquals(18_700.0, s.needs.remainder, 0.0)
        assertEquals(7_650.0, s.wants.actual, 0.0) // Shopping + Entertainment
        assertEquals(25_500.0, s.wants.target, 0.0)
        assertEquals(17_850.0, s.wants.remainder, 0.0)
        assertEquals(17_000.0, s.sipRecorded, 0.0)
        assertEquals(17_000.0, s.sipGoal, 0.0) // 20% of 85,000
        assertEquals(36_550.0, s.unallocated, 0.0) // retained − SIP
        assertEquals("63%", Inr.share0(s.retained, s.income))
        assertEquals("37%", Inr.share0(s.spent, s.income))
        assertEquals("56%", Inr.share0(s.needs.actual, s.needs.target))
        assertEquals("100%", Inr.share0(s.sipRecorded, s.sipGoal))
        // The artboard's own identity: remainders + SIP = retained.
        assertEquals(s.retained, s.needs.remainder + s.wants.remainder + s.sipRecorded, 0.0)
    }

    @Test
    fun `a goal allocation reduces the unallocated remainder without touching SIP`() {
        val s = PlanMath.summary(demoMonth().copy(goalAllocated = 2_150.0))
        assertEquals(34_400.0, s.unallocated, 0.0)
        assertEquals(17_000.0, s.sipRecorded, 0.0)
    }

    @Test
    fun `first run is empty only with no income and no expenses`() {
        assertFalse(PlanMath.isEmpty(PlanMath.summary(demoMonth())))
        assertTrue(PlanMath.isEmpty(PlanMath.summary(PlanMonth(0.0, emptyMap(), 0.0, 0.0))))
        assertFalse(PlanMath.isEmpty(PlanMath.summary(demoMonth().copy(income = 0.0)))) // spent, still activity
    }

    @Test
    fun `ranked categories order by amount, largest first`() {
        val ranked = PlanMath.rankedCategories(demoMonth())
        assertEquals(
            listOf(BudgetCategories.FOOD, BudgetCategories.RENT, BudgetCategories.SHOPPING, BudgetCategories.TRANSPORT, BudgetCategories.ENTERTAINMENT),
            ranked.map { it.first },
        )
    }

    @Test
    fun `lenient income reads the backend's int zero and strings safely`() {
        val income = buildJsonObject {
            put("2026", buildJsonObject {
                put("November", JsonPrimitive(0)) // int-income quirk payload
                put("December", JsonPrimitive(48_000.0))
            })
        }
        assertEquals(0.0, PlanMath.income(income, 2026, "November"), 0.0)
        assertEquals(48_000.0, PlanMath.income(income, 2026, "December"), 0.0)
        assertEquals(0.0, PlanMath.income(income, 2025, "November"), 0.0)
        assertEquals(0.0, PlanMath.income(buildJsonObject {}, 2026, "November"), 0.0)
    }

    @Test
    fun `month categories reduce the wire JSON with counts`() = runBlocking {
        val api = FakeExpenseBackend()
        api.addExpense(Expense("Chai", 200.0, BudgetCategories.FOOD, 2026, "November"))
        api.addExpense(Expense("Rent", 7_500.0, BudgetCategories.RENT, 2026, "November"))
        val data = api.getExpenses(2026, "November")
        val categories = PlanMath.monthCategories(data, 2026, "November")
        assertEquals(200.0 to 1, categories[BudgetCategories.FOOD])
        assertEquals(7_500.0 to 1, categories[BudgetCategories.RENT])
        assertEquals(2, categories.values.sumOf { it.second })
    }

    @Test
    fun `the month stepper wraps across year boundaries`() {
        assertEquals(2026 to "December", PlanMath.shift(2026, "November", 1))
        assertEquals(2026 to "October", PlanMath.shift(2026, "November", -1))
        assertEquals(2025 to "December", PlanMath.shift(2026, "January", -1))
        assertEquals(2026 to "January", PlanMath.shift(2025, "December", 1))
        assertEquals(2027 to "February", PlanMath.shift(2026, "February", 12))
        assertEquals("Nov 2026", PlanMath.monthLabel(2026, "November"))
    }
}

/** Allocation reduction (frame 08: only SIP-kind counts toward the 20% goal). */
class AllocationsTest {

    @Test
    fun `sip and goal totals separate by kind`() {
        // The totals reduce an ALREADY month-scoped list (forMonth does the scoping).
        val month = listOf(
            Allocation(AllocationKind.SIP, "Monthly SIP", 17_000.0, 2026, "November"),
            Allocation(AllocationKind.GOAL, "Emergency Fund", 2_150.0, 2026, "November"),
        )
        assertEquals(17_000.0, Allocations.sipTotal(month), 0.0)
        assertEquals(2_150.0, Allocations.goalTotal(month), 0.0)
        val store = InMemoryAllocationStore()
        runBlocking { store.record("demo", month[0]) }
        assertEquals(1, runBlocking { store.forMonth("demo", 2026, "November").size })
        assertEquals(0, runBlocking { store.forMonth("other", 2026, "November").size })
    }
}
