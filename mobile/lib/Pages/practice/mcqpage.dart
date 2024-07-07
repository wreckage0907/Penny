import 'package:flutter/material.dart';
import 'package:font_awesome_flutter/font_awesome_flutter.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:percent_indicator/linear_percent_indicator.dart';
import 'package:http/http.dart' as http;
import 'dart:convert';


class Chapter {
  final Map<String, Question> questions;

  Chapter({required this.questions});

  factory Chapter.fromJson(Map<String, dynamic> json) {
    var questionsMap = json['chapter 1'] as Map<String, dynamic>;
    var questions = questionsMap.map((key, value) => MapEntry(key, Question.fromJson(value)));
    return Chapter(questions: questions);
  }
}

class Question {
  final String question;
  final List<String> options;
  final String answer;
  final String hint;

  Question({
    required this.question,
    required this.options,
    required this.answer,
    required this.hint,
  });

  factory Question.fromJson(Map<String, dynamic> json) {
    return Question(
      question: json['question'],
      options: List<String>.from(json['options']),
      answer: json['ans'],
      hint: json['hint'],
    );
  }
}


class MCQPage extends StatefulWidget {
  const MCQPage({super.key});

  @override
  State<MCQPage> createState() => _MCQPageState();
}

class _MCQPageState extends State<MCQPage> {
  late Future<Map<String, dynamic>> futureQuestions;
  late Chapter chapter;
  int currentQuestionIndex = 0;
  String? selectedAnswer;

  @override
  void initState() {
    super.initState();
    futureQuestions = fetchQuestions();
  }

  Future<Map<String, dynamic>> fetchQuestions() async {
    final response = await http.get(Uri.parse('http://10.0.2.2:5000/questions'));
    if (response.statusCode == 200) {
      var json = jsonDecode(response.body);
      chapter = Chapter.fromJson(json);
      return json;
    } else {
      throw Exception('Failed to load questions');
    }
  }

  void nextQuestion() {
    if (currentQuestionIndex < chapter.questions.length - 1) {
      setState(() {
        currentQuestionIndex++;
        selectedAnswer = null;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return FutureBuilder<Map<String, dynamic>>(
      future: futureQuestions,
      builder: (context, snapshot) {
        if (snapshot.connectionState == ConnectionState.waiting) {
          return const CircularProgressIndicator();
        } else if (snapshot.hasError) {
          return Text('Error: ${snapshot.error}');
        } else if (snapshot.hasData) {
          return buildQuestionPage();
        } else {
          return const Text('No data');
        }
      },
    );
  }

  Widget buildQuestionPage() {
    Question currentQuestion = chapter.questions.values.elementAt(currentQuestionIndex);
    return Scaffold(
      appBar: AppBar(
        title: Text(
          "Chapter 1", // You might want to make this dynamic based on the actual chapter name
          style: GoogleFonts.dmSans(
            fontSize: 25,
            fontWeight: FontWeight.bold,
          ),
        ),
      ),
      body: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              "Question ${currentQuestionIndex + 1}/${chapter.questions.length}",
              style: const TextStyle(
                fontWeight: FontWeight.bold,
                fontSize: 32,
              ),
            ),
            const SizedBox(height: 5),
            LinearPercentIndicator(
              lineHeight: 10,
              progressColor: Colors.black87,
              backgroundColor: Colors.black12,
              percent: (currentQuestionIndex + 1) / chapter.questions.length,
              barRadius: const Radius.circular(5),
            ),
            const SizedBox(height: 20),
            Expanded(
              child: SingleChildScrollView(
                child: Column(
                  children: [
                    Stack(
                      children:[
                        Card(
                          color: Colors.black12,
                          child: Padding(
                            padding: const EdgeInsets.fromLTRB(40, 100, 40, 100),
                            child: Text(
                              currentQuestion.question,
                              textAlign: TextAlign.center,
                              style: GoogleFonts.dmSans(fontSize: 24),
                            ),
                          ),
                        ),
                        Positioned(
                          top: 15,
                          right: 15,
                          child: Container(
                            decoration: BoxDecoration(
                              color: Colors.black54,
                              borderRadius: BorderRadius.circular(50),
                            ),
                            child: IconButton(
                              onPressed: () {
                                // Show hint
                                ScaffoldMessenger.of(context).showSnackBar(
                                  SnackBar(content: Text(currentQuestion.hint)),
                                );
                              },
                              icon: const FaIcon(FontAwesomeIcons.lightbulb, color: Colors.white, size: 30,)
                            ),
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 20),
                    ListView.separated(
                      physics: const NeverScrollableScrollPhysics(),
                      shrinkWrap: true,
                      separatorBuilder: (BuildContext context, int index) => const Divider(
                        color: Colors.transparent,
                        height: 5,
                      ),
                      itemCount: currentQuestion.options.length,
                      itemBuilder: (BuildContext context, int index) {
                        return TextButton(
                          style: TextButton.styleFrom(
                            backgroundColor: selectedAnswer == currentQuestion.options[index] 
                                ? Colors.black26 
                                : Colors.black12,
                          ),
                          onPressed: () {
                            setState(() {
                              selectedAnswer = currentQuestion.options[index];
                            });
                          },
                          child: Text(
                            currentQuestion.options[index],
                            style: GoogleFonts.dmSans(color: Colors.black),
                          ),
                        );
                      },
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 20),
            Padding(
              padding: const EdgeInsets.fromLTRB(100, 0, 100, 0),
              child: ElevatedButton(
                style: ButtonStyle(
                  backgroundColor: WidgetStateProperty.all(
                    selectedAnswer != null ? Colors.black : Colors.black26
                  ),
                ),
                onPressed: selectedAnswer != null ? nextQuestion : null,
                child: Container(
                  alignment: Alignment.center,
                  child: const Text("NEXT")
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}