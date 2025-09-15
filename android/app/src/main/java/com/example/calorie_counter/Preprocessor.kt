package com.example.calorie_counter

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint

object Preprocessor {
    const val INPUT_SIZE = 224

    /** Center-crop to square then scale to 224x224 (no fancy libs needed). */
    fun to224(bm: Bitmap): Bitmap {
        val w = bm.width
        val h = bm.height
        val side = minOf(w, h)
        val x = (w - side) / 2
        val y = (h - side) / 2
        val cropped = Bitmap.createBitmap(bm, x, y, side, side)

        val out = Bitmap.createBitmap(INPUT_SIZE, INPUT_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val m = Matrix()
        val scale = INPUT_SIZE.toFloat() / side
        m.setScale(scale, scale)
        val p = Paint(Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(cropped, m, p)
        return out
    }
}