package com.example.calorie_counter.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.calorie_counter.data.KcalResult

@Composable
fun ResultScreen(
    result: KcalResult,
    onLog: () -> Unit,
    onStartNew: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Calorie Estimate",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(Modifier.height(18.dp))

        Text("${result.kcal.toInt()} kcal", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(6.dp))
        Text("±${result.uncertaintyPct}% → ${result.low.toInt()}–${result.high.toInt()} kcal")

        Spacer(Modifier.height(14.dp))
        Text(
            "${result.displayName}: " +
                    "${prettyQty(result.qty, result.unit)} × ${result.perUnit.toInt()} kcal " +
                    "(bias ${if (result.biasPct >= 0) "+" else ""}${result.biasPct}%)"
        )

        Spacer(Modifier.height(28.dp))

        Button(onClick = onLog, modifier = Modifier.fillMaxWidth()) {
            Text("Log This")
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = onStartNew, modifier = Modifier.fillMaxWidth()) {
            Text("Start New")
        }
    }
}

private fun prettyQty(q: Double, unit: String): String {
    val intPart = q.toInt()
    val frac = (q - intPart)
    val qStr = if (frac == 0.0) "$intPart" else String.format("%.1f", q)
    return "$qStr $unit${if (q >= 2.0 && !unit.endsWith("s")) "s" else ""}"
}
