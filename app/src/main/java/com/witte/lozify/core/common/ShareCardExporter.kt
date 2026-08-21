package com.witte.lozify.core.common

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * Utility for capturing, exporting and sharing rendered note cards as images.
 */
object ShareCardExporter {

    /**
     * Capture a full-height Bitmap from an Android View (including ComposeView).
     * Measures full content dimensions so the entire long card is captured cleanly.
     */
    fun captureViewToBitmap(view: View): Bitmap {
        val width = if (view.width > 0) view.width else 1080
        val specWidth = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY)
        val specHeight = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        view.measure(specWidth, specHeight)
        val measuredHeight = view.measuredHeight.coerceAtLeast(view.height).coerceAtLeast(200)
        view.layout(0, 0, view.measuredWidth, measuredHeight)

        val bitmap = Bitmap.createBitmap(view.measuredWidth, measuredHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }

    /**
     * Save a bitmap image directly to the device's public photo gallery (Pictures/Lozify).
     */
    fun saveBitmapToGallery(context: Context, bitmap: Bitmap, filenamePrefix: String = "Lozify_Note"): Boolean {
        val fileName = "${filenamePrefix}_${System.currentTimeMillis()}.png"
        return try {
            val contentResolver = context.contentResolver
            val imageUri: Uri?

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Lozify")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                imageUri?.let { uri ->
                    contentResolver.openOutputStream(uri)?.use { outputStream ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    }
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    contentResolver.update(uri, contentValues, null, null)
                }
            } else {
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val lozifyDir = File(picturesDir, "Lozify").apply { if (!exists()) mkdirs() }
                val imageFile = File(lozifyDir, fileName)

                FileOutputStream(imageFile).use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                }

                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DATA, imageFile.absolutePath)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                }
                contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            }

            Toast.makeText(context, "图片已保存至相册", Toast.LENGTH_SHORT).show()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "保存失败: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            false
        }
    }

    /**
     * Share a bitmap image using Android's system share sheet (Intent.ACTION_SEND).
     */
    fun shareBitmap(context: Context, bitmap: Bitmap, shareText: String? = null) {
        try {
            val shareDir = File(context.cacheDir, "shared_images").apply {
                if (!exists()) mkdirs()
            }
            val imageFile = File(shareDir, "share_${System.currentTimeMillis()}.png")

            FileOutputStream(imageFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                imageFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                if (!shareText.isNullOrBlank()) {
                    putExtra(Intent.EXTRA_TEXT, shareText)
                }
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(shareIntent, "分享笔记卡片").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "分享失败: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}
