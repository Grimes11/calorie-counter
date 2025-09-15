package com.example.calorie_counter.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class MealEntry(
    val id: String = UUID.randomUUID().toString(),   // <-- add id
    val ts: Long,
    val source: String, // "camera" | "gallery" | "manual"
    val result: KcalResult
)

class MealLogRepo(
    private val context: Context,
    private val gson: Gson
) {
    private val file: File by lazy {
        File(context.filesDir, "meal_log.json")
    }

    private fun readAll(): MutableList<MealEntry> {
        if (!file.exists()) return mutableListOf()
        val json = file.readText()
        if (json.isBlank()) return mutableListOf()
        val type = object : TypeToken<MutableList<MealEntry>>() {}.type
        return runCatching { gson.fromJson<MutableList<MealEntry>>(json, type) }
            .getOrElse { mutableListOf() }
    }

    private fun writeAll(list: List<MealEntry>) {
        file.writeText(gson.toJson(list))
    }

    fun log(entry: MealEntry) {
        val list = readAll()
        list.add(entry)
        writeAll(list)
    }

    fun today(): List<MealEntry> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val todayKey = sdf.format(Date())
        return readAll().filter {
            sdf.format(Date(it.ts)) == todayKey
        }.sortedBy { it.ts }
    }

    fun delete(id: String) {
        val list = readAll()
        val newList = list.filterNot { it.id == id }
        writeAll(newList)
    }

    fun clearToday() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val todayKey = sdf.format(Date())
        val list = readAll()
        val kept = list.filter { sdf.format(Date(it.ts)) != todayKey }
        writeAll(kept)
    }

    fun totalKcal(entries: List<MealEntry>): Int =
        entries.sumOf { it.result.kcal.toInt() }
}
