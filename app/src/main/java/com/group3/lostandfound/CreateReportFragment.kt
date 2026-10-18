package com.group3.lostandfound

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.DecelerateInterpolator
import android.view.animation.TranslateAnimation
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.ViewFlipper
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

/**
 * CreateReportFragment allows users to report a lost or found item across 5 sliding steps:
 * - Step 1: Type & Date
 * - Step 2: Category & Details
 * - Step 3: Location Picker
 * - Step 4: Photos Upload
 * - Step 5: Review & Submit
 * Modern sliding transitions between steps with interactive progress indicator.
 */
class CreateReportFragment : Fragment() {

    private lateinit var btnBack: ImageView
    private lateinit var btnBackBottom: TextView
    private lateinit var btnContinue: TextView

    // Progress Segments
    private lateinit var segStep1: View
    private lateinit var segStep2: View
    private lateinit var segStep3: View
    private lateinit var segStep4: View
    private lateinit var segStep5: View

    // Step Headings
    private lateinit var tvStepNumber: TextView
    private lateinit var tvStepTitle: TextView
    private lateinit var tvStepSubtitle: TextView

    // ViewFlipper
    private lateinit var flipperSteps: ViewFlipper

    // Step 1 Views
    private lateinit var cardLostItem: View
    private lateinit var cardFoundItem: View
    private lateinit var iconLost: ImageView
    private lateinit var iconFound: ImageView
    private lateinit var chipDateToday: TextView
    private lateinit var chipDateYesterday: TextView
    private lateinit var chipDateEarlier: TextView

    // Step 2 Views
    private lateinit var boxCategory: View
    private lateinit var tvCategoryValue: TextView
    private lateinit var inputTitle: EditText
    private lateinit var inputDescription: EditText

    // Step 3 Views
    private lateinit var chipAreaColombo3: TextView
    private lateinit var chipAreaKollupitiya: TextView
    private lateinit var chipAreaBamba: TextView
    private lateinit var chipAreaFort: TextView
    private lateinit var inputLandmark: EditText
    private lateinit var tvSelectedLocationPreview: TextView

    // Step 4 Views
    private lateinit var cardUploadDropzone: View
    private lateinit var tvPhotosCountStatus: TextView

    // Step 5 Review Views
    private lateinit var badgeReviewStatus: TextView
    private lateinit var tvReviewCategory: TextView
    private lateinit var tvReviewTitle: TextView
    private lateinit var tvReviewLocation: TextView
    private lateinit var tvReviewDesc: TextView

    // State
    private var currentStep = 1
    private var isLostSelected = true
    private var selectedDate = "Today"
    private var selectedCategory = "Electronics"
    private var selectedArea = "Colombo 03"
    private var photosCount = 2

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_create_report, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)

        val initialIsLost = arguments?.getBoolean(ARG_IS_LOST, true) ?: true
        setItemType(initialIsLost)

        setupStepListeners()
        updateStepHeader()
        setupBottomNav(view, "post")
    }

    private fun initViews(view: View) {
        btnBack = view.findViewById(R.id.btnBack)
        btnBackBottom = view.findViewById(R.id.btnBackBottom)
        btnContinue = view.findViewById(R.id.btnContinue)

        segStep1 = view.findViewById(R.id.segStep1)
        segStep2 = view.findViewById(R.id.segStep2)
        segStep3 = view.findViewById(R.id.segStep3)
        segStep4 = view.findViewById(R.id.segStep4)
        segStep5 = view.findViewById(R.id.segStep5)

        tvStepNumber = view.findViewById(R.id.tvStepNumber)
        tvStepTitle = view.findViewById(R.id.tvStepTitle)
        tvStepSubtitle = view.findViewById(R.id.tvStepSubtitle)

        flipperSteps = view.findViewById(R.id.flipperSteps)

        // Step 1
        cardLostItem = view.findViewById(R.id.cardLostItem)
        cardFoundItem = view.findViewById(R.id.cardFoundItem)
        iconLost = view.findViewById(R.id.iconLost)
        iconFound = view.findViewById(R.id.iconFound)
        chipDateToday = view.findViewById(R.id.chipDateToday)
        chipDateYesterday = view.findViewById(R.id.chipDateYesterday)
        chipDateEarlier = view.findViewById(R.id.chipDateEarlier)

        // Step 2
        boxCategory = view.findViewById(R.id.boxCategory)
        tvCategoryValue = view.findViewById(R.id.tvCategoryValue)
        inputTitle = view.findViewById(R.id.inputTitle)
        inputDescription = view.findViewById(R.id.inputDescription)

        // Step 3
        chipAreaColombo3 = view.findViewById(R.id.chipAreaColombo3)
        chipAreaKollupitiya = view.findViewById(R.id.chipAreaKollupitiya)
        chipAreaBamba = view.findViewById(R.id.chipAreaBamba)
        chipAreaFort = view.findViewById(R.id.chipAreaFort)
        inputLandmark = view.findViewById(R.id.inputLandmark)
        tvSelectedLocationPreview = view.findViewById(R.id.tvSelectedLocationPreview)

        // Step 4
        cardUploadDropzone = view.findViewById(R.id.cardUploadDropzone)
        tvPhotosCountStatus = view.findViewById(R.id.tvPhotosCountStatus)

        // Step 5
        badgeReviewStatus = view.findViewById(R.id.badgeReviewStatus)
        tvReviewCategory = view.findViewById(R.id.tvReviewCategory)
        tvReviewTitle = view.findViewById(R.id.tvReviewTitle)
        tvReviewLocation = view.findViewById(R.id.tvReviewLocation)
        tvReviewDesc = view.findViewById(R.id.tvReviewDesc)
    }

    private fun setItemType(isLost: Boolean) {
        isLostSelected = isLost
        if (isLost) {
            cardLostItem.setBackgroundResource(R.drawable.bg_card_select_lost)
            iconLost.setColorFilter(ContextCompat.getColor(requireContext(), R.color.tag_lost_color))

            cardFoundItem.setBackgroundResource(R.drawable.bg_card_select_unselected)
            iconFound.setColorFilter(ContextCompat.getColor(requireContext(), R.color.text_primary))
        } else {
            cardFoundItem.setBackgroundResource(R.drawable.bg_card_select_lost)
            iconFound.setColorFilter(ContextCompat.getColor(requireContext(), R.color.tag_lost_color))

            cardLostItem.setBackgroundResource(R.drawable.bg_card_select_unselected)
            iconLost.setColorFilter(ContextCompat.getColor(requireContext(), R.color.text_primary))
        }
    }

    private fun setupStepListeners() {
        // Back buttons
        val onBackAction = View.OnClickListener {
            if (currentStep > 1) {
                slideBackward()
            } else {
                parentFragmentManager.popBackStack()
            }
        }
        btnBack.setOnClickListener(onBackAction)
        btnBackBottom.setOnClickListener(onBackAction)

        // Continue button
        btnContinue.setOnClickListener {
            if (currentStep < 5) {
                slideForward()
            } else {
                // Submit step 5
                val title = inputTitle.text.toString().trim()
                val reportTitle = if (title.isNotEmpty()) title else if (isLostSelected) "Lost Item" else "Found Item"
                val itemType = if (isLostSelected) "Lost" else "Found"
                Toast.makeText(requireContext(), "🎉 $itemType report '$reportTitle' published successfully!", Toast.LENGTH_LONG).show()

                parentFragmentManager.beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                    .replace(R.id.screenBox, ReportsFragment.newInstance())
                    .commit()
            }
        }

        // Step 1: Type Selection
        cardLostItem.setOnClickListener { setItemType(true) }
        cardFoundItem.setOnClickListener { setItemType(false) }

        val dateChips = listOf(chipDateToday, chipDateYesterday, chipDateEarlier)
        for (chip in dateChips) {
            chip.setOnClickListener {
                for (c in dateChips) {
                    if (c == chip) {
                        c.setBackgroundResource(R.drawable.bg_chip_selected)
                        c.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                        selectedDate = c.text.toString()
                    } else {
                        c.setBackgroundResource(R.drawable.bg_chip_unselected)
                        c.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
                    }
                }
            }
        }

        // Step 2: Category dialog
        boxCategory.setOnClickListener {
            val categories = arrayOf(
                "Electronics",
                "Documents & IDs",
                "Wallets & Bags",
                "Pets & Animals",
                "Keys",
                "Jewelry & Watches",
                "Other"
            )
            AlertDialog.Builder(requireContext())
                .setTitle("Select Category")
                .setItems(categories) { _, which ->
                    selectedCategory = categories[which]
                    tvCategoryValue.text = selectedCategory
                }
                .show()
        }

        // Step 3: Area chips
        val areaChips = listOf(chipAreaColombo3, chipAreaKollupitiya, chipAreaBamba, chipAreaFort)
        for (chip in areaChips) {
            chip.setOnClickListener {
                for (c in areaChips) {
                    if (c == chip) {
                        c.setBackgroundResource(R.drawable.bg_chip_selected)
                        c.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                        selectedArea = c.text.toString()
                        tvSelectedLocationPreview.text = "$selectedArea, Western Province"
                    } else {
                        c.setBackgroundResource(R.drawable.bg_chip_unselected)
                        c.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
                    }
                }
            }
        }

        // Step 4: Photo dropzone
        cardUploadDropzone.setOnClickListener {
            photosCount++
            tvPhotosCountStatus.text = "$photosCount photos attached (Tap to add more)"
            Toast.makeText(requireContext(), "Added photo from device gallery", Toast.LENGTH_SHORT).show()
        }
    }

    private fun slideForward() {
        val slideInRight = TranslateAnimation(
            Animation.RELATIVE_TO_PARENT, 1.0f,
            Animation.RELATIVE_TO_PARENT, 0.0f,
            Animation.RELATIVE_TO_PARENT, 0.0f,
            Animation.RELATIVE_TO_PARENT, 0.0f
        ).apply {
            duration = 320
            interpolator = DecelerateInterpolator()
        }

        val slideOutLeft = TranslateAnimation(
            Animation.RELATIVE_TO_PARENT, 0.0f,
            Animation.RELATIVE_TO_PARENT, -1.0f,
            Animation.RELATIVE_TO_PARENT, 0.0f,
            Animation.RELATIVE_TO_PARENT, 0.0f
        ).apply {
            duration = 320
            interpolator = DecelerateInterpolator()
        }

        flipperSteps.inAnimation = slideInRight
        flipperSteps.outAnimation = slideOutLeft
        flipperSteps.showNext()

        currentStep++
        updateStepHeader()
    }

    private fun slideBackward() {
        val slideInLeft = TranslateAnimation(
            Animation.RELATIVE_TO_PARENT, -1.0f,
            Animation.RELATIVE_TO_PARENT, 0.0f,
            Animation.RELATIVE_TO_PARENT, 0.0f,
            Animation.RELATIVE_TO_PARENT, 0.0f
        ).apply {
            duration = 320
            interpolator = DecelerateInterpolator()
        }

        val slideOutRight = TranslateAnimation(
            Animation.RELATIVE_TO_PARENT, 0.0f,
            Animation.RELATIVE_TO_PARENT, 1.0f,
            Animation.RELATIVE_TO_PARENT, 0.0f,
            Animation.RELATIVE_TO_PARENT, 0.0f
        ).apply {
            duration = 320
            interpolator = DecelerateInterpolator()
        }

        flipperSteps.inAnimation = slideInLeft
        flipperSteps.outAnimation = slideOutRight
        flipperSteps.showPrevious()

        currentStep--
        updateStepHeader()
    }

    private fun updateStepHeader() {
        tvStepNumber.text = "STEP $currentStep OF 5"

        val activeDrawable = R.drawable.bg_step_progress_active
        val inactiveDrawable = R.drawable.bg_step_progress_inactive

        segStep1.setBackgroundResource(if (currentStep >= 1) activeDrawable else inactiveDrawable)
        segStep2.setBackgroundResource(if (currentStep >= 2) activeDrawable else inactiveDrawable)
        segStep3.setBackgroundResource(if (currentStep >= 3) activeDrawable else inactiveDrawable)
        segStep4.setBackgroundResource(if (currentStep >= 4) activeDrawable else inactiveDrawable)
        segStep5.setBackgroundResource(if (currentStep >= 5) activeDrawable else inactiveDrawable)

        when (currentStep) {
            1 -> {
                tvStepTitle.text = "What happened?"
                tvStepSubtitle.text = "Select whether you lost or found an item."
                btnContinue.text = "Continue"
            }
            2 -> {
                tvStepTitle.text = "Category & details"
                tvStepSubtitle.text = "Tell the community what to look for."
                btnContinue.text = "Continue"
            }
            3 -> {
                tvStepTitle.text = "Where was it?"
                tvStepSubtitle.text = "Pin the neighborhood or specific spot."
                btnContinue.text = "Continue"
            }
            4 -> {
                tvStepTitle.text = "Add photos"
                tvStepSubtitle.text = "Clear photos increase reunion chances by 85%."
                btnContinue.text = "Continue"
            }
            5 -> {
                tvStepTitle.text = "Review & publish"
                tvStepSubtitle.text = "Double-check your report before publishing."
                btnContinue.text = "Publish Report"

                // Populate Step 5 Review
                val title = inputTitle.text.toString().trim()
                val landmark = inputLandmark.text.toString().trim()
                val desc = inputDescription.text.toString().trim()

                tvReviewTitle.text = if (title.isNotEmpty()) title else if (isLostSelected) "Lost Item" else "Found Item"
                badgeReviewStatus.text = if (isLostSelected) "LOST ITEM" else "FOUND ITEM"
                badgeReviewStatus.setTextColor(ContextCompat.getColor(requireContext(), if (isLostSelected) R.color.tag_lost_color else R.color.tag_found_color))
                tvReviewCategory.text = selectedCategory
                tvReviewLocation.text = "$selectedArea ${if (landmark.isNotEmpty()) "· $landmark" else ""} · Reported $selectedDate"
                tvReviewDesc.text = if (desc.isNotEmpty()) desc else "No additional description provided."
            }
        }
    }

    companion object {
        private const val ARG_IS_LOST = "arg_is_lost"

        fun newInstance(isLost: Boolean = true): CreateReportFragment {
            val fragment = CreateReportFragment()
            val args = Bundle()
            args.putBoolean(ARG_IS_LOST, isLost)
            fragment.arguments = args
            return fragment
        }
    }
}
