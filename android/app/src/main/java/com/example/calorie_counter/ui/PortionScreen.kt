package com.example.calorie_counter.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.calorie_counter.data.CaloriesRepo
import com.example.calorie_counter.data.KcalResult
import kotlin.math.roundToInt

@Composable
fun PortionScreen(
    foodId: String,
    repo: CaloriesRepo,
    onComputed: (KcalResult) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val food = remember(foodId) { repo.get(foodId) }
    requireNotNull(food) { "Invalid food id: $foodId" }

    // Default to middle preset or first
    var qty by remember(foodId) {
        val p = food.presets
        mutableStateOf(if (p.isNotEmpty()) p[p.size / 2] else 1.0)
    }
    // Bias slider -20..+20 (step 5)
    var bias by remember { mutableStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = food.display_name,
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(Modifier.height(6.dp))
        Text("1 ${food.unit} ≈ ${food.kcal_per_unit.toInt()} kcal")

        Spacer(Modifier.height(16.dp))

        // Preset chips
        Column(Modifier.fillMaxWidth()) {
            Text("Quick presets", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            FlowRow(spacing = 8.dp, runSpacing = 8.dp) {
                food.presets.forEach { p ->
                    val selected = p == qty
                    Chip(
                        text = prettyQty(p, food.unit),
                        selected = selected,
                        onClick = { qty = p }
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Bias slider
        Text("Size adjustment (bias)")
        Spacer(Modifier.height(6.dp))
        Text("${if (bias >= 0) "+" else ""}$bias%")
        Slider(
            value = bias.toFloat(),
            onValueChange = { bias = it.roundToInt() },
            valueRange = -20f..20f,
            steps = 8, // -20,-15,-10,-5,0,5,10,15,20
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(Modifier.height(20.dp))

        val preview = remember(foodId, qty, bias) { repo.compute(foodId, qty, bias) }
        Text(
            "${preview.kcal.toInt()} kcal",
            style = MaterialTheme.typography.headlineMedium
        )
        Text("±${preview.uncertaintyPct}% → ${preview.low.toInt()}–${preview.high.toInt()} kcal")
        Spacer(Modifier.height(6.dp))
        Text("${prettyQty(qty, food.unit)} × ${food.kcal_per_unit.toInt()} kcal (bias ${if (bias>=0) "+" else ""}$bias%)")

        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(onClick = onBack) { Text("Back") }
            Button(onClick = { onComputed(preview) }) { Text("Continue") }
        }
    }
}

@Composable
private fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else Color.LightGray,
                shape = RoundedCornerShape(24.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface
        )
    }
}

/** Simple one-line flow layout for chips. */
@Composable
private fun FlowRow(
    spacing: androidx.compose.ui.unit.Dp,
    runSpacing: androidx.compose.ui.unit.Dp,
    content: @Composable () -> Unit
) {
    // For MVP keep it simple; Compose has an experimental FlowRow,
    // but we avoid extra dependencies. Stacking chips in Column is fine.
    Column(verticalArrangement = Arrangement.spacedBy(runSpacing)) {
        Row(horizontalArrangement = Arrangement.spacedBy(spacing)) { content() }
    }
}

private fun prettyQty(q: Double, unit: String): String {
    // Show 0.5/1/1.5 nicely
    val intPart = q.toInt()
    val frac = (q - intPart)
    val qStr = if (frac == 0.0) "$intPart" else String.format("%.1f", q)
    return "$qStr $unit${if (q >= 2.0 && !unit.endsWith("s")) "s" else ""}"
}
