package com.group3.lostandfound

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment

class NotificationsFragment : Fragment() {

    private lateinit var btnBack: ImageView
    private lateinit var btnCheckAll: ImageView

    private lateinit var itemWallet: LinearLayout
    private lateinit var itemKasun: LinearLayout
    private lateinit var itemPassport: LinearLayout
    private lateinit var itemAdmin: LinearLayout
    private lateinit var itemAirPods: LinearLayout
    private lateinit var itemGalleFace: LinearLayout

    private lateinit var dotWallet: View
    private lateinit var dotAdmin: View
    private lateinit var dotGalleFace: View

    private var currentNotifs: List<com.group3.lostandfound.NotificationItem> = emptyList()

    // Creates and displays the notifications screen layout.
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_notifications, container, false)
    }

    // Initializes notification views, sets up bottom navigation, and loads Firebase notifications.
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnBack = view.findViewById(R.id.btnBack)
        btnCheckAll = view.findViewById(R.id.btnCheckAll)

        itemWallet = view.findViewById(R.id.itemWallet)
        itemKasun = view.findViewById(R.id.itemKasun)
        itemPassport = view.findViewById(R.id.itemPassport)
        itemAdmin = view.findViewById(R.id.itemAdmin)
        itemAirPods = view.findViewById(R.id.itemAirPods)
        itemGalleFace = view.findViewById(R.id.itemGalleFace)

        dotWallet = view.findViewById(R.id.dotWallet)
        dotAdmin = view.findViewById(R.id.dotAdmin)
        dotGalleFace = view.findViewById(R.id.dotGalleFace)

        setupBottomNav(view, "")

        val tvNotifTitle1 = view.findViewById<TextView>(R.id.tvNotifTitle1)
        val tvNotifDesc1 = view.findViewById<TextView>(R.id.tvNotifDesc1)
        val tvNotifTitle2 = view.findViewById<TextView>(R.id.tvNotifTitle2)
        val tvNotifDesc2 = view.findViewById<TextView>(R.id.tvNotifDesc2)

        val currentUid = FirebaseManager.currentUid
        val notifRef = com.google.firebase.database.FirebaseDatabase.getInstance(FirebaseManager.DATABASE_URL)
            .getReference("notifications")
            .child(currentUid)

        notifRef.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
            // Handles notification data changes and updates the notification list in real time.
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                if (!isAdded) return
                val notifList = mutableListOf<com.group3.lostandfound.NotificationItem>()
                for (child in snapshot.children) {
                    val notif = try {
                        child.getValue(com.group3.lostandfound.NotificationItem::class.java)
                    } catch (e: Exception) {
                        null
                    }
                    if (notif != null) {
                        notifList.add(notif)
                    } else {
                        val title = child.child("title").getValue(String::class.java) ?: "Notification"
                        val msg = child.child("message").getValue(String::class.java) ?: ""
                        notifList.add(com.group3.lostandfound.NotificationItem(title = title, message = msg))
                    }
                }

                currentNotifs = notifList
                val hasNotifs = notifList.isNotEmpty()
                itemWallet.visibility = if (hasNotifs) View.VISIBLE else View.GONE
                itemKasun.visibility = if (notifList.size > 1) View.VISIBLE else View.GONE
                itemPassport.visibility = View.GONE
                itemAirPods.visibility = View.GONE
                itemGalleFace.visibility = View.GONE

                dotWallet.visibility = if (hasNotifs) View.VISIBLE else View.GONE
                dotGalleFace.visibility = View.GONE

                if (hasNotifs) {
                    tvNotifTitle1?.text = notifList[0].title.ifEmpty { "Notification Alert" }
                    tvNotifDesc1?.text = notifList[0].message.ifEmpty { "Active item notification." }
                }
                if (notifList.size > 1) {
                    tvNotifTitle2?.text = notifList[1].title.ifEmpty { "Community Message" }
                    tvNotifDesc2?.text = notifList[1].message.ifEmpty { "New community message received." }
                }

                val lblToday = view.findViewById<TextView>(R.id.lblToday)
                val lblEarlier = view.findViewById<TextView>(R.id.lblEarlier)
                lblToday?.text = if (hasNotifs) "TODAY" else "No new notifications. You're all caught up!"
                lblEarlier?.visibility = View.GONE
            }
            // Handles Firebase errors when notification data cannot be retrieved.
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
        })

        // Listen for Global Admin Announcements from /announcements
        var latestAnnouncementTitle = "Foundly Official Announcement"
        var latestAnnouncementMessage = ""
        val announcementsRef = com.google.firebase.database.FirebaseDatabase.getInstance(FirebaseManager.DATABASE_URL)
            .getReference("announcements")
            .limitToLast(5)

        announcementsRef.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                if (!isAdded) return
                var foundAnn = false
                for (child in snapshot.children.reversed()) {
                    val msg = child.child("message").getValue(String::class.java) ?: ""
                    val title = child.child("title").getValue(String::class.java) ?: "Foundly Official Announcement"
                    if (msg.isNotEmpty()) {
                        latestAnnouncementTitle = title
                        latestAnnouncementMessage = msg
                        val tvTitle = view.findViewById<TextView>(R.id.tvAdminNotifTitle)
                        val tvDesc = view.findViewById<TextView>(R.id.tvAdminNotifDesc)
                        tvTitle?.text = title
                        tvDesc?.text = msg
                        itemAdmin.visibility = View.VISIBLE
                        dotAdmin.visibility = View.VISIBLE
                        foundAnn = true
                        break
                    }
                }
                if (!foundAnn) {
                    itemAdmin.visibility = View.GONE
                    dotAdmin.visibility = View.GONE
                }
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
        })

        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnCheckAll.setOnClickListener {
            dotWallet.visibility = View.GONE
            dotAdmin.visibility = View.GONE
            dotGalleFace.visibility = View.GONE
            notifRef.removeValue()
            com.group3.lostandfound.util.ModernToast.show(requireContext(), "All notifications marked as read.", view)
        }

        itemWallet.setOnClickListener {
            dotWallet.visibility = View.GONE
            if (currentNotifs.isNotEmpty()) {
                handleNotificationClick(currentNotifs[0])
            } else {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.screenBox, ReportsFragment.newInstance())
                    .addToBackStack(null)
                    .commit()
            }
        }

        itemKasun.setOnClickListener {
            if (currentNotifs.size > 1) {
                handleNotificationClick(currentNotifs[1])
            } else {
                parentFragmentManager.beginTransaction()
                    .replace(
                        R.id.screenBox,
                        ChatFragment.newInstance(
                            recipientUserId = "support_admin",
                            recipientName = "Community Contact",
                            itemId = "",
                            itemTitle = "Inquiry"
                        )
                    )
                    .addToBackStack(null)
                    .commit()
            }
        }

        itemPassport.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(
                    R.id.screenBox,
                    DetailsFragment.newInstance(
                        title = "Sri Lankan passport",
                        desc = "Your reported passport has passed moderation review and is visible to the community.",
                        status = "FOUND",
                        location = "Fort Railway Station",
                        imageResId = R.drawable.ic_item_placeholder
                    )
                )
                .addToBackStack(null)
                .commit()
        }

        itemAdmin.setOnClickListener {
            dotAdmin.visibility = View.GONE
            if (latestAnnouncementMessage.isNotEmpty()) {
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle(latestAnnouncementTitle)
                    .setMessage(latestAnnouncementMessage)
                    .setPositiveButton("Understood", null)
                    .show()
            } else {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.screenBox, TrustFragment.newInstance())
                    .addToBackStack(null)
                    .commit()
            }
        }

        itemAirPods.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(
                    R.id.screenBox,
                    DetailsFragment.newInstance(
                        title = "AirPods Pro Case",
                        desc = "This case has been marked as resolved and reunited with owner.",
                        status = "RESOLVED",
                        location = "Bambalapitiya",
                        imageResId = R.drawable.ic_item_placeholder
                    )
                )
                .addToBackStack(null)
                .commit()
        }

        itemGalleFace.setOnClickListener {
            dotGalleFace.visibility = View.GONE
            parentFragmentManager.beginTransaction()
                .replace(R.id.screenBox, MapFragment.newInstance())
                .addToBackStack(null)
                .commit()
        }
    }

    // Opens the appropriate screen based on the selected notification type.
    private fun handleNotificationClick(notif: com.group3.lostandfound.NotificationItem) {
        if (notif.type == "CHAT_MESSAGE" || notif.conversationId.isNotEmpty()) {
            val targetUid = notif.senderUserId.ifEmpty { "support_admin" }
            val targetName = notif.senderName.ifEmpty { "Community Member" }
            val itemTitle = notif.title.removePrefix("💬 Message regarding ").trim('\'', '"', ' ')
            parentFragmentManager.beginTransaction()
                .replace(
                    R.id.screenBox,
                    ChatFragment.newInstance(
                        recipientUserId = targetUid,
                        recipientName = targetName,
                        itemId = notif.itemId,
                        itemTitle = itemTitle.ifEmpty { "Item Discussion" }
                    )
                )
                .addToBackStack(null)
                .commit()
        } else if (notif.itemId.isNotEmpty()) {
            parentFragmentManager.beginTransaction()
                .replace(
                    R.id.screenBox,
                    DetailsFragment.newInstance(
                        itemId = notif.itemId,
                        title = notif.title,
                        desc = notif.message,
                        status = "LOST",
                        location = "Sri Lanka",
                        reporterUserId = notif.senderUserId
                    )
                )
                .addToBackStack(null)
                .commit()
        } else {
            parentFragmentManager.beginTransaction()
                .replace(R.id.screenBox, ReportsFragment.newInstance())
                .addToBackStack(null)
                .commit()
        }
    }

    companion object {
        // Creates a new instance of NotificationsFragment.
        fun newInstance(): NotificationsFragment {
            return NotificationsFragment()
        }
    }
}
