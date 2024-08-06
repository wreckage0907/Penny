import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:mobile/Pages/expenseTracker/expense_dashboard.dart';
import 'package:mobile/Pages/expenseTracker/income_analysis.dart';
import 'package:mobile/consts/app_colours.dart';

class ExpenseTracker extends StatefulWidget {
  const ExpenseTracker({super.key});

  @override
  State<ExpenseTracker> createState() => _ExpenseTrackerState();
}

class _ExpenseTrackerState extends State<ExpenseTracker> with SingleTickerProviderStateMixin {
  late TabController _tabController;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 2, vsync: this);
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        backgroundColor: AppColours.backgroundColor,
        title: Text(
          "Expense Tracker",
          style: GoogleFonts.dmSans(
            fontSize: 28,
            fontWeight: FontWeight.w600,
            color: AppColours.textColor,
          ),
        ),
        bottom: TabBar(
          controller: _tabController,
          tabs: const [
            Tab(text: 'Dashboard'),
            Tab(text: 'Analysis'),
          ],
          labelStyle: GoogleFonts.dmSans(
            fontSize: 16,
            fontWeight: FontWeight.w500,
          ),
          labelColor: AppColours.textColor,
          unselectedLabelColor: AppColours.textColor.withOpacity(0.5),
          indicatorColor: AppColours.textColor,
        ),
      ),
      backgroundColor: AppColours.backgroundColor,
      body: TabBarView(
        controller: _tabController,
        children: const [
          ExpenseDashboard(),
          IncomeAnalysis(),
        ],
      ),
    );
  }
}