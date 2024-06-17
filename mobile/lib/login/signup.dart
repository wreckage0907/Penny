import 'package:flutter/material.dart';
import 'package:mobile/login/input_field.dart';
import 'package:mobile/login/page_buttons.dart';
import 'package:flutter/gestures.dart';

class SignUp extends StatelessWidget {
  SignUp({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return  Scaffold(
      backgroundColor: Color.fromRGBO(255, 246, 229, 1),
      body: Padding(
        padding: const EdgeInsets.all(36),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Center(
              child:  Text(
                'SIGN UP',
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
            const EmailField(),
            const SizedBox(height: 14),
            const PasswordField(),
            const SizedBox(height: 26),
            const SignUpButton(),
            const SizedBox(height: 20),
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
            const Row(
              children: [
                 SignInWithGoogleButton(),
                 FacebookButton(),
              ],
            ),
            const SizedBox(height: 26),
            RichText(
              text: TextSpan(
                style: const TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.w300,
                  color: Colors.black87,
                ),
                children: [
                  const TextSpan(text: 'Already have an account?'),
                  TextSpan(
                    text: 'Login',
                    style: const TextStyle(
                      color: Colors.blue,
                      decoration: TextDecoration.underline,
                    ),
                    recognizer: TapGestureRecognizer()
                      ..onTap = () {
                        Navigator.pushNamed(context, '/login');
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
