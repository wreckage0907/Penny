import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:mobile/consts/app_colours.dart';

class ProfileSettings extends StatelessWidget {
  const ProfileSettings({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColours.backgroundColor,
      appBar: AppBar(
          backgroundColor: AppColours.backgroundColor,
          title: const Text("Profile Info")),
      body: SingleChildScrollView(
        child: Padding(
          padding: const EdgeInsets.all(20),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const CircleAvatar(
                radius: 50,
                backgroundColor: Colors.transparent,
                backgroundImage: AssetImage('assets/home_logo.png'),
              ),
              const SizedBox(height: 20),
              Text("Username",
              style: GoogleFonts.dmSans(
                fontSize: 18,
                fontWeight: FontWeight.w500
              ),),
              const SizedBox(height: 10),
              const TextField(
                decoration: const InputDecoration(
                    border: OutlineInputBorder(),
                    labelText: "Username",
                    labelStyle: TextStyle(color: AppColours.textColor)),
              ),
              const SizedBox(height: 20),
              Text("First Name",
              style: GoogleFonts.dmSans(
                fontSize: 18,
                fontWeight: FontWeight.w500
              ),),
              const SizedBox(height: 10),
              const TextField(
                decoration: const InputDecoration(
                    border: OutlineInputBorder(),
                    labelText: "First Name",
                    labelStyle: TextStyle(color: AppColours.textColor)),
              ),
              const SizedBox(height: 20),
              Text("Last Name",
              style: GoogleFonts.dmSans(
                fontSize: 18,
                fontWeight: FontWeight.w500
              ),),
              const SizedBox(height: 10),
              const TextField(
                decoration: const InputDecoration(
                    border: OutlineInputBorder(),
                    labelText: "Last Name",
                    labelStyle: TextStyle(color: AppColours.textColor)),
              ),
              const SizedBox(height: 20),
              Text("Email ID",
              style: GoogleFonts.dmSans(
                fontSize: 18,
                fontWeight: FontWeight.w500
              ),),
              const SizedBox(height: 10),
              const TextField(
                decoration: const InputDecoration(
                    border: OutlineInputBorder(),
                    labelText: "Email",
                    labelStyle: TextStyle(color: AppColours.textColor)),
              ),
              const SizedBox(height: 20),
              Text("Phone Number",
              style: GoogleFonts.dmSans(
                fontSize: 18,
                fontWeight: FontWeight.w500
              ),),
              const SizedBox(height: 10),
              const TextField(
                decoration: const InputDecoration(
                    border: OutlineInputBorder(),
                    labelText: "Phone No.",
                    labelStyle: TextStyle(color: AppColours.textColor)),
              ),
            ],
          ),
        ),
      ),
      bottomNavigationBar: Padding(
        padding: const EdgeInsets.only(bottom: 14),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.spaceAround,
          children: [
            IconButton(
                onPressed: () => Navigator.pushNamed(context, '/home'),
                icon: const Icon(Icons.home_outlined, size: 40, color: AppColours.textColor)),
            IconButton(
                onPressed: () => Navigator.pushNamed(context, '/chatbot'),
                icon: const Icon(
                  Icons.chat_outlined,
                  size: 38,
                  color: AppColours.textColor,
                )),
            IconButton(
                onPressed: () =>
                    Navigator.pushNamed(context, '/profilesettings'),
                icon: const Icon(
                  Icons.person_rounded,
                  size: 40,
                  color: AppColours.textColor,
                )),
          ],
        ),
      ),
    );
  }
}
