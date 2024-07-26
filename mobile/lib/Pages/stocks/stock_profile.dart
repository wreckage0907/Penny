import 'dart:async';
import 'dart:convert';

import 'package:fl_chart/fl_chart.dart';
import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:mobile/consts/app_colours.dart';
import 'package:http/http.dart' as http;

class StockProfile extends StatefulWidget {
  const StockProfile({super.key});

  @override
  State<StockProfile> createState() => _StockProfileState();
}

class _StockProfileState extends State<StockProfile> {
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
      body: Column(
        children: [
          Container(
            height: 80,
            child: ListView.builder(
              scrollDirection: Axis.horizontal,
              physics: const ClampingScrollPhysics(),
              itemCount: stockName.length,
              itemBuilder: (BuildContext context, int index) {
                return Padding(
                  padding: const EdgeInsets.all(8.0),
                  child: GestureDetector(
                    onTap: () {
                      setState(() {
                        selectedStock = stockName[index];
                        getLiveStockData();
                      });
                    },
                    child: CircleAvatar(
                      backgroundColor: selectedStock == stockName[index] ? AppColours.buttonColor : AppColours.cardColor,
                      radius: 30,
                      child: Text(
                        stockName[index],
                        style: TextStyle(color: selectedStock == stockName[index] ? AppColours.backgroundColor : AppColours.textColor),
                      ),
                    ),
                  ),
                );
              },
            ),
          ),
          const SizedBox(height: 10),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 45.0),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Expanded(
                  child: Text(
                    "$selectedStock\n${stockDetails[selectedStock]['name']}",
                    style: GoogleFonts.dmSans(
                        color: AppColours.textColor,
                        fontSize: 26,
                        fontWeight: FontWeight.bold),
                    overflow: TextOverflow.ellipsis,
                  ),
                ),
                Text(
                  "\$387",
                  style: GoogleFonts.dmSans(
                      fontSize: 26,
                      fontWeight: FontWeight.bold,
                      color: AppColours.textColor),
                )
              ],
            ),
          ),
          const SizedBox(height: 10),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceAround,
            children: [
              Text(
                "Open: \$133",
                style: GoogleFonts.dmSans(
                  fontSize: 20,
                  fontWeight: FontWeight.w500,
                  color: AppColours.textColor,
                ),
              ),
              Text(
                "High: \$129",
                style: GoogleFonts.dmSans(
                  fontSize: 20,
                  fontWeight: FontWeight.w500,
                  color: AppColours.textColor,
                ),
              ),
              Text(
                "Low: \$333",
                style: GoogleFonts.dmSans(
                  fontSize: 20,
                  fontWeight: FontWeight.w500,
                  color: AppColours.textColor,
                ),
              ),
            ],
          ),
          const SizedBox(height: 15),
          Container(
            alignment: Alignment.center,
            height: 50,
            child: ListView.builder(
              scrollDirection: Axis.horizontal,
              physics: const ClampingScrollPhysics(),
              itemCount: stockDates.length,
              itemBuilder: (BuildContext context, int index) {
                return Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 12.0),
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
                      style: TextStyle(
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
          AspectRatio(
            aspectRatio: 1,
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
          const SizedBox(height: 25),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceEvenly,
            children: [
              ElevatedButton(
                  onPressed: () => print("Buy"),
                  style: ElevatedButton.styleFrom(
                      backgroundColor: AppColours.buttonColor,
                      padding: const EdgeInsets.symmetric(
                          horizontal: 35, vertical: 10),
                      shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(30))),
                  child: Text(
                    "BUY",
                    style: GoogleFonts.dmSans(
                        color: AppColours.backgroundColor,
                        fontSize: 28,
                        fontWeight: FontWeight.w500),
                  )),
              ElevatedButton(
                  onPressed: () => print("Sell"),
                  style: ElevatedButton.styleFrom(
                      padding: const EdgeInsets.symmetric(
                          horizontal: 30, vertical: 10),
                      backgroundColor: AppColours.buttonColor,
                      shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(30))),
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
    );
  }
}
