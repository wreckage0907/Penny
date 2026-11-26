@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.penny.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.penny.app.PennyServices
import com.penny.app.core.AiContext
import com.penny.app.core.AiReplies
import com.penny.app.core.Allocation
import com.penny.app.core.AllocationKind
import com.penny.app.core.Allocations
import com.penny.app.core.BudgetCategories
import com.penny.app.core.ChatEvent
import com.penny.app.core.ChatLogic
import com.penny.app.core.DashboardFlows
import com.penny.app.core.ExpenseAggregation
import com.penny.app.core.ExpenseDashboardText
import com.penny.app.core.Inr
import com.penny.app.core.PlanMath
import com.penny.app.core.PlanMonth
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.time.LocalDate
import java.time.LocalTime

/**
 * v2.1 Penny AI (artboard frame 13 + state frame S06): replies grounded in
 * the month's own numbers with a source note, prompt chips for the three
 * common asks, structured allocation proposals that need an explicit
 * Confirm, pending skeletons, and a non-destructive error with retry
 * (the message is preserved in-thread). Replaces the v1 Gemini drawer page.
 */
@Composable
fun PennyAiScreen(services: PennyServices, navigate: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var threads by remember { mutableStateOf(listOf(ChatLogic.FIRST_THREAD)) }
    var current by remember { mutableStateOf(ChatLogic.FIRST_THREAD) }
    val history = remember { mutableStateMapOf<String, List<ChatLogic.Message>>() }
    var input by remember { mutableStateOf("") }
    var pending by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var threadsOpen by remember { mutableStateOf(false) }
    var sourceNote by remember { mutableStateOf("") }
    var context by remember { mutableStateOf<AiContext?>(null) }
    var user by remember { mutableStateOf<String?>(null) }

    fun newThread() {
        val id = ChatLogic.newThreadId(threads)
        threads = threads + id
        current = id
        threadsOpen = false
    }

    // Build the grounding snapshot once per entry (fake data is static).
    LaunchedEffect(Unit) {
        val u = DashboardFlows.username(services.auth.customClaims(), services.prefs.username()) ?: ""
        user = u
        val today = LocalDate.now()
        val month = ExpenseDashboardText.initialMonth(today.monthValue)
        val api = services.expenses(u)
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
        val categories = PlanMath.monthCategories(expenses, today.year, month)
        val allocations = services.allocations.forMonth(u, today.year, month)
        val plan = PlanMath.summary(
            PlanMonth(PlanMath.income(income, today.year, month), categories, Allocations.sipTotal(allocations), Allocations.goalTotal(allocations)),
        )
        // Card dining average grounds the editable assumption (frame 13).
        var cardCount = 0
        var cardSum = 0.0
        val foodList = ExpenseAggregation.categoriesFor(expenses, today.year, month)[BudgetCategories.FOOD] as? JsonArray
        foodList?.forEach { el ->
            val obj = el as? JsonObject ?: return@forEach
            val name = (obj["name"] as? JsonPrimitive)?.content ?: return@forEach
            val amount = (obj["amount"] as? JsonPrimitive)?.content?.toDoubleOrNull() ?: return@forEach
            if (services.meta.detail(u, today.year, month, BudgetCategories.FOOD, name).payment == "Card") {
                cardCount += 1
                cardSum += amount
            }
        }
        context = AiContext(
            monthLabel = PlanMath.monthLabel(today.year, month),
            txCount = categories.values.sumOf { it.second },
            plan = plan,
            foodSpend = categories[BudgetCategories.FOOD]?.first ?: 0.0,
            cardDiningCount = cardCount,
            cardDiningAvg = if (cardCount > 0) cardSum / cardCount else 0.0,
        )
        sourceNote = "Based on ${PlanMath.monthLabel(today.year, month)} · ${categories.values.sumOf { it.second }} transactions · updated ${LocalTime.now().toString().take(5)}"
    }

    fun send(text: String) {
        val question = text.trim()
        if (question.isEmpty() || pending) return
        val ctx = context ?: return
        input = ""
        failed = false
        history[current] = ChatLogic.send(history[current] ?: emptyList(), question)
        pending = true
        scope.launch {
            try {
                services.chat.reply(question, ctx).collect { event ->
                    val base = history[current] ?: emptyList()
                    history[current] = when (event) {
                        is ChatEvent.Text -> ChatLogic.onTextEvent(base, event.parts)
                        is ChatEvent.Proposal -> ChatLogic.onProposal(
                            base,
                            ChatLogic.Proposal(event.amount, event.fromLabel, event.toLabel),
                        )
                    }
                }
                pending = false
            } catch (e: Exception) {
                pending = false
                failed = true
            }
        }
    }

    fun confirmProposal(proposal: ChatLogic.Proposal) {
        val u = user ?: return
        val today = LocalDate.now()
        val month = ExpenseDashboardText.initialMonth(today.monthValue)
        scope.launch {
            services.allocations.record(
                u,
                Allocation(AllocationKind.GOAL, "Emergency Fund", proposal.amount, today.year, month),
            )
            val base = history[current] ?: emptyList()
            val confirmed = base.toMutableList()
            val i = confirmed.indexOfFirst { it.proposal == proposal }
            if (i >= 0) confirmed[i] = confirmed[i].copy(
                text = "Allocation recorded: ${Inr.plain(proposal.amount)} → ${proposal.toLabel}.",
                proposal = null,
            )
            history[current] = confirmed
        }
    }

    Column(Modifier.fillMaxSize().tag("ai-page")) {
        Column(Modifier.padding(horizontal = 18.dp).padding(top = 5.dp)) {
            NavBar(
                ChatLogic.TITLE,
                onBack = { navigate(tabRoute(PennyTab.HOME)) },
                leading = {
                    Text("◉", color = PennyPalette.brand, style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.W900))
                },
                trailing = {
                    PillBtn(
                        ChatLogic.threadCountLabel(threads.size),
                        { threadsOpen = true },
                        tag = "ai-threads",
                    )
                },
                tag = "ai-nav",
            )
            Row(
                Modifier
                    .padding(bottom = 12.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf("50/30/20 check", "Cut Food spend", "SIP for 10 years").forEachIndexed { i, chip ->
                    PillBtn(chip, { send(chip) }, tag = "ai-chip-$i")
                }
            }
        }
        val messages = history[current] ?: emptyList()
        // Chat anchoring: reverseLayout keeps the NEWEST message at the
        // bottom of the viewport (visible, clickable), older ones above.
        LazyColumn(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            reverseLayout = true,
        ) {
            item { Hint(AiReplies.CAUTION, Modifier.padding(vertical = 8.dp)) }
            item {
                if (pending) {
                    PendingBubble(sourceNote)
                } else if (failed) {
                    ErrorNotice(
                        onRetry = {
                            failed = false
                            send(messages.firstOrNull()?.text ?: "")
                        },
                    )
                }
            }
            items(messages.mapIndexed { i, m -> i to m }, key = { (i, _) -> "m$i" }) { (index, message) ->
                when {
                    message.proposal != null -> ActionCard(
                        message.proposal!!,
                        onCancel = {
                            val base = history[current] ?: emptyList()
                            history[current] = base.map {
                                if (it.proposal == message.proposal) {
                                    it.copy(text = "Allocation cancelled. No budget change was made.", proposal = null)
                                } else it
                            }
                        },
                        onConfirm = { confirmProposal(message.proposal!!) },
                    )
                    message.author == ChatLogic.Author.USER -> Bubble(message.text, mine = true, tag = "ai-user-$index")
                    else -> AiBubble(message.text, sourceNote, tag = "ai-text-$index")
                }
            }
        }
        Row(
            Modifier
                .padding(horizontal = 18.dp, vertical = 10.dp)
                .border(1.dp, PennyPalette.ink.copy(alpha = 0.09f)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 52.dp)
                    .clip(PennyShape.field)
                    .background(PennyPalette.sand)
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                androidx.compose.material3.OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    singleLine = true,
                    placeholder = { Text(ChatLogic.HINT, color = PennyPalette.muted, style = PennyType.secondaryPlain) },
                    textStyle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.W600),
                    modifier = Modifier.fillMaxWidth().testTag("ai-input"),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                        focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                        cursorColor = PennyPalette.brand,
                    ),
                )
            }
            IconBtn(
                Icons.AutoMirrored.Filled.Send, "Send message",
                { send(input) },
                tint = PennyPalette.surface, bg = PennyPalette.brand,
                tag = "ai-send",
            )
        }
    }

    if (threadsOpen) {
        PennySheet(onDismiss = { threadsOpen = false }, tag = "ai-threads-sheet") {
            Column(Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                Text("Threads", style = PennyType.sheetTitle, modifier = Modifier.padding(bottom = 10.dp).testTag("ai-threads-title"))
                PennyButton("New Thread", { newThread() }, Modifier.padding(bottom = 10.dp), tag = "ai-new-thread")
                threads.reversed().forEach { id ->
                    ListRow(
                        key = id,
                        onClick = { current = id; threadsOpen = false },
                        value = "${history[id]?.size ?: 0} messages",
                        tag = "ai-thread-${id.replace(' ', '-')}",
                    )
                }
            }
        }
    }
}

private fun Modifier.thinBubbleBorder(): Modifier =
    this.border(1.dp, PennyPalette.ink.copy(alpha = 0.09f), RoundedCornerShape(17.dp))

@Composable
private fun Bubble(text: String, mine: Boolean, tag: String) {
    val shape = RoundedCornerShape(
        topStart = 17.dp, topEnd = 17.dp,
        bottomStart = if (mine) 17.dp else 5.dp,
        bottomEnd = if (mine) 5.dp else 17.dp,
    )
    Box(
        Modifier
            .padding(vertical = 6.dp)
            .fillMaxWidth()
            .padding(start = if (mine) 60.dp else 0.dp, end = if (mine) 0.dp else 60.dp),
        contentAlignment = if (mine) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Text(
            text,
            color = if (mine) PennyPalette.surface else PennyPalette.ink,
            style = TextStyle(fontSize = 14.sp, lineHeight = 22.sp),
            modifier = Modifier
                .tag(tag)
                .clip(shape)
                .background(if (mine) PennyPalette.brand else PennyPalette.surface)
                .thinBubbleBorder()
                .padding(horizontal = 14.dp, vertical = 13.dp),
        )
    }
}

@Composable
private fun AiBubble(text: String, note: String, tag: String) {
    Column(
        Modifier
            .padding(vertical = 6.dp)
            .fillMaxWidth()
            .padding(end = 60.dp),
    ) {
        Text(
            "PENNY AI",
            color = PennyPalette.muted,
            style = PennyType.eyebrow,
        )
        Bubble(text, mine = false, tag = tag)
        if (note.isNotEmpty()) {
            Text(note, color = PennyPalette.muted, style = TextStyle(fontSize = 11.sp), modifier = Modifier.padding(start = 2.dp, top = 2.dp))
        }
    }
}

@Composable
private fun PendingBubble(note: String) {
    Column(Modifier.padding(vertical = 6.dp).fillMaxWidth().padding(end = 60.dp)) {
        Text("PENNY AI", color = PennyPalette.muted, style = PennyType.eyebrow)
        Text(
            AiReplies.THINKING,
            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.W800),
            modifier = Modifier
                .tag("ai-pending")
                .clip(RoundedCornerShape(17.dp))
                .background(PennyPalette.surface)
                .thinBubbleBorder()
                .padding(horizontal = 14.dp, vertical = 13.dp),
        )
        Text(
            AiReplies.THINKING_NOTE,
            color = PennyPalette.muted,
            style = TextStyle(fontSize = 11.sp),
            modifier = Modifier.padding(start = 2.dp, top = 2.dp),
        )
        Skeleton(Modifier.padding(top = 10.dp, start = 2.dp).widthIn(max = 220.dp).height(12.dp))
        Skeleton(Modifier.padding(top = 8.dp, start = 2.dp).widthIn(max = 165.dp).height(12.dp))
    }
}

@Composable
private fun ErrorNotice(onRetry: () -> Unit) {
    Column(Modifier.padding(vertical = 6.dp).tag("ai-error")) {
        Notice(
            "Penny couldn't answer right now. Your message is saved in this thread. No allocation or financial change was made.",
            Modifier,
            NoticeKind.NEG,
            boldParts = listOf("Penny couldn't answer right now."),
        )
        TextLink("Retry", onRetry, Modifier.padding(top = 2.dp), tag = "ai-retry")
        Hint("If you're offline, reconnect before retrying. Your earlier answers remain available.", Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun ActionCard(
    proposal: ChatLogic.Proposal,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    CardBox(Modifier.padding(vertical = 12.dp), soft = true, tag = "ai-action") {
        Column {
            Text("Confirm budget allocation", style = PennyType.secondary)
            ActionRow("Amount", Inr.plain(proposal.amount))
            ActionRow("From", proposal.fromLabel)
            ActionRow("To", proposal.toLabel)
            Hint(
                "This records a Penny budget allocation. No money moves at your bank.",
                Modifier.padding(top = 8.dp),
                boldParts = listOf("No money moves at your bank."),
            )
            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PennyButton("Cancel", onCancel, Modifier.weight(1f), kind = BtnKind.SECONDARY, tag = "ai-action-cancel")
                PennyButton("Confirm ${Inr.plain(proposal.amount)}", onConfirm, Modifier.weight(1f), tag = "ai-action-confirm")
            }
        }
    }
}

@Composable
private fun ActionRow(label: String, value: String) {
    Row(Modifier.padding(vertical = 6.dp)) {
        Text(label, color = PennyPalette.muted, style = TextStyle(fontSize = 13.sp))
        Spacer(Modifier.weight(1f))
        Text(value, style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.W800).merge(MoneyStyle))
    }
}
