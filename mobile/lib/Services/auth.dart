import 'package:firebase_auth/firebase_auth.dart';

class Auth{
  Future<void> Signup({
    required String email,
    required String password,
    required String username
  }) async{
    try{
        await FirebaseAuth.instance.createUserWithEmailAndPassword(
          email: email,
          password: password
        );  
        print('xcvbnhjkl');
    }
    on FirebaseAuthException catch(e){
      if (e.code == 'weak-password') {
        print('The password provided is too weak.');
      } else if (e.code == 'email-already-in-use') {
        print('The account already exists for that email.');
      }
    }
    catch(e){
      print(e.toString());
    }
  }
}