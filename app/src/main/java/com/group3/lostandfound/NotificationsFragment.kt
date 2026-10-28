package com.group3.lostandfound

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.group3.lostandfound.util.ModernToast

/**
 * Notifications screen matching the reference design:
 * Displays categorized notifications (Today, Yesterday, Earlier),
 * real-time Firebase sync, interactive notification cards, and consistent bottom navigation.
 */
class NotificationsFragment : Fragment() {

    private lateinit var btnBack: FrameLayout
    private lateinit var btnCheckAll: FrameLayout
    private lateinit var badgeNewToday: TextView

    private lateinit var itemWallet: LinearLayout
    private lateinit var itemKasun: LinearLayout
    private lateinit var itemPassport: LinearLayout
    private lateinit var itemAdmin: LinearLayout
    private lateinit var itemAirPods: LinearLayout
    private lateinit var itemGalleFace: LinearLayout

    private lateinit var dotWallet: View
    private lateinit var dotAdmin: View
    private lateinit var dotGalleFace: View

    private var liveNotifications: List<NotificationItem> = emptyList()
    private var adminAlertTitle = "Admin alert"
    private var adminAlertMessage = "Please verify your contact number."

    //	Loads the notification screen layout from fragment_notifications.xml.
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_notifications, container, false)
    }

    //Initializes the screen and sets up listeners and notification loading.
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupBottomNav(view, "")
        setupCardClicks()
        setupHeaderActions()
        loadLiveNotifications(view)
    }

    //Connects Kotlin variables to UI elements
    private fun initViews(view: View) {
        btnBack = view.findViewById(R.id.btnBack)
        btnCheckAll = view.findViewById(R.id.btnCheckAll)
        badgeNewToday = view.findViewById(R.id.badgeNewToday)

        itemWallet = view.findViewById(R.id.itemWallet)
        itemKasun = view.findViewById(R.id.itemKasun)
        itemPassport = view.findViewById(R.id.itemPassport)
        itemAdmin = view.findViewById(R.id.itemAdmin)
        itemAirPods = view.findViewById(R.id.itemAirPods)
        itemGalleFace = view.findViewById(R.id.itemGalleFace)

        dotWallet = view.findViewById(R.id.dotWallet)
        dotAdmin = view.findViewById(R.id.dotAdmin)
        dotGalleFace = view.findViewById(R.id.dotGalleFace)
    }

    private fun setupHeaderActions() {
        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnCheckAll.setOnClickListener {
            dotWallet.visibility = View.GONE
            dotAdmin.visibility = View.GONE
            dotGalleFace.visibility = View.GONE
            badgeNewToday.visibility = View.GONE

            val currentUid = FirebaseManager.currentUid
            if (currentUid.isNotEmpty()) {
                com.google.firebase.database.FirebaseDatabase.getInstance(FirebaseManager.DATABASE_URL)
                    .getReference("notifications")
                    .child(currentUid)
                    .removeValue()
            }
            ModernToast.show(requireContext(), "All notifications marked as read.", view)
        }
    }

    private fun setupCardClicks() {
        itemWallet.setOnClickListener {
            dotWallet.visibility = View.GONE
            if (liveNotifications.isNotEmpty()) {
                openNotificationTarget(liveNotifications[0])
            } else {
                parentFragmentManager.beginTransaction()
                    .replace(
                        R.id.screenBox,
                        DetailsFragment.newInstance(
                            title = "Brown Leather Wallet",
                            desc = "A brown leather wallet found in Kollupitiya with credit cards and national ID card.",
                            status = "FOUND",
                            location = "Kollupitiya",
                            imageResId = R.drawable.img_wallet
                        )
                    )
                    .addToBackStack(null)
                    .commit()
            }
        }

        itemKasun.setOnClickListener {
            if (liveNotifications.size > 1) {
                openNotificationTarget(liveNotifications[1])
            } else {
                parentFragmentManager.beginTransaction()
                    .replace(
                        R.id.screenBox,
                        ChatFragment.newInstance(
                            recipientUserId = "kasun_sender",
                            recipientName = "Kasun",
                            itemId = "wallet_report",
                            itemTitle = "Found Wallet"
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
                        title = "Sri Lankan Passport",
                        desc = "Your reported passport is under review by moderation team.",
                        status = "LOST",
                        location = "Fort Railway Station",
                        imageResId = R.drawable.img_passport
                    )
                )
                .addToBackStack(null)
                .commit()
        }

        itemAdmin.setOnClickListener {
            dotAdmin.visibility = View.GONE
            AlertDialog.Builder(requireContext())
                .setTitle(adminAlertTitle)
                .setMessage(adminAlertMessage)
                .setPositiveButton("Verify Now") { _, _ ->
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.screenBox, SettingsFragment.newInstance())
                        .addToBackStack(null)
                        .commit()
                }
                .setNegativeButton("Dismiss", null)
                .show()
        }

        itemAirPods.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(
                    R.id.screenBox,
                    DetailsFragment.newInstance(
                        title = "AirPods Pro Case",
                        desc = "AirPods Pro case marked as resolved and reunited with owner.",
                        status = "RESOLVED",
                        location = "Bambalapitiya",
                        imageResId = R.drawable.img_airpods
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

    private fun loadLiveNotifications(view: View) {
        val currentUid = FirebaseManager.currentUid
        if (currentUid.isEmpty()) return

        val notifRef = com.google.firebase.database.FirebaseDatabase.getInstance(FirebaseManager.DATABASE_URL)
            .getReference("notifications")
            .child(currentUid)

        notifRef.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                if (!isAdded) return
                val list = mutableListOf<NotificationItem>()
                for (child in snapshot.children) {
                    val notif = try {
                        child.getValue(NotificationItem::class.java)
                    } catch (e: Exception) {
                        null
                    }
                    if (notif != null) {
                        list.add(notif)
                    }
                }

                liveNotifications = list
                if (list.isNotEmpty()) {
                    view.findViewById<TextView>(R.id.tvNotifTitle1)?.text = list[0].title.ifEmpty { "Match found" }
                    view.findViewById<TextView>(R.id.tvNotifDesc1)?.text = list[0].message.ifEmpty { "A wallet found in Kollupitiya is a 92% match." }
                    dotWallet.visibility = View.VISIBLE
                    badgeNewToday.visibility = View.VISIBLE
                    badgeNewToday.text = "${list.size} new"
                }

                if (list.size > 1) {
                    view.findViewById<TextView>(R.id.tvNotifTitle2)?.text = list[1].title.ifEmpty { "Message received" }
                    view.findViewById<TextView>(R.id.tvNotifDesc2)?.text = list[1].message.ifEmpty { "Kasun sent a message about your report." }
                }
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
        })

        // Check for latest admin announcement
        val annRef = com.google.firebase.database.FirebaseDatabase.getInstance(FirebaseManager.DATABASE_URL)
            .getReference("announcements")
            .limitToLast(1)

        annRef.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                if (!isAdded) return
                for (child in snapshot.children) {
                    val msg = child.child("message").getValue(String::class.java) ?: ""
                    val title = child.child("title").getValue(String::class.java) ?: "Admin alert"
                    if (msg.isNotEmpty()) {
                        adminAlertTitle = title
                        adminAlertMessage = msg
                        view.findViewById<TextView>(R.id.tvAdminNotifTitle)?.text = title
                        view.findViewById<TextView>(R.id.tvAdminNotifDesc)?.text = msg
                    }
                }
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
        })
    }

    private fun openNotificationTarget(notif: NotificationItem) {
        if (notif.type == "CHAT_MESSAGE" || notif.conversationId.isNotEmpty()) {
            parentFragmentManager.beginTransaction()
                .replace(
                    R.id.screenBox,
                    ChatFragment.newInstance(
                        recipientUserId = notif.senderUserId.ifEmpty { "kasun_sender" },
                        recipientName = notif.senderName.ifEmpty { "Kasun" },
                        itemId = notif.itemId,
                        itemTitle = notif.title.removePrefix("💬 Message regarding ").trim('\'', '"', ' ').ifEmpty { "Report Discussion" }
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
        fun newInstance(): NotificationsFragment {
            return NotificationsFragment()
        }
    }
}
