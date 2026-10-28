package com.group3.lostandfound.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.group3.lostandfound.MainActivity
import com.group3.lostandfound.R

/**
 * Foreground Service for "Draw Over Other Apps" / System Alert Window.
 * Displays a draggable floating chat head & quick-assist bubble over other applications,
 * allowing instant multitasking and seamless communication for Lost & Found item reports.
 */
class FloatingOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var floatingBubbleView: View? = null
    private var windowParams: WindowManager.LayoutParams? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (!canDrawOverlays(this)) {
            Log.w(TAG, "Draw over other apps permission not granted. Stopping overlay service.")
            stopSelf()
            return START_NOT_STICKY
        }

        startForegroundServiceNotification()
        showFloatingBubble()

        return START_STICKY
    }

    private fun startForegroundServiceNotification() {
        createNotificationChannel()

        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("open_tab", "messages")
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Foundly Floating Assistant Active")
            .setContentText("Tap or drag the floating bubble over any app to access live chat.")
            .setSmallIcon(R.drawable.ic_nav_messages)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Foundly Floating Overlay",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows status of the active floating chat head overlay"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun showFloatingBubble() {
        if (floatingBubbleView != null) return

        try {
            windowManager = getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            val inflater = LayoutInflater.from(this)
            floatingBubbleView = inflater.inflate(R.layout.layout_floating_bubble, null)

            val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 40
                y = 300
            }
            windowParams = params

            val cardBubble = floatingBubbleView?.findViewById<View>(R.id.cardFloatingBubble)
            val btnDismiss = floatingBubbleView?.findViewById<View>(R.id.btnDismissBubble)

            // Draggable touch handling
            cardBubble?.setOnTouchListener(object : View.OnTouchListener {
                private var initialX = 0
                private var initialY = 0
                private var initialTouchX = 0f
                private var initialTouchY = 0f
                private var isDragging = false
                private var touchStartTime = 0L

                override fun onTouch(v: View?, event: MotionEvent): Boolean {
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            initialX = params.x
                            initialY = params.y
                            initialTouchX = event.rawX
                            initialTouchY = event.rawY
                            isDragging = false
                            touchStartTime = System.currentTimeMillis()
                            return true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            val dx = (event.rawX - initialTouchX).toInt()
                            val dy = (event.rawY - initialTouchY).toInt()
                            if (Math.abs(dx) > 12 || Math.abs(dy) > 12) {
                                isDragging = true
                            }
                            if (isDragging) {
                                params.x = initialX + dx
                                params.y = initialY + dy
                                try {
                                    windowManager?.updateViewLayout(floatingBubbleView, params)
                                } catch (_: Exception) {}
                            }
                            return true
                        }
                        MotionEvent.ACTION_UP -> {
                            val clickDuration = System.currentTimeMillis() - touchStartTime
                            if (!isDragging && clickDuration < 300) {
                                // Tap action: launch app into Live Chat / Messages
                                val launchIntent = Intent(this@FloatingOverlayService, MainActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                    putExtra("open_tab", "messages")
                                }
                                startActivity(launchIntent)
                            }
                            return true
                        }
                    }
                    return false
                }
            })

            // Close button to dismiss overlay
            btnDismiss?.setOnClickListener {
                stopSelf()
            }

            windowManager?.addView(floatingBubbleView, params)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to display floating bubble: ${e.message}", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        floatingBubbleView?.let { view ->
            try {
                windowManager?.removeView(view)
            } catch (_: Exception) {}
        }
        floatingBubbleView = null
    }

    companion object {
        private const val TAG = "FloatingOverlayService"
        private const val CHANNEL_ID = "foundly_floating_overlay_channel"
        private const val NOTIFICATION_ID = 4040

        const val ACTION_START = "com.group3.lostandfound.action.START_OVERLAY"
        const val ACTION_STOP = "com.group3.lostandfound.action.STOP_OVERLAY"

        var isServiceRunning = false
            private set

        /**
         * Checks if the app has the "Draw Over Other Apps" / SYSTEM_ALERT_WINDOW permission.
         */
        fun canDrawOverlays(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else {
                true
            }
        }

        /**
         * Starts the floating overlay service if permission is granted.
         */
        fun startService(context: Context) {
            if (!canDrawOverlays(context)) return
            val intent = Intent(context, FloatingOverlayService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        /**
         * Stops the floating overlay service.
         */
        fun stopService(context: Context) {
            val intent = Intent(context, FloatingOverlayService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
