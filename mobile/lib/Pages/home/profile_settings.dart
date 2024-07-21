import 'package:cached_network_image/cached_network_image.dart';
import 'package:firebase_auth/firebase_auth.dart';
import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:mobile/Services/auth.dart';
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
  final Auth _authService = Auth();
  String? username;
  String? fullName;
  String? profileImageUrl;
  Map<String, String?>? userData;

  final TextEditingController _currentPasswordController =
      TextEditingController();
  final TextEditingController _newPasswordController = TextEditingController();
  final TextEditingController _confirmNewPasswordController =
      TextEditingController();

  @override
  void initState() {
    super.initState();
    _loadUsername();
  }

  Future<void> _loadUsername() async {
    final prefs = await SharedPreferences.getInstance();
    setState(() {
      username = prefs.getString('username');
      fullName = prefs.getString('fullName');
    });
    if (username != null) {
      _loadProfileImage();
    }
  }

  Future<void> _loadProfileImage() async {
    if (username != null) {
      try {
        final response = await http.get(
          Uri.parse('http://10.0.2.2:8000/prof/$username'),
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

  Future<void> _changePassword() async {
    if (_newPasswordController.text != _confirmNewPasswordController.text) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('New passwords do not match')),
      );
      return;
    }

    try {
      User? user = FirebaseAuth.instance.currentUser;

      if (user != null) {
        AuthCredential credential = EmailAuthProvider.credential(
          email: user.email!,
          password: _currentPasswordController.text,
        );

        await user.reauthenticateWithCredential(credential);

        await user.updatePassword(_newPasswordController.text);

        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Password changed successfully')),
        );
        Navigator.of(context).pop();
      } else {
        throw Exception('No user currently signed in');
      }
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Failed to change password: ${e.toString()}')),
      );
    }
  }

  Future<void> _deleteAccount() async {
    try {
      User? user = FirebaseAuth.instance.currentUser;

      if (user != null && username != null) {
        await user.delete();

        final response = await http.delete(
          Uri.parse('http://10.0.2.2:8000/onboarding/$username'),
        );

        if (response.statusCode == 200) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Account deleted successfully')),
          );
          await _authService.signout(context: context);
          final prefs = await SharedPreferences.getInstance();
          await prefs.clear();
          Navigator.of(context).pushReplacementNamed('/login');
        } else {
          throw Exception('Failed to delete account on server');
        }
      } else {
        throw Exception('No user currently signed in');
      }
    } catch (e) {
      print("Error deleting account: $e");
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('An error occurred while deleting account: $e')),
      );
    }
  }

  void _showChangePasswordDialog() {
    showDialog(
      context: context,
      builder: (BuildContext context) {
        return AlertDialog(
          title: Text('Change Password'),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextField(
                controller: _currentPasswordController,
                obscureText: true,
                decoration: InputDecoration(labelText: 'Current Password'),
              ),
              TextField(
                controller: _newPasswordController,
                obscureText: true,
                decoration: InputDecoration(labelText: 'New Password'),
              ),
              TextField(
                controller: _confirmNewPasswordController,
                obscureText: true,
                decoration: InputDecoration(labelText: 'Confirm New Password'),
              ),
            ],
          ),
          actions: [
            TextButton(
              child: Text('Cancel'),
              onPressed: () {
                Navigator.of(context).pop();
              },
            ),
            TextButton(
              child: Text('Change'),
              onPressed: _changePassword,
            ),
          ],
        );
      },
    );
  }

  void _showDeleteAccountDialog() {
    showDialog(
      context: context,
      builder: (BuildContext context) {
        return AlertDialog(
          title: Text('Delete Account'),
          content: Text(
              'Are you sure you want to delete your account? This action cannot be undone.'),
          actions: [
            TextButton(
              child: Text('Cancel'),
              onPressed: () {
                Navigator.of(context).pop();
              },
            ),
            TextButton(
              child: Text('Delete', style: TextStyle(color: Colors.red)),
              onPressed: () {
                Navigator.of(context).pop();
                _deleteAccount();
              },
            ),
          ],
        );
      },
    );
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
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  profileImageUrl != null
                      ? CachedNetworkImage(
                          imageUrl: profileImageUrl!,
                          imageBuilder: (context, imageProvider) =>
                              CircleAvatar(
                            backgroundImage: imageProvider,
                            radius: 40,
                          ),
                          placeholder: (context, url) =>
                              const CircularProgressIndicator(),
                          errorWidget: (context, url, error) {
                            print("Error loading image: $error");
                            return const Icon(Icons.error);
                          },
                        )
                      : const CircleAvatar(
                          radius: 40,
                          backgroundColor: AppColours.buttonColor,
                          child: Icon(Icons.person,
                              color: AppColours.backgroundColor, size: 45),
                        ),
                  const SizedBox(width: 15),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          fullName ?? '',
                          style: GoogleFonts.dmSans(
                            fontSize: 32,
                            fontWeight: FontWeight.bold,
                            color: AppColours.textColor,
                          ),
                          overflow: TextOverflow.ellipsis,
                        ),
                        Text(
                          username ?? '',
                          style: GoogleFonts.dmSans(
                            fontSize: 26,
                            fontWeight: FontWeight.w600,
                            color: AppColours.textColor.withOpacity(0.7),
                          ),
                        ),
                      ],
                    ),
                  )
                ],
              ),
              const SizedBox(height: 30),
              ElevatedButton(
                  onPressed: () => Navigator.pushNamed(context, '/editprofile'),
                  style: ElevatedButton.styleFrom(
                      backgroundColor: AppColours.buttonColor,
                      shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(10)),
                      padding: const EdgeInsets.symmetric(
                          horizontal: 20, vertical: 8)),
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Text(
                        "Edit Profile",
                        style: GoogleFonts.dmSans(
                            fontSize: 28,
                            fontWeight: FontWeight.w500,
                            color: AppColours.backgroundColor),
                      ),
                    ],
                  )),
              const SizedBox(height: 30),
              ElevatedButton(
                  onPressed: _showChangePasswordDialog,
                  style: ElevatedButton.styleFrom(
                      backgroundColor: AppColours.buttonColor,
                      shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(10)),
                      padding: const EdgeInsets.symmetric(
                          horizontal: 20, vertical: 8)),
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Text(
                        "Change Password",
                        style: GoogleFonts.dmSans(
                            fontSize: 28,
                            fontWeight: FontWeight.w500,
                            color: AppColours.backgroundColor),
                      ),
                    ],
                  )),
              const SizedBox(height: 30),
              ElevatedButton(
                  onPressed: () async {
                    _authService.signout(context: context);
                  },
                  style: ElevatedButton.styleFrom(
                      backgroundColor: AppColours.buttonColor,
                      shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(10)),
                      padding: const EdgeInsets.symmetric(
                          horizontal: 20, vertical: 8)),
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Text(
                        "Sign Out",
                        style: GoogleFonts.dmSans(
                            fontSize: 28,
                            fontWeight: FontWeight.w500,
                            color: AppColours.backgroundColor),
                      ),
                    ],
                  )),
              const SizedBox(height: 30),
              ElevatedButton(
                  onPressed: _showDeleteAccountDialog,
                  style: ElevatedButton.styleFrom(
                      backgroundColor: AppColours.buttonColor,
                      shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(10)),
                      padding: const EdgeInsets.symmetric(
                          horizontal: 20, vertical: 8)),
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Text(
                        "Delete Account",
                        style: GoogleFonts.dmSans(
                            fontSize: 28,
                            fontWeight: FontWeight.w500,
                            color: AppColours.backgroundColor),
                      ),
                    ],
                  ))
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
            const IconButton(
                onPressed: null,
                icon: Icon(
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
