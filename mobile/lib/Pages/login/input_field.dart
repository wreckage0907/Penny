import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

class UsernameField extends StatelessWidget {
  final TextEditingController controller;
  const UsernameField({Key? key, required this.controller}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return TextField(
      controller: controller,
      decoration: const InputDecoration(
        hintText: 'Username',
        prefixIcon: Padding(
          padding: EdgeInsets.only(right: 14),
          child: Icon(Icons.person_outlined, size: 36),
        ),
        prefixIconColor: Colors.black54,
        contentPadding: EdgeInsets.only(top: 10),
      ),
      style: GoogleFonts.spectral(
        fontSize: 24,
        fontWeight: FontWeight.w400,
        color: Colors.black54,
      ),
    );
  }
}

class EmailField extends StatelessWidget {
  final TextEditingController controller;
  const EmailField({Key? key, required this.controller}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return TextField(
      controller: controller,
      style: GoogleFonts.spectral(
        fontSize: 24,
        fontWeight: FontWeight.w400,
        color: Colors.black54,
      ),
      decoration: const InputDecoration(
        hintText: 'Email',
        prefixIcon: Padding(
          padding: EdgeInsets.only(right: 14),
          child: Icon(Icons.email_outlined, size: 36),
        ),
        prefixIconColor: Colors.black54,
        contentPadding: EdgeInsets.only(top: 10),
      ),
    );
  }
}

class PasswordField extends StatelessWidget {
  final TextEditingController controller;
  const PasswordField({Key? key, required this.controller}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return TextField(
      controller: controller,
      style: GoogleFonts.spectral(
        fontSize: 24,
        fontWeight: FontWeight.w400,
        color: Colors.black54,
      ),
      decoration: const InputDecoration(
        hintText: 'Password',
        prefixIcon: Padding(
          padding: EdgeInsets.only(right: 14),
          child: Icon(Icons.lock_outline, size: 36),
        ),
        prefixIconColor: Colors.black54,
        contentPadding: EdgeInsets.only(top: 10),
      ),
    );
  }
}