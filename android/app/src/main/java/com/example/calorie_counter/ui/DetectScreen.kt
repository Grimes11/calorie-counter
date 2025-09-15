package com.example.calorie_counter.ui

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.calorie_counter.ImageClassifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tensorflow.lite.support.common.FileUtil

data class UiPred(val label: String, val pct: Float)

@Composable
fun DetectScreen(
    onConfirm: (confirmedLabel: String) -> Unit,
    onManual: () -> Unit,
    onOpenCamera: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Lazily load labels & create classifier once
    val labels by remember {
        mutableStateOf(FileUtil.loadLabels(context, "labels.txt"))
    }
    val classifier by remember {
        mutableStateOf(ImageClassifier(context, labels, threads = 3))
    }
    DisposableEffect(Unit) { onDispose { classifier.close() } }

    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var preds by remember { mutableStateOf<List<UiPred>>(emptyList()) }
    var selectedIdx by remember { mutableStateOf<Int?>(null) }
    var running by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Pick or capture a photo to start") }

    // Gallery picker
    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            preds = emptyList()
            selectedIdx = null
            status = "Classifying…"
            running = true

            scope.launch {
                val bmp = loadBitmap(context, uri)
                bitmap = bmp
                val p = withContext(Dispatchers.Default) {
                    classifier.classify(bmp!!, topK = 3)
                        .map { UiPred(it.label, it.score) }
                }
                preds = p
                running = false
                val top1 = p.firstOrNull()?.pct ?: 0f
                status = if (top1 >= 0.75f) {
                    selectedIdx = 0
                    "Top-1 ≥ 0.75 — auto-selected"
                } else {
                    "Top-1 < 0.75 — please choose"
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Detect Food",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(Modifier.height(12.dp))
        Text(status)

        Spacer(Modifier.height(16.dp))

        // Image preview
        bitmap?.let { bm ->
            Image(
                bitmap = bm.asImageBitmap(),
                contentDescription = "preview",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp))
            )
            Spacer(Modifier.height(16.dp))
        }

        // Action buttons row
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                enabled = !running,
                onClick = {
                    picker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            ) { Text(if (running) "Classifying…" else "Pick Photo") }

            OutlinedButton(
                enabled = !running,
                onClick = { onOpenCamera() }
            ) { Text("Use Camera") }
        }

        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            enabled = !running,
            onClick = { onOpenSettings() }
        ) { Text("Settings") }

        Spacer(Modifier.height(16.dp))

        // Top-3 predictions list
        if (preds.isNotEmpty()) {
            Column(Modifier.fillMaxWidth()) {
                preds.forEachIndexed { idx, p ->
                    val selected = selectedIdx == idx
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .border(
                                width = if (selected) 2.dp else 1.dp,
                                color = if (selected) MaterialTheme.colorScheme.primary else Color.LightGray,
                                shape = RoundedCornerShape(24.dp)
                            )
                            .clickable { selectedIdx = idx },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${p.label} — ${"%.1f".format(p.pct * 100)}%",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            color = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onManual) {
                Text("Not in list → Manual search")
            }
            Spacer(Modifier.height(16.dp))
            Button(
                enabled = selectedIdx != null && !running,
                onClick = { onConfirm(preds[selectedIdx!!].label) }
            ) { Text("Confirm") }
        }
    }
}

private fun loadBitmap(context: android.content.Context, uri: Uri): Bitmap? {
    return try {
        if (Build.VERSION.SDK_INT >= 28) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source).copy(Bitmap.Config.ARGB_8888, true)
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                .copy(Bitmap.Config.ARGB_8888, true)
        }
    } catch (_: Exception) { null }
}
