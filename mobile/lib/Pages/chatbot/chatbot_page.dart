import 'package:dash_chat_2/dash_chat_2.dart';
import 'package:flutter/material.dart';
import 'package:flutter_gemini/flutter_gemini.dart';

class ChatbotPage extends StatefulWidget {
  const ChatbotPage({super.key});

  @override
  State<ChatbotPage> createState() => _ChatbotPageState();
}

class _ChatbotPageState extends State<ChatbotPage> {
  final Gemini gemini = Gemini.instance;

  List<ChatMessage> messages = [];

  ChatUser currentUser = ChatUser(id: "0", firstName: "User");
  ChatUser geminiUser = ChatUser(
    id: "1", 
    firstName: "Gemini",
    profileImage: "https://seeklogo.com/images/G/google-gemini-logo-A5787B2669-seeklogo.com.png",
  );

  void _sendMessage(ChatMessage chatMessage) {
    setState(() {
      messages = [chatMessage, ...messages];
    });

    try {
      String question = chatMessage.text;
      gemini.streamGenerateContent(question).listen((event) {
        ChatMessage? lastMesssage = messages.firstOrNull;
        if(lastMesssage != null && lastMesssage.user == geminiUser) {
          lastMesssage = messages.removeAt(0);
          String response = event.content?.parts?.fold(
            "", 
            (previous, current) => "$previous ${current.text}") ?? "";
          lastMesssage.text += response;
          setState(() {
            messages = [lastMesssage!, ...messages];
          });
        }
        else {
          String response = event.content?.parts?.fold(
            "", 
            (previous, current) => "$previous ${current.text}") ?? "";
          ChatMessage message = ChatMessage(
            user: geminiUser, 
            createdAt: DateTime.now(), 
            text: response
          );
          setState(() {
            messages = [message, ...messages];
          });
        }
      });
    } catch (e) {
      print(e);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text(
          "Gemini Chat",
        ),
      ),
      body: DashChat(
        currentUser: currentUser,
        onSend: _sendMessage,
        messages: messages,
      ),
      bottomNavigationBar: Padding(
          padding: const EdgeInsets.only(bottom: 14),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceAround,
            children: [
              IconButton(
                onPressed: () => Navigator.pushNamed(context, '/home'), 
                icon: const Icon(
                  Icons.home_filled,
                  size: 40,
                  //color: Color.fromRGBO(53, 51, 58, 1),
                )
              ),
              IconButton(
                onPressed: () => Navigator.pushNamed(context, '/coursepage'), 
                icon: const Icon(
                  Icons.bar_chart_rounded,
                  size: 40,
                  //color: Color.fromRGBO(53, 51, 58, 1),
                )
              ),
              IconButton(
                onPressed: () => Navigator.pushNamed(context, '/chatbot'), 
                icon: const Icon(
                  Icons.chat_rounded,
                  size: 40,
                  //color: Color.fromRGBO(53, 51, 58, 1),
                )
              )
            ],
          ),
        ),
    );
  }
}