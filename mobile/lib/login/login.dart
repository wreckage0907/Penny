import 'package:flutter/material.dart';
import 'package:flutter/gestures.dart'; // Add this line to import the gesture_detector.dart file
import 'package:mobile/login/page_buttons.dart';
import 'package:mobile/login/input_field.dart';

class LoginPage extends StatelessWidget {
  const LoginPage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color.fromRGBO(255, 246, 229, 1),
      body: Padding(
        padding: const EdgeInsets.all(36),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Center(
              child:  Text(
                'LOGIN',
                style: TextStyle(
                  fontSize: 48,
                  fontWeight: FontWeight.bold,
                  color: Color.fromRGBO(0, 0, 0, 0.7),
                ),
              ),
            ),
            const SizedBox(height: 26),
            const UsernameField(),
            const SizedBox(height: 14),
            const PasswordField(),
            const SizedBox(height: 10),
            const Center(
              child: Text(
                'or',
                style: TextStyle(
                  fontSize: 23,
                  fontWeight: FontWeight.w300,
                  color: Colors.black87,
                ),
              ),
            ),
            const SizedBox(height: 10),
            const Row(
              children: [
                 SignInWithGoogleButton(),
                 FacebookButton(),
              ],
            ),
            const SizedBox(height: 20),
            const LoginButton(),
            const SizedBox(height: 26),
            RichText(
              text: TextSpan(
                style: const TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.w300,
                  color: Colors.black87,
                ),
                children: [
                  const TextSpan(text: 'Don\'t have an account?'),
                  TextSpan(
                    text: ' Sign Up',
                    style: const TextStyle(
                      color: Colors.blue,
                      decoration: TextDecoration.underline,
                    ),
                    recognizer: TapGestureRecognizer()
                      ..onTap = () {
                        Navigator.pushNamed(context, '/signup');
                      },
                  ),
                ],
              ),
            ),
            
          ],
        ),
      ),
    );
  }
}