//Pages Import
import 'package:flutter/material.dart';
import 'package:mobile/Pages/chatbot/chatbot_page.dart';
import 'package:mobile/Pages/coursePage/course_page.dart';
import 'package:mobile/Pages/expenseTracker/expense_tracker.dart';
import 'package:mobile/Pages/home/onboarding_page.dart';
import 'package:mobile/Pages/login/login.dart';
import 'package:mobile/Pages/login/signup.dart';
import 'package:mobile/Pages/home/home.dart';
import 'package:mobile/Pages/practice/list_of_modules.dart';
import 'package:mobile/Pages/stocks/stock_profile.dart';
import 'package:mobile/firebase_options.dart';

//Firebase Import
import 'package:firebase_core/firebase_core.dart';

//Gemini import
import 'package:flutter_gemini/flutter_gemini.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:mobile/main_functions.dart';

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
  const MyApp({super.key});
  
  @override
  Widget build(BuildContext context) {    
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      home: const AuthWrapper(),
      routes: {
        '/login': (context) => const LoginPage(),
        '/signup': (context) => const SignUpPage(),
        '/home': (context) => const Home(),
        '/coursepage': (context) => const CoursePage(),
        '/chatbot': (context) => const ChatbotPage(),
        '/budget': (context) => const ExpenseTracker(),
        '/practice': (context) => PracticeList(),
        '/onboarding': (context) => const OnboardingPage(),
        '/stockprofile': (context) => const StockProfile(),
      },
    );
  }
}