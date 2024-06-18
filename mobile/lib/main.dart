import 'package:flutter/material.dart';
import './login/login.dart';
import './login/signup.dart';
import './home/home.dart';

void main(){
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      home: const LoginPage(),
      initialRoute: '/login',
      routes: {
        '/login': (context) => const LoginPage(),
        '/signup': (context) =>  SignUp(),
        '/home': (context) => const Home(),
      },
    );
  }
}