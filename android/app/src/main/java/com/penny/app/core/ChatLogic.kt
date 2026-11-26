package com.penny.app.core

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * The Penny AI seam (artboard frame 13 + S06). The Flutter page called
 * Gemini from the app with a bundled key — still a maintainer security
 * decision — so screens depend on this interface and the app ships a fake.
 *
 * v2.1 changes the contract deliberately: replies are grounded in an
 * [AiContext] snapshot of the user's month (source note, real numbers),
 * and the model may propose a STRUCTURED allocation ([ChatEvent.Proposal])
 * which the screen renders as an action card with Cancel/Confirm — no
 * mutation happens without that confirmation.
 */
interface ChatModel {
    fun reply(question: String, context: AiContext): Flow<ChatEvent>
}

/** A bot turn: streamed text, or a confirm-before-acting proposal. */
sealed interface ChatEvent {
    data class Text(val parts: List<String>) : ChatEvent
    data class Proposal(val amount: Double, val fromLabel: String, val toLabel: String) : ChatEvent
}

/** The month snapshot the fake grounds its answers in. */
data class AiContext(
    val monthLabel: String,
    val txCount: Int,
    val plan: PlanSummary,
    val foodSpend: Double,
    val cardDiningCount: Int,
    val cardDiningAvg: Double,
)

/**
 * Canned Penny AI replies computed from the live [AiContext] — the numbers
 * on screen are always the budget's own, never hard-coded. Streamed in
 * sentence chunks with a small delay so the incremental path runs and the
 * S06 pending state is observable.
 */
class FakeChatModel(private val chunkDelayMs: Long = 120) : ChatModel {
    override fun reply(question: String, context: AiContext): Flow<ChatEvent> = flow {
        val q = question.lowercase()
        val events: List<ChatEvent> = when {
            "budget" in q || "50/30/20" in q -> listOf(ChatEvent.Text(listOf(AiReplies.budgetCheck(context))))
            ("move" in q || "put" in q) && "emergency" in q ->
                listOf(ChatEvent.Text(listOf(AiReplies.assumption(context))), ChatEvent.Proposal(
                    amount = AiReplies.diningSwapSavings(context),
                    fromLabel = "Unallocated cash · ${Inr.plain(context.plan.unallocated)}",
                    toLabel = "Emergency Fund goal",
                ))
            "cut" in q && "food" in q -> listOf(ChatEvent.Text(listOf(AiReplies.cutFood(context))))
            "sip" in q || "invest" in q -> listOf(ChatEvent.Text(listOf(AiReplies.sip(context))))
            else -> listOf(ChatEvent.Text(listOf(AiReplies.intro())))
        }
        for (event in events) {
            if (event is ChatEvent.Text) {
                event.parts.first().split(Regex("(?<=[.!?])\\s+")).forEach { sentence ->
                    delay(chunkDelayMs)
                    emit(ChatEvent.Text(listOf(sentence)))
                }
            } else {
                delay(chunkDelayMs)
                emit(event)
            }
        }
    }
}

/** Pure reply text; every number comes from the context. */
object AiReplies {
    /** Frame 13's first answer. */
    fun budgetCheck(context: AiContext): String {
        val plan = context.plan
        return "You retained ${Inr.plain(plan.retained)} after spending. " +
            "${Inr.plain(plan.sipRecorded)} is already recorded as SIP, leaving " +
            "${Inr.plain(plan.unallocated)} unallocated.\n" +
            "• Needs: ${Inr.plain(plan.needs.actual)} of ${Inr.plain(plan.needs.target)} · " +
            "${Inr.plain(plan.needs.remainder)} remainder\n" +
            "• Wants: ${Inr.plain(plan.wants.actual)} of ${Inr.plain(plan.wants.target)} · " +
            "${Inr.plain(plan.wants.remainder)} remainder\n" +
            "• SIP goal: ${Inr.plain(plan.sipRecorded)} of ${Inr.plain(plan.sipGoal)} · " +
            Inr.share0(plan.sipRecorded, plan.sipGoal)
    }

    /** The editable assumption (frame 13 chat-tip): 2 home meals at ₹900. */
    const val HOME_MEAL_COST = 900.0

    fun diningSwapSavings(context: AiContext): Double {
        if (context.cardDiningCount == 0) return 0.0
        return context.cardDiningCount * (context.cardDiningAvg - HOME_MEAL_COST).coerceAtLeast(0.0)
    }

    fun assumption(context: AiContext): String {
        val savings = diningSwapSavings(context)
        return "Assumption you can edit: your ${context.cardDiningCount} card dining transactions " +
            "average ${Inr.plain(context.cardDiningAvg)}. If home meals cost ${Inr.plain(HOME_MEAL_COST)} " +
            "each instead, estimated savings are ${Inr.plain(savings)}. Actual savings will vary."
    }

    fun cutFood(context: AiContext): String =
        "Your Food & Dining spend is ${Inr.plain(context.foodSpend)} this month. " +
            "The card dining transactions are the easiest to trim first — cooking the same meals " +
            "at home is where the assumption in my earlier estimate came from."

    fun sip(context: AiContext): String {
        val monthly = context.plan.sipRecorded
        val value = SipIllustration.futureValue(monthly, 120, 0.12)
        return "Your recorded SIP is ${Inr.plain(monthly)} a month. Left alone for 10 illustrative " +
            "years at an assumed 12% it would compound to about ${Inr.lakh(value)} before tax and fees. " +
            "Returns are not guaranteed; this is not personalized investment advice."
    }

    fun intro(): String =
        "I'm Penny AI. I read your saved budget and answer with your own numbers. " +
            "Ask about your spending, your plan, or your SIP."

    /** The standing caution under every action card (frame 13). */
    const val CAUTION = "Penny AI can make mistakes. Check figures before acting. " +
        "Financial guidance is informational, not personalized investment advice."

    /** The S06 pending bubble. */
    const val THINKING = "Penny is thinking…"
    const val THINKING_NOTE = "Reading your saved snapshot. You can keep typing or leave this thread."
}

/** Thread logic of the Penny AI screen (chatbot_page.dart's fold, v2.1). */
object ChatLogic {
    const val TITLE = "Penny AI"
    const val HINT = "Ask about spending or SIPs…"
    const val FIRST_THREAD = "Thread 1"

    enum class Author { USER, PENNY }

    data class Proposal(val amount: Double, val fromLabel: String, val toLabel: String)

    data class Message(val author: Author, val text: String, val proposal: Proposal? = null)

    /** `event.content?.parts?.fold("", (p, c) => "$p ${c.text}") ?? ""` — every part gets a leading space. */
    fun partsText(parts: List<String>?): String = parts?.fold("") { previous, current -> "$previous $current" } ?: ""

    /** Sending: the user's message is prepended (the list is newest-first). */
    fun send(messages: List<Message>, text: String): List<Message> = listOf(Message(Author.USER, text)) + messages

    /** One streamed text event: append to the newest Penny message, else prepend one. */
    fun onTextEvent(messages: List<Message>, parts: List<String>?): List<Message> {
        val response = partsText(parts)
        val last = messages.firstOrNull()
        return if (last != null && last.author == Author.PENNY && last.proposal == null) {
            listOf(last.copy(text = last.text + response)) + messages.drop(1)
        } else {
            listOf(Message(Author.PENNY, response)) + messages
        }
    }

    /** A proposal always starts its own bubble (it renders as an action card). */
    fun onProposal(messages: List<Message>, proposal: Proposal): List<Message> =
        listOf(Message(Author.PENNY, "", proposal)) + messages

    /** _startNewChat: the count BEFORE inserting, like the Flutter page. */
    fun newThreadId(existing: Collection<String>): String = "Thread ${existing.size + 1}"

    /** The navbar pill (frame 13): "Threads · 2". */
    fun threadCountLabel(count: Int): String = "Threads · $count"
}
