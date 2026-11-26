package com.penny.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.penny.app.PennyServices
import com.penny.app.R
import com.penny.app.core.Allocations
import com.penny.app.core.BudgetCategories
import com.penny.app.core.DashboardFlows
import com.penny.app.core.ExpenseDashboardText
import com.penny.app.core.HomeLogic
import com.penny.app.core.Inr
import com.penny.app.core.LearnRules
import com.penny.app.core.PlanMath
import com.penny.app.core.PlanMonth
import com.penny.app.core.PlanSummary
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime

/**
 * v2.1 Home (artboard frame 04 + S03 first-run): the month's hero (net cash
 * retained, never called "invested savings"), top spending, and the next
 * learning step — with the dock's repeated Add in thumb reach. Replaces the
 * old greeting/2×2-tiles/news-carousel page; the news surface is dropped by
 * the v2.1 IA (ledger).
 */
@Composable
fun HomeScreen(services: PennyServices, navigate: (String) -> Unit, tab: PennyTab = PennyTab.HOME) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var identity by remember { mutableStateOf<Pair<String?, String?>>(null to null) }
    var avatarUrl by remember { mutableStateOf<String?>(null) }
    var plan by remember { mutableStateOf<PlanSummary?>(null) }
    var ranked by remember { mutableStateOf<List<Pair<String, Pair<Double, Int>>>>(emptyList()) }
    var learn by remember { mutableStateOf(services.learn.state.value) }
    val today = remember { LocalDate.now() }
    val month = remember { ExpenseDashboardText.initialMonth(today.monthValue) }

    LaunchedEffect(Unit) {
        val claims = services.auth.customClaims()
        identity = HomeLogic.identity(claims, services.prefs.username(), services.prefs.fullName())
        avatarUrl = try {
            services.profileImages.imageUrl(identity.first ?: "")
        } catch (e: Exception) {
            null
        }
        val user = DashboardFlows.username(claims, services.prefs.username()) ?: ""
        val api = services.expenses(user)
        val expenses = try {
            api.getExpenses(today.year, month)
        } catch (e: Exception) {
            kotlinx.serialization.json.JsonObject(emptyMap())
        }
        val income = try {
            api.getIncome(today.year, month)
        } catch (e: Exception) {
            kotlinx.serialization.json.JsonObject(emptyMap())
        }
        val allocations = services.allocations.forMonth(user, today.year, month)
        val monthData = PlanMonth(
            income = PlanMath.income(income, today.year, month),
            categories = PlanMath.monthCategories(expenses, today.year, month),
            sipRecorded = Allocations.sipTotal(allocations),
            goalAllocated = Allocations.goalTotal(allocations),
        )
        plan = PlanMath.summary(monthData)
        ranked = PlanMath.rankedCategories(monthData)
    }
    LaunchedEffect(services.learn.state) {
        services.learn.state.collect { learn = it }
    }

    val (username, fullName) = identity
    val initials = (fullName ?: "?").split(' ').filter { it.isNotBlank() }
        .take(2).joinToString("") { it.first().uppercase() }

    Column(Modifier.fillMaxSize().tag("home-page")) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(top = 5.dp, bottom = 24.dp),
        ) {
            HomeHeader(
                hello = HomeLogic.hello(fullName, LocalTime.now().hour),
                sub = "${PlanMath.monthLabel(today.year, month)} · SIP goal recorded",
                avatarUrl = avatarUrl,
                initials = initials,
                onAi = { navigate("ai") },
                onProfile = { navigate("/profilesettings") },
            )
            val summary = plan
            if (summary == null) {
                HomeSkeleton()
            } else if (PlanMath.isEmpty(summary)) {
                FirstRun(navigate)
            } else {
                HomeHero(summary)
                SectionHead("Top Spending") {
                    TextLink("See all →", { navigate("/budget") }, tag = "home-see-all")
                }
                CardBox(tag = "home-top-spending") {
                    Column {
                        ranked.take(3).forEachIndexed { i, (category, total) ->
                            SpendRow(
                                name = category,
                                meta = "${Inr.share0(total.first, summary.spent)} of spend · ${total.second} items",
                                amount = Inr.spent(total.first),
                                dot = dotOf(category),
                                miniTrack = total.first / summary.spent,
                                tag = "home-spend-$i",
                            )
                        }
                    }
                }
                ContinueLearning(learn, navigate)
            }
        }
        val summary = plan
        if (summary != null && !PlanMath.isEmpty(summary)) {
            Dock(
                "Log Expense" to { navigate("/budget?add=true") },
                "50/30/20 Plan" to { navigate("/budget?tab=plan") },
            )
        }
        BottomNavBar(tab, { tabSelected -> navigate(tabRoute(tabSelected)) })
    }
}

/** Tabs route home via the root (AuthWrapper shows Home when signed in). */
fun tabRoute(tab: PennyTab): String = when (tab) {
    PennyTab.HOME -> "/home"
    PennyTab.BUDGET -> "/budget"
    PennyTab.LEARN -> "/learn"
    PennyTab.PROFILE -> "/profilesettings"
}

@Composable
private fun HomeHeader(
    hello: String,
    sub: String,
    avatarUrl: String?,
    initials: String,
    onAi: () -> Unit,
    onProfile: () -> Unit,
) {
    Row(
        Modifier.padding(top = 7.dp, bottom = 15.dp).tag("home-head"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            painterResource(R.drawable.title),
            contentDescription = "Penny",
            tint = PennyPalette.brand,
            modifier = Modifier.size(width = 32.dp, height = 32.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(hello, style = PennyType.secondary, modifier = Modifier.tag("home-greeting"))
            Text(sub, color = PennyPalette.muted, style = PennyType.meta, modifier = Modifier.padding(top = 2.dp))
        }
        IconBtn(Icons.Filled.AutoAwesome, "Open Penny AI", onAi, tag = "home-ai")
        Avatar(
            initials,
            avatarUrl,
            tag = if (avatarUrl != null) "home-avatar-image" else "home-avatar-default",
            onClick = onProfile,
        )
    }
}

@Composable
private fun HomeSkeleton() {
    Skeleton(Modifier.fillMaxWidth().size(240.dp))
}

@Composable
private fun HomeHero(summary: PlanSummary) {
    Box(
        Modifier
            .tag("home-hero")
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(
                    listOf(PennyPalette.heroInk, PennyPalette.heroShade),
                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                    end = androidx.compose.ui.geometry.Offset(600f, 400f),
                ),
            )
            .padding(20.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Eyebrow(
                    "${PlanMath.monthLabel(LocalDate.now().year, ExpenseDashboardText.initialMonth(LocalDate.now().monthValue))} net cash retained",
                    color = PennyPalette.bg.copy(alpha = 0.82f),
                )
                Spacer(Modifier.weight(1f))
                Tag("${Inr.share0(summary.retained, summary.income)} of inflow", kind = TagKind.PLAIN)
            }
            Text(
                Inr.signed(summary.retained),
                color = PennyPalette.bg,
                style = PennyType.hero.merge(MoneyStyle),
                modifier = Modifier.padding(top = 9.dp).tag("home-hero-amount"),
            )
            Text(
                "Income minus spending; not all invested",
                color = PennyPalette.bg.copy(alpha = 0.80f),
                style = PennyType.meta,
                modifier = Modifier.padding(top = 3.dp),
            )
            Row(
                Modifier
                    .padding(top = 16.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                    .background(PennyPalette.bg.copy(alpha = 0.08f))
                    .padding(vertical = 10.dp),
            ) {
                HeroSplit("↑ Income", Inr.signed(summary.income), Icons.Filled.TrendingUp)
                HeroSplit("↓ Spent", Inr.spent(summary.spent), Icons.Filled.TrendingDown)
            }
            Track(
                summary.spent / summary.income,
                Modifier.padding(top = 15.dp),
                color = PennyPalette.blush,
            )
            Text(
                "${Inr.plain(summary.sipRecorded)} SIP recorded · ${Inr.plain(summary.unallocated)} remains unallocated",
                color = PennyPalette.bg.copy(alpha = 0.80f),
                style = TextStyle(fontSize = 11.sp, lineHeight = 16.sp),
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.HeroSplit(label: String, amount: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        Modifier.weight(1f).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, contentDescription = null, tint = PennyPalette.bg.copy(alpha = 0.82f), modifier = Modifier.size(14.dp))
        Column {
            Text(label, color = PennyPalette.bg.copy(alpha = 0.82f), style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.W700))
            Text(
                amount,
                color = PennyPalette.bg,
                style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.W800).merge(MoneyStyle),
                modifier = Modifier.padding(top = 3.dp),
            )
        }
    }
}

@Composable
private fun ContinueLearning(
    learn: com.penny.app.core.LearnState,
    navigate: (String) -> Unit,
) {
    val unit = com.penny.app.core.FakeCourseContent.SAMPLE.keys.first()
    val completed = LearnRules.completedCount(learn, unit)
    val next = completed.coerceAtMost(com.penny.app.core.FakeCourseContent.SAMPLE[unit]!!.size - 1)
    CardBox(Modifier.padding(top = 16.dp), soft = true, tag = "home-learn") {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f)) {
                Eyebrow("Continue learning")
                Text(
                    com.penny.app.core.FakeCourseContent.SAMPLE[unit]!![next],
                    style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.W800),
                    modifier = Modifier.padding(top = 5.dp),
                )
                Text(
                    "Lesson ${next + 1} of ${com.penny.app.core.LearnRules.LESSONS_PER_UNIT} · $completed complete · 🎧",
                    color = PennyPalette.muted,
                    style = PennyType.meta,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Icon(Icons.Filled.Headphones, contentDescription = null, tint = PennyPalette.brand, modifier = Modifier.size(20.dp))
            PennyButton("Start", { navigate("lesson/$unit/$next") }, Modifier, small = true, full = false, tag = "home-learn-start")
        }
    }
}

// ── S03 · First run ───────────────────────────────────────────────────────

@Composable
private fun FirstRun(navigate: (String) -> Unit) {
    CardBox(Modifier.tag("home-empty")) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            Box(
                Modifier.size(72.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(24.dp)).background(PennyPalette.soft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Wallet, contentDescription = null, tint = PennyPalette.brand, modifier = Modifier.size(34.dp))
            }
            Text("No activity yet", style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.W800), modifier = Modifier.padding(top = 16.dp))
            Text(
                "Add income or your first expense. Penny will calculate cashflow, categories, and your 50/30/20 plan from there.",
                color = PennyPalette.muted,
                style = PennyType.secondaryPlain,
                modifier = Modifier.padding(top = 8.dp, bottom = 18.dp),
            )
            PennyButton("Add First Transaction", { navigate("/budget?add=true") }, tag = "home-empty-add")
            TextLink("Add Monthly Income", { navigate("/budget?income=true") }, tag = "home-empty-income")
        }
    }
    SectionHead("Your learning path is ready")
    CardBox(soft = true, tag = "home-empty-learn") {
        Column {
            Eyebrow("Start with the basics")
            Text("The 50/30/20 Rule", style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.W800), modifier = Modifier.padding(top = 5.dp))
            Text(
                "Lesson 1 of ${LearnRules.LESSONS_PER_UNIT} · 4 min · 🎧",
                color = PennyPalette.muted,
                style = PennyType.meta,
                modifier = Modifier.padding(top = 4.dp),
            )
            PennyButton("Start Lesson 1", { navigate("lesson/${com.penny.app.core.FakeCourseContent.SAMPLE.keys.first()}/0") }, Modifier.padding(top = 12.dp), tag = "home-empty-lesson")
        }
    }
    Notice(
        "Empty is not an error. No fake ₹0 charts or \"315% saved\" claims appear before real data exists.",
        Modifier.padding(top = 16.dp),
        tag = "home-empty-notice",
    )
}

private fun dotOf(category: String): Color = Color(BudgetCategories.COLORS[category] ?: 0xFFA35555)
