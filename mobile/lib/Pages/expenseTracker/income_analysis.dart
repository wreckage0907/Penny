import 'package:dropdown_button2/dropdown_button2.dart';
import 'package:fl_chart/fl_chart.dart';
import 'package:flutter/material.dart';
import 'package:font_awesome_flutter/font_awesome_flutter.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:mobile/consts/app_colours.dart';
import 'package:mobile/Pages/expenseTracker/expense_service.dart';
import 'package:firebase_auth/firebase_auth.dart';
import 'package:mobile/consts/backend_url.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:smooth_page_indicator/smooth_page_indicator.dart';

class IncomeAnalysis extends StatefulWidget {
  const IncomeAnalysis({super.key});

  @override
  State<IncomeAnalysis> createState() => _IncomeAnalysisState();
}

class _IncomeAnalysisState extends State<IncomeAnalysis> {
  ExpenseService? _expenseService;
  double incomeAmount = 0;
  double essentials = 0;
  double wants = 0;
  double savings = 0;
  int selectedYear = DateTime.now().year;
  String selectedMonth = DateTime.now().month.toString().padLeft(2, '0');
  bool isLoading = true;
  String? username;
  Map<String, List<Map<String, dynamic>>> expenses = {};

  final _controller = PageController();

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

  @override
  void initState() {
    super.initState();
    selectedMonth = monthNames[DateTime.now().month - 1];
    _loadUsername().then((_) {
      _initializeExpenseService();
      _loadIncomeData();
    });
  }

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
    }
  }

  void _initializeExpenseService() {
    if (username != null) {
      _expenseService =
          ExpenseService(baseUrl: backendUrl(), userId: username);
    }
  }

  Future<void> _loadIncomeData() async {
    setState(() {
      isLoading = true;
    });
    try {
      Map<String, dynamic> incomeData = await _expenseService!.getIncome(
        year: selectedYear,
        month: selectedMonth,
      );
      Map<String, dynamic> expenseData = await _expenseService!.getExpenses(
        year: selectedYear,
        month: selectedMonth,
      );
      setState(() {
        incomeAmount =
            incomeData[selectedYear.toString()]?[selectedMonth] ?? 0.0;
        expenses = (expenseData[selectedYear.toString()]?[selectedMonth]
                    ?['categories'] as Map<String, dynamic>?)
                ?.map((key, value) => MapEntry(key,
                    (value as List<dynamic>).cast<Map<String, dynamic>>())) ??
            {};
        _calculateSplit();
        isLoading = false;
      });
    } catch (e) {
      setState(() {
        incomeAmount = 0.0;
        expenses = {};
        _calculateSplit();
        isLoading = false;
      });
    }
  }

  void _calculateSplit() {
    essentials = incomeAmount * 0.5;
    wants = incomeAmount * 0.3;
    savings = incomeAmount * 0.2;
  }

  final List<Color> colors = [
    Colors.pink,
    Colors.deepPurple,
    Colors.deepOrange,
    Colors.blueAccent,
    Colors.teal,
    Colors.yellow,
  ];

  List<String> investmentTypes = [
    'Fixed Deposits',
    'Mutual Funds',
    'Stocks',
    'Gold'
  ];
  List<double> annualReturns = [0.03, 0.06, 0.10, 0.12, 0.10, 0.075, 0.065];
  int years = 5;

  List<List<double>> calculateInvestmentValues() {
    List<List<double>> investmentValues = [];
    for (int i = 0; i < investmentTypes.length; i++) {
      List<double> values = [savings];
      for (int year = 1; year <= years; year++) {
        double newValue = values.last * (1 + annualReturns[i]);
        values.add(newValue);
      }
      investmentValues.add(values);
    }
    return investmentValues;
  }

  List<PieChartSectionData> getSections(String category) {
    List<PieChartSectionData> sections = [];
    double total = 0;

    for (var expense in expenses[category] ?? []) {
      total += expense['amount'] as double;
    }

    if (total.isFinite && total > 0) {
      for (var i = 0; i < (expenses[category] ?? []).length; i++) {
        var expense = expenses[category]![i];
        double percentage = (expense['amount'] as double) / total;
        sections.add(
          PieChartSectionData(
            color: colors[i % colors.length],
            value: percentage * 100,
            title: '',
            radius: 10,
          ),
        );
      }
    }
    return sections;
  }

  List<Widget> get widgetsList {
    if (expenses.isEmpty) {
      return [
        Card(
          elevation: 1.5,
          color: AppColours.backgroundColor,
          shape:
              RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
          child: Padding(
            padding: const EdgeInsets.all(16.0),
            child: Center(
              child: Text(
                'No data available for $selectedMonth $selectedYear',
                style: GoogleFonts.dmSans(
                  fontSize: 18,
                  fontWeight: FontWeight.bold,
                  color: AppColours.textColor,
                ),
              ),
            ),
          ),
        )
      ];
    }

    return expenses.keys.map((category) {
      double totalAmount = expenses[category]?.fold(0.0,
              (sum, expense) => sum! + (expense['amount'] as double? ?? 0.0)) ??
          0.0;

      return Card(
        elevation: 1.5,
        color: AppColours.backgroundColor,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
        child: Padding(
          padding: const EdgeInsets.all(16.0),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Expanded(
                child: PieChart(
                  PieChartData(
                    sectionsSpace: 3,
                    centerSpaceRadius: 80,
                    sections: getSections(category),
                  ),
                ),
              ),
              const SizedBox(width: 20),
              Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    category,
                    style: GoogleFonts.dmSans(
                      fontSize: 24,
                      fontWeight: FontWeight.bold,
                      color: AppColours.textColor,
                    ),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    '\$${totalAmount.toStringAsFixed(2)}',
                    style: GoogleFonts.dmSans(
                      fontSize: 22,
                      fontWeight: FontWeight.w600,
                      color: AppColours.textColor,
                    ),
                  ),
                  const SizedBox(height: 16),
                  ...expenses[category]!.asMap().entries.map<Widget>((entry) {
                    int index = entry.key;
                    var expense = entry.value;
                    return Padding(
                      padding: const EdgeInsets.symmetric(vertical: 4.0),
                      child: Row(
                        children: [
                          Container(
                            height: 10,
                            width: 10,
                            decoration: BoxDecoration(
                                color: colors[index % colors.length],
                                shape: BoxShape.circle),
                          ),
                          const SizedBox(width: 10),
                          Text(
                            '${expense['name']}',
                            style: GoogleFonts.dmSans(
                              fontSize: 16,
                              fontWeight: FontWeight.w600,
                              color: AppColours.textColor,
                            ),
                          ),
                        ],
                      ),
                    );
                  }).toList(),
                ],
              ),
            ],
          ),
        ),
      );
    }).toList();
  }

  Widget buildInvestmentBarChart() {
    List<List<double>> investmentValues = calculateInvestmentValues();
    return SizedBox(
      height: 300,
      child: BarChart(
        BarChartData(
          gridData: const FlGridData(show: false),
          alignment: BarChartAlignment.spaceBetween,
          maxY:
              investmentValues.expand((e) => e).reduce((a, b) => a > b ? a : b),
          titlesData: FlTitlesData(
            leftTitles: AxisTitles(
              sideTitles: SideTitles(
                showTitles: true,
                reservedSize: 40,
                getTitlesWidget: (value, meta) {
                  return Text('\$${value.toInt()}',
                      style: GoogleFonts.dmSans(
                          color: AppColours.textColor, fontSize: 10));
                },
              ),
            ),
            bottomTitles: AxisTitles(
              sideTitles: SideTitles(
                showTitles: true,
                getTitlesWidget: (value, meta) {
                  return Text(value.toInt().toString(),
                      style: GoogleFonts.dmSans(
                          color: AppColours.textColor, fontSize: 12,
                          fontWeight: FontWeight.bold));
                },
              ),
            ),
            rightTitles: const AxisTitles(sideTitles: SideTitles(showTitles: false)),
            topTitles: const AxisTitles(sideTitles: SideTitles(showTitles: false)),
          ),
          borderData: FlBorderData(show: false),
          barGroups: List.generate(years + 1, (yearIndex) {
            return BarChartGroupData(
              x: yearIndex,
              barRods: List.generate(investmentTypes.length, (typeIndex) {
                return BarChartRodData(
                  toY: investmentValues[typeIndex][yearIndex],
                  color: colors[typeIndex % colors.length],
                  width: 8,
                );
              }),
            );
          }),
        ),
      ),
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
            children: [
              Card(
                color: AppColours.backgroundColor,
                child: Padding(
                  padding: const EdgeInsets.all(16.0),
                  child: Column(
                    children: [
                      Text(
                        "Investment Returns Over the Years",
                        style: GoogleFonts.dmSans(
                          fontSize: 18,
                          fontWeight: FontWeight.w800,
                          color: AppColours.textColor,
                        ),
                      ),
                      const SizedBox(height: 16),
                      buildInvestmentBarChart(),
                      const SizedBox(height: 16),
                      Wrap(
                        spacing: 12,
                        runSpacing: 8,
                        children:
                            List.generate(investmentTypes.length, (index) {
                          return Row(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              Container(
                                width: 8,
                                height: 8,
                                color: colors[index % colors.length],
                              ),
                              const SizedBox(width: 4),
                              Text(
                                investmentTypes[index],
                                style: GoogleFonts.dmSans(
                                  fontSize: 12,
                                  fontWeight: FontWeight.bold,
                                  color: AppColours.textColor,
                                ),
                              ),
                            ],
                          );
                        }),
                      ),
                    ],
                  ),
                ),
              ),
              Card(
                color: AppColours.backgroundColor,
                child: SizedBox(
                  width: double.infinity,
                  child: Padding(
                    padding: const EdgeInsets.all(12),
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
                                        borderRadius:
                                            BorderRadius.circular(35))),
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
                                        borderRadius:
                                            BorderRadius.circular(35))),
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
                            : Row(
                                mainAxisAlignment:
                                    MainAxisAlignment.spaceAround,
                                children: [
                                  _buildSplitColumn(
                                      "Essentials", essentials, Colors.blue),
                                  Text(
                                    "/",
                                    style: GoogleFonts.dmSans(
                                      fontSize: 20,
                                      fontWeight: FontWeight.w500,
                                      color:
                                          AppColours.textColor.withOpacity(0.8),
                                    ),
                                  ),
                                  _buildSplitColumn("Wants", wants, Colors.red),
                                  Text(
                                    "/",
                                    style: GoogleFonts.dmSans(
                                      fontSize: 20,
                                      fontWeight: FontWeight.w500,
                                      color:
                                          AppColours.textColor.withOpacity(0.8),
                                    ),
                                  ),
                                  _buildSplitColumn(
                                      "Savings", savings, Colors.green),
                                ],
                              ),
                        const SizedBox(height: 15),
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
                                  flex: (essentials * 100).round(),
                                  child: Container(
                                    height: 10,
                                    decoration: BoxDecoration(
                                        color: Colors.blue,
                                        borderRadius: BorderRadius.circular(6)),
                                  ),
                                ),
                                const SizedBox(width: 10),
                                Flexible(
                                  flex: (wants * 100).round(),
                                  child: Container(
                                    height: 10,
                                    decoration: BoxDecoration(
                                        color: Colors.red,
                                        borderRadius: BorderRadius.circular(6)),
                                  ),
                                ),
                                const SizedBox(width: 10),
                                Flexible(
                                  flex: (savings * 100).round(),
                                  child: Container(
                                    height: 10,
                                    decoration: BoxDecoration(
                                        color: Colors.green,
                                        borderRadius: BorderRadius.circular(6)),
                                  ),
                                ),
                              ],
                            )
                          ],
                        )
                      ],
                    ),
                  ),
                ),
              ),
              SizedBox(
                height: 250,
                child: PageView(
                  controller: _controller,
                  children: widgetsList,
                ),
              ),
              const SizedBox(
                height: 12,
              ),
              SmoothPageIndicator(
                controller: _controller,
                count: widgetsList.length,
                effect: const SwapEffect(
                  activeDotColor: AppColours.buttonColor,
                  dotColor: AppColours.cardColor,
                  dotHeight: 5,
                  dotWidth: 5,
                ),
              )
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildSplitColumn(String title, double amount, Color color) {
    return Column(
      children: [
        Text(
          title,
          style: GoogleFonts.dmSans(
            fontSize: 16,
            fontWeight: FontWeight.w600,
            color: AppColours.textColor,
          ),
        ),
        Text(
          "\$${amount.toStringAsFixed(2)}",
          style: GoogleFonts.dmSans(
            fontSize: 18,
            fontWeight: FontWeight.w600,
            color: color,
          ),
        )
      ],
    );
  }
}
