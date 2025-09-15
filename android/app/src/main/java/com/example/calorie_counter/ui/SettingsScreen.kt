package com.example.calorie_counter.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.calorie_counter.data.PrefsRepo
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    prefs: PrefsRepo,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    // Read current values (defaults to false)
    val syncEnabled by prefs.syncEnabled.collectAsState(initial = false)
    val shareCorrections by prefs.shareCorrections.collectAsState(initial = false)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = onBack) { Text("Back") }
            Text(
                "Settings & Privacy",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(Modifier.width(64.dp)) // balance layout vs Back button
        }

        Text(
            "We process images on your device. Photos never leave your phone.",
            style = MaterialTheme.typography.bodyMedium
        )

        // Sync my logs (opt-in)
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Sync my logs", style = MaterialTheme.typography.titleMedium)
                Text("Keep a cloud copy, view on multiple devices (optional).")
            }
            Switch(
                checked = syncEnabled,
                onCheckedChange = { checked ->
                    scope.launch { prefs.setSyncEnabled(checked) }
                }
            )
        }

        // Help improve accuracy (send corrections) (opt-in)
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Help improve accuracy (send corrections)",
                    style = MaterialTheme.typography.titleMedium
                )
                Text("Only labels & model signals. No photos are uploaded.")
            }
            Switch(
                checked = shareCorrections,
                onCheckedChange = { checked ->
                    scope.launch { prefs.setShareCorrections(checked) }
                }
            )
        }
    }
}
