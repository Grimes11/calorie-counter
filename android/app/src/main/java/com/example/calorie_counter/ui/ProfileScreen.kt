package com.example.calorie_counter.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.calorie_counter.auth.AuthViewModel
import com.example.calorie_counter.auth.UserProfile
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    vm: AuthViewModel,
    profileFlow: StateFlow<UserProfile?>,   // <-- pass whatever your VM exposes
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    // Collect the flow safely; starts as null until first emission
    val profile = profileFlow.collectAsState(initial = null).value

    // Local editable weight state (Double-backed to avoid warnings)
    var weightKg by remember(profile) { mutableDoubleStateOf(profile?.weightKg ?: 0.0) }

    // Live BMI calculation
    val bmi = remember(profile, weightKg) {
        val h = profile?.heightCm ?: 0.0
        if (h > 0.0) {
            val m = h / 100.0
            weightKg / (m * m)
        } else 0.0
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (profile == null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator()
                    Spacer(Modifier.width(12.dp))
                    Text("Loading profile…", style = MaterialTheme.typography.bodyLarge)
                }
                return@Column
            }

            // Name
            ProfileItem(label = "Name", value = profile.name)

            // Email
            ProfileItem(label = "Email", value = profile.email)

            // Gender — explicit Char type fixes “Cannot infer a type for this parameter”
            ProfileItem(
                label = "Gender",
                value = profile.gender.replaceFirstChar { c: Char ->
                    c.titlecase(Locale.getDefault())
                }
            )

            // Age
            ProfileItem(label = "Age", value = "${profile.age} years")

            // Height
            ProfileItem(label = "Height", value = "${profile.heightCm} cm")

            // Editable Weight only
            OutlinedTextField(
                value = if (weightKg == 0.0) "" else weightKg.toString(),
                onValueChange = { new ->
                    if (new.isBlank()) {
                        weightKg = 0.0
                    } else {
                        new.toDoubleOrNull()?.let { weightKg = it }
                    }
                },
                label = { Text("Weight (kg)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // BMI Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("BMI", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        String.format(Locale.getDefault(), "%.1f", bmi),
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(bmiCategory(bmi), style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    scope.launch {
                        vm.updateWeight(weightKg)
                        Toast.makeText(ctx, "Weight updated", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Changes")
            }
        }
    }
}

@Composable
private fun ProfileItem(label: String, value: String) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun bmiCategory(bmi: Double): String = when {
    bmi <= 0.0 -> "—"
    bmi < 18.5 -> "Underweight"
    bmi < 25.0 -> "Normal"
    bmi < 30.0 -> "Overweight"
    else -> "Obese"
}
