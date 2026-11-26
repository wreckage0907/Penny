package com.penny.app

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.penny.app.core.AiContext
import com.penny.app.core.ChatEvent
import com.penny.app.core.ChatModel
import com.penny.app.ui.PennyApp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * v2.1 Penny AI (frame 13 + S06): grounded replies with the month's own
 * numbers, the confirm-before-acting allocation card, threads, and the
 * non-destructive error state.
 */
class AiE2eTest {

    @get:Rule
    val compose = createComposeRule()

    private fun boot(chat: ChatModel = com.penny.app.core.FakeChatModel()): PennyServices {
        val defaults = PennyServices.fakes(signedIn = true)
        val services = PennyServices(
            auth = defaults.auth, users = defaults.users, prefs = defaults.prefs,
            profileImages = defaults.profileImages, expenses = defaults.expenses,
            courses = defaults.courses, mcq = defaults.mcq, stocks = defaults.stocks,
            meta = defaults.meta, allocations = defaults.allocations,
            learn = defaults.learn, connectivity = defaults.connectivity,
            chat = chat,
        )
        compose.setContent { PennyApp(services) }
        compose.onNodeWithTag("home-ai").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("ai-page") }
        return services
    }

    @Test
    fun theBudgetCheckChipAnswersWithTheMonthsOwnNumbers() {
        boot()
        compose.onNodeWithTag("ai-chip-0").performClick() // "50/30/20 check"
        compose.waitUntil(5_000) { compose.onAllNodesWithTextExists("• SIP goal: ₹17,000 of ₹17,000 · 100%", substring = true) } // the reply streams in sentence chunks
        assertTrue(compose.onAllNodesWithTextExists("You retained ₹53,550 after spending.", substring = true))
        assertTrue(compose.onAllNodesWithTextExists("₹36,550 unallocated", substring = true))
        assertTrue(compose.onAllNodesWithTextExists("• Needs: ₹23,800 of ₹42,500 · ₹18,700 remainder", substring = true))
        assertTrue(compose.onAllNodesWithTextExists("Based on ", substring = true)) // the note always cites the month
    }

    @Test
    fun movingSavingsIntoTheEmergencyFundRequiresAnExplicitConfirmation() {
        val services = boot()
        compose.onNodeWithTag("ai-input").performTextInput("Move that into my emergency fund?")
        compose.onNodeWithTag("ai-send").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("ai-action") }

        compose.onNodeWithTag("ai-action-cancel").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTextExists("Allocation cancelled. No budget change was made.") }

        compose.onNodeWithTag("ai-input").performTextInput("Move it into my emergency fund")
        compose.onNodeWithTag("ai-send").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("ai-action") }
        compose.onNodeWithTag("ai-action-confirm").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTextExists("Allocation recorded: ₹2,150 → Emergency Fund goal.") }
    }

    @Test
    fun threadsPillCountsAndSwitches() {
        boot()
        compose.onNodeWithTag("ai-threads").assertTextEquals("Threads · 1")
        compose.onNodeWithTag("ai-threads").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("ai-new-thread") }
        compose.onNodeWithTag("ai-new-thread").performClick()
        compose.onNodeWithTag("ai-threads").assertTextEquals("Threads · 2")
    }

    @Test
    fun aFailingModelPreservesTheMessageAndOffersRetry() {
        val exploding = object : ChatModel {
            override fun reply(question: String, context: AiContext): Flow<ChatEvent> = flow {
                throw RuntimeException("backend down")
            }
        }
        boot(exploding)
        compose.onNodeWithTag("ai-input").performTextInput("Can I increase my SIP next month?")
        compose.onNodeWithTag("ai-send").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists("ai-error") }
        assertTrue(compose.onAllNodesWithTextExists("Penny couldn't answer right now.", substring = true))
        assertTrue(compose.onAllNodesWithTextExists("No allocation or financial change was made.", substring = true))
        // The user's message stays in the thread.
        assertTrue(compose.onAllNodesWithTextExists("Can I increase my SIP next month?"))
    }
}
