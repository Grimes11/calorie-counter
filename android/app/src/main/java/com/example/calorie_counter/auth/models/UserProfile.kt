package com.example.calorie_counter.auth.models

data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val sex: String = "",          // "male" | "female"
    val age: Int = 0,
    val heightCm: Float = 0f,
    val weightKg: Float = 0f,
    val bmi: Float = 0f,
    val updatedAt: Long = 0L
) {
    companion object {
        fun bmiFor(heightCm: Float, weightKg: Float): Float {
            val hM = (heightCm / 100f).coerceAtLeast(0.01f)
            return (weightKg / (hM * hM)).coerceAtLeast(0f)
        }
    }
}
