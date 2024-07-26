import 'dart:io';
import 'dart:convert';
import 'package:firebase_auth/firebase_auth.dart';
import 'package:flutter/material.dart';
import 'package:font_awesome_flutter/font_awesome_flutter.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:http/http.dart' as http;
import 'package:image_picker/image_picker.dart';
import 'package:mobile/Services/auth.dart';
import 'package:mobile/consts/app_colours.dart';
import 'package:path/path.dart' as path;
import 'package:path_provider/path_provider.dart';

class NewUser {
  final String username;
  final String firstName;
  final String lastName;
  final String email;
  final String phoneNo;

  NewUser({
    required this.username,
    required this.firstName,
    required this.lastName,
    required this.email,
    required this.phoneNo,
  });
}

class OnboardingPage extends StatefulWidget {
  final Map<String, dynamic>? googleSignInData;

  const OnboardingPage({Key? key, this.googleSignInData}) : super(key: key);

  @override
  State<OnboardingPage> createState() => _OnboardingPageState();
}

class _OnboardingPageState extends State<OnboardingPage> {
  final Auth _authService = Auth();

  late NewUser user;

  final usernameController = TextEditingController();
  final firstNameController = TextEditingController();
  final lastNameController = TextEditingController();
  final emailController = TextEditingController();
  final phoneNoController = TextEditingController();

  File? _image;
  final picker = ImagePicker();
  
  @override
  void initState() {
    super.initState();
    if (widget.googleSignInData != null) {
      _prefillGoogleData();
    }
  }

  Future getImage() async {
    final pickedFile = await picker.pickImage(source: ImageSource.gallery);

    setState(() {
      if (pickedFile != null) {
        _image = File(pickedFile.path);
      }
    });
  }

  void _prefillGoogleData() async {
    User? googleUser = widget.googleSignInData?['user'];
    if (googleUser != null) {
      String? fullName = googleUser.displayName;
      List<String> nameParts = fullName?.split(' ') ?? ['', ''];
      
      setState(() {
        emailController.text = googleUser.email ?? '';
        firstNameController.text = nameParts.first;
        lastNameController.text = nameParts.length > 1 ? nameParts.last : '';
      });

      if (googleUser.photoURL != null) {
        await _downloadAndSetImage(googleUser.photoURL!);
      }
    }
  }

  Future<void> _downloadAndSetImage(String imageUrl) async {
    try {
      final response = await http.get(Uri.parse(imageUrl));
      if (response.statusCode == 200) {
        final bytes = response.bodyBytes;
        final tempDir = await getTemporaryDirectory();
        final tempImageFile = File('${tempDir.path}/temp_profile_image.jpg');
        await tempImageFile.writeAsBytes(bytes);
        setState(() {
          _image = tempImageFile;
        });
      }
    } catch (e) {
      print('Error downloading Google profile image: $e');
    }
  }


  Future<String?> uploadProfileImage(String userId, File imageFile) async {
    final url = Uri.parse('https://penny-uts7.onrender.com/prof');

    try {
      var request = http.MultipartRequest('POST', url);

      request.fields['user'] = userId;

      var fileStream = http.ByteStream(imageFile.openRead());
      var fileLength = await imageFile.length();

      var multipartFile = http.MultipartFile(
        'file',
        fileStream,
        fileLength,
        filename: path.basename(imageFile.path),
      );

      request.files.add(multipartFile);

      var response = await request.send();

      if (response.statusCode == 200) {
        final responseBody = await response.stream.bytesToString();
        print('Image uploaded successfully: $responseBody');
        final jsonResponse = json.decode(responseBody);
        return jsonResponse['url'];
      } else {
        print('Image upload failed with status: ${response.statusCode}');
        print('Response: ${await response.stream.bytesToString()}');
        return null;
      }
    } catch (e) {
      print('Error uploading image: $e');
      return null;
    }
  }

  @override
  void dispose() {
    usernameController.dispose();
    firstNameController.dispose();
    lastNameController.dispose();
    emailController.dispose();
    phoneNoController.dispose();
    super.dispose();
  }

  Future<void> postNewUserData(NewUser user) async {
    try {
      final Map<String, String> queryParameters = {
        'first_name': user.firstName,
        'last_name': user.lastName,
        'email': user.email,
        'phone': user.phoneNo,
      };

      final uri = Uri.parse(
              'https://penny-uts7.onrender.com/onboarding/${user.username}')
          .replace(
        queryParameters: queryParameters,
      );

      final response = await http.post(
        uri,
        headers: <String, String>{
          'Content-Type': 'application/json',
        },
        body: json.encode(queryParameters),
      );

      if (response.statusCode == 200) {
        print('User data posted successfully');
        print('Response body: ${response.body}');
      } else {
        print('Failed to post user data. Status code: ${response.statusCode}');
        print('Response body: ${response.body}');
        throw Exception('Failed to post user data: ${response.body}');
      }
    } catch (e) {
      print('Error posting user data: $e');
      throw e;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
        backgroundColor: AppColours.backgroundColor,
        body: Center(
          child: SingleChildScrollView(
            child: Padding(
              padding: const EdgeInsets.all(20),
              child: Column(
                children: [
                  Text(
                    "Let's Get Started!",
                    style: GoogleFonts.darkerGrotesque(
                        fontSize: 42,
                        fontWeight: FontWeight.w600,
                        color: AppColours.textColor),
                  ),
                  const SizedBox(height: 20),
                  GestureDetector(
                    onTap: getImage,
                    child: CircleAvatar(
                      radius: 50,
                      backgroundColor: Colors.grey[300],
                      backgroundImage:
                          _image != null ? FileImage(_image!) : null,
                      child: _image == null
                          ? Icon(Icons.add_a_photo_outlined,
                              size: 40, color: Colors.grey[600])
                          : null,
                    ),
                  ),
                  const SizedBox(height: 20),
                  TextField(
                    controller: usernameController,
                    decoration: const InputDecoration(
                        border: OutlineInputBorder(),
                        labelText: "Username",
                        labelStyle: TextStyle(color: AppColours.textColor)),
                  ),
                  const SizedBox(height: 20),
                  TextField(
                    controller: firstNameController,
                    decoration: const InputDecoration(
                        border: OutlineInputBorder(),
                        labelText: "First Name",
                        labelStyle: TextStyle(color: AppColours.textColor)),
                  ),
                  const SizedBox(height: 20),
                  TextField(
                    controller: lastNameController,
                    decoration: const InputDecoration(
                        border: OutlineInputBorder(),
                        labelText: "Last Name",
                        labelStyle: TextStyle(color: AppColours.textColor)),
                  ),
                  const SizedBox(height: 20),
                  TextField(
                    controller: emailController,
                    decoration: const InputDecoration(
                        border: OutlineInputBorder(),
                        labelText: "Email",
                        labelStyle: TextStyle(color: AppColours.textColor)),
                  ),
                  const SizedBox(height: 20),
                  TextField(
                    controller: phoneNoController,
                    decoration: const InputDecoration(
                        border: OutlineInputBorder(),
                        labelText: "Phone No.",
                        labelStyle: TextStyle(color: AppColours.textColor)),
                  ),
                  const SizedBox(height: 15),
                  Container(
                    alignment: Alignment.centerRight,
                    child: TextButton(
                        onPressed: () async {
                          try {
                            setState(() {
                              user = NewUser(
                                username: usernameController.text,
                                firstName: firstNameController.text,
                                lastName: lastNameController.text,
                                email: emailController.text,
                                phoneNo: phoneNoController.text,
                              );
                            });

                            if (_image != null) {
                              await uploadProfileImage(
                                  usernameController.text, _image!);
                            }

                            await postNewUserData(user);

                            User? currentUser =
                                FirebaseAuth.instance.currentUser;
                            if (currentUser != null) {
                              await _authService.setCustomClaimsOnServer(
                                  currentUser.uid,
                                  usernameController.text,
                                  '${firstNameController.text} ${lastNameController.text}');

                              await currentUser.getIdToken(true);
                            } else {
                              throw Exception("No user is currently signed in");
                            }

                            Navigator.of(context).pushReplacementNamed('/home');
                          } catch (e) {
                            print("Error during submission: $e");
                            ScaffoldMessenger.of(context).showSnackBar(
                              SnackBar(content: Text("An error occurred: $e")),
                            );
                          }
                        },
                        child: Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            Text(
                              "Submit",
                              style: GoogleFonts.darkerGrotesque(
                                fontSize: 36,
                                fontWeight: FontWeight.w500,
                                color: AppColours.textColor,
                              ),
                            ),
                            const SizedBox(width: 10),
                            Transform.translate(
                              offset: const Offset(0, 4),
                              child: const FaIcon(
                                FontAwesomeIcons.chevronRight,
                                size: 22,
                                color: AppColours.textColor,
                              ),
                            )
                          ],
                        )),
                  )
                ],
              ),
            ),
          ),
        ));
  }
}
