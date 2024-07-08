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
    profileImage: "assets/gemini_logo.webp",
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

  int _selectedIndex = 0;

  static const List<Widget> _widgetOptions = <Widget> [
    Text(
      "Hello 1",
    ),
    Text(
      "Hello 2",
    ),
    Text(
      "Hello 3",
    ),
    Text(
      "Hello 4",
    ),
  ];

  void _onItemTapped(int index) {
    setState(() {
      _selectedIndex = index;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text(
          "Gemini Chat",
        ),
        leading: Builder(
          builder: (context) {
            return IconButton(
              onPressed: () {
                Scaffold.of(context).openDrawer(); 
              },
              icon: const Icon(Icons.menu),
            );
          },
        ),
      ),
      drawer: Drawer(
        child: ListView(
          children: [
            Container(
              padding: const EdgeInsets.all(12),
              height: 70,
              child: const Text(
                "Chat History",
                style: TextStyle(
                  fontSize: 24,
                ),
              ),
            ),
            ListTile(
              title: const Text("Hello 1"),
              selectedTileColor: Colors.black12,
              selectedColor: Colors.black,
              selected: _selectedIndex == 0,
              onTap: () {
                _onItemTapped(0);
                Navigator.pop(context);
              },
            ),
            ListTile(
              title: const Text("Hello 2"),
              selectedTileColor: Colors.black12,
              selectedColor: Colors.black,
              selected: _selectedIndex == 1,
              onTap: () {
                _onItemTapped(1);
                Navigator.pop(context);
              },
            ),
            ListTile(
              title: const Text("Hello 3"),
              selectedTileColor: Colors.black12,
              selectedColor: Colors.black,              
              selected: _selectedIndex == 2,
              onTap: () {
                _onItemTapped(2);
                Navigator.pop(context);
              },
            ),
            ListTile(
              title: const Text("Hello 4"),
              selectedTileColor: Colors.black12,
              selectedColor: Colors.black,              
              selected: _selectedIndex == 3,
              onTap: () {
                _onItemTapped(3);
                Navigator.pop(context);
              },
            ),
          ],
        )
      ),
      body: SafeArea(
        child: Column(
          children: [
            _widgetOptions[_selectedIndex],
            Expanded(
              child: DashChat(
                currentUser: currentUser,
                onSend: _sendMessage,
                messages: messages,
              ),
            ),
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
              const IconButton(
                onPressed: null, 
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