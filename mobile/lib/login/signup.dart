import 'package:flutter/material.dart';
import 'package:mobile/login/input_field.dart';
import 'package:mobile/login/page_buttons.dart';
import 'package:flutter/gestures.dart';
import 'package:google_fonts/google_fonts.dart';

class SignUp extends StatelessWidget {
  SignUp({Key? key}) : super(key: key);

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
                'Create Account!',
                style: GoogleFonts.spectral(
                  fontSize: 45,
                  fontWeight: FontWeight.w300,
                  color: Colors.black87,
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
            const SizedBox(height: 26),
            Center(
              child: RichText(
                text: TextSpan(
                  style: GoogleFonts.spectral(
                    fontSize: 19,
                    fontWeight: FontWeight.w400,
                    color: Colors.black,
                  ),
                  children: [
                    const TextSpan(text: 'Already have an account? '),
                    TextSpan(
                      text: 'Login',
                      style: const TextStyle(
                        color: Colors.redAccent,
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
            ),
          ],
        ),
      ),
    );
  }
}
