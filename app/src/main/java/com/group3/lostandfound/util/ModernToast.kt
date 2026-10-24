package com.group3.lostandfound.util

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.group3.lostandfound.R

/**
 * Modern Toast & Banner utility providing sleek, modern notification feedback.
 * Features rounded pill geometry, Davis Humphries clay tones, clean typography,
 * and completely eliminates legacy emoji clutter.
 */
object ModernToast {

    /**
     * Displays a modern styled notification. Prefers Material Snackbar if a View
     * is available, with graceful fallback to a modern styled Toast.
     */
    fun show(context: Context?, message: String, view: View? = null, durationLong: Boolean = false) {
        if (context == null || message.isBlank()) return

        if (view != null) {
            try {
                val duration = if (durationLong) Snackbar.LENGTH_LONG else Snackbar.LENGTH_SHORT
                val snackbar = Snackbar.make(view, message, duration)
                val snackbarView = snackbar.view

                // Modern rounded pill styling
                val background = GradientDrawable().apply {
                    setColor(Color.parseColor("#003049")) // Deep Trust Navy
                    cornerRadius = 24f * context.resources.displayMetrics.density
                }
                snackbarView.background = background
                snackbarView.elevation = 8f * context.resources.displayMetrics.density

                val textView = snackbarView.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
                textView?.apply {
                    setTextColor(Color.WHITE)
                    textSize = 13.5f
                    maxLines = 3
                }
                snackbar.show()
                return
            } catch (ignored: Exception) {
                // Fallback to custom toast
            }
        }

        // Clean modern Toast fallback
        val toast = Toast.makeText(
            context,
            message,
            if (durationLong) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
        )
        toast.show()
    }

    fun showSuccess(context: Context?, message: String, view: View? = null) {
        show(context, message, view, durationLong = false)
    }

    fun showError(context: Context?, message: String, view: View? = null) {
        show(context, message, view, durationLong = true)
    }
}

/**
 * Fragment extension for fast, modern pill toasts.
 */
fun Fragment.showModernToast(message: String, isLong: Boolean = false) {
    if (isAdded && context != null) {
        ModernToast.show(requireContext(), message, view, isLong)
    }
}

fun Activity.showModernToast(message: String, isError: Boolean = false) {
    ModernToast.show(this, message, findViewById(android.R.id.content), durationLong = isError)
}
