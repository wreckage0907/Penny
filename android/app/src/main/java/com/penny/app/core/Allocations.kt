package com.penny.app.core

/**
 * Recorded allocations (artboard frame 08 "Actual SIP · 20%": only
 * *completed allocations* count — unspent cash is not SIP). SIP entries are
 * the month's recorded goal; non-SIP goals (a Penny AI emergency-fund move)
 * reduce the unallocated remainder without pretending to be invested.
 *
 * New behaviour — the Flutter app had no notion of allocations. A real
 * implementation would need a backend store; until then the in-memory fake
 * is the only implementation.
 */
enum class AllocationKind { SIP, GOAL }

data class Allocation(
    val kind: AllocationKind,
    val label: String,
    val amount: Double,
    val year: Int,
    val month: String,
)

interface AllocationStore {
    suspend fun record(user: String, allocation: Allocation)
    suspend fun forMonth(user: String, year: Int, month: String): List<Allocation>
}

class InMemoryAllocationStore : AllocationStore {
    private val entries = mutableListOf<Pair<String, Allocation>>()

    override suspend fun record(user: String, allocation: Allocation) {
        entries.add(user to allocation)
    }

    override suspend fun forMonth(user: String, year: Int, month: String): List<Allocation> =
        entries.filter { it.first == user && it.second.year == year && it.second.month == month }
            .map { it.second }
}

/** Reduce a month's allocations to the [PlanMonth] inputs. */
object Allocations {
    fun sipTotal(allocations: List<Allocation>): Double =
        allocations.filter { it.kind == AllocationKind.SIP }.sumOf { it.amount }

    fun goalTotal(allocations: List<Allocation>): Double =
        allocations.filter { it.kind == AllocationKind.GOAL }.sumOf { it.amount }
}
