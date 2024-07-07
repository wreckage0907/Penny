import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

class PracticeList extends StatelessWidget {
  PracticeList({super.key});

  final List<String> finishedModules = [
    "Introduction to Finance",
    "Lesson 2",
    "Lesson 3",
    "Introduction to Finance",
    "Lesson 2",
    "Lesson 3"
  ];
  final List<String> unfinishedModules = [
    "Lesson 4",
    "Lesson 5",
    "Lesson 6",
    "Introduction to Finance",
    "Lesson 2",
    "Lesson 3"
  ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color.fromRGBO(230, 242, 232, 1),
      appBar: AppBar(
        toolbarHeight: 70,
        title: Text(
          'Practice',
          style: GoogleFonts.dmSans(
            fontSize: 35,
            fontWeight: FontWeight.w800,
          ),
        ),
        backgroundColor: const Color.fromRGBO(230, 242, 232, 1),
      ),
      body: Padding(
        padding: const EdgeInsets.all(15.0),
        child: Container(
          child: SingleChildScrollView(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  "Completed Modules",
                  style: GoogleFonts.dmSans(
                    fontSize: 25,
                    fontWeight: FontWeight.w800,
                  ),
                ),
                ListView.builder(
                  shrinkWrap: true,
                  physics: NeverScrollableScrollPhysics(),
                  itemCount: finishedModules.length,
                  itemBuilder: (BuildContext context, int position) {
                    return TextButton(
                      onPressed: () => print("Print"),
                      child: Container(
                        height: 55,
                        decoration: BoxDecoration(
                          color: Colors.white,
                          borderRadius: BorderRadius.circular(10),
                          boxShadow: [
                            BoxShadow(
                              color: Colors.grey.withOpacity(1),
                              offset: const Offset(2, 2),
                              blurRadius: 2.0,
                              spreadRadius: 2.0,
                            ),
                            BoxShadow(
                              color: Colors.grey.withOpacity(0.5),
                              offset: const Offset(0.0, 0.0),
                              blurRadius: 0.0,
                              spreadRadius: 0.0,
                            ),
                          ],
                        ),
                        child: Center(
                          child: Text(
                            finishedModules[position],
                            style: GoogleFonts.dmSans(
                              fontSize: 20,
                              fontWeight: FontWeight.w800,
                              color: Colors.black,
                            ),
                          ),
                        ),
                      ),
                    );
                  },
                ),
                SizedBox(height: 20),
                Text(
                  "Unfinished Modules",
                  style: GoogleFonts.dmSans(
                    fontSize: 25,
                    fontWeight: FontWeight.w800,
                  ),
                ),
                ListView.builder(
                  shrinkWrap: true,
                  physics: NeverScrollableScrollPhysics(),
                  itemCount: unfinishedModules.length,
                  itemBuilder: (BuildContext context, int position) {
                    return TextButton(
                      onPressed: () => print("Print"),
                      child: Container(
                        height: 55,
                        padding: EdgeInsets.all(10),
                        decoration: BoxDecoration(
                          color: Colors.grey.shade300,
                          borderRadius: BorderRadius.circular(10),
                          boxShadow: [
                            BoxShadow(
                              color: Colors.grey.withOpacity(1),
                              offset: const Offset(2, 2),
                              blurRadius: 2.0,
                              spreadRadius: 2.0,
                            ),
                            BoxShadow(
                              color: Colors.grey.withOpacity(0.5),
                              offset: const Offset(0.0, 0.0),
                              blurRadius: 0.0,
                              spreadRadius: 0.0,
                            ),
                          ],
                        ),
                        child: Center(
                          child: Text(
                            unfinishedModules[position],
                            style: GoogleFonts.dmSans(
                              fontSize: 20,
                              fontWeight: FontWeight.w800,
                              color: Colors.grey,                          
                              ),
                          ),
                        ),
                      ),
                    );
                  },
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
