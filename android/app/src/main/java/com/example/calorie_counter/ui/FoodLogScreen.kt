package com.example.calorie_counter.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.calorie_counter.data.MealEntry
import com.example.calorie_counter.data.MealLogRepo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FoodLogScreen(
    items: List<MealEntry>,
    onBack: () -> Unit,
    // Provide these so the screen can mutate the log then navigate/recompose
    repo: MealLogRepo? = null,
    onRefresh: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val total = items.sumOf { it.result.kcal.toInt() }
    val dateStr = SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(Date())

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = onBack) { Text("Back") }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Food Log",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text("$dateStr • Total: $total kcal")
            }
            Spacer(Modifier.width(64.dp))
        }

        Spacer(Modifier.height(12.dp))

        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No meals logged today.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items, key = { it.id }) { entry ->
                    MealRow(
                        entry = entry,
                        onDelete = {
                            repo?.delete(entry.id)
                            onRefresh?.invoke()
                        }
                    )
                    Divider()
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                    Text("Back")
                }
                Spacer(Modifier.width(10.dp))
                Button(
                    onClick = {
                        repo?.clearToday()
                        onRefresh?.invoke()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Clear Today")
                }
            }
        }
    }
}

@Composable
private fun MealRow(
    entry: MealEntry,
    onDelete: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(entry.result.displayName, style = MaterialTheme.typography.titleMedium)
            Text(
                "${prettyQty(entry.result.qty, entry.result.unit)} • ${entry.source}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${entry.result.kcal.toInt()} kcal")
            OutlinedButton(onClick = onDelete) { Text("Delete") }
        }
    }
}

private fun prettyQty(q: Double, unit: String): String {
    val intPart = q.toInt()
    val frac = (q - intPart)
    val qStr = if (frac == 0.0) "$intPart" else String.format("%.1f", q)
    return "$qStr $unit" + if (q >= 2.0 && !unit.endsWith("s")) "s" else ""
}
