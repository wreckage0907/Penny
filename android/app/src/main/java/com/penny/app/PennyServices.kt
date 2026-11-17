package com.penny.app

import com.penny.app.core.AuthService
import com.penny.app.core.Expense
import com.penny.app.core.ExpenseApi
import com.penny.app.core.ExpenseDashboardText
import com.penny.app.core.FakeExpenseBackend
import com.penny.app.core.FakeAuthService
import com.penny.app.core.FakeProfileImages
import com.penny.app.core.FakeUserDirectory
import com.penny.app.core.InMemoryUserPrefs
import com.penny.app.core.ProfileImages
import com.penny.app.core.UserDirectory
import com.penny.app.core.UserPrefs
import kotlinx.coroutines.runBlocking

/**
 * Everything the screens talk to, behind interfaces. Until the maintainer
 * picks a backend/auth stack every entry is a fake that mimics the current
 * services; tests build their own instance.
 */
class PennyServices(
    val auth: AuthService,
    val users: UserDirectory,
    val prefs: UserPrefs = InMemoryUserPrefs(),
    val profileImages: ProfileImages = FakeProfileImages(),
    /** `ExpenseService(userId: username)` — one backend view per username. */
    val expenses: (String) -> ExpenseApi = FakeExpenseRegistry()::forUser,
) {
    companion object {
        /** Demo world: account demo@penny.app / penny123, onboarded as "demo". */
        fun fakes(signedIn: Boolean = false): PennyServices {
            val auth = FakeAuthService(
                mapOf(FakeAuthService.DEMO_EMAIL to FakeAuthService.DEMO_PASSWORD),
                signedInAs = if (signedIn) FakeAuthService.DEMO_EMAIL else null,
            )
            val users = runBlocking { FakeUserDirectory.demo(auth) }
            return PennyServices(auth, users, expenses = FakeExpenseRegistry.demo()::forUser)
        }
    }
}

/** Process-wide instance used by MainActivity (survives configuration changes). */
object AppGraph {
    val services: PennyServices by lazy { PennyServices.fakes() }
}

/** One in-memory [FakeExpenseBackend] per username (Firestore collection per user). */
class FakeExpenseRegistry {
    private val backends = mutableMapOf<String, FakeExpenseBackend>()
    fun forUser(username: String): FakeExpenseBackend = backends.getOrPut(username) { FakeExpenseBackend() }

    companion object {
        /** The demo user with sample expenses + income this month and last. */
        fun demo(today: java.time.LocalDate = java.time.LocalDate.now()): FakeExpenseRegistry {
            val registry = FakeExpenseRegistry()
            val api = registry.forUser("demo")
            val month = ExpenseDashboardText.initialMonth(today.monthValue)
            val prev = today.minusMonths(1)
            val prevMonth = ExpenseDashboardText.initialMonth(prev.monthValue)
            runBlocking {
                listOf(
                    Expense("Groceries", 3250.5, "Food", today.year, month),
                    Expense("Dinner out", 1200.0, "Food", today.year, month),
                    Expense("Metro pass", 800.0, "Transport", today.year, month),
                    Expense("Electricity", 1450.75, "Utility Bills", today.year, month),
                    Expense("Shoes", 2499.99, "Shopping", today.year, month),
                    Expense("Movie night", 650.0, "Entertainment", today.year, month),
                    Expense("Rent share", 9000.0, "Miscellaneous", prev.year, prevMonth),
                ).forEach { api.addExpense(it) }
                api.addIncome(50_000.0, today.year, month)
                api.addIncome(48_000.0, prev.year, prevMonth)
            }
            return registry
        }
    }
}
