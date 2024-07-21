import 'package:flutter/material.dart';
import 'package:flutter/gestures.dart';
import 'package:font_awesome_flutter/font_awesome_flutter.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:mobile/Pages/login/input_field.dart';
import 'package:mobile/Pages/login/page_buttons.dart';
import 'package:mobile/Services/auth.dart';
import 'package:firebase_auth/firebase_auth.dart';
import 'package:mobile/consts/app_colours.dart';

class SignUpPage extends StatefulWidget {
  const SignUpPage({super.key});

  @override
  State<SignUpPage> createState() => _SignUpPageState();
}

class _SignUpPageState extends State<SignUpPage> {
  final TextEditingController _passwordController = TextEditingController();
  final TextEditingController _emailController = TextEditingController();
  final Auth _authService = Auth();

  @override
  void dispose() {
    _passwordController.dispose();
    _emailController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColours.backgroundColor,
      body: Container(
        alignment: Alignment.center,
        child: SingleChildScrollView(
          child: Padding(
            padding: const EdgeInsets.all(28),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                const SizedBox(
                  width: 200,
                  child: Image(
                    image: AssetImage('assets/title.png'),
                  ),
                ),
                const SizedBox(
                  height: 20,
                ),
                Text(
                  'Welcome!',
                  style: GoogleFonts.spectral(
                    fontSize: 45,
                    fontWeight: FontWeight.w300,
                    color: AppColours.textColor,
                  ),
                ),
                const SizedBox(height: 14),
                EmailField(
                  controller: _emailController,
                ),
                const SizedBox(height: 14),
                PasswordField(
                  controller: _passwordController,
                ),
                const SizedBox(height: 25),
                Text(
                  'or continue with',
                  style: GoogleFonts.spectral(
                    fontSize: 24,
                    fontWeight: FontWeight.w300,
                    color: AppColours.textColor,
                  ),
                ),
                const SizedBox(height: 10),
                Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    IconButton(
                      onPressed: () async {
                        final result = await _authService.signinWithGoogle();
                        if (result['user'] != null) {
                          if (result['isNewUser']) {
                            Navigator.pushNamed(context, '/onboarding');
                          } else {
                            Navigator.pushNamed(context, '/home');
                          }
                        } else {
                          print('Error: ${result['error']}');
                        }
                      },
                      icon: const FaIcon(FontAwesomeIcons.google,
                          color: AppColours.buttonColor, size: 32),
                      style: const ButtonStyle(
                        backgroundColor:
                            WidgetStatePropertyAll(AppColours.cardColor),
                        padding: WidgetStatePropertyAll(
                            EdgeInsets.symmetric(horizontal: 20, vertical: 8)),
                        minimumSize: WidgetStatePropertyAll(Size(80, 20)),
                        shape: WidgetStatePropertyAll(
                          CircleBorder(
                            side: BorderSide(
                              color: AppColours.cardColor,
                              width: 2,
                            ),
                          ),
                        ),
                        elevation: WidgetStatePropertyAll(2),
                      ),
                    ),
                    IconButton(
                      onPressed: () async {
                        final result = await _authService.signInWithGithub();
                        if (result['user'] != null) {
                          if (result['isNewUser']) {
                            Navigator.pushNamed(context, '/onboarding');
                          } else {
                            Navigator.pushNamed(context, '/home');
                          }
                        } else {
                          print('Error: ${result['error']}');
                        }
                      },
                      icon: const FaIcon(FontAwesomeIcons.github,
                          color: AppColours.buttonColor, size: 32),
                      style: const ButtonStyle(
                        backgroundColor:
                            WidgetStatePropertyAll(AppColours.cardColor),
                        padding: WidgetStatePropertyAll(
                            EdgeInsets.symmetric(horizontal: 1, vertical: 8)),
                        minimumSize: WidgetStatePropertyAll(Size(80, 20)),
                        shape: WidgetStatePropertyAll(
                          CircleBorder(
                            side: BorderSide(
                              color: AppColours.cardColor,
                              width: 2,
                            ),
                          ),
                        ),
                        elevation: WidgetStatePropertyAll(2),
                      ),
                    ),
                    const MicrosoftButton()
                  ],
                ),
                const SizedBox(height: 20),
                Padding(
                  padding: const EdgeInsets.only(
                      left: 40, right: 40, top: 20, bottom: 5),
                  child: TextButton(
                    onPressed: () async {
                      UserCredential? user = await _authService.signUp(
                          email: _emailController.text,
                          password: _passwordController.text);

                      if (user != null) {
                        Navigator.pushNamed(context, '/onboarding');
                      }
                    },
                    style: const ButtonStyle(
                      alignment: Alignment.center,
                      backgroundColor:
                          WidgetStatePropertyAll(AppColours.buttonColor),
                      padding: WidgetStatePropertyAll(
                          EdgeInsets.symmetric(vertical: 16, horizontal: 24)),
                      minimumSize:
                          WidgetStatePropertyAll(Size(double.infinity, 24)),
                      shape: WidgetStatePropertyAll(
                        RoundedRectangleBorder(
                          side: BorderSide(
                            color: AppColours.buttonColor,
                            width: 1.5,
                          ),
                          borderRadius: BorderRadius.all(Radius.circular(34)),
                        ),
                      ),
                    ),
                    child: Text(
                      'Sign Up',
                      style: GoogleFonts.spectral(
                        fontSize: 24,
                        fontWeight: FontWeight.bold,
                        color: AppColours.backgroundColor,
                      ),
                    ),
                  ),
                ),
                const SizedBox(height: 30),
                RichText(
                  text: TextSpan(
                    style: GoogleFonts.spectral(
                      fontSize: 19,
                      fontWeight: FontWeight.w400,
                      color: Colors.black,
                    ),
                    children: [
                      const TextSpan(text: 'Already have an account? '),
                      TextSpan(
                        text: 'Sign In',
                        style: const TextStyle(
                          color: AppColours.buttonColor,
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
        ),
      ),
    );
  }
}
