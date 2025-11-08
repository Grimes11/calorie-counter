package com.example.calorie_counter.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.calorie_counter.auth.AuthViewModel
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    vm: AuthViewModel,
    onBack: () -> Unit,
    onRegistered: () -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var gender by rememberSaveable { mutableStateOf("male") } // "male" | "female"
    var age by rememberSaveable { mutableIntStateOf(25) }
    var heightCm by rememberSaveable { mutableFloatStateOf(170f) }
    var weightKg by rememberSaveable { mutableFloatStateOf(70f) }
    var autoLogin by rememberSaveable { mutableStateOf(true) }
    var showPassword by rememberSaveable { mutableStateOf(false) }

    val bmi = remember(heightCm, weightKg) {
        val m = (heightCm / 100f).coerceAtLeast(0.01f)
        weightKg / (m * m)
    }

    fun submit() {
        if (name.isBlank() || email.isBlank() || password.length < 6) {
            Toast.makeText(ctx, "Fill name, email and a 6+ char password", Toast.LENGTH_SHORT).show()
            return
        }
        scope.launch {
            vm.signUp(
                name = name.trim(),
                email = email.trim(),
                password = password,
                gender = gender,
                age = age,
                heightCm = heightCm.toDouble(),
                weightKg = weightKg.toDouble(),
                autoLogin = autoLogin
            )
            // If your VM exposes success/failure, react to that instead:
            Toast.makeText(ctx, "Account created", Toast.LENGTH_SHORT).show()
            onRegistered()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create your account") },
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next)
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                )
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (showPassword) VisualTransformation.None
                else PasswordVisualTransformation(),
                trailingIcon = {
                    val icon = if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(icon, contentDescription = if (showPassword) "Hide" else "Show")
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                )
            )

            Text("Gender", style = MaterialTheme.typography.labelLarge)
            SingleChoiceSegmentedButtonRow {
                listOf("male", "female").forEachIndexed { idx, opt ->
                    SegmentedButton(
                        selected = gender == opt,
                        onClick = { gender = opt },
                        shape = SegmentedButtonDefaults.itemShape(index = idx, count = 2),
                        label = { Text(opt.replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            LabeledNumberField(
                label = "Age",
                value = age.toString(),
                onValue = { v -> v.toIntOrNull()?.let { age = it.coerceIn(5, 120) } },
                suffix = "yrs",
                keyboardType = KeyboardType.Number
            )

            LabeledNumberField(
                label = "Height",
                value = heightCm.toString(),
                onValue = { v -> v.toFloatOrNull()?.let { heightCm = it.coerceIn(80f, 250f) } },
                suffix = "cm",
                keyboardType = KeyboardType.Decimal
            )

            LabeledNumberField(
                label = "Weight",
                value = weightKg.toString(),
                onValue = { v -> v.toFloatOrNull()?.let { weightKg = it.coerceIn(20f, 400f) } },
                suffix = "kg",
                keyboardType = KeyboardType.Decimal,
                keyboardActions = KeyboardActions(onDone = { submit() })
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("BMI preview", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        String.format(Locale.getDefault(), "%.1f", bmi),
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(bmiCategory(bmi), style = MaterialTheme.typography.bodyMedium)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = autoLogin, onCheckedChange = { autoLogin = it })
                Spacer(Modifier.width(8.dp))
                Text("Sign me in automatically next time")
            }

            // Optional VM flags (if your VM doesn't have them, this still compiles)
            val isBusy = runCatching { vm.javaClass.getDeclaredField("isBusy") }
                .map { f -> f.isAccessible = true; f.getBoolean(vm) }
                .getOrDefault(false)
            val errorMsg = runCatching { vm.javaClass.getDeclaredField("error") }
                .map { f -> f.isAccessible = true; f.get(vm) as? String }
                .getOrNull()

            AnimatedVisibility(visible = !errorMsg.isNullOrEmpty()) {
                Text(errorMsg.orEmpty(), color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = { submit() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !isBusy
            ) {
                if (isBusy) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                }
                Text("Create account")
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

/* ---------- helpers ---------- */

@Composable
private fun LabeledNumberField(
    label: String,
    value: String,
    onValue: (String) -> Unit,
    suffix: String,
    keyboardType: KeyboardType,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValue,
        label = { Text(label) },
        trailingIcon = { Text(suffix) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Next),
        keyboardActions = keyboardActions
    )
}

private fun bmiCategory(bmi: Float): String = when {
    bmi < 18.5f -> "Underweight"
    bmi < 25f -> "Normal"
    bmi < 30f -> "Overweight"
    else -> "Obese"
}
