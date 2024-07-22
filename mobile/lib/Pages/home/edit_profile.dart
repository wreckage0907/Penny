import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:image_picker/image_picker.dart';
import 'package:mobile/Services/auth.dart';
import 'package:mobile/consts/app_colours.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'dart:convert';
import 'package:http/http.dart' as http;

class EditProfile extends StatefulWidget {
  const EditProfile({super.key});

  @override
  State<EditProfile> createState() => _EditProfileState();
}

class _EditProfileState extends State<EditProfile> {
  String? username;
  String? profileImageUrl;
  Map<String, String?>? userData;
  final Auth _authService = Auth();

  final TextEditingController _firstNameController = TextEditingController();
  final TextEditingController _lastNameController = TextEditingController();
  final TextEditingController _emailController = TextEditingController();
  final TextEditingController _phoneController = TextEditingController();

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

  Future<void> _loadUserData() async {
    if (username != null) {
      try {
        userData = await getUserData(username!);
        setState(() {
          _firstNameController.text = userData?['firstName'] ?? '';
          _lastNameController.text = userData?['lastName'] ?? '';
          _emailController.text = userData?['email'] ?? '';
          _phoneController.text = userData?['phone'] ?? '';
        });
      } catch (e) {
        print("Error loading user data: $e");
      }
    }
  }

  Future<Map<String, String?>> getUserData(String userId) async {
    try {
      final response =
          await http.get(Uri.parse('http://10.0.2.2:8000/onboarding/$userId'));

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        if (data['user'] != null) {
          return {
            'firstName': data['user']['firstName'],
            'lastName': data['user']['lastName'],
            'email': data['user']['email'],
            'phone': data['user']['phone'],
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

  Future<void> _updateUserData() async {
    if (username != null) {
      try {
        final response = await http.put(
          Uri.parse('http://10.0.2.2:8000/onboarding/$username'),
          headers: {'Content-Type': 'application/json'},
          body: json.encode({
            'first_name': _firstNameController.text,
            'last_name': _lastNameController.text,
            'email': _emailController.text,
            'phone': _phoneController.text,
          }),
        );

        if (response.statusCode == 200) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Profile updated successfully')),
          );
          _loadUserData();
        } else {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Failed to update profile')),
          );
        }
      } catch (e) {
        print("Error updating user data: $e");
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
              content: Text('An error occurred while updating profile')),
        );
      }
    }
  }

  Future<void> _updateProfilePicture() async {
    final ImagePicker picker = ImagePicker();
    final XFile? image = await picker.pickImage(source: ImageSource.gallery);

    if (image != null && username != null) {
      try {
        var request = http.MultipartRequest(
          'PUT',
          Uri.parse('http://10.0.2.2:8000/prof/$username'),
        );
        request.files
            .add(await http.MultipartFile.fromPath('file', image.path));

        var response = await request.send();
        if (response.statusCode == 200) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(
                content: Text('Profile picture updated successfully')),
          );
          _loadProfileImage();
        } else {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Failed to update profile picture')),
          );
        }
      } catch (e) {
        print("Error updating profile picture: $e");
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
              content:
                  Text('An error occurred while updating profile picture')),
        );
      }
    }
  }

  Future<void> _deleteProfilePicture() async {
    if (username != null) {
      try {
        final response = await http.delete(
          Uri.parse('http://10.0.2.2:8000/prof/$username'),
        );

        if (response.statusCode == 200) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(
                content: Text('Profile picture deleted successfully')),
          );
          setState(() {
            profileImageUrl = null;
          });
        } else {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Failed to delete profile picture')),
          );
        }
      } catch (e) {
        print("Error deleting profile picture: $e");
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
              content:
                  Text('An error occurred while deleting profile picture')),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColours.backgroundColor,
      appBar: AppBar(
          backgroundColor: AppColours.backgroundColor,
          title: const Text("Edit Profile")),
      body: SingleChildScrollView(
        child: Padding(
          padding: const EdgeInsets.all(20),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Center(
                child: Stack(children: [
                  profileImageUrl != null
                      ? CachedNetworkImage(
                          imageUrl: profileImageUrl!,
                          imageBuilder: (context, imageProvider) =>
                              CircleAvatar(
                            backgroundImage: imageProvider,
                            radius: 50,
                          ),
                          placeholder: (context, url) =>
                              const CircularProgressIndicator(),
                          errorWidget: (context, url, error) {
                            print("Error loading image: $error");
                            return const Icon(Icons.error);
                          },
                        )
                      : const CircleAvatar(
                          radius: 50,
                          backgroundColor: AppColours.buttonColor,
                          child: Icon(Icons.person,
                              color: AppColours.backgroundColor, size: 50),
                        ),
                  Positioned(
                    bottom: 0,
                    right: 0,
                    child: CircleAvatar(
                      backgroundColor: AppColours.cardColor,
                      radius: 18,
                      child: IconButton(
                        icon: const Icon(Icons.edit,
                            size: 18, color: AppColours.textColor),
                        onPressed: _updateProfilePicture,
                      ),
                    ),
                  ),
                ]),
              ),
              const SizedBox(height: 10),
              Center(
                child: TextButton(
                  onPressed: _deleteProfilePicture,
                  child: Text(
                    "Delete Profile Picture",
                    style: GoogleFonts.dmSans(
                      color: Colors.red,
                      fontSize: 16,
                      fontWeight: FontWeight.w500,
                    ),
                  ),
                ),
              ),
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
                controller: _firstNameController,
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
                controller: _lastNameController,
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
                controller: _emailController,
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
                controller: _phoneController,
                decoration: const InputDecoration(
                    border: OutlineInputBorder(),
                    labelStyle: TextStyle(color: AppColours.textColor)),
              ),
              const SizedBox(height: 30),
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 16),
                child: ElevatedButton(
                    onPressed: () async {
                      await _updateUserData();
                      await _authService.saveFullName(
                                '${_firstNameController.text} ${_lastNameController.text}');
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
                          "Save Changes",
                          style: GoogleFonts.dmSans(
                              fontSize: 28,
                              fontWeight: FontWeight.w500,
                              color: AppColours.backgroundColor),
                        ),
                      ],
                    )),
              )
            ],
          ),
        ),
      ),
    );
  }
}
