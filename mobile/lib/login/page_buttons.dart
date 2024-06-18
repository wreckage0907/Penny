import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:font_awesome_flutter/font_awesome_flutter.dart';


class SignInWithGoogleButton extends StatelessWidget {
  const SignInWithGoogleButton({super.key});

  static void printstatement() {
    if (kDebugMode) print("Google Clicked");
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 16),
      child: TextButton.icon(
        onPressed: printstatement,
        style: const ButtonStyle(
          backgroundColor: WidgetStatePropertyAll(Color.fromRGBO(255, 246, 229, 1)),
          padding: WidgetStatePropertyAll(EdgeInsets.symmetric(horizontal: 16, vertical: 8)),
          minimumSize: WidgetStatePropertyAll(Size(120, 40)),
          shape: WidgetStatePropertyAll(
            RoundedRectangleBorder(
              side: BorderSide(
                color: Colors.black38,
                width: 2,
              ),
              borderRadius: BorderRadius.all(Radius.circular(8)),
            ),
          ),
          elevation: WidgetStatePropertyAll(2),
        ),
        icon: const Center(
          child:  FaIcon(
            FontAwesomeIcons.google,
            color: Colors.black,
            size: 32,
          )
        ),
        label: const Text(''), 
      ),
    );
  }
}

class FacebookButton extends StatelessWidget {
  const FacebookButton({super.key});

  static void printstatement() {
    if (kDebugMode) print("Facebook Clicked");
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 16),
      child: TextButton.icon(
        onPressed: printstatement,
        style: const ButtonStyle(
            backgroundColor: WidgetStatePropertyAll(Color.fromRGBO(255, 246, 229, 1)),
            padding: WidgetStatePropertyAll(EdgeInsets.symmetric(horizontal: 16, vertical: 8)),
            minimumSize: WidgetStatePropertyAll(Size(120, 40)),
            shape: WidgetStatePropertyAll(
              RoundedRectangleBorder(
                side: BorderSide(
                  color: Colors.black38,
                  width: 2,
                ),
                borderRadius: BorderRadius.all(Radius.circular(8)),
              ),
            ),
            elevation: WidgetStatePropertyAll(2),
          ),
        icon: const Center(
          child:  FaIcon(
            FontAwesomeIcons.facebookF,
            color: Colors.black,
            size: 32,
          )
        ),
        label: const Text(''), 
      ),
    );
  }
}

class LoginButton extends StatelessWidget {
  const LoginButton({super.key});

  static void printstatement() {
    if (kDebugMode) print("Login Clicked");
  }

  @override
  Widget build(BuildContext context) {
    return const ElevatedButton(
      onPressed: printstatement,
      style: ButtonStyle(
        backgroundColor:  WidgetStatePropertyAll(Color.fromRGBO(255, 246, 229, 1)),
        padding:  WidgetStatePropertyAll(EdgeInsets.symmetric(vertical: 16, horizontal: 32)),
        minimumSize:  WidgetStatePropertyAll(Size(double.infinity, 48)),
        shape:  WidgetStatePropertyAll(
          RoundedRectangleBorder(
            side: BorderSide(
              color: Color.fromRGBO(109, 109, 109, 1),
              width: 1.5,
            ),
            borderRadius: BorderRadius.all(Radius.circular(28)),
          ),
        ),
      ),
      child:  Text(
        'Login',
        style: TextStyle(
          fontSize: 20,
          fontWeight: FontWeight.bold,
          color: Color.fromRGBO(109, 109, 109, 1),
        ),
      ),
    );
  }
}

class SignUpButton extends StatelessWidget {
  const SignUpButton({super.key});

  static void printstatement() {
    if (kDebugMode) print("Sign Up Clicked");
  }

  @override
  Widget build(BuildContext context) {
    return const ElevatedButton(
      onPressed: printstatement,
      style: ButtonStyle(
        backgroundColor:  WidgetStatePropertyAll(Color.fromRGBO(255, 246, 229, 1)),
        padding:  WidgetStatePropertyAll(EdgeInsets.symmetric(vertical: 16, horizontal: 32)),
        minimumSize:  WidgetStatePropertyAll(Size(double.infinity, 48)),
        shape:  WidgetStatePropertyAll(
          RoundedRectangleBorder(
            side: BorderSide(
              color: Color.fromRGBO(109, 109, 109, 1),
              width: 1.5,
            ),
            borderRadius: BorderRadius.all(Radius.circular(28)),
          ),
        ),
      ),
      child:  Text(
        'Sign Up',
        style: TextStyle(
          fontSize: 20,
          fontWeight: FontWeight.bold,
          color: Color.fromRGBO(109, 109, 109, 1),
        ),
      ),
    );
  }
}