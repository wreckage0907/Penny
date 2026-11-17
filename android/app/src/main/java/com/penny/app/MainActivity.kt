package com.penny.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.penny.app.core.BudgetSplit
import com.penny.app.core.InvestmentProjection
import com.penny.app.core.Money

/**
 * Migration slice 1: the Analysis tab's budget math, ported from Flutter.
 * The screen is deliberately minimal — it exists to prove the ported logic
 * runs on a real device. Real data wiring lands with the expense-service slice.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface {
                    AnalysisScreen(sampleIncome = 50_000.0)
                }
            }
        }
    }
}

@Composable
fun AnalysisScreen(sampleIncome: Double) {
    val split = BudgetSplit.of(sampleIncome)
    val projections = InvestmentProjection.project(split.savings)

    Column(Modifier.padding(24.dp).testTag("analysis")) {
        Text(
            "Penny — Analysis",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.testTag("analysis-title"),
        )
        Spacer(Modifier.height(12.dp))
        Text("Income: ${Money.format(sampleIncome)}", Modifier.testTag("income"))
        Text("Essentials (50%): ${Money.format(split.essentials)}", Modifier.testTag("essentials"))
        Text("Wants (30%): ${Money.format(split.wants)}", Modifier.testTag("wants"))
        Text("Savings (20%): ${Money.format(split.savings)}", Modifier.testTag("savings"))
        Spacer(Modifier.height(16.dp))
        Text(
            "5-year projection of savings",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.testTag("projection-title"),
        )
        InvestmentProjection.TYPES.forEachIndexed { i, type ->
            Text("$type: ${Money.format(projections[i].last())}", Modifier.testTag("projection-$i"))
        }
    }
}
