// import 'package:flutter/material.dart';
// import 'package:mobile/Pages/login/input_field.dart';
// import 'package:mobile/Pages/login/page_buttons.dart';
// import 'package:flutter/gestures.dart';
// import 'package:google_fonts/google_fonts.dart';

// class SignUp extends StatelessWidget {
//   final TextEditingController _usernameController = TextEditingController();
//   final TextEditingController _passwordController = TextEditingController();
//   final TextEditingController _emailController = TextEditingController();
//   SignUp({Key? key}) : super(key: key);

//   @override
//   Widget build(BuildContext context) {
//     return Scaffold(
//       backgroundColor: const Color.fromRGBO(255, 246, 229, 1),
//       body: Padding(
//         padding: const EdgeInsets.all(36),
//         child: Column(
//           mainAxisAlignment: MainAxisAlignment.center,
//           crossAxisAlignment: CrossAxisAlignment.start,
//           children: [
//             Center(
//               child: Container(
//                 width: 200,
//                 child: const Image(
//                   image: AssetImage('assets/title.png'),
//                 ),
//               ),
//             ),
//             const SizedBox(
//               height: 20,
//             ),
//             Center(
//               child: Text(
//                 'Create Account!',
//                 style: GoogleFonts.spectral(
//                   fontSize: 45,
//                   fontWeight: FontWeight.w300,
//                   color: Colors.black87,
//                 ),
//               ),
//             ),
//             const SizedBox(height: 26),
//             UsernameField(controller: _usernameController,),
//             const SizedBox(height: 14),
//             EmailField(controller: _emailController,),
//             const SizedBox(height: 14),
//             PasswordField(controller: _passwordController,),
//             const SizedBox(height: 26),
//             //SignUpButton(username: _usernameController.text,email: _emailController.text,password: _passwordController.text,),
//             const SizedBox(height: 20),
//             Center(
//               child: Text(
//                 'or continue with',
//                 style: GoogleFonts.spectral(
//                   fontSize: 24,
//                   fontWeight: FontWeight.w300,
//                   color: Colors.black87,
//                 ),
//               ),
//             ),
//             const SizedBox(height: 10),
//             const Row(
//               mainAxisAlignment: MainAxisAlignment.center,
//               children: [
//                 SignInWithGoogleButton(),
//                 FacebookButton(),
//                 AppleButton()
//               ],
//             ),
//             const SizedBox(height: 26),
//             Center(
//               child: RichText(
//                 text: TextSpan(
//                   style: GoogleFonts.spectral(
//                     fontSize: 19,
//                     fontWeight: FontWeight.w400,
//                     color: Colors.black,
//                   ),
//                   children: [
//                     const TextSpan(text: 'Already have an account? '),
//                     TextSpan(
//                       text: 'Login',
//                       style: const TextStyle(
//                         color: Colors.redAccent,
//                         decoration: TextDecoration.underline,
//                       ),
//                       recognizer: TapGestureRecognizer()
//                         ..onTap = () {
//                           Navigator.pushNamed(context, '/login');
//                         },
//                     ),
//                   ],
//                 ),
//               ),
//             ),
//           ],
//         ),
//       ),
//     );
//   }
// }
import 'package:flutter/material.dart';
import 'package:flutter/gestures.dart';
import 'package:font_awesome_flutter/font_awesome_flutter.dart'; 
import 'package:google_fonts/google_fonts.dart';
import 'package:mobile/Pages/login/input_field.dart'; // Assuming EmailField is defined here
import 'package:mobile/Pages/login/page_buttons.dart';
import 'package:mobile/Services/auth.dart';

class SignUpPage extends StatefulWidget {
  const SignUpPage({Key? key}) : super(key: key);

  @override
  _SignUpPageState createState() => _SignUpPageState();
}

class _SignUpPageState extends State<SignUpPage> {
  final TextEditingController _usernameController = TextEditingController();
  final TextEditingController _passwordController = TextEditingController();
  final TextEditingController _emailController = TextEditingController();
  final Auth _authService = Auth();

  @override
  void dispose() {
    _usernameController.dispose();
    _passwordController.dispose();
    _emailController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color.fromRGBO(255, 246, 229, 1),
      body: Padding(
        padding: const EdgeInsets.all(28),

        child: SingleChildScrollView(
          child: ConstrainedBox(constraints: const BoxConstraints(minHeight: 700),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Center(
                child: Container(
                  width: 200,
                  child: const Image(
                    image: AssetImage('assets/title.png'),
                  ),
                ),
              ),
              const SizedBox(
                height: 20,
              ),
              Center(
                child: Text(
                  'Welcome!',
                  style: GoogleFonts.spectral(
                    fontSize: 45,
                    fontWeight: FontWeight.w300,
                    color: Colors.black87,
                  ),
                ),
              ),
              const SizedBox(height: 26),
              UsernameField(controller: _usernameController,),
              const SizedBox(height: 14),
              EmailField(controller: _emailController,),
              const SizedBox(height: 14),
              PasswordField(controller: _passwordController,),
              const SizedBox(height: 25),
              Center(
                child: Text(
                  'or continue with',
                  style: GoogleFonts.spectral(
                    fontSize: 24,
                    fontWeight: FontWeight.w300,
                    color: Colors.black87,
                  ),
                ),
              ),
              const SizedBox(height: 10),
              Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  IconButton(
                    onPressed: () async {
                      await _authService.loginWithGoogle();
                    },
                    icon: const FaIcon(FontAwesomeIcons.google, color: Colors.black, size: 32),
                    style: const ButtonStyle(
                      backgroundColor: WidgetStatePropertyAll(Color.fromRGBO(255, 246, 229, 1)),
                      padding: WidgetStatePropertyAll(EdgeInsets.symmetric(horizontal: 20, vertical: 8)),
                      minimumSize: WidgetStatePropertyAll(Size(80, 20)),
                      shape: WidgetStatePropertyAll(
                        CircleBorder(
                          side: BorderSide( 
                            color: Colors.black38,
                            width: 2,
                          ),
                        ),
                      ),
                      elevation: WidgetStatePropertyAll(2),
                    ),
                  ),
                  const FacebookButton(),
                  const AppleButton()
                ],
              ),
              const SizedBox(height: 20),
              Padding(
                padding: const EdgeInsets.only(left: 20,right:20,top: 20,bottom: 5),
                child: TextButton(
                  onPressed: () async {
                    _authService.signUp(username:_usernameController.text,email: _emailController.text,password:_passwordController.text);
                  },
                  style: const ButtonStyle(
                    alignment: Alignment.center,
                    backgroundColor: WidgetStatePropertyAll(Color.fromRGBO(175, 92, 92, 0.8)),
                    padding: WidgetStatePropertyAll(EdgeInsets.symmetric(vertical: 16, horizontal: 24)),
                    minimumSize: WidgetStatePropertyAll(Size(double.infinity, 24)),
                    shape: WidgetStatePropertyAll(
                      RoundedRectangleBorder(
                        side: BorderSide(
                          color: Color.fromRGBO(109, 109, 109, 1),
                          width: 1.5,
                        ),
                        borderRadius: BorderRadius.all(Radius.circular(28)),
                      ),
                    ),
                  ),
                  child: Text(
                    'Sign Up',
                    style: GoogleFonts.spectral(
                      fontSize: 20,
                      fontWeight: FontWeight.bold,
                      color: Colors.white,
                    ),
                  ),
                ),
              ),
              const SizedBox(height: 30),
              Center(
                child: RichText(
                  text: TextSpan(
                    style: GoogleFonts.spectral(
                      fontSize: 19,
                      fontWeight: FontWeight.w400,
                      color: Colors.black,
                    ),
                    children: [
                      const TextSpan(text: 'Already have an account? '),
                      TextSpan(
                        text: 'Sign In',
                        style: const TextStyle(
                          color: Colors.redAccent,
                          decoration: TextDecoration.underline,
                        ),
                        recognizer: TapGestureRecognizer()
                          ..onTap = () {
                            Navigator.pushNamed(context, '/login');
                          },
                      ),
                    ],
                  ),
                ),
              ),
            ],
          ),
        ),
        ),
      ),
    );
  }
}