import 'package:flutter/material.dart';
import 'package:flutter/gestures.dart'; // Add this line to import the gesture_detector.dart file
import 'package:mobile/Pages/login/page_buttons.dart';
import 'package:mobile/Pages/login/input_field.dart';
import 'package:google_fonts/google_fonts.dart';

class LoginPage extends StatelessWidget {
  final TextEditingController _usernameController = TextEditingController();
  final TextEditingController _passwordController = TextEditingController();
  LoginPage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color.fromRGBO(255, 246, 229, 1),
      body: Padding(
        padding: const EdgeInsets.all(28),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Center(
              child: Container(
                width: 200,
                child: const Image(
                  image: AssetImage('assets/title.png'),
                ),
              ),
            ),
            const SizedBox(
              height: 20,
            ),
            Center(
              child: Text(
                'Welcome Back!',
                style: GoogleFonts.spectral(
                  fontSize: 45,
                  fontWeight: FontWeight.w300,
                  color: Colors.black87,
                ),
              ),
            ),
            const SizedBox(height: 26),
            UsernameField(controller: _usernameController,),
            const SizedBox(height: 14),
            PasswordField(controller: _passwordController,),
            const SizedBox(height: 25),
            Center(
              child: Text(
                'or continue with',
                style: GoogleFonts.spectral(
                  fontSize: 24,
                  fontWeight: FontWeight.w300,
                  color: Colors.black87,
                ),
              ),
            ),
            const SizedBox(height: 10),
            const Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                SignInWithGoogleButton(),
                FacebookButton(),
                AppleButton()
              ],
            ),
            const SizedBox(height: 20),
            const LoginButton(),
            const SizedBox(height: 30),
            Center(
              child: RichText(
                text: TextSpan(
                  style: GoogleFonts.spectral(
                    fontSize: 19,
                    fontWeight: FontWeight.w400,
                    color: Colors.black,
                  ),
                  children: [
                    const TextSpan(text: 'Don\'t have an account? '),
                    TextSpan(
                      text: 'Sign Up',
                      style: const TextStyle(
                        color: Colors.redAccent,
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
            ),
          ],
        ),
      ),
    );
  }
}
