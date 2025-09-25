package com.ttri.clockapp

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream

object ImageUtils {
    fun resizeImageIfNeeded(file: File, maxWidth: Int = 640, maxHeight: Int = 480) {
        val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return

        if (bitmap.width <= maxWidth && bitmap.height <= maxHeight) {
            return // already small enough
        }

        val ratio = minOf(maxWidth.toFloat() / bitmap.width, maxHeight.toFloat() / bitmap.height)
        val newWidth = (bitmap.width * ratio).toInt()
        val newHeight = (bitmap.height * ratio).toInt()

        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)

        FileOutputStream(file).use { out ->
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }

        scaledBitmap.recycle()
        bitmap.recycle()
    }
}
