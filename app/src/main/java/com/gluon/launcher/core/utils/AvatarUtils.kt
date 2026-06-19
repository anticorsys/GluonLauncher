// app/src/main/java/com/gluon/launcher/core/utils/AvatarUtils.kt
package com.gluon.launcher.core.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object AvatarUtils {

    private const val MAX_AVATAR_SIZE_KB = 200
    private const val TARGET_WIDTH = 512
    private const val TARGET_HEIGHT = 512
    private const val JPEG_QUALITY = 85

    fun processImageUri(context: Context, uri: Uri): File? {
        return try {
            val appContext = context.applicationContext

            val orientation = getOrientationFromUri(appContext, uri)

            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }

            val boundsStream = appContext.contentResolver.openInputStream(uri) ?: return null
            boundsStream.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            var scale = 1
            while ((options.outWidth / scale) > TARGET_WIDTH || (options.outHeight / scale) > TARGET_HEIGHT) {
                scale *= 2
            }
            options.inJustDecodeBounds = false
            options.inSampleSize = scale

            var bitmap = appContext.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            } ?: return null

            bitmap = rotateBitmap(bitmap, orientation)

            // ОПТИМИЗАЦИЯ: Предотвращение OOM. Не создаем массив байтов на каждой итерации цикла.
            val bos = ByteArrayOutputStream()
            var quality = JPEG_QUALITY
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, bos)

            while (bos.size() > MAX_AVATAR_SIZE_KB * 1024 && quality > 30) {
                bos.reset()
                quality -= 10
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, bos)
            }

            val file = File(appContext.cacheDir, "gluon_upload_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { output ->
                // Используем writeTo напрямую, чтобы не вызывать toByteArray() и не аллоцировать дубликат массива
                bos.writeTo(output)
            }

            bitmap.recycle()
            bos.close()

            if (file.exists() && file.length() > 0) file else {
                file.delete()
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getOrientationFromUri(context: Context, uri: Uri): Int {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            } ?: 0
        } catch (_: Exception) {
            0
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, orientation: Int): Bitmap {
        if (orientation == 0) return bitmap
        val matrix = Matrix()
        matrix.postRotate(orientation.toFloat())
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated != bitmap) {
            bitmap.recycle()
        }
        return rotated
    }
}