import 'dart:convert';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:font_awesome_flutter/font_awesome_flutter.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:mobile/consts/app_colours.dart';
import 'package:mobile/consts/loading_widgets.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:syncfusion_flutter_charts/charts.dart';
// ignore: depend_on_referenced_packages
import 'package:http/http.dart' as http;

class Year {
  final int year;
  final List<Month> months;

  Year({required this.year, required this.months});
}

class Month {
  final String month;
  final int monthlyBudget;
  final int amountSpent;
  final List<ExpenseCategory> categories;

  Month(
      {required this.month,
      required this.monthlyBudget,
      required this.amountSpent,
      required this.categories});
}

class ExpenseCategory {
  final String categoryTitle;
  final int totalBudget;
  final List<Expense> expenses;

  ExpenseCategory(
      {required this.categoryTitle,
      required this.totalBudget,
      required this.expenses});
}

class Expense {
  final String name;
  final int amountSpent;

  Expense({required this.name, required this.amountSpent});
}

class ExpenseTracker extends StatefulWidget {
  const ExpenseTracker({super.key});

  @override
  State<ExpenseTracker> createState() => _ExpenseTrackerState();
}

class _ExpenseTrackerState extends State<ExpenseTracker> {
  String? username;
  int selectedMonth = DateTime.now().month;
  int selectedYear = DateTime.now().year;

  final List<String> months = [
    "January",
    "February",
    "March",
    "April",
    "May",
    "June",
    "July",
    "August",
    "September",
    "October",
    "November",
    "December"
  ];

  late List<ExpenseCategory> _categories;
  late final TooltipBehavior _tooltipBehavior = TooltipBehavior(
    enable: true,
    format: 'point.x : \$point.y',
    textStyle: const TextStyle(color: Colors.white),
    color: Colors.black.withOpacity(0.7),
    duration: 1500,
    canShowMarker: true,
    shared: false,
    animationDuration: 700,
    builder: (dynamic data, dynamic point, dynamic series, int pointIndex,
        int seriesIndex) {
      Expense expense = data as Expense;
      String categoryTitle = _categories
          .firstWhere((category) => category.expenses.contains(expense))
          .categoryTitle;

      return Container(
        padding: const EdgeInsets.all(10),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(categoryTitle,
                style: const TextStyle(
                    fontWeight: FontWeight.bold, color: Colors.white)),
            Text('${expense.name}: \$${expense.amountSpent.toStringAsFixed(2)}',
                style: const TextStyle(color: Colors.white)),
          ],
        ),
      );
    },
  );

  @override
  void initState() {
    super.initState();
    _loadUsername();
    _categories = [];
  }

  Future<void> _loadUsername() async {
    final prefs = await SharedPreferences.getInstance();
    setState(() {
      username = prefs.getString('username');
    });
  }

  Future<Map<String, dynamic>> getData() async {
    if (username == null) {
      await _loadUsername();
    }
    try {
      final response = await http.get(
        Uri.parse("https://penny-uts7.onrender.com/expenses/$username"),
      );

      if (response.statusCode == 200) {
        Map<String, dynamic> data = jsonDecode(response.body);
        return data;
      } else {
        print('Failed to load data. Status code: ${response.statusCode}');
        print('Response body: ${response.body}');
        return {};
      }
    } catch (e) {
      print('Error occurred: $e');
      return {};
    }
  }

  Future<void> editExpense(ExpenseCategory category, int index,
      String newExpenseName, int newExpenseAmount) async {
    try {
      final response = await http.put(
        Uri.parse(
                'https://penny-uts7.onrender.com/expenses/$username/year/$selectedYear/month/${Uri.encodeComponent(months[selectedMonth - 1])}/category/${Uri.encodeComponent(category.categoryTitle)}/subcategory/${Uri.encodeComponent(category.expenses[index].name)}')
            .replace(queryParameters: {
          'new_sub_category': newExpenseName,
          'new_amount_spent': newExpenseAmount.toString(),
        }),
        headers: <String, String>{
          'Content-Type': 'application/x-www-form-urlencoded',
        },
      );

      if (response.statusCode == 200) {
        print('Expense updated successfully');
        setState(() {
          category.expenses[index] =
              Expense(name: newExpenseName, amountSpent: newExpenseAmount);
        });
      } else {
        print('Failed to update expense. Status code: ${response.statusCode}');
        print('Response body: ${response.body}');
        throw Exception('Failed to update expense: ${response.body}');
      }
    } catch (e) {
      print('Error updating expense: $e');
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Failed to update expense: $e')),
      );
    }
  }

  Future<void> addExpense(
      String newExpense, int newAmount, String category) async {
    final response = await http.post(
      Uri.parse(
              'https://penny-uts7.onrender.com/expenses/$username/year/$selectedYear/month/${months[selectedMonth - 1]}/category/$category')
          .replace(queryParameters: {
        'subcategory': newExpense,
        'amount_spent': newAmount.toString(),
      }),
      headers: <String, String>{
        'Content-Type': 'application/x-www-form-urlencoded',
      },
    );

    if (response.statusCode == 200) {
      print('User data posted successfully');
      setState(() {
        _categories
            .firstWhere((element) => element.categoryTitle == category)
            .expenses
            .add(Expense(name: newExpense, amountSpent: newAmount));
      });
    } else {
      print('Failed to post user data. Status code: ${response.statusCode}');
      print('Response body: ${response.body}');
      throw Exception('Failed to post user data');
    }
  }

  Future<void> addCategory(String newCategory) async {
    try {
      final response = await http.post(
        Uri.parse(
            'https://penny-uts7.onrender.com/expenses/$username/year/$selectedYear/month/${months[selectedMonth - 1]}/category'),
        headers: <String, String>{
          'Content-Type': 'application/json; charset=UTF-8',
        },
        body: jsonEncode(<String, String>{
          'category': newCategory,
        }),
      );

      if (response.statusCode == 200) {
        print('Category added successfully');
        setState(() {
          _categories.add(ExpenseCategory(
              categoryTitle: newCategory, totalBudget: 0, expenses: []));
        });
      } else {
        print('Failed to add category: ${response.statusCode}');
        print('Response body: ${response.body}');
        throw Exception('Failed to add category');
      }
    } catch (e) {
      print('Error adding category: $e');
    }
  }

  void _showAddCategoryDialog() {
    String categoryName = '';

    showDialog(
        context: context,
        builder: (BuildContext context) {
          return AlertDialog(
            backgroundColor: AppColours.cardColor,
            title: Text(
              'Add New Category',
              style: GoogleFonts.darkerGrotesque(
                fontSize: 30,
                fontWeight: FontWeight.w500,
                color: AppColours.textColor,
              ),
            ),
            content: TextField(
              style: GoogleFonts.spectral(
                fontSize: 20,
                fontWeight: FontWeight.w300,
                color: AppColours.textColor,
              ),
              onChanged: (value) {
                categoryName = value;
              },
              decoration: InputDecoration(
                labelText: 'Category Name',
                labelStyle: GoogleFonts.spectral(
                  fontSize: 20,
                  fontWeight: FontWeight.w300,
                  color: AppColours.textColor,
                ),
              ),
            ),
            actions: [
              TextButton(
                  onPressed: () {
                    addCategory(categoryName);
                    Navigator.of(context).pop();
                    ScaffoldMessenger.of(context).showSnackBar(
                      const SnackBar(content: Text('Category added successfully')),
                    );
                  },
                  child: Text(
                    'Add',
                    style: GoogleFonts.spectral(
                      fontSize: 20,
                      fontWeight: FontWeight.w300,
                      color: AppColours.textColor,
                    ),
                  )),
              TextButton(
                onPressed: Navigator.of(context).pop,
                child: Text(
                  'Cancel',
                  style: GoogleFonts.spectral(
                    fontSize: 20,
                    fontWeight: FontWeight.w300,
                    color: AppColours.textColor,
                  ),
                ),
              )
            ],
          );
        });
  }

  void _showEditExpenseDialog(ExpenseCategory category, int index) {
    String expenseName = category.expenses[index].name;
    double amount = category.expenses[index].amountSpent.toDouble();

    showDialog(
        context: context,
        builder: (BuildContext context) {
          return AlertDialog(
            backgroundColor: AppColours.cardColor,
            title: Text(
              'Edit Expense',
              style: GoogleFonts.spectral(
                fontSize: 30,
                fontWeight: FontWeight.w500,
                color: AppColours.textColor,
              ),
            ),
            content: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                TextField(
                  style: GoogleFonts.spectral(
                    fontSize: 20,
                    fontWeight: FontWeight.w300,
                    color: AppColours.textColor,
                  ),
                  controller: TextEditingController(text: expenseName),
                  onChanged: (value) {
                    expenseName = value;
                  },
                  decoration: InputDecoration(
                    labelText: 'Expense Name',
                    labelStyle: GoogleFonts.spectral(
                      fontSize: 20,
                      fontWeight: FontWeight.w300,
                      color: AppColours.textColor,
                    ),
                  ),
                ),
                TextField(
                  style: GoogleFonts.spectral(
                    fontSize: 20,
                    fontWeight: FontWeight.w300,
                    color: AppColours.textColor,
                  ),
                  controller: TextEditingController(text: amount.toString()),
                  keyboardType: TextInputType.number,
                  onChanged: (value) {
                    amount = double.tryParse(value) ?? amount;
                  },
                  decoration: InputDecoration(
                    labelText: 'Amount',
                    labelStyle: GoogleFonts.spectral(
                      fontSize: 20,
                      fontWeight: FontWeight.w300,
                      color: AppColours.textColor,
                    ),
                  ),
                ),
              ],
            ),
            actions: [
              TextButton(
                  child: Text(
                    'Cancel',
                    style: GoogleFonts.spectral(
                      fontSize: 20,
                      fontWeight: FontWeight.w300,
                      color: AppColours.textColor,
                    ),
                  ),
                  onPressed: () {
                    Navigator.of(context).pop();
                  }),
              TextButton(
                onPressed: () async {
                  Navigator.of(context).pop();
                  await editExpense(
                      category, index, expenseName, amount.toInt());
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(content: Text('Expense updated successfully')),
                  );
                },
                child: Text(
                  'Save',
                  style: GoogleFonts.spectral(
                    fontSize: 20,
                    fontWeight: FontWeight.w300,
                    color: AppColours.textColor,
                  ),
                ),
              )
            ],
          );
        });
  }

  void _showAddExpenseDialog(ExpenseCategory category) {
    String expenseName = '';
    double amount = 0.0;

    showDialog(
      context: context,
      builder: (BuildContext context) {
        return AlertDialog(
          backgroundColor: AppColours.cardColor,
          title: Text(
            "Add New Expense",
            style: GoogleFonts.spectral(
              fontSize: 28,
              fontWeight: FontWeight.w500,
              color: AppColours.textColor,
            ),
          ),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextField(
                style: GoogleFonts.spectral(
                  fontSize: 20,
                  fontWeight: FontWeight.w300,
                  color: AppColours.textColor,
                ),
                onChanged: (value) {
                  expenseName = value;
                },
                decoration: InputDecoration(
                  labelText: 'Expense Name',
                  labelStyle: GoogleFonts.spectral(
                    fontSize: 20,
                    fontWeight: FontWeight.w300,
                    color: AppColours.textColor,
                  ),
                ),
              ),
              TextField(
                style: GoogleFonts.spectral(
                  fontSize: 20,
                  fontWeight: FontWeight.w300,
                  color: AppColours.textColor,
                ),
                keyboardType: TextInputType.number,
                onChanged: (value) {
                  amount = double.tryParse(value) ?? 0.0;
                },
                decoration: InputDecoration(
                  labelText: 'Amount',
                  labelStyle: GoogleFonts.spectral(
                    fontSize: 20,
                    fontWeight: FontWeight.w300,
                    color: AppColours.textColor,
                  ),
                ),
              ),
            ],
          ),
          actions: [
            TextButton(
              child: Text(
                'Cancel',
                style: GoogleFonts.spectral(
                  fontSize: 20,
                  fontWeight: FontWeight.w300,
                  color: AppColours.textColor,
                ),
              ),
              onPressed: () {
                Navigator.of(context).pop();
              },
            ),
            TextButton(
              onPressed: () {
                addExpense(expenseName, amount.toInt(), category.categoryTitle);
                Navigator.of(context).pop();
              },
              child: Text(
                'Add',
                style: GoogleFonts.spectral(
                  fontSize: 20,
                  fontWeight: FontWeight.w300,
                  color: AppColours.textColor,
                ),
              ),
            )
          ],
        );
      },
    );
  }

  void _showMonthYearPicker() {
    int tempMonth = selectedMonth;
    int tempYear = selectedYear;

    showModalBottomSheet(
      backgroundColor: AppColours.backgroundColor,
        context: context,
        builder: (BuildContext context) {
          return StatefulBuilder(
            builder: (BuildContext context, StateSetter setModalState) {
              return Container(
                height: 300,
                child: Column(
                  children: [
                    Expanded(
                      child: Row(
                        children: [
                          Expanded(
                            child: CupertinoPicker(
                              magnification: 1.1,
                              scrollController: FixedExtentScrollController(
                                initialItem: tempMonth - 1,
                              ),
                              itemExtent: 32.0,
                              onSelectedItemChanged: (int index) {
                                setModalState(() {
                                  tempMonth = index + 1;
                                });
                              },
                              children: List<Widget>.generate(12, (int index) {
                                return Center(
                                  child: Text(
                                    months[index],
                                    style: GoogleFonts.darkerGrotesque(
                                      fontSize: 20,
                                      color: AppColours.textColor,
                                      fontWeight: FontWeight.w500,
                                    ),
                                  ),
                                );
                              }),
                            ),
                          ),
                          Expanded(
                            child: CupertinoPicker(
                              magnification: 1.1,
                              scrollController: FixedExtentScrollController(
                                initialItem: tempYear - 2000,
                              ),
                              itemExtent: 32.0,
                              onSelectedItemChanged: (int index) {
                                setModalState(() {
                                  tempYear = index + 2000;
                                });
                              },
                              children: List<Widget>.generate(50, (int index) {
                                return Center(
                                  child: Text(
                                    '${index + 2000}',
                                    style: GoogleFonts.darkerGrotesque(
                                      fontSize: 20,
                                      fontWeight: FontWeight.w500,
                                      color: AppColours.textColor
                                    ),
                                  ),
                                );
                              }),
                            ),
                          ),
                        ],
                      ),
                    ),
                    Padding(
                      padding: const EdgeInsets.only(right: 8.0, bottom: 12.0),
                      child: Row(
                        mainAxisAlignment: MainAxisAlignment.end,
                        children: [
                          TextButton(
                            onPressed: () {
                              Navigator.pop(context);
                            },
                            child: Text(
                              'Cancel',
                              style: GoogleFonts.darkerGrotesque(
                                fontSize: 22,
                                fontWeight: FontWeight.w600,
                                color: AppColours.textColor,
                              ),
                            ),
                          ),
                          TextButton(
                            onPressed: () {
                              setState(() {
                                selectedMonth = tempMonth;
                                selectedYear = tempYear;
                              });
                              Navigator.pop(context);
                            },
                            child: Text(
                              'OK',
                              style: GoogleFonts.darkerGrotesque(
                                fontSize: 22,
                                fontWeight: FontWeight.w600,
                                color: AppColours.textColor,
                              ),
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              );
            },
          );
        });
  }

  List<Year> processData(Map<String, dynamic> data) {
    List<Year> years = [];
    if (data.containsKey('years') && data['years'] is Map) {
      data['years'].forEach((yearString, yearData) {
        int year = int.parse(yearString);
        Year yearObject = Year(
          year: year,
          months: (yearData as List).map((monthData) {
            return Month(
              month: monthData['month'] ?? '',
              monthlyBudget: monthData['monthly budget'] ?? 0,
              amountSpent: monthData['amount spent'] ?? 0,
              categories:
                  (monthData['categories'] as List? ?? []).map((categoryData) {
                return ExpenseCategory(
                  categoryTitle: categoryData['category'] ?? '',
                  totalBudget: categoryData['total budget'] ?? 0,
                  expenses: (categoryData['sub categories'] as List? ?? [])
                      .map((subCategoryData) {
                    return Expense(
                      name: subCategoryData['name'] ?? '',
                      amountSpent: subCategoryData['amount_spent'] ?? 0,
                    );
                  }).toList(),
                );
              }).toList(),
            );
          }).toList(),
        );
        years.add(yearObject);
      });
    }
    return years;
  }

  @override
  Widget build(BuildContext context) {
    return FutureBuilder<Map<String, dynamic>>(
        future: getData(),
        builder: (context, snapshot) {
          if (snapshot.connectionState == ConnectionState.waiting) {
            return Scaffold(
              backgroundColor: AppColours.backgroundColor,
              body: Center(child: CustomLoadingWidgets.spinningCircle())
              );
          }
          if (snapshot.hasError) {
            return Center(child: Text('Error: ${snapshot.error}'));
          }
          if (!snapshot.hasData) {
            return const Center(child: Text('No data available'));
          }

          Map<String, dynamic>? data = snapshot.data!;

          List<Year> years = processData(data);

          _categories.clear();

          for (var year in years) {
            if (year.year == selectedYear) {
              for (var month in year.months) {
                if (month.month == months[selectedMonth - 1]) {
                  for (var category in month.categories) {
                    _categories.add(ExpenseCategory(
                      categoryTitle: category.categoryTitle,
                      totalBudget: category.totalBudget,
                      expenses: category.expenses,
                    ));
                  }
                }
              }
            }
          }

          return Scaffold(
            backgroundColor: AppColours.backgroundColor,
            appBar: AppBar(
              backgroundColor: AppColours.backgroundColor,
              toolbarHeight: 70,
              title: Text(
                'Expense Tracker',
                style: GoogleFonts.darkerGrotesque(
                  fontSize: 35,
                  fontWeight: FontWeight.w800,
                  color: AppColours.textColor
                ),
              ),
            ),
            body: Column(
              children: [
                Padding(
                  padding: const EdgeInsets.only(left: 20, right: 20, top: 20),
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Row(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Text(
                            '${months[selectedMonth - 1]} $selectedYear',
                            style: GoogleFonts.spectral(
                              fontSize: 20,
                              fontWeight: FontWeight.w300,
                              color: AppColours.textColor,
                            ),
                          ),
                          IconButton(
                            onPressed: _showMonthYearPicker,
                            icon: const FaIcon(
                              FontAwesomeIcons.caretDown,
                              color: AppColours.textColor,
                              size: 20,
                            ),
                          ),
                        ],
                      ),
                      Text(
                        "Monthly Budget: ",
                        style: GoogleFonts.spectral(
                          fontSize: 18,
                          fontWeight: FontWeight.w600,
                          color: AppColours.textColor
                        ),
                      )
                    ],
                  ),
                ),
                SfCircularChart(
                  tooltipBehavior: _tooltipBehavior,
                  series: <CircularSeries>[
                    DoughnutSeries<Expense, String>(
                      enableTooltip: true,
                      dataSource: _categories
                          .expand((category) => category.expenses)
                          .toList(),
                      xValueMapper: (Expense data, _) => data.name,
                      yValueMapper: (Expense data, _) => data.amountSpent,
                      innerRadius: "55%",
                      radius: "80%",
                      strokeColor: AppColours.backgroundColor,
                      strokeWidth: 3,
                    )
                  ],
                  annotations: <CircularChartAnnotation>[
                    CircularChartAnnotation(
                      widget: Container(
                        child: Text(
                          "\$${_categories.expand((category) => category.expenses).map((expense) => expense.amountSpent).reduce((a, b) => a + b).toString()}",
                          style: GoogleFonts.spectral(
                            fontSize: 20,
                            fontWeight: FontWeight.w600,
                            color: AppColours.textColor
                          ),
                        ),
                      ),
                    )
                  ],
                ),
                Expanded(
                  child: ListView.builder(
                    itemCount: _categories.length,
                    itemBuilder: (context, index) {
                      return Card(
                        color: AppColours.cardColor,
                        margin: const EdgeInsets.all(10),
                        child: Padding(
                          padding: const EdgeInsets.fromLTRB(18, 5, 18, 5),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Row(
                                mainAxisAlignment:
                                    MainAxisAlignment.spaceBetween,
                                children: [
                                  Row(
                                    children: [
                                      const Icon(
                                        Icons.car_crash,
                                        color: AppColours.textColor,
                                        size: 20,
                                      ),
                                      const SizedBox(width: 8),
                                      Text(_categories[index].categoryTitle,
                                          style: GoogleFonts.darkerGrotesque(
                                            fontSize: 20,
                                            fontWeight: FontWeight.w500,
                                            color: AppColours.textColor,
                                          )),
                                      Transform.translate(
                                          offset: const Offset(-12, -12),
                                          child: IconButton(
                                            onPressed: () {
                                              _showAddExpenseDialog(
                                                  _categories[index]);
                                            },
                                            icon: const Icon(
                                              Icons.add,
                                              color: AppColours.textColor,
                                              size: 20,
                                            ),
                                          )),
                                    ],
                                  ),
                                  Text(
                                    _categories[index]
                                        .expenses
                                        .map((expense) => expense.amountSpent)
                                        .reduce((a, b) => a + b)
                                        .toString(),
                                    style: GoogleFonts.darkerGrotesque(
                                      fontSize: 20,
                                      fontWeight: FontWeight.w500,
                                      color: AppColours.textColor,
                                    ),
                                  ),
                                ],
                              ),
                              Column(
                                children:
                                    _categories[index].expenses.map((expense) {
                                  return Row(
                                    mainAxisAlignment:
                                        MainAxisAlignment.spaceBetween,
                                    children: [
                                      if (expense.name != '')
                                        Row(
                                          children: [
                                            Text(
                                              expense.name,
                                              style: GoogleFonts.dmSans(
                                                fontSize: 18,
                                                fontWeight: FontWeight.w300,
                                                color: AppColours.textColor,
                                              ),
                                            ),
                                            Transform.translate(
                                                offset: const Offset(-12, -12),
                                                child: IconButton(
                                                    onPressed: () =>
                                                        _showEditExpenseDialog(
                                                            _categories[index],
                                                            _categories[index]
                                                                .expenses
                                                                .indexOf(
                                                                    expense)),
                                                    icon: const FaIcon(
                                                      FontAwesomeIcons.pencil,
                                                      size: 12,
                                                      color: AppColours.textColor,
                                                    )))
                                          ],
                                        ),
                                      if (expense.amountSpent.toString() != '0')
                                        Text(expense.amountSpent.toString()),
                                    ],
                                  );
                                }).toList(),
                              ),
                            ],
                          ),
                        ),
                      );
                    },
                  ),
                ),
              ],
            ),
            floatingActionButton: FloatingActionButton(
              backgroundColor: AppColours.buttonColor,
              onPressed: _showAddCategoryDialog,
              child: const Icon(
                Icons.add,
                color: AppColours.backgroundColor,
              ),
            ),
          );
        });
  }
}