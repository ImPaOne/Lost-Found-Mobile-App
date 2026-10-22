package com.group3.lostandfound.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
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
 * FCM Integration service handling incoming push messages and persisting refreshed tokens
 * under Firebase Realtime Database path `/users/{uid}/fcmToken`.
 */
class FCMService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed FCM token received: $token")
        saveTokenLocally(token)
        sendTokenToServer(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "FCM message received from: ${remoteMessage.from}")

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "Foundly Alert"

        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: remoteMessage.data["message"]
            ?: "New update on your reported lost or found item."

        val itemId = remoteMessage.data["itemId"]

        showNotification(title, body, itemId)
    }

    private fun saveTokenLocally(token: String) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_FCM_TOKEN, token).apply()
    }

    private fun sendTokenToServer(token: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            val userTokenRef = FirebaseDatabase.getInstance(FirebaseManager.DATABASE_URL)
                .getReference("users")
                .child(uid)
                .child("fcmToken")

            userTokenRef.setValue(token).addOnSuccessListener {
                Log.d(TAG, "Successfully synced FCM token to /users/$uid/fcmToken")
            }.addOnFailureListener { error ->
                Log.e(TAG, "Failed to sync FCM token to database: ${error.message}")
            }
        } else {
            Log.d(TAG, "No authenticated user yet; token cached locally for post-login sync.")
        }
    }

    private fun showNotification(title: String, body: String, itemId: String?) {
        createNotificationChannel(this)

        val soundUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (itemId != null) {
                putExtra("extra_item_id", itemId)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_nav_my_reports)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 300, 200, 300))
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), notification)
        } catch (e: Throwable) {
            Log.e(TAG, "Notification dispatch failed: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "FCMService"
        const val CHANNEL_ID = "foundly_alerts_channel"
        private const val PREFS_NAME = "lost_found_prefs"
        private const val KEY_FCM_TOKEN = "cached_fcm_token"

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    val name = "Foundly Urgent Alerts & Messages"
                    val descriptionText = "Push notifications with modern sounds for urgent items and incoming messages"
                    val importance = NotificationManager.IMPORTANCE_HIGH
                    val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                        description = descriptionText
                        enableVibration(true)
                        vibrationPattern = longArrayOf(0, 200, 100, 200, 100, 250)
                        enableLights(true)
                        lightColor = android.graphics.Color.rgb(255, 112, 67)
                        lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                        setShowBadge(true)
                        val soundUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
                        val audioAttributes = android.media.AudioAttributes.Builder()
                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
                            .build()
                        setSound(soundUri, audioAttributes)
                    }
                    val notificationManager: NotificationManager =
                        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.createNotificationChannel(channel)
                } catch (e: Throwable) {
                    Log.e(TAG, "Failed to create notification channel: ${e.message}")
                }
            }
        }

        /**
         * Triggers a push notification with sound and vibration immediately.
         */
        fun triggerPushNotification(context: Context, title: String, body: String, itemId: String? = null) {
            try {
                createNotificationChannel(context)
                val soundUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)

                // Audible sound feedback for modern alerts
                try {
                    val ringtone = android.media.RingtoneManager.getRingtone(context.applicationContext, soundUri)
                    ringtone?.play()
                } catch (_: Exception) {}

                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    if (itemId != null) {
                        putExtra("extra_item_id", itemId)
                    }
                }

                val pendingIntent = PendingIntent.getActivity(
                    context,
                    System.currentTimeMillis().toInt(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

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
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .build()

                NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), notification)
            } catch (e: Throwable) {
                Log.e(TAG, "triggerPushNotification failed gracefully: ${e.message}")
            }
        }

        /**
         * Call upon successful user login or app start to ensure current user's token is saved under /users/{uid}/fcmToken.
         */
        fun syncUserToken(context: Context) {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val cachedToken = prefs.getString(KEY_FCM_TOKEN, null)

            if (cachedToken != null) {
                FirebaseDatabase.getInstance(FirebaseManager.DATABASE_URL)
                    .getReference("users")
                    .child(uid)
                    .child("fcmToken")
                    .setValue(cachedToken)
            } else {
                FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                    prefs.edit().putString(KEY_FCM_TOKEN, token).apply()
                    FirebaseDatabase.getInstance(FirebaseManager.DATABASE_URL)
                        .getReference("users")
                        .child(uid)
                        .child("fcmToken")
                        .setValue(token)
                }
            }
        }
    }
}
