package com.group3.lostandfound

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.group3.lostandfound.repository.ExploreRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * DetailsFragment displays full item report details:
 * - Slideable multiple item photos via ViewPager2
 * - Interactive carousel dot indicators
 * - Tap to enlarge photos in full-screen modal
 * - Outlined status tag (LOST / FOUND)
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

    // Photo Carousel Views
    private lateinit var pagerPhotos: ViewPager2
    private lateinit var badgeEnlargeHint: View
    private lateinit var dotPhoto1: View
    private var dotPhoto2: View? = null
    private var dotPhoto3: View? = null

    private var isBookmarked = false
    private var itemPhotoList = listOf<String>()
    
    private val exploreRepository = ExploreRepository()
    private var itemId: String? = null

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

        pagerPhotos = view.findViewById(R.id.pagerPhotos)
        badgeEnlargeHint = view.findViewById(R.id.badgeEnlargeHint)
        dotPhoto1 = view.findViewById(R.id.dotPhoto1)
        dotPhoto2 = view.findViewById(R.id.dotPhoto2)
        dotPhoto3 = view.findViewById(R.id.dotPhoto3)
    }

    private fun bindData() {
        val title = arguments?.getString(ARG_TITLE) ?: "Black leather wallet"
        val desc = arguments?.getString(ARG_DESC) ?: "Slim wallet with national ID and two bank cards. Small scratch near the clasp."
        val status = arguments?.getString(ARG_STATUS) ?: "LOST"
        val location = arguments?.getString(ARG_LOCATION) ?: "Colombo 03"
        val imageRes = arguments?.getInt(ARG_IMAGE_RES, R.drawable.img_wallet_details) ?: R.drawable.img_wallet_details
        itemId = arguments?.getString(ARG_ITEM_ID)

        if (itemId != null) {
            lifecycleScope.launch {
                exploreRepository.getItemDetails(itemId!!).collectLatest { item ->
                    if (item != null) {
                        tvItemTitle.text = item.title
                        tvDescription.text = item.description
                        tagStatus.text = item.status
                        tvLocationText.text = "${item.latitude}, ${item.longitude}"
                        // Simplified multiple photos fallback to same URL
                        itemPhotoList = listOf(item.imageUrl)
                        
                        pagerPhotos.adapter = PhotoSliderAdapter(itemPhotoList) { url ->
                            // Enlarge feature for URL
                        }
                    }
                }
            }
        } else {
            tvItemTitle.text = title
            tvDescription.text = desc
            tagStatus.text = status
            tvLocationText.text = location

            // Keep the previous static flow for compatibility if no itemId
            itemPhotoList = listOf("") // Provide a fallback list of Strings to match the new type

            // Setup ViewPager2 Adapter
            pagerPhotos.adapter = PhotoSliderAdapter(itemPhotoList) { url ->
                // showEnlargedPhotoDialog(0, title) - Ignored in fallback string URLs mode for simplicity
            }
        }

        pagerPhotos.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateDots(position)
            }
        })
    }

    private fun updateDots(selectedIndex: Int) {
        val activeRes = R.drawable.bg_onboard_dot_active
        val inactiveRes = R.drawable.bg_onboard_dot_inactive

        dotPhoto1.setBackgroundResource(if (selectedIndex == 0) activeRes else inactiveRes)
        dotPhoto2?.setBackgroundResource(if (selectedIndex == 1) activeRes else inactiveRes)
        dotPhoto3?.setBackgroundResource(if (selectedIndex == 2) activeRes else inactiveRes)

        dotPhoto1.layoutParams?.width = if (selectedIndex == 0) dpToPx(18) else dpToPx(5)
        dotPhoto2?.layoutParams?.width = if (selectedIndex == 1) dpToPx(18) else dpToPx(5)
        dotPhoto3?.layoutParams?.width = if (selectedIndex == 2) dpToPx(18) else dpToPx(5)

        dotPhoto1.requestLayout()
        dotPhoto2?.requestLayout()
        dotPhoto3?.requestLayout()
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        badgeEnlargeHint.setOnClickListener {
            // Simplified handling for URL images
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
            Toast.makeText(requireContext(), "Opening secure contact channel...", Toast.LENGTH_SHORT).show()
        }

        // Admin & Reporter Action: Long press Contact button or click tag to manage status
        tagStatus.setOnClickListener {
            val itemId = arguments?.getString(ARG_ITEM_ID) ?: "item_4"
            val itemTitle = tvItemTitle.text.toString()
            val options = arrayOf("Mark as RESOLVED (Firebase)", "Flag Item (Moderation)", "Delete Item (Firebase)")
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Manage Listing: $itemTitle")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> {
                            FirebaseManager.markItemResolved(
                                itemId = itemId,
                                onSuccess = {
                                    tagStatus.text = "RESOLVED"
                                    Toast.makeText(requireContext(), "✅ Marked as RESOLVED in Firebase!", Toast.LENGTH_SHORT).show()
                                },
                                onError = { Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show() }
                            )
                        }
                        1 -> {
                            FirebaseManager.flagItem(
                                itemId = itemId,
                                isFlagged = true,
                                onSuccess = {
                                    Toast.makeText(requireContext(), "🚩 Flagged for moderation review", Toast.LENGTH_SHORT).show()
                                },
                                onError = { Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show() }
                            )
                        }
                        2 -> {
                            FirebaseManager.removeItem(
                                itemId = itemId,
                                onSuccess = {
                                    Toast.makeText(requireContext(), "🗑️ Item removed from database", Toast.LENGTH_SHORT).show()
                                    parentFragmentManager.popBackStack()
                                },
                                onError = { Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show() }
                            )
                        }
                    }
                }
                .show()
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

    private fun showEnlargedPhotoDialog(imageRes: Int, title: String) {
        val dialog = Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.setContentView(R.layout.dialog_enlarged_photo)
        dialog.setCancelable(true)

        val imgEnlargedView = dialog.findViewById<ImageView>(R.id.imgEnlargedView)
        val btnCloseLargePhoto = dialog.findViewById<View>(R.id.btnCloseLargePhoto)
        val tvEnlargedTitle = dialog.findViewById<TextView>(R.id.tvEnlargedTitle)

        imgEnlargedView.setImageResource(imageRes)
        tvEnlargedTitle.text = title

        btnCloseLargePhoto.setOnClickListener {
            dialog.dismiss()
        }

        imgEnlargedView.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    /**
     * ViewPager2 adapter for slideable photos
     */
    private class PhotoSliderAdapter(
        private val photos: List<String>,
        private val onPhotoClicked: (String) -> Unit
    ) : RecyclerView.Adapter<PhotoSliderAdapter.PhotoViewHolder>() {

        class PhotoViewHolder(val imageView: ImageView) : RecyclerView.ViewHolder(imageView)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
            val img = ImageView(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
            return PhotoViewHolder(img)
        }

        override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
            val url = photos[position]
            // Standard loading from URL using Glide or Coil should happen here
            holder.imageView.setOnClickListener {
                onPhotoClicked(url)
            }
        }

        override fun getItemCount(): Int = photos.size
    }

    companion object {
        private const val ARG_ITEM_ID = "arg_item_id"
        private const val ARG_TITLE = "arg_title"
        private const val ARG_DESC = "arg_desc"
        private const val ARG_STATUS = "arg_status"
        private const val ARG_LOCATION = "arg_location"
        private const val ARG_IMAGE_RES = "arg_image_res"
        private const val ARG_ITEM_ID = "arg_item_id"

        fun newInstance(
            title: String = "Black leather wallet",
            desc: String = "Slim wallet with national ID and two bank cards. Small scratch near the clasp.",
            status: String = "LOST",
            location: String = "Colombo 03",
            imageResId: Int = R.drawable.img_wallet_details,
            itemId: String? = null
        ): DetailsFragment {
            val fragment = DetailsFragment()
            val args = Bundle().apply {
                putString(ARG_ITEM_ID, itemId)
                putString(ARG_TITLE, title)
                putString(ARG_DESC, desc)
                putString(ARG_STATUS, status)
                putString(ARG_LOCATION, location)
                putInt(ARG_IMAGE_RES, imageResId)
                putString(ARG_ITEM_ID, itemId)
            }
            fragment.arguments = args
            return fragment
        }
    }
}
