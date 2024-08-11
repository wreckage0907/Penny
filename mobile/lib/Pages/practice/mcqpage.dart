import 'package:flutter/material.dart';
import 'package:font_awesome_flutter/font_awesome_flutter.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:mobile/consts/app_colours.dart';
import 'package:mobile/consts/backend_url.dart';
import 'package:mobile/consts/loading_widgets.dart';
import 'package:mobile/consts/toast_messages.dart';
import 'package:percent_indicator/linear_percent_indicator.dart';
import 'package:http/http.dart' as http;
import 'dart:convert';

class Chapter {
  final Map<String, Question> questions;

  Chapter({required this.questions});

  factory Chapter.fromJson(Map<String, dynamic> json) {
    var chapterData = json.values.first as Map<String, dynamic>;
    var questions = chapterData
        .map((key, value) => MapEntry(key, Question.fromJson(value)));
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
  const MCQPage({required this.index, super.key});

  final int index;

  @override
  State<MCQPage> createState() => _MCQPageState();
}

class _MCQPageState extends State<MCQPage> {
  late Map things = {
    0: "introduction_to_personal_finance",
    1: "setting_financial_goals",
    2: "budgeting_and_expense_tracking",
    3: "online_and_mobile_banking"
  };
  late Future<Map<String, dynamic>> futureQuestions;
  late String chapterName = things[widget.index];
  late int noOfQuestions = 5;
  late Chapter chapter;
  int currentQuestionIndex = 0;
  String? selectedAnswer;
  bool isAnswerChecked = false;

  @override
  void initState() {
    super.initState();
    futureQuestions = fetchQuestions(chapterName, noOfQuestions);
  }

  Future<Map<String, dynamic>> fetchQuestions(String chap, int n) async {
    try {
      final response = await http.get(Uri.parse(
          '${backendUrl()}/generate_questions/$chap?num_question=$n'));
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

  void selectAnswer(String answer) {
    setState(() {
      selectedAnswer = answer;
      isAnswerChecked = true;
    });
  }

  void nextQuestion() {
    if (currentQuestionIndex < chapter.questions.length - 1) {
      setState(() {
        currentQuestionIndex++;
        selectedAnswer = null;
        isAnswerChecked = false;
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
    Question currentQuestion =
        chapter.questions.values.elementAt(currentQuestionIndex);
    return Scaffold(
      backgroundColor: AppColours.backgroundColor,
      appBar: AppBar(
        backgroundColor: AppColours.backgroundColor,
        title: Text("Chapter 1",
            style:
                GoogleFonts.dmSans(fontSize: 24, fontWeight: FontWeight.bold)),
      ),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.all(16),
            child: LinearPercentIndicator(
              lineHeight: 15,
              progressColor: AppColours.buttonColor.withOpacity(0.7),
              backgroundColor: AppColours.cardColor.withOpacity(0.6),
              percent: (currentQuestionIndex + 1) / chapter.questions.length,
              barRadius: const Radius.circular(20),
              leading: Text(
                  "${currentQuestionIndex + 1}/${chapter.questions.length}",
                  style: GoogleFonts.dmSans(
                      fontWeight: FontWeight.bold, fontSize: 18,
                      color: Colors.black)),
            ),
          ),
          Expanded(
            child: SingleChildScrollView(
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Card(
                  color: AppColours.backgroundColor,
                  child: Padding(
                    padding: const EdgeInsets.all(16),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            Text(
                                "Question ${currentQuestionIndex + 1}/${chapter.questions.length}",
                                style: GoogleFonts.dmSans(
                                    fontWeight: FontWeight.w300, fontSize: 16)),
                            GestureDetector(
                              onTap: () => ToastMessages.infoToast(
                                  context, currentQuestion.hint),
                              child: Row(
                                children: [
                                  const FaIcon(FontAwesomeIcons.circleQuestion,
                                      size: 16),
                                  const SizedBox(width: 5),
                                  Text("Hint",
                                      style: GoogleFonts.dmSans(
                                          fontWeight: FontWeight.w300,
                                          fontSize: 16)),
                                ],
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 16),
                        Text(currentQuestion.question,
                            style: GoogleFonts.dmSans(
                                fontWeight: FontWeight.w700, fontSize: 20)),
                        const SizedBox(height: 16),
                        ...currentQuestion.options.asMap().entries.map(
                            (entry) => buildOptionButton(
                                entry.value,
                                String.fromCharCode(65 + entry.key),
                                currentQuestion)),
                      ],
                    ),
                  ),
                ),
              ),
            ),
          ),
        ],
      ),
      bottomNavigationBar: Padding(
        padding: const EdgeInsets.all(16),
        child: ElevatedButton(
          onPressed: nextQuestion,
          style: ElevatedButton.styleFrom(
            backgroundColor: isAnswerChecked ? AppColours.buttonColor : AppColours.backgroundColor,
            minimumSize: const Size(double.infinity, 50),
          ),
          child: Text("Next Question", style: GoogleFonts.dmSans(fontSize: 18,
          color: isAnswerChecked ? AppColours.backgroundColor : AppColours.textColor)),
        ),
      ),
    );
  }

  Widget buildOptionButton(
      String option, String optionLetter, Question currentQuestion) {
    bool isSelected = selectedAnswer == optionLetter;
    bool isCorrectAnswer = optionLetter == currentQuestion.answer;
    Color backgroundColor = AppColours.backgroundColor;
    Color borderColor = Colors.grey.shade300;
    IconData? trailingIcon;

    if (isAnswerChecked) {
      if (isCorrectAnswer) {
        backgroundColor = Colors.green.shade100;
        borderColor = Colors.green;
        trailingIcon = Icons.check_circle;
      } else if (isSelected && !isCorrectAnswer) {
        backgroundColor = Colors.red.shade100;
        borderColor = Colors.red;
        trailingIcon = Icons.cancel;
      }
    }

    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: InkWell(
        onTap: isAnswerChecked ? null : () => selectAnswer(optionLetter),
        child: Container(
          padding: const EdgeInsets.all(12),
          decoration: BoxDecoration(
            color: backgroundColor,
            borderRadius: BorderRadius.circular(8),
            border: Border.all(color: borderColor),
          ),
          child: Row(
            children: [
              Expanded(
                child: Text(option, style: GoogleFonts.dmSans(fontSize: 16)),
              ),
              if (trailingIcon != null)
                Icon(trailingIcon,
                    color: isCorrectAnswer ? Colors.green : Colors.red),
            ],
          ),
        ),
      ),
    );
  }
}
