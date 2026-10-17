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

        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnCheckAll.setOnClickListener {
            dotWallet.visibility = View.GONE
            dotAdmin.visibility = View.GONE
            dotGalleFace.visibility = View.GONE
            Toast.makeText(requireContext(), "All notifications marked as read", Toast.LENGTH_SHORT).show()
        }

        itemWallet.setOnClickListener {
            dotWallet.visibility = View.GONE
            parentFragmentManager.beginTransaction()
                .replace(R.id.screenBox, DetailsFragment.newInstance())
                .addToBackStack(null)
                .commit()
        }

        itemKasun.setOnClickListener {
            Toast.makeText(requireContext(), "Kasun sent a message about your report.", Toast.LENGTH_SHORT).show()
        }

        itemPassport.setOnClickListener {
            Toast.makeText(requireContext(), "Your passport report is under review.", Toast.LENGTH_SHORT).show()
        }

        itemAdmin.setOnClickListener {
            dotAdmin.visibility = View.GONE
            Toast.makeText(requireContext(), "Please verify your contact number.", Toast.LENGTH_SHORT).show()
        }

        itemAirPods.setOnClickListener {
            Toast.makeText(requireContext(), "AirPods Pro case marked as resolved.", Toast.LENGTH_SHORT).show()
        }

        itemGalleFace.setOnClickListener {
            dotGalleFace.visibility = View.GONE
            Toast.makeText(requireContext(), "New possible match near Galle Face.", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        fun newInstance(): NotificationsFragment {
            return NotificationsFragment()
        }
    }
}
