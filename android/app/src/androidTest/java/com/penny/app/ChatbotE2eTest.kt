package com.penny.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.penny.app.ui.PennyApp
import org.junit.Rule
import org.junit.Test

/** 015: Gemini Chat page (chatbot_page.dart) on the fake chat model. */
class ChatbotE2eTest {

    @get:Rule
    val compose = createComposeRule()

    private fun open() {
        compose.setContent { PennyApp(PennyServices.fakes(signedIn = true)) }
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("home-page") }
        compose.onNodeWithTag("home-nav-chat").performClick()
        compose.onNodeWithTag("chat-title", useUnmergedTree = true).assertTextEquals("Gemini Chat")
    }

    private fun send(text: String) {
        compose.onNodeWithTag("chat-input").performTextInput(text)
        compose.onNodeWithTag("chat-send").performClick()
    }

    @Test
    fun aQuestionGetsAStreamedMarkdownReply() {
        open()
        send("How do I budget?")
        compose.onNodeWithTag("chat-text-0", useUnmergedTree = true).assertTextEquals("How do I budget?")
        // The reply streams in three sentence chunks; wait for the last one.
        compose.waitUntil(5_000) { compose.onAllNodesWithTextExists("Save or invest the remaining 20%.", substring = true) }
        compose.onNodeWithTag("chat-bot-0", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun newChatsAreNumberedAndKeepTheirOwnHistory() {
        open()
        send("Tell me about debt")
        compose.waitUntil(5_000) { compose.onAllNodesWithTextExists("highest rate first.", substring = true) }
        compose.onNodeWithTag("chat-new").performClick()
        check(!compose.onAllNodesWithTagExists("chat-user-1"))
        compose.onNodeWithTag("chat-menu").performClick()
        compose.onNodeWithTag("drawer-chat-New Chat 1").assertIsSelected()
        compose.onNodeWithTag("drawer-chat-New Chat").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("chat-user-1") }
    }

    @Test
    fun emptyInputIsNotSentAndBottomBarNavigates() {
        open()
        compose.onNodeWithTag("chat-send").performClick()
        check(!compose.onAllNodesWithTagExists("chat-user-0"))
        compose.onNodeWithTag("chat-nav-profile").performClick()
        compose.onNodeWithTag("profile-page").assertIsDisplayed()
    }
}
