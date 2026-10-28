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

    // Connects to the Firebase Realtime Database's items collection.
    private val db = FirebaseDatabase.getInstance(FirebaseManager.DATABASE_URL).getReference("items")
    private val storage = FirebaseStorage.getInstance(FirebaseManager.STORAGE_BUCKET).getReference("item_images")

    // Opens the selected image using different URI access methods.
    private fun openInputStreamSafe(context: Context, uri: Uri): java.io.InputStream? {
        val scheme = uri.scheme

        // 1. Direct file access for file:// scheme or null scheme
        if (scheme == null || scheme == "file") {
            try {
                val path = uri.path ?: uri.toString().removePrefix("file://")
                val file = java.io.File(path)
                if (file.exists() && file.canRead()) {
                    return java.io.FileInputStream(file)
                }
            } catch (_: Exception) {}
        }

        // 2. Try standard ContentResolver
        try {
            val stream = context.contentResolver.openInputStream(uri)
            if (stream != null) return stream
        } catch (_: Exception) {}

        // 3. Fallback: Check if uri.path points to an existing file
        try {
            val path = uri.path
            if (path != null) {
                val file = java.io.File(path)
                if (file.exists() && file.canRead()) {
                    return java.io.FileInputStream(file)
                }
            }
        } catch (_: Exception) {}

        // 4. Fallback: Parse /data/user/0 or /data/data from uri string
        try {
            val uriString = uri.toString()
            val dataIndex = uriString.indexOf("/data/")
            if (dataIndex != -1) {
                val extractedPath = uriString.substring(dataIndex)
                val file = java.io.File(extractedPath)
                if (file.exists() && file.canRead()) {
                    return java.io.FileInputStream(file)
                }
            }
        } catch (_: Exception) {}

        return null
    }

    // Compresses and resizes the image into JPEG bytes.
    suspend fun compressImageUri(
        context: Context,
        imageUri: Uri,
        maxWidth: Int = 1280,
        maxHeight: Int = 1280,
        quality: Int = 80
    ): ByteArray = withContext(Dispatchers.IO) {
        val rawBytes = openInputStreamSafe(context, imageUri)?.use { it.readBytes() }
            ?: throw IllegalArgumentException("Cannot open stream for image URI: $imageUri")

        if (rawBytes.isEmpty()) {
            throw IllegalArgumentException("Image at URI is empty: $imageUri")
        }

        // 1. Determine bounds
        val boundsOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, boundsOptions)

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
        val bitmap = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOptions)
            ?: throw IllegalArgumentException("Failed to decode image from URI: $imageUri")

        // 3. Compress to byte array
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        bitmap.recycle()
        outputStream.toByteArray()
    }

    // Uploads the image and saves the report to Firebase.
    suspend fun uploadImageAndCreateReport(
        context: Context,
        imageUri: Uri?,
        report: ItemReport,
        maxRetries: Int = 3,
        onProgress: (Int) -> Unit // Reports upload progress to the ViewModel.
    ): Result<ItemReport> = withContext(Dispatchers.IO) {
        // Stores the most recent exception if an attempt fails.
        var lastException: Exception? = null

        // Retries the submission if an operation fails.
        for (attempt in 1..maxRetries) {
            try {
                // Generate unique key using push()
                val itemId = db.push().key ?: UUID.randomUUID().toString()
                // Starts with any image URL already present in the report.
                var downloadUrl = report.imageUrl

                if (imageUri != null) {
                    onProgress(10)
                    try {
                        val compressedBytes = compressImageUri(context, imageUri, maxWidth = 1024, maxHeight = 1024, quality = 75)
                        var uploaded = false

                        // Tier 1: Try Cloudinary direct upload
                        try {
                            downloadUrl = com.group3.lostandfound.util.CloudinaryManager.uploadImage(compressedBytes) { cloudProgress ->
                                onProgress(cloudProgress)
                            }
                            if (downloadUrl.startsWith("http://") || downloadUrl.startsWith("https://")) {
                                uploaded = true
                            }
                        } catch (cEx: Exception) {
                            android.util.Log.w("ReportRepository", "Tier 1 Cloudinary upload failed: ${cEx.message}")
                        }

                        // Tier 2: Try Firebase Storage if Cloudinary didn't complete
                        if (!uploaded) {
                            try {
                                onProgress(60)
                                val storageRef = com.google.firebase.storage.FirebaseStorage.getInstance(FirebaseManager.STORAGE_BUCKET)
                                    .getReference("reports")
                                    .child("${itemId}_${System.currentTimeMillis()}.jpg")
                                storageRef.putBytes(compressedBytes).await()
                                downloadUrl = storageRef.downloadUrl.await().toString()
                                uploaded = true
                            } catch (sEx: Exception) {
                                android.util.Log.w("ReportRepository", "Tier 2 Firebase Storage upload failed: ${sEx.message}")
                            }
                        }

                        // Tier 3: Guaranteed 100% resilient Base64 data URI fallback
                        if (!uploaded) {
                            android.util.Log.i("ReportRepository", "Using Tier 3 Base64 storage fallback")
                            val base64Str = android.util.Base64.encodeToString(compressedBytes, android.util.Base64.NO_WRAP)
                            downloadUrl = "data:image/jpeg;base64,$base64Str"
                        }
                    } catch (imgEx: Exception) {
                        android.util.Log.e("ReportRepository", "Image compression failed: ${imgEx.message}", imgEx)
                    }
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
