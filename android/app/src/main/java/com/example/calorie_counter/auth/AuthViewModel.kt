package com.example.calorie_counter.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

// ---- Firebase KTX (note the capital F in Firebase) ----
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore

import androidx.lifecycle.viewModelScope


// ---------------------------------------------
// Models
// ---------------------------------------------
data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val name: String = "",
    val gender: String = "",      // "male" | "female"
    val age: Int = 0,
    val heightCm: Double = 0.0,
    val weightKg: Double = 0.0,
    val bmi: Double = 0.0,
    val autoLogin: Boolean = true
)

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLoggedIn: Boolean = false,
    val user: UserProfile? = null,
    val autoLogin: Boolean = true
)

// ---------------------------------------------
// ViewModel
// ---------------------------------------------
class AuthViewModel(application: Application) : AndroidViewModel(application) {

    // Firebase handles
    private val auth = Firebase.auth
    private val users = Firebase.firestore.collection("users")

    // UI state
    private val _ui = MutableStateFlow(AuthUiState())
    val ui: StateFlow<AuthUiState> = _ui

    // Convenience flow used by MainActivity
    val isLoggedIn: StateFlow<Boolean> =
        ui.map { it.isLoggedIn }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    init {
        // If already signed in, flip flag and fetch profile
        auth.currentUser?.let {
            _ui.update { it.copy(isLoggedIn = true) }
            refreshProfile()
        }
    }

    // ---------------- Auth ----------------
    fun signIn(email: String, password: String) {
        _ui.update { it.copy(isLoading = true, error = null) }
        auth.signInWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener {
                _ui.update { it.copy(isLoggedIn = true) }
                refreshProfile()
            }
            .addOnFailureListener { e ->
                _ui.update {
                    it.copy(
                        isLoading = false,
                        error = e.localizedMessage ?: "Sign-in failed"
                    )
                }
            }
    }

    fun signUp(
        name: String,
        email: String,
        password: String,
        gender: String,
        age: Int,
        heightCm: Double,
        weightKg: Double,
        autoLogin: Boolean
    ) {
        _ui.update { it.copy(isLoading = true, error = null) }

        auth.createUserWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid ?: return@addOnSuccessListener
                val bmi = computeBmi(heightCm, weightKg)

                val profile = UserProfile(
                    uid = uid,
                    email = email.trim(),
                    name = name.trim(),
                    gender = gender,
                    age = age,
                    heightCm = heightCm,
                    weightKg = weightKg,
                    bmi = bmi,
                    autoLogin = autoLogin
                )

                users.document(uid).set(profile)
                    .addOnSuccessListener {
                        _ui.update {
                            it.copy(
                                isLoading = false,
                                isLoggedIn = true,
                                user = profile,
                                autoLogin = autoLogin
                            )
                        }
                    }
                    .addOnFailureListener { e ->
                        _ui.update {
                            it.copy(
                                isLoading = false,
                                error = e.localizedMessage ?: "Failed to save profile"
                            )
                        }
                    }
            }
            .addOnFailureListener { e ->
                _ui.update {
                    it.copy(
                        isLoading = false,
                        error = e.localizedMessage ?: "Sign-up failed"
                    )
                }
            }
    }

    fun signOut() {
        auth.signOut()
        _ui.update { AuthUiState() }
    }

    // ------------- Profile -------------
    fun refreshProfile() {
        val uid = auth.currentUser?.uid ?: run {
            _ui.update { it.copy(isLoggedIn = false, user = null) }
            return
        }

        _ui.update { it.copy(isLoading = true, error = null) }
        users.document(uid).get()
            .addOnSuccessListener { snap ->
                val p = snap.toObject(UserProfile::class.java)
                _ui.update {
                    it.copy(
                        isLoading = false,
                        user = p,
                        autoLogin = p?.autoLogin ?: true
                    )
                }
            }
            .addOnFailureListener { e ->
                _ui.update {
                    it.copy(
                        isLoading = false,
                        error = e.localizedMessage ?: "Failed to load profile"
                    )
                }
            }
    }

    fun updateWeight(newWeightKg: Double) {
        val uid = auth.currentUser?.uid ?: return
        val current = _ui.value.user ?: return

        val newBmi = computeBmi(current.heightCm, newWeightKg)
        val updates = mapOf("weightKg" to newWeightKg, "bmi" to newBmi)

        _ui.update { it.copy(isLoading = true, error = null) }
        users.document(uid).update(updates)
            .addOnSuccessListener {
                val updated = current.copy(weightKg = newWeightKg, bmi = newBmi)
                _ui.update { it.copy(isLoading = false, user = updated) }
            }
            .addOnFailureListener { e ->
                _ui.update {
                    it.copy(
                        isLoading = false,
                        error = e.localizedMessage ?: "Failed to update weight"
                    )
                }
            }
    }

    fun setAutoLogin(value: Boolean) {
        _ui.update { it.copy(autoLogin = value) }
        val uid = auth.currentUser?.uid ?: return
        users.document(uid).update("autoLogin", value)
    }

    // ------------- Helpers -------------
    private fun computeBmi(heightCm: Double, weightKg: Double): Double {
        val hM = heightCm / 100.0
        return if (hM > 0.0) {
            val bmi = weightKg / (hM * hM)
            kotlin.math.round(bmi * 10) / 10.0
        } else 0.0
    }
}
