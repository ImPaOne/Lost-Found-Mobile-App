package com.group3.lostandfound.util

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

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
     * Uploads compressed JPEG image bytes to Cloudinary.
     * Returns the secure HTTPS URL of the uploaded image.
     */
    suspend fun uploadImage(
        imageBytes: ByteArray,
        onProgress: (Int) -> Unit = {}
    ): String = withContext(Dispatchers.IO) {
        onProgress(30)
        try {
            val boundary = "Boundary-${System.currentTimeMillis()}"
            val url = URL(UPLOAD_URL)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                doInput = true
                useCaches = false
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                connectTimeout = 30000
                readTimeout = 30000
            }

            val base64Data = "data:image/jpeg;base64," + Base64.encodeToString(imageBytes, Base64.NO_WRAP)

            DataOutputStream(connection.outputStream).use { output ->
                // 1. file parameter (as data URI)
                output.writeBytes("--$boundary\r\n")
                output.writeBytes("Content-Disposition: form-data; name=\"file\"\r\n\r\n")
                output.writeBytes(base64Data)
                output.writeBytes("\r\n")

                // 2. api_key parameter
                output.writeBytes("--$boundary\r\n")
                output.writeBytes("Content-Disposition: form-data; name=\"api_key\"\r\n\r\n")
                output.writeBytes(API_KEY)
                output.writeBytes("\r\n")

                // 3. upload_preset parameter
                output.writeBytes("--$boundary\r\n")
                output.writeBytes("Content-Disposition: form-data; name=\"upload_preset\"\r\n\r\n")
                output.writeBytes("ml_default")
                output.writeBytes("\r\n")

                // 4. timestamp parameter
                val timestamp = (System.currentTimeMillis() / 1000).toString()
                output.writeBytes("--$boundary\r\n")
                output.writeBytes("Content-Disposition: form-data; name=\"timestamp\"\r\n\r\n")
                output.writeBytes(timestamp)
                output.writeBytes("\r\n")

                // End of multipart
                output.writeBytes("--$boundary--\r\n")
                output.flush()
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
                    onProgress(95)
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
