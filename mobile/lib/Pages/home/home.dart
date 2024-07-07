import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:mobile/Pages/chatbot/chatbot_page.dart';
import 'package:mobile/Pages/home/lesson_page.dart';
import 'package:mobile/Services/auth.dart';
import 'package:percent_indicator/circular_percent_indicator.dart';
import 'package:percent_indicator/linear_percent_indicator.dart';
import 'package:mobile/Pages/home/learning_page.dart';

class home extends StatefulWidget {
  const home({super.key});

  @override
  State<home> createState() => _HomeState();
}

class _HomeState extends State<home> {
  final Auth _authService = Auth();

  String username = "wreckage";

  List data = [
    {"title": "Course\nPage", "image": "assets/course_page.png"},
    {"title": "Expense\nTracker", "image": "assets/expense_tracker.png"},
    {"title": "Predict\nStocks", "image": "assets/predict_stocks.png"},
    {"title": "Practice", "image": "assets/practice.png"}
  ];

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      routes: {
        '/coursepage': (context) => const LearningPage(),
        '/lesson1': (context) => const LessonPage(
          lessonName: "Lesson 1",
          fileName: "assets/1_1.md",
        ),
        '/chatbot': (context) => const ChatbotPage(),
      },
      home: Scaffold(
        //backgroundColor: const Color.fromRGBO(232, 245, 233, 1),
        body: Padding(
          padding: const EdgeInsets.all(20.0),
          child: Column(
            children: [
              const SizedBox(height: 20),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(
                    'WELCOME,\n$username',
                    style: GoogleFonts.dmSans(
                      fontSize: 40,
                      fontWeight: FontWeight.w500,
                      //color: const Color.fromRGBO(53, 51, 58, 1),
                      height: 1.2,
                    ),
                  ),
                  IconButton(
                    onPressed: () async { _authService.signout(context: context);}, 
                    icon: const Image(
                      image: AssetImage('assets/logo.png'),
                    )
                  )
                ],
              ),
              const SizedBox(height: 20),
              GestureDetector(
                onTap: () => print("Card clicked"),
                child: Card(
                  elevation: 10,
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(15)
                  ),
                  //color: const Color.fromRGBO(167, 196, 188, 1),
                  child: Padding(
                    padding: const EdgeInsets.all(12.0),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          'Chapter 3',
                          style: GoogleFonts.darkerGrotesque(
                            fontSize: 36,
                            fontWeight: FontWeight.bold,
                            //color: const Color.fromRGBO(53, 51, 58, 1),
                          ),
                        ),
                        Text(
                          'Continue where you left',
                          style: GoogleFonts.darkerGrotesque(
                            fontSize: 26,
                            fontWeight: FontWeight.w600,
                            //color: const Color.fromRGBO(53, 51, 58, 1),
                          ),
                        ),
                        LinearPercentIndicator(
                          animation: true,
                          animationDuration: 500,
                          lineHeight: 16,
                          //progressColor: const Color.fromRGBO(98, 117, 127, 1),
                          //backgroundColor: const Color.fromRGBO(198, 219, 210, 1),
                          percent: 0.6,
                          padding: const EdgeInsets.only(top: 4, left: 6),
                          barRadius: const Radius.circular(8),
                          leading: Text(
                            '60%',
                            style: GoogleFonts.darkerGrotesque(
                              fontSize: 24,
                              fontWeight: FontWeight.w600,
                              //color: const Color.fromRGBO(53, 51, 58, 1),
                            ),
                          ),
                        )
                      ],
                    ),
                  ),
                ),
              ),
              const SizedBox(height: 35),
              Text(
                'LOREM LIPSUM',
                style: GoogleFonts.kronaOne(
                  fontSize: 36,
                  fontWeight: FontWeight.w500,
                  //color: const Color.fromRGBO(74, 91, 99, 1),
                ),
              ),
              const SizedBox(height: 30),
              Expanded(
                child: GridView.builder(
                  itemCount: data.length,
                  padding: EdgeInsets.zero,
                  physics: const NeverScrollableScrollPhysics(),
                  shrinkWrap: true,
                  gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                    crossAxisCount: 2,
                    mainAxisSpacing: 14,
                    crossAxisSpacing: 14,
                  ), 
                  itemBuilder: (context, index) {
                    switch (index) {
                      case 0:
                        return GestureDetector(
                          onTap: () => Navigator.pushNamed(context, '/coursepage'),
                          child: Card(
                            elevation: 10,
                            //color: const Color.fromRGBO(98, 117, 127, 1),
                            shape: RoundedRectangleBorder(
                              borderRadius: BorderRadius.circular(20),
                            ),
                            child: Padding(
                              padding: const EdgeInsets.all(8),
                              child: Column(
                                mainAxisAlignment: MainAxisAlignment.spaceAround,
                                children: [
                                  Container(
                                    alignment: Alignment.topLeft,
                                    child: Text(
                                      data[index]['title'],
                                      style: GoogleFonts.darkerGrotesque(
                                        //color: const Color.fromRGBO(230, 242, 232, 1),
                                        fontSize: 28,
                                        fontWeight: FontWeight.w600,
                                        height: 1.1,
                                      ),
                                    ),
                                  ),
                                  Row(
                                    mainAxisAlignment: MainAxisAlignment.spaceAround,
                                    children: [
                                      Text(
                                        '74%',
                                        style: GoogleFonts.darkerGrotesque(
                                          //color: const Color.fromRGBO(230, 242, 232, 1),
                                          fontSize: 28,
                                          fontWeight: FontWeight.w600,
                                        )
                                      ),
                                      CircularPercentIndicator(
                                        animation: true,
                                        animationDuration: 500,
                                        radius: 40,
                                        //progressColor: const Color.fromRGBO(230, 242, 232, 1),
                                        //backgroundColor: Colors.transparent,
                                        percent: 0.7,
                                        center: Image(
                                          image: AssetImage(data[index]['image']),
                                        ),
                                      ),
                                    ],
                                  )
                                ],
                              ),
                            ),
                          ),
                        );
                      default:
                        return GestureDetector(
                          onTap: () => print("Card $index clicked"),
                          child: Card(
                            elevation: 10,
                            //color: const Color.fromRGBO(98, 117, 127, 1),
                            shape: RoundedRectangleBorder(
                              borderRadius: BorderRadius.circular(20),
                            ),
                            child: Padding(
                              padding: const EdgeInsets.all(10),
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(
                                    data[index]['title'],
                                    style: GoogleFonts.darkerGrotesque(
                                      //color: const Color.fromRGBO(230, 242, 232, 1),
                                      fontSize: 28,
                                      fontWeight: FontWeight.w600,
                                      height: 1.1,
                                    ),
                                  ),
                                  Container(
                                    alignment: Alignment.bottomRight,
                                    child: Image(
                                      image: AssetImage(data[index]['image'])
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          ),
                        );
                    }
                  }
                ),
              ),
            ],
          ),
        ),
        bottomNavigationBar: Padding(
          padding: const EdgeInsets.only(bottom: 14),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceAround,
            children: [
              IconButton(
                onPressed: () => Navigator.pushNamed(context, '/home'), 
                icon: const Icon(
                  Icons.home_filled,
                  size: 40,
                  //color: Color.fromRGBO(53, 51, 58, 1),
                )
              ),
              IconButton(
                onPressed: () => Navigator.pushNamed(context, '/coursepage'), 
                icon: const Icon(
                  Icons.bar_chart_rounded,
                  size: 40,
                  //color: Color.fromRGBO(53, 51, 58, 1),
                )
              ),
              IconButton(
                onPressed: () => Navigator.pushNamed(context, '/chatbot'), 
                icon: const Icon(
                  Icons.chat_rounded,
                  size: 40,
                  //color: Color.fromRGBO(53, 51, 58, 1),
                )
              )
            ],
          ),
        ),
      ),
    );
  }
}