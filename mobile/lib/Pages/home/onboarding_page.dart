import 'package:flutter/material.dart';
import 'package:font_awesome_flutter/font_awesome_flutter.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:http/http.dart' as http;
import 'package:mobile/Services/auth.dart';

class NewUser{
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
  const OnboardingPage({super.key});

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
    final response = await http.post(
        Uri.parse('http://10.0.2.2:8000/onboarding/${user.username}').replace(
            queryParameters: {
              'first_name': user.firstName,
              'last_name': user.lastName,
              'email': user.email,
              'phone': user.phoneNo,
            },
          ),
          headers: <String, String>{
            'Content-Type': 'application/x-www-form-urlencoded',
          },
        );
        
        if (response.statusCode == 200) {
          print('User data posted successfully');
        } else {
          throw Exception('Failed to post user data');
        }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
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
                  ),
                ),
                const SizedBox(height: 20),
                Image.asset("assets/onboarding.png"),
                const SizedBox(height: 20),
                TextField(
                  controller: usernameController,
                  decoration: const InputDecoration(
                    border: OutlineInputBorder(),
                    labelText: "Username"
                  ),
                ),
                const SizedBox(height: 20),
                TextField(
                  controller: firstNameController,
                  decoration: const InputDecoration(
                    border: OutlineInputBorder(),
                    labelText: "First Name"
                  ),
                ),
                const SizedBox(height: 20),
                TextField(
                  controller: lastNameController,
                  decoration: const InputDecoration(
                    border: OutlineInputBorder(),
                    labelText: "Last Name"
                  ),
                ),
                const SizedBox(height: 20),
                TextField(
                  controller: emailController,
                  decoration: const InputDecoration(
                    border: OutlineInputBorder(),
                    labelText: "Email"
                  ),
                ),
                const SizedBox(height: 20),
                TextField(
                  controller: phoneNoController,
                  decoration: const InputDecoration(
                    border: OutlineInputBorder(),
                    labelText: "Phone No."
                  ),
                ),
                const SizedBox(height: 15),
                Container(
                  alignment: Alignment.centerRight,
                  child: TextButton(
                    onPressed: () async {
                      setState(() {
                        user = NewUser(
                          username: usernameController.text,
                          firstName: firstNameController.text,
                          lastName: lastNameController.text,
                          email: emailController.text,
                          phoneNo: phoneNoController.text,
                        );
                      });
                      postNewUserData(user);
                      await _authService.saveUsername(usernameController.text);
                      await _authService.saveFullName('${firstNameController.text} ${lastNameController.text}');
                      Navigator.of(context).pushReplacementNamed('/home');
                    },
                    child: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Text(
                          "Submit",
                          style: GoogleFonts.darkerGrotesque(
                            fontSize: 36,
                            fontWeight: FontWeight.w500,
                            color: Colors.black,
                          ),
                        ),
                        const SizedBox(width: 10),
                        Transform.translate(
                          offset: const Offset(0, 4),
                          child: const FaIcon(
                            FontAwesomeIcons.chevronRight,
                            size: 22,
                            color: Colors.black,
                          ),
                        )
                      ],
                    )
                  ),
                )
              ],
            ),
          ),
        ),
      )
    );
  }
}