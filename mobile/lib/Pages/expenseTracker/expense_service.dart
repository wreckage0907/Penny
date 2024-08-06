import 'dart:convert';
import 'package:http/http.dart' as http;


class Expense {
  final String name;
  final double amount;
  final String category;
  final int year;
  final String month;

  Expense({
    required this.name,
    required this.amount,
    required this.category,
    required this.year,
    required this.month,
  });

  Map<String, dynamic> toJson() => {
    'name': name,
    'amount': amount,
    'category': category,
    'year': year,
    'month': month,
  };
}

class ExpenseService {
  final String baseUrl;
  final String? userId;

  ExpenseService({required this.baseUrl, required this.userId});

  Future<void> addExpense(Expense expense) async {
    final response = await http.post(
      Uri.parse('$baseUrl/expenses/$userId'),
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode(expense.toJson()),
    );

    if (response.statusCode != 200) {
      throw Exception('Failed to add expense: ${response.body}');
    }
  }

  Future<void> addIncome({
    required double amount,
    required int year,
    required String month,
  }) async {
    final response = await http.post(
      Uri.parse('$baseUrl/income/$userId'),
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode({
        'amount': amount,
        'year': year,
        'month': month,
      }),
    );

    if (response.statusCode != 200) {
      throw Exception('Failed to add income: ${response.body}');
    }
  }

  Future<void> updateIncome({
    required int year,
    required String month,
    required double amount,
  }) async {
    final response = await http.put(
      Uri.parse('$baseUrl/income/$userId/$year/$month'),
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode({
        'amount': amount,
      }),
    );

    if (response.statusCode != 200) {
      throw Exception('Failed to update income: ${response.body}');
    }
  }

  Future<Map<String, dynamic>> getIncome({int? year, String? month}) async {
    final queryParams = <String, String>{};
    if (year != null) queryParams['year'] = year.toString();
    if (month != null) queryParams['month'] = month;

    final response = await http.get(
      Uri.parse('$baseUrl/income/$userId').replace(queryParameters: queryParams),
    );

    if (response.statusCode == 200) {
      return jsonDecode(response.body);
    } else {
      throw Exception('Failed to get income: ${response.body}');
    }
  }

  

  Future<void> updateExpense({
    required String oldName,
    required double oldAmount,
    required String oldCategory,
    required String newName,
    required double newAmount,
    required String newCategory,
    required int year,
    required String month,
  }) async {
    final response = await http.put(
      Uri.parse('$baseUrl/expenses/$userId?year=$year&month=$month'),
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode({
        'old_name': oldName,
        'old_amount': oldAmount,
        'old_category': oldCategory,
        'new_name': newName,
        'new_amount': newAmount,
        'new_category': newCategory,
      }),
    );

    if (response.statusCode != 200) {
      throw Exception('Failed to update expense: ${response.body}');
    }
  }

  Future<Map<String, dynamic>> getExpenses({int? year, String? month}) async {
    String url = '$baseUrl/expenses/$userId';
    if (year != null) {
      url += '?year=$year';
      if (month != null) {
        url += '&month=$month';
      }
    }

    final response = await http.get(Uri.parse(url));

    if (response.statusCode == 200) {
      return jsonDecode(response.body);
    } else {
      throw Exception('Failed to get expenses: ${response.body}');
    }
  }

  Future<void> deleteExpense({
    required int year,
    required String month,
    required String category,
    required String name,
  }) async {
    final response = await http.delete(
      Uri.parse('$baseUrl/expenses/$userId?year=$year&month=$month&category=$category&name=$name'),
    );

    if (response.statusCode != 200) {
      throw Exception('Failed to delete expense: ${response.body}');
    }
  }
}

