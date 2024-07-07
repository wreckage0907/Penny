import 'package:flutter/material.dart';
import 'package:font_awesome_flutter/font_awesome_flutter.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:syncfusion_flutter_charts/charts.dart';

class Expense {
  final String name;
  final double amount;

  Expense(this.name, this.amount);
}

class CategoryExpense {
  final String category;
  final Expense expense;

  CategoryExpense(this.category, this.expense);
}

class Category extends StatefulWidget {
  final String categoryName;
  final IconData iconData;
  final List<Expense> expenses;
  final Function(Expense) onExpenseAdded;

  Category({
    Key? key,
    required this.categoryName,
    required this.iconData,
    List<Expense>? expenses,
    required this.onExpenseAdded,
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
                  final newExpense = Expense(expenseName, amount);
                  setState(() {
                    widget.expenses.add(newExpense);
                  });
                  widget.onExpenseAdded(newExpense);  
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
                    const SizedBox(width: 10),
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
                const SizedBox(height: 10),
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
  final int _monthlyBudget = 800;
  final List<Category> _categories = [];

  @override
  void initState() {
    super.initState();
    _categories.add(Category(
      categoryName: "Travel and Transport",
      iconData: FontAwesomeIcons.car,
      expenses: [Expense("Car Insurance", 350.0)],
      onExpenseAdded: _handleExpenseAdded,
    ));
    _categories.add(Category(
      categoryName: "Utility Charges",
      iconData: FontAwesomeIcons.lightbulb,
      expenses: [Expense("Electricity", 100.0)],
      onExpenseAdded: _handleExpenseAdded,
    ));
    _categories.add(Category(
      categoryName: "Food",
      iconData: FontAwesomeIcons.utensils,
      expenses: [Expense("Groceries", 200.0)],
      onExpenseAdded: _handleExpenseAdded,
    ));
    _categories.add(Category(
      categoryName: "Subscriptions",
      iconData: FontAwesomeIcons.tv,
      expenses: [Expense("Youtube", 100.0), Expense("Spotify", 150.0), Expense("Hotstar", 150.0), Expense("Prime", 250.0)],
      onExpenseAdded: _handleExpenseAdded,
    ));
  }

  void _handleExpenseAdded(Expense expense) {
    setState(() {});
  }

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
                      expenses: const [],
                      onExpenseAdded: _handleExpenseAdded, 
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
    List<CategoryExpense> allCategoryExpenses = _categories.expand((category) => 
      category.expenses.map((expense) => CategoryExpense(category.categoryName, expense))
    ).toList();    
    double total = allCategoryExpenses.fold(0, (sum, item) => sum + item.expense.amount);

    final TooltipBehavior tooltipBehavior = TooltipBehavior(
      enable: true,
      format: 'point.x : \$point.y',
      textStyle: const TextStyle(color: Colors.white),
      color: Colors.black.withOpacity(0.7),
      duration: 3000,
      canShowMarker: true,
      shared: false,
      animationDuration: 1000,
      builder: (dynamic data, dynamic point, dynamic series, int pointIndex, int seriesIndex) {
        CategoryExpense categoryExpense = data as CategoryExpense;
        return Container(
          padding: const EdgeInsets.all(10),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(categoryExpense.category, style: const TextStyle(fontWeight: FontWeight.bold, color: Colors.white)),
              Text('${categoryExpense.expense.name}: \$${categoryExpense.expense.amount.toStringAsFixed(2)}', style: const TextStyle(color: Colors.white)),
            ],
          ),
        );
      },
    );

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
              padding: const EdgeInsets.only(left: 20, right: 20, top: 20),
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
                    style: GoogleFonts.spectral(
                      fontSize: 18,
                      fontWeight: FontWeight.w600,
                    ),
                  )
                ],
              ),
            ),
            SfCircularChart(
              margin: const EdgeInsets.all(0),
              tooltipBehavior: tooltipBehavior,
              series: <CircularSeries> [
                DoughnutSeries <CategoryExpense, String> (
                  dataSource: allCategoryExpenses,
                  xValueMapper: (CategoryExpense data, _) => data.expense.name,
                  yValueMapper: (CategoryExpense data, _) => data.expense.amount,
                  radius: '90%',
                  innerRadius: '60%',
                  enableTooltip: true,
                  strokeWidth: 5,
                  strokeColor: const Color.fromRGBO(230, 242, 232, 1),
                  pointRenderMode: PointRenderMode.segment,
                ),
              ],
              annotations: <CircularChartAnnotation> [
                CircularChartAnnotation(
                   widget: Text(
                    "\$ ${total.toStringAsFixed(2)}",
                    style: GoogleFonts.spectral(
                      fontSize: 24,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                )
              ],
            ),
            Column(
              children: _categories,
            ),
          ],
        ),
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: _showAddCategoryDialog,
        child: const Icon(Icons.add),
      ),
    );
  }
}