import 'package:fl_chart/fl_chart.dart';
import 'package:flutter/material.dart';
import 'package:font_awesome_flutter/font_awesome_flutter.dart';
import 'package:google_fonts/google_fonts.dart';

class Expense {
  final String name;
  final double amount;

  Expense(this.name, this.amount);
}

class Category extends StatefulWidget {
  final String categoryName;
  final IconData iconData;
  final List<Expense> expenses;

  Category({
    Key? key,
    required this.categoryName,
    required this.iconData,
    List<Expense>? expenses,
  }) : expenses = expenses ?? [], super(key: key);

  @override
  _CategoryState createState() => _CategoryState();
}

class _CategoryState extends State<Category> {
  double get totalAmount => widget.expenses.fold(0, (sum, item) => sum + item.amount);

  void _showAddExpenseDialog() {
    String expenseName = '';
    double amount = 0.0;

    showDialog(
      context: context,
      builder: (BuildContext context) {
        return AlertDialog(
          title: Text(
            'Add New Expense',
            style: GoogleFonts.spectral(
              fontSize: 30,
              fontWeight: FontWeight.w300,
              color: Colors.black87,
            ),
          ),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextField(
                style: GoogleFonts.spectral(
                  fontSize: 20,
                  fontWeight: FontWeight.w300,
                  color: Colors.black87,
                ),
                onChanged: (value) {
                  expenseName = value;
                },
                decoration: InputDecoration(
                  labelText: 'Expense Name',
                  labelStyle: GoogleFonts.spectral(
                    fontSize: 20,
                    fontWeight: FontWeight.w300,
                    color: Colors.black87,
                  ),
                ),
              ),
              TextField(
                style: GoogleFonts.spectral(
                  fontSize: 20,
                  fontWeight: FontWeight.w300,
                  color: Colors.black87,
                ),
                keyboardType: TextInputType.number,
                onChanged: (value) {
                  amount = double.parse(value);
                },
                decoration: InputDecoration(
                  labelText: 'Amount',
                  labelStyle: GoogleFonts.spectral(
                    fontSize: 20,
                    fontWeight: FontWeight.w300,
                    color: Colors.black87,
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
                  color: Colors.black87,
                ),
              ),
              onPressed: () {
                Navigator.of(context).pop();
              },
            ),
            TextButton(
              child: Text(
                'Add',
                style: GoogleFonts.spectral(
                  fontSize: 20,
                  fontWeight: FontWeight.w300,
                  color: Colors.black87,
                ),
              ),
              onPressed: () {
                if (expenseName.isNotEmpty && amount > 0) {
                  setState(() {
                    widget.expenses.add(Expense(expenseName, amount));
                  });
                  Navigator.of(context).pop();
                }
              },
            ),
          ],
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: _showAddExpenseDialog,
      child: Padding(
        padding: const EdgeInsets.fromLTRB(18, 5, 18, 5),
        child: Card(
          child: Padding(
            padding: const EdgeInsets.fromLTRB(20, 10, 20, 10),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Icon(widget.iconData, color: Colors.black, size: 20),
                    SizedBox(width: 10),
                    Expanded(
                      child: Text(
                        widget.categoryName,
                        style: GoogleFonts.spectral(
                          fontSize: 20,
                          fontWeight: FontWeight.w300,
                          color: Colors.black87,
                        ),
                      ),
                    ),
                    Text(
                      "\$${totalAmount.toStringAsFixed(2)}",
                      style: GoogleFonts.spectral(
                        fontSize: 20,
                        fontWeight: FontWeight.w300,
                        color: Colors.black87,
                      ),
                    ),
                  ],
                ),
                SizedBox(height: 10),
                Column(
                  children: widget.expenses.map((expense) {
                    return Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Text(
                          expense.name,
                          style: GoogleFonts.spectral(
                            fontSize: 20,
                            fontWeight: FontWeight.w300,
                            color: Colors.black87,
                          ),
                        ),
                        Text(
                          "\$${expense.amount.toStringAsFixed(2)}",
                          style: GoogleFonts.spectral(
                            fontSize: 20,
                            fontWeight: FontWeight.w300,
                            color: Colors.black87,
                          ),
                        ),
                      ],
                    );
                  }).toList(),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class Budget extends StatefulWidget {
  const Budget({super.key});

  @override
  State<Budget> createState() => _BudgetState();
}

class _BudgetState extends State<Budget> {
  int _monthlyBudget = 800;
  List<Category> _categories = [
    Category(
      categoryName: "Travel and Transport",
      iconData: FontAwesomeIcons.car,
      expenses: [Expense("Car Insurance", 350.0)],
    ),
  ];

  void _showAddCategoryDialog() {
    String categoryName = '';

    showDialog(
      context: context,
      builder: (BuildContext context) {
        return AlertDialog(
          title: Text(
            'Add New Category',
            style: GoogleFonts.spectral(
              fontSize: 30,
              fontWeight: FontWeight.w300,
              color: Colors.black87,
            ),
          ),
          content: TextField(
            style: GoogleFonts.spectral(
              fontSize: 20,
              fontWeight: FontWeight.w300,
              color: Colors.black87,
            ),
            onChanged: (value) {
              categoryName = value;
            },
            decoration: InputDecoration(
              labelText: 'Category Name',
              labelStyle: GoogleFonts.spectral(
                fontSize: 20,
                fontWeight: FontWeight.w300,
                color: Colors.black87,
              ),
            ),
          ),
          actions: [
            TextButton(
              child: Text(
                'Cancel',
                style: GoogleFonts.spectral(
                  fontSize: 20,
                  fontWeight: FontWeight.w300,
                  color: Colors.black87,
                ),
              ),
              onPressed: () {
                Navigator.of(context).pop();
              },
            ),
            TextButton(
              child: Text(
                'Add',
                style: GoogleFonts.spectral(
                  fontSize: 20,
                  fontWeight: FontWeight.w300,
                  color: Colors.black87,
                ),
              ),
              onPressed: () {
                if (categoryName.isNotEmpty) {
                  setState(() {
                    _categories.add(Category(
                      categoryName: categoryName,
                      iconData: FontAwesomeIcons.moneyBill,
                      expenses: [],
                    ));
                  });
                  Navigator.of(context).pop();
                }
              },
            ),
          ],
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        toolbarHeight: 70,
        title: Text(
          'Budget',
          style: GoogleFonts.dmSans(
            fontSize: 35,
            fontWeight: FontWeight.w800,
          ),
        ),
        backgroundColor: const Color.fromRGBO(230, 242, 232, 1),
      ),
      backgroundColor: const Color.fromRGBO(230, 242, 232, 1),
      body: SingleChildScrollView(
        child: Column(
          children: [
            Padding(
              padding: const EdgeInsets.all(20),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  SizedBox(
                    width: 125,
                    child: Card(
                      elevation: 5,
                      child: Row(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Text(
                            '  Today',
                            style: GoogleFonts.spectral(
                              fontSize: 20,
                              fontWeight: FontWeight.w300,
                              color: Colors.black87,
                            ),
                          ),
                          const IconButton(
                            onPressed: null,
                            icon: FaIcon(
                              FontAwesomeIcons.caretDown,
                              color: Colors.black,
                              size: 20,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                  Text(
                    "Monthly Budget:  \$ $_monthlyBudget",
                    style: GoogleFonts.spectral(),
                  )
                ],
              ),
            ),
            
            Column(
              children: _categories,
            ),

            
          ],
        ),
      ),
      floatingActionButton: FloatingActionButton(
        
        onPressed: _showAddCategoryDialog,
        child: Icon(Icons.add),
      ),
    );
  }
}
