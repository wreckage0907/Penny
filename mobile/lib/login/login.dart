import 'package:flutter/material.dart';
import 'package:mobile/login/login_page_buttons.dart';
import 'package:mobile/login/login_page_input_fields.dart';

class LoginPage extends StatelessWidget {
  const LoginPage({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        scaffoldBackgroundColor: const Color.fromARGB(255, 240, 229, 223),
      ),
      home: const Scaffold(
        body: Padding(
          padding: EdgeInsets.all(36),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                'LOGIN',
                style: TextStyle(
                  fontSize: 48,
                  fontWeight: FontWeight.bold,
                  color: Color.fromRGBO(0, 0, 0, 0.7),
                ),
              ),
              SizedBox(height: 26),
              UsernameField(),
              SizedBox(height: 14),
              PasswordField(),
              SizedBox(height: 10),
              Center(
                child: Text(
                  'or',
                  style: TextStyle(
                    fontSize: 23,
                    fontWeight: FontWeight.w300,
                    color: Colors.black87,
                  )
                ),
              ),
              SizedBox(height: 10),
              SignInWithGoogleButton(),
              SizedBox(height: 26),
              Text(
                'Having trouble logging in? Try Signing Up',
                style: TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.w300,
                  color: Colors.black87,
                )
              ),
              SizedBox(height: 20),
              LoginButton(),
            ],
          ),
        )
      ),
    );
  }
}