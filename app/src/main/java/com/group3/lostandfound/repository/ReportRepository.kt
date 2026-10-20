package com.group3.lostandfound.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.group3.lostandfound.model.ItemReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import com.group3.lostandfound.FirebaseManager
import java.util.UUID

/**
 * Clean Architecture Repository handling report submission, image compression,
 * Firebase Storage uploads (/item_images/{itemId}.jpg), and Realtime Database writes (/items/{itemId}).
 */
class ReportRepository {

    private val db = FirebaseDatabase.getInstance(FirebaseManager.DATABASE_URL).getReference("items")
    private val storage = FirebaseStorage.getInstance(FirebaseManager.STORAGE_BUCKET).getReference("item_images")

    /**
     * Compresses the selected image URI into JPEG byte array, downscaling to prevent out-of-memory errors.
     */
    suspend fun compressImageUri(
        context: Context,
        imageUri: Uri,
        maxWidth: Int = 1280,
        maxHeight: Int = 1280,
        quality: Int = 80
    ): ByteArray = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver

        // 1. Determine bounds
        val boundsOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        resolver.openInputStream(imageUri)?.use { input ->
            BitmapFactory.decodeStream(input, null, boundsOptions)
        } ?: throw IllegalArgumentException("Cannot open stream for image URI: $imageUri")

        var sampleSize = 1
        val origWidth = boundsOptions.outWidth
        val origHeight = boundsOptions.outHeight
        if (origWidth > maxWidth || origHeight > maxHeight) {
            val halfWidth = origWidth / 2
            val halfHeight = origHeight / 2
            while ((halfWidth / sampleSize) >= maxWidth && (halfHeight / sampleSize) >= maxHeight) {
                sampleSize *= 2
            }
        }

        // 2. Decode sampled image
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
        }
        val bitmap = resolver.openInputStream(imageUri)?.use { input ->
            BitmapFactory.decodeStream(input, null, decodeOptions)
        } ?: throw IllegalArgumentException("Failed to decode image from URI: $imageUri")

        // 3. Compress to byte array
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        bitmap.recycle()
        outputStream.toByteArray()
    }

    /**
     * Uploads report image bytes to Firebase Storage, retrieves download URL, and writes payload to Realtime Database.
     * Includes automatic retry mechanism for network resilience and real-time progress callbacks (0-100%).
     */
    suspend fun uploadImageAndCreateReport(
        context: Context,
        imageUri: Uri?,
        report: ItemReport,
        maxRetries: Int = 3,
        onProgress: (Int) -> Unit
    ): Result<ItemReport> = withContext(Dispatchers.IO) {
        var lastException: Exception? = null

        for (attempt in 1..maxRetries) {
            try {
                // Generate unique key using push()
                val itemId = db.push().key ?: UUID.randomUUID().toString()
                var downloadUrl = report.imageUrl

                if (imageUri != null) {
                    onProgress(5)
                    val compressedBytes = compressImageUri(context, imageUri)

                    // Upload to Firebase Storage path /item_images/{itemId}.jpg
                    val imageRef = storage.child("$itemId.jpg")
                    val uploadTask = imageRef.putBytes(compressedBytes)

                    uploadTask.addOnProgressListener { snapshot ->
                        if (snapshot.totalByteCount > 0) {
                            val percent = (100.0 * snapshot.bytesTransferred / snapshot.totalByteCount).toInt()
                            val mappedPercent = 5 + (percent * 0.85).toInt()
                            onProgress(mappedPercent.coerceIn(5, 90))
                        }
                    }.await()

                    // Retrieve download URL via await()
                    downloadUrl = imageRef.downloadUrl.await().toString()
                    onProgress(95)
                } else {
                    onProgress(50)
                }

                // Create complete ItemReport payload
                val finalReport = report.copy(
                    itemId = itemId,
                    imageUrl = downloadUrl,
                    timestamp = if (report.timestamp > 0L) report.timestamp else System.currentTimeMillis()
                )

                // Write complete payload to /items/{itemId}
                db.child(itemId).setValue(finalReport.toMap()).await()
                onProgress(100)

                return@withContext Result.success(finalReport)

            } catch (e: Exception) {
                lastException = e
                if (attempt < maxRetries) {
                    delay(1000L * attempt)
                }
            }
        }

        Result.failure(lastException ?: IllegalStateException("Failed to upload report after $maxRetries attempts"))
    }
}
