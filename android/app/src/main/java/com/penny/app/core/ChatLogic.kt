package com.penny.app.core

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * The chat model seam. Flutter calls Gemini directly from the app with a
 * BUNDLED API key (`flutter_gemini`, chatbot_page.dart:42) — a security
 * problem the maintainer must decide on (LEDGER). Until then the app ships a
 * fake; a real implementation (backend proxy or on-device key) only has to
 * emit the same stream shape: one event per chunk, each a list of text parts
 * (`event.content.parts`).
 */
interface ChatModel {
    fun streamGenerateContent(question: String): Flow<List<String>>
}

/**
 * Canned finance-assistant replies, streamed in sentence chunks with a small
 * delay so the incremental UI path (append to the last bot message) runs.
 * NEW behaviour by design — it is not Gemini.
 */
class FakeChatModel(private val chunkDelayMs: Long = 120) : ChatModel {
    override fun streamGenerateContent(question: String): Flow<List<String>> = flow {
        reply(question).split(Regex("(?<=[.!?])\\s+")).forEach { sentence ->
            delay(chunkDelayMs)
            emit(listOf(sentence))
        }
    }

    companion object {
        fun reply(question: String): String {
            val q = question.lowercase()
            return when {
                "budget" in q -> "A simple budget is the **50/30/20 rule**. Put 50% of income toward needs. " +
                    "Use 30% for wants. Save or invest the remaining 20%."
                "save" in q || "saving" in q -> "Start with an emergency fund of three to six months of expenses. " +
                    "Automate a fixed transfer on payday. Increase it whenever your income grows."
                "invest" in q -> "Investing grows money over time through returns. " +
                    "Diversify across *fixed deposits*, *mutual funds*, stocks and gold. Match risk to your time horizon."
                "debt" in q || "loan" in q -> "List your debts by interest rate. " +
                    "Pay the minimum on all of them and put extra money toward the highest rate first."
                else -> "I'm Penny's practice assistant. Ask me about budgeting, saving, investing or debt."
            }
        }
    }
}

/** Pure logic of ChatbotPage (chatbot_page.dart). */
object ChatLogic {
    const val FIRST_CHAT = "New Chat"
    const val TITLE = "Gemini Chat"
    const val HINT = "Type a message..."

    enum class Author { USER, GEMINI }

    data class Message(val author: Author, val text: String)

    /** `event.content?.parts?.fold("", (p, c) => "$p ${c.text}") ?? ""` — every part gets a leading space. */
    fun partsText(parts: List<String>?): String = parts?.fold("") { previous, current -> "$previous $current" } ?: ""

    /** _startNewChat (:78-83): "New Chat ${chatHistory.keys.length}" (count BEFORE inserting). */
    fun newChatId(existing: Collection<String>): String = "New Chat ${existing.size}"

    /** Sending (:32-38): the user's message is prepended (the list is newest-first). */
    fun send(messages: List<Message>, text: String): List<Message> = listOf(Message(Author.USER, text)) + messages

    /**
     * One stream event (:42-71): if the newest message is Gemini's, append to
     * it (`text +=`), otherwise prepend a new Gemini message.
     */
    fun onEvent(messages: List<Message>, parts: List<String>?): List<Message> {
        val response = partsText(parts)
        val last = messages.firstOrNull()
        return if (last != null && last.author == Author.GEMINI) {
            listOf(last.copy(text = last.text + response)) + messages.drop(1)
        } else {
            listOf(Message(Author.GEMINI, response)) + messages
        }
    }
}
