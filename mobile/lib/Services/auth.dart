import 'package:google_sign_in/google_sign_in.dart';
import 'package:firebase_auth/firebase_auth.dart';

class Auth {
  final FirebaseAuth _auth = FirebaseAuth.instance;

  Future<void> signUp({
    required String email,
    required String password,
    required String username,
  }) async {
    try {
      print('signup called $email $password $username');
      // Create a new user with email and password
      await _auth.createUserWithEmailAndPassword(
        email: email,
        password: password,
      );
      // You can also update the user's display name or other profile information here
      // await _auth.currentUser?.updateDisplayName(username);
    } on FirebaseAuthException catch (e) {
      if (e.code == 'weak-password') {
        print('The password provided is too weak.');
      } else if (e.code == 'email-already-in-use') {
        print('The account already exists for that email.');
      }
    } catch (e) {
      print(e.toString());
    }
  }

  Future<void> login({
    required String email,
    required String password,
  }) async {
    try {
      print('login called $email $password');
      // Sign in with email and password
      await _auth.signInWithEmailAndPassword(
        email: email,
        password: password,
      );
    } on FirebaseAuthException catch (e) {
      print('Error signing in: ${e.code}');
    }
  }

  Future<void> otherLogin({
    required String provider,
  }) async {
    if (provider == 'Google') {
      try{
          final user = await GoogleSignIn().signIn();
          
      }
      catch (e){
        print(e.toString());
      }
    } else if (provider == 'Facebook') {
      // Implement Facebook Sign-In logic here
      // You'll need to use the flutter_facebook_auth package and follow the official Firebase guide
    } else if (provider == 'Apple') {
      // Implement Apple Sign-In logic here
      // You'll need to use the sign_in_with_apple package and follow the official Firebase guide
    }
  }
}