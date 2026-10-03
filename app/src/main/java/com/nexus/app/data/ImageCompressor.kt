package com.nexus.app.data

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import kotlin.math.max

// Shrinks a photo before uploading: smaller size + JPEG quality 80.
// A 6 MB phone photo usually becomes 200-400 KB, which uploads much faster on slow data.
object ImageCompressor {
    fun compress(resolver: ContentResolver, uri: Uri, maxSide: Int = 1280, quality: Int = 80): ByteArray {
        // 1) Read only the photo's size (cheap)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val width = bounds.outWidth
        val height = bounds.outHeight
        if (width <= 0 || height <= 0) throw IllegalStateException("That file isn't a readable image.")

        // 2) Load a reduced version so a huge photo can't use up the phone's memory
        var sample = 1
        while (max(width, height) / (sample * 2) >= maxSide) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: throw IllegalStateException("Could not read that image.")

        // 3) Photos from the camera may be stored sideways; read the rotation note and fix it
        val orientation = try {
            resolver.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (e: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }
        val rotation = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }

        // 4) Scale down so the longest side is at most maxSide
        val scale = minOf(1f, maxSide.toFloat() / max(decoded.width, decoded.height))
        val matrix = Matrix().apply {
            postRotate(rotation)
            postScale(scale, scale)
        }
        val resized = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)

        // 5) JPEG has no transparency, so put the picture on a white background first
        val flat = Bitmap.createBitmap(resized.width, resized.height, Bitmap.Config.ARGB_8888)
        Canvas(flat).apply {
            drawColor(Color.WHITE)
            drawBitmap(resized, 0f, 0f, null)
        }

        val out = ByteArrayOutputStream()
        flat.compress(Bitmap.CompressFormat.JPEG, quality, out)
        return out.toByteArray()
    }
}
