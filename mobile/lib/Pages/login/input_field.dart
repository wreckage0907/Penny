import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:mobile/consts/app_colours.dart';

class UsernameField extends StatelessWidget {
  final TextEditingController controller;
  const UsernameField({super.key, required this.controller});

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
        prefixIconColor: AppColours.textColor,
        contentPadding: EdgeInsets.only(top: 10),
      ),
      style: GoogleFonts.spectral(
        fontSize: 24,
        fontWeight: FontWeight.w400,
        color: AppColours.textColor,
      ),
    );
  }
}

class EmailField extends StatelessWidget {
  final TextEditingController controller;
  const EmailField({super.key, required this.controller});

  @override
  Widget build(BuildContext context) {
    return TextField(
      controller: controller,
      style: GoogleFonts.spectral(
        fontSize: 24,
        fontWeight: FontWeight.w400,
        color: AppColours.textColor,
      ),
      decoration: const InputDecoration(
        hintText: 'Email',
        prefixIcon: Padding(
          padding: EdgeInsets.only(right: 14),
          child: Icon(Icons.email_outlined, size: 36),
        ),
        prefixIconColor: AppColours.textColor,
        contentPadding: EdgeInsets.only(top: 10),
      ),
    );
  }
}

class PasswordField extends StatefulWidget {
  final TextEditingController controller;
  const PasswordField({super.key, required this.controller});

  @override
  State<PasswordField> createState() => _PasswordFieldState();
}

class _PasswordFieldState extends State<PasswordField> {
  bool _isObscured = true;

  @override
  Widget build(BuildContext context) {
    return TextField(
      obscureText: _isObscured,
      controller: widget.controller,
      style: GoogleFonts.spectral(
        fontSize: 24,
        fontWeight: FontWeight.w400,
        color: AppColours.textColor,
      ),
      decoration: InputDecoration(
        hintText: 'Password',
        prefixIcon: Padding(
          padding: const EdgeInsets.only(right: 14),
          child: Icon(
            _isObscured ? Icons.lock_outline : Icons.lock_open_rounded, 
            size: 36
          ),
        ),
        prefixIconColor: AppColours.textColor,
        contentPadding: const EdgeInsets.only(top: 10),
        suffixIcon: IconButton(
          icon: Icon(
            _isObscured ? Icons.visibility_off_rounded : Icons.visibility,
            size: 32,
          ),
          onPressed: () {
            setState(() {
              _isObscured = !_isObscured;           
            });
          },
        ),
        suffixIconColor: AppColours.textColor,
      ),
    );
  }
}