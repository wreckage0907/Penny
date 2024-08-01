import 'package:flutter/material.dart';
import 'package:flutter_markdown/flutter_markdown.dart';
import 'package:flutter_tts/flutter_tts.dart';
import 'package:font_awesome_flutter/font_awesome_flutter.dart';
import 'package:http/http.dart' as http;
import 'dart:convert';

import 'package:mobile/consts/app_colours.dart';
import 'package:mobile/consts/loading_widgets.dart';
import 'package:google_fonts/google_fonts.dart';

class LessonPage extends StatefulWidget {
  const LessonPage({
    required this.lessonName,
    required this.fileName,
    required this.subfolder,
    super.key
  });

  final String lessonName, fileName, subfolder;

  @override
  State<LessonPage> createState() => _LessonPageState();
}

class _LessonPageState extends State<LessonPage> {
  late String content;
  final FlutterTts flutterTts = FlutterTts();

  Future<String> fetchFile() async {
    try {
      final res = await http.get(Uri.parse("https://penny-uts7.onrender.com/get-file/${widget.subfolder}/${widget.fileName}"));
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

  
  Future<void> speak(String text) async {
    await flutterTts.setLanguage("en-US");
    await flutterTts.setPitch(0.5);
    print(text);
    await flutterTts.speak(text);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(
          widget.lessonName,
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
            content = snapshot.data!;
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
      floatingActionButton: ElevatedButton(
        onPressed: () {
          speak(content);
        },
        style: ElevatedButton.styleFrom(
          padding: const EdgeInsets.all(20),
          shape: const CircleBorder(),
          backgroundColor: AppColours.cardColor,
        ),
        child: const FaIcon(
          FontAwesomeIcons.volumeHigh,
          color: AppColours.textColor,
        ),
      )
    );
  }
}