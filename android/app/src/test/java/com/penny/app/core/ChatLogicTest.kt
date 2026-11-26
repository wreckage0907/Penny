package com.penny.app.core

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Penny AI thread logic (v2.1): the Dart fold semantics kept from
 * chatbot_page.dart, the proposal bubble, thread ids, and the grounded
 * canned replies (frame 13's numbers come from the AiContext, never
 * hard-coded).
 */
class ChatLogicTest {

    private fun demoContext() = AiContext(
        monthLabel = "Nov 2026",
        txCount = 18,
        plan = PlanMath.summary(
            PlanMonth(
                income = 85_000.0,
                categories = mapOf(
                    BudgetCategories.FOOD to (11_200.0 to 6),
                    BudgetCategories.RENT to (9_500.0 to 3),
                    BudgetCategories.SHOPPING to (5_450.0 to 2),
                    BudgetCategories.TRANSPORT to (3_100.0 to 4),
                    BudgetCategories.ENTERTAINMENT to (2_200.0 to 3),
                ),
                sipRecorded = 17_000.0,
                goalAllocated = 0.0,
            ),
        ),
        foodSpend = 11_200.0,
        cardDiningCount = 2,
        cardDiningAvg = 1_975.0,
    )

    @Test
    fun `parts fold gives every part a leading space, null parts are empty`() {
        assertEquals(" Hello", ChatLogic.partsText(listOf("Hello")))
        assertEquals(" there, friend.", ChatLogic.partsText(listOf("there,", "friend.")))
        assertEquals("  Spaced", ChatLogic.partsText(listOf(" Spaced")))
        assertEquals("", ChatLogic.partsText(emptyList()))
        assertEquals("", ChatLogic.partsText(null))
    }

    @Test
    fun `stream events accumulate into the newest Penny message exactly like the Dart fold`() {
        var msgs = ChatLogic.send(emptyList(), "hi")
        listOf(listOf("Hello"), listOf("there,", "friend."), listOf(" Spaced"), emptyList<String>()).forEach {
            msgs = ChatLogic.onTextEvent(msgs, it)
        }
        assertEquals(
            listOf(
                ChatLogic.Message(ChatLogic.Author.PENNY, " Hello there, friend.  Spaced"),
                ChatLogic.Message(ChatLogic.Author.USER, "hi"),
            ),
            msgs,
        )
        // A new user message in between makes the next event start a NEW Penny message.
        msgs = ChatLogic.onTextEvent(ChatLogic.send(msgs, "again"), listOf("Second"))
        assertEquals(" Second", msgs.first().text)
        assertEquals(4, msgs.size)
    }

    @Test
    fun `a proposal always starts its own bubble and never merges into text`() {
        var msgs = ChatLogic.send(emptyList(), "move it")
        msgs = ChatLogic.onTextEvent(msgs, listOf("Assumption…"))
        msgs = ChatLogic.onProposal(msgs, ChatLogic.Proposal(2_150.0, "Unallocated", "Emergency Fund goal"))
        val head = msgs.first()
        assertEquals(ChatLogic.Author.PENNY, head.author)
        assertEquals("", head.text)
        assertEquals(2_150.0, head.proposal!!.amount, 0.0)
        // A following text event starts a NEW bubble; the proposal stays intact below it.
        msgs = ChatLogic.onTextEvent(msgs, listOf("More text"))
        assertEquals(" More text", msgs[0].text)
        assertNull(msgs[0].proposal)
        assertEquals("", msgs[1].text)
        assertEquals(2_150.0, msgs[1].proposal!!.amount, 0.0)
        assertEquals(4, msgs.size)
    }

    @Test
    fun `thread ids count existing threads and the navbar pill shows the count`() {
        val ids = mutableListOf(ChatLogic.FIRST_THREAD)
        repeat(3) { ids += ChatLogic.newThreadId(ids) }
        assertEquals(listOf("Thread 1", "Thread 2", "Thread 3", "Thread 4"), ids)
        assertEquals("Threads · 4", ChatLogic.threadCountLabel(4))
        assertEquals("Threads · 2", ChatLogic.threadCountLabel(2))
    }

    @Test
    fun `budget check reply quotes the month's own numbers`() {
        val reply = AiReplies.budgetCheck(demoContext())
        assertEquals(
            "You retained ₹53,550 after spending. ₹17,000 is already recorded as SIP, " +
                "leaving ₹36,550 unallocated.\n" +
                "• Needs: ₹23,800 of ₹42,500 · ₹18,700 remainder\n" +
                "• Wants: ₹7,650 of ₹25,500 · ₹17,850 remainder\n" +
                "• SIP goal: ₹17,000 of ₹17,000 · 100%",
            reply,
        )
    }

    @Test
    fun `the editable assumption computes the card-dining swap`() {
        assertEquals(2_150.0, AiReplies.diningSwapSavings(demoContext()), 0.0) // 2 × (1,975 − 900)
        val none = demoContext().copy(cardDiningCount = 0)
        assertEquals(0.0, AiReplies.diningSwapSavings(none), 0.0)
        val cheap = demoContext().copy(cardDiningAvg = 500.0)
        assertEquals(0.0, AiReplies.diningSwapSavings(cheap), 0.0) // never negative
        assertTrue(
            AiReplies.assumption(demoContext()).contains("2 card dining transactions average ₹1,975"),
        )
    }

    @Test
    fun `fake model streams sentence chunks of a keyword reply`() = runBlocking {
        val events = FakeChatModel(chunkDelayMs = 0).reply("How is my BUDGET looking?", demoContext()).toList()
        assertTrue(events.isNotEmpty())
        assertTrue(events.filterIsInstance<ChatEvent.Text>().all { it.parts.size == 1 })
        var msgs = ChatLogic.send(emptyList(), "How is my BUDGET looking?")
        events.forEach { event ->
            msgs = when (event) {
                is ChatEvent.Text -> ChatLogic.onTextEvent(msgs, event.parts)
                is ChatEvent.Proposal -> ChatLogic.onProposal(msgs, ChatLogic.Proposal(event.amount, event.fromLabel, event.toLabel))
            }
        }
        // The sentence split consumes the newline after "unallocated." (the
        // bullets carry no sentence-ending punctuation), and the fold adds
        // one leading space per chunk.
        assertEquals(" " + AiReplies.budgetCheck(demoContext()).replaceFirst("\n", " "), msgs.first().text)
        assertTrue(AiReplies.intro().startsWith("I'm Penny AI"))
    }

    @Test
    fun `moving savings into the emergency fund proposes an allocation`() = runBlocking {
        val events = FakeChatModel(chunkDelayMs = 0)
            .reply("Move that into my emergency fund?", demoContext())
            .toList()
        val proposal = events.filterIsInstance<ChatEvent.Proposal>().single()
        assertEquals(2_150.0, proposal.amount, 0.0)
        assertEquals("Unallocated cash · ₹36,550", proposal.fromLabel)
        assertEquals("Emergency Fund goal", proposal.toLabel)
    }
}
