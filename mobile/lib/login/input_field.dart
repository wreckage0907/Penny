import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

class UsernameField extends StatelessWidget {
  const UsernameField({super.key});

  @override
  Widget build(BuildContext context) {
    return TextField(
      decoration: const InputDecoration(
        hintText: 'Username',
        hintStyle: TextStyle(
          fontSize: 32,
          fontWeight: FontWeight.w400,
          color: Colors.black54,
        ),
        prefixIcon: Icon(Icons.person_outlined, size: 36),
        prefixIconColor: Colors.black54,
      ),

      style: GoogleFonts.spectral(
        fontSize: 32,
        fontWeight: FontWeight.w400,
        color: Colors.black54,
      ),
      
    );
  }
}


class PasswordField extends StatelessWidget {
  const PasswordField({super.key});

  @override
  Widget build(BuildContext context) {
    return TextField(
      style: GoogleFonts.spectral(
        fontSize: 32,
        fontWeight: FontWeight.w400,
        color: Colors.black54,
      ),
      decoration: const InputDecoration(
        hintText: 'Password',
        prefixIcon: Icon(Icons.lock_outline, size: 36),
        prefixIconColor: Colors.black54,
      ),
    );
  }
}


class EmailField extends StatelessWidget {
  const EmailField({super.key});

  @override
  Widget build(BuildContext context) {
    return const TextField(
      style: TextStyle(
        fontSize: 32,
        fontWeight: FontWeight.w400,
        color: Colors.black54,
      ),
      decoration: InputDecoration(
        hintText: 'Email',
        hintStyle: TextStyle(
          fontSize: 32,
          fontWeight: FontWeight.w400,
          color: Colors.black54,
        ),
        prefixIcon: Icon(Icons.email_outlined, size: 36),
        prefixIconColor: Colors.black54,
      ),
    );
  }
}