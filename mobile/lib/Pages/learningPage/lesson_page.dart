import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_markdown/flutter_markdown.dart';

class LessonPage extends StatelessWidget {
  const LessonPage({required this.lessonName, required this.fileName, super.key});

  final String lessonName, fileName;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(lessonName),
      ),
      body: FutureBuilder(
        future: rootBundle.loadString(fileName),
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