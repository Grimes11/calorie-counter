package com.example.calorie_counter.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import kotlin.math.roundToInt

// Matches calories.json keys you used in UI: display_name, kcal_per_unit, uncertainty_pct
data class FoodInfo(
    val id: String,
    @SerializedName("display_name") val display_name: String,
    val unit: String,
    val presets: List<Double>,
    @SerializedName("kcal_per_unit") val kcal_per_unit: Double,
    @SerializedName("uncertainty_pct") val uncertainty_pct: Int
)

// Matches your ResultScreen/PortionScreen expectations
data class KcalResult(
    val foodId: String,
    val displayName: String,
    val unit: String,
    val qty: Double,
    val perUnit: Double,
    val biasPct: Int,
    val kcal: Double,           // total adjusted kcal
    val uncertaintyPct: Int,    // same as food.uncertainty_pct
    val low: Double,            // kcal - band
    val high: Double            // kcal + band
)

class CaloriesRepo(
    private val context: Context,
    private val gson: Gson = Gson()
) {
    private val foods: Map<String, FoodInfo> by lazy {
        val json = context.assets.open("calories.json").bufferedReader().use { it.readText() }
        val type = object : TypeToken<List<FoodInfo>>() {}.type
        val list: List<FoodInfo> = gson.fromJson(json, type)
        list.associateBy { it.id }
    }

    fun get(foodId: String): FoodInfo? = foods[foodId]
    fun all(): List<FoodInfo> = foods.values.sortedBy { it.display_name }

    /**
     * qty: number of "unit" (e.g., 1.0 plate, 2.0 pieces, 0.5 plate)
     * biasPct: -20..+20 from your slider
     */
    fun compute(foodId: String, qty: Double, biasPct: Int): KcalResult {
        val f = foods[foodId] ?: error("Unknown food id: $foodId")

        val base = qty * f.kcal_per_unit           // base kcal
        val adjusted = base * (1.0 + biasPct / 100.0) // apply user bias
        val bandHalf = adjusted * (f.uncertainty_pct / 100.0)

        val total = adjusted
        val low = adjusted - bandHalf
        val high = adjusted + bandHalf

        return KcalResult(
            foodId = f.id,
            displayName = f.display_name,
            unit = f.unit,
            qty = qty,
            perUnit = f.kcal_per_unit,
            biasPct = biasPct,
            kcal = total,
            uncertaintyPct = f.uncertainty_pct,
            low = low,
            high = high
        )
    }
}
