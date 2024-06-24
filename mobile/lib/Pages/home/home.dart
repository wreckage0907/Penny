import 'package:flutter/material.dart';
import 'package:mobile/Services/auth.dart';

class home extends StatefulWidget {
  const home({super.key});

  @override
  State<home> createState() => _homeState();
}

class _homeState extends State<home> {
  final Auth _authService = Auth();
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Home'),
      ),
      body: Center(
        child: TextButton(onPressed: () async { _authService.signout(context: context);}, child: const Text('Sign Out'),
      ),
      
    ),
    floatingActionButton: FloatingActionButton(
        onPressed: () {
          Navigator.pushNamed(context, '/expense');
        },
        child: const Icon(Icons.add),
      ),
    );
  }
}