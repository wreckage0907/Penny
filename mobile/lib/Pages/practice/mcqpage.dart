import 'package:flutter/material.dart';
import 'package:font_awesome_flutter/font_awesome_flutter.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:mobile/consts/app_colours.dart';
import 'package:mobile/consts/loading_widgets.dart';
import 'package:percent_indicator/linear_percent_indicator.dart';
import 'package:http/http.dart' as http;
import 'dart:convert';


class Chapter {
  final Map<String, Question> questions;

  Chapter({required this.questions});

  factory Chapter.fromJson(Map<String, dynamic> json) {
    var chapterData = json.values.first as Map<String, dynamic>;
    var questions = chapterData.map((key, value) => MapEntry(key, Question.fromJson(value)));
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
  const MCQPage({required this.index ,super.key});
  
  final int index;

  @override
  State<MCQPage> createState() => _MCQPageState();
}

class _MCQPageState extends State<MCQPage> {
  
  late Map things = { 
    0:"introduction_to_personal_finance",
    1:"setting_financial_goals",
    2:"budgeting_and_expense_tracking",
    3:"online_and_mobile_banking"
  };
  late Future<Map<String, dynamic>> futureQuestions;
  late String chapterName = things[widget.index];
  late int noOfQuestions = 5;
  late Chapter chapter;
  int currentQuestionIndex = 0;
  String? selectedAnswer;

  @override
  void initState() {
    super.initState();
    futureQuestions = fetchQuestions(chapterName, noOfQuestions);
  }

  Future<Map<String, dynamic>> fetchQuestions(String chap, int n) async {
    try {
      final response = await http.get(Uri.parse('https://penny-uts7.onrender.com/generate_questions/$chap?num_question=$n'));
      if (response.statusCode == 200) {
        var json = jsonDecode(response.body);
        if (json is Map<String, dynamic>) {
          chapter = Chapter.fromJson(json);
          return json;
        } else {
          throw Exception('Invalid response format');
        }
      } else {
        throw Exception('Failed to load questions: ${response.statusCode}');
      }
    } catch (e) {
      print('Error fetching questions: $e');
      throw Exception('Failed to load questions: $e');
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
          return Scaffold(
            backgroundColor: AppColours.backgroundColor,
            body: Center(child: CustomLoadingWidgets.fourRotatingDots()),
          );
        } else if (snapshot.hasError) {
          print('Error in FutureBuilder: ${snapshot.error}');
          return Scaffold(
            body: Center(
              child: Text('Error: ${snapshot.error}'),
            ),
          );
        } else if (snapshot.hasData && snapshot.data != null) {
          print('Data received: ${snapshot.data}');
          return buildQuestionPage();
        } else {
          print('No data available');
          return const Scaffold(
            body: Center(
              child: Text('No data available'),
            ),
          );
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