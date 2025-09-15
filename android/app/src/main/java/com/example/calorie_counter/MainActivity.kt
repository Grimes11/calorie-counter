package com.example.calorie_counter

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.calorie_counter.data.CaloriesRepo
import com.example.calorie_counter.data.KcalResult
import com.example.calorie_counter.data.MealEntry
import com.example.calorie_counter.data.MealLogRepo
import com.example.calorie_counter.data.PrefsRepo
import com.example.calorie_counter.ui.CameraScreen
import com.example.calorie_counter.ui.DetectScreen
import com.example.calorie_counter.ui.FoodLogScreen
import com.example.calorie_counter.ui.ManualEntryScreen
import com.example.calorie_counter.ui.PortionScreen
import com.example.calorie_counter.ui.ResultScreen
import com.example.calorie_counter.ui.SettingsScreen
import com.example.calorie_counter.ui.theme.CaloriecounterTheme
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.rememberCoroutineScope
import org.tensorflow.lite.support.common.FileUtil

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val caloriesRepo = CaloriesRepo(this)
        val prefsRepo = PrefsRepo(this)
        val gson = Gson()
        val mealLog = MealLogRepo(this, gson)

        setContent {
            CaloriecounterTheme {
                val nav = rememberNavController()
                val context = LocalContext.current
                val scope = rememberCoroutineScope()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = nav,
                        startDestination = "detect",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        // Detect (gallery)
                        composable("detect") {
                            DetectScreen(
                                onConfirm = { confirmedLabel ->
                                    // mark source for logging later
                                    nav.currentBackStackEntry?.savedStateHandle?.set("lastSource", "gallery")
                                    nav.navigate("portion/$confirmedLabel")
                                },
                                onManual = { nav.navigate("manual") },
                                onOpenCamera = { nav.navigate("camera") },
                                onOpenSettings = { nav.navigate("settings") }
                            )
                        }

                        // Camera capture → classify → portion
                        composable("camera") {
                            CameraScreen(
                                onCaptured = { uri: Uri ->
                                    scope.launch {
                                        try {
                                            val labels = FileUtil.loadLabels(context, "labels.txt")
                                            ImageClassifier(context, labels, threads = 3).use { clf ->
                                                val bm = loadBitmap(context, uri)
                                                if (bm == null) {
                                                    nav.popBackStack()
                                                    return@launch
                                                }
                                                val preds = withContext(Dispatchers.Default) {
                                                    clf.classify(bm, topK = 3)
                                                }
                                                val top1 = preds.firstOrNull()
                                                if (top1 != null) {
                                                    nav.currentBackStackEntry?.savedStateHandle?.set("lastSource", "camera")
                                                    nav.navigate("portion/${top1.label}")
                                                } else {
                                                    nav.popBackStack()
                                                }
                                            }
                                        } catch (_: Throwable) {
                                            nav.popBackStack()
                                        }
                                    }
                                },
                                onCancel = { nav.popBackStack() }
                            )
                        }

                        // Manual entry
                        composable("manual") {
                            ManualEntryScreen(
                                repo = caloriesRepo,
                                onComputed = { result ->
                                    val json = gson.toJson(result)
                                    nav.currentBackStackEntry?.savedStateHandle?.set("lastResultJson", json)
                                    nav.currentBackStackEntry?.savedStateHandle?.set("lastSource", "manual")
                                    nav.navigate("result")
                                },
                                onBack = { nav.popBackStack() }
                            )
                        }

                        // Portion
                        composable(
                            route = "portion/{foodId}",
                            arguments = listOf(navArgument("foodId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val foodId = backStackEntry.arguments?.getString("foodId")!!
                            PortionScreen(
                                foodId = foodId,
                                repo = caloriesRepo,
                                onComputed = { result: KcalResult ->
                                    // carry forward source ("gallery" | "camera")
                                    val source = nav.previousBackStackEntry
                                        ?.savedStateHandle
                                        ?.get<String>("lastSource") ?: "unknown"
                                    val json = gson.toJson(result)

                                    nav.currentBackStackEntry?.savedStateHandle?.set("lastResultJson", json)
                                    nav.currentBackStackEntry?.savedStateHandle?.set("lastSource", source)
                                    nav.navigate("result")
                                },
                                onBack = { nav.popBackStack() }
                            )
                        }

                        // Result
                        composable("result") {
                            val prev = nav.previousBackStackEntry?.savedStateHandle
                            val json = prev?.get<String>("lastResultJson")
                            val source = prev?.get<String>("lastSource") ?: "unknown"

                            if (json == null) {
                                nav.popBackStack(route = "detect", inclusive = false)
                            } else {
                                val result = gson.fromJson(json, KcalResult::class.java)
                                ResultScreen(
                                    result = result,
                                    onLog = {
                                        val entry = MealEntry(
                                            ts = System.currentTimeMillis(),
                                            source = source,
                                            result = result
                                        )
                                        mealLog.log(entry)
                                        Toast.makeText(this@MainActivity, "Saved to log", Toast.LENGTH_SHORT).show()
                                        nav.navigate("foodlog")
                                    },
                                    onStartNew = {
                                        nav.popBackStack(route = "detect", inclusive = false)
                                    }
                                )
                            }
                        }

                        // Food log
                        composable("foodlog") {
                            // Re-read each time we navigate here
                            val items = mealLog.today()
                            FoodLogScreen(
                                items = items,
                                repo = mealLog,
                                onRefresh = {
                                    // Easiest refresh is to re-navigate to the same route
                                    nav.popBackStack()
                                    nav.navigate("foodlog")
                                },
                                onBack = {
                                    nav.popBackStack(route = "detect", inclusive = false)
                                }
                            )
                        }


                        // Settings
                        composable("settings") {
                            SettingsScreen(
                                prefs = prefsRepo,
                                onBack = { nav.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Decode a content Uri into a mutable ARGB_8888 Bitmap. */
private fun loadBitmap(context: android.content.Context, uri: Uri): Bitmap? {
    return try {
        if (Build.VERSION.SDK_INT >= 28) {
            val src = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(src).copy(Bitmap.Config.ARGB_8888, true)
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                .copy(Bitmap.Config.ARGB_8888, true)
        }
    } catch (_: Exception) {
        null
    }
}
