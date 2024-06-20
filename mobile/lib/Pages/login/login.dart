import 'package:flutter/material.dart';
import 'package:flutter/gestures.dart'; 
import 'package:google_fonts/google_fonts.dart';
import 'package:mobile/Pages/login/input_field.dart';
import 'package:mobile/Pages/login/page_buttons.dart';
import 'package:mobile/Services/auth.dart';

class LoginPage extends StatefulWidget {
  const LoginPage({Key? key}) : super(key: key);

  @override
  _LoginPageState createState() => _LoginPageState();
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
            Padding(
              padding: const EdgeInsets.only(left: 20,right:20,top: 20,bottom: 5),
              child: TextButton(
                onPressed: () async {
                  _authService.Login(email: _usernameController.text,password:_passwordController.text);
                },
                style: const ButtonStyle(
                  alignment: Alignment.center,
                  backgroundColor: WidgetStatePropertyAll(Color.fromRGBO(175, 92, 92, 0.8)),
                  padding: WidgetStatePropertyAll(EdgeInsets.symmetric(vertical: 16, horizontal: 24)),
                  minimumSize: WidgetStatePropertyAll(Size(double.infinity, 24)),
                  shape: WidgetStatePropertyAll(
                    RoundedRectangleBorder(
                      side: BorderSide(
                        color: Color.fromRGBO(109, 109, 109, 1),
                        width: 1.5,
                      ),
                      borderRadius: BorderRadius.all(Radius.circular(28)),
                    ),
                  ),
                ),
                child: Text(
                  'Sign In',
                  style: GoogleFonts.spectral(
                    fontSize: 20,
                    fontWeight: FontWeight.bold,
                    color: Colors.white,
                  ),
                ),
              ),
            ),
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