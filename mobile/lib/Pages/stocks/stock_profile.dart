import 'dart:async';
import 'dart:convert';

import 'package:fl_chart/fl_chart.dart';
import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:mobile/consts/app_colours.dart';
import 'package:http/http.dart' as http;
import 'package:mobile/consts/toast_messages.dart';

class StockProfile extends StatefulWidget {
  const StockProfile({super.key});

  @override
  State<StockProfile> createState() => _StockProfileState();
}

class _StockProfileState extends State<StockProfile> {
  Map<String, double> userHoldings = {};
  Map<String, dynamic> stockDetails = {
    'AAPL': {
      'name': 'Apple INC.',
      'open': 453,
      'high': 232,
      'low': 323,
    },
    'DIS': {
      'name': 'The Walt Disney Company',
      'open': 453,
      'high': 232,
      'low': 323,
    },
    'SBUX': {
      'name': 'Starbucks Corporation',
      'open': 453,
      'high': 232,
      'low': 323,
    },
    'NKE': {
      'name': 'Nike INC.',
      'open': 453,
      'high': 232,
      'low': 323,
    },
    '^NSEI': {
      'name': 'NIFTY 50',
      'open': 453,
      'high': 232,
      'low': 323,
    },
    'TSLA': {
      'name': 'Tesla INC.',
      'open': 453,
      'high': 232,
      'low': 323,
    },
    'INTC': {
      'name': 'Intel Corporation',
      'open': 453,
      'high': 232,
      'low': 323,
    },
    'AMZN': {
      'name': 'Amazon.com INC.',
      'open': 453,
      'high': 232,
      'low': 323,
    },
    'GOOGL': {
      'name': 'Alphabet INC.',
      'open': 453,
      'high': 232,
      'low': 323,
    },
    'PFE': {
      'name': 'Pfizer INC.',
      'open': 453,
      'high': 232,
      'low': 323,
    },
  };
  List<String> stockName = [
    'AAPL',
    'DIS',
    'SBUX',
    'NKE',
    '^NSEI',
    'TSLA',
    'INTC',
    'AMZN',
    'GOOGL',
    'PFE'
  ];
  String selectedStock = 'AAPL';
  List<FlSpot> stockDataPoints = [];
  List<String> stockDates = ['1D', '1W', '1M', '3M', '6M', '1Y', '2Y', '3Y'];
  String selectedTimeRange = '1D';
  Timer? liveDataTimer;

  @override
  void initState() {
    super.initState();
    getLiveStockData();
    startLiveDataTimer();
  }

  @override
  void dispose() {
    liveDataTimer?.cancel();
    super.dispose();
  }

  void startLiveDataTimer() {
    liveDataTimer?.cancel();
    if (selectedTimeRange == '1D') {
      liveDataTimer = Timer.periodic(
          const Duration(minutes: 1), (Timer t) => getLiveStockData());
    }
  }

  Future<void> getLiveStockData() async {
    try {
      final response = await http.get(
        Uri.parse("http://10.0.2.2:8000/stocks/live?ticker=$selectedStock"),
      );

      if (response.statusCode == 200) {
        Map<String, dynamic> jsonData = jsonDecode(response.body);
        List<FlSpot> flPoints = [];

        jsonData.forEach((key, value) {
          final DateTime dateTime = DateTime.parse(key);
          final double price = value["Close"];
          flPoints
              .add(FlSpot(dateTime.millisecondsSinceEpoch.toDouble(), price));
        });

        setState(() {
          stockDataPoints = flPoints;
        });
      }
    } catch (e) {
      print("Error: $e");
    }
  }

  void _showBuyDialog() {
    double quantity = 0;
    showDialog(
      context: context,
      builder: (BuildContext context) {
        return AlertDialog(
          backgroundColor: AppColours.backgroundColor,
          title: Text(
            'Buy $selectedStock',
            style: GoogleFonts.dmSans(
                color: AppColours.textColor,
                fontSize: 28,
                fontWeight: FontWeight.w600),
          ),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(
                'Current Price: \$387',
                style: GoogleFonts.dmSans(
                  fontSize: 18,
                  fontWeight: FontWeight.w500,
                  color: AppColours.textColor,
                ),
              ),
              TextField(
                keyboardType: TextInputType.number,
                decoration: InputDecoration(
                    labelText: 'Quantity',
                    labelStyle: GoogleFonts.dmSans(
                      fontSize: 18,
                      fontWeight: FontWeight.w500,
                      color: AppColours.textColor,
                    )),
                onChanged: (value) {
                  quantity = double.tryParse(value) ?? 0;
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
                    color: AppColours.textColor),
              ),
              onPressed: () => Navigator.of(context).pop(),
            ),
            TextButton(
              child: Text(
                'Buy',
                style: GoogleFonts.dmSans(
                    fontSize: 18,
                    fontWeight: FontWeight.w500,
                    color: AppColours.textColor),
              ),
              onPressed: () {
                _buyStock(quantity);
                Navigator.of(context).pop();
              },
            ),
          ],
        );
      },
    );
  }

  void _buyStock(double quantity) {
    setState(() {
      userHoldings[selectedStock] =
          (userHoldings[selectedStock] ?? 0) + quantity;
    });
    print('Bought $quantity shares of $selectedStock');
    ToastMessages.successToast(context, 'Bought $quantity shares of $selectedStock');
  }

  void _showSellDialog() {
    double quantity = 0;
    double availableQuantity = userHoldings[selectedStock] ?? 0;

    showDialog(
      context: context,
      builder: (BuildContext context) {
        return AlertDialog(
          backgroundColor: AppColours.backgroundColor,
          title: Text('Sell $selectedStock'),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(
                'Current Price: \$387',
                style: GoogleFonts.dmSans(
                  fontSize: 18,
                  fontWeight: FontWeight.w500,
                  color: AppColours.textColor,
                ),
              ),
              Text(
                'Available: $availableQuantity',
                style: GoogleFonts.dmSans(
                  fontSize: 18,
                  fontWeight: FontWeight.w500,
                  color: AppColours.textColor,
                ),
              ),
              TextField(
                keyboardType: TextInputType.number,
                decoration: InputDecoration(
                    labelText: 'Quantity',
                    labelStyle: GoogleFonts.dmSans(
                      fontSize: 18,
                      fontWeight: FontWeight.w500,
                      color: AppColours.textColor,
                    )),
                onChanged: (value) {
                  quantity = double.tryParse(value) ?? 0;
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
                    color: AppColours.textColor),
              ),
              onPressed: () => Navigator.of(context).pop(),
            ),
            TextButton(
              child: Text(
                'Sell',
                style: GoogleFonts.dmSans(
                    fontSize: 18,
                    fontWeight: FontWeight.w500,
                    color: AppColours.textColor),
              ),
              onPressed: () {
                if (quantity <= availableQuantity) {
                  _sellStock(quantity);
                  Navigator.of(context).pop();
                } else {
                  Navigator.of(context).pop();
                  ToastMessages.errorToast(context, 'Not enough shares to sell');
                }
              },
            ),
          ],
        );
      },
    );
  }

  void _sellStock(double quantity) {
    setState(() {
      userHoldings[selectedStock] =
          (userHoldings[selectedStock] ?? 0) - quantity;
      if (userHoldings[selectedStock] == 0) {
        userHoldings.remove(selectedStock);
      }
    });
    print('Sold $quantity shares of $selectedStock');
    ToastMessages.successToast(context, 'Sold $quantity shares of $selectedStock');
  }

  Future<void> getHistoricalStockData(String timeRange) async {
    String endDate = DateTime.now().toIso8601String().split('T')[0];
    String startDate;

    switch (timeRange) {
      case '1W':
        startDate = DateTime.now()
            .subtract(const Duration(days: 7))
            .toIso8601String()
            .split('T')[0];
        break;
      case '1M':
        startDate = DateTime.now()
            .subtract(const Duration(days: 30))
            .toIso8601String()
            .split('T')[0];
        break;
      case '3M':
        startDate = DateTime.now()
            .subtract(const Duration(days: 90))
            .toIso8601String()
            .split('T')[0];
        break;
      case '6M':
        startDate = DateTime.now()
            .subtract(const Duration(days: 180))
            .toIso8601String()
            .split('T')[0];
        break;
      case '1Y':
        startDate = DateTime.now()
            .subtract(const Duration(days: 365))
            .toIso8601String()
            .split('T')[0];
        break;
      case '2Y':
        startDate = DateTime.now()
            .subtract(const Duration(days: 730))
            .toIso8601String()
            .split('T')[0];
        break;
      case '3Y':
        startDate = DateTime.now()
            .subtract(const Duration(days: 1095))
            .toIso8601String()
            .split('T')[0];
        break;
      default:
        startDate = DateTime.now()
            .subtract(const Duration(days: 1))
            .toIso8601String()
            .split('T')[0];
    }

    try {
      final response = await http.get(Uri.parse(
          "http://10.0.2.2:8000/stocks/historical?ticker=$selectedStock&start_date=$startDate&end_date=$endDate"));

      if (response.statusCode == 200) {
        Map<String, dynamic> jsonData = jsonDecode(response.body);
        List<FlSpot> flPoints = [];

        jsonData.forEach((key, value) {
          final DateTime dateTime = DateTime.parse(key);
          final double price = value["Close"];
          flPoints
              .add(FlSpot(dateTime.millisecondsSinceEpoch.toDouble(), price));
        });

        setState(() {
          stockDataPoints = flPoints;
        });
      }
    } catch (e) {
      print("Error: $e");
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        backgroundColor: AppColours.backgroundColor,
        title: const Text("Market"),
      ),
      backgroundColor: AppColours.backgroundColor,
      body: SingleChildScrollView(
        child: Column(
          children: [
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16.0),
              child: GestureDetector(
                onTap: () async {
                  final selected = await showSearch(
                    context: context,
                    delegate: StockSearch(stockName, stockDetails),
                  );
                  if (selected != null && selected.isNotEmpty) {
                    setState(() {
                      selectedStock = selected;
                      getLiveStockData();
                    });
                  }
                },
                child: Container(
                  height: 45,
                  decoration: BoxDecoration(
                    color: Colors.black38,
                    borderRadius: BorderRadius.circular(10),
                  ),
                  child: Row(
                    children: [
                      const SizedBox(width: 20),
                      const Icon(Icons.search, color: Colors.white, size: 28),
                      const SizedBox(width: 10),
                      Text(
                        "Search stocks",
                        style: GoogleFonts.dmSans(
                          fontSize: 24,
                          fontWeight: FontWeight.w300,
                          color: Colors.white,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
            Padding(
              padding:
                  const EdgeInsets.symmetric(horizontal: 16.0, vertical: 8.0),
              child: Card(
                color: AppColours.backgroundColor,
                child: Padding(
                  padding: const EdgeInsets.all(16.0),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        "$selectedStock - ${stockDetails[selectedStock]['name']}",
                        style: GoogleFonts.dmSans(
                          color: AppColours.textColor,
                          fontSize: 24,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      const SizedBox(height: 5),
                      Text(
                        "\$387",
                        style: GoogleFonts.dmSans(
                          fontSize: 32,
                          fontWeight: FontWeight.bold,
                          color: AppColours.buttonColor,
                        ),
                      ),
                      const SizedBox(height: 5),
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          _buildInfoItem("Open", "\$133"),
                          _buildInfoItem("High", "\$129"),
                          _buildInfoItem("Low", "\$333"),
                        ],
                      ),
                    ],
                  ),
                ),
              ),
            ),
            const SizedBox(height: 15),
            Text(
              'Your Holdings: ${userHoldings[selectedStock] ?? 0} shares',
              style: GoogleFonts.dmSans(
                color: AppColours.textColor,
                fontSize: 18,
                fontWeight: FontWeight.w500,
              ),
            ),
            SizedBox(height: 15),
            Container(
              alignment: Alignment.center,
              height: 35,
              child: ListView.builder(
                scrollDirection: Axis.horizontal,
                physics: const ClampingScrollPhysics(),
                itemCount: stockDates.length,
                itemBuilder: (BuildContext context, int index) {
                  return Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 14.0),
                    child: GestureDetector(
                      onTap: () {
                        setState(() {
                          selectedTimeRange = stockDates[index];
                          if (selectedTimeRange == '1D') {
                            getLiveStockData();
                            startLiveDataTimer();
                          } else {
                            liveDataTimer?.cancel();
                            getHistoricalStockData(selectedTimeRange);
                          }
                        });
                      },
                      child: Text(
                        stockDates[index],
                        style: GoogleFonts.dmSans(
                            color: selectedTimeRange == stockDates[index]
                                ? AppColours.buttonColor
                                : AppColours.textColor,
                            fontSize: 20,
                            fontWeight: FontWeight.w400),
                      ),
                    ),
                  );
                },
              ),
            ),
            Container(
              alignment: Alignment.center,
              height: 350,
              child: LineChart(LineChartData(
                titlesData: const FlTitlesData(
                  rightTitles:
                      AxisTitles(sideTitles: SideTitles(showTitles: false)),
                  leftTitles:
                      AxisTitles(sideTitles: SideTitles(showTitles: false)),
                  topTitles:
                      AxisTitles(sideTitles: SideTitles(showTitles: false)),
                  bottomTitles:
                      AxisTitles(sideTitles: SideTitles(showTitles: false)),
                ),
                borderData: FlBorderData(show: false),
                gridData: const FlGridData(
                  show: false,
                ),
                lineBarsData: [
                  LineChartBarData(
                      color: Colors.green,
                      dotData: const FlDotData(
                        show: false,
                      ),
                      spots: stockDataPoints)
                ],
                minX: stockDataPoints.isNotEmpty ? stockDataPoints.first.x : 0,
                maxX: stockDataPoints.isNotEmpty ? stockDataPoints.last.x : 0,
                minY: stockDataPoints.isNotEmpty
                    ? stockDataPoints
                        .map((e) => e.y)
                        .reduce((a, b) => a < b ? a : b)
                    : 0,
                maxY: stockDataPoints.isNotEmpty
                    ? stockDataPoints
                        .map((e) => e.y)
                        .reduce((a, b) => a > b ? a : b)
                    : 0,
              )),
            ),
            const SizedBox(height: 15),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceEvenly,
              children: [
                ElevatedButton(
                    onPressed: () => _showBuyDialog(),
                    style: ElevatedButton.styleFrom(
                        backgroundColor: Colors.green.withOpacity(0.9),
                        padding: const EdgeInsets.symmetric(
                            horizontal: 35, vertical: 10),
                        shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(10))),
                    child: Text(
                      "BUY",
                      style: GoogleFonts.dmSans(
                          color: AppColours.backgroundColor,
                          fontSize: 28,
                          fontWeight: FontWeight.w500),
                    )),
                ElevatedButton(
                    onPressed: () => _showSellDialog(),
                    style: ElevatedButton.styleFrom(
                        padding: const EdgeInsets.symmetric(
                            horizontal: 30, vertical: 10),
                        backgroundColor: Colors.red.withOpacity(0.9),
                        shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(10))),
                    child: Text(
                      "SELL",
                      style: GoogleFonts.dmSans(
                          color: AppColours.backgroundColor,
                          fontSize: 28,
                          fontWeight: FontWeight.w500),
                    ))
              ],
            )
          ],
        ),
      ),
    );
  }
}

Widget _buildInfoItem(String label, String value) {
  return Column(
    crossAxisAlignment: CrossAxisAlignment.start,
    children: [
      Text(
        label,
        style: GoogleFonts.dmSans(
          color: AppColours.textColor.withOpacity(0.7),
          fontSize: 18,
        ),
      ),
      Text(
        value,
        style: GoogleFonts.dmSans(
          color: AppColours.textColor,
          fontSize: 22,
          fontWeight: FontWeight.bold,
        ),
      ),
    ],
  );
}

class StockSearch extends SearchDelegate<String> {
  final List<String> stockList;
  final Map<String, dynamic> stockDetails;

  StockSearch(this.stockList, this.stockDetails);

  @override
  ThemeData appBarTheme(BuildContext context) {
    return ThemeData(
      appBarTheme: const AppBarTheme(
        backgroundColor: AppColours.backgroundColor,
        iconTheme: IconThemeData(color: AppColours.textColor),
      ),
      inputDecorationTheme: InputDecorationTheme(
        hintStyle: TextStyle(color: AppColours.textColor.withOpacity(0.5)),
      ),
    );
  }

  @override
  List<Widget> buildActions(BuildContext context) {
    return [
      IconButton(
        icon: const Icon(Icons.clear),
        onPressed: () {
          query = '';
          showResults(context);
        },
      ),
    ];
  }

  @override
  Widget buildLeading(BuildContext context) {
    return IconButton(
      icon: const Icon(Icons.arrow_back),
      onPressed: () => close(context, ''),
    );
  }

  @override
  Widget buildResults(BuildContext context) {
    return _buildSearchList(context);
  }

  @override
  Widget buildSuggestions(BuildContext context) {
    return _buildSearchList(context);
  }

  Widget _buildSearchList(BuildContext context) {
    final List<String> resultList = query.isEmpty
        ? stockList
        : stockList
            .where((stock) =>
                stock.toLowerCase().contains(query.toLowerCase()) ||
                stockDetails[stock]['name']
                    .toLowerCase()
                    .contains(query.toLowerCase()))
            .toList();

    return Scaffold(
      backgroundColor: AppColours.backgroundColor,
      body: ListView.builder(
        itemCount: resultList.length,
        itemBuilder: (context, index) {
          return ListTile(
            title: Text(
              resultList[index],
              style: const TextStyle(color: AppColours.textColor),
            ),
            subtitle: Text(
              stockDetails[resultList[index]]['name'],
              style: TextStyle(color: AppColours.textColor.withOpacity(0.7)),
            ),
            onTap: () {
              close(context, resultList[index]);
            },
          );
        },
      ),
    );
  }
}
