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
  const LessonPage(
      {required this.lessonName,
      required this.fileName,
      required this.subfolder,
      super.key});

  final String lessonName, fileName, subfolder;

  @override
  State<LessonPage> createState() => _LessonPageState();
}

class _LessonPageState extends State<LessonPage> {
  late String content;
  final FlutterTts flutterTts = FlutterTts();
  bool isSpeaking = false;

  @override
  void dispose() {
    flutterTts.stop();
    super.dispose();
  }

  Future<String> fetchFile() async {
    try {
      final res = await http.get(Uri.parse(
          "https://penny-uts7.onrender.com/get-file/${widget.subfolder}/${widget.fileName}"));
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

  String cleanMarkdownText(String markdownText) {
    markdownText = markdownText.replaceAll(RegExp(r'#+\s'), '');

    markdownText = markdownText.replaceAll(RegExp(r'\*{1,2}'), '');

    markdownText =
        markdownText.replaceAll(RegExp(r'^\s*[-*]\s', multiLine: true), '');

    markdownText = markdownText.replaceAll(RegExp(r'`{1,3}'), '');

    markdownText = markdownText.replaceAll(RegExp(r'\[|\]|\(.*?\)'), '');

    return markdownText.trim();
  }

  Future<void> toggleSpeak(String text) async {
    if (isSpeaking) {
      await flutterTts.stop();
      setState(() {
        isSpeaking = false;
      });
    } else {
      await flutterTts.setLanguage("en-US");
      await flutterTts.setPitch(0.8);
      String cleanedText = cleanMarkdownText(text);
      await flutterTts.speak(cleanedText);
      setState(() {
        isSpeaking = true;
      });

      flutterTts.setCompletionHandler(() {
        setState(() {
          isSpeaking = false;
        });
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text(
              widget.lessonName,
              style: GoogleFonts.poppins(
                fontWeight: FontWeight.bold,
                color: AppColours.textColor,
              ),
            ),
            IconButton(
                onPressed: () => toggleSpeak(content),
                icon: FaIcon(
                  isSpeaking
                      ? FontAwesomeIcons.volumeXmark
                      : FontAwesomeIcons.volumeHigh,
                  color: AppColours.textColor,
                ))
          ],
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
                body: Center(child: CustomLoadingWidgets.fourRotatingDots()));
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
    );
  }
}
