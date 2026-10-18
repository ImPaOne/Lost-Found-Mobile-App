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
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2

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
    private lateinit var dotPhoto2: View
    private lateinit var dotPhoto3: View

    private var isBookmarked = false
    private var itemPhotoList = listOf<Int>()

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

        tvItemTitle.text = title
        tvDescription.text = desc
        tagStatus.text = status
        tvLocationText.text = location

        // Build list of multiple slideable photos for this item
        itemPhotoList = when {
            title.contains("wallet", ignoreCase = true) -> listOf(
                imageRes,
                R.drawable.img_wallet,
                R.drawable.img_passport
            )
            title.contains("dog", ignoreCase = true) || title.contains("Milo", ignoreCase = true) -> listOf(
                R.drawable.img_dog_milo,
                R.drawable.img_tabby_cat,
                imageRes
            )
            title.contains("phone", ignoreCase = true) || title.contains("samsung", ignoreCase = true) -> listOf(
                R.drawable.img_samsung_phone,
                R.drawable.img_airpods_bench,
                imageRes
            )
            title.contains("airpods", ignoreCase = true) -> listOf(
                R.drawable.img_airpods_bench,
                R.drawable.img_wallet_details,
                imageRes
            )
            else -> listOf(
                imageRes,
                R.drawable.img_wallet,
                R.drawable.img_wallet_details
            )
        }

        // Setup ViewPager2 Adapter
        pagerPhotos.adapter = PhotoSliderAdapter(itemPhotoList) { clickedImageRes ->
            showEnlargedPhotoDialog(clickedImageRes, title)
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
        dotPhoto2.setBackgroundResource(if (selectedIndex == 1) activeRes else inactiveRes)
        dotPhoto3.setBackgroundResource(if (selectedIndex == 2) activeRes else inactiveRes)

        dotPhoto1.layoutParams.width = if (selectedIndex == 0) dpToPx(18) else dpToPx(5)
        dotPhoto2.layoutParams.width = if (selectedIndex == 1) dpToPx(18) else dpToPx(5)
        dotPhoto3.layoutParams.width = if (selectedIndex == 2) dpToPx(18) else dpToPx(5)

        dotPhoto1.requestLayout()
        dotPhoto2.requestLayout()
        dotPhoto3.requestLayout()
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        badgeEnlargeHint.setOnClickListener {
            val currentIdx = pagerPhotos.currentItem
            val currentImg = if (currentIdx in itemPhotoList.indices) itemPhotoList[currentIdx] else itemPhotoList.firstOrNull() ?: R.drawable.img_wallet_details
            showEnlargedPhotoDialog(currentImg, tvItemTitle.text.toString())
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
        private val photos: List<Int>,
        private val onPhotoClicked: (Int) -> Unit
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
            val resId = photos[position]
            holder.imageView.setImageResource(resId)
            holder.imageView.setOnClickListener {
                onPhotoClicked(resId)
            }
        }

        override fun getItemCount(): Int = photos.size
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
