import 'package:firebase_auth/firebase_auth.dart';

class Auth{
  Future<void> Signup({
    required String email,
    required String password,
    required String username
  }) async{
    try{
      print('signup called'+email+password+username);
        await FirebaseAuth.instance.createUserWithEmailAndPassword(
          email: email,
          password: password
        );  
        
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
  Future<void> Login({
    required String email,
    required String password
  }) async {
    try{
      print('login called'+email+password);
      await FirebaseAuth.instance.signInWithEmailAndPassword(email: email, password: password);
    }
    on FirebaseAuthException catch(e){
      print('error signing in '+e.code); 
      }
  }
}