import 'package:flutter/material.dart';

class UsernameField extends StatelessWidget {
  const UsernameField({super.key});

  @override
  Widget build(BuildContext context) {
    return const TextField(
      style: TextStyle(
        fontSize: 32,
        fontWeight: FontWeight.w400,
        color: Colors.black54,
      ),
      decoration: InputDecoration(
        hintText: 'Username',
        hintStyle: TextStyle(
          fontSize: 32,
          fontWeight: FontWeight.w400,
          color: Colors.black54,
        ),
        prefixIcon: Icon(Icons.person, size: 36),
        prefixIconColor: Colors.black54,
      ),
    );
  }
}


class PasswordField extends StatelessWidget {
  const PasswordField({super.key});

  @override
  Widget build(BuildContext context) {
    return const TextField(
      style: TextStyle(
        fontSize: 32,
        fontWeight: FontWeight.w400,
        color: Colors.black54,
      ),
      decoration: InputDecoration(
        hintText: 'Password',
        hintStyle: TextStyle(
          fontSize: 32,
          fontWeight: FontWeight.w400,
          color: Colors.black54,
        ),
        prefixIcon: Icon(Icons.lock_rounded, size: 36),
        prefixIconColor: Colors.black54,
      ),
    );
  }
}