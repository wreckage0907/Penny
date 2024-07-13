import 'package:flutter/material.dart';
import 'package:font_awesome_flutter/font_awesome_flutter.dart';
import 'package:google_fonts/google_fonts.dart';
import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:mobile/Pages/coursePage/learning_page.dart';

extension StringExtension on String {
  String capitalize() {
    return "${this[0].toUpperCase()}${this.substring(1).toLowerCase()}";
  }
}

class CoursePage extends StatefulWidget {
  CoursePage({super.key});

  @override
  State<CoursePage> createState() => _CoursePageState();
}

class _CoursePageState extends State<CoursePage> {
  List<String> modulesList = [];

  @override
  void initState() {
    super.initState();
    fetchFolderNames();
  }

  Future<void> fetchFolderNames() async {
    try {
      final response =
          await http.get(Uri.parse('http://10.0.2.2:8000/subfolder'));
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
        });
      } else {
        throw Exception('Failed to load folder names');
      }
    } catch (e) {
      print('Error: $e');
    }
  }

  String getModuleNames(String name) {
    final parts = name.split('_');
    return parts.sublist(1).map((part) => part.capitalize()).join(' ');
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
        appBar: AppBar(),
        body: Padding(
          padding: const EdgeInsets.all(20),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Image.asset("assets/modules1.png"),
              const SizedBox(height: 10),
              Container(
                alignment: Alignment.centerLeft,
                child: const Text(
                  "Modules",
                  style: TextStyle(
                    fontWeight: FontWeight.bold,
                    fontSize: 28,
                  ),
                ),
              ),
              const SizedBox(height: 20),
              Expanded(
                child: ListView.separated(
                    padding: const EdgeInsets.all(0),
                    itemBuilder: (context, int index) {
                      return SizedBox(
                        height: 70,
                        child: ElevatedButton(
                            style: ElevatedButton.styleFrom(
                              backgroundColor:
                                  const Color.fromRGBO(210, 211, 219, 1),
                              shape: RoundedRectangleBorder(
                                  borderRadius: BorderRadius.circular(15)),
                            ),
                            onPressed: () {
                              Navigator.push(
                                  context,
                                  MaterialPageRoute(
                                    builder: (context) => LearningPage(
                                      moduleName:
                                          getModuleNames(modulesList[index]),
                                      subfolder: modulesList[index],
                                    ),
                                  ));
                            },
                            child: Row(
                              children: [
                                const IconButton(
                                    onPressed: null,
                                    icon: FaIcon(
                                      FontAwesomeIcons.circlePlay,
                                      size: 32,
                                      color: Color.fromRGBO(72, 75, 106, 1),
                                    )),
                                Expanded(
                                  child: Text(
                                    getModuleNames(modulesList[index]),
                                    style: GoogleFonts.darkerGrotesque(
                                      fontSize: 30,
                                      fontWeight: FontWeight.w600,
                                      color: Colors.black,
                                    ),
                                    overflow: TextOverflow.ellipsis,
                                  ),
                                ),
                              ],
                            )),
                      );
                    },
                    separatorBuilder: (context, int index) => const Divider(
                          color: Colors.transparent,
                          height: 15,
                        ),
                    itemCount: modulesList.length),
              )
            ],
          ),
        ));
  }
}
