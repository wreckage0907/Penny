import 'package:firebase_auth/firebase_auth.dart';
import 'package:flutter/material.dart';
import 'package:flutter/gestures.dart';
import 'package:font_awesome_flutter/font_awesome_flutter.dart'; 
import 'package:google_fonts/google_fonts.dart';
import 'package:mobile/Pages/login/input_field.dart';
import 'package:mobile/Services/auth.dart';

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

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color.fromRGBO(255, 246, 229, 1),
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
                    color: Colors.black87,
                  ),
                ),
                const SizedBox(height: 26),
                UsernameField(controller: _usernameController,),
                const SizedBox(height: 14),
                PasswordField(controller: _passwordController,),
                const SizedBox(height: 25),
                Text(
                  'or continue with',
                  style: GoogleFonts.spectral(
                    fontSize: 24,
                    fontWeight: FontWeight.w300,
                    color: Colors.black87,
                  ),
                ),
                const SizedBox(height: 10),
                Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    IconButton(
                      onPressed: () async {
                        UserCredential? user = await _authService.signinWithGoogle();
                        if(user != null) {
                          Navigator.pushNamed(context, '/home');
                        }
                      },
                      icon: const FaIcon(FontAwesomeIcons.google, color: Colors.black, size: 32),
                      style: const ButtonStyle(
                        backgroundColor: WidgetStatePropertyAll(Color.fromRGBO(255, 246, 229, 1)),
                        padding: WidgetStatePropertyAll(EdgeInsets.symmetric(horizontal: 20, vertical: 8)),
                        minimumSize: WidgetStatePropertyAll(Size(80, 20)),
                        shape: WidgetStatePropertyAll(
                          CircleBorder(
                            side: BorderSide( 
                              color: Colors.black38,
                              width: 2,
                            ),
                          ),
                        ),
                        elevation: WidgetStatePropertyAll(2),
                      ),
                    ),
                    IconButton(
                      onPressed: () async {
                        UserCredential? user =await _authService.signInWithGithub();
                        if(user != null){
                          Navigator.pushNamed(context, '/home');
                        }
                      },
                      icon: const FaIcon(FontAwesomeIcons.github, color: Colors.black, size: 32),
                      style: const ButtonStyle(
                        backgroundColor:  WidgetStatePropertyAll(Color.fromRGBO(255, 246, 229, 1)),
                        padding:  WidgetStatePropertyAll(EdgeInsets.symmetric(horizontal: 1, vertical: 8)),
                        minimumSize:  WidgetStatePropertyAll(Size(80,20)),
                        shape:  WidgetStatePropertyAll(
                          CircleBorder(
                            side: BorderSide(
                              color: Colors.black38,
                              width: 2,
                            ),
                          ),
                        ),
                        elevation:  WidgetStatePropertyAll(2),
                      ),
                    ),
                    IconButton(
                      onPressed: () => print("Microsoft Clicked"),
                      icon: const FaIcon(FontAwesomeIcons.microsoft, color: Colors.black, size: 32),
                      style: const ButtonStyle(
                        backgroundColor:  WidgetStatePropertyAll(Color.fromRGBO(255, 246, 229, 1)),
                        padding:  WidgetStatePropertyAll(EdgeInsets.symmetric(horizontal: 1, vertical: 8)),
                        minimumSize:  WidgetStatePropertyAll(Size(80,20)),
                        shape:  WidgetStatePropertyAll(
                          CircleBorder(
                            side: BorderSide(
                              color: Colors.black38,
                              width: 2,
                            ),
                          ),
                        ),
                        elevation:  WidgetStatePropertyAll(2),
                      ),
                    ),  
                  ],
                ),
                const SizedBox(height: 20),
                Padding(
                  padding: const EdgeInsets.only(left: 20,right:20,top: 20,bottom: 5),
                  child: TextButton(
                    onPressed: () async {
                      UserCredential? user = await _authService.login(email: _usernameController.text,password:_passwordController.text);
                      if(user != null){
                        Navigator.pushNamed(context, '/home');
                      }
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
              ],
            ),
          ),
        ),
      ),
    );
  }
}