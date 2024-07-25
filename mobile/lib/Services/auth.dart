import 'dart:convert';
import 'package:flutter/widgets.dart';
import 'package:google_sign_in/google_sign_in.dart';
import 'package:firebase_auth/firebase_auth.dart';
import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:http/http.dart' as http;

class Auth {
  final FirebaseAuth _auth = FirebaseAuth.instance;

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
      } else if (e.code == 'email-already-in-use') {
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
      final User? user = userCredential.user;

      if (user != null) {
        bool isNewUser = userCredential.additionalUserInfo?.isNewUser ?? false;

        if (isNewUser) {
          return {
            'user': user,
            'isNewUser': true,
          };
        } else {
          await user.getIdToken(true);
          IdTokenResult idTokenResult = await user.getIdTokenResult(false);
          Map<String, dynamic>? claims = idTokenResult.claims;

          String? username = claims?['username'];
          String? fullName = claims?['fullName'];

          return {
            'user': user,
            'isNewUser': false,
            'username': username,
            'fullName': fullName,
          };
        }
      } else {
        return {'error': 'No user returned from Google sign in'};
      }
    } catch (e) {
      print('Error during Google sign in: $e');
      return {'error': e.toString()};
    }
  }

  Future<Map<String, dynamic>> signInWithGithub() async {
    try {
      GithubAuthProvider githubProvider = GithubAuthProvider();
      final UserCredential userCredential =
          await _auth.signInWithProvider(githubProvider);
      final User? user = userCredential.user;

      if (user != null) {
        bool isNewUser = userCredential.additionalUserInfo?.isNewUser ?? false;

        if (isNewUser) {
          return {
            'user': user,
            'isNewUser': true,
          };
        } else {
          await user.getIdToken(true);
          IdTokenResult idTokenResult = await user.getIdTokenResult(false);
          Map<String, dynamic>? claims = idTokenResult.claims;

          String? username = claims?['username'];
          String? fullName = claims?['fullName'];

          return {
            'user': user,
            'isNewUser': false,
            'username': username,
            'fullName': fullName,
          };
        }
      } else {
        return {'error': 'No user returned from GitHub sign in'};
      }
    } catch (e) {
      print('Error during GitHub sign in: $e');
      return {'error': e.toString()};
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

  Future<Map<String, dynamic>?> getCustomClaims() async {
    User? user = FirebaseAuth.instance.currentUser;
    if (user != null) {
      await user.getIdToken(true);
      IdTokenResult idTokenResult = await user.getIdTokenResult(false);
      return idTokenResult.claims;
    }
    return null;
  }

  Future<void> setCustomClaimsOnServer(
      String uid, String username, String fullName) async {
    final url = Uri.parse('http://10.0.2.2:8000/set-custom-claims');
    final response = await http.post(
      url,
      headers: {'Content-Type': 'application/json'},
      body: json.encode({
        'uid': uid,
        'username': username,
        'fullName': fullName,
      }),
    );

    if (response.statusCode != 200) {
      throw Exception('Failed to set custom claims');
    }
  }
}
