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
        prefixIcon: Icon(Icons.person_outlined, size: 36),
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