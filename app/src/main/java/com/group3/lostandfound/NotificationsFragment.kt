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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_notifications, container, false)
    }

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

        setupBottomNav(view, "messages")

        val tvNotifTitle1 = view.findViewById<TextView>(R.id.tvNotifTitle1)
        val tvNotifDesc1 = view.findViewById<TextView>(R.id.tvNotifDesc1)
        val tvNotifTitle2 = view.findViewById<TextView>(R.id.tvNotifTitle2)
        val tvNotifDesc2 = view.findViewById<TextView>(R.id.tvNotifDesc2)

        val currentUid = FirebaseManager.currentUid
        val notifRef = com.google.firebase.database.FirebaseDatabase.getInstance(FirebaseManager.DATABASE_URL)
            .getReference("notifications")
            .child(currentUid)

        notifRef.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
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

                val hasNotifs = notifList.isNotEmpty()
                itemWallet.visibility = if (hasNotifs) View.VISIBLE else View.GONE
                itemKasun.visibility = if (notifList.size > 1) View.VISIBLE else View.GONE
                itemPassport.visibility = View.GONE
                itemAdmin.visibility = View.GONE
                itemAirPods.visibility = View.GONE
                itemGalleFace.visibility = View.GONE

                dotWallet.visibility = if (hasNotifs) View.VISIBLE else View.GONE
                dotAdmin.visibility = View.GONE
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
            parentFragmentManager.beginTransaction()
                .replace(R.id.screenBox, ReportsFragment.newInstance())
                .addToBackStack(null)
                .commit()
        }

        itemKasun.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(
                    R.id.screenBox,
                    ChatFragment.newInstance(
                        recipientUserId = "default_user_101",
                        recipientName = "Community Contact",
                        itemId = "",
                        itemTitle = "Inquiry"
                    )
                )
                .addToBackStack(null)
                .commit()
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
                        imageResId = R.drawable.img_passport
                    )
                )
                .addToBackStack(null)
                .commit()
        }

        itemAdmin.setOnClickListener {
            dotAdmin.visibility = View.GONE
            parentFragmentManager.beginTransaction()
                .replace(R.id.screenBox, TrustFragment.newInstance())
                .addToBackStack(null)
                .commit()
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
                        imageResId = R.drawable.img_wallet_details
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

    companion object {
        fun newInstance(): NotificationsFragment {
            return NotificationsFragment()
        }
    }
}
