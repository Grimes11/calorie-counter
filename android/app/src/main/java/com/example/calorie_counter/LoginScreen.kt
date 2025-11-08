package com.example.calorie_counter

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.calorie_counter.auth.AuthViewModel
import kotlinx.coroutines.launch
import kotlin.math.pow

// ---- Top-level enums (cannot be local in Kotlin)
enum class AuthMode { SignIn, SignUp }
enum class Gender { Male, Female }

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    onAuthed: () -> Unit,
    vm: AuthViewModel = viewModel()
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var mode by remember { mutableStateOf(AuthMode.SignIn) }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var autoLogin by remember { mutableStateOf(true) }

    // sign-up extras
    var fullName by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf<Gender?>(null) }
    var age by remember { mutableStateOf("") }
    var heightCm by remember { mutableStateOf("") }
    var weightKg by remember { mutableStateOf("") }

    var busy by remember { mutableStateOf(false) }

    val bmi by remember(heightCm, weightKg) {
        mutableFloatStateOf(
            runCatching {
                val h = (heightCm.toFloatOrNull() ?: 0f) / 100f
                val w = weightKg.toFloatOrNull() ?: 0f
                if (h > 0f && w > 0f) w / h.pow(2) else 0f
            }.getOrDefault(0f)
        )
    }

    val bg = Brush.verticalGradient(
        listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            MaterialTheme.colorScheme.surface
        )
    )

    Box(
        modifier
            .fillMaxSize()
            .background(bg)
            .systemBarsPadding()
    ) {
        Card(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
                .align(Alignment.Center),
            shape = MaterialTheme.shapes.extraLarge,
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (mode == AuthMode.SignIn) "Welcome back" else "Create your account",
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (mode == AuthMode.SignIn) "Sign in to continue" else "One quick step to get started",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(20.dp))

                // mode switch
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = mode == AuthMode.SignIn,
                        onClick = { mode = AuthMode.SignIn },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        label = { Text("Sign in") }
                    )
                    SegmentedButton(
                        selected = mode == AuthMode.SignUp,
                        onClick = { mode = AuthMode.SignUp },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        label = { Text("Sign up") }
                    )
                }

                Spacer(Modifier.height(16.dp))

                // email
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it.trim() },
                    label = { Text("Email") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                // password
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = if (mode == AuthMode.SignIn) ImeAction.Done else ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        scope.launch {
                            submit(
                                ctx, vm, mode, email, password, autoLogin,
                                fullName, gender, age, heightCm, weightKg,
                                setBusy = { busy = it }, onAuthed
                            )
                        }
                    }),
                    modifier = Modifier.fillMaxWidth()
                )

                // sign-up only fields
                AnimatedVisibility(visible = mode == AuthMode.SignUp, enter = fadeIn(), exit = fadeOut()) {
                    Column {
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = { Text("Full name") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(12.dp))
                        Text("Gender", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            FilterChip(
                                selected = gender == Gender.Male,
                                onClick = { gender = Gender.Male },
                                label = { Text("Male") }
                            )
                            FilterChip(
                                selected = gender == Gender.Female,
                                onClick = { gender = Gender.Female },
                                label = { Text("Female") }
                            )
                        }

                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = age,
                                onValueChange = { age = it.filter(Char::isDigit) },
                                label = { Text("Age") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = heightCm,
                                onValueChange = { heightCm = it.filter(Char::isDigit) },
                                label = { Text("Height (cm)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = weightKg,
                                onValueChange = { weightKg = it.filter(Char::isDigit) },
                                label = { Text("Weight (kg)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (bmi > 0f) "BMI: ${"%.1f".format(bmi)}" else "BMI: –",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // auto-login toggle
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Switch(
                        checked = autoLogin,
                        onCheckedChange = {
                            autoLogin = it
                            // persist immediately
                            scope.launch { vm.setAutoLogin(it) }
                        }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Auto-login next time")
                }

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = {
                        scope.launch {
                            submit(
                                ctx, vm, mode, email, password, autoLogin,
                                fullName, gender, age, heightCm, weightKg,
                                setBusy = { busy = it }, onAuthed
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !busy
                ) {
                    Text(if (mode == AuthMode.SignIn) "Sign in" else "Create account")
                }
            }
        }

        AnimatedVisibility(
            visible = busy,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .matchParentSize()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}

private suspend fun submit(
    ctx: android.content.Context,
    vm: AuthViewModel,
    mode: AuthMode,
    email: String,
    password: String,
    autoLogin: Boolean,
    fullName: String,
    gender: Gender?,
    age: String,
    heightCm: String,
    weightKg: String,
    setBusy: (Boolean) -> Unit,
    onAuthed: () -> Unit
) {
    if (email.isBlank() || password.isBlank()) {
        Toast.makeText(ctx, "Email and password are required", Toast.LENGTH_SHORT).show()
        return
    }

    try {
        setBusy(true)

        if (mode == AuthMode.SignIn) {
            // ViewModel signature: signIn(email, password)
            vm.signIn(email, password)
            vm.setAutoLogin(autoLogin)
            Toast.makeText(ctx, "Signed in", Toast.LENGTH_SHORT).show()
            onAuthed()
        } else {
            // Validate signup fields
            if (fullName.isBlank() || gender == null || age.isBlank()
                || heightCm.isBlank() || weightKg.isBlank()
            ) {
                Toast.makeText(ctx, "Please complete all profile fields", Toast.LENGTH_SHORT).show()
                return
            }

            val ageInt = age.toIntOrNull() ?: 0
            val h = heightCm.toDoubleOrNull() ?: 0.0
            val w = weightKg.toDoubleOrNull() ?: 0.0
            val genderStr = if (gender == Gender.Male) "male" else "female"

            // ViewModel signature: signUp(email, password, name, gender, age, heightCm: Double, weightKg: Double)
            vm.signUp(
                email = email,
                password = password,
                autoLogin = autoLogin,
                name = fullName,
                gender = genderStr,
                age = ageInt,
                heightCm = h,
                weightKg = w
            )

            vm.setAutoLogin(autoLogin)
            Toast.makeText(ctx, "Account created", Toast.LENGTH_SHORT).show()
            onAuthed()
        }
    } catch (t: Throwable) {
        Toast.makeText(
            ctx,
            t.message?.takeIf { it.isNotBlank() } ?: "Something went wrong",
            Toast.LENGTH_LONG
        ).show()
    } finally {
        setBusy(false)
    }
}
