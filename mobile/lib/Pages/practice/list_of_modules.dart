import 'package:flutter/material.dart';
import 'package:font_awesome_flutter/font_awesome_flutter.dart';
import 'package:google_fonts/google_fonts.dart';

class PracticeList extends StatelessWidget {
  PracticeList({super.key});

  final List<String> finishedModules = [
    "Basics of Personal Finance",
    "Banking and Accounts",
    "Credit and Debt Management",
    "Saving and Investing",
  ];
  final List<String> unfinishedModules = [
    "Insurance and Risk Management",
    "Real Estate and Home Management",
    "Financial Planning and Taxes",
    "Consumer Awarness",
    "Enterprenuership and Business Finance",
    "Advance Personal Finance"
  ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      //backgroundColor: const Color.fromRGBO(230, 242, 232, 1),
      appBar: AppBar(
        toolbarHeight: 70,
        title: Text(
          'Practice',
          style: GoogleFonts.dmSans(
            fontSize: 35,
            fontWeight: FontWeight.w800,
          ),
        ),
        //backgroundColor: const Color.fromRGBO(230, 242, 232, 1),
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
                                    color: Colors.black,
                                  ),
                                  overflow: TextOverflow.ellipsis,
                                ),
                              ),
                              const FaIcon(
                                FontAwesomeIcons.chevronRight,
                                color: Colors.black,
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
                        child: Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            Expanded(
                              child: Text(
                                unfinishedModules[position],
                                style: GoogleFonts.dmSans(
                                  fontSize: 20,
                                  fontWeight: FontWeight.w800,
                                  color: Colors.grey,                          
                                ),
                                overflow: TextOverflow.ellipsis,
                              ),
                            ),
                            const Icon(
                              Icons.lock,
                              color: Colors.black38,
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
