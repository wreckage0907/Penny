import 'package:flutter/material.dart';
import 'package:flutter/widgets.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:mobile/consts/app_colours.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'dart:convert';
import 'package:http/http.dart' as http;

class ProfileSettings extends StatefulWidget {
  const ProfileSettings({super.key});

  @override
  State<ProfileSettings> createState() => _ProfileSettingsState();
}

class _ProfileSettingsState extends State<ProfileSettings> {
  String? username;
  String? profileImageUrl;
  Map<String, String?>? userData;

  @override
  void initState() {
    super.initState();
    _loadUsername();
  }

  Future<void> _loadUsername() async {
    final prefs = await SharedPreferences.getInstance();
    setState(() {
      username = prefs.getString('username');
    });
    if (username != null) {
      _loadProfileImage();
      _loadUserData();
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

  Future<void> _loadUserData() async {
    if (username != null) {
      try {
        userData = await getUserData(username!);
        setState(() {});
      } catch (e) {
        print("Error loading user data: $e");
      }
    }
  }

  Future<Map<String, String?>> getUserData(String userId) async {
    try {
      final response = await http.get(
        Uri.parse('https://penny-4jam.onrender.com/user').replace(
          queryParameters: {'user_id': userId},
        ),
      );

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        if (data['user'] != null) {
          return {
            'firstName': data['user']['firstName'][0],
            'lastName': data['user']['lastName'][0],
            'email': data['user']['email'][0],
            'phone': data['user']['phone'][0],
          };
        } else {
          print('User data not found in response');
          return {
            'firstName': null,
            'lastName': null,
            'email': null,
            'phone': null,
          };
        }
      } else if (response.statusCode == 404) {
        print('User not found');
        return {
          'firstName': null,
          'lastName': null,
          'email': null,
          'phone': null,
        };
      } else {
        print('Error: ${response.statusCode}');
        return {
          'firstName': null,
          'lastName': null,
          'email': null,
          'phone': null,
        };
      }
    } catch (e) {
      print('Error retrieving user data: $e');
      return {
        'firstName': null,
        'lastName': null,
        'email': null,
        'phone': null,
      };
    }
  }

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
              CircleAvatar(
                radius: 50,
                backgroundColor: Colors.transparent,
                backgroundImage: profileImageUrl != null
                    ? NetworkImage(profileImageUrl!)
                    : const AssetImage('assets/home_logo.png') as ImageProvider,
              ),
              const SizedBox(height: 20),
              Text(
                "Username",
                style: GoogleFonts.dmSans(
                    fontSize: 18, fontWeight: FontWeight.w500),
              ),
              const SizedBox(height: 10),
              TextField(
                readOnly: true,
                controller: TextEditingController(
                  text: username ?? ' ',
                ),
                decoration: const InputDecoration(
                    border: OutlineInputBorder(),
                    labelStyle: TextStyle(color: AppColours.textColor)),
              ),
              const SizedBox(height: 20),
              Text(
                "First Name",
                style: GoogleFonts.dmSans(
                    fontSize: 18, fontWeight: FontWeight.w500),
              ),
              const SizedBox(height: 10),
              TextField(
                readOnly: true,
                controller: TextEditingController(
                  text: userData?['firstName'] ?? '',
                ),
                decoration: const InputDecoration(
                    border: OutlineInputBorder(),
                    labelStyle: TextStyle(color: AppColours.textColor)),
              ),
              const SizedBox(height: 20),
              Text(
                "Last Name",
                style: GoogleFonts.dmSans(
                    fontSize: 18, fontWeight: FontWeight.w500),
              ),
              const SizedBox(height: 10),
              TextField(
                readOnly: true,
                controller: TextEditingController(
                  text: userData?['lastName'] ?? '',
                ),
                decoration: const InputDecoration(
                    border: OutlineInputBorder(),
                    labelStyle: TextStyle(color: AppColours.textColor)),
              ),
              const SizedBox(height: 20),
              Text(
                "Email ID",
                style: GoogleFonts.dmSans(
                    fontSize: 18, fontWeight: FontWeight.w500),
              ),
              const SizedBox(height: 10),
              TextField(
                readOnly: true,
                controller: TextEditingController(
                  text: userData?['email'] ?? '',
                ),
                decoration: const InputDecoration(
                    border: OutlineInputBorder(),
                    labelStyle: TextStyle(color: AppColours.textColor)),
              ),
              const SizedBox(height: 20),
              Text(
                "Phone Number",
                style: GoogleFonts.dmSans(
                    fontSize: 18, fontWeight: FontWeight.w500),
              ),
              const SizedBox(height: 10),
              TextField(
                readOnly: true,
                controller: TextEditingController(
                  text: userData?['phone'] ?? '',
                ),
                decoration: const InputDecoration(
                    border: OutlineInputBorder(),
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
                icon: const Icon(Icons.home_outlined,
                    size: 40, color: AppColours.textColor)),
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
