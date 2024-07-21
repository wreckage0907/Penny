import 'package:flutter/material.dart';
import 'package:flutter_markdown/flutter_markdown.dart';
import 'package:http/http.dart' as http;
import 'dart:convert';

import 'package:mobile/consts/app_colours.dart';
import 'package:mobile/consts/loading_widgets.dart';

class LessonPage extends StatelessWidget {
  const LessonPage(
      {required this.lessonName,
      required this.fileName,
      required this.subfolder,
      super.key});

  final String lessonName, fileName, subfolder;

  Future<String> fetchFile() async {
    try {
      final res = await http
          .get(Uri.parse("http://10.0.2.2:8000/get-file/$subfolder/$fileName"));
      if (res.statusCode == 200) {
        final data = json.decode(res.body);
        return data['content'];
      } else {
        throw Exception('Failed to load file');
      }
    } catch (e) {
      print('Error: $e');
      return 'Failed to load content';
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
        appBar: AppBar(
          title: Text(lessonName),
        ),
        body: FutureBuilder(
          future: fetchFile(),
          builder: (BuildContext context, AsyncSnapshot<String> snapshot) {
            if (snapshot.connectionState == ConnectionState.waiting) {
              return Scaffold(
                backgroundColor: AppColours.backgroundColor,
                body: Center(child: CustomLoadingWidgets.fourRotatingDots())
              );
            } else if (snapshot.hasError) {
              return Center(child: Text('Error: ${snapshot.error}'));
            } else {
              return Markdown(data: snapshot.data ?? '');
            }
          },
        ));
  }
}
