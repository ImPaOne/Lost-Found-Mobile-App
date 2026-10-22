package com.group3.lostandfound

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.DecelerateInterpolator
import android.view.animation.TranslateAnimation
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import android.widget.ViewFlipper
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.group3.lostandfound.model.ItemReport
import com.group3.lostandfound.repository.ReportRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

/**
 * CreateReportFragment allows users to report a lost or found item across 5 sliding steps:
 * - Step 1: Type & Date
 * - Step 2: Category & Details
 * - Step 3: Location Picker
 * - Step 4: Photos Upload (Camera & Gallery with image compression)
 * - Step 5: Review & Submit (with 0-100% horizontal progress bar, retry mechanisms, and Clean Architecture)
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
    private lateinit var tvSelectedLocationCoords: TextView
    private lateinit var cardMapPinPreview: View

    // Step 4 Views
    private lateinit var cardUploadDropzone: View
    private lateinit var tvPhotosCountStatus: TextView
    private lateinit var rowThumbnails: View
    private lateinit var imgThumbnail1: ImageView

    // Step 5 Review Views
    private lateinit var badgeReviewStatus: TextView
    private lateinit var tvReviewCategory: TextView
    private lateinit var tvReviewTitle: TextView
    private lateinit var tvReviewLocation: TextView
    private lateinit var tvReviewDesc: TextView

    // Upload Progress & Error UI Views
    private lateinit var cardUploadProgress: View
    private lateinit var tvUploadStatusText: TextView
    private lateinit var tvUploadPercentage: TextView
    private lateinit var pbUploadBar: ProgressBar
    private lateinit var layoutUploadError: LinearLayout
    private lateinit var tvErrorMessage: TextView
    private lateinit var btnRetryUpload: TextView

    // State
    private var currentStep = 1
    private var isLostSelected = true
    private var selectedDate = "Today"
    private var selectedCategory = "Electronics"
    private var selectedArea = "Colombo 03"
    private var selectedLatitude = 6.9042
    private var selectedLongitude = 79.8530
    private var photosCount = 0
    private var selectedImageUri: Uri? = null

    // Persisted form inputs across fragment backstack/step navigation
    private var savedTitle: String = ""
    private var savedDescription: String = ""
    private var savedLandmark: String = ""

    private val reportRepository = ReportRepository()
    private var uploadJob: Job? = null

    // Gallery Picker Launcher
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            photosCount = 1
            tvPhotosCountStatus.text = "$photosCount photo attached from gallery"
            imgThumbnail1.setImageURI(uri)
            rowThumbnails.visibility = View.VISIBLE
            Toast.makeText(requireContext(), "Photo selected from device gallery", Toast.LENGTH_SHORT).show()
        }
    }

    // Camera Permission Launcher
    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            takePictureLauncher.launch(null)
        } else {
            Toast.makeText(
                requireContext(),
                "Camera permission is required to capture photos",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Camera Capture Launcher
    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            val uri = saveBitmapToTempUri(bitmap)
            if (uri != null) {
                selectedImageUri = uri
                photosCount = 1
                tvPhotosCountStatus.text = "$photosCount photo captured with camera"
                imgThumbnail1.setImageBitmap(bitmap)
                rowThumbnails.visibility = View.VISIBLE
                Toast.makeText(requireContext(), "Camera photo captured successfully", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        parentFragmentManager.setFragmentResultListener(
            MapFragment.REQUEST_KEY_LOCATION,
            this
        ) { _, bundle ->
            val lat = bundle.getDouble(MapFragment.KEY_LATITUDE, 0.0)
            val lng = bundle.getDouble(MapFragment.KEY_LONGITUDE, 0.0)
            val locName = bundle.getString(MapFragment.KEY_LOCATION_NAME)
            if (lat != 0.0 && lng != 0.0) {
                selectedLatitude = lat
                selectedLongitude = lng
                selectedArea = locName ?: String.format(java.util.Locale.US, "%.4f, %.4f", lat, lng)

                if (view != null) {
                    tvSelectedLocationPreview.text = "$selectedArea, Western Province"
                    tvSelectedLocationCoords.text = String.format(
                        java.util.Locale.US,
                        "%.4f° N, %.4f° E · Selected from map",
                        selectedLatitude,
                        selectedLongitude
                    )
                }
            }
        }
    }

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

        // Only parse initial navigation arguments if starting fresh at Step 1 with empty inputs
        if (savedInstanceState == null && currentStep == 1 && savedTitle.isEmpty() && savedDescription.isEmpty()) {
            val initialIsLost = arguments?.getBoolean(ARG_IS_LOST, true) ?: true
            isLostSelected = initialIsLost

            val argLat = arguments?.getDouble(ARG_LAT, 0.0) ?: 0.0
            val argLng = arguments?.getDouble(ARG_LNG, 0.0) ?: 0.0
            val argLoc = arguments?.getString(ARG_LOC_NAME)

            if (argLat != 0.0 && argLng != 0.0) {
                selectedLatitude = argLat
                selectedLongitude = argLng
                selectedArea = argLoc ?: String.format(java.util.Locale.US, "%.4f, %.4f", argLat, argLng)
            }
        }

        // Apply state to views
        setItemType(isLostSelected)
        tvCategoryValue.text = selectedCategory
        tvSelectedLocationPreview.text = "$selectedArea, Western Province"
        tvSelectedLocationCoords.text = String.format(
            java.util.Locale.US,
            "%.4f° N, %.4f° E · Selected on map",
            selectedLatitude,
            selectedLongitude
        )

        // Restore input texts if previously entered
        if (savedTitle.isNotEmpty()) inputTitle.setText(savedTitle)
        if (savedDescription.isNotEmpty()) inputDescription.setText(savedDescription)
        if (savedLandmark.isNotEmpty()) inputLandmark.setText(savedLandmark)

        // Restore photo thumbnail if selected
        if (selectedImageUri != null) {
            imgThumbnail1.setImageURI(selectedImageUri)
            rowThumbnails.visibility = View.VISIBLE
            tvPhotosCountStatus.text = "$photosCount photo attached"
        }

        // Restore date chips
        val dateChips = listOf(chipDateToday, chipDateYesterday, chipDateEarlier)
        for (c in dateChips) {
            if (c.text.toString().equals(selectedDate, ignoreCase = true)) {
                c.setBackgroundResource(R.drawable.bg_chip_selected)
                c.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            } else {
                c.setBackgroundResource(R.drawable.bg_chip_unselected)
                c.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
            }
        }

        // Restore ViewFlipper step without animation
        flipperSteps.displayedChild = (currentStep - 1).coerceIn(0, 4)

        setupStepListeners()
        updateStepHeader()
        setupBottomNav(view, "post")

        // Handle Back button to step backward if within flow
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (currentStep > 1) {
                    slideBackward()
                } else {
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        })
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
        tvSelectedLocationCoords = view.findViewById(R.id.tvSelectedLocationCoords)
        cardMapPinPreview = view.findViewById(R.id.cardMapPinPreview)

        // Step 4
        cardUploadDropzone = view.findViewById(R.id.cardUploadDropzone)
        tvPhotosCountStatus = view.findViewById(R.id.tvPhotosCountStatus)
        rowThumbnails = view.findViewById(R.id.rowThumbnails)
        imgThumbnail1 = view.findViewById(R.id.imgThumbnail1)

        // Step 5
        badgeReviewStatus = view.findViewById(R.id.badgeReviewStatus)
        tvReviewCategory = view.findViewById(R.id.tvReviewCategory)
        tvReviewTitle = view.findViewById(R.id.tvReviewTitle)
        tvReviewLocation = view.findViewById(R.id.tvReviewLocation)
        tvReviewDesc = view.findViewById(R.id.tvReviewDesc)

        // Upload Progress UI
        cardUploadProgress = view.findViewById(R.id.cardUploadProgress)
        tvUploadStatusText = view.findViewById(R.id.tvUploadStatusText)
        tvUploadPercentage = view.findViewById(R.id.tvUploadPercentage)
        pbUploadBar = view.findViewById(R.id.pbUploadBar)
        layoutUploadError = view.findViewById(R.id.layoutUploadError)
        tvErrorMessage = view.findViewById(R.id.tvErrorMessage)
        btnRetryUpload = view.findViewById(R.id.btnRetryUpload)
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
        // Back navigation
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
            if (validateCurrentStep()) {
                if (currentStep < 5) {
                    slideForward()
                } else {
                    submitReport()
                }
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

        // Step 3: Area chips & Coordinates
        val areaChips = listOf(chipAreaColombo3, chipAreaKollupitiya, chipAreaBamba, chipAreaFort)
        for (chip in areaChips) {
            chip.setOnClickListener {
                for (c in areaChips) {
                    if (c == chip) {
                        c.setBackgroundResource(R.drawable.bg_chip_selected)
                        c.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                        selectedArea = c.text.toString()
                        when (selectedArea) {
                            "Colombo 03" -> { selectedLatitude = 6.9042; selectedLongitude = 79.8530 }
                            "Kollupitiya" -> { selectedLatitude = 6.9128; selectedLongitude = 79.8507 }
                            "Bambalapitiya" -> { selectedLatitude = 6.8920; selectedLongitude = 79.8550 }
                            "Fort" -> { selectedLatitude = 6.9360; selectedLongitude = 79.8450 }
                        }
                        tvSelectedLocationPreview.text = "$selectedArea, Western Province"
                        tvSelectedLocationCoords.text = String.format(java.util.Locale.US, "%.4f° N, %.4f° E · Precision 15m", selectedLatitude, selectedLongitude)
                    } else {
                        c.setBackgroundResource(R.drawable.bg_chip_unselected)
                        c.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
                    }
                }
            }
        }

        // Step 3: Map card click - open Leaflet map in picker mode
        cardMapPinPreview.setOnClickListener {
            if (::inputTitle.isInitialized) savedTitle = inputTitle.text.toString()
            if (::inputDescription.isInitialized) savedDescription = inputDescription.text.toString()
            if (::inputLandmark.isInitialized) savedLandmark = inputLandmark.text.toString()

            parentFragmentManager.beginTransaction()
                .replace(R.id.screenBox, MapFragment.newInstance(isPickerMode = true))
                .addToBackStack(null)
                .commit()
        }

        // Step 4: Photo dropzone - choose Camera or Gallery
        cardUploadDropzone.setOnClickListener {
            showImageSourceChooser()
        }

        // Retry Upload Action
        btnRetryUpload.setOnClickListener {
            submitReport()
        }
    }

    private fun showImageSourceChooser() {
        val options = arrayOf("Choose from Gallery", "Take Photo with Camera")
        AlertDialog.Builder(requireContext())
            .setTitle("Add Item Photo")
            .setItems(options) { _, which ->
                if (which == 0) {
                    pickImageLauncher.launch("image/*")
                } else {
                    if (ContextCompat.checkSelfPermission(
                            requireContext(),
                            android.Manifest.permission.CAMERA
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    ) {
                        takePictureLauncher.launch(null)
                    } else {
                        requestCameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                    }
                }
            }
            .show()
    }

    private fun saveBitmapToTempUri(bitmap: Bitmap): Uri? {
        return try {
            val cachePath = File(requireContext().cacheDir, "images")
            cachePath.mkdirs()
            val file = File(cachePath, "captured_${System.currentTimeMillis()}.jpg")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            stream.close()
            Uri.fromFile(file)
        } catch (e: Exception) {
            null
        }
    }

    private fun validateCurrentStep(): Boolean {
        if (currentStep == 2) {
            val title = inputTitle.text.toString().trim()
            if (title.isEmpty()) {
                inputTitle.error = "Please enter an item title"
                inputTitle.requestFocus()
                return false
            }
        }
        return true
    }

    private fun slideForward() {
        if (currentStep == 2) {
            if (::inputTitle.isInitialized) savedTitle = inputTitle.text.toString()
            if (::inputDescription.isInitialized) savedDescription = inputDescription.text.toString()
        } else if (currentStep == 3) {
            if (::inputLandmark.isInitialized) savedLandmark = inputLandmark.text.toString()
        }

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
        if (currentStep == 2) {
            if (::inputTitle.isInitialized) savedTitle = inputTitle.text.toString()
            if (::inputDescription.isInitialized) savedDescription = inputDescription.text.toString()
        } else if (currentStep == 3) {
            if (::inputLandmark.isInitialized) savedLandmark = inputLandmark.text.toString()
        }

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
                badgeReviewStatus.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        if (isLostSelected) R.color.tag_lost_color else R.color.tag_found_color
                    )
                )
                tvReviewCategory.text = selectedCategory
                tvReviewLocation.text = "$selectedArea ${if (landmark.isNotEmpty()) "· $landmark" else ""} · Reported $selectedDate"
                tvReviewDesc.text = if (desc.isNotEmpty()) desc else "No additional description provided."
            }
        }
    }

    private fun submitReport() {
        val title = inputTitle.text.toString().trim()
        val desc = inputDescription.text.toString().trim()
        val landmark = inputLandmark.text.toString().trim()
        val reportTitle = if (title.isNotEmpty()) title else if (isLostSelected) "Lost Item" else "Found Item"

        val uid = FirebaseManager.currentUid

        val fullDescription = buildString {
            if (desc.isNotEmpty()) append(desc)
            if (landmark.isNotEmpty()) {
                if (isNotEmpty()) append("\nLandmark: ")
                append(landmark)
            }
        }

        val reportLocation = "$selectedArea, Western Province"

        val report = ItemReport(
            userId = uid,
            title = reportTitle,
            description = fullDescription,
            category = selectedCategory,
            status = if (isLostSelected) "LOST" else "FOUND",
            location = reportLocation,
            latitude = selectedLatitude,
            longitude = selectedLongitude,
            timestamp = System.currentTimeMillis()
        )

        // Show Progress Bar (0-100%)
        cardUploadProgress.visibility = View.VISIBLE
        layoutUploadError.visibility = View.GONE
        pbUploadBar.progress = 0
        tvUploadPercentage.text = "0%"
        tvUploadStatusText.text = "Compressing photo & preparing upload..."

        btnContinue.isEnabled = false
        btnContinue.text = "Publishing..."

        uploadJob?.cancel()
        uploadJob = viewLifecycleOwner.lifecycleScope.launch {
            val result = reportRepository.uploadImageAndCreateReport(
                context = requireContext(),
                imageUri = selectedImageUri,
                report = report,
                maxRetries = 3
            ) { progress ->
                pbUploadBar.progress = progress
                tvUploadPercentage.text = "$progress%"
                tvUploadStatusText.text = when {
                    progress < 25 -> "Compressing image..."
                    progress < 90 -> "Uploading photo to Storage ($progress%)..."
                    progress < 100 -> "Saving report to Realtime Database..."
                    else -> "Done! Finalizing report..."
                }
                btnContinue.text = "Publishing $progress%"
            }

            result.onSuccess { createdReport ->
                pbUploadBar.progress = 100
                tvUploadPercentage.text = "100%"
                tvUploadStatusText.text = "Successfully published"

                if (isLostSelected) {
                    com.group3.lostandfound.service.FCMService.triggerPushNotification(
                        requireContext(),
                        "Lost Item Broadcasted",
                        "Search alert active for '${createdReport.title}'. Nearby community members notified.",
                        createdReport.itemId
                    )
                } else {
                    com.group3.lostandfound.service.FCMService.triggerPushNotification(
                        requireContext(),
                        "Found Item Registered",
                        "Thank you for reporting '${createdReport.title}'. Searching for the verified owner.",
                        createdReport.itemId
                    )
                }

                com.group3.lostandfound.util.ModernToast.show(
                    requireContext(),
                    "Report published successfully. Redirecting to My Reports.",
                    view
                )

                // Redirect user directly to My Reports (ReportsFragment) as requested
                parentFragmentManager.beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                    .replace(R.id.screenBox, ReportsFragment.newInstance())
                    .commit()
            }.onFailure { error ->
                layoutUploadError.visibility = View.VISIBLE
                tvErrorMessage.text = "Upload error: ${error.localizedMessage ?: "Network error"}. Check connection and retry."
                tvUploadStatusText.text = "Upload interrupted."

                btnContinue.isEnabled = true
                btnContinue.text = "Publish Report"

                Toast.makeText(
                    requireContext(),
                    "Upload failed: ${error.message}. You can retry.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onDestroyView() {
        if (::inputTitle.isInitialized) {
            savedTitle = inputTitle.text.toString()
        }
        if (::inputDescription.isInitialized) {
            savedDescription = inputDescription.text.toString()
        }
        if (::inputLandmark.isInitialized) {
            savedLandmark = inputLandmark.text.toString()
        }
        super.onDestroyView()
    }

    companion object {
        private const val ARG_IS_LOST = "arg_is_lost"
        private const val ARG_LAT = "arg_lat"
        private const val ARG_LNG = "arg_lng"
        private const val ARG_LOC_NAME = "arg_loc_name"

        fun newInstance(
            isLost: Boolean = true,
            latitude: Double = 6.9042,
            longitude: Double = 79.8530,
            locationName: String? = null
        ): CreateReportFragment {
            val fragment = CreateReportFragment()
            val args = Bundle().apply {
                putBoolean(ARG_IS_LOST, isLost)
                putDouble(ARG_LAT, latitude)
                putDouble(ARG_LNG, longitude)
                if (locationName != null) {
                    putString(ARG_LOC_NAME, locationName)
                }
            }
            fragment.arguments = args
            return fragment
        }
    }
}
