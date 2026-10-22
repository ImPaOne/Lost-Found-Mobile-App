package com.group3.lostandfound.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import android.util.LruCache
import android.widget.ImageView
import com.group3.lostandfound.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * High-performance, zero-dependency asynchronous ImageLoader supporting:
 * - Base64 data URIs (data:image/jpeg;base64,...)
 * - Local content:// and file:// URIs
 * - Remote HTTP / HTTPS URLs (Firebase Storage, Cloudinary, etc.)
 * - Memory caching via LruCache with automatic garbage collection
 */
object ImageLoader {

    private const val TAG = "ImageLoader"
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 8

    private val memoryCache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    fun clearCache() {
        memoryCache.evictAll()
    }

    fun load(
        imageView: ImageView,
        url: String?,
        placeholderRes: Int = R.drawable.ic_item_placeholder,
        scope: CoroutineScope
    ): Job? {
        if (url.isNullOrBlank()) {
            imageView.setImageResource(placeholderRes)
            return null
        }

        // Check in-memory cache
        val cached = memoryCache.get(url)
        if (cached != null) {
            imageView.setImageBitmap(cached)
            return null
        }

        imageView.setImageResource(placeholderRes)
        imageView.tag = url

        return scope.launch(Dispatchers.IO) {
            val bitmap = resolveBitmap(imageView.context, url)
            if (bitmap != null) {
                memoryCache.put(url, bitmap)
                withContext(Dispatchers.Main) {
                    if (imageView.tag == url) {
                        imageView.setImageBitmap(bitmap)
                    }
                }
            }
        }
    }

    private fun resolveBitmap(context: Context, source: String): Bitmap? {
        return try {
            when {
                // 1. Base64 Data URI (e.g. data:image/jpeg;base64,...)
                source.startsWith("data:image", ignoreCase = true) -> {
                    val base64Index = source.indexOf("base64,")
                    val rawBase64 = if (base64Index != -1) source.substring(base64Index + 7) else source
                    val imageBytes = Base64.decode(rawBase64.trim(), Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                }

                // 2. Android Content URI (e.g. content://media/...)
                source.startsWith("content://", ignoreCase = true) -> {
                    context.contentResolver.openInputStream(Uri.parse(source))?.use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                }

                // 3. File URI or Absolute File Path
                source.startsWith("file://", ignoreCase = true) || source.startsWith("/data/") -> {
                    val cleanPath = if (source.startsWith("file://", ignoreCase = true)) {
                        source.substring(7)
                    } else {
                        source
                    }
                    val file = File(cleanPath)
                    if (file.exists() && file.canRead()) {
                        FileInputStream(file).use { stream ->
                            BitmapFactory.decodeStream(stream)
                        }
                    } else {
                        null
                    }
                }

                // 4. Remote HTTP / HTTPS URL
                source.startsWith("http://", ignoreCase = true) || source.startsWith("https://", ignoreCase = true) -> {
                    downloadRemoteBitmap(source)
                }

                // 5. Fallback pure base64 without prefix
                source.length > 100 && !source.contains("://") -> {
                    try {
                        val imageBytes = Base64.decode(source.trim(), Base64.DEFAULT)
                        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    } catch (_: Exception) {
                        null
                    }
                }

                else -> null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resolve bitmap for source (${source.take(40)}...): ${e.message}")
            null
        }
    }

    private fun downloadRemoteBitmap(imageUrl: String): Bitmap? {
        var connection: HttpURLConnection? = null
        var inputStream: InputStream? = null
        return try {
            val url = URL(imageUrl)
            connection = (url.openConnection() as HttpURLConnection).apply {
                doInput = true
                instanceFollowRedirects = true
                connectTimeout = 12000
                readTimeout = 12000
                connect()
            }
            if (connection.responseCode in 200..299) {
                inputStream = connection.inputStream
                BitmapFactory.decodeStream(inputStream)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        } finally {
            try {
                inputStream?.close()
                connection?.disconnect()
            } catch (_: Exception) {}
        }
    }
}
