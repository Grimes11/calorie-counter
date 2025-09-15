package com.example.calorie_counter

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

data class Prediction(val label: String, val score: Float, val index: Int)

class ImageClassifier(
    context: Context,
    private val labels: List<String>,
    threads: Int = 3
) : AutoCloseable {

    private val interpreter: Interpreter
    private val inputScale: Float
    private val inputZeroPoint: Int
    private val outputScale: Float
    private val outputZeroPoint: Int

    init {
        val model = FileUtil.loadMappedFile(context, "model/food_int8.tflite")
        val opts = Interpreter.Options().apply { setNumThreads(threads) }
        interpreter = Interpreter(model, opts)

        // Quantization params (UINT8) so we can quantize/dequantize correctly.
        val inTensor = interpreter.getInputTensor(0)
        inputScale = inTensor.quantizationParams().scale
        inputZeroPoint = inTensor.quantizationParams().zeroPoint

        val outTensor = interpreter.getOutputTensor(0)
        outputScale = outTensor.quantizationParams().scale
        outputZeroPoint = outTensor.quantizationParams().zeroPoint

        require(labels.isNotEmpty()) { "Labels list is empty" }
    }

    /** Classify a bitmap and return top-K (default 3) predictions. */
    fun classify(src: Bitmap, topK: Int = 3): List<Prediction> {
        val b224 = Preprocessor.to224(src)
        val input = quantizeToUint8Buffer(b224) // [1,224,224,3] UINT8

        // Output buffer: [1, numClasses] UINT8
        val numClasses = labels.size
        val out = ByteArray(numClasses)
        val outputs = arrayOf(out)

        interpreter.run(input, outputs)

        // Dequantize to float and softmax for stable probabilities.
        val logits = FloatArray(numClasses) { i ->
            outputScale * ( (out[i].toInt() and 0xFF) - outputZeroPoint )
        }
        val probs = softmax(logits)

        // Build & sort
        val preds = probs.mapIndexed { i, p -> Prediction(labels[i], p, i) }
            .sortedByDescending { it.score }
            .take(topK)

        return preds
    }

    override fun close() {
        interpreter.close()
    }

    /** Convert ARGB_8888 pixels → normalized [-1,1] → UINT8 using tensor quant params. */
    private fun quantizeToUint8Buffer(bm: Bitmap): ByteBuffer {
        val w = bm.width
        val h = bm.height
        val input = ByteBuffer.allocateDirect(1 * w * h * 3)
        input.order(ByteOrder.nativeOrder())

        val pixels = IntArray(w * h)
        bm.getPixels(pixels, 0, w, 0, 0, w, h)

        // Convert each pixel
        for (y in 0 until h) {
            for (x in 0 until w) {
                val c = pixels[y * w + x]
                val r = (c shr 16) and 0xFF
                val g = (c shr 8) and 0xFF
                val b = c and 0xFF

                // MobileNetV2 training used [-1, 1] normalization: (v/127.5) - 1
                val rf = (r / 127.5f) - 1f
                val gf = (g / 127.5f) - 1f
                val bf = (b / 127.5f) - 1f

                // Quantize float → uint8 using input scale/zeroPoint.
                input.put(floatToUint8(rf))
                input.put(floatToUint8(gf))
                input.put(floatToUint8(bf))
            }
        }
        input.rewind()
        return input
    }

    private fun floatToUint8(v: Float): Byte {
        // q = round(v / scale + zeroPoint), clamp 0..255
        val q = (v / inputScale + inputZeroPoint).toInt()
        val clamped = min(255, max(0, q))
        return clamped.toByte()
    }

    private fun softmax(x: FloatArray): FloatArray {
        // Numerically stable softmax
        val maxX = x.maxOrNull() ?: 0f
        var sum = 0.0
        val exps = DoubleArray(x.size)
        for (i in x.indices) {
            val e = exp((x[i] - maxX).toDouble())
            exps[i] = e
            sum += e
        }
        val out = FloatArray(x.size)
        for (i in x.indices) out[i] = (exps[i] / sum).toFloat()
        return out
    }
}