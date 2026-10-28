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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.card.MaterialCardView
import com.group3.lostandfound.model.ItemReport
import com.group3.lostandfound.service.FCMService
import com.group3.lostandfound.util.ImageLoader
import com.group3.lostandfound.viewmodel.HomeViewModel
import kotlinx.coroutines.launch

/**
 * HomeFragment displays the main dashboard of the Foundly app following MVVM architecture:
 * - Realtime Live Data from HomeViewModel (ZERO DUMMY DATA)
 * - Dynamic Urgent Item Highlights Carousel Slider from HomeViewModel
 * - Quick action cards: "Report Lost" & "Report Found"
 * - Live Community statistics
 * - Category filter chips ("All", "Electronics", "Documents")
 * - Dynamic recent items from ViewModel
 * - Push notifications with sound triggers
 */
class HomeFragment : Fragment() {

    // Header views
    private lateinit var buttonHeaderClose: ImageView
    private lateinit var buttonNotifications: FrameLayout

    // Search views
    private lateinit var inputSearchField: EditText
    private lateinit var buttonFilterSettings: ImageView

    // Urgent banner slider views
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

    // MVVM ViewModel & active reports
    private lateinit var homeViewModel: HomeViewModel
    private var currentReportsList: List<ItemReport> = emptyList()
    private var pageChangeCallback: ViewPager2.OnPageChangeCallback? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireContext().getSharedPreferences("findit_prefs", android.content.Context.MODE_PRIVATE)
        val isAdmin = prefs.getBoolean("is_admin", false) || FirebaseManager.currentUserEmail.equals("admin@gmail.com", ignoreCase = true)
        if (isAdmin) {
            parentFragmentManager.beginTransaction()
                .replace(R.id.screenBox, AdminFragment.newInstance())
                .commit()
            return
        }

        homeViewModel = ViewModelProvider(this)[HomeViewModel::class.java]

        initViews(view)
        setupClickListeners()
        observeHomeViewModel()
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

    private fun observeHomeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            homeViewModel.uiState.collect { state ->
                if (!isAdded || view == null) return@collect

                view?.findViewById<TextView>(R.id.textGreetingMessage)?.text = "Good day, ${state.userGreetingName}"

                currentReportsList = state.recentItems
                updateUrgentSliderFromLive(state.urgentItems)
                updateRecentCardsFromLive(state.recentItems)

                view?.findViewById<TextView>(R.id.textReunitedStatistics)?.let { tvStat ->
                    val countDisplay = state.recentItems.size
                    tvStat.text = "$countDisplay community reports active"
                }
            }
        }
    }

    private fun updateUrgentSliderFromLive(reports: List<ItemReport>) {
        if (!isAdded || view == null) return

        val urgentItems = reports.map { report ->
            UrgentItemData(
                id = report.itemId,
                badge = if (report.status.equals("LOST", ignoreCase = true)) "URGENT LOST" else "FOUND REPORT",
                isUrgent = report.status.equals("LOST", ignoreCase = true),
                rewardText = if (report.status.equals("LOST", ignoreCase = true)) "Active Search" else "Community Found",
                title = report.title.ifEmpty { "Reported Item" },
                subtitle = "${report.location.ifEmpty { "Colombo" }} · Live Post",
                location = report.location.ifEmpty { "Colombo" },
                status = report.status.ifEmpty { "LOST" },
                imageUrl = report.imageUrl,
                description = report.description.ifEmpty { "No description provided." },
                userId = report.userId
            )
        }

        if (urgentItems.isEmpty()) {
            urgentSliderContainer.visibility = View.GONE
            sliderRunnable?.let { sliderHandler.removeCallbacks(it) }
        } else {
            urgentSliderContainer.visibility = View.VISIBLE
            vpUrgentAlertSlider.adapter = UrgentSliderAdapter(urgentItems, viewLifecycleOwner.lifecycleScope) { item ->
                parentFragmentManager.beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                    .replace(
                        R.id.screenBox,
                        DetailsFragment.newInstance(
                            title = item.title,
                            desc = item.description,
                            status = item.status,
                            location = item.location,
                            imageResId = R.drawable.ic_item_placeholder,
                            itemId = item.id,
                            reporterUserId = item.userId
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

            updateSliderDots(0, urgentItems.size)
            startAutoScrollSlider(urgentItems.size)
        }
    }

    private fun startAutoScrollSlider(itemCount: Int) {
        sliderRunnable?.let { sliderHandler.removeCallbacks(it) }
        if (itemCount <= 1) return

        val runnable = object : Runnable {
            override fun run() {
                if (!isAdded || view == null) return
                val current = vpUrgentAlertSlider.currentItem
                val next = (current + 1) % itemCount
                vpUrgentAlertSlider.setCurrentItem(next, true)
                sliderHandler.postDelayed(this, 4000)
            }
        }
        sliderRunnable = runnable
        sliderHandler.postDelayed(runnable, 4000)
    }

    private fun updateSliderDots(position: Int, totalItems: Int) {
        val density = resources.displayMetrics.density
        val activeWidth = (22 * density).toInt()
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

    private fun updateRecentCardsFromLive(reports: List<ItemReport>) {
        view?.findViewById<TextView>(R.id.tvEmptyRecent)?.visibility = if (reports.isEmpty()) View.VISIBLE else View.GONE

        // Item 1
        if (reports.isNotEmpty()) {
            val first = reports[0]
            cardWalletItem.visibility = View.VISIBLE
            cardWalletItem.findViewById<TextView>(R.id.textWalletItemTitle)?.text = first.title
            cardWalletItem.findViewById<TextView>(R.id.textWalletLocationName)?.text = first.location
            cardWalletItem.findViewById<TextView>(R.id.badgeWalletLostTag)?.apply {
                text = first.status.uppercase()
                val isLost = first.status.equals("LOST", ignoreCase = true)
                setTextColor(ContextCompat.getColor(context, if (isLost) R.color.tag_lost_color else R.color.tag_found_color))
                setBackgroundResource(if (isLost) R.drawable.bg_tag_lost_outlined else R.drawable.bg_tag_found_outlined)
            }
            cardWalletItem.findViewById<ImageView>(R.id.imageWalletPhoto)?.let { img ->
                ImageLoader.load(img, first.imageUrl, R.drawable.ic_item_placeholder, viewLifecycleOwner.lifecycleScope)
            }
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
            cardDogItem.findViewById<TextView>(R.id.badgeDogFoundTag)?.apply {
                text = second.status.uppercase()
                val isLost = second.status.equals("LOST", ignoreCase = true)
                setTextColor(ContextCompat.getColor(context, if (isLost) R.color.tag_lost_color else R.color.tag_found_color))
                setBackgroundResource(if (isLost) R.drawable.bg_tag_lost_outlined else R.drawable.bg_tag_found_outlined)
            }
            cardDogItem.findViewById<ImageView>(R.id.imageDogPhoto)?.let { img ->
                ImageLoader.load(img, second.imageUrl, R.drawable.ic_item_placeholder, viewLifecycleOwner.lifecycleScope)
            }
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
            cardLaptopHome?.findViewById<TextView>(R.id.textLaptopItemTitle)?.text = third.title
            cardLaptopHome?.findViewById<TextView>(R.id.textLaptopLocationName)?.text = third.location
            cardLaptopHome?.findViewById<TextView>(R.id.badgeLaptopTag)?.apply {
                text = third.status.uppercase()
                val isLost = third.status.equals("LOST", ignoreCase = true)
                setTextColor(ContextCompat.getColor(context, if (isLost) R.color.tag_lost_color else R.color.tag_found_color))
                setBackgroundResource(if (isLost) R.drawable.bg_tag_lost_outlined else R.drawable.bg_tag_found_outlined)
            }
            cardLaptopHome?.findViewById<ImageView>(R.id.imageLaptopPhoto)?.let { img ->
                ImageLoader.load(img, third.imageUrl, R.drawable.ic_item_placeholder, viewLifecycleOwner.lifecycleScope)
            }
            cardLaptopHome?.setOnClickListener {
                openItem(third)
            }
        } else {
            cardLaptopHome?.visibility = View.GONE
        }
    }

    private fun openItem(report: ItemReport) {
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(
                R.id.screenBox,
                DetailsFragment.newInstance(
                    itemId = report.itemId,
                    title = report.title,
                    desc = report.description,
                    status = report.status,
                    location = report.location,
                    imageUrl = report.imageUrl,
                    reporterUserId = report.userId,
                    authorName = report.authorName
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
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.screenBox, NotificationsFragment.newInstance())
                .addToBackStack(null)
                .commit()
        }

        buttonFilterSettings.setOnClickListener {
            val bottomSheet = com.google.android.material.bottomsheet.BottomSheetDialog(requireContext())
            val sheetView = layoutInflater.inflate(R.layout.dialog_notification_options, null)
            bottomSheet.setContentView(sheetView)
            bottomSheet.setOnShowListener { dialogInterface ->
                val dialog = dialogInterface as? com.google.android.material.bottomsheet.BottomSheetDialog
                dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                    ?.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            }

            val btnClose = sheetView.findViewById<View>(R.id.btnCloseNotificationOptions)
            val btnSave = sheetView.findViewById<View>(R.id.btnSavePreferences)
            val chip1 = sheetView.findViewById<TextView>(R.id.chipRadius1km)
            val chip5 = sheetView.findViewById<TextView>(R.id.chipRadius5km)
            val chip10 = sheetView.findViewById<TextView>(R.id.chipRadius10km)
            val tabInstant = sheetView.findViewById<TextView>(R.id.tabInstant)
            val tabDaily = sheetView.findViewById<TextView>(R.id.tabDailyDigest)

            var selectedRadius = 5
            fun updateRadiusChips(radius: Int) {
                selectedRadius = radius
                chip1?.setBackgroundResource(if (radius == 1) R.drawable.bg_btn_save_profile else R.drawable.bg_btn_cancel_edit)
                chip1?.setTextColor(if (radius == 1) android.graphics.Color.WHITE else android.graphics.Color.parseColor("#002B3D"))

                chip5?.setBackgroundResource(if (radius == 5) R.drawable.bg_btn_save_profile else R.drawable.bg_btn_cancel_edit)
                chip5?.setTextColor(if (radius == 5) android.graphics.Color.WHITE else android.graphics.Color.parseColor("#002B3D"))

                chip10?.setBackgroundResource(if (radius == 10) R.drawable.bg_btn_save_profile else R.drawable.bg_btn_cancel_edit)
                chip10?.setTextColor(if (radius == 10) android.graphics.Color.WHITE else android.graphics.Color.parseColor("#002B3D"))
            }

            chip1?.setOnClickListener { updateRadiusChips(1) }
            chip5?.setOnClickListener { updateRadiusChips(5) }
            chip10?.setOnClickListener { updateRadiusChips(10) }

            tabInstant?.setOnClickListener {
                tabInstant.setBackgroundResource(R.drawable.bg_segment_active)
                tabInstant.setTextColor(android.graphics.Color.parseColor("#002B3D"))
                tabDaily?.setBackgroundResource(0)
                tabDaily?.setTextColor(android.graphics.Color.parseColor("#64748B"))
            }

            tabDaily?.setOnClickListener {
                tabDaily.setBackgroundResource(R.drawable.bg_segment_active)
                tabDaily.setTextColor(android.graphics.Color.parseColor("#002B3D"))
                tabInstant?.setBackgroundResource(0)
                tabInstant?.setTextColor(android.graphics.Color.parseColor("#64748B"))
            }

            btnClose?.setOnClickListener {
                bottomSheet.dismiss()
            }

            btnSave?.setOnClickListener {
                com.group3.lostandfound.util.ModernToast.show(requireContext(), "Preferences saved!", view)
                bottomSheet.dismiss()
            }

            bottomSheet.show()
        }

        buttonSeeAllRecent.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.screenBox, ExploreFragment.newInstance())
                .addToBackStack(null)
                .commit()
        }

        cardReportLostItem.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.screenBox, CreateReportFragment.newInstance(true))
                .addToBackStack(null)
                .commit()
        }

        cardReportFoundItem.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.screenBox, CreateReportFragment.newInstance(false))
                .addToBackStack(null)
                .commit()
        }

        chipCategoryAll.setOnClickListener {
            highlightChip(chipCategoryAll)
            filterByCategory("All")
        }

        chipCategoryElectronics.setOnClickListener {
            highlightChip(chipCategoryElectronics)
            filterByCategory("Electronics")
        }

        chipCategoryDocuments.setOnClickListener {
            highlightChip(chipCategoryDocuments)
            filterByCategory("Documents")
        }
    }

    private fun highlightChip(selectedChip: TextView) {
        val chips = listOf(chipCategoryAll, chipCategoryElectronics, chipCategoryDocuments)
        val selectedBg = ContextCompat.getDrawable(requireContext(), R.drawable.bg_chip_selected)
        val unselectedBg = ContextCompat.getDrawable(requireContext(), R.drawable.bg_chip_unselected)
        val selectedColor = ContextCompat.getColor(requireContext(), R.color.white)
        val unselectedColor = ContextCompat.getColor(requireContext(), R.color.text_primary)

        chips.forEach { chip ->
            if (chip == selectedChip) {
                chip.background = selectedBg
                chip.setTextColor(selectedColor)
            } else {
                chip.background = unselectedBg
                chip.setTextColor(unselectedColor)
            }
        }
    }

    private fun filterByCategory(category: String) {
        if (category.equals("All", ignoreCase = true)) {
            updateRecentCardsFromLive(currentReportsList)
        } else {
            val filtered = currentReportsList.filter {
                it.category.contains(category, ignoreCase = true)
            }
            updateRecentCardsFromLive(filtered)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        sliderRunnable?.let { sliderHandler.removeCallbacks(it) }
        pageChangeCallback?.let { vpUrgentAlertSlider.unregisterOnPageChangeCallback(it) }
    }

    companion object {
        fun newInstance(): HomeFragment = HomeFragment()
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
        val imageUrl: String,
        val description: String,
        val userId: String = ""
    )

    class UrgentSliderAdapter(
        private val items: List<UrgentItemData>,
        private val scope: kotlinx.coroutines.CoroutineScope,
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
                ImageLoader.load(imgSliderThumbnail, item.imageUrl, R.drawable.ic_item_placeholder, scope)

                itemView.setOnClickListener { onClick(item) }
            }
        }
    }
}
