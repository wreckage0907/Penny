import 'package:fl_chart/fl_chart.dart';
import 'package:flutter/material.dart';
import 'package:font_awesome_flutter/font_awesome_flutter.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:mobile/consts/app_colours.dart';

class StockProfile extends StatefulWidget {
  const StockProfile({super.key});

  @override
  State<StockProfile> createState() => _StockProfileState();
}

class _StockProfileState extends State<StockProfile> {
  final List<Map<String, dynamic>> barData = [
    {"day": "MON", "value": 150.0},
    {"day": "TUE", "value": 190.0},
    {"day": "WED", "value": 130.0},
    {"day": "THU", "value": 170.0},
    {"day": "FRI", "value": 160.0},
    {"day": "SAT", "value": 140.0},
    {"day": "SUN", "value": 110.0},
  ];

  final List<Map<String, double>> stockData = [
    {"AAPL": 200},
    {"MSFT": 231.75},
    {"META": 300.2},
    {"AMZN": 175},
  ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(),
      body: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 20.0),
            child: Text(
              "CURRENT BALANCE",
              style: GoogleFonts.dmSans(
                fontWeight: FontWeight.w600,
                fontSize: 18,
                color: AppColours.textColor,
              )
            ),
          ),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 20.0),
            child: Text(
              "\$2000.30",
              style: GoogleFonts.dmSans(
                fontSize: 48,
                fontWeight: FontWeight.bold,
                color: AppColours.textColor,
              )
            ),
          ),
          const SizedBox(height: 8),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 20.0),
            child: SizedBox(
              height: 300,
              child: BarChart(
                BarChartData(
                  maxY: 200,
                  minY: 0,
                  gridData: const FlGridData(show: false),
                  borderData: FlBorderData(show: false),
                  titlesData: FlTitlesData(
                    show: true,
                    topTitles: const AxisTitles(sideTitles: SideTitles(showTitles: false)),
                    rightTitles: const AxisTitles(sideTitles: SideTitles(showTitles: false)),
                    leftTitles: const AxisTitles(sideTitles: SideTitles(showTitles: false)),
                    bottomTitles: AxisTitles(
                      sideTitles: SideTitles(
                        showTitles: true,
                        getTitlesWidget: (double value, TitleMeta meta) {
                          int index = value.toInt();
                          if(index >= 0 && index < barData.length) {
                            return Padding(
                              padding: const EdgeInsets.only(top: 8.0),
                              child: Text(
                                barData[index]['day'] as String,
                                style: GoogleFonts.spectral(
                                  color: AppColours.textColor,
                                  fontSize: 14,
                                  fontWeight: FontWeight.w600,
                                ),
                              ),
                            );
                          }
                          return const Text('');
                        }
                      )
                    ),
                  ),
                  barGroups: barData.asMap().entries.map((entry) {
                    int index = entry.key;
                    Map<String, dynamic> data = entry.value;
                    return BarChartGroupData(
                      x: index,
                      barRods: [
                        BarChartRodData(
                          toY: data['value'] as double,
                          color: AppColours.buttonColor,
                          width: 50,
                          borderRadius: BorderRadius.circular(8),
                        ),
                      ],
                    );
                  }).toList(),
                )
              ),
            ),
          ),
          const SizedBox(height: 20),
          Expanded(
            child: Card(
              borderOnForeground: false,
              color: AppColours.cardColor,
              child: Padding(
                padding: const EdgeInsets.only(top: 20.0),
                child: Column(
                  children: [
                    Text(
                      "TRANSACTION HISTORY",
                      style: GoogleFonts.dmSans(
                        fontSize: 24,
                        fontWeight: FontWeight.bold,
                        color: AppColours.textColor,
                      ),
                    ),
                    const SizedBox(height: 15),
                    Padding(
                      padding: const EdgeInsets.symmetric(horizontal: 30.0),
                      child: Column(
                        children: stockData.map((stock) {
                          return Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            children: [
                              Text(
                                stock.keys.first,
                                style: GoogleFonts.dmSans(
                                  fontSize: 24,
                                  fontWeight: FontWeight.bold,
                                  color: AppColours.textColor,
                                ),
                              ),
                              Text(
                                "\$${stock.values.first}",
                                style: GoogleFonts.dmSans(
                                  fontSize: 20,
                                  fontWeight: FontWeight.bold,
                                  color: AppColours.textColor,
                                ),
                              )
                            ],
                          );
                        }).toList(),
                      ),
                    ),
                    Padding(
                      padding: const EdgeInsets.all(12),
                      child: Card(
                        color: AppColours.textColor,
                        child: Padding(
                          padding: const EdgeInsets.all(16),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                "Want to see the market?",
                                style: GoogleFonts.dmSans(
                                  fontSize: 24,
                                  fontWeight: FontWeight.w500,
                                  color: AppColours.backgroundColor,
                                )
                              ),
                              Row(
                                children: [
                                  const FaIcon(
                                    FontAwesomeIcons.search,
                                    color: AppColours.backgroundColor,
                                    size: 20,
                                  ),
                                  const SizedBox(width: 10),
                                  Text(
                                    "Search",
                                    style: GoogleFonts.dmSans(
                                      fontSize: 24,
                                      fontWeight: FontWeight.w500,
                                      color: AppColours.backgroundColor.withOpacity(0.7),
                                    )
                                  )
                                ],
                              )
                            ],
                          ),
                        ),
                      ),
                    )
                  ],
                ),
              ),
            ),
          )
        ],
      ),
    );
  }
}