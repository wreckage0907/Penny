import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_markdown/flutter_markdown.dart';

class LessonPage extends StatelessWidget {
  const LessonPage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text("Introduction to Personal Finance"),
      ),
      body: FutureBuilder(
        future: rootBundle.loadString("assets/1_1.md"),
        builder: (BuildContext context, AsyncSnapshot<String> snapshot) {
          if(snapshot.hasData) {
            return Markdown(data: snapshot.data ?? '');
          }
          return const Center(
            child: CircularProgressIndicator(),
          );
        },
      )
    );
  }
}