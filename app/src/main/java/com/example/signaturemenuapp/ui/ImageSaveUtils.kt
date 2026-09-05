package com.example.signaturemenuapp.ui

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal suspend fun saveImageBitmapToPictures(
    context: Context,
    imageBitmap: ImageBitmap,
    fileName: String,
): Boolean = withContext(Dispatchers.IO) {
    val displayName = sanitizeImageFileName(fileName).ifBlank { "SignatureMenu" }
    val resolver = context.contentResolver
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "$displayName.png")
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/SignatureMenu")
        put(MediaStore.Images.Media.IS_PENDING, 1)
    }
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return@withContext false
    try {
        val saved = resolver.openOutputStream(uri)?.use { output ->
            imageBitmap.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, output)
        } == true
        if (!saved) {
            resolver.delete(uri, null, null)
            return@withContext false
        }
        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        true
    } catch (_: Exception) {
        resolver.delete(uri, null, null)
        false
    }
}

internal fun showImageSaveResult(context: Context, success: Boolean) {
    Toast.makeText(
        context,
        if (success) "图片已保存到相册" else "图片保存失败，请稍后再试",
        Toast.LENGTH_SHORT,
    ).show()
}

private fun sanitizeImageFileName(rawName: String): String {
    val invalidChars = setOf('\\', '/', ':', '*', '?', '"', '<', '>', '|')
    return rawName
        .trim()
        .map { if (it in invalidChars) '_' else it }
        .joinToString("")
        .take(80)
}
