package com.penny.app.core

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** ChatbotPage logic (chatbot_page.dart). Fold/accumulation/ids executed on Dart 3.14 (dart015/fold.dart). */
class ChatLogicTest {

    @Test
    fun `parts fold gives every part a leading space, null parts are empty`() {
        assertEquals(" Hello", ChatLogic.partsText(listOf("Hello")))
        assertEquals(" there, friend.", ChatLogic.partsText(listOf("there,", "friend.")))
        assertEquals("  Spaced", ChatLogic.partsText(listOf(" Spaced")))
        assertEquals("", ChatLogic.partsText(emptyList()))
        assertEquals("", ChatLogic.partsText(null))
    }

    @Test
    fun `stream events accumulate into the newest Gemini message exactly like Dart`() {
        var msgs = ChatLogic.send(emptyList(), "hi")
        listOf(listOf("Hello"), listOf("there,", "friend."), listOf(" Spaced"), emptyList<String>()).forEach {
            msgs = ChatLogic.onEvent(msgs, it)
        }
        assertEquals(
            listOf(
                ChatLogic.Message(ChatLogic.Author.GEMINI, " Hello there, friend.  Spaced"),
                ChatLogic.Message(ChatLogic.Author.USER, "hi"),
            ),
            msgs,
        )
        // A new user message in between makes the next event start a NEW Gemini message.
        msgs = ChatLogic.onEvent(ChatLogic.send(msgs, "again"), listOf("Second"))
        assertEquals(" Second", msgs.first().text)
        assertEquals(4, msgs.size)
    }

    @Test
    fun `new chat ids count the existing chats`() {
        val ids = mutableListOf(ChatLogic.FIRST_CHAT)
        repeat(3) { ids += ChatLogic.newChatId(ids) }
        assertEquals(listOf("New Chat", "New Chat 1", "New Chat 2", "New Chat 3"), ids)
    }

    @Test
    fun `fake model streams sentence chunks of a keyword reply`() = runBlocking {
        val events = FakeChatModel(chunkDelayMs = 0).streamGenerateContent("How do I BUDGET?").toList()
        assertEquals(4, events.size) // four sentences in the budget reply
        assertTrue(events.all { it.size == 1 })
        var msgs = emptyList<ChatLogic.Message>()
        events.forEach { msgs = ChatLogic.onEvent(msgs, it) }
        // The split drops each single separating space and the fold re-adds one before every chunk.
        assertEquals(" " + FakeChatModel.reply("budget"), msgs.single().text)
        assertTrue(FakeChatModel.reply("hello").startsWith("I'm Penny's practice assistant"))
    }
}
