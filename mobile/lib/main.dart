//Pages Import
import 'package:flutter/material.dart';
import 'package:mobile/Pages/chatbot/chatbot_page.dart';
import 'package:mobile/Pages/coursePage/course_page.dart';
import 'package:mobile/Pages/coursePage/learning_page.dart';
import 'package:mobile/Pages/coursePage/lesson_page.dart';
import 'package:mobile/Pages/expenseTracker/expense_tracker.dart';
//import 'package:mobile/Pages/expenseTracker/budget.dart';
import 'package:mobile/Pages/home/onboarding_page.dart';
import 'package:mobile/Pages/login/login.dart';
import 'package:mobile/Pages/login/signup.dart';
import 'package:mobile/Pages/home/home.dart';
import 'package:mobile/Pages/practice/list_of_modules.dart';
import 'package:mobile/Pages/practice/mcqpage.dart';
import 'package:mobile/firebase_options.dart';

//Firebase Import
import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_auth/firebase_auth.dart';

//Gemini import
import 'package:flutter_gemini/flutter_gemini.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:shared_preferences/shared_preferences.dart';


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
  runApp(MyApp());
}

class MyApp extends StatelessWidget {
  MyApp({Key? key}) : super(key: key);

  Future<String?> _getUsername() async {
    SharedPreferences prefs =  await SharedPreferences.getInstance();
    return prefs.getString('username');
  }
  
  @override
  Widget build(BuildContext context) {    
    return FutureBuilder<String?> (
      future: _getUsername(),
      builder: (context, snapshot) {
        String? userName = snapshot.data;
        return MaterialApp(
          debugShowCheckedModeBanner: false,
          home: AuthWrapper(username: userName),
          routes: {
            '/login': (context) => const LoginPage(),
            '/signup': (context) => const SignUpPage(),
            '/home': (context) => Home(
              username: userName,
            ),
            '/coursepage': (context) => CoursePage(),
            '/chatbot': (context) => const ChatbotPage(),
            '/budget': (context) => ExpenseTracker(
              username: userName,
            ),
            '/practice': (context) => PracticeList(),
            '/mcq': (context) => const MCQPage(),
            '/onboarding': (context) => const OnboardingPage(),
          },
        );
      }
    );
  }
}

class AuthWrapper extends StatelessWidget {
  final String? username;
  const AuthWrapper({required this.username, super.key});

  @override
  Widget build(BuildContext context) {
    return StreamBuilder<User?>(
      stream: FirebaseAuth.instance.authStateChanges(),
      builder: (context, snapshot) {
        if (snapshot.connectionState == ConnectionState.waiting) {
          return const CircularProgressIndicator();
        } else {
          if (snapshot.hasData) {

            return Home(username: username);
          } else {
            return const LoginPage();
          }
        }
      },
    );
  }
}