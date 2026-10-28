package com.group3.lostandfound.util

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * CloudinaryManager handles direct image uploads to Cloudinary storage
 * Cloud Name: zudf2f0g
 * API Key: 324692715343129
 */
object CloudinaryManager {

    private const val TAG = "CloudinaryManager"
    const val CLOUD_NAME = "zudf2f0g"
    const val API_KEY = "324692715343129"
    private const val UPLOAD_URL = "https://api.cloudinary.com/v1_1/$CLOUD_NAME/image/upload"

    /**
     * Uploads compressed JPEG image bytes to Cloudinary using standard URL-encoded form data.
     * Returns the secure HTTPS URL of the uploaded image.
     */
    suspend fun uploadImage(
        imageBytes: ByteArray,
        onProgress: (Int) -> Unit = {}
    ): String = withContext(Dispatchers.IO) {
        onProgress(25)
        try {
            val url = URL(UPLOAD_URL)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                doInput = true
                useCaches = false
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                connectTimeout = 30000
                readTimeout = 30000
            }

            val base64Data = "data:image/jpeg;base64," + Base64.encodeToString(imageBytes, Base64.NO_WRAP)
            val postData = "upload_preset=" + URLEncoder.encode("ml_default", "UTF-8") +
                    "&file=" + URLEncoder.encode(base64Data, "UTF-8")

            onProgress(50)

            OutputStreamWriter(connection.outputStream, "UTF-8").use { writer ->
                writer.write(postData)
                writer.flush()
            }

            onProgress(75)

            val responseCode = connection.responseCode
            val responseStream = if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }

            val responseText = BufferedReader(InputStreamReader(responseStream)).use { it.readText() }
            Log.d(TAG, "Cloudinary response ($responseCode): $responseText")

            if (responseCode in 200..299) {
                val json = JSONObject(responseText)
                val secureUrl = json.optString("secure_url")
                if (secureUrl.isNotEmpty()) {
                    onProgress(100)
                    return@withContext secureUrl
                }
            }

            throw java.io.IOException("Cloudinary upload failed (HTTP $responseCode): $responseText")
        } catch (e: Exception) {
            Log.e(TAG, "Cloudinary upload error: ${e.message}", e)
            throw e
        }
    }
}
