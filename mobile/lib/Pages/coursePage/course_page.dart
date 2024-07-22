import 'package:flutter/material.dart';
import 'package:font_awesome_flutter/font_awesome_flutter.dart';
import 'package:google_fonts/google_fonts.dart';
import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:mobile/Pages/coursePage/learning_page.dart';
import 'package:mobile/consts/app_colours.dart';
import 'package:skeletonizer/skeletonizer.dart';

extension StringExtension on String {
  String capitalize() {
    return "${this[0].toUpperCase()}${this.substring(1).toLowerCase()}";
  }
}

class CoursePage extends StatefulWidget {
  const CoursePage({super.key});

  @override
  State<CoursePage> createState() => _CoursePageState();
}

class _CoursePageState extends State<CoursePage> {
  List<String> modulesList = [];
    bool isLoading = true;


  @override
  void initState() {
    super.initState();
    fetchFolderNames();
  }

Future<void> fetchFolderNames() async {
  try {
    final response =
        await http.get(Uri.parse('https://penny-uts7.onrender.com/subfolder'));
    if (response.statusCode == 200) {
      final data = json.decode(response.body);
      setState(() {
        modulesList = List<String>.from(data['title'])
            .where((name) => name.isNotEmpty)
            .toList();
        modulesList.sort((a, b) {
          int aNum = int.tryParse(a.split('_')[0]) ?? 0;
          int bNum = int.tryParse(b.split('_')[0]) ?? 0;
          return aNum.compareTo(bNum);
        });
        isLoading = false; // Add this line
      });
    } else {
      throw Exception('Failed to load folder names');
    }
  } catch (e) {
    print('Error: $e');
    setState(() {
      isLoading = false;
    });
  }
}

  String getModuleNames(String name) {
    final parts = name.split('_');
    return parts.sublist(1).map((part) => part.capitalize()).join(' ');
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
        backgroundColor: AppColours.backgroundColor,
        appBar: AppBar(
          backgroundColor: AppColours.backgroundColor,
        ),
        body: Padding(
          padding: const EdgeInsets.all(20),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              SizedBox(
                width: MediaQuery.of(context).size.width,
                height: 250,
                child: Image.asset(
                  "assets/modules1.png",
                  fit: BoxFit.cover,
                ),
              ),
              const SizedBox(height: 20),
              Container(
                alignment: Alignment.centerLeft,
                child: const Text(
                  "Modules",
                  style: TextStyle(
                    fontWeight: FontWeight.bold,
                    fontSize: 28,
                    color: AppColours.textColor,
                  ),
                ),
              ),
              const SizedBox(height: 20),
              Expanded(
            child: Skeletonizer(
              effect: ShimmerEffect(
                baseColor: AppColours.buttonColor.withOpacity(0.2),
                highlightColor: AppColours.buttonColor.withOpacity(0.3),
              ),
              enabled: isLoading,
              ignoreContainers: true,
              child: ListView.separated(
                padding: const EdgeInsets.all(0),
                itemBuilder: (context, int index) {
                  return SizedBox(
                    height: 70,
                    child: ElevatedButton(
                      style: ElevatedButton.styleFrom(
                        backgroundColor: AppColours.buttonColor,
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(15),
                        ),
                      ),
                      onPressed: isLoading ? null : () {
                        Navigator.push(
                          context,
                          MaterialPageRoute(
                            builder: (context) => LearningPage(
                              moduleName: getModuleNames(modulesList[index]),
                              subfolder: modulesList[index],
                            ),
                          ),
                        );
                      },
                      child: Center(
                        child: Row(
                          mainAxisAlignment: MainAxisAlignment.start,
                          crossAxisAlignment: CrossAxisAlignment.center,
                          children: [
                            const IconButton(
                              onPressed: null,
                              icon: FaIcon(
                                FontAwesomeIcons.circlePlay,
                                size: 32,
                                color: AppColours.backgroundColor,
                              ),
                            ),
                            const SizedBox(width: 8),
                            Expanded(
                              child: Transform.translate(
                                offset: const Offset(0, -3),
                                child: Text(
                                  isLoading ? 'Module Name' : getModuleNames(modulesList[index]),
                                  style: GoogleFonts.darkerGrotesque(
                                    fontSize: 30,
                                    fontWeight: FontWeight.w600,
                                    color: AppColours.backgroundColor,
                                  ),
                                  overflow: TextOverflow.ellipsis,
                                ),
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  );
                },
                separatorBuilder: (context, int index) => const Divider(
                  color: Colors.transparent,
                  height: 15,
                ),
                itemCount: isLoading ? 5 : modulesList.length,),
              )),
            ],
          ),
        ));
  }
}
