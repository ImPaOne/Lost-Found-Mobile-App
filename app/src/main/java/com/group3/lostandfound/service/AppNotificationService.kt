package com.group3.lostandfound.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.group3.lostandfound.ConversationMetadata
import com.group3.lostandfound.NotificationItem
import com.group3.lostandfound.FirebaseManager

class AppNotificationService : Service() {

    private var myUid: String = ""
    private var serviceStartTime: Long = 0L

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        serviceStartTime = System.currentTimeMillis()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val user = FirebaseAuth.getInstance().currentUser
        val prefs = getSharedPreferences("findit_prefs", Context.MODE_PRIVATE)
        val savedUid = prefs.getString("user_uid", "") ?: ""

        val activeUid = if (!user?.uid.isNullOrEmpty()) user!!.uid else savedUid

        if (activeUid.isNotEmpty() && activeUid != myUid) {
            myUid = activeUid
            startListening()
        }

        return START_STICKY
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
                    if (notif.timestamp > serviceStartTime && notif.senderUserId != myUid && notif.type != "CHAT_MESSAGE") {
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
            if (!isFromMe && conv.lastUpdatedTimestamp > serviceStartTime && conv.otherUserId.isNotEmpty()) {
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
