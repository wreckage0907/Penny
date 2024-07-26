import 'dart:convert';
import 'package:flutter/widgets.dart';
import 'package:google_sign_in/google_sign_in.dart';
import 'package:firebase_auth/firebase_auth.dart';
import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:http/http.dart' as http;
import 'package:path_provider/path_provider.dart';
import 'dart:io';

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

  Future<String?> getUsername() async {
    User? user = FirebaseAuth.instance.currentUser;
    if (user != null) {
      await user.getIdToken(true);
      IdTokenResult idTokenResult = await user.getIdTokenResult(false);
      Map<String, dynamic>? claims = idTokenResult.claims;

      if (claims != null && claims['username'] != null) {
        return claims['username'];
      } else {
        final prefs = await SharedPreferences.getInstance();
        return prefs.getString('username');
      }
    }
    return null;
  }

  Future<String?> uploadProfileImage(String userId, File imageFile) async {
    final url = Uri.parse('https://penny-uts7.onrender.com/prof');

    try {
      String? username = await getUsername();
      if (username == null) {
        print('Error: Username not found');
        return null;
      }

      var request = http.MultipartRequest('POST', url);

      request.fields['user'] = userId;
      request.fields['username'] = username;  // Add username to the request

      var fileStream = http.ByteStream(imageFile.openRead());
      var fileLength = await imageFile.length();

      var multipartFile = http.MultipartFile(
        'file',
        fileStream,
        fileLength,
        filename: 'profile_image.jpg',  // Include username in the filename
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
  
  Future<String?> downloadAndUploadProfileImage( String imageUrl) async {
    try {
      // Download the image
      final response = await http.get(Uri.parse(imageUrl));
      if (response.statusCode != 200) {
        print('Failed to download image: ${response.statusCode}');
        return null;
      }
      String? userId = await getUsername();
      // Get temporary directory to store the downloaded image
      final tempDir = await getTemporaryDirectory();
      File file = File('${tempDir.path}/temp_profile_image.jpg');
      await file.writeAsBytes(response.bodyBytes);

      // Upload the image
      return await uploadProfileImage(userId.toString(), file);
    } catch (e) {
      print('Error downloading and uploading image: $e');
      return null;
    }
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
        String? photoUrl = user.photoURL;

        if (isNewUser && photoUrl != null) {
          // Download and upload the Google profile image
          String? uploadedImageUrl = await downloadAndUploadProfileImage(photoUrl);
          photoUrl = uploadedImageUrl ?? photoUrl;
        }

        if (isNewUser) {
          return {
            'user': user,
            'isNewUser': true,
            'photoUrl': photoUrl,
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
            'photoUrl': photoUrl,
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
    final url = Uri.parse('https://penny-uts7.onrender.com/set-custom-claims');
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
