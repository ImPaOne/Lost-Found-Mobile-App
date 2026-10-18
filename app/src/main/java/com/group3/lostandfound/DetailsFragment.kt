package com.group3.lostandfound

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment

/**
 * DetailsFragment displays full item report details:
 * - Photo with carousel indicator
 * - Outlined status tag (LOST)
 * - Item title, description, and location metadata
 * - Address banner and reporter verification info
 * - "Contact Reporter" action
 * - Similar items recommendations
 * - Persistent bottom navigation
 */
class DetailsFragment : Fragment() {

    private lateinit var btnBack: ImageView
    private lateinit var btnBookmark: ImageView
    private lateinit var btnContact: TextView
    private lateinit var bannerLocation: View
    private lateinit var cardSimilarItem: View

    private lateinit var tvItemTitle: TextView
    private lateinit var tvDescription: TextView
    private lateinit var tagStatus: TextView
    private lateinit var tvLocationText: TextView
    private lateinit var imgWallet: ImageView

    private var isBookmarked = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        bindData()
        setupListeners()
        NavigationHelper.setup(this, view, NavigationHelper.Tab.HOME)
    }

    private fun initViews(view: View) {
        btnBack = view.findViewById(R.id.btnBack)
        btnBookmark = view.findViewById(R.id.btnBookmark)
        btnContact = view.findViewById(R.id.btnContact)
        bannerLocation = view.findViewById(R.id.bannerLocation)
        cardSimilarItem = view.findViewById(R.id.cardSimilarItem)

        tvItemTitle = view.findViewById(R.id.tvItemTitle)
        tvDescription = view.findViewById(R.id.tvDescription)
        tagStatus = view.findViewById(R.id.tagStatus)
        tvLocationText = view.findViewById(R.id.tvLocationText)
        imgWallet = view.findViewById(R.id.imgWallet)
    }

    private fun bindData() {
        val title = arguments?.getString(ARG_TITLE) ?: "Black leather wallet"
        val desc = arguments?.getString(ARG_DESC) ?: "Slim wallet with national ID and two bank cards. Small scratch near the clasp."
        val status = arguments?.getString(ARG_STATUS) ?: "LOST"
        val location = arguments?.getString(ARG_LOCATION) ?: "Colombo 03"
        val imageRes = arguments?.getInt(ARG_IMAGE_RES, R.drawable.img_wallet_details) ?: R.drawable.img_wallet_details

        tvItemTitle.text = title
        tvDescription.text = desc
        tagStatus.text = status
        tvLocationText.text = location
        try {
            imgWallet.setImageResource(imageRes)
        } catch (_: Exception) {
            imgWallet.setImageResource(R.drawable.img_wallet_details)
        }
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnBookmark.setOnClickListener {
            isBookmarked = !isBookmarked
            if (isBookmarked) {
                btnBookmark.setImageResource(R.drawable.ic_nav_my_reports)
                Toast.makeText(requireContext(), "Item saved to bookmarks", Toast.LENGTH_SHORT).show()
            } else {
                btnBookmark.setImageResource(R.drawable.ic_bookmark_outline)
                Toast.makeText(requireContext(), "Removed from bookmarks", Toast.LENGTH_SHORT).show()
            }
        }

        btnContact.setOnClickListener {
            Toast.makeText(requireContext(), "Opening contact options for reporter...", Toast.LENGTH_SHORT).show()
        }

        bannerLocation.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.screenBox, MapFragment.newInstance())
                .addToBackStack(null)
                .commit()
        }

        cardSimilarItem.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(
                    R.id.screenBox,
                    newInstance(
                        title = "Brown card holder",
                        desc = "Leather card holder found near cafe counter.",
                        status = "FOUND",
                        location = "Colombo 03",
                        imageResId = R.drawable.img_wallet
                    )
                )
                .addToBackStack(null)
                .commit()
        }
    }

    companion object {
        private const val ARG_TITLE = "arg_title"
        private const val ARG_DESC = "arg_desc"
        private const val ARG_STATUS = "arg_status"
        private const val ARG_LOCATION = "arg_location"
        private const val ARG_IMAGE_RES = "arg_image_res"

        fun newInstance(
            title: String = "Black leather wallet",
            desc: String = "Slim wallet with national ID and two bank cards. Small scratch near the clasp.",
            status: String = "LOST",
            location: String = "Colombo 03",
            imageResId: Int = R.drawable.img_wallet_details
        ): DetailsFragment {
            val fragment = DetailsFragment()
            val args = Bundle().apply {
                putString(ARG_TITLE, title)
                putString(ARG_DESC, desc)
                putString(ARG_STATUS, status)
                putString(ARG_LOCATION, location)
                putInt(ARG_IMAGE_RES, imageResId)
            }
            fragment.arguments = args
            return fragment
        }
    }
}
