//INITIAL IMPORTS
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:google_fonts/google_fonts.dart';

// PAGES IMPORT
import 'package:mobile/Pages/chatbot/chatbot_page.dart';
import 'package:mobile/Pages/coursePage/course_page.dart';
import 'package:mobile/Pages/expenseTracker/expense_tracker.dart';
import 'package:mobile/Pages/home/onboarding_page.dart';
import 'package:mobile/Pages/practice/list_of_modules.dart';
import 'package:mobile/Pages/practice/mcqpage.dart';
import 'package:mobile/Services/auth.dart';

// EXTERNAL IMPORTS
import 'package:percent_indicator/circular_percent_indicator.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:smooth_page_indicator/smooth_page_indicator.dart';
import 'dart:convert';
import 'package:url_launcher/url_launcher.dart';
import 'package:http/http.dart' as http;

class Home extends StatefulWidget {
  const Home({super.key});

  @override
  State<Home> createState() => _HomeState();
}

class _HomeState extends State<Home> {
  final Auth _authService = Auth();
  String? username;
  String? name;

  Future<String?> getName(String userId) async {
    try {
      final response = await http.get(
        Uri.parse('http://10.0.2.2:8000/user').replace(
          queryParameters: {'user_id': userId},
        ),
      );

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        if (data['user'] != null &&
            data['user']['firstName'] != null &&
            data['user']['lastName'] != null) {
          return '${data['user']['firstName'][0]} ${data['user']['lastName'][0]}';
        } else {
          print('Name not found in response data');
          return null;
        }
      } else if (response.statusCode == 404) {
        print('User not found');
        return null;
      } else {
        print('Error: ${response.statusCode}');
        return null;
      }
    } catch (e) {
      print('Error retrieving user data: $e');
      return null;
    }
  }

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
    _loadUsername();
    readJson();
  }

  Future<void> _loadUsername() async {
    final prefs = await SharedPreferences.getInstance();
    setState(() {
      username = prefs.getString('username');
    });
  }

  final _controller = PageController();

  List<dynamic> articles = [];
  List<Widget> get widgetsList {
    return List.generate(
      20,
      (index) => GestureDetector(
        onTap: () => launch(articles[index]['url']),
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
                  articles[index]['urlToImage'] ??
                      'https://via.placeholder.com/300x200',
                  fit: BoxFit.cover,
                  height: 160,
                  width: double.infinity,
                  errorBuilder: (context, error, stackTrace) {
                    return Container(
                      height: 150,
                      color: Colors.grey,
                      child: const Center(
                        child: Icon(Icons.error),
                      ),
                    );
                  },
                ),
              ),
              Expanded(
                child: Padding(
                  padding: const EdgeInsets.all(12),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        articles[index]['title'] ?? 'No title',
                        style: const TextStyle(
                            fontSize: 16, fontWeight: FontWeight.bold),
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
        '/chatbot': (context) => const ChatbotPage(),
        '/budget': (context) => ExpenseTracker(),
        '/practice': (context) => PracticeList(),
        '/mcq': (context) => const MCQPage(),
        '/onboarding': (context) => const OnboardingPage(),
      },
      home: Scaffold(
        //backgroundColor: const Color.fromRGBO(232, 245, 233, 1),
        body: SafeArea(
          child: FutureBuilder<String?>(
            future: username != null ? getName(username!) : Future.value(null),
            builder: (context, snapshot) {
              if (snapshot.connectionState == ConnectionState.waiting) {
                return Center(child: CircularProgressIndicator());
              } else if (snapshot.hasError) {
                return Center(child: Text('Error: ${snapshot.error}'));
              } else {
                final name = snapshot.data ?? 'Guest';
                return SingleChildScrollView(
                  child: Padding(
                    padding: const EdgeInsets.all(20.0),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            Expanded(
                              child: Text(
                                'WELCOME,\n$name',
                                style: GoogleFonts.dmSans(
                                  fontSize: 34,
                                  fontWeight: FontWeight.w500,
                                  //color: const Color.fromRGBO(53, 51, 58, 1),
                                  height: 1.2,
                                ),
                                overflow: TextOverflow.ellipsis,
                              ),
                            ),
                            IconButton(
                                onPressed: () async {
                                  _authService.signout(context: context);
                                },
                                icon: const Image(
                                  image: AssetImage('assets/home_logo.png'),
                                ))
                          ],
                        ),
                        const SizedBox(height: 25),
                        GridView.builder(
                            physics: const NeverScrollableScrollPhysics(),
                            itemCount: data.length,
                            padding: EdgeInsets.zero,
                            shrinkWrap: true,
                            gridDelegate:
                                const SliverGridDelegateWithFixedCrossAxisCount(
                              crossAxisCount: 2,
                              mainAxisSpacing: 8,
                              crossAxisSpacing: 8,
                            ),
                            itemBuilder: (context, index) {
                              switch (index) {
                                case 0:
                                  return GestureDetector(
                                    onTap: () => Navigator.pushNamed(
                                        context, '/coursepage'),
                                    child: Card(
                                      //color: const Color.fromRGBO(72, 75, 106, 1),
                                      elevation: 10,
                                      //color: const Color.fromRGBO(98, 117, 127, 1),
                                      shape: RoundedRectangleBorder(
                                        borderRadius: BorderRadius.circular(20),
                                      ),
                                      child: Padding(
                                        padding: const EdgeInsets.all(8),
                                        child: Column(
                                          mainAxisAlignment:
                                              MainAxisAlignment.spaceAround,
                                          children: [
                                            Container(
                                              alignment: Alignment.topLeft,
                                              child: Text(
                                                data[index]['title'],
                                                style:
                                                    GoogleFonts.darkerGrotesque(
                                                  //color: const Color.fromRGBO(250, 250, 250, 1),
                                                  fontSize: 28,
                                                  fontWeight: FontWeight.w600,
                                                  height: 1.1,
                                                ),
                                              ),
                                            ),
                                            Row(
                                              mainAxisAlignment:
                                                  MainAxisAlignment.spaceAround,
                                              children: [
                                                Text('74%',
                                                    style: GoogleFonts
                                                        .darkerGrotesque(
                                                      //color: const Color.fromRGBO(250, 250, 250, 1),
                                                      fontSize: 28,
                                                      fontWeight:
                                                          FontWeight.w600,
                                                    )),
                                                CircularPercentIndicator(
                                                  animation: true,
                                                  animationDuration: 500,
                                                  radius: 40,
                                                  //progressColor: const Color.fromRGBO(230, 242, 232, 1),
                                                  //backgroundColor: Colors.transparent,
                                                  percent: 0.7,
                                                  center: Image(
                                                    image: AssetImage(
                                                        data[index]['image']),
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
                                      if (index == 1)
                                        {
                                          Navigator.pushNamed(
                                              context, '/budget')
                                        }
                                      else if (index == 2)
                                        {Navigator.pushNamed(context, '/mcq')}
                                      else
                                        {
                                          Navigator.pushNamed(
                                              context, '/practice')
                                        }
                                    },
                                    child: Card(
                                      elevation: 10,
                                      //color: Color.fromRGBO(72, 75, 106, 1),
                                      //color: const Color.fromRGBO(98, 117, 127, 1),
                                      shape: RoundedRectangleBorder(
                                        borderRadius: BorderRadius.circular(20),
                                      ),
                                      child: Padding(
                                        padding: const EdgeInsets.all(10),
                                        child: Column(
                                          crossAxisAlignment:
                                              CrossAxisAlignment.start,
                                          children: [
                                            Text(
                                              data[index]['title'],
                                              style:
                                                  GoogleFonts.darkerGrotesque(
                                                //color: const Color.fromRGBO(250, 250, 250, 1),
                                                fontSize: 28,
                                                fontWeight: FontWeight.w600,
                                                height: 1.1,
                                              ),
                                            ),
                                            Container(
                                              alignment: Alignment.bottomRight,
                                              child: Image(
                                                  image: AssetImage(
                                                      data[index]['image'])),
                                            ),
                                          ],
                                        ),
                                      ),
                                    ),
                                  );
                              }
                            }),
                        const SizedBox(height: 20),
                        const Text(
                          'Finance News',
                          style: TextStyle(
                            fontSize: 24,
                            fontWeight: FontWeight.bold,
                            //color: Color.fromRGBO(53, 51, 58, 1),
                          ),
                          textAlign: TextAlign.left,
                        ),
                        Column(
                          crossAxisAlignment: CrossAxisAlignment.center,
                          children: [
                            SizedBox(
                              height: 260,
                              child: PageView(
                                  controller: _controller,
                                  children: widgetsList),
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
                          ],
                        ),
                      ],
                    ),
                  ),
                );
              }
            },
          ),
        ),
        bottomNavigationBar: Padding(
          padding: const EdgeInsets.only(bottom: 14),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceAround,
            children: [
              const IconButton(
                  onPressed: null,
                  icon: Icon(
                    Icons.home_filled,
                    size: 40,
                    //color: Color.fromRGBO(53, 51, 58, 1),
                  )),
              IconButton(
                  onPressed: null,
                  icon: const Icon(
                    Icons.bar_chart_rounded,
                    size: 40,
                    //color: Color.fromRGBO(53, 51, 58, 1),
                  )),
              IconButton(
                  onPressed: () => Navigator.pushNamed(context, '/chatbot'),
                  icon: const Icon(
                    Icons.chat_rounded,
                    size: 40,
                    //color: Color.fromRGBO(53, 51, 58, 1),
                  ))
            ],
          ),
        ),
      ),
    );
  }
}
