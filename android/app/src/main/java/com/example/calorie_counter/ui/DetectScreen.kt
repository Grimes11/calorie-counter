package com.example.calorie_counter.ui

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width // <-- add this
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tensorflow.lite.support.common.FileUtil
import com.example.calorie_counter.ImageClassifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetectScreen(
    onConfirm: (String) -> Unit,
    onManual: () -> Unit,
    onOpenCamera: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var pickedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var lastPrediction by remember { mutableStateOf<String?>(null) }

    val galleryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        error = null
        lastPrediction = null
        if (uri == null) return@rememberLauncherForActivityResult
        pickedBitmap = decodeBitmap(context, uri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detect from Gallery") },
                navigationIcon = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Settings"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenCamera) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = "Open camera")
                    }
                }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Choose a food photo and let the model detect it.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(12.dp))

                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { galleryPicker.launch("image/*") }
                    ) {
                        Icon(Icons.Filled.Image, contentDescription = null)
                        Spacer(Modifier.width(8.dp)) // works now
                        Text("Pick from Gallery")
                    }

                    Spacer(Modifier.height(8.dp))

                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onManual
                    ) {
                        Text("Enter Manually")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            pickedBitmap?.let { bm ->
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Preview",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Spacer(Modifier.height(12.dp))
                        Image(
                            bitmap = bm.asImageBitmap(),
                            contentDescription = "Chosen image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.height(12.dp))

                        Button(
                            enabled = !isLoading,
                            onClick = {
                                error = null
                                lastPrediction = null
                                isLoading = true
                                scope.launch {
                                    try {
                                        val labels = withContext(Dispatchers.IO) {
                                            FileUtil.loadLabels(context, "labels.txt")
                                        }
                                        val top1 = withContext(Dispatchers.Default) {
                                            ImageClassifier(context, labels, threads = 3).use { clf ->
                                                clf.classify(bm, topK = 3).firstOrNull()
                                            }
                                        }
                                        if (top1 != null) {
                                            lastPrediction = top1.label
                                        } else {
                                            error = "No prediction."
                                        }
                                    } catch (t: Throwable) {
                                        error = t.localizedMessage ?: "Classification failed."
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            }
                        ) {
                            Text(if (isLoading) "Detecting…" else "Detect")
                        }

                        if (isLoading) {
                            Spacer(Modifier.height(12.dp))
                            CircularProgressIndicator()
                        }

                        error?.let {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        lastPrediction?.let { label ->
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "Top result: $label",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(Modifier.height(8.dp))
                            Button(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { onConfirm(label) }
                            ) { Text("Use this result") }
                        }
                    }
                }
            }
        }
    }
}

/** Decode a content Uri into a mutable ARGB_8888 Bitmap. */
private fun decodeBitmap(context: android.content.Context, uri: Uri): Bitmap? {
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
