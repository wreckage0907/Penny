import 'package:flutter/material.dart';
import 'package:font_awesome_flutter/font_awesome_flutter.dart';
import 'package:google_fonts/google_fonts.dart';

class CoursePage extends StatelessWidget {
  CoursePage({super.key});

  final List<String> modulesList = [
    "Introduction to Finance",
    "Introduction to Finance",
    "Introduction to Finance",
    "Introduction to Finance",
    "Introduction to Finance",
    "Introduction to Finance",
    "Introduction to Finance",
    "Introduction to Finance",
  ];

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
                        backgroundColor: const Color.fromRGBO(210, 211, 219, 1),
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(15)
                        ),
                      ),
                      onPressed: null, 
                      child: Row(
                        children: [
                          const IconButton(
                            onPressed: null, 
                            icon: FaIcon(
                              FontAwesomeIcons.circlePlay,
                              size: 32,
                              color: Color.fromRGBO(72, 75, 106, 1),
                            )
                          ),
                          Text(
                            modulesList[index],
                            style: GoogleFonts.darkerGrotesque(
                              fontSize: 32, 
                              fontWeight: FontWeight.w600,
                              color: Colors.black,
                            ),
                          ),
                        ],
                      )
                    ),
                  );
                }, 
                separatorBuilder: (context, int index) => const Divider(
                  color: Colors.transparent,
                  height: 15,
                ), 
                itemCount: modulesList.length
              ),
            )
          ],
        ),
      )
    );
  }
}