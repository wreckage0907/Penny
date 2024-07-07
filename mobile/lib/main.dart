//Pages Import
import 'package:flutter/material.dart';
import 'package:mobile/Pages/chatbot/chatbot_page.dart';
import 'package:mobile/Pages/home/learning_page.dart';
import 'package:mobile/Pages/login/login.dart';
import 'package:mobile/Pages/login/signup.dart';
import 'package:mobile/Pages/home/home.dart';

//Firebase Import
import 'package:firebase_core/firebase_core.dart';
import 'package:mobile/firebase_options.dart';
import 'package:firebase_auth/firebase_auth.dart';

//Gemini import
import 'package:flutter_gemini/flutter_gemini.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';

Future<void> main() async {
  await dotenv.load(fileName: ".env");
  String apiKey = dotenv.env['GEMINI_API_KEY']!;

  WidgetsFlutterBinding.ensureInitialized(); 
  await Firebase.initializeApp(
    options: DefaultFirebaseOptions.currentPlatform,
  ); 

  Gemini.init(
    apiKey: apiKey,
  );
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      home: AuthWrapper(),
      routes: {
        '/login': (context) => const LoginPage(),
        '/signup': (context) => const SignUpPage(),
        '/home': (context) => const home(),
        '/coursepage': (context) => const LearningPage(),
        '/chatbot': (context) => const ChatbotPage(),
        //'/expense': (context) => const Expense(),
      },
    );
  }
}

class AuthWrapper extends StatelessWidget {
  const AuthWrapper({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return StreamBuilder<User?>(
      stream: FirebaseAuth.instance.authStateChanges(),
      builder: (context, snapshot) {
        if (snapshot.connectionState == ConnectionState.waiting) {
          return const CircularProgressIndicator(); // Or a splash screen
        } else {
          if (snapshot.hasData) {
            return const home();
          } else {
            return const LoginPage();
          }
        }
      },
    );
  }
}