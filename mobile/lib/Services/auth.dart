import 'package:flutter/widgets.dart';
import 'package:google_sign_in/google_sign_in.dart';
import 'package:firebase_auth/firebase_auth.dart';
import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

class Auth {
  final FirebaseAuth _auth = FirebaseAuth.instance;
  late String _verificationId;

  Future<UserCredential?> signUp({
    required String email,
    required String password,
  }) async {
    try {
      print('signup called $email $password');
      return await _auth.createUserWithEmailAndPassword(
        email: email,
        password: password,
      );
    } on FirebaseAuthException catch (e) {
      if (e.code == 'weak-password') {
        print('The password provided is too weak.');
        print('The account already exists for that email.');
      }
    } catch (e) {
      print(e.toString());
    }
    return null;
  }

  Future<UserCredential?> login({
    required String email,
    required String password,
  }) async {
    try {
      print('login called $email $password');
      return await _auth.signInWithEmailAndPassword(
        email: email,
        password: password,
      );
    } on FirebaseAuthException catch (e) {
      print('Error signing in: ${e.code}');
    }
    return null;
  }

  Future<Map<String, dynamic>> signinWithGoogle() async {
    try {
      final GoogleSignInAccount? googleUser = await GoogleSignIn().signIn();
      final GoogleSignInAuthentication? googleAuth =
          await googleUser?.authentication;
      final credential = GoogleAuthProvider.credential(
        accessToken: googleAuth?.accessToken,
        idToken: googleAuth?.idToken,
      );

      final UserCredential userCredential =
          await FirebaseAuth.instance.signInWithCredential(credential);

      // Check if the user is new or existing
      bool isNewUser = userCredential.additionalUserInfo?.isNewUser ?? false;

      return {
        'user': userCredential.user,
        'isNewUser': isNewUser,
      };
    } catch (e) {
      print('Error during Google sign in: $e');
      return {'error': e.toString()};
    }
  }

  Future<Map<String, dynamic>> signInWithGithub() async {
    try {
      GithubAuthProvider githubProvider = GithubAuthProvider();
      final UserCredential userCredential = await _auth.signInWithProvider(githubProvider);
      bool isNewUser = userCredential.additionalUserInfo?.isNewUser ?? false;
      User? user = userCredential.user;
      if (user != null) {
        return {
          'user': user,
          'isNewUser': isNewUser,
        };
      } else {
        return {'error': 'No user returned from GitHub sign in'};
      }
    } catch (e) {
      print('Error during GitHub sign in: $e');
      return {'error': e.toString()};
    }
  }

   Future<void> signInWithPhoneNumber(String phoneNumber) async {
    try {
      await _auth.verifyPhoneNumber(
        phoneNumber: phoneNumber,
        verificationCompleted: (PhoneAuthCredential credential) async {
          await _auth.signInWithCredential(credential);
        },
        verificationFailed: (FirebaseAuthException e) {
          print('Phone number verification failed: ${e.message}');
        },
        codeSent: (String verificationId, int? resendToken) {
          print('Code sent to $phoneNumber');
        },
        codeAutoRetrievalTimeout: (String verificationId) {
          print('Auto-retrieval timeout');
        },
      );
    } catch (e) {
      print('Error during phone number sign in: $e');
    }
  }

  Future<void> signout({required BuildContext context}) async {
    await FirebaseAuth.instance.signOut();
    await clearUserData();
    Navigator.of(context)
        .pushNamedAndRemoveUntil('/login', (Route<dynamic> route) => false);
  }

  Future<void> saveUsername(String username) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString('username', username);
  }

  Future<void> clearUserData() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove('username');
    await prefs.remove('fullName');
  }

  Future<void> saveFullName(String fullName) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString('fullName', fullName);
  }
}
