package com.example.calorie_counter.ui

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume

@Composable
fun CameraScreen(
    onCaptured: (Uri) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    // ---- runtime CAMERA permission
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val requestPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    // ---- CameraX use-cases
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    Column(modifier.fillMaxSize()) {
        // Simple header (no experimental Material API)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Take Photo",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
            Button(onClick = onCancel) { Text("Back") }
        }

        if (!hasPermission) {
            Column(
                Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Camera permission is required to take a photo.")
                Spacer(Modifier.height(16.dp))
                Button(onClick = { requestPermission.launch(Manifest.permission.CAMERA) }) {
                    Text("Grant permission")
                }
            }
            return@Column
        }

        // ---- Camera preview
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }

                val providerFuture = ProcessCameraProvider.getInstance(ctx)
                providerFuture.addListener({
                    val cameraProvider = providerFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val selector = CameraSelector.DEFAULT_BACK_CAMERA

                    imageCapture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner, selector, preview, imageCapture
                        )
                    } catch (_: Exception) { /* ignore */ }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            }
        )

        Divider()

        // ---- Capture button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = {
                val ic = imageCapture ?: return@Button
                // Launch a coroutine -> call suspend takePhoto() safely
                scope.launch {
                    val uri = takePhoto(context, ic)
                    uri?.let(onCaptured)
                }
            }) {
                Text("Capture")
            }
        }
    }
}

/** Save the photo and return its Uri; null on error. */
private suspend fun takePhoto(
    context: Context,
    imageCapture: ImageCapture
): Uri? = withContext(Dispatchers.IO) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val name = "calorie_${System.currentTimeMillis()}.jpg"
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MyCalorieApp")
        }
        val resolver = context.contentResolver
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val uri = resolver.insert(collection, values) ?: return@withContext null

        val opts = ImageCapture.OutputFileOptions.Builder(resolver, uri, values).build()
        return@withContext imageCapture.awaitSave(context, opts) ?: uri
    } else {
        val file = File.createTempFile("calorie_", ".jpg", context.cacheDir)
        val opts = ImageCapture.OutputFileOptions.Builder(file).build()
        return@withContext imageCapture.awaitSave(context, opts) ?: Uri.fromFile(file)
    }
}

/** Await CameraX save callback without lifecycleScope. */
private suspend fun ImageCapture.awaitSave(
    context: Context,
    outputOptions: ImageCapture.OutputFileOptions
): Uri? = suspendCancellableCoroutine { cont ->
    this.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                cont.resume(output.savedUri)
            }
            override fun onError(exception: ImageCaptureException) {
                cont.resume(null)
            }
        }
    )
}
