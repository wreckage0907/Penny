import 'package:flutter/material.dart';
import 'package:font_awesome_flutter/font_awesome_flutter.dart';

class SignInWithGoogleButton extends StatelessWidget {
  const SignInWithGoogleButton({super.key});

  @override
  Widget build(BuildContext context) {
    return IconButton(
      onPressed: () => print("Google Clicked"),
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
    );
  }
}

class FacebookButton extends StatelessWidget {
  const FacebookButton({super.key});

  @override
  Widget build(BuildContext context) {
    return IconButton(onPressed: () => print("Facebook Clicked"),
    icon: const FaIcon(FontAwesomeIcons.facebookF, color: Colors.black, size: 32),
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
    );  
  }
}

class AppleButton extends StatelessWidget {
  const AppleButton({super.key});

  @override
  Widget build(BuildContext context) {
    return IconButton(onPressed: () => print("Facebook Clicked"),
    icon: const FaIcon(FontAwesomeIcons.apple, color: Colors.black, size: 32),
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
    );  
  }
}
