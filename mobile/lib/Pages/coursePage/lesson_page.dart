import 'package:flutter/material.dart';
import 'package:flutter_markdown/flutter_markdown.dart';
import 'package:http/http.dart' as http;
import 'dart:convert';

import 'package:mobile/consts/app_colours.dart';
import 'package:mobile/consts/loading_widgets.dart';
import 'package:google_fonts/google_fonts.dart';

class LessonPage extends StatelessWidget {
  const LessonPage({
    required this.lessonName,
    required this.fileName,
    required this.subfolder,
    super.key
  });

  final String lessonName, fileName, subfolder;

  Future<String> fetchFile() async {
    try {
      final res = await http.get(Uri.parse("https://penny-uts7.onrender.com/get-file/$subfolder/$fileName"));
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
        title: Text(
          lessonName,
          style: GoogleFonts.poppins(
            fontWeight: FontWeight.bold,
            color: AppColours.textColor,
          ),
        ),
        backgroundColor: AppColours.backgroundColor,
        elevation: 0,
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
            return Container(
              color: AppColours.backgroundColor,
              child: Markdown(
                data: snapshot.data ?? '',
                styleSheet: MarkdownStyleSheet(
                  h1: GoogleFonts.poppins(
                    fontSize: 28,
                    fontWeight: FontWeight.bold,
                    color: AppColours.textColor,
                  ),
                  h2: GoogleFonts.poppins(
                    fontSize: 24,
                    fontWeight: FontWeight.bold,
                    color: AppColours.textColor,
                  ),
                  h3: GoogleFonts.poppins(
                    fontSize: 20,
                    fontWeight: FontWeight.bold,
                    color: AppColours.textColor,
                  ),
                  p: GoogleFonts.dmSans(
                    fontSize: 16,
                    color: AppColours.textColor,
                    height: 1.5,
                  ),
                  strong: GoogleFonts.dmSans(
                    fontWeight: FontWeight.bold,
                    color: AppColours.textColor,
                  ),
                  em: GoogleFonts.dmSans(
                    fontStyle: FontStyle.italic,
                    color: AppColours.textColor,
                  ),
                  blockquote: GoogleFonts.dmSans(
                    fontSize: 16,
                    fontStyle: FontStyle.italic,
                    color: AppColours.textColor.withOpacity(0.7),
                  ),
                  code: GoogleFonts.firaCode(
                    fontSize: 14,
                    backgroundColor: Colors.grey[200],
                    color: Colors.black87,
                  ),
                  codeblockDecoration: BoxDecoration(
                    color: Colors.grey[200],
                    borderRadius: BorderRadius.circular(8),
                  ),
                  listBullet: GoogleFonts.dmSans(
                    fontSize: 16,
                    color: AppColours.textColor,
                  ),
                ),
                padding: const EdgeInsets.all(16),
              ),
            );
          }
        },
      ),
    );
  }
}