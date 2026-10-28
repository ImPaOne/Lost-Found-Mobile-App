package com.group3.lostandfound.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.group3.lostandfound.ConversationMetadata
import com.group3.lostandfound.MainActivity
import com.group3.lostandfound.NotificationItem
import com.group3.lostandfound.FirebaseManager
import com.group3.lostandfound.R

class AppNotificationService : Service() {

    private var myUid: String = ""
    private var serviceStartTime: Long = 0L
    private val notifiedAlertIds = mutableSetOf<String>()
    private var isAnnouncementListenerStarted = false

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        serviceStartTime = System.currentTimeMillis() - 15000
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startAsForegroundService()

        val user = FirebaseAuth.getInstance().currentUser
        val prefs = getSharedPreferences("findit_prefs", Context.MODE_PRIVATE)
        val savedUid = prefs.getString("user_uid", "") ?: ""
        val intentUid = intent?.getStringExtra("user_uid") ?: ""

        val activeUid = when {
            intentUid.isNotEmpty() -> intentUid
            !user?.uid.isNullOrEmpty() -> user!!.uid
            else -> savedUid
        }

        // Global announcements are listened to regardless of user UID
        startAnnouncementListening()

        if (activeUid.isNotEmpty() && activeUid != myUid) {
            myUid = activeUid
            startListening()
        }

        return START_STICKY
    }

    private fun startAsForegroundService() {
        val channelId = "foundly_bg_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Foundly Background Sync",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Maintains connection for instant notification delivery"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Foundly Alert Service")
            .setContentText("Listening for live messages and matched reports")
            .setSmallIcon(R.drawable.logoo)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        startForeground(2002, notification)
    }

    private fun startAnnouncementListening() {
        if (isAnnouncementListenerStarted) return
        isAnnouncementListenerStarted = true

        val prefs = getSharedPreferences("findit_prefs", Context.MODE_PRIVATE)
        val lastSeenTime = prefs.getLong("last_seen_announcement_time", 0L)
        val thresholdTime = if (lastSeenTime > 0L) lastSeenTime else serviceStartTime

        val database = FirebaseDatabase.getInstance(FirebaseManager.DATABASE_URL)
        val annRef = database.getReference("announcements")
        annRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                try {
                    val timestamp = snapshot.child("timestamp").getValue(Long::class.java) ?: 0L
                    val annKey = snapshot.key ?: "ann_$timestamp"
                    val notifiedSet = prefs.getStringSet("notified_ann_ids", emptySet()) ?: emptySet()
                    if (!notifiedSet.contains(annKey) && timestamp > thresholdTime) {
                        val newSet = notifiedSet.toMutableSet()
                        newSet.add(annKey)
                        prefs.edit()
                            .putStringSet("notified_ann_ids", newSet)
                            .putLong("last_seen_announcement_time", timestamp)
                            .apply()
                        notifiedAlertIds.add(annKey)

                        val title = snapshot.child("title").getValue(String::class.java) ?: "Foundly Official Announcement"
                        val message = snapshot.child("message").getValue(String::class.java) ?: ""
                        val enabled = prefs.getBoolean("push_notifications_enabled", true)
                        if (enabled && message.isNotEmpty()) {
                            FCMService.triggerPushNotification(
                                applicationContext,
                                "📢 $title",
                                message
                            )
                        }
                    }
                } catch (_: Exception) {}
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        try {
            val restartIntent = Intent(applicationContext, AppNotificationService::class.java).apply {
                putExtra("user_uid", myUid)
            }
            val pendingIntent = PendingIntent.getService(
                applicationContext,
                101,
                restartIntent,
                PendingIntent.FLAG_ONE_SHOT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager
            alarmManager?.set(
                android.app.AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + 1500,
                pendingIntent
            )
        } catch (_: Exception) {}
        super.onTaskRemoved(rootIntent)
    }

    private fun startListening() {
        if (myUid.isEmpty()) return

        val database = FirebaseDatabase.getInstance(FirebaseManager.DATABASE_URL)

        val chatRef = database.getReference("user_conversations").child(myUid)
        chatRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                handleChatMessage(snapshot)
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                handleChatMessage(snapshot)
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })

        val notifRef = database.getReference("notifications").child(myUid)
        notifRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                try {
                    val notif = snapshot.getValue(NotificationItem::class.java) ?: return
                    val notifKey = notif.id.ifEmpty { "notif_${notif.timestamp}_${notif.title}" }
                    if (!notifiedAlertIds.contains(notifKey) && notif.timestamp > serviceStartTime && notif.senderUserId != myUid) {
                        notifiedAlertIds.add(notifKey)
                        val prefs = getSharedPreferences("findit_prefs", Context.MODE_PRIVATE)
                        val enabled = prefs.getBoolean("push_notifications_enabled", true)
                        if (enabled) {
                            FCMService.triggerPushNotification(
                                applicationContext,
                                notif.title,
                                notif.message,
                                notif.itemId
                            )
                        }
                    }
                } catch (e: Exception) {
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun handleChatMessage(snapshot: DataSnapshot) {
        try {
            val conv = snapshot.getValue(ConversationMetadata::class.java) ?: return
            val isFromMe = conv.lastSenderId == myUid
            val convKey = "conv_${conv.chatConversationId}_${conv.lastUpdatedTimestamp}"
            if (!isFromMe && !notifiedAlertIds.contains(convKey) && conv.lastUpdatedTimestamp > serviceStartTime && conv.otherUserId.isNotEmpty()) {
                notifiedAlertIds.add(convKey)
                val prefs = getSharedPreferences("findit_prefs", Context.MODE_PRIVATE)
                val enabled = prefs.getBoolean("push_notifications_enabled", true)
                if (enabled) {
                    val senderName = if (conv.otherUserName.isNotEmpty()) conv.otherUserName else "Foundly User"
                    val title = if (conv.relatedItemTitle.isNotEmpty()) {
                        "$senderName: '${conv.relatedItemTitle}'"
                    } else {
                        "Message from $senderName"
                    }
                    val body = if (conv.lastMessageTextContent.isNotEmpty()) conv.lastMessageTextContent else "New message"

                    FCMService.triggerPushNotification(
                        applicationContext,
                        title,
                        body,
                        conv.relatedItemId
                    )
                }
            }
        } catch (e: Exception) {
        }
    }
}
