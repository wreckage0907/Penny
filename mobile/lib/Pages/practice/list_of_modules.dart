import 'package:flutter/material.dart';
import 'package:font_awesome_flutter/font_awesome_flutter.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:mobile/Pages/practice/mcqpage.dart';
import 'package:mobile/consts/app_colours.dart';

class PracticeList extends StatelessWidget {
  PracticeList({super.key});

  final List<String> finishedModules = [
    "Introduction to Personal Finance",
  ];
  final List<String> unfinishedModules = [
    "Insurance and Risk Management",
    "Real Estate and Home Management",
  ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColours.backgroundColor,
      appBar: AppBar(
        backgroundColor: AppColours.backgroundColor,
        toolbarHeight: 70,
        title: Text(
          'Practice',
          style: GoogleFonts.dmSans(
            fontSize: 35,
            fontWeight: FontWeight.w800,
            color: AppColours.textColor,
          ),
        ),
      ),
      body: Padding(
        padding: const EdgeInsets.all(15.0),
        child: Container(
          child: SingleChildScrollView(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  "List of Modules",
                  style: GoogleFonts.dmSans(
                    fontSize: 25,
                    fontWeight: FontWeight.w800,
                    color: AppColours.textColor,
                  ),
                ),
                ListView.builder(
                  shrinkWrap: true,
                  physics: const NeverScrollableScrollPhysics(),
                  itemCount: finishedModules.length,
                  itemBuilder: (BuildContext context, int position) {
                    return TextButton(
                      onPressed: () {
                        Navigator.push(context, MaterialPageRoute(builder: (context) => MCQPage(index: position)));
                      },
                      child: Container(
                        height: 55,
                        decoration: BoxDecoration(
                          color: AppColours.buttonColor,
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
                        child: Padding(
                          padding: const EdgeInsets.all(12),
                          child: Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            children: [
                              Expanded(
                                child: Text(
                                  finishedModules[position],
                                  style: GoogleFonts.dmSans(
                                    fontSize: 20,
                                    fontWeight: FontWeight.w800,
                                    color: AppColours.backgroundColor,
                                  ),
                                  overflow: TextOverflow.ellipsis,
                                ),
                              ),
                              const FaIcon(
                                FontAwesomeIcons.chevronRight,
                                color: AppColours.backgroundColor,
                                size: 18,
                              )
                            ],
                          ),
                        ),
                      ),
                    );
                  },
                ),
                const SizedBox(height: 8),
                const Divider(
                  color: Colors.black12,
                  thickness: 2,
                ),
                ListView.builder(
                  shrinkWrap: true,
                  physics: const NeverScrollableScrollPhysics(),
                  itemCount: unfinishedModules.length,
                  itemBuilder: (BuildContext context, int position) {
                    return TextButton(
                      onPressed: () => print("Print"),
                      child: Container(
                        height: 55,
                        padding: const EdgeInsets.all(10),
                        decoration: BoxDecoration(
                          color: AppColours.cardColor,
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
                        child: Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            Expanded(
                              child: Text(
                                unfinishedModules[position],
                                style: GoogleFonts.dmSans(
                                  fontSize: 20,
                                  fontWeight: FontWeight.w800,
                                  color: Colors.black45,                         
                                ),
                                overflow: TextOverflow.ellipsis,
                              ),
                            ),
                            const Icon(
                              Icons.lock,
                              color: Colors.black45,
                            )
                          ],
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
