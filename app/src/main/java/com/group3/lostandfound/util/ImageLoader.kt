package com.group3.lostandfound.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import android.widget.ImageView
import com.group3.lostandfound.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Lightweight, zero-dependency asynchronous ImageLoader powered by Kotlin Coroutines and LruCache.
 */
object ImageLoader {

    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 8

    private val memoryCache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
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
            val bitmap = downloadBitmap(url)
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

    private fun downloadBitmap(imageUrl: String): Bitmap? {
        var connection: HttpURLConnection? = null
        var inputStream: InputStream? = null
        return try {
            val url = URL(imageUrl)
            connection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.connect()
            inputStream = connection.inputStream
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            null
        } finally {
            try {
                inputStream?.close()
                connection?.disconnect()
            } catch (e: Exception) {
                // Ignore close error
            }
        }
    }
}
