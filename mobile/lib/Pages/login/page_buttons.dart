import 'package:flutter/material.dart';
import 'package:font_awesome_flutter/font_awesome_flutter.dart';
import 'package:mobile/consts/app_colours.dart';

class SignInWithGoogleButton extends StatelessWidget {
  const SignInWithGoogleButton({super.key});

  @override
  Widget build(BuildContext context) {
    return IconButton(
      onPressed: () => print("Google Clicked"),
      icon: const FaIcon(FontAwesomeIcons.google, color: AppColours.buttonColor, size: 32),
      style: const ButtonStyle(
        backgroundColor: WidgetStatePropertyAll(AppColours.cardColor),
        padding: WidgetStatePropertyAll(EdgeInsets.symmetric(horizontal: 20, vertical: 8)),
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
    );
  }
}

class FacebookButton extends StatelessWidget {
  const FacebookButton({super.key});

  @override
  Widget build(BuildContext context) {
    return IconButton(onPressed: () => print("Facebook Clicked"),
    icon: const FaIcon(FontAwesomeIcons.facebookF, color: AppColours.buttonColor, size: 32),
    style: const ButtonStyle(
      backgroundColor:  WidgetStatePropertyAll(AppColours.cardColor),
      padding:  WidgetStatePropertyAll(EdgeInsets.symmetric(horizontal: 1, vertical: 8)),
      minimumSize:  WidgetStatePropertyAll(Size(80,20)),
      shape:  WidgetStatePropertyAll(
        CircleBorder(
          side: BorderSide(
            color: AppColours.cardColor,
            width: 2,
          ),
        ),
      ),
      elevation:  WidgetStatePropertyAll(2),
    ),
    );  
  }
}

class MicrosoftButton extends StatelessWidget {
  const MicrosoftButton({super.key});

  @override
  Widget build(BuildContext context) {
    return IconButton(onPressed: () => print("Microsoft Clicked"),
    icon: const FaIcon(FontAwesomeIcons.microsoft, color: AppColours.buttonColor, size: 32),
    style: const ButtonStyle(
      backgroundColor:  WidgetStatePropertyAll(AppColours.cardColor),
      padding:  WidgetStatePropertyAll(EdgeInsets.symmetric(horizontal: 1, vertical: 8)),
      minimumSize:  WidgetStatePropertyAll(Size(80,20)),
      shape:  WidgetStatePropertyAll(
        CircleBorder(
          side: BorderSide(
            color: AppColours.cardColor,
            width: 2,
          ),
        ),
      ),
      elevation:  WidgetStatePropertyAll(2),
    ),
    );  
  }
}
