package com.group3.lostandfound.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.group3.lostandfound.FirebaseManager
import com.group3.lostandfound.MainActivity
import com.group3.lostandfound.R

/**
 * FCMService handles incoming push messages from Firebase Cloud Messaging.
 * Notifications are delivered even when the app is closed or the phone is locked.
 * Uses Apple-style chime sound stored in res/raw/apple_chime.wav.
 */
class FCMService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM token refreshed")
        saveTokenLocally(token)
        sendTokenToServer(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "Foundly"

        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: remoteMessage.data["message"]
            ?: "New update on your item."

        val itemId = remoteMessage.data["itemId"]
        showNotification(title, body, itemId)
    }

    private fun saveTokenLocally(token: String) {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_FCM_TOKEN, token).apply()
    }

    private fun sendTokenToServer(token: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseDatabase.getInstance(FirebaseManager.DATABASE_URL)
            .getReference("users").child(uid).child("fcmToken")
            .setValue(token)
    }

    private fun showNotification(title: String, body: String, itemId: String?) {
        createNotificationChannel(this)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            itemId?.let { putExtra("extra_item_id", it) }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Selected Apple tone sound URI from findit_prefs
        val prefs = getSharedPreferences("findit_prefs", Context.MODE_PRIVATE)
        val soundRes = prefs.getInt("selected_notification_sound", R.raw.apple_chime)
        val soundUri = Uri.parse("android.resource://$packageName/$soundRes")

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_nav_my_reports)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 250, 150, 250))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), notification)
        } catch (e: Throwable) {
            Log.e(TAG, "Notification failed: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "FCMService"
        const val CHANNEL_ID = "foundly_alerts_v3"
        private const val PREFS_NAME = "lost_found_prefs"
        private const val KEY_FCM_TOKEN = "cached_fcm_token"

        /**
         * Creates the notification channel with user-chosen Apple tone.
         * Must be called before any notification is shown.
         */
        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            try {
                val prefs = context.getSharedPreferences("findit_prefs", Context.MODE_PRIVATE)
                val soundRes = prefs.getInt("selected_notification_sound", R.raw.apple_chime)
                val soundUri = Uri.parse("android.resource://${context.packageName}/$soundRes")
                val audioAttr = android.media.AudioAttributes.Builder()
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
                    .build()

                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Foundly Alerts & Messages",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Push alerts with Apple chime for messages and item matches"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 250, 150, 250)
                    enableLights(true)
                    lightColor = 0xFFC1121F.toInt()
                    lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                    setShowBadge(true)
                    setSound(soundUri, audioAttr)
                }

                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.createNotificationChannel(channel)
            } catch (e: Throwable) {
                Log.e(TAG, "Channel creation failed: ${e.message}")
            }
        }

        /**
         * Triggers an immediate notification — works even when app is in background.
         * Plays Apple chime via the notification channel.
         */
        fun triggerPushNotification(
            context: Context,
            title: String,
            body: String,
            itemId: String? = null
        ) {
            try {
                createNotificationChannel(context)

                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    itemId?.let { putExtra("extra_item_id", it) }
                }

                val pendingIntent = PendingIntent.getActivity(
                    context,
                    System.currentTimeMillis().toInt(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // Apple chime from channel (system handles sound delivery)
                val soundUri = Uri.parse("android.resource://${context.packageName}/${R.raw.apple_chime}")

                val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_nav_my_reports)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setSound(soundUri)
                    .setVibrate(longArrayOf(0, 200, 100, 200, 100, 250))
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .build()

                NotificationManagerCompat.from(context)
                    .notify(System.currentTimeMillis().toInt(), notification)

                try {
                    val pm = context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
                    if (pm != null && !pm.isInteractive) {
                        @Suppress("DEPRECATION")
                        val wl = pm.newWakeLock(
                            android.os.PowerManager.SCREEN_BRIGHT_WAKE_LOCK or android.os.PowerManager.ACQUIRE_CAUSES_WAKEUP,
                            "foundly:wake_screen_notification"
                        )
                        wl.acquire(3000)
                    }
                } catch (e: Throwable) {}
            } catch (e: Throwable) {
                Log.e(TAG, "triggerPushNotification failed: ${e.message}")
            }
        }

        /** Syncs the user's FCM token to Firebase after login. */
        fun syncUserToken(context: Context) {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val token = prefs.getString(KEY_FCM_TOKEN, null)
            val ref = FirebaseDatabase.getInstance(FirebaseManager.DATABASE_URL)
                .getReference("users").child(uid).child("fcmToken")

            if (token != null) {
                ref.setValue(token)
            } else {
                FirebaseMessaging.getInstance().token.addOnSuccessListener { t ->
                    prefs.edit().putString(KEY_FCM_TOKEN, t).apply()
                    ref.setValue(t)
                }
            }
        }
    }
}
