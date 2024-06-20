import 'package:flutter/material.dart';
import 'package:mobile/Pages/login/login.dart';
import 'package:mobile/Pages/login/signup.dart';
import 'package:mobile/Pages/home/home.dart';

import 'package:firebase_core/firebase_core.dart';
import 'package:mobile/firebase_options.dart';
import 'package:firebase_auth/firebase_auth.dart';

void main()async{
  WidgetsFlutterBinding.ensureInitialized(); 
  await Firebase.initializeApp(
    options: DefaultFirebaseOptions.currentPlatform,
  ); 
  
  runApp(const MyApp());
}
class MyApp extends StatelessWidget {
  const MyApp({super.key});

  Future<int> userState() async {
      User? user = await FirebaseAuth.instance.authStateChanges().first;
      if (user == null) {
        print('User is currently signed out!');
        return -1;
      } else {
        print('User is signed in!');
        return 1;
      }
    }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      home: const Home(),
      initialRoute: userState() == 1 ? '/home' : '/login',
      routes: {
        '/login': (context) => LoginPage(),
        '/signup': (context) =>  SignUpPage(),
        '/home': (context) => const Home(),
      },
    );
  }
}

