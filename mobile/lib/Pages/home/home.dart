import 'package:flutter/material.dart';

class Home extends StatelessWidget {
  const Home({super.key});

  @override
  Widget build(BuildContext context) {
    return  Scaffold(
      appBar: AppBar(
          title: const Text('Basic Flutter Progress Bar'),
        ),
        body:  const Center(
          child: CircularProgressIndicator(
  valueColor: AlwaysStoppedAnimation<Color>(Colors.blue),
  strokeWidth: 5.0, // Width of the circular line
),

        ),
    );
  }
}