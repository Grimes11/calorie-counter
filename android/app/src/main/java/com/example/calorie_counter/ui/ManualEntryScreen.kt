package com.example.calorie_counter.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.calorie_counter.data.CaloriesRepo
import com.example.calorie_counter.data.FoodInfo
import com.example.calorie_counter.data.KcalResult
import kotlin.math.roundToInt

@Composable
fun ManualEntryScreen(
    repo: CaloriesRepo,
    onComputed: (KcalResult) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allFoods = remember { repo.all() }              // List<FoodInfo>
    var query by remember { mutableStateOf("") }
    var selected: FoodInfo? by remember { mutableStateOf(null) }

    // Defaults for portion and bias
    var qty by remember { mutableStateOf(1.0) }
    var bias by remember { mutableStateOf(0) } // -20..+20

    val filtered = remember(query, allFoods) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) allFoods
        else allFoods.filter { f ->
            f.display_name.lowercase().contains(q) ||
                    f.id.lowercase().contains(q)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Header(onBack = onBack)

        // Search box
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Search food…") },
            singleLine = true
        )

        Spacer(Modifier.height(10.dp))

        // Results list
        if (selected == null) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered) { food ->
                    FoodRow(food = food, onPick = {
                        selected = food
                        qty = food.presets.getOrNull(food.presets.size / 2) ?: 1.0
                        bias = 0
                    })
                }
            }
        } else {
            // Portion + bias selection for the chosen food
            PortionAndBiasSection(
                food = selected!!,
                qty = qty,
                onQtyChange = { qty = it },
                bias = bias,
                onBiasChange = { bias = it }
            )

            Spacer(Modifier.height(16.dp))

            // Preview & Continue
            val preview = remember(selected!!.id, qty, bias) {
                repo.compute(selected!!.id, qty, bias)
            }

            Text(
                "${preview.kcal.toInt()} kcal",
                style = MaterialTheme.typography.headlineMedium
            )
            Text("±${preview.uncertaintyPct}% → ${preview.low.toInt()}–${preview.high.toInt()} kcal")
            Spacer(Modifier.height(6.dp))
            Text("${prettyQty(qty, selected!!.unit)} × ${selected!!.kcal_per_unit.toInt()} kcal (bias ${if (bias>=0) "+" else ""}$bias%)")

            Spacer(Modifier.height(20.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { onComputed(preview) },
                    modifier = Modifier.weight(1f)
                ) { Text("Save") }
                Button(
                    onClick = { selected = null },
                    modifier = Modifier.weight(1f)
                ) { Text("Change Food") }
            }
        }
    }
}

@Composable
private fun Header(onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Button(onClick = onBack) { Text("Back") }
        Text(
            "Enter Food Manually",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(Modifier.width(64.dp)) // spacer to balance the "Back" button width
    }
}

@Composable
private fun FoodRow(food: FoodInfo, onPick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onPick() }
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(food.display_name, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(2.dp))
            Text(
                "1 ${food.unit} ≈ ${food.kcal_per_unit.roundToInt()} kcal",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Text("Select", color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun PortionAndBiasSection(
    food: FoodInfo,
    qty: Double,
    onQtyChange: (Double) -> Unit,
    bias: Int,
    onBiasChange: (Int) -> Unit
) {
    Spacer(Modifier.height(8.dp))
    Text("Selected: ${food.display_name}", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(12.dp))

    // Quick presets
    Text("Presets", style = MaterialTheme.typography.labelLarge)
    Spacer(Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        food.presets.forEach { p ->
            val selected = qty == p
            Chip(
                text = prettyQty(p, food.unit),
                selected = selected,
                onClick = { onQtyChange(p) }
            )
        }
    }

    Spacer(Modifier.height(14.dp))

    // Simple +/- bias controls for MVP (no Slider to keep code light)
    Text("Size adjustment (bias)", style = MaterialTheme.typography.labelLarge)
    Spacer(Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { onBiasChange((bias - 5).coerceAtLeast(-20)) }) { Text("-5%") }
        Text("Bias: ${if (bias>=0) "+" else ""}$bias%")
        Button(onClick = { onBiasChange((bias + 5).coerceAtMost(20)) }) { Text("+5%") }
    }
}

// Reuse the chip you already have in PortionScreen; here’s a self-contained version:
@Composable
private fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .then(Modifier)
    ) {
        Text(
            text = text,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun prettyQty(q: Double, unit: String): String {
    val intPart = q.toInt()
    val frac = (q - intPart)
    val qStr = if (frac == 0.0) "$intPart" else String.format("%.1f", q)
    return "$qStr $unit" + if (q >= 2.0 && !unit.endsWith("s")) "s" else ""
}
