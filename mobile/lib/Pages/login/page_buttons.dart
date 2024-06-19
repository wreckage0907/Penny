import 'package:flutter/material.dart';
import 'package:font_awesome_flutter/font_awesome_flutter.dart';
import 'package:google_fonts/google_fonts.dart';

import 'package:mobile/Services/auth.dart';



class SignInWithGoogleButton extends StatelessWidget {
  const SignInWithGoogleButton({super.key});

  @override
  Widget build(BuildContext context) {
    return IconButton(onPressed: () => print("Google Clicked"),
    icon: const FaIcon(FontAwesomeIcons.google, color: Colors.black, size: 32),
    style: const ButtonStyle(
      backgroundColor:  WidgetStatePropertyAll(Color.fromRGBO(255, 246, 229, 1)),
      padding:  WidgetStatePropertyAll(EdgeInsets.symmetric(horizontal: 20, vertical: 8)),
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
class LoginButton extends StatelessWidget {
  const LoginButton({super.key});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(left: 20,right:20,top: 20,bottom: 5),
      child: TextButton(onPressed : () {
                          Navigator.pushNamed(context, '/home');
                        }, 
      style:const ButtonStyle(alignment: Alignment.center,
      backgroundColor:  WidgetStatePropertyAll(Color.fromRGBO(175, 92, 92, 0.8)),
      padding:  WidgetStatePropertyAll(EdgeInsets.symmetric(vertical: 16, horizontal:  24)),
      minimumSize:  WidgetStatePropertyAll(Size(double.infinity, 24)),
      //elevation:  WidgetStatePropertyAll(2),
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
          'Sign In',
          style: GoogleFonts.spectral(
          fontSize: 20,
          fontWeight: FontWeight.bold,
          color: Colors.white,
        )
        )),
    );
      }
}

class SignUpButton extends StatelessWidget {
  final String username;
  final String email;
  final String password;
  const SignUpButton({super.key, required this.username, required this.email, required this.password});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(left: 20,right:20,top: 20,bottom: 5),
      child: TextButton(onPressed:  ()async=> {
        print(username+email+'sdfghjk'+password),
        await Auth().Signup(email: email, password: password, username: username)
      }, 
      style:const ButtonStyle(alignment: Alignment.center,
      backgroundColor:  WidgetStatePropertyAll(Color.fromRGBO(175, 92, 92, 0.8)),
      padding:  WidgetStatePropertyAll(EdgeInsets.symmetric(vertical: 16, horizontal:  24)),
      minimumSize:  WidgetStatePropertyAll(Size(double.infinity, 24)),
      //elevation:  WidgetStatePropertyAll(2),
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
          style: GoogleFonts.spectral(
          fontSize: 20,
          fontWeight: FontWeight.bold,
          color: Colors.white,
        )
        )),
    );
      }
}