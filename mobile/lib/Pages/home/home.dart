import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:mobile/Pages/chatbot/chatbot_page.dart';
import 'package:mobile/Pages/coursePage/course_page.dart';
import 'package:mobile/Pages/expenseTracker/budget.dart';
import 'package:mobile/Pages/coursePage/lesson_page.dart';
import 'package:mobile/Pages/practice/list_of_modules.dart';
import 'package:mobile/Pages/practice/mcqpage.dart';
import 'package:mobile/Services/auth.dart';
import 'package:percent_indicator/circular_percent_indicator.dart';
import 'package:smooth_page_indicator/smooth_page_indicator.dart';
import 'dart:convert';
import 'package:url_launcher/url_launcher.dart';



class Home extends StatefulWidget {
  const Home({super.key});

  @override
  State<Home> createState() => _HomeState();
}

class _HomeState extends State<Home> {
  final Auth _authService = Auth();
  Future<void> readJson() async {
    final String response = await rootBundle.loadString('assets/finance.json');
    final data = await json.decode(response);
    setState(() {
      articles = data['articles'];
    });
  }

  @override
  void initState() {
    super.initState();
    readJson();
  }

  String username = "wreckage";
  final _controller = PageController();
  
  List<dynamic> articles = [];
List<Widget> get widgetsList {
  return List.generate(
    20,
    (index) => GestureDetector(
      onTap: () => launch(articles[index]['url']), // Launch URL on tap
      child: Card(
        elevation: 1.5,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(20),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            ClipRRect(
              borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
              child: Image.network(
                articles[index]['urlToImage'] ?? 'https://via.placeholder.com/300x200',
                fit: BoxFit.cover,
                height: 160,
                width: double.infinity,
                errorBuilder: (context, error, stackTrace) {
                  return Container(
                    height: 150,
                    color: Colors.grey,
                    child: Center(
                      child: Icon(Icons.error),
                    ),
                  );
                },
              ),
            ),
            Expanded(
              child: Padding(
                padding: EdgeInsets.all(12),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      articles[index]['title'] ?? 'No title',
                      style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                    ),
                    SizedBox(height: 4),
                    Expanded(
                      child: Text(
                        articles[index]['description'] ?? 'No description',
                        style: TextStyle(fontSize: 12),
                        overflow: TextOverflow.fade,
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    ),
  );
}

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
        '/coursepage': (context) => CoursePage(),
        '/lesson1': (context) => const LessonPage(
          lessonName: "Lesson 1",
          fileName: "assets/1_1.md",
        ),
        '/chatbot': (context) => const ChatbotPage(),
        '/budget': (context) => const Budget(),
        '/practice': (context) => PracticeList(),
        '/mcq': (context) => const MCQPage(),
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
              SizedBox(
                height: 260,
                child: PageView(
                  controller: _controller,
                  children: widgetsList
                ),
              ),
              const SizedBox(height: 12),
              SmoothPageIndicator(
                controller: _controller, 
                count: widgetsList.length,
                effect: const SwapEffect(
                  activeDotColor: Colors.black87,
                  dotColor: Colors.black38,
                  dotHeight: 5,
                  dotWidth: 5,
                ),
              ),
              const SizedBox(height: 15),
              Expanded(
                child: GridView.builder(
                  itemCount: data.length,
                  padding: EdgeInsets.zero,
                  physics: const NeverScrollableScrollPhysics(),
                  shrinkWrap: true,
                  gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                    crossAxisCount: 2,
                    mainAxisSpacing: 8,
                    crossAxisSpacing: 8,
                  ), 
                  itemBuilder: (context, index) {
                    switch (index) {
                      case 0:
                        return GestureDetector(
                          onTap: () => Navigator.pushNamed(context, '/coursepage'),
                          child: Card(
                            color: Color.fromRGBO(72, 75, 106, 1),
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
                                        color: const Color.fromRGBO(250, 250, 250, 1),
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
                                          color: const Color.fromRGBO(250, 250, 250, 1),
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
                          onTap: () => {
                            if (index == 1) {
                              Navigator.pushNamed(context, '/budget')
                            } else if (index == 2) {
                              Navigator.pushNamed(context, '/mcq')
                            } else {
                              Navigator.pushNamed(context, '/practice')
                            }
                          },
                          child: Card(
                            elevation: 10,
                            color: Color.fromRGBO(72, 75, 106, 1),
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
                                      color: const Color.fromRGBO(250, 250, 250, 1),
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