import 'package:dropdown_button2/dropdown_button2.dart';
import 'package:firebase_auth/firebase_auth.dart';
import 'package:fl_chart/fl_chart.dart';
import 'package:flutter/material.dart';
import 'package:font_awesome_flutter/font_awesome_flutter.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:mobile/consts/app_colours.dart';
import 'package:mobile/Pages/expenseTracker/expense_service.dart';
import 'package:mobile/consts/toast_messages.dart';
import 'package:shared_preferences/shared_preferences.dart';

class ExpenseDashboard extends StatefulWidget {
  const ExpenseDashboard({super.key});

  @override
  State<ExpenseDashboard> createState() => _ExpenseTrackerState();
}

class _ExpenseTrackerState extends State<ExpenseDashboard> {
  String? username;
  int touchedIndex = -1;
  int selectedYear = DateTime.now().year;
  String selectedMonth = DateTime.now().month.toString().padLeft(2, '0');
  Map<String, dynamic> expenseData = {};
  bool isLoading = true;
  ExpenseService? _expenseService;
  double totalAmount = 0;
  double incomeAmount = 0;

  final List<Color> colors = [
    Colors.teal,
    Colors.yellow,
    Colors.deepOrange,
    Colors.pink,
    Colors.blueAccent,
    Colors.blueGrey
  ];

  final Map<String, FaIcon> icons = {
    "Utility Bills": const FaIcon(
      FontAwesomeIcons.moneyBillWave,
      color: AppColours.textColor,
    ),
    "Food": const FaIcon(
      FontAwesomeIcons.utensils,
      color: AppColours.textColor,
    ),
    "Transport": const FaIcon(
      FontAwesomeIcons.car,
      color: AppColours.textColor,
    ),
    "Shopping": const FaIcon(
      FontAwesomeIcons.cartShopping,
      color: AppColours.textColor,
    ),
    "Entertainment": const FaIcon(
      FontAwesomeIcons.film,
      color: AppColours.textColor,
    ),
    "Holidays": const FaIcon(
      FontAwesomeIcons.champagneGlasses,
      color: AppColours.textColor,
    ),
    "Gifts": const FaIcon(
      FontAwesomeIcons.gifts,
      color: AppColours.textColor,
    ),
    "Miscellaneous": const FaIcon(
      FontAwesomeIcons.tags,
      color: AppColours.textColor,
    ),
  };

  final List<String> monthNames = [
    'January',
    'February',
    'March',
    'April',
    'May',
    'June',
    'July',
    'August',
    'September',
    'October',
    'November',
    'December'
  ];

  Future<void> _loadUsername() async {
    User? user = FirebaseAuth.instance.currentUser;
    if (user != null) {
      await user.getIdToken(true);
      IdTokenResult idTokenResult = await user.getIdTokenResult(false);
      Map? claims = idTokenResult.claims;

      if (claims != null && claims['username'] != null) {
        setState(() {
          username = claims['username'];
        });
      } else {
        final prefs = await SharedPreferences.getInstance();
        setState(() {
          username = prefs.getString('username');
        });
      }

      _initializeExpenseService();
    }
  }

  void _initializeExpenseService() {
    if (username != null) {
      _expenseService =
          ExpenseService(baseUrl: "http://10.0.2.2:8000", userId: username);
    }
  }

  @override
  void initState() {
    super.initState();
    selectedMonth = monthNames[DateTime.now().month - 1];
    _loadUsername().then((_) {
      _loadExpenseData();
      _loadIncomeData();
    });
  }

  Future<void> _loadIncomeData() async {
    try {
      Map<String, dynamic> incomeData = await _expenseService!.getIncome(
        year: selectedYear,
        month: selectedMonth,
      );
      setState(() {
        incomeAmount =
            incomeData[selectedYear.toString()]?[selectedMonth] ?? 0.0;
      });
    } catch (e) {
      print('Error loading income data: $e');
      setState(() {
        incomeAmount = 0.0;
      });
    }
  }

  Future<void> _loadExpenseData() async {
    setState(() {
      isLoading = true;
    });
    try {
      expenseData = await _expenseService!
          .getExpenses(year: selectedYear, month: selectedMonth);
      _calculateTotalAmount();
    } catch (e) {
      print('Error loading expense data: $e');
      expenseData = {};
    }
    setState(() {
      isLoading = false;
    });
  }

  void _calculateTotalAmount() {
    totalAmount = 0;
    getCategoriesForSelectedMonthAndYear().forEach((category, expenses) {
      totalAmount += (expenses as List).fold(
          0.0, (sum, expense) => sum + (expense['amount'] as num).toDouble());
    });
  }

  Map<String, dynamic> getCategoriesForSelectedMonthAndYear() {
    if (expenseData.containsKey(selectedYear.toString()) &&
        expenseData[selectedYear.toString()].containsKey(selectedMonth) &&
        expenseData[selectedYear.toString()][selectedMonth]
            .containsKey('categories')) {
      return expenseData[selectedYear.toString()][selectedMonth]['categories'];
    }
    return {};
  }

  List<PieChartSectionData> getSections() {
    Map<String, dynamic> categories = getCategoriesForSelectedMonthAndYear();
    List<MapEntry<String, double>> categoryTotals = [];

    categories.forEach((category, expenses) {
      double total = (expenses as List).fold(
          0.0, (sum, expense) => sum + (expense['amount'] as num).toDouble());
      categoryTotals.add(MapEntry(category, total));
    });

    return List.generate(categoryTotals.length, (i) {
      final category = categoryTotals[i];
      return PieChartSectionData(
        color: colors[i % colors.length],
        value: category.value,
        title: '',
        radius: 10,
        titleStyle: GoogleFonts.dmSans(
          fontSize: 14,
          fontWeight: FontWeight.w700,
          color: AppColours.textColor,
        ),
      );
    });
  }

  void _showExpensesForCategory(String category, List<dynamic> expenses) {
    showBottomSheet(
      context: context,
      backgroundColor: AppColours.backgroundColor,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(25.0)),
      ),
      builder: (BuildContext context) {
        return StatefulBuilder(
          builder: (BuildContext context, StateSetter setState) {
            return Container(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    "Expenses for $category",
                    style: GoogleFonts.dmSans(
                      fontSize: 24,
                      fontWeight: FontWeight.bold,
                      color: AppColours.textColor,
                    ),
                  ),
                  const SizedBox(height: 16),
                  Expanded(
                    child: ListView.builder(
                      itemCount: expenses.length,
                      itemBuilder: (context, index) {
                        final expense = expenses[index];
                        return ListTile(
                          title: Text(
                            expense['name'],
                            style: GoogleFonts.dmSans(
                              fontSize: 18,
                              fontWeight: FontWeight.w500,
                              color: AppColours.textColor,
                            ),
                          ),
                          trailing: Row(
                            mainAxisAlignment: MainAxisAlignment.spaceAround,
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              Text(
                                "\$${expense['amount'].toStringAsFixed(2)}",
                                style: GoogleFonts.dmSans(
                                  fontSize: 18,
                                  fontWeight: FontWeight.w500,
                                  color: AppColours.textColor,
                                ),
                              ),
                              IconButton(
                                icon: const Icon(Icons.edit,
                                    color: AppColours.textColor),
                                onPressed: () =>
                                    _editExpense(expense, category, setState),
                              ),
                              IconButton(
                                icon: const FaIcon(FontAwesomeIcons.trashCan,
                                    color: Colors.red),
                                onPressed: () => _showDeleteConfirmation(
                                    context, category, expense, setState),
                              ),
                            ],
                          ),
                        );
                      },
                    ),
                  ),
                ],
              ),
            );
          },
        );
      },
    );
  }

  void _editExpense(Map<String, dynamic> expense, String category,
      StateSetter setModalState) {
    String newName = expense['name'];
    double newAmount = expense['amount'].toDouble();

    showDialog(
      context: context,
      builder: (BuildContext context) {
        return AlertDialog(
          backgroundColor: AppColours.backgroundColor,
          title: Text(
            "Edit Expense",
            style: GoogleFonts.dmSans(
              fontSize: 24,
              fontWeight: FontWeight.w500,
              color: AppColours.textColor,
            ),
          ),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextField(
                style: GoogleFonts.dmSans(
                  fontSize: 20,
                  fontWeight: FontWeight.w300,
                  color: AppColours.textColor,
                ),
                decoration: InputDecoration(
                  labelText: 'Expense Name',
                  labelStyle: GoogleFonts.dmSans(
                    fontSize: 20,
                    fontWeight: FontWeight.w300,
                    color: AppColours.textColor,
                  ),
                ),
                onChanged: (value) {
                  newName = value;
                },
                controller: TextEditingController(text: expense['name']),
              ),
              TextField(
                style: GoogleFonts.dmSans(
                  fontSize: 20,
                  fontWeight: FontWeight.w300,
                  color: AppColours.textColor,
                ),
                keyboardType: TextInputType.number,
                decoration: InputDecoration(
                  labelText: 'Amount',
                  labelStyle: GoogleFonts.dmSans(
                    fontSize: 20,
                    fontWeight: FontWeight.w300,
                    color: AppColours.textColor,
                  ),
                ),
                onChanged: (value) {
                  newAmount = double.tryParse(value) ?? newAmount;
                },
                controller:
                    TextEditingController(text: expense['amount'].toString()),
              ),
            ],
          ),
          actions: [
            TextButton(
              child: Text(
                'Cancel',
                style: GoogleFonts.dmSans(
                  fontSize: 18,
                  fontWeight: FontWeight.w500,
                  color: AppColours.textColor,
                ),
              ),
              onPressed: () {
                Navigator.of(context).pop();
              },
            ),
            TextButton(
              child: Text(
                'Save',
                style: GoogleFonts.dmSans(
                  fontSize: 18,
                  fontWeight: FontWeight.w500,
                  color: AppColours.textColor,
                ),
              ),
              onPressed: () async {
                try {
                  await _expenseService!.updateExpense(
                    oldName: expense['name'],
                    oldAmount: expense['amount'],
                    oldCategory: category,
                    newName: newName,
                    newAmount: newAmount,
                    newCategory: category,
                    year: selectedYear,
                    month: selectedMonth,
                  );
                  Navigator.of(context).pop();
                  setModalState(() {
                    expense['name'] = newName;
                    expense['amount'] = newAmount;
                  });
                  _loadExpenseData();
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(
                      content: Text('Expense updated successfully'),
                      backgroundColor: Colors.green,
                    ),
                  );
                } catch (e) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(
                      content: Text('Failed to update expense: $e'),
                      backgroundColor: Colors.red,
                    ),
                  );
                }
              },
            ),
          ],
        );
      },
    );
  }

  void _deleteExpense(String category, Map<String, dynamic> expense,
      StateSetter setState) async {
    try {
      await _expenseService!.deleteExpense(
        year: selectedYear,
        month: selectedMonth,
        category: category,
        name: expense['name'],
      );
      setState(() {
        getCategoriesForSelectedMonthAndYear()[category].removeWhere((e) =>
            e['name'] == expense['name'] && e['amount'] == expense['amount']);
      });
      _loadExpenseData();
      ToastMessages.successToast(context, "Expense Deleted Successfully");
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Failed to delete expense: $e'),
          backgroundColor: Colors.red,
        ),
      );
    }
  }

  void _showDeleteConfirmation(BuildContext context, String category,
      Map<String, dynamic> expense, StateSetter setState) {
    showDialog(
      context: context,
      builder: (BuildContext context) {
        return AlertDialog(
          backgroundColor: AppColours.backgroundColor,
          title: Text(
            "Delete Expense",
            style: GoogleFonts.dmSans(
              fontSize: 24,
              fontWeight: FontWeight.w500,
              color: AppColours.textColor,
            ),
          ),
          content: Text(
            "Are you sure you want to delete this expense?",
            style: GoogleFonts.dmSans(
              fontSize: 18,
              fontWeight: FontWeight.w300,
              color: AppColours.textColor,
            ),
          ),
          actions: [
            TextButton(
              child: Text(
                'Cancel',
                style: GoogleFonts.dmSans(
                  fontSize: 18,
                  fontWeight: FontWeight.w500,
                  color: AppColours.textColor,
                ),
              ),
              onPressed: () {
                Navigator.of(context).pop();
              },
            ),
            TextButton(
              child: Text(
                'Delete',
                style: GoogleFonts.dmSans(
                  fontSize: 18,
                  fontWeight: FontWeight.w500,
                  color: Colors.red,
                ),
              ),
              onPressed: () {
                Navigator.of(context).pop();
                _deleteExpense(category, expense, setState);
              },
            ),
          ],
        );
      },
    );
  }

  void _showAddExpenseDialog() {
    String? selectedCategory;
    String expenseName = '';
    String expenseAmount = '';

    showDialog(
      context: context,
      builder: (BuildContext context) {
        return StatefulBuilder(
          builder: (BuildContext context, StateSetter setModalState) {
            return AlertDialog(
              backgroundColor: AppColours.backgroundColor,
              title: Text(
                "Add New Expense",
                style: GoogleFonts.dmSans(
                  fontSize: 24,
                  fontWeight: FontWeight.w500,
                  color: AppColours.textColor,
                ),
              ),
              content: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  TextField(
                    style: GoogleFonts.dmSans(
                      fontSize: 20,
                      fontWeight: FontWeight.w300,
                      color: AppColours.textColor,
                    ),
                    decoration: InputDecoration(
                      labelText: 'Expense Name',
                      labelStyle: GoogleFonts.dmSans(
                        fontSize: 20,
                        fontWeight: FontWeight.w300,
                        color: AppColours.textColor,
                      ),
                    ),
                    onChanged: (value) {
                      expenseName = value;
                    },
                  ),
                  TextField(
                    style: GoogleFonts.dmSans(
                      fontSize: 20,
                      fontWeight: FontWeight.w300,
                      color: AppColours.textColor,
                    ),
                    keyboardType: TextInputType.number,
                    decoration: InputDecoration(
                      labelText: 'Amount',
                      labelStyle: GoogleFonts.dmSans(
                        fontSize: 20,
                        fontWeight: FontWeight.w300,
                        color: AppColours.textColor,
                      ),
                    ),
                    onChanged: (value) {
                      expenseAmount = value;
                    },
                  ),
                  const SizedBox(height: 10),
                  DropdownButtonFormField<String>(
                    decoration: InputDecoration(
                      labelText: 'Select a Category',
                      labelStyle: GoogleFonts.dmSans(
                        fontSize: 20,
                        fontWeight: FontWeight.w300,
                        color: AppColours.textColor,
                      ),
                    ),
                    value: selectedCategory,
                    items: icons.keys.map((String category) {
                      return DropdownMenuItem<String>(
                        value: category,
                        child: Text(
                          category,
                          style: GoogleFonts.dmSans(
                            fontSize: 20,
                            fontWeight: FontWeight.w300,
                            color: AppColours.textColor,
                          ),
                        ),
                      );
                    }).toList(),
                    onChanged: (String? newValue) {
                      setModalState(() {
                        selectedCategory = newValue;
                      });
                    },
                  ),
                ],
              ),
              actions: [
                TextButton(
                  child: Text(
                    'Cancel',
                    style: GoogleFonts.dmSans(
                      fontSize: 18,
                      fontWeight: FontWeight.w500,
                      color: AppColours.textColor,
                    ),
                  ),
                  onPressed: () {
                    Navigator.of(context).pop();
                  },
                ),
                TextButton(
                  child: Text(
                    'Add',
                    style: GoogleFonts.dmSans(
                      fontSize: 18,
                      fontWeight: FontWeight.w500,
                      color: AppColours.textColor,
                    ),
                  ),
                  onPressed: () async {
                    if (expenseName.isNotEmpty &&
                        expenseAmount.isNotEmpty &&
                        selectedCategory != null) {
                      double amount = double.parse(expenseAmount);
                      Expense newExpense = Expense(
                        name: expenseName,
                        amount: amount,
                        category: selectedCategory!,
                        year: selectedYear,
                        month: selectedMonth,
                      );

                      try {
                        await _expenseService!.addExpense(newExpense);
                        Navigator.of(context).pop();
                        _loadExpenseData();
                        ScaffoldMessenger.of(context).showSnackBar(
                          const SnackBar(
                            content: Text('Expense added successfully'),
                            backgroundColor: Colors.green,
                          ),
                        );
                      } catch (e) {
                        ScaffoldMessenger.of(context).showSnackBar(
                          SnackBar(
                            content: Text('Failed to add expense: $e'),
                            backgroundColor: Colors.red,
                          ),
                        );
                      }
                    } else {
                      ScaffoldMessenger.of(context).showSnackBar(
                        const SnackBar(
                          content: Text('Please fill all fields'),
                          backgroundColor: Colors.red,
                        ),
                      );
                    }
                  },
                ),
              ],
            );
          },
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    final currentYear = DateTime.now().year;
    final yearRange = List<int>.generate(11, (i) => currentYear - 5 + i);

    return Scaffold(
      backgroundColor: AppColours.backgroundColor,
      body: SingleChildScrollView(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 5),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Card(
                color: AppColours.backgroundColor,
                elevation: 3,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(16),
                ),
                child: Padding(
                  padding: const EdgeInsets.all(16.0),
                  child: Column(
                    children: [
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceAround,
                        children: [
                          Text(
                            "Month",
                            style: GoogleFonts.dmSans(
                                fontSize: 14,
                                fontWeight: FontWeight.w300,
                                color: AppColours.textColor),
                          ),
                          SizedBox(
                            width: 130,
                            child: DropdownButtonFormField2<String>(
                              isExpanded: true,
                              decoration: const InputDecoration(
                                  enabled: false, border: InputBorder.none),
                              buttonStyleData: ButtonStyleData(
                                  height: 30,
                                  padding: const EdgeInsets.only(right: 10),
                                  decoration: BoxDecoration(
                                      border: Border.all(),
                                      borderRadius: BorderRadius.circular(35))),
                              iconStyleData: const IconStyleData(
                                  icon: FaIcon(
                                FontAwesomeIcons.chevronDown,
                                color: AppColours.textColor,
                                size: 14,
                              )),
                              value: selectedMonth,
                              items: monthNames
                                  .map((item) => DropdownMenuItem<String>(
                                        value: item,
                                        child: Text(
                                          item,
                                          style: const TextStyle(
                                              fontSize: 14,
                                              fontWeight: FontWeight.w600),
                                        ),
                                      ))
                                  .toList(),
                              onChanged: (String? newValue) {
                                if (newValue != null) {
                                  setState(() {
                                    selectedMonth = newValue;
                                    _loadExpenseData();
                                    _loadIncomeData();
                                  });
                                }
                              },
                            ),
                          ),
                          Text(
                            "Year",
                            style: GoogleFonts.dmSans(
                                fontSize: 14,
                                fontWeight: FontWeight.w300,
                                color: AppColours.textColor),
                          ),
                          SizedBox(
                            width: 95,
                            child: DropdownButtonFormField2<int>(
                              isExpanded: true,
                              decoration: const InputDecoration(
                                  enabled: false, border: InputBorder.none),
                              buttonStyleData: ButtonStyleData(
                                  height: 30,
                                  padding: const EdgeInsets.only(right: 10),
                                  decoration: BoxDecoration(
                                      border: Border.all(),
                                      borderRadius: BorderRadius.circular(35))),
                              iconStyleData: const IconStyleData(
                                  icon: FaIcon(
                                FontAwesomeIcons.chevronDown,
                                color: AppColours.textColor,
                                size: 14,
                              )),
                              value: selectedYear,
                              items: yearRange
                                  .map((int year) => DropdownMenuItem<int>(
                                        value: year,
                                        child: Text(
                                          year.toString(),
                                        ),
                                      ))
                                  .toList(),
                              onChanged: (int? newValue) {
                                if (newValue != null) {
                                  setState(() {
                                    selectedYear = newValue;
                                    _loadExpenseData();
                                    _loadIncomeData();
                                  });
                                }
                              },
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 10),
                      isLoading
                          ? const CircularProgressIndicator()
                          : getCategoriesForSelectedMonthAndYear().isEmpty
                              ? Center(
                                  child: Text(
                                    "No data for this month and year",
                                    style: GoogleFonts.dmSans(
                                      fontSize: 16,
                                      fontWeight: FontWeight.w500,
                                      color: AppColours.textColor,
                                    ),
                                  ),
                                )
                              : Row(
                                  mainAxisAlignment: MainAxisAlignment.center,
                                  children: [
                                    Expanded(
                                      flex: 1,
                                      child: Stack(
                                        children: [
                                          SizedBox(
                                            height: 200,
                                            child: PieChart(
                                              PieChartData(
                                                sectionsSpace: 3,
                                                centerSpaceRadius: 70,
                                                sections: getSections(),
                                              ),
                                            ),
                                          ),
                                          Positioned.fill(
                                            child: Column(
                                              mainAxisAlignment:
                                                  MainAxisAlignment.center,
                                              children: [
                                                Text(
                                                  'Total',
                                                  style: GoogleFonts.dmSans(
                                                    fontSize: 16,
                                                    fontWeight: FontWeight.bold,
                                                    color: AppColours.textColor,
                                                  ),
                                                ),
                                                Text(
                                                  '\$${totalAmount.toStringAsFixed(2)}',
                                                  style: GoogleFonts.dmSans(
                                                    fontSize: 20,
                                                    fontWeight: FontWeight.bold,
                                                    color: AppColours.textColor,
                                                  ),
                                                ),
                                              ],
                                            ),
                                          ),
                                        ],
                                      ),
                                    ),
                                    const SizedBox(width: 20),
                                    Expanded(
                                      flex: 1,
                                      child: Column(
                                        crossAxisAlignment:
                                            CrossAxisAlignment.start,
                                        children:
                                            getCategoriesForSelectedMonthAndYear()
                                                .entries
                                                .map((entry) {
                                          String category = entry.key;
                                          int index =
                                              getCategoriesForSelectedMonthAndYear()
                                                  .keys
                                                  .toList()
                                                  .indexOf(category);
                                          return Padding(
                                            padding: const EdgeInsets.symmetric(
                                                vertical: 4.0),
                                            child: Row(
                                              children: [
                                                Container(
                                                  height: 10,
                                                  width: 10,
                                                  decoration: BoxDecoration(
                                                    color: colors[
                                                        index % colors.length],
                                                    shape: BoxShape.circle,
                                                  ),
                                                ),
                                                const SizedBox(width: 10),
                                                Expanded(
                                                  child: Text(
                                                    category,
                                                    style: GoogleFonts.dmSans(
                                                      fontSize: 14,
                                                      fontWeight:
                                                          FontWeight.w600,
                                                      color:
                                                          AppColours.textColor,
                                                    ),
                                                    overflow:
                                                        TextOverflow.ellipsis,
                                                  ),
                                                ),
                                              ],
                                            ),
                                          );
                                        }).toList(),
                                      ),
                                    ),
                                  ],
                                ),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 10),
              Card(
                color: AppColours.backgroundColor,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(16),
                ),
                elevation: 3,
                child: Padding(
                  padding: const EdgeInsets.all(16.0),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        selectedMonth,
                        style: GoogleFonts.dmSans(
                          fontSize: 28,
                          fontWeight: FontWeight.bold,
                          color: AppColours.textColor,
                        ),
                      ),
                      const SizedBox(height: 8),
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceAround,
                        children: [
                          Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                "Income",
                                style: GoogleFonts.dmSans(
                                  fontSize: 20,
                                  fontWeight: FontWeight.w300,
                                  color: AppColours.textColor,
                                ),
                              ),
                              Text(
                                "\$${incomeAmount.toStringAsFixed(2)}",
                                style: GoogleFonts.dmSans(
                                  fontSize: 24,
                                  fontWeight: FontWeight.w600,
                                  color: Colors.green,
                                ),
                              )
                            ],
                          ),
                          Text(
                            "/",
                            style: GoogleFonts.dmSans(
                              fontSize: 28,
                              fontWeight: FontWeight.w500,
                              color: AppColours.textColor.withOpacity(0.8),
                            ),
                          ),
                          Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                "Expenses",
                                style: GoogleFonts.dmSans(
                                  fontSize: 20,
                                  fontWeight: FontWeight.w300,
                                  color: AppColours.textColor,
                                ),
                              ),
                              Text(
                                "\$${totalAmount.toStringAsFixed(2)}",
                                style: GoogleFonts.dmSans(
                                  fontSize: 24,
                                  fontWeight: FontWeight.w600,
                                  color: Colors.red,
                                ),
                              )
                            ],
                          )
                        ],
                      ),
                      const SizedBox(height: 8),
                      Stack(
                        children: [
                          Container(
                            height: 10,
                            decoration: BoxDecoration(
                              color: Colors.transparent,
                              borderRadius: BorderRadius.circular(6),
                            ),
                          ),
                          Row(
                            children: [
                              Flexible(
                                flex: (incomeAmount * 100).round(),
                                child: Container(
                                  height: 10,
                                  decoration: BoxDecoration(
                                      color: Colors.green,
                                      borderRadius: BorderRadius.circular(6)),
                                ),
                              ),
                              const SizedBox(width: 10),
                              Flexible(
                                flex: (totalAmount * 100).round(),
                                child: Container(
                                  height: 10,
                                  decoration: BoxDecoration(
                                      color: Colors.red,
                                      borderRadius: BorderRadius.circular(6)),
                                ),
                              ),
                            ],
                          ),
                        ],
                      ),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 20),
              Padding(
                padding: const EdgeInsets.only(left: 8.0),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text(
                      "Expenses",
                      style: GoogleFonts.dmSans(
                        fontSize: 28,
                        fontWeight: FontWeight.w600,
                        color: AppColours.textColor,
                      ),
                    ),
                    IconButton(
                        style: const ButtonStyle(
                            backgroundColor:
                                WidgetStatePropertyAll(AppColours.cardColor)),
                        onPressed: _showAddExpenseDialog,
                        icon: const FaIcon(
                          FontAwesomeIcons.plus,
                          color: AppColours.textColor,
                        ))
                  ],
                ),
              ),
              const SizedBox(height: 8),
              ListView.builder(
                shrinkWrap: true,
                physics: const NeverScrollableScrollPhysics(),
                itemCount: getCategoriesForSelectedMonthAndYear().length,
                itemBuilder: (context, categoryIndex) {
                  String category = getCategoriesForSelectedMonthAndYear()
                      .keys
                      .elementAt(categoryIndex);
                  List<dynamic> expenses =
                      getCategoriesForSelectedMonthAndYear()[category];
                  double total = expenses.fold(
                      0.0,
                      (sum, expense) =>
                          sum + (expense['amount'] as num).toDouble());
                  return Card(
                    margin: const EdgeInsets.only(bottom: 15),
                    color: AppColours.backgroundColor,
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(16),
                    ),
                    elevation: 3,
                    child: ListTile(
                      leading: CircleAvatar(
                        radius: 25,
                        backgroundColor: AppColours.cardColor,
                        child: icons.containsKey(category)
                            ? icons[category]
                            : const FaIcon(FontAwesomeIcons.tags,
                                color: AppColours.textColor),
                      ),
                      title: Text(
                        category,
                        style: GoogleFonts.dmSans(
                          fontSize: 20,
                          fontWeight: FontWeight.w600,
                          color: AppColours.textColor,
                        ),
                      ),
                      subtitle: Text(
                        "$selectedMonth, $selectedYear",
                        style: GoogleFonts.dmSans(
                          fontSize: 14,
                          fontWeight: FontWeight.w400,
                          color: AppColours.textColor,
                        ),
                      ),
                      trailing: Text(
                        "\$${total.toStringAsFixed(2)}",
                        style: GoogleFonts.dmSans(
                          fontSize: 20,
                          fontWeight: FontWeight.w600,
                          color: AppColours.textColor,
                        ),
                      ),
                      onTap: () => _showExpensesForCategory(category, expenses),
                    ),
                  );
                },
              ),
            ],
          ),
        ),
      ),
    );
  }
}
