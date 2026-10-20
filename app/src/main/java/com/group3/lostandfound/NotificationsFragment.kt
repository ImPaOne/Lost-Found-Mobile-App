package com.group3.lostandfound

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
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

        val currentUid = FirebaseManager.currentUid
        val notifRef = com.google.firebase.database.FirebaseDatabase.getInstance(FirebaseManager.DATABASE_URL)
            .getReference("notifications")
            .child(currentUid)

        notifRef.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                if (snapshot.exists() && snapshot.childrenCount > 0) {
                    dotWallet.visibility = View.VISIBLE
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
            Toast.makeText(requireContext(), "All notifications marked as read", Toast.LENGTH_SHORT).show()
        }

        itemWallet.setOnClickListener {
            dotWallet.visibility = View.GONE
            parentFragmentManager.beginTransaction()
                .replace(
                    R.id.screenBox,
                    DetailsFragment.newInstance(
                        title = "Black leather wallet",
                        desc = "Slim wallet with national ID and two bank cards. A user sent you a match lead.",
                        status = "LOST",
                        location = "Colombo 03",
                        imageResId = R.drawable.img_wallet_details
                    )
                )
                .addToBackStack(null)
                .commit()
        }

        itemKasun.setOnClickListener {
            Toast.makeText(requireContext(), "Kasun: 'Hi, I think I found your wallet at Coffee Beans!'", Toast.LENGTH_LONG).show()
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
