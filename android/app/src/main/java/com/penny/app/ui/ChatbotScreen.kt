package com.penny.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.penny.app.PennyServices
import com.penny.app.core.AppRoutes
import com.penny.app.core.ChatLogic
import kotlinx.coroutines.launch

/** ChatbotPage (chatbot_page.dart): drawer of chats, streamed replies, bottom bar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatbotScreen(services: PennyServices, navigate: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    val drawer = rememberDrawerState(DrawerValue.Closed)
    // chatHistory: insertion-ordered, lost when the page is left (in-memory State).
    val history = remember { mutableStateMapOf(ChatLogic.FIRST_CHAT to emptyList<ChatLogic.Message>()) }
    val order = remember { mutableStateOf(listOf(ChatLogic.FIRST_CHAT)) }
    var current by remember { mutableStateOf(ChatLogic.FIRST_CHAT) }
    var input by remember { mutableStateOf("") }

    fun startNewChat() {
        val id = ChatLogic.newChatId(order.value)
        order.value = order.value + id
        history[id] = emptyList()
        current = id
    }

    fun send() {
        val text = input
        if (text.isEmpty()) return // DashChat only sends non-empty input
        input = ""
        history[current] = ChatLogic.send(history.getValue(current), text)
        scope.launch {
            // Errors on the stream are unhandled in Flutter: nothing visible.
            runCatching {
                services.chat.streamGenerateContent(text).collect { parts ->
                    // Events land in whichever chat is CURRENT when they arrive (quirk).
                    history[current] = ChatLogic.onEvent(history.getValue(current), parts)
                }
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawer,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = PennyPalette.background, modifier = Modifier.testTag("chat-drawer")) {
                Row(Modifier.fillMaxWidth().height(70.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Chat History", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = PennyPalette.text)
                    IconButton({ startNewChat(); scope.launch { drawer.close() } }, Modifier.testTag("drawer-new-chat")) {
                        Icon(Icons.Filled.EditNote, "New chat", tint = Color.Black)
                    }
                }
                order.value.forEach { id ->
                    NavigationDrawerItem(
                        label = { Text(id) },
                        selected = id == current,
                        onClick = { current = id; scope.launch { drawer.close() } },
                        colors = NavigationDrawerItemDefaults.colors(selectedContainerColor = PennyPalette.card,
                            selectedTextColor = PennyPalette.text),
                        modifier = Modifier.testTag("drawer-chat-$id"),
                    )
                }
            }
        },
    ) {
        Scaffold(
            containerColor = PennyPalette.background,
            topBar = {
                TopAppBar(
                    title = {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(ChatLogic.TITLE, fontWeight = FontWeight.SemiBold, color = PennyPalette.text,
                                modifier = Modifier.testTag("chat-title"))
                            IconButton(::startNewChat, Modifier.testTag("chat-new")) {
                                Icon(Icons.Filled.EditNote, "New chat", Modifier.size(22.dp), tint = PennyPalette.text)
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton({ scope.launch { drawer.open() } }, Modifier.testTag("chat-menu")) {
                            Icon(Icons.Filled.Menu, "Chat history", tint = PennyPalette.text)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = PennyPalette.background),
                )
            },
            bottomBar = {
                Row(Modifier.fillMaxWidth().padding(bottom = 14.dp), horizontalArrangement = Arrangement.SpaceAround) {
                    IconButton({ navigate(AppRoutes.HOME) }, Modifier.testTag("chat-nav-home")) {
                        Icon(Icons.Outlined.Home, "Home", Modifier.size(40.dp), tint = PennyPalette.text)
                    }
                    IconButton({}, enabled = false, modifier = Modifier.testTag("chat-nav-chat")) {
                        Icon(Icons.Filled.Chat, "Chatbot", Modifier.size(40.dp), tint = PennyPalette.text)
                    }
                    IconButton({ navigate(AppRoutes.PROFILE_SETTINGS) }, Modifier.testTag("chat-nav-profile")) {
                        Icon(Icons.Rounded.PersonOutline, "Profile", Modifier.size(40.dp), tint = PennyPalette.text)
                    }
                }
            },
        ) { inner ->
            Column(Modifier.fillMaxSize().padding(inner).testTag("chat-page")) {
                val messages = history[current].orEmpty()
                LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp), reverseLayout = true) {
                    itemsIndexed(messages) { i, m -> Bubble(i, m) }
                }
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextField(
                        input, { input = it }, Modifier.weight(1f).testTag("chat-input"),
                        placeholder = { Text(ChatLogic.HINT) },
                        shape = RoundedCornerShape(24.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = PennyPalette.card, unfocusedContainerColor = PennyPalette.card,
                            focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                        ),
                    )
                    IconButton(::send, Modifier.testTag("chat-send")) {
                        Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = PennyPalette.text)
                    }
                }
            }
        }
    }
}

/** DashChat bubble: user = plain text on buttonColor (right); Gemini = markdown on cardColor (left). */
@Composable
private fun Bubble(index: Int, message: ChatLogic.Message) {
    val mine = message.author == ChatLogic.Author.USER
    Box(Modifier.fillMaxWidth().padding(vertical = 4.dp), contentAlignment = if (mine) Alignment.CenterEnd else Alignment.CenterStart) {
        Column(
            Modifier.widthIn(max = 300.dp).background(if (mine) PennyPalette.button else PennyPalette.card, RoundedCornerShape(16.dp))
                .padding(12.dp).testTag(if (mine) "chat-user-$index" else "chat-bot-$index"),
        ) {
            if (!mine) Text("Gemini", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PennyPalette.text)
            if (mine) {
                Text(message.text, color = PennyPalette.background, modifier = Modifier.testTag("chat-text-$index"))
            } else {
                Box(Modifier.testTag("chat-text-$index").clickable(enabled = false) {}) {
                    Markdown(message.text, colors = markdownColor(text = PennyPalette.text))
                }
            }
        }
    }
}
