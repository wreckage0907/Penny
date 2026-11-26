package com.penny.app

import android.content.Context
import com.penny.app.core.Allocation
import com.penny.app.core.AllocationKind
import com.penny.app.core.AllocationStore
import com.penny.app.core.AuthService
import com.penny.app.core.ChatModel
import com.penny.app.core.FakeChatModel
import com.penny.app.core.FakeConnectivity
import com.penny.app.core.CourseContent
import com.penny.app.core.FakeCourseContent
import com.penny.app.core.FakeMcqBackend
import com.penny.app.core.FakeStockMarket
import com.penny.app.core.InMemoryLearnStore
import com.penny.app.core.LearnStore
import com.penny.app.core.LearnStores
import com.penny.app.core.StockMarket
import com.penny.app.core.McqApi
import com.penny.app.core.Speech
import com.penny.app.core.Expense
import com.penny.app.core.ExpenseApi
import com.penny.app.core.FakeAuthService
import com.penny.app.core.FakeExpenseBackend
import com.penny.app.core.FakeProfileImages
import com.penny.app.core.FakeUserDirectory
import com.penny.app.core.InMemoryAllocationStore
import com.penny.app.core.InMemoryTransactionMeta
import com.penny.app.core.InMemoryUserPrefs
import com.penny.app.core.ProfileImages
import com.penny.app.core.TransactionMeta
import com.penny.app.core.TxDetail
import com.penny.app.core.UserDirectory
import com.penny.app.core.UserPrefs
import kotlinx.coroutines.runBlocking
import java.time.LocalDate

/**
 * Everything the screens talk to, behind interfaces. Until the maintainer
 * picks a backend/auth stack every entry is a fake that mimics the current
 * services; tests build their own instance.
 *
 * v2.1 additions (redesign): transaction display metadata, recorded
 * allocations, learning progress, connectivity, and the grounded chat
 * context — all fakes, all optional seams a real stack must fill in.
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
    /** Penny AI stand-in (provider decision pending, LEDGER 015). */
    val chat: ChatModel = FakeChatModel(),
    /** Per-transaction display metadata (day · subcategory · payment). */
    val meta: TransactionMeta = InMemoryTransactionMeta(),
    /** Recorded SIP / goal allocations. */
    val allocations: AllocationStore = InMemoryAllocationStore(),
    /** Learning journey: coins, streak, completed lessons, quiz sessions. */
    val learn: LearnStore = InMemoryLearnStore(),
    /** Offline/online lever for the S04 state library. */
    val connectivity: FakeConnectivity = FakeConnectivity(),
) {
    companion object {
        /** Demo world: account demo@penny.app / penny123, onboarded as "demo". */
        fun fakes(signedIn: Boolean = false, today: LocalDate = LocalDate.now()): PennyServices {
            val auth = FakeAuthService(
                mapOf(FakeAuthService.DEMO_EMAIL to FakeAuthService.DEMO_PASSWORD),
                signedInAs = if (signedIn) FakeAuthService.DEMO_EMAIL else null,
            )
            // Frame 02's expired-code state is seeded so the recovery flow's
            // "This code expired" path is demoable.
            auth.seedExpiredReset(FakeAuthService.DEMO_EMAIL, FakeAuthService.EXPIRED_DEMO_CODE)
            val registry = FakeExpenseRegistry()
            val meta = InMemoryTransactionMeta()
            val allocations = InMemoryAllocationStore()
            DemoMonth.seed(registry.forUser("demo"), meta, allocations, "demo", today)
            val images = FakeProfileImages()
            // delete_user also drops the user's expenses doc and profile blobs.
            val users = runBlocking {
                FakeUserDirectory.demo(auth) { username ->
                    registry.remove(username)
                    runBlocking { images.delete(username) }
                }
            }
            return PennyServices(
                auth, users, profileImages = images, expenses = registry::forUser,
                meta = meta, allocations = allocations,
                learn = InMemoryLearnStore(LearnStores.demo(today)),
            )
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
}

/**
 * The demo month of the v2.1 artboard: income ₹85,000, 18 expenses across
 * five categories totalling ₹31,450, one recorded SIP of ₹17,000 — the
 * dataset every frame 04–08/S02 number derives from (see the artboard's
 * "Dataset check"). Seeded into `today`'s month so the demo always populates.
 */
object DemoMonth {
    // (name, amount, category, day, subcategory, payment)
    private val EXPENSES = listOf(
        // Food & Dining — ₹11,200 · 6 items
        "BigBasket Monthly" to T(3850.0, "Food & Dining", 18, "Groceries", "UPI"),
        "Swiggy Family Dinner" to T(1850.0, "Food & Dining", 14, "Dining out", "Card"),
        "Nature's Basket" to T(1800.0, "Food & Dining", 9, "Groceries", "UPI"),
        "Filter Coffee & Tiffin" to T(1400.0, "Food & Dining", 4, "Everyday food", "Cash"),
        "Restaurant Dinner" to T(2100.0, "Food & Dining", 2, "Dining out", "Card"),
        "Chai & Snacks" to T(200.0, "Food & Dining", 1, "Everyday food", "UPI"),
        // Rent & Utilities — ₹9,500 · 3 items
        "Flat Rent" to T(7500.0, "Rent & Utilities", 1, "Rent", "UPI"),
        "Electricity Bill" to T(1200.0, "Rent & Utilities", 5, "Bills", "UPI"),
        "Broadband" to T(800.0, "Rent & Utilities", 6, "Bills", "UPI"),
        // Shopping — ₹5,450 · 2 items
        "Amazon Sale Haul" to T(3200.0, "Shopping", 11, "Online", "Card"),
        "Diwali Clothes" to T(2250.0, "Shopping", 20, "Apparel", "Card"),
        // Transport & Metro — ₹3,100 · 4 items
        "Metro Pass" to T(1500.0, "Transport & Metro", 3, "Commute", "UPI"),
        "Cab to Airport" to T(900.0, "Transport & Metro", 21, "Travel", "UPI"),
        "Petrol Top-up" to T(450.0, "Transport & Metro", 16, "Fuel", "Card"),
        "Auto Rides" to T(250.0, "Transport & Metro", 8, "Commute", "Cash"),
        // Entertainment — ₹2,200 · 3 items
        "Movie Night" to T(750.0, "Entertainment", 12, "Movies", "UPI"),
        "Concert Tickets" to T(1200.0, "Entertainment", 22, "Live events", "Card"),
        "Streaming Subscription" to T(250.0, "Entertainment", 7, "Streaming", "UPI"),
    )

    const val INCOME = 85_000.0
    const val SIP = 17_000.0

    private data class T(val amount: Double, val category: String, val day: Int, val sub: String, val payment: String)

    fun seed(
        api: FakeExpenseBackend,
        meta: TransactionMeta,
        allocations: AllocationStore,
        user: String,
        today: LocalDate,
    ) {
        val month = com.penny.app.core.ExpenseDashboardText.initialMonth(today.monthValue)
        val year = today.year
        runBlocking {
            EXPENSES.forEach { (name, t) ->
                api.addExpense(Expense(name, t.amount, t.category, year, month))
                meta.put(user, year, month, t.category, name, TxDetail(t.day, t.sub, t.payment))
            }
            api.addIncome(INCOME, year, month)
            allocations.record(user, Allocation(AllocationKind.SIP, "Monthly SIP", SIP, year, month))
        }
    }
}
