package com.penny.app

import android.content.Context
import com.penny.app.core.AuthService
import com.penny.app.core.ChatModel
import com.penny.app.core.FakeChatModel
import com.penny.app.core.CourseContent
import com.penny.app.core.FakeCourseContent
import com.penny.app.core.FakeMcqBackend
import com.penny.app.core.FakeStockMarket
import com.penny.app.core.StockMarket
import com.penny.app.core.McqApi
import com.penny.app.core.Speech
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
    val courses: CourseContent = FakeCourseContent(),
    /** Text-to-speech factory; null = the Android TextToSpeech implementation. */
    val speech: ((Context) -> Speech)? = null,
    val mcq: McqApi = FakeMcqBackend(),
    /** Gallery picker; null = the system photo picker. Returns an image URI or null if cancelled. */
    val pickImage: (suspend () -> String?)? = null,
    val stocks: StockMarket = FakeStockMarket(),
    /** Gemini stand-in (Flutter bundles the API key — security decision pending). */
    val chat: ChatModel = FakeChatModel(),
) {
    companion object {
        /** Demo world: account demo@penny.app / penny123, onboarded as "demo". */
        fun fakes(signedIn: Boolean = false): PennyServices {
            val auth = FakeAuthService(
                mapOf(FakeAuthService.DEMO_EMAIL to FakeAuthService.DEMO_PASSWORD),
                signedInAs = if (signedIn) FakeAuthService.DEMO_EMAIL else null,
            )
            val expenses = FakeExpenseRegistry.demo()
            val images = FakeProfileImages()
            // delete_user also drops the user's expenses doc and profile blobs.
            val users = runBlocking {
                FakeUserDirectory.demo(auth) { username ->
                    expenses.remove(username)
                    runBlocking { images.delete(username) }
                }
            }
            return PennyServices(auth, users, profileImages = images, expenses = expenses::forUser)
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
    fun remove(username: String) { backends.remove(username) }

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
