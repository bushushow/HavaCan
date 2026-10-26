package com.kampplus.havacan.data.wardrobe

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Copies picked or captured photos into the app's private storage (files/wardrobe), upright and
 * at most [MAX_SIDE] px, so the wardrobe never depends on a temporary content URI.
 */
class PhotoStore(private val context: Context) {
    private val dir = File(context.filesDir, "wardrobe").apply { mkdirs() }

    /** A file the camera app can write into, exposed through our FileProvider. */
    fun newCameraFile(): File = File(context.cacheDir, "camera").apply { mkdirs() }.let { File(it, "capture.jpg") }

    suspend fun import(uri: Uri): File = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        // Bounds-only decoding always returns null; success shows in outWidth
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) error("Fotoğraf açılamadı")
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= MAX_SIDE || bounds.outHeight / (sample * 2) >= MAX_SIDE) sample *= 2
        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: error("Fotoğraf okunamadı")
        val rotation = resolver.openInputStream(uri)?.use { exifRotation(ExifInterface(it)) } ?: 0
        val upright = scaleAndRotate(decoded, rotation)
        val file = File(dir, "${UUID.randomUUID()}.jpg")
        file.outputStream().use { upright.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        if (upright !== decoded) decoded.recycle()
        upright.recycle()
        file
    }

    fun delete(path: String) {
        File(path).takeIf { it.parentFile == dir }?.delete()
    }

    /** Removes photos no clothing item points to (editor left by a crash or a killed app). */
    suspend fun deleteOrphans(keep: Set<String>): Int = withContext(Dispatchers.IO) {
        deleteOrphans(dir, keep, System.currentTimeMillis())
    }

    /** Small pixel grid for on-device colour detection. */
    suspend fun pixels(path: String, side: Int = 96): Triple<IntArray, Int, Int>? = withContext(Dispatchers.IO) {
        val bitmap = decodeSampled(path, side * 2) ?: return@withContext null
        val scaled = Bitmap.createScaledBitmap(bitmap, side, side * bitmap.height / bitmap.width.coerceAtLeast(1), true)
        val pixels = IntArray(scaled.width * scaled.height)
        scaled.getPixels(pixels, 0, scaled.width, 0, 0, scaled.width, scaled.height)
        Triple(pixels, scaled.width, scaled.height)
    }

    /** Compressed copy for the AI backend, ~512 px, a few dozen KB. */
    suspend fun jpegForUpload(path: String, side: Int = 512): ByteArray? = withContext(Dispatchers.IO) {
        val bitmap = decodeSampled(path, side) ?: return@withContext null
        ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
            out.toByteArray()
        }
    }

    private fun decodeSampled(path: String, side: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0) return null
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= side && bounds.outHeight / (sample * 2) >= side) sample *= 2
        return BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
    }

    private fun scaleAndRotate(bitmap: Bitmap, rotation: Int): Bitmap {
        val scale = minOf(1f, MAX_SIDE.toFloat() / maxOf(bitmap.width, bitmap.height))
        if (rotation == 0 && scale == 1f) return bitmap
        val matrix = Matrix().apply {
            postScale(scale, scale)
            postRotate(rotation.toFloat())
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun exifRotation(exif: ExifInterface): Int =
        when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }

    companion object {
        private const val MAX_SIDE = 1280

        /** Files younger than this may belong to an import that is still in progress. */
        private const val ORPHAN_MIN_AGE_MS = 10 * 60 * 1000L

        internal fun deleteOrphans(dir: File, keep: Set<String>, now: Long): Int = dir.listFiles().orEmpty().count { file ->
            file.isFile && file.absolutePath !in keep && now - file.lastModified() > ORPHAN_MIN_AGE_MS && file.delete()
        }
    }
}
