import 'package:firebase_auth/firebase_auth.dart';
import 'package:flutter/material.dart';
import 'package:flutter/gestures.dart';
import 'package:font_awesome_flutter/font_awesome_flutter.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:mobile/Pages/home/home.dart';
import 'package:mobile/Pages/login/input_field.dart';
import 'package:mobile/Services/auth.dart';
import 'package:http/http.dart' as http;
import 'dart:convert';
import 'package:mobile/app_colours.dart';

class LoginPage extends StatefulWidget {
  const LoginPage({super.key});

  @override
  State<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends State<LoginPage> {
  final TextEditingController _usernameController = TextEditingController();
  final TextEditingController _passwordController = TextEditingController();
  final Auth _authService = Auth();

  @override
  void dispose() {
    _usernameController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  Future<Map<String, String?>> getUserData(String userId) async {
    try {
      final response = await http.get(
        Uri.parse('https://penny-4jam.onrender.com/user').replace(
          queryParameters: {'user_id': userId},
        ),
      );

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        if (data['user'] != null) {
          return {
            'email': data['user']['email'][0],
            'fullName':
                '${data['user']['firstName'][0]} ${data['user']['lastName'][0]}'
          };
        } else {
          print('User data not found in response');
          return {'email': null, 'fullName': null};
        }
      } else if (response.statusCode == 404) {
        print('User not found');
        return {'email': null, 'fullName': null};
      } else {
        print('Error: ${response.statusCode}');
        return {'email': null, 'fullName': null};
      }
    } catch (e) {
      print('Error retrieving user data: $e');
      return {'email': null, 'fullName': null};
    }
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
                  'Welcome Back!',
                  style: GoogleFonts.spectral(
                    fontSize: 45,
                    fontWeight: FontWeight.w300,
                    color: AppColours.textColor,
                  ),
                ),
                const SizedBox(height: 26),
                UsernameField(
                  controller: _usernameController,
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
                        UserCredential? user =
                            await _authService.signinWithGoogle();
                        if (user != null) {
                          Navigator.pushReplacement(context,
                              MaterialPageRoute(builder: (context) => const Home()));
                        }
                      },
                      icon: const FaIcon(FontAwesomeIcons.google,
                          color: AppColours.buttonColor, size: 32),
                      style: const ButtonStyle(
                        backgroundColor: WidgetStatePropertyAll(AppColours.cardColor),
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
                        UserCredential? user =
                            await _authService.signInWithGithub();
                        if (user != null) {
                          Navigator.pushReplacement(context,
                              MaterialPageRoute(builder: (context) => Home()));
                        }
                      },
                      icon: const FaIcon(FontAwesomeIcons.github,
                          color: AppColours.buttonColor, size: 32),
                      style: const ButtonStyle(
                        backgroundColor:  WidgetStatePropertyAll(AppColours.cardColor),
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
                    IconButton(
                      onPressed: () => _authService.signInWithFacebook(),
                      icon: const FaIcon(FontAwesomeIcons.facebookF,
                          color: AppColours.buttonColor, size: 32),
                      style: const ButtonStyle(
                        backgroundColor:  WidgetStatePropertyAll(AppColours.cardColor),
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
                  ],
                ),
                const SizedBox(height: 20),
                Padding(
                  padding: const EdgeInsets.only(
                      left: 40, right: 40, top: 20, bottom: 5),
                  child: TextButton(
                    onPressed: () async {
                      final userData =
                          await getUserData(_usernameController.text);
                      if (userData['email'] != null) {
                        UserCredential? user = await _authService.login(
                            email: userData['email']!,
                            password: _passwordController.text);
                        if (user != null) {
                          await _authService
                              .saveUsername(_usernameController.text);
                          if (userData['fullName'] != null) {
                            await _authService
                                .saveFullName(userData['fullName']!);
                          }
                          Navigator.of(context).pushReplacementNamed('/home');
                        }
                      } else {
                        print('Email not found for username');
                      }
                    },
                    style: const ButtonStyle(
                      alignment: Alignment.center,
                      backgroundColor: WidgetStatePropertyAll(AppColours.buttonColor),
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
                      'Sign In',
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
                      const TextSpan(text: 'Don\'t have an account? '),
                      TextSpan(
                        text: 'Sign Up',
                        style: const TextStyle(
                          color: AppColours.buttonColor,
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
        ),
      ),
    );
  }
}
