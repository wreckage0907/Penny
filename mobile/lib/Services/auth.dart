import 'package:flutter/widgets.dart';
import 'package:google_sign_in/google_sign_in.dart';
import 'package:firebase_auth/firebase_auth.dart';
import 'package:mobile/firebase_options.dart';
import 'package:flutter_facebook_auth/flutter_facebook_auth.dart';
import 'package:shared_preferences/shared_preferences.dart';

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

  Future<UserCredential?> signinWithGoogle() async {
    try {
      final GoogleSignInAccount? googleUser = await GoogleSignIn().signIn();
      final GoogleSignInAuthentication? googleAuth =
          await googleUser?.authentication;
      final credential = GoogleAuthProvider.credential(
        accessToken: googleAuth?.accessToken,
        idToken: googleAuth?.idToken,
      );
      return await FirebaseAuth.instance.signInWithCredential(credential);
    } catch (e) {
      print('Error during Google sign in: $e');
      return null;
    }
  }

  Future<UserCredential?> signInWithGithub() async {
    GithubAuthProvider githubProvider = GithubAuthProvider();
    return await FirebaseAuth.instance.signInWithProvider(githubProvider);
  }

  Future<UserCredential?> signInWithFacebook() async {
    final LoginResult result = await FacebookAuth.instance.login();
    final OAuthCredential facebookauthcred = FacebookAuthProvider.credential(result.accessToken!.token);
    return await FirebaseAuth.instance.signInWithCredential(facebookauthcred);
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
