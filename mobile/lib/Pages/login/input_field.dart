import 'package:firebase_auth/firebase_auth.dart';
import 'package:flutter/material.dart';
import 'package:font_awesome_flutter/font_awesome_flutter.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:mobile/consts/app_colours.dart';

class UsernameField extends StatelessWidget {
  final TextEditingController controller;
  const UsernameField({super.key, required this.controller});

  @override
  Widget build(BuildContext context) {
    return TextField(
      controller: controller,
      decoration: const InputDecoration(
        hintText: 'Username',
        prefixIcon: Padding(
          padding: EdgeInsets.only(right: 14),
          child: Icon(Icons.person_outlined, size: 36),
        ),
        prefixIconColor: AppColours.textColor,
        contentPadding: EdgeInsets.only(top: 10),
      ),
      style: GoogleFonts.spectral(
        fontSize: 24,
        fontWeight: FontWeight.w400,
        color: AppColours.textColor,
      ),
    );
  }
}

class EmailField extends StatelessWidget {
  final TextEditingController controller;
  const EmailField({super.key, required this.controller});

  @override
  Widget build(BuildContext context) {
    return TextField(
      controller: controller,
      style: GoogleFonts.spectral(
        fontSize: 24,
        fontWeight: FontWeight.w400,
        color: AppColours.textColor,
      ),
      decoration: const InputDecoration(
        hintText: 'Email',
        prefixIcon: Padding(
          padding: EdgeInsets.only(right: 14),
          child: Icon(Icons.email_outlined, size: 36),
        ),
        prefixIconColor: AppColours.textColor,
        contentPadding: EdgeInsets.only(top: 10),
      ),
    );
  }
}

class PasswordField extends StatefulWidget {
  final TextEditingController controller;
  const PasswordField({super.key, required this.controller});

  @override
  State<PasswordField> createState() => _PasswordFieldState();
}

class _PasswordFieldState extends State<PasswordField> {
  bool _isObscured = true;

  @override
  Widget build(BuildContext context) {
    return TextField(
      obscureText: _isObscured,
      controller: widget.controller,
      style: GoogleFonts.spectral(
        fontSize: 24,
        fontWeight: FontWeight.w400,
        color: AppColours.textColor,
      ),
      decoration: InputDecoration(
        hintText: 'Password',
        prefixIcon: Padding(
          padding: const EdgeInsets.only(right: 14),
          child: Icon(
              _isObscured ? Icons.lock_outline : Icons.lock_open_rounded,
              size: 36),
        ),
        prefixIconColor: AppColours.textColor,
        contentPadding: const EdgeInsets.only(top: 10),
        suffixIcon: IconButton(
          icon: Icon(
            _isObscured ? Icons.visibility_off_rounded : Icons.visibility,
            size: 32,
          ),
          onPressed: () {
            setState(() {
              _isObscured = !_isObscured;
            });
          },
        ),
        suffixIconColor: AppColours.textColor,
      ),
    );
  }
}

class MyPhoneSignInWidget extends StatefulWidget {
  @override
  _MyPhoneSignInWidgetState createState() => _MyPhoneSignInWidgetState();
}

class _MyPhoneSignInWidgetState extends State<MyPhoneSignInWidget> {
  final FirebaseAuth _auth = FirebaseAuth.instance;
  late String _verificationId;

  Future<void> signInWithPhoneNumber(String phoneNumber) async {
    try {
      await _auth.verifyPhoneNumber(
        phoneNumber: phoneNumber,
        verificationCompleted: (PhoneAuthCredential credential) async {
          await _auth.signInWithCredential(credential);
          _handleRouting();
        },
        verificationFailed: (FirebaseAuthException e) {
          print('Phone number verification failed: ${e.message}');
          print('Error code: ${e.code}');
          print('Error details: ${e.toString()}');
        },
        codeSent: (String verificationId, int? resendToken) {
          setState(() {
            _verificationId = verificationId;
          });
          print('Code sent to $phoneNumber');
          _showVerificationCodeDialog();
        },
        codeAutoRetrievalTimeout: (String verificationId) {
          print('Auto-retrieval timeout');
        },
      );
    } catch (e) {
      print('Error during phone number sign in: $e');
    }
  }

  Future<void> signInWithSmsCode(String smsCode) async {
    try {
      PhoneAuthCredential credential = PhoneAuthProvider.credential(
        verificationId: _verificationId,
        smsCode: smsCode,
      );
      await _auth.signInWithCredential(credential);
      print('Successfully signed in');
      _handleRouting();
    } catch (e) {
      print('Error during sign in with SMS code: $e');
    }
  }

  void _handleRouting() {
    User? user = _auth.currentUser;
    if (user != null) {
      // Replace the following line with actual logic to determine if the user is signing in for the first time
      bool isFirstTime = true; // Placeholder logic

      if (isFirstTime) {
        Navigator.of(context).pushReplacementNamed('/onboarding');
      } else {
        Navigator.of(context).pushReplacementNamed('/home');
      }
    }
  }

  void _showPhoneNumberDialog() {
    final TextEditingController _phoneController = TextEditingController();

    showDialog(
      context: context,
      builder: (context) {
        return AlertDialog(
          title: Text('Enter your phone number'),
          content: TextField(
            controller: _phoneController,
            keyboardType: TextInputType.phone,
            decoration: InputDecoration(
              labelText: 'Phone Number',
              hintText: '+1 123 456 7890',
            ),
          ),
          actions: [
            TextButton(
              onPressed: () {
                Navigator.of(context).pop();
              },
              child: Text('Cancel'),
            ),
            TextButton(
              onPressed: () {
                Navigator.of(context).pop();
                signInWithPhoneNumber(_phoneController.text);
              },
              child: Text('Submit'),
            ),
          ],
        );
      },
    );
  }

  void _showVerificationCodeDialog() {
    final TextEditingController _codeController = TextEditingController();

    showDialog(
      context: context,
      builder: (context) {
        return AlertDialog(
          title: Text('Enter the verification code'),
          content: TextField(
            controller: _codeController,
            keyboardType: TextInputType.number,
            decoration: InputDecoration(
              labelText: 'Verification Code',
            ),
          ),
          actions: [
            TextButton(
              onPressed: () {
                Navigator.of(context).pop();
              },
              child: Text('Cancel'),
            ),
            TextButton(
              onPressed: () {
                Navigator.of(context).pop();
                signInWithSmsCode(_codeController.text);
              },
              child: Text('Submit'),
            ),
          ],
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    return IconButton(
      onPressed: _showPhoneNumberDialog,
      icon: const FaIcon(
        FontAwesomeIcons.phone,
        color: AppColours.buttonColor,
        size: 30,
      ),
      style: const ButtonStyle(
        backgroundColor: WidgetStatePropertyAll(AppColours.cardColor),
        padding: WidgetStatePropertyAll(
          EdgeInsets.symmetric(horizontal: 1, vertical: 8),
        ),
        minimumSize: WidgetStatePropertyAll(Size(80, 20)),
        shape: WidgetStatePropertyAll(
          CircleBorder(
            side: BorderSide(
              color: AppColours.cardColor,
              width: 2,
            ),
          ),
        ),
        elevation: WidgetStatePropertyAll(2),
      ),
    );
  }
}
