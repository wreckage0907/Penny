//INITIAL IMPORTS
import 'dart:async';

import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:google_fonts/google_fonts.dart';

// PAGES IMPORT
import 'package:mobile/Pages/chatbot/chatbot_page.dart';
import 'package:mobile/Pages/coursePage/course_page.dart';
import 'package:mobile/Pages/expenseTracker/expense_tracker.dart';
import 'package:mobile/Pages/home/onboarding_page.dart';
import 'package:mobile/Pages/practice/list_of_modules.dart';
import 'package:mobile/Pages/stocks/stock_profile.dart';
import 'package:mobile/Services/auth.dart';
import 'package:mobile/app_colours.dart';

// EXTERNAL IMPORTS
import 'package:percent_indicator/circular_percent_indicator.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:smooth_page_indicator/smooth_page_indicator.dart';
import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:url_launcher/url_launcher.dart';

class Home extends StatefulWidget {
  const Home({super.key});

  @override
  State<Home> createState() => _HomeState();
}

class _HomeState extends State<Home> {
  final Auth _authService = Auth();
  String? fullName;
  String? username;
  String? profileImageUrl;
  Timer? _timer;

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
    _loadInitialData();
    readJson();
    _timer = Timer.periodic(const Duration(minutes: 50), (_) => _loadProfileImage());
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }

  Future<void> _loadInitialData() async {
    await _loadUserData();
    if (mounted) {
      setState(() {});
    }
  }

  Future<void> _loadUserData() async {
    final prefs = await SharedPreferences.getInstance();
    setState(() {
      username = prefs.getString('username');
      fullName = prefs.getString('fullName');
    });

    if (username != null) {
      await _loadProfileImage();
    }
  }

  Future<void> _loadProfileImage() async {
      if (username != null) {
        try {
          final response = await http.get(
            Uri.parse('https://penny-4jam.onrender.com/prof/$username'),
          );

          if (response.statusCode == 200) {
            final data = json.decode(response.body);
            if (mounted) {
              setState(() {
                profileImageUrl = data['url'];
              });
            }
          } else {
            print("Error loading profile image: ${response.statusCode}");
          }
        } catch (e) {
          print("Error loading profile image: $e");
        }
      }
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
          color: AppColours.buttonColor,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(20),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              ClipRRect(
                borderRadius:
                    const BorderRadius.vertical(top: Radius.circular(20)),
                child: Image.network(
                  articles[index]['urlToImage'] ??
                      'https://via.placeholder.com/300x200',
                  fit: BoxFit.cover,
                  height: 160,
                  width: double.infinity,
                  errorBuilder: (context, error, stackTrace) {
                    return Container(
                      height: 150,
                      color: AppColours.buttonColor,
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
                          fontSize: 16,
                          fontWeight: FontWeight.bold,
                          color: AppColours.backgroundColor,
                        ),
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis,
                      ),
                      const SizedBox(height: 4),
                      Expanded(
                        child: Text(
                          articles[index]['description'] ?? 'No description',
                          style: const TextStyle(
                            fontSize: 12,
                            color: AppColours.backgroundColor,
                          ),
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
        '/coursepage': (context) => const CoursePage(),
        '/chatbot': (context) => const ChatbotPage(),
        '/budget': (context) => const ExpenseTracker(),
        '/practice': (context) => PracticeList(),
        '/onboarding': (context) => const OnboardingPage(),
        '/stockprofile': (context) => const StockProfile(),
      },
      home: Scaffold(
        backgroundColor: AppColours.backgroundColor,
        body: SafeArea(
          child: SingleChildScrollView(
            child: Padding(
              padding: const EdgeInsets.all(20.0),
              child: Column(
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Expanded(
                        child: Text(
                          'WELCOME,\n$fullName',
                          style: GoogleFonts.dmSans(
                            fontSize: 34,
                            fontWeight: FontWeight.w500,
                            color: AppColours.textColor,
                            height: 1.2,
                          ),
                          overflow: TextOverflow.ellipsis,
                        ),
                      ),
                      IconButton(
                        onPressed: () async {
                          _authService.signout(context: context);
                        },
                        icon: profileImageUrl != null
                            ? CachedNetworkImage(
                                imageUrl: profileImageUrl!,
                                imageBuilder: (context, imageProvider) =>
                                    CircleAvatar(
                                  backgroundImage: imageProvider,
                                  radius: 25,
                                ),
                                placeholder: (context, url) =>
                                    const CircularProgressIndicator(),
                                errorWidget: (context, url, error) {
                                  print("Error loading image: $error");
                                  return const Icon(Icons.error);
                                },
                              )
                            : const Image(
                                image: AssetImage('assets/home_logo.png'),
                                width: 40,
                                height: 40,
                              ),
                      )
                    ],
                  ),
                  const SizedBox(height: 15),
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
                              onTap: () =>
                                  Navigator.pushNamed(context, '/coursepage'),
                              child: Card(
                                color: AppColours.cardColor,
                                elevation: 10,
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
                                          style: GoogleFonts.darkerGrotesque(
                                            color: AppColours.textColor,
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
                                              style:
                                                  GoogleFonts.darkerGrotesque(
                                                color: AppColours.textColor,
                                                fontSize: 28,
                                                fontWeight: FontWeight.w600,
                                              )),
                                          CircularPercentIndicator(
                                            animation: true,
                                            animationDuration: 500,
                                            radius: 40,
                                            progressColor:
                                                AppColours.buttonColor,
                                            backgroundColor: Colors.transparent,
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
                                  {Navigator.pushNamed(context, '/budget')}
                                else if (index == 2)
                                  {Navigator.pushNamed(context, '/stockbuy')}
                                else
                                  {Navigator.pushNamed(context, '/practice')}
                              },
                              child: Card(
                                elevation: 10,
                                color: AppColours.cardColor,
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
                                        style: GoogleFonts.darkerGrotesque(
                                          color: AppColours.textColor,
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
                  SizedBox(
                    width: double.infinity,
                    child: Text(
                      "Finance News",
                      textAlign: TextAlign.start,
                      style: GoogleFonts.darkerGrotesque(
                        fontSize: 28,
                        fontWeight: FontWeight.w700,
                        color: AppColours.textColor,
                      ),
                    ),
                  ),
                  const SizedBox(height: 8),
                  SizedBox(
                    height: 260,
                    child: PageView(
                        controller: _controller, children: widgetsList),
                  ),
                  const SizedBox(height: 12),
                  SmoothPageIndicator(
                    controller: _controller,
                    count: widgetsList.length,
                    effect: const SwapEffect(
                      activeDotColor: AppColours.buttonColor,
                      dotColor: AppColours.cardColor,
                      dotHeight: 5,
                      dotWidth: 5,
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
        bottomNavigationBar: Padding(
          padding: const EdgeInsets.only(bottom: 14),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceAround,
            children: [
              const IconButton(
                  onPressed: null,
                  icon:
                      Icon(Icons.home, size: 40, color: AppColours.textColor)),
              const IconButton(
                  onPressed: null,
                  icon: Icon(
                    Icons.bar_chart_rounded,
                    size: 40,
                    color: AppColours.textColor,
                  )),
              IconButton(
                  onPressed: () => Navigator.pushNamed(context, '/chatbot'),
                  icon: const Icon(
                    Icons.chat_outlined,
                    size: 40,
                    color: AppColours.textColor,
                  ))
            ],
          ),
        ),
      ),
    );
  }
}
