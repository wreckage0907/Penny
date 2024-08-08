import 'package:dash_chat_2/dash_chat_2.dart';
import 'package:flutter/material.dart';
import 'package:flutter_gemini/flutter_gemini.dart';
import 'package:flutter_markdown/flutter_markdown.dart';
import 'package:font_awesome_flutter/font_awesome_flutter.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:mobile/consts/app_colours.dart';

class ChatbotPage extends StatefulWidget {
  const ChatbotPage({super.key});

  @override
  State<ChatbotPage> createState() => _ChatbotPageState();
}

class _ChatbotPageState extends State<ChatbotPage> {
  final Gemini gemini = Gemini.instance;

  Map<String, List<ChatMessage>> chatHistory = {
    "New Chat": [],
  };

  String currentChatId = "New Chat";

  ChatUser currentUser = ChatUser(id: "0", firstName: "User");
  ChatUser geminiUser = ChatUser(
    id: "1",
    firstName: "Gemini",
    profileImage: "assets/gemini_logo.webp",
  );

  void _sendMessage(ChatMessage chatMessage) {
    setState(() {
      chatHistory[currentChatId] = [
        chatMessage,
        ...chatHistory[currentChatId]!
      ];
    });

    try {
      String question = chatMessage.text;
      gemini.streamGenerateContent(question).listen((event) {
        ChatMessage? lastMesssage = chatHistory[currentChatId]?.firstOrNull;
        if (lastMesssage != null && lastMesssage.user == geminiUser) {
          lastMesssage = chatHistory[currentChatId]?.removeAt(0);
          String response = event.content?.parts?.fold(
                  "", (previous, current) => "$previous ${current.text}") ??
              "";
          lastMesssage?.text += response;
          setState(() {
            chatHistory[currentChatId] = [
              lastMesssage!,
              ...chatHistory[currentChatId]!
            ];
          });
        } else {
          String response = event.content?.parts?.fold(
                  "", (previous, current) => "$previous ${current.text}") ??
              "";
          ChatMessage message = ChatMessage(
              user: geminiUser,
              createdAt: DateTime.now(),
              text: response,
              isMarkdown: true);
          setState(() {
            chatHistory[currentChatId] = [
              message,
              ...chatHistory[currentChatId]!
            ];
          });
        }
      });
    } catch (e) {
      print(e);
    }
  }

  void _startNewChat() {
    setState(() {
      currentChatId = "New Chat ${chatHistory.keys.length}";
      chatHistory[currentChatId] = [];
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColours.backgroundColor,
      appBar: AppBar(
        backgroundColor: AppColours.backgroundColor,
        title: Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            const Text(
              "Gemini Chat",
              style: TextStyle(
                color: AppColours.textColor,
              ),
            ),
            IconButton(
                onPressed: _startNewChat,
                icon: const FaIcon(
                  FontAwesomeIcons.penToSquare,
                  color: AppColours.textColor,
                  size: 22,
                )),
          ],
        ),
        leading: Builder(
          builder: (context) {
            return IconButton(
              onPressed: () {
                Scaffold.of(context).openDrawer();
              },
              icon: const Icon(
                Icons.menu,
                color: AppColours.textColor,
              ),
            );
          },
        ),
      ),
      drawer: Drawer(
          backgroundColor: AppColours.backgroundColor,
          child: ListView(
            children: [
              Container(
                padding: const EdgeInsets.all(12),
                height: 70,
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    const Text(
                      "Chat History",
                      style:
                          TextStyle(fontSize: 24, color: AppColours.textColor),
                    ),
                    IconButton(
                        onPressed: () {
                          _startNewChat();
                          Navigator.pop(context);
                        },
                        icon: const FaIcon(
                          FontAwesomeIcons.penToSquare,
                          color: Colors.black,
                          size: 22,
                        )),
                  ],
                ),
              ),
              ...chatHistory.keys.map((chatId) {
                return ListTile(
                  title: Text(chatId),
                  selectedTileColor: AppColours.cardColor,
                  selectedColor: AppColours.textColor,
                  selected: currentChatId == chatId,
                  onTap: () {
                    setState(() {
                      currentChatId = chatId;
                    });
                    Navigator.pop(context);
                  },
                );
              }).toList(),
            ],
          )),
      body: SafeArea(
        child: Column(
          children: [
            Expanded(
                child: DashChat(
              currentUser: currentUser,
              onSend: _sendMessage,
              messages: chatHistory[currentChatId]!,
              messageOptions: MessageOptions(
                currentUserContainerColor: AppColours.buttonColor,
                containerColor: AppColours.cardColor,
                currentUserTextColor: AppColours.backgroundColor,
                textColor: AppColours.textColor,
                messagePadding: const EdgeInsets.all(10),
                borderRadius: 12,
                markdownStyleSheet: MarkdownStyleSheet(
                  h1: GoogleFonts.poppins(
                    fontSize: 28,
                    fontWeight: FontWeight.bold,
                    color: AppColours.textColor,
                  ),
                  h2: GoogleFonts.poppins(
                    fontSize: 24,
                    fontWeight: FontWeight.bold,
                    color: AppColours.textColor,
                  ),
                  h3: GoogleFonts.poppins(
                    fontSize: 20,
                    fontWeight: FontWeight.bold,
                    color: AppColours.textColor,
                  ),
                  p: GoogleFonts.dmSans(
                    fontSize: 16,
                    color: AppColours.textColor,
                    height: 1.5,
                  ),
                  strong: GoogleFonts.dmSans(
                    fontWeight: FontWeight.bold,
                    color: AppColours.textColor,
                  ),
                  em: GoogleFonts.dmSans(
                    fontStyle: FontStyle.italic,
                    color: AppColours.textColor,
                  ),
                  blockquote: GoogleFonts.dmSans(
                    fontSize: 16,
                    fontStyle: FontStyle.italic,
                    color: AppColours.textColor.withOpacity(0.7),
                  ),
                  code: GoogleFonts.firaCode(
                    fontSize: 14,
                    backgroundColor: Colors.grey[200],
                    color: Colors.black87,
                  ),
                  codeblockDecoration: BoxDecoration(
                    color: Colors.grey[200],
                    borderRadius: BorderRadius.circular(8),
                  ),
                  listBullet: GoogleFonts.dmSans(
                    fontSize: 16,
                    color: AppColours.textColor,
                  ),
                ),
                messageTextBuilder: (ChatMessage message,
                    ChatMessage? previousMessage, ChatMessage? nextMessage) {
                  if (message.user.id == currentUser.id) {
                    return Text(
                      message.text,
                      style: GoogleFonts.poppins(
                        color: AppColours.backgroundColor,
                      ),
                    );
                  }
                  return MarkdownBody(
                    data: message.text,
                    styleSheet: MarkdownStyleSheet(
                      p: GoogleFonts.dmSans(
                        fontSize: 16,
                        color: AppColours.textColor,
                      ),
                      strong: GoogleFonts.dmSans(
                        fontWeight: FontWeight.bold,
                        color: AppColours.textColor,
                      ),
                      em: GoogleFonts.dmSans(
                        fontStyle: FontStyle.italic,
                        color: AppColours.textColor,
                      ),
                      blockquote: GoogleFonts.dmSans(
                        fontSize: 16,
                        fontStyle: FontStyle.italic,
                        color: AppColours.textColor.withOpacity(0.7),
                      ),
                      code: GoogleFonts.firaCode(
                        fontSize: 14,
                        backgroundColor: Colors.grey[200],
                        color: Colors.black87,
                      ),
                      codeblockDecoration: BoxDecoration(
                        color: Colors.grey[200],
                        borderRadius: BorderRadius.circular(8),
                      ),
                      listBullet: GoogleFonts.dmSans(
                        fontSize: 16,
                        color: AppColours.textColor,
                      ),
                    ),
                  );
                },
              ),
              inputOptions: InputOptions(
                inputDecoration: InputDecoration(
                  fillColor: AppColours.cardColor,
                  filled: true,
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(20),
                    borderSide: BorderSide.none,
                  ),
                  hintText: "Type a message...",
                  hintStyle: GoogleFonts.poppins(
                      color: AppColours.textColor.withOpacity(0.6)),
                ),
                sendButtonBuilder: (onSend) {
                  return IconButton(
                    icon: const Icon(
                      Icons.send,
                      color: AppColours.textColor,
                    ),
                    onPressed: onSend,
                  );
                },
                inputTextStyle: GoogleFonts.poppins(
                  color: AppColours.textColor,
                ),
              ),
            ))
          ],
        ),
      ),
      bottomNavigationBar: Padding(
        padding: const EdgeInsets.only(bottom: 14),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.spaceAround,
          children: [
            IconButton(
                onPressed: () => Navigator.pushNamed(context, '/home'),
                icon: const Icon(
                  Icons.home_outlined,
                  size: 40,
                  color: AppColours.textColor,
                )),
            const IconButton(
                onPressed: null,
                icon: Icon(
                  Icons.chat_rounded,
                  size: 40,
                  color: AppColours.textColor,
                )),
            IconButton(
                onPressed: () =>
                    Navigator.pushNamed(context, '/profilesettings'),
                icon: const Icon(
                  Icons.person_outline_rounded,
                  size: 40,
                  color: AppColours.textColor,
                )),
          ],
        ),
      ),
    );
  }
}
