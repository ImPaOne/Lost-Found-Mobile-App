package com.group3.lostandfound

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.card.MaterialCardView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.group3.lostandfound.service.FCMService

/**
 * HomeFragment displays the main dashboard of the Foundly app:
 * - Realtime Live Data from Firebase /items (ZERO DUMMY DATA)
 * - Dynamic Urgent Item Highlights Carousel Slider from Firebase /items
 * - Quick action cards: "Report Lost" & "Report Found"
 * - Live Community statistics
 * - Category filter chips ("All", "Electronics", "Documents")
 * - Dynamic recent items from Firebase
 * - Push notifications with sound triggers
 */
class HomeFragment : Fragment() {

    // Header views
    private lateinit var buttonHeaderClose: ImageView
    private lateinit var buttonNotifications: FrameLayout

    // Search views
    private lateinit var inputSearchField: EditText
    private lateinit var buttonFilterSettings: ImageView

    // Urgent Highlights Slider
    private lateinit var urgentSliderContainer: LinearLayout
    private lateinit var vpUrgentAlertSlider: ViewPager2
    private lateinit var dotSlider1: View
    private lateinit var dotSlider2: View
    private lateinit var dotSlider3: View
    private val sliderHandler = Handler(Looper.getMainLooper())
    private var sliderRunnable: Runnable? = null

    // Action cards
    private lateinit var cardReportLostItem: LinearLayout
    private lateinit var cardReportFoundItem: LinearLayout

    // Category chips
    private lateinit var chipCategoryAll: TextView
    private lateinit var chipCategoryElectronics: TextView
    private lateinit var chipCategoryDocuments: TextView

    // Recent items section
    private lateinit var buttonSeeAllRecent: TextView
    private lateinit var cardWalletItem: MaterialCardView
    private lateinit var cardDogItem: MaterialCardView
    private var cardLaptopHome: View? = null

    // Real reports from Firebase
    private val liveReportsList = mutableListOf<ReportItem>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupClickListeners()
        observeLiveFirebaseItems()
        NavigationHelper.setup(this, view, NavigationHelper.Tab.HOME)
    }

    private fun initViews(view: View) {
        buttonHeaderClose = view.findViewById(R.id.buttonHeaderClose)
        buttonNotifications = view.findViewById(R.id.buttonNotifications)

        inputSearchField = view.findViewById(R.id.inputSearchField)
        buttonFilterSettings = view.findViewById(R.id.buttonFilterSettings)

        urgentSliderContainer = view.findViewById(R.id.urgentSliderContainer)
        vpUrgentAlertSlider = view.findViewById(R.id.vpUrgentAlertSlider)
        dotSlider1 = view.findViewById(R.id.dotSlider1)
        dotSlider2 = view.findViewById(R.id.dotSlider2)
        dotSlider3 = view.findViewById(R.id.dotSlider3)

        cardReportLostItem = view.findViewById(R.id.cardReportLostItem)
        cardReportFoundItem = view.findViewById(R.id.cardReportFoundItem)

        chipCategoryAll = view.findViewById(R.id.chipCategoryAll)
        chipCategoryElectronics = view.findViewById(R.id.chipCategoryElectronics)
        chipCategoryDocuments = view.findViewById(R.id.chipCategoryDocuments)

        buttonSeeAllRecent = view.findViewById(R.id.buttonSeeAllRecent)
        cardWalletItem = view.findViewById(R.id.cardWalletItem)
        cardDogItem = view.findViewById(R.id.cardDogItem)
        cardLaptopHome = view.findViewById(R.id.cardLaptopHome)
    }

    private var itemsListener: ValueEventListener? = null
    private var pageChangeCallback: ViewPager2.OnPageChangeCallback? = null

    /**
     * Reads real-time data from /items node in Firebase.
     * ZERO dummy data: only actual user created posts are bound and shown.
     */
    private fun observeLiveFirebaseItems() {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded || view == null) return

                liveReportsList.clear()
                for (child in snapshot.children) {
                    val report = ReportItem.fromSnapshot(child)
                    if (report.itemId.isNotEmpty() || report.title.isNotEmpty()) {
                        liveReportsList.add(report)
                    }
                }

                updateUrgentSliderFromLive(liveReportsList)
                updateRecentCardsFromLive(liveReportsList)

                view?.findViewById<TextView>(R.id.textReunitedStatistics)?.let { tvStat ->
                    val countDisplay = liveReportsList.size
                    tvStat.text = "$countDisplay community reports active"
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        itemsListener = listener
        FirebaseDatabase.getInstance(FirebaseManager.DATABASE_URL)
            .getReference("items")
            .addValueEventListener(listener)
    }

    private fun updateUrgentSliderFromLive(reports: List<ReportItem>) {
        if (!isAdded || view == null) return

        val urgentItems = reports.filter {
            it.status.equals("LOST", ignoreCase = true) || it.isFlagged
        }.map { report ->
            UrgentItemData(
                id = report.itemId,
                badge = if (report.status.equals("LOST", ignoreCase = true)) "URGENT LOST" else "FOUND REPORT",
                isUrgent = report.status.equals("LOST", ignoreCase = true),
                rewardText = if (report.status.equals("LOST", ignoreCase = true)) "Active Search" else "Community Found",
                title = report.title.ifEmpty { "Reported Item" },
                subtitle = "${report.location.ifEmpty { "Colombo" }} · Live Post",
                location = report.location.ifEmpty { "Colombo" },
                status = report.status.ifEmpty { "LOST" },
                imageResId = R.drawable.img_wallet_details,
                description = report.description.ifEmpty { "No description provided." }
            )
        }

        if (urgentItems.isEmpty()) {
            urgentSliderContainer.visibility = View.GONE
            sliderRunnable?.let { sliderHandler.removeCallbacks(it) }
        } else {
            urgentSliderContainer.visibility = View.VISIBLE
            vpUrgentAlertSlider.adapter = UrgentSliderAdapter(urgentItems) { item ->
                parentFragmentManager.beginTransaction()
                    .replace(
                        R.id.screenBox,
                        DetailsFragment.newInstance(
                            title = item.title,
                            desc = item.description,
                            status = item.status,
                            location = item.location,
                            imageResId = item.imageResId,
                            itemId = item.id
                        )
                    )
                    .addToBackStack(null)
                    .commit()
            }

            pageChangeCallback?.let { vpUrgentAlertSlider.unregisterOnPageChangeCallback(it) }
            val newCallback = object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    updateSliderDots(position, urgentItems.size)
                }
            }
            pageChangeCallback = newCallback
            vpUrgentAlertSlider.registerOnPageChangeCallback(newCallback)
            updateSliderDots(vpUrgentAlertSlider.currentItem, urgentItems.size)

            sliderRunnable?.let { sliderHandler.removeCallbacks(it) }
            if (urgentItems.size > 1) {
                sliderRunnable = object : Runnable {
                    override fun run() {
                        if (!isAdded || view == null) return
                        val currentPos = vpUrgentAlertSlider.currentItem
                        val nextPos = (currentPos + 1) % urgentItems.size
                        vpUrgentAlertSlider.setCurrentItem(nextPos, true)
                        sliderHandler.postDelayed(this, 4000)
                    }
                }
                sliderHandler.postDelayed(sliderRunnable!!, 4000)
            }
        }
    }

    private fun updateSliderDots(position: Int, totalItems: Int) {
        val ctx = context ?: return
        val density = ctx.resources.displayMetrics.density
        val activeWidth = (16 * density).toInt()
        val inactiveWidth = (6 * density).toInt()

        val dots = listOf(dotSlider1, dotSlider2, dotSlider3)
        for (i in dots.indices) {
            val dot = dots[i]
            if (i < totalItems) {
                dot.visibility = View.VISIBLE
                val params = dot.layoutParams
                if (i == position) {
                    params.width = activeWidth
                    dot.setBackgroundResource(R.drawable.bg_dot_active)
                } else {
                    params.width = inactiveWidth
                    dot.setBackgroundResource(R.drawable.bg_dot_inactive_1)
                }
                dot.layoutParams = params
            } else {
                dot.visibility = View.GONE
            }
        }
    }

    private fun updateRecentCardsFromLive(reports: List<ReportItem>) {
        // Item 1
        if (reports.isNotEmpty()) {
            val first = reports[0]
            cardWalletItem.visibility = View.VISIBLE
            cardWalletItem.findViewById<TextView>(R.id.textWalletItemTitle)?.text = first.title
            cardWalletItem.findViewById<TextView>(R.id.textWalletLocationName)?.text = first.location
            cardWalletItem.findViewById<TextView>(R.id.badgeWalletLostTag)?.text = first.status
            cardWalletItem.setOnClickListener {
                openItem(first)
            }
        } else {
            cardWalletItem.visibility = View.GONE
        }

        // Item 2
        if (reports.size > 1) {
            val second = reports[1]
            cardDogItem.visibility = View.VISIBLE
            cardDogItem.findViewById<TextView>(R.id.textDogItemTitle)?.text = second.title
            cardDogItem.findViewById<TextView>(R.id.textDogLocationName)?.text = second.location
            cardDogItem.findViewById<TextView>(R.id.badgeDogFoundTag)?.text = second.status
            cardDogItem.setOnClickListener {
                openItem(second)
            }
        } else {
            cardDogItem.visibility = View.GONE
        }

        // Item 3
        if (reports.size > 2) {
            val third = reports[2]
            cardLaptopHome?.visibility = View.VISIBLE
            cardLaptopHome?.setOnClickListener {
                openItem(third)
            }
        } else {
            cardLaptopHome?.visibility = View.GONE
        }
    }

    private fun openItem(report: ReportItem) {
        parentFragmentManager.beginTransaction()
            .replace(
                R.id.screenBox,
                DetailsFragment.newInstance(
                    title = report.title,
                    desc = report.description,
                    status = report.status,
                    location = report.location,
                    imageResId = R.drawable.img_wallet_details,
                    itemId = report.itemId
                )
            )
            .addToBackStack(null)
            .commit()
    }

    private fun setupClickListeners() {
        buttonHeaderClose.setOnClickListener {
            if (parentFragmentManager.backStackEntryCount > 0) {
                parentFragmentManager.popBackStack()
            } else {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.screenBox, ProfileFragment.newInstance())
                    .addToBackStack(null)
                    .commit()
            }
        }

        buttonNotifications.setOnClickListener {
            val options = arrayOf(
                "View Notification Inbox",
                "Test Push Notification (Sound & Vibration)"
            )
            AlertDialog.Builder(requireContext())
                .setTitle("Notifications")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> {
                            parentFragmentManager.beginTransaction()
                                .replace(R.id.screenBox, NotificationsFragment.newInstance())
                                .addToBackStack(null)
                                .commit()
                        }
                        1 -> {
                            FCMService.triggerPushNotification(
                                context = requireContext(),
                                title = "Urgent Match Alert",
                                body = "Live community update for your reported item.",
                                itemId = liveReportsList.firstOrNull()?.itemId
                            )
                            com.group3.lostandfound.util.ModernToast.show(
                                requireContext(),
                                "Push notification dispatched with sound & vibration.",
                                view
                            )
                        }
                    }
                }
                .show()
        }

        buttonFilterSettings.setOnClickListener {
            Toast.makeText(requireContext(), "Filtering: All recent items", Toast.LENGTH_SHORT).show()
        }

        cardReportLostItem.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.screenBox, CreateReportFragment.newInstance(isLost = true))
                .addToBackStack(null)
                .commit()
        }

        cardReportFoundItem.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.screenBox, CreateReportFragment.newInstance(isLost = false))
                .addToBackStack(null)
                .commit()
        }

        chipCategoryAll.setOnClickListener {
            selectCategoryChip(chipCategoryAll)
            updateRecentCardsFromLive(liveReportsList)
        }
        chipCategoryElectronics.setOnClickListener {
            selectCategoryChip(chipCategoryElectronics)
            updateRecentCardsFromLive(liveReportsList.filter { it.category.contains("Electronic", ignoreCase = true) })
        }
        chipCategoryDocuments.setOnClickListener {
            selectCategoryChip(chipCategoryDocuments)
            updateRecentCardsFromLive(liveReportsList.filter { it.category.contains("Document", ignoreCase = true) })
        }

        buttonSeeAllRecent.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.screenBox, ExploreFragment.newInstance())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun selectCategoryChip(selectedChip: TextView) {
        val ctx = context ?: return
        val allChips = listOf(chipCategoryAll, chipCategoryElectronics, chipCategoryDocuments)
        for (chip in allChips) {
            if (chip == selectedChip) {
                chip.setBackgroundResource(R.drawable.bg_chip_selected)
                chip.setTextColor(ContextCompat.getColor(ctx, R.color.white))
            } else {
                chip.setBackgroundResource(R.drawable.bg_chip_unselected)
                chip.setTextColor(ContextCompat.getColor(ctx, R.color.text_primary))
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        pageChangeCallback?.let { vpUrgentAlertSlider.unregisterOnPageChangeCallback(it) }
        itemsListener?.let {
            FirebaseDatabase.getInstance(FirebaseManager.DATABASE_URL)
                .getReference("items")
                .removeEventListener(it)
        }
        sliderRunnable?.let { sliderHandler.removeCallbacks(it) }
    }

    companion object {
        fun newInstance(): HomeFragment {
            return HomeFragment()
        }
    }

    data class UrgentItemData(
        val id: String,
        val badge: String,
        val isUrgent: Boolean,
        val rewardText: String,
        val title: String,
        val subtitle: String,
        val location: String,
        val status: String,
        val imageResId: Int,
        val description: String
    )

    private class UrgentSliderAdapter(
        private val items: List<UrgentItemData>,
        private val onClick: (UrgentItemData) -> Unit
    ) : RecyclerView.Adapter<UrgentSliderAdapter.SliderViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SliderViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_urgent_slider, parent, false)
            return SliderViewHolder(view)
        }

        override fun onBindViewHolder(holder: SliderViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size

        inner class SliderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val tvSliderBadge: TextView = itemView.findViewById(R.id.tvSliderBadge)
            private val tvSliderReward: TextView = itemView.findViewById(R.id.tvSliderReward)
            private val tvSliderTitle: TextView = itemView.findViewById(R.id.tvSliderTitle)
            private val tvSliderSubtitle: TextView = itemView.findViewById(R.id.tvSliderSubtitle)
            private val imgSliderThumbnail: ImageView = itemView.findViewById(R.id.imgSliderThumbnail)

            fun bind(item: UrgentItemData) {
                tvSliderBadge.text = item.badge
                if (item.isUrgent) {
                    tvSliderBadge.setBackgroundResource(R.drawable.bg_badge_flagged)
                    tvSliderBadge.setTextColor(ContextCompat.getColor(itemView.context, R.color.badge_flagged_text))
                } else {
                    tvSliderBadge.setBackgroundResource(R.drawable.bg_badge_pending)
                    tvSliderBadge.setTextColor(ContextCompat.getColor(itemView.context, R.color.badge_pending_text))
                }

                tvSliderReward.text = item.rewardText
                tvSliderTitle.text = item.title
                tvSliderSubtitle.text = item.subtitle
                imgSliderThumbnail.setImageResource(item.imageResId)

                itemView.setOnClickListener { onClick(item) }
            }
        }
    }
}
