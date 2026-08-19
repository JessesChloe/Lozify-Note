package com.witte.lozify.core.common

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

/**
 * ImageUtils - Utility object for image processing and storage management.
 *
 * Stage 6: Handles image selection, compression, privatization, and cleanup.
 *
 * Key Features:
 * - Copy images from external storage to app's private directory
 * - Compress images to reduce storage usage
 * - Handle EXIF orientation correction
 * - Generate unique filenames to prevent conflicts
 * - Clean up orphaned image files
 *
 * Security:
 * - All images stored in app private directory (context.filesDir/images/)
 * - External deletion won't break app's image references
 * - No READ_EXTERNAL_STORAGE permission needed after copying
 */
object ImageUtils {

    private const val IMAGES_DIR = "images"
    private const val MAX_IMAGE_DIMENSION = 2048 // Max width/height in pixels
    private const val JPEG_QUALITY = 85 // Compression quality (0-100)

    /**
     * Copy image from external URI to app's private storage.
     *
     * Process:
     * 1. Read image from source URI with sampling to prevent OOM
     * 2. Correct EXIF orientation
     * 3. Compress to max dimension and quality
     * 4. Save to private storage with unique filename
     * 5. Return private file URI
     *
     * @param context Android context
     * @param sourceUri External image URI (from gallery, camera, etc.)
     * @param enableCompression Whether to apply 2K visually lossless compression (Stage 37)
     * @return Internal file URI, or null if operation failed
     */
    fun copyImageToPrivateStorage(
        context: Context,
        sourceUri: Uri,
        enableCompression: Boolean = true
    ): Uri? {
        if (!enableCompression) {
            // Direct lossless raw copy mode (Original untouched stream)
            return try {
                val filename = "${UUID.randomUUID()}.jpg"
                val imagesDir = File(context.filesDir, IMAGES_DIR)
                if (!imagesDir.exists()) {
                    imagesDir.mkdirs()
                }
                val destFile = File(imagesDir, filename)
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                Uri.fromFile(destFile)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        var originalBitmap: Bitmap? = null
        var rotatedBitmap: Bitmap? = null
        var compressedBitmap: Bitmap? = null

        return try {
            // Stage 6 Fix: Use inSampleSize to prevent OOM when loading large images
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }

            // First pass: get image dimensions without loading into memory
            context.contentResolver.openInputStream(sourceUri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            // Calculate sample size to reduce memory usage
            options.inSampleSize = calculateInSampleSize(options, MAX_IMAGE_DIMENSION, MAX_IMAGE_DIMENSION)
            options.inJustDecodeBounds = false

            // Second pass: load sampled bitmap
            context.contentResolver.openInputStream(sourceUri)?.use { stream ->
                originalBitmap = BitmapFactory.decodeStream(stream, null, options)
            }

            if (originalBitmap == null) {
                return null
            }

            // Correct EXIF orientation
            rotatedBitmap = correctOrientation(context, sourceUri, originalBitmap!!)

            // Compress bitmap if larger than max dimension
            compressedBitmap = compressBitmap(rotatedBitmap)

            // Generate unique filename
            val filename = "${UUID.randomUUID()}.jpg"
            val imagesDir = File(context.filesDir, IMAGES_DIR)
            if (!imagesDir.exists()) {
                imagesDir.mkdirs()
            }
            val destFile = File(imagesDir, filename)

            // Save compressed bitmap to private storage
            FileOutputStream(destFile).use { outputStream ->
                compressedBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)
            }

            // Return private file URI
            Uri.fromFile(destFile)
        } catch (e: OutOfMemoryError) {
            e.printStackTrace()
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            // Clean up bitmaps to prevent memory leaks
            try {
                if (rotatedBitmap != null && rotatedBitmap != originalBitmap) {
                    rotatedBitmap?.recycle()
                }
                if (compressedBitmap != null && compressedBitmap != rotatedBitmap) {
                    compressedBitmap?.recycle()
                }
                originalBitmap?.recycle()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Calculate optimal inSampleSize to prevent OOM when loading large images.
     *
     * @param options BitmapFactory.Options with image dimensions
     * @param reqWidth Required width
     * @param reqHeight Required height
     * @return Sample size (power of 2)
     */
    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight &&
                   (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    /**
     * Correct image orientation based on EXIF data.
     *
     * Some cameras save images in incorrect orientation and store rotation
     * info in EXIF. This function reads EXIF and rotates bitmap accordingly.
     *
     * @param context Android context
     * @param uri Image URI
     * @param bitmap Original bitmap
     * @return Correctly oriented bitmap
     */
    private fun correctOrientation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val exif = inputStream?.let { ExifInterface(it) }
            inputStream?.close()

            val orientation = exif?.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            ) ?: ExifInterface.ORIENTATION_NORMAL

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                else -> return bitmap // No rotation needed
            }

            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: Exception) {
            e.printStackTrace()
            bitmap
        }
    }

    /**
     * Compress bitmap to max dimension while maintaining aspect ratio.
     *
     * @param bitmap Original bitmap
     * @return Compressed bitmap (or original if already small enough)
     */
    private fun compressBitmap(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        // Check if compression needed
        if (width <= MAX_IMAGE_DIMENSION && height <= MAX_IMAGE_DIMENSION) {
            return bitmap
        }

        // Calculate scale factor
        val scale = if (width > height) {
            MAX_IMAGE_DIMENSION.toFloat() / width
        } else {
            MAX_IMAGE_DIMENSION.toFloat() / height
        }

        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * Delete image file from private storage.
     *
     * @param context Android context
     * @param uri File URI to delete
     * @return true if deletion succeeded
     */
    fun deleteImage(context: Context, uri: Uri): Boolean {
        return try {
            val file = File(uri.path ?: return false)
            if (file.exists() && file.parentFile?.name == IMAGES_DIR) {
                file.delete()
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Clean up orphaned images (files not referenced by any attachment).
     *
     * Call this periodically or during app maintenance.
     *
     * @param context Android context
     * @param referencedPaths List of image paths currently in use
     * @return Number of files deleted
     */
    fun cleanupOrphanedImages(context: Context, referencedPaths: List<String>): Int {
        val imagesDir = File(context.filesDir, IMAGES_DIR)
        if (!imagesDir.exists()) {
            return 0
        }

        val referencedSet = referencedPaths.toSet()
        var deletedCount = 0

        imagesDir.listFiles()?.forEach { file ->
            if (file.isFile && !referencedSet.contains(file.absolutePath)) {
                if (file.delete()) {
                    deletedCount++
                }
            }
        }

        return deletedCount
    }

    /**
     * Get total size of all images in private storage.
     *
     * @param context Android context
     * @return Total size in bytes
     */
    fun getTotalImageSize(context: Context): Long {
        val imagesDir = File(context.filesDir, IMAGES_DIR)
        if (!imagesDir.exists()) {
            return 0L
        }

        var totalSize = 0L
        imagesDir.listFiles()?.forEach { file ->
            if (file.isFile) {
                totalSize += file.length()
            }
        }

        return totalSize
    }

    /**
     * Save an image file from app private storage to the system gallery (Pictures/Lozify/).
     * Uses Scoped Storage (MediaStore) compatible with Android 10+ without needing dangerous permissions.
     *
     * @param context Android context
     * @param imageFile The local image file to save
     * @return true if successfully saved, false otherwise
     */
    fun saveImageToGallery(context: Context, imageFile: File): Boolean {
        if (!imageFile.exists() || imageFile.length() == 0L) {
            return false
        }

        return try {
            val filename = "Lozify_${System.currentTimeMillis()}.jpg"
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Lozify")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    imageFile.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    context.contentResolver.update(uri, contentValues, null, null)
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
