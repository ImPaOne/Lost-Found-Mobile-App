package com.group3.lostandfound

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.group3.lostandfound.repository.ExploreRepository
import com.group3.lostandfound.util.ImageLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * DetailsFragment displays full item report details with:
 * - Live real-time single-item observer from `/items/{itemId}` via addValueEventListener
 * - Slideable multiple item photos via ViewPager2 with Coroutine ImageLoader
 * - Dynamic status updates (LOST / FOUND / RESOLVED)
 * - Tap to enlarge photos in full-screen modal
 * - Title, description, and location coordinates
 * - Contact reporter and listing resolution
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
    private var activeImageUrl: String = ""

    private val exploreRepository = ExploreRepository()
    private var itemId: String? = null
    private var detailsJob: Job? = null

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
        val initialTitle = arguments?.getString(ARG_TITLE) ?: "Item Report"
        val initialDesc = arguments?.getString(ARG_DESC) ?: "No description provided."
        val initialStatus = arguments?.getString(ARG_STATUS) ?: "LOST"
        val initialLocation = arguments?.getString(ARG_LOCATION) ?: "Colombo 03"
        itemId = arguments?.getString(ARG_ITEM_ID)

        // Set initial bundle data
        tvItemTitle.text = initialTitle
        tvDescription.text = initialDesc
        updateStatusTag(initialStatus)
        tvLocationText.text = initialLocation

        // Setup Photo Carousel
        val fallbackPhotos = listOf("")
        itemPhotoList = fallbackPhotos
        pagerPhotos.adapter = PhotoSliderAdapter(itemPhotoList, viewLifecycleOwner.lifecycleScope) { url ->
            showEnlargedPhotoDialog(url, tvItemTitle.text.toString())
        }

        // Live status updates from /items/{itemId}
        if (!itemId.isNullOrEmpty()) {
            detailsJob?.cancel()
            detailsJob = viewLifecycleOwner.lifecycleScope.launch {
                exploreRepository.getItemDetails(itemId!!).collectLatest { report ->
                    if (report != null) {
                        tvItemTitle.text = report.title.ifEmpty { "Report Details" }
                        tvDescription.text = report.description.ifEmpty { "No description provided." }
                        updateStatusTag(report.status)

                        val loc = if (report.latitude != 0.0 || report.longitude != 0.0) {
                            "${report.latitude}, ${report.longitude}"
                        } else {
                            "Colombo 03"
                        }
                        tvLocationText.text = loc

                        if (report.imageUrl.isNotEmpty()) {
                            activeImageUrl = report.imageUrl
                            itemPhotoList = listOf(report.imageUrl)
                            pagerPhotos.adapter = PhotoSliderAdapter(itemPhotoList, viewLifecycleOwner.lifecycleScope) { url ->
                                showEnlargedPhotoDialog(url, report.title)
                            }
                        }
                    }
                }
            }
        }

        pagerPhotos.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateDots(position)
            }
        })
    }

    private fun updateStatusTag(status: String) {
        tagStatus.text = status.uppercase()
        when (status.uppercase()) {
            "LOST" -> {
                tagStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.tag_lost_color))
                tagStatus.setBackgroundResource(R.drawable.bg_tag_lost_outlined)
            }
            "FOUND" -> {
                tagStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.tag_found_color))
                tagStatus.setBackgroundResource(R.drawable.bg_tag_lost_outlined)
            }
            "RESOLVED" -> {
                tagStatus.setTextColor(Color.parseColor("#4A7C59"))
                tagStatus.setBackgroundResource(R.drawable.bg_tag_lost_outlined)
            }
            else -> {
                tagStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
                tagStatus.setBackgroundResource(R.drawable.bg_tag_lost_outlined)
            }
        }
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
            showEnlargedPhotoDialog(activeImageUrl, tvItemTitle.text.toString())
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

        // Live status resolution & management
        tagStatus.setOnClickListener {
            val currentItemId = itemId ?: return@setOnClickListener
            val itemTitle = tvItemTitle.text.toString()
            val options = arrayOf("Mark as RESOLVED", "Mark as LOST", "Mark as FOUND")
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Update Status: $itemTitle")
                .setItems(options) { _, which ->
                    val newStatus = when (which) {
                        0 -> "RESOLVED"
                        1 -> "LOST"
                        else -> "FOUND"
                    }
                    FirebaseManager.markItemResolved(
                        itemId = currentItemId,
                        onSuccess = {
                            Toast.makeText(requireContext(), "Status updated to $newStatus in Firebase!", Toast.LENGTH_SHORT).show()
                        },
                        onError = { error ->
                            Toast.makeText(requireContext(), "Failed to update: $error", Toast.LENGTH_SHORT).show()
                        }
                    )
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

    private fun showEnlargedPhotoDialog(imageUrl: String, title: String) {
        val dialog = Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.setContentView(R.layout.dialog_enlarged_photo)
        dialog.setCancelable(true)

        val imgEnlargedView = dialog.findViewById<ImageView>(R.id.imgEnlargedView)
        val btnCloseLargePhoto = dialog.findViewById<View>(R.id.btnCloseLargePhoto)
        val tvEnlargedTitle = dialog.findViewById<TextView>(R.id.tvEnlargedTitle)

        tvEnlargedTitle.text = title
        ImageLoader.load(imgEnlargedView, imageUrl, R.drawable.img_wallet_details, viewLifecycleOwner.lifecycleScope)

        btnCloseLargePhoto.setOnClickListener { dialog.dismiss() }
        imgEnlargedView.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    /**
     * ViewPager2 adapter for slideable photos with Coroutine ImageLoader.
     */
    private class PhotoSliderAdapter(
        private val photos: List<String>,
        private val scope: CoroutineScope,
        private val onPhotoClicked: (String) -> Unit
    ) : RecyclerView.Adapter<PhotoSliderAdapter.PhotoViewHolder>() {

        class PhotoViewHolder(val imageView: ImageView) : RecyclerView.ViewHolder(imageView) {
            var job: Job? = null
        }

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
            holder.job?.cancel()
            holder.job = ImageLoader.load(holder.imageView, url, R.drawable.img_wallet_details, scope)

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

        fun newInstance(
            title: String = "Item Report",
            desc: String = "No description provided.",
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
            }
            fragment.arguments = args
            return fragment
        }
    }
}
