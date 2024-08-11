import 'package:flutter_dotenv/flutter_dotenv.dart';

String backendUrl() {
  String url = dotenv.env['BACKEND_URL']!;
  return url;
}