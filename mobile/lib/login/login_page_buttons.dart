import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:material_design_icons_flutter/material_design_icons_flutter.dart';

class SignInWithGoogleButton extends StatelessWidget {
  const SignInWithGoogleButton({super.key});

  static void printstatement() {
    if(kDebugMode) print("Google Clicked");
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(
        left: 14,
        right: 14,
      ),
      child: TextButton.icon(
        onPressed: printstatement,
        style: const ButtonStyle(
          backgroundColor: WidgetStatePropertyAll(Color.fromARGB(255, 240, 229, 223)),
          padding: WidgetStatePropertyAll(EdgeInsets.all(8)),
          minimumSize: WidgetStatePropertyAll(Size(double.infinity, 20)),
          shape: WidgetStatePropertyAll(
            RoundedRectangleBorder(
              side: BorderSide(
                color: Colors.black54,
                width: 2,
              ),
              borderRadius: BorderRadius.all(Radius.circular(10)),
            )
          )
        ),
        icon: const Icon(
          MdiIcons.google,
          size: 32,
          color: Colors.blue,
        ),
        label: const Text(
          'Sign in with Google',
          style: TextStyle(
            color: Color.fromRGBO(0, 0, 0, 0.7),
            fontSize: 25,
            fontWeight: FontWeight.w400,
          )
        ), 
      ),
    );
  }
}


class LoginButton extends StatelessWidget {
  const LoginButton({super.key});

  static void printstatement() {
    if(kDebugMode) print("Login Clicked");
  }

  @override
  Widget build(BuildContext context) {
    return const ElevatedButton(
      onPressed: printstatement,
      style: ButtonStyle(
        backgroundColor: WidgetStatePropertyAll(Color.fromARGB(255, 240, 229, 223)),
        padding: WidgetStatePropertyAll(EdgeInsets.all(16)),
        minimumSize: WidgetStatePropertyAll(Size(double.infinity, 20)),
        shape: WidgetStatePropertyAll(
          RoundedRectangleBorder(
            side: BorderSide(
              color: Colors.black54,
              width: 2,
            ),
            borderRadius: BorderRadius.all(Radius.circular(38)),
          )
        )
      ),
      child: Text(
        'Login',
        style: TextStyle(
          fontSize: 28,
          fontWeight: FontWeight.w600,
          color: Color.fromRGBO(0, 0, 0, 0.7),
        )
      )
    );
  }
}