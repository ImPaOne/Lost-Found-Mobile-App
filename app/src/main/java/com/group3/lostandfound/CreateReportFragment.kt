package com.group3.lostandfound

import com.group3.lostandfound.R
import com.group3.lostandfound.databinding.FragmentCreateReportBinding
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.DecelerateInterpolator
import android.view.animation.TranslateAnimation
import android.widget.CheckBox
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
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth
import com.group3.lostandfound.model.ItemReport
import com.group3.lostandfound.viewmodel.CreateReportViewModel
import com.group3.lostandfound.viewmodel.ReportUploadState
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

    // Holds references to XML views using ViewBinding.
    private var _binding: FragmentCreateReportBinding? = null
    private val binding get() = _binding!!

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
    private var imgRadioLost: ImageView? = null
    private var imgRadioFound: ImageView? = null
    private var tvNextStepHint: TextView? = null

    // Step 2 Views
    private lateinit var boxCategory: View
    private lateinit var tvCategoryValue: TextView
    private lateinit var inputTitle: EditText
    private lateinit var inputDescription: EditText
    private lateinit var badgeStep2Type: TextView
    private lateinit var tvStep2SummaryTitle: TextView
    private lateinit var tvStep2SummarySubtitle: TextView
    private lateinit var btnEditStep2Summary: TextView
    private lateinit var chipCategoryPersonal: TextView
    private lateinit var chipCategoryElectronics: TextView
    private lateinit var chipCategoryDocuments: TextView
    private lateinit var chipCategoryKeys: TextView
    private lateinit var chipCategoryWallets: TextView
    private lateinit var chipCategoryOther: TextView

    // Step 3 Views
    private lateinit var mapCard: View
    private lateinit var searchInput: EditText
    private lateinit var btnClearSearch: View
    private lateinit var btnGps: View
    private lateinit var inputLocation: EditText
    private lateinit var lblDate: TextView
    private lateinit var boxDate: View
    private lateinit var tvDate: TextView
    private lateinit var lblTime: TextView
    private lateinit var boxTime: View
    private lateinit var tvTime: TextView
    private var selectedDateFormatted = "12 Sep"
    private var selectedTimeFormatted = "6:30 PM"

    // Step 4 Views
    private lateinit var cardPhotoPreview: View
    private lateinit var imgPhotoPreview: ImageView
    private lateinit var btnRemovePhoto: View
    private lateinit var badgeCoverPhoto: TextView
    private lateinit var btnTakePhoto: View
    private lateinit var btnGallery: View
    private lateinit var tvPhotosCountStatus: TextView

    // Step 5 Review Views
    private lateinit var badgeReviewStatus: TextView
    private lateinit var btnEditSummary: View
    private lateinit var tvReviewTitle: TextView
    private lateinit var tvReviewCategory: TextView
    private lateinit var tvReviewDesc: TextView
    private lateinit var imgReviewThumbnail: ImageView
    private lateinit var tvReviewLocation: TextView
    private lateinit var tvReviewDateTime: TextView
    private lateinit var btnEditLocation: View
    private lateinit var tvPrivacyHiddenNote: TextView
    private lateinit var switchMessages: androidx.appcompat.widget.SwitchCompat
    private lateinit var cbConfirmAccuracy: CheckBox

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
    private var selectedCategory = "Personal items"
    private var selectedArea = "Galle"
    private var selectedLatitude = 6.0367
    private var selectedLongitude = 80.2170
    private var photosCount = 0
    private var selectedImageUri: Uri? = null
    private val attachedImagesList = ArrayList<Uri>()
    private var tempCameraUri: Uri? = null

    // Persisted form inputs across fragment backstack/step navigation
    private var savedTitle: String = ""
    private var savedDescription: String = ""
    private var savedLandmark: String = ""

    private lateinit var createReportViewModel: CreateReportViewModel
    private var uploadJob: Job? = null

    //copies a selected image in to the apps cache so the app can access it.
    private fun copyUriToInternalStorage(uri: Uri): Uri? {
        return try {
            val dir = File(requireContext().cacheDir, "report_images")
            if (!dir.exists()) dir.mkdirs()
            val destFile = File(dir, "report_${System.currentTimeMillis()}.jpg")
            requireContext().contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            Uri.fromFile(destFile)
        } catch (e: Exception) {
            android.util.Log.e("CreateReportFragment", "Error caching image locally", e)
            uri
        }
    }

    //Adds a photo to the selected list, sets the primary photo, and moves to Step 4.
    private fun addImageToSelection(uri: Uri) {
        if (!attachedImagesList.contains(uri)) {
            attachedImagesList.add(uri)
        }
        selectedImageUri = attachedImagesList.firstOrNull()
        photosCount = attachedImagesList.size
        currentStep = 4
        if (::flipperSteps.isInitialized) {
            flipperSteps.displayedChild = 3
        }
        updatePhotoUI()
        updateStepHeader()
    }

    //Displays selected photos and updates the photo count.
    private fun updatePhotoUI() {
        if (!::tvPhotosCountStatus.isInitialized) return
        val count = attachedImagesList.size
        tvPhotosCountStatus.text = "$count photo${if (count == 1) "" else "s"} added"

        if (selectedImageUri != null && ::imgPhotoPreview.isInitialized) {
            com.group3.lostandfound.util.ImageLoader.load(requireContext(), selectedImageUri.toString(), imgPhotoPreview)
            btnRemovePhoto.visibility = View.VISIBLE
            badgeCoverPhoto.visibility = View.VISIBLE
        } else if (::imgPhotoPreview.isInitialized) {
            imgPhotoPreview.setImageResource(R.drawable.ic_item_placeholder)
            btnRemovePhoto.visibility = View.GONE
            badgeCoverPhoto.visibility = View.GONE
        }

        if (::imgReviewThumbnail.isInitialized) {
            if (attachedImagesList.isNotEmpty()) {
                com.group3.lostandfound.util.ImageLoader.load(requireContext(), attachedImagesList[0].toString(), imgReviewThumbnail)
            } else {
                imgReviewThumbnail.setImageResource(R.drawable.ic_item_placeholder)
            }
        }
    }

    // Gallery Picker Launcher
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val persistentUri = copyUriToInternalStorage(uri) ?: uri
            addImageToSelection(persistentUri)
            Toast.makeText(requireContext(), "Photo selected from gallery", Toast.LENGTH_SHORT).show()
        }
    }

    // Camera Permission Launcher
    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchCamera()
        } else {
            Toast.makeText(
                requireContext(),
                "Camera permission is required to capture photos",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Camera Capture Launcher for Real High Quality photo
    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && tempCameraUri != null) {
            addImageToSelection(tempCameraUri!!)
            Toast.makeText(requireContext(), "Camera photo captured successfully", Toast.LENGTH_SHORT).show()
        }
    }

    //Creates a destination for the photo and launches the camera app.
    private fun launchCamera() {
        try {
            val cacheImagesDir = File(requireContext().cacheDir, "images")
            if (!cacheImagesDir.exists()) cacheImagesDir.mkdirs()
            val photoFile = File(cacheImagesDir, "captured_${System.currentTimeMillis()}.jpg")
            val authority = "${requireContext().packageName}.fileprovider"
            val uri = androidx.core.content.FileProvider.getUriForFile(requireContext(), authority, photoFile)
            tempCameraUri = uri
            takePictureLauncher.launch(uri)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Camera launch error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    //Registers a listener to receive the location selected on the map.
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
                    if (::inputLocation.isInitialized) {
                        inputLocation.setText("Near $selectedArea")
                    }
                    if (::searchInput.isInitialized) {
                        searchInput.setText(selectedArea)
                    }
                }
            }
        }
    }

    //Inflates fragment_create_report.xml to create the screen's UI.
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateReportBinding.inflate(inflater, container, false)
        return binding.root
    }

    //Initializes the ViewModel and UI, restores form state, and registers listeners.
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireContext().getSharedPreferences("findit_prefs", android.content.Context.MODE_PRIVATE)
        val isAdmin = prefs.getBoolean("is_admin", false) || FirebaseManager.currentUserEmail.equals("admin@gmail.com", ignoreCase = true)
        if (isAdmin) {
            parentFragmentManager.beginTransaction()
                .replace(R.id.screenBox, AnalyticsFragment.newInstance())
                .commit()
            return
        }

        createReportViewModel = ViewModelProvider(this)[CreateReportViewModel::class.java]

        initViews()
        observeViewModel()

        // Restore state if process/fragment was killed or rotated
        if (savedInstanceState != null) {
            currentStep = savedInstanceState.getInt(KEY_SAVED_STEP, currentStep)
            isLostSelected = savedInstanceState.getBoolean(KEY_SAVED_IS_LOST, isLostSelected)
            selectedDate = savedInstanceState.getString(KEY_SAVED_DATE, selectedDate) ?: selectedDate
            selectedCategory = savedInstanceState.getString(KEY_SAVED_CATEGORY, selectedCategory) ?: selectedCategory
            selectedArea = savedInstanceState.getString(KEY_SAVED_AREA, selectedArea) ?: selectedArea
            selectedLatitude = savedInstanceState.getDouble(KEY_SAVED_LAT, selectedLatitude)
            selectedLongitude = savedInstanceState.getDouble(KEY_SAVED_LNG, selectedLongitude)
            val uriStr = savedInstanceState.getString(KEY_SAVED_IMAGE_URI)
            if (!uriStr.isNullOrEmpty()) {
                selectedImageUri = Uri.parse(uriStr)
            }
            photosCount = savedInstanceState.getInt(KEY_SAVED_PHOTOS_COUNT, photosCount)
            savedTitle = savedInstanceState.getString(KEY_SAVED_TITLE, savedTitle) ?: savedTitle
            savedDescription = savedInstanceState.getString(KEY_SAVED_DESC, savedDescription) ?: savedDescription
            savedLandmark = savedInstanceState.getString(KEY_SAVED_LANDMARK, savedLandmark) ?: savedLandmark
        } else if (currentStep == 1 && savedTitle.isEmpty() && savedDescription.isEmpty()) {
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
        if (selectedArea.isNotEmpty()) {
            inputLocation.setText("Near $selectedArea, Colombo 03")
            searchInput.setText("$selectedArea, Colombo 03")
        }

        // Restore input texts if previously entered
        if (savedTitle.isNotEmpty()) inputTitle.setText(savedTitle)
        if (savedDescription.isNotEmpty()) inputDescription.setText(savedDescription)
        if (savedLandmark.isNotEmpty()) {
            inputLocation.setText(savedLandmark)
            searchInput.setText(savedLandmark)
        }

        // Restore photo thumbnail if selected
        if (selectedImageUri != null) {
            com.group3.lostandfound.util.ImageLoader.load(requireContext(), selectedImageUri.toString(), imgPhotoPreview)
            tvPhotosCountStatus.text = "$photosCount photo${if (photosCount == 1) "" else "s"} added"
        }

        // Initialize report type UI
        setItemType(isLostSelected)

        // Restore ViewFlipper step without animation
        flipperSteps.displayedChild = (currentStep - 1).coerceIn(0, 4)

        setupStepListeners()
        updateStepHeader()
        setupBottomNav(view, "post")
        setupStep3Map()

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

    //	Connects Kotlin variables to UI components using ViewBinding.
    private fun initViews() {
        btnBack = binding.btnBack
        btnBackBottom = binding.btnBackBottom
        btnContinue = binding.btnContinue

        segStep1 = binding.segStep1
        segStep2 = binding.segStep2
        segStep3 = binding.segStep3
        segStep4 = binding.segStep4
        segStep5 = binding.segStep5

        tvStepNumber = binding.tvStepNumber
        tvStepTitle = binding.tvStepTitle
        tvStepSubtitle = binding.tvStepSubtitle

        flipperSteps = binding.flipperSteps

        // Step 1
        cardLostItem = binding.cardLostItem
        cardFoundItem = binding.cardFoundItem
        iconLost = binding.iconLost
        iconFound = binding.iconFound
        imgRadioLost = binding.imgRadioLost
        imgRadioFound = binding.imgRadioFound
        tvNextStepHint = binding.tvNextStepHint

        // Step 2
        boxCategory = binding.boxCategory
        tvCategoryValue = binding.tvCategoryValue
        inputTitle = binding.inputTitle
        inputDescription = binding.inputDescription
        badgeStep2Type = binding.badgeStep2Type
        tvStep2SummaryTitle = binding.tvStep2SummaryTitle
        tvStep2SummarySubtitle = binding.tvStep2SummarySubtitle
        btnEditStep2Summary = binding.btnEditStep2Summary
        chipCategoryPersonal = binding.chipCategoryPersonal
        chipCategoryElectronics = binding.chipCategoryElectronics
        chipCategoryDocuments = binding.chipCategoryDocuments
        chipCategoryKeys = binding.chipCategoryKeys
        chipCategoryWallets = binding.chipCategoryWallets
        chipCategoryOther = binding.chipCategoryOther

        // Step 3
        mapCard = binding.mapCard
        searchInput = binding.searchInput
        btnClearSearch = binding.btnClearSearch
        btnGps = binding.btnGps
        inputLocation = binding.inputLocation
        lblDate = binding.lblDate
        boxDate = binding.boxDate
        tvDate = binding.tvDate
        lblTime = binding.lblTime
        boxTime = binding.boxTime
        tvTime = binding.tvTime

        // Step 4
        cardPhotoPreview = binding.cardPhotoPreview
        imgPhotoPreview = binding.imgPhotoPreview
        btnRemovePhoto = binding.btnRemovePhoto
        badgeCoverPhoto = binding.badgeCoverPhoto
        btnTakePhoto = binding.btnTakePhoto
        btnGallery = binding.btnGallery
        tvPhotosCountStatus = binding.tvPhotosCountStatus

        // Step 5 Review Views
        badgeReviewStatus = binding.badgeReviewStatus
        btnEditSummary = binding.btnEditSummary
        tvReviewTitle = binding.tvReviewTitle
        tvReviewCategory = binding.tvReviewCategory
        tvReviewDesc = binding.tvReviewDesc
        imgReviewThumbnail = binding.imgReviewThumbnail
        tvReviewLocation = binding.tvReviewLocation
        tvReviewDateTime = binding.tvReviewDateTime
        btnEditLocation = binding.btnEditLocation
        tvPrivacyHiddenNote = binding.tvPrivacyHiddenNote
        switchMessages = binding.switchMessages
        cbConfirmAccuracy = binding.cbConfirmAccuracy

        // Upload Progress UI
        cardUploadProgress = binding.cardUploadProgress
        tvUploadStatusText = binding.tvUploadStatusText
        tvUploadPercentage = binding.tvUploadPercentage
        pbUploadBar = binding.pbUploadBar
        layoutUploadError = binding.layoutUploadError
        tvErrorMessage = binding.tvErrorMessage
        btnRetryUpload = binding.btnRetryUpload
    }

    //	Updates the UI when the user selects LOST or FOUND.
    private fun setItemType(isLost: Boolean) {
        isLostSelected = isLost
        if (isLost) {
            cardLostItem.setBackgroundResource(R.drawable.bg_card_create_selected_lost)
            imgRadioLost?.setImageResource(R.drawable.ic_radio_checked_red)

            cardFoundItem.setBackgroundResource(R.drawable.bg_card_create_unselected)
            imgRadioFound?.setImageResource(R.drawable.ic_radio_unchecked_grey)
        } else {
            cardFoundItem.setBackgroundResource(R.drawable.bg_card_create_selected_found)
            imgRadioFound?.setImageResource(R.drawable.ic_radio_checked_red)

            cardLostItem.setBackgroundResource(R.drawable.bg_card_create_unselected)
            imgRadioLost?.setImageResource(R.drawable.ic_radio_unchecked_grey)
        }
    }

    //Attaches click listeners to buttons, categories, locations, and photo controls.
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

        // Real-time error clearing & dynamic title preview
        inputTitle.doAfterTextChanged {
            inputTitle.error = null
            if (::tvStep2SummaryTitle.isInitialized) {
                val currentText = it?.toString()?.trim() ?: ""
                tvStep2SummaryTitle.text = if (currentText.isNotEmpty()) currentText else "Black leather wallet"
            }
        }
        inputDescription.doAfterTextChanged { inputDescription.error = null }
        inputLocation.doAfterTextChanged { inputLocation.error = null }

        // Continue / Submit button
        btnContinue.setOnClickListener {
            if (validateCurrentStep()) {
                if (currentStep < 5) {
                    slideForward()
                } else {
                    if (cbConfirmAccuracy.isChecked) {
                        submitReport()
                    } else {
                        Toast.makeText(requireContext(), "Please confirm details accuracy before submitting", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // Step 1: Type Selection
        cardLostItem.setOnClickListener { setItemType(true) }
        cardFoundItem.setOnClickListener { setItemType(false) }

        // Step 2: Edit Summary button (returns to Step 1)
        btnEditStep2Summary.setOnClickListener { jumpToStep(1) }

        // Category chips selection
        fun updateCategoryChipSelection(categoryName: String) {
            selectedCategory = categoryName
            tvCategoryValue.text = categoryName

            val chipsList = listOf(
                chipCategoryPersonal to "Personal items",
                chipCategoryElectronics to "Electronics",
                chipCategoryDocuments to "Documents",
                chipCategoryKeys to "Keys & Cards",
                chipCategoryWallets to "Wallets & Bags",
                chipCategoryOther to "Other"
            )

            for ((chip, name) in chipsList) {
                val isSelected = name.equals(categoryName, ignoreCase = true)
                chip.setBackgroundResource(if (isSelected) R.drawable.bg_chip_selected else R.drawable.bg_chip_unselected)
                chip.setTextColor(if (isSelected) ContextCompat.getColor(requireContext(), R.color.white) else android.graphics.Color.parseColor("#64748B"))
            }

            if (::tvStep2SummarySubtitle.isInitialized) {
                val areaName = if (selectedArea.isNotEmpty()) selectedArea else "Colombo"
                tvStep2SummarySubtitle.text = "$selectedCategory · $areaName"
            }
        }

        chipCategoryPersonal.setOnClickListener { updateCategoryChipSelection("Personal items") }
        chipCategoryElectronics.setOnClickListener { updateCategoryChipSelection("Electronics") }
        chipCategoryDocuments.setOnClickListener { updateCategoryChipSelection("Documents") }
        chipCategoryKeys.setOnClickListener { updateCategoryChipSelection("Keys & Cards") }
        chipCategoryWallets.setOnClickListener { updateCategoryChipSelection("Wallets & Bags") }
        chipCategoryOther.setOnClickListener { updateCategoryChipSelection("Other") }

        updateCategoryChipSelection(selectedCategory)

        // Step 2: Category dialog fallback
        boxCategory.setOnClickListener {
            val categories = arrayOf(
                "Personal items",
                "Electronics",
                "Documents",
                "Keys & Cards",
                "Wallets & Bags",
                "Other"
            )
            AlertDialog.Builder(requireContext())
                .setTitle("Select Category")
                .setItems(categories) { _, which ->
                    updateCategoryChipSelection(categories[which])
                }
                .show()
        }

        // Step 3: Map card, GPS, Search, Enlarge, and Date/Time pickers
        val openEnlargedMapAction = View.OnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(
                    R.id.screenBox,
                    MapFragment.newInstance(
                        isPickerMode = true,
                        targetLat = selectedLatitude,
                        targetLng = selectedLongitude,
                        targetItemId = ""
                    )
                )
                .addToBackStack(null)
                .commit()
        }
        binding.btnEnlargeMap.setOnClickListener(openEnlargedMapAction)
        binding.pillMapHint.setOnClickListener(openEnlargedMapAction)

        btnGps.setOnClickListener {
            val lat = 6.0367
            val lng = 80.2170
            selectedLatitude = lat
            selectedLongitude = lng
            selectedArea = "Galle"
            inputLocation.setText("Near $selectedArea, Southern Province")
            searchInput.setText("$selectedArea, Southern Province")
            binding.webViewStep3Map.evaluateJavascript("if (typeof updateCenter === 'function') { updateCenter($lat, $lng); }", null)
            Toast.makeText(requireContext(), "Centered on GPS location (Galle: 6.0367° N, 80.2170° E)", Toast.LENGTH_SHORT).show()
        }

        btnClearSearch.setOnClickListener {
            searchInput.text?.clear()
        }

        boxDate.setOnClickListener {
            showDatePicker()
        }

        boxTime.setOnClickListener {
            showTimePicker()
        }

        // Step 4: Photo Actions
        btnTakePhoto.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    android.Manifest.permission.CAMERA
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                launchCamera()
            } else {
                requestCameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
            }
        }

        btnGallery.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        btnRemovePhoto.setOnClickListener {
            attachedImagesList.clear()
            selectedImageUri = null
            photosCount = 0
            imgPhotoPreview.setImageDrawable(null)
            tvPhotosCountStatus.text = "0 photos added"
            badgeCoverPhoto.visibility = View.GONE
            btnRemovePhoto.visibility = View.GONE
        }

        // Step 5: Edit Actions
        btnEditSummary.setOnClickListener {
            jumpToStep(2)
        }

        btnEditLocation.setOnClickListener {
            jumpToStep(3)
        }

        // Retry Upload Action
        btnRetryUpload.setOnClickListener {
            submitReport()
        }
    }

    private fun setupStep3Map() {
        val webView = binding.webViewStep3Map
        val webSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.databaseEnabled = true
        webSettings.allowFileAccess = true
        webSettings.allowContentAccess = true
        webSettings.useWideViewPort = true
        webSettings.loadWithOverviewMode = true

        webView.webChromeClient = android.webkit.WebChromeClient()
        webView.webViewClient = object : android.webkit.WebViewClient() {
            override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                super.onPageFinished(view, url)
                webView.postDelayed({
                    webView.evaluateJavascript("if (typeof map !== 'undefined') { map.invalidateSize(); }", null)
                }, 300)
            }
        }

        webView.setOnTouchListener { v, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN, android.view.MotionEvent.ACTION_MOVE -> {
                    v.parent?.requestDisallowInterceptTouchEvent(true)
                }
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                    v.parent?.requestDisallowInterceptTouchEvent(false)
                }
            }
            false
        }

        // Connects the JavaScript map to Android Kotlin.
        // Receives the selected latitude and longitude, updates the location variables,
        webView.addJavascriptInterface(object {
            @android.webkit.JavascriptInterface
            fun onLocationChanged(lat: Double, lng: Double) {
                activity?.runOnUiThread {
                    selectedLatitude = lat
                    selectedLongitude = lng
                    selectedArea = String.format(java.util.Locale.US, "%.4f, %.4f", lat, lng)
                    inputLocation.setText("Selected coordinates: $selectedArea")
                    searchInput.setText(selectedArea)
                }
            }
        }, "AndroidBridge")

        loadStep3MapHtml(selectedLatitude, selectedLongitude)
    }

    private fun loadStep3MapHtml(lat: Double, lng: Double) {
        val pinColor = if (isLostSelected) "#DC2626" else "#059669"
        val html = """
<!DOCTYPE html>
<html>
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
    <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
    <style>
        html, body, #map {
            width: 100%;
            height: 100%;
            margin: 0;
            padding: 0;
            background: #e2e8f0;
        }
        .leaflet-control-attribution {
            display: none !important;
        }
    </style>
</head>
<body>
    <div id="map"></div>
    <script>
        var map = L.map('map', {
            zoomControl: true,
            dragging: true,
            touchZoom: true,
            scrollWheelZoom: true
        }).setView([$lat, $lng], 15);

        L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
            maxZoom: 19
        }).addTo(map);

        var pinSvg = '<svg width="32" height="42" viewBox="0 0 28 38" fill="none" xmlns="http://www.w3.org/2000/svg" style="filter: drop-shadow(0 3px 6px rgba(0,0,0,0.4));">' +
            '<path d="M14 0C6.268 0 0 6.268 0 14C0 24.5 14 38 14 38C14 38 28 24.5 28 14C28 6.268 21.732 0 14 0Z" fill="$pinColor"/>' +
            '<circle cx="14" cy="14" r="5" fill="#FFFFFF"/>' +
            '</svg>';

        var pinIcon = L.divIcon({
            className: '',
            html: pinSvg,
            iconSize: [32, 42],
            iconAnchor: [16, 42]
        });

        var marker = L.marker([$lat, $lng], { icon: pinIcon, draggable: true }).addTo(map);

        marker.on('dragend', function(e) {
            var position = marker.getLatLng();
            if (window.AndroidBridge) {
                window.AndroidBridge.onLocationChanged(position.lat, position.lng);
            }
        });

        map.on('click', function(e) {
            marker.setLatLng(e.latlng);
            if (window.AndroidBridge) {
                window.AndroidBridge.onLocationChanged(e.latlng.lat, e.latlng.lng);
            }
        });

        function updateCenter(newLat, newLng) {
            map.setView([newLat, newLng], 15);
            marker.setLatLng([newLat, newLng]);
        }

        setTimeout(function() {
            map.invalidateSize();
        }, 400);
    </script>
</body>
</html>
        """.trimIndent()
        binding.webViewStep3Map.loadDataWithBaseURL("https://unpkg.com", html, "text/html", "UTF-8", null)
    }

    private fun jumpToStep(targetStep: Int) {
        if (targetStep in 1..5 && targetStep != currentStep) {
            currentStep = targetStep
            flipperSteps.displayedChild = targetStep - 1
            updateStepHeader()
        }
    }

    private fun showDatePicker() {
        val calendar = java.util.Calendar.getInstance()
        android.app.DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val cal = java.util.Calendar.getInstance()
                cal.set(year, month, dayOfMonth)
                val format = java.text.SimpleDateFormat("d MMM", java.util.Locale.US)
                selectedDateFormatted = format.format(cal.time)
                tvDate.text = selectedDateFormatted
                selectedDate = selectedDateFormatted
            },
            calendar.get(java.util.Calendar.YEAR),
            calendar.get(java.util.Calendar.MONTH),
            calendar.get(java.util.Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showTimePicker() {
        val calendar = java.util.Calendar.getInstance()
        android.app.TimePickerDialog(
            requireContext(),
            { _, hourOfDay, minute ->
                val cal = java.util.Calendar.getInstance()
                cal.set(java.util.Calendar.HOUR_OF_DAY, hourOfDay)
                cal.set(java.util.Calendar.MINUTE, minute)
                val format = java.text.SimpleDateFormat("h:mm a", java.util.Locale.US)
                selectedTimeFormatted = format.format(cal.time)
                tvTime.text = selectedTimeFormatted
            },
            calendar.get(java.util.Calendar.HOUR_OF_DAY),
            calendar.get(java.util.Calendar.MINUTE),
            false
        ).show()
    }

    //Lets the user choose between the gallery and camera.
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
                        launchCamera()
                    } else {
                        requestCameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                    }
                }
            }
            .show()
    }

    //	Saves a bitmap as a temporary JPEG file.
    private fun saveBitmapToTempUri(bitmap: Bitmap): Uri? {
        return try {
            val cachePath = File(requireContext().cacheDir, "images")
            cachePath.mkdirs()
            val file = File(cachePath, "captured_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                stream.flush()
            }
            Uri.fromFile(file)
        } catch (e: Exception) {
            null
        }
    }

    //Checks whether required input is valid before continuing.
    private fun validateCurrentStep(): Boolean {
        when (currentStep) {
            2 -> {
                val title = inputTitle.text.toString().trim()
                if (title.isEmpty()) {
                    inputTitle.error = "Item title is required"
                    inputTitle.requestFocus()
                    return false
                }
                if (title.length < 3) {
                    inputTitle.error = "Title must be at least 3 characters"
                    inputTitle.requestFocus()
                    return false
                }

                if (selectedCategory.isBlank()) {
                    Toast.makeText(requireContext(), "Please select an item category", Toast.LENGTH_SHORT).show()
                    return false
                }

                val desc = inputDescription.text.toString().trim()
                if (desc.isEmpty()) {
                    inputDescription.error = "Please provide an item description"
                    inputDescription.requestFocus()
                    return false
                }
                if (desc.length < 10) {
                    inputDescription.error = "Description must be at least 10 characters"
                    inputDescription.requestFocus()
                    return false
                }
            }
            3 -> {
                val loc = inputLocation.text.toString().trim()
                if (loc.isEmpty() && selectedArea.isBlank()) {
                    inputLocation.error = "Please enter a location or landmark"
                    inputLocation.requestFocus()
                    return false
                }
                if (loc.isNotEmpty() && loc.length < 3) {
                    inputLocation.error = "Location must be at least 3 characters"
                    inputLocation.requestFocus()
                    return false
                }
            }
        }
        return true
    }

    //	Animates navigation to the next form step and saves input.
    private fun slideForward() {
        if (currentStep == 2) {
            if (::inputTitle.isInitialized) savedTitle = inputTitle.text.toString().trim()
            if (::inputDescription.isInitialized) savedDescription = inputDescription.text.toString().trim()
        } else if (currentStep == 3) {
            if (::inputLocation.isInitialized) {
                val loc = inputLocation.text.toString().trim()
                savedLandmark = loc.ifEmpty { selectedArea }
            }
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

    //Animates navigation to the previous form step.
    private fun slideBackward() {
        if (currentStep == 2) {
            if (::inputTitle.isInitialized) savedTitle = inputTitle.text.toString()
            if (::inputDescription.isInitialized) savedDescription = inputDescription.text.toString()
        } else if (currentStep == 3) {
            if (::inputLocation.isInitialized) savedLandmark = inputLocation.text.toString()
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

    //Updates progress indicators, titles, buttons, and Step 5's review information.
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
                tvStepNumber.setBackgroundResource(android.R.color.transparent)
                tvStepNumber.setTextColor(android.graphics.Color.parseColor("#64748B"))
                tvStepTitle.text = "What happened?"
                tvStepSubtitle.text = "Choose a report type. We'll help you with the next steps."
                tvNextStepHint?.visibility = View.VISIBLE
                tvNextStepHint?.text = "➔ Next: Category & details"
                btnBackBottom.text = "Cancel"
                btnContinue.text = "Continue ➔"
            }
            2 -> {
                tvStepNumber.setBackgroundResource(android.R.color.transparent)
                tvStepNumber.setTextColor(android.graphics.Color.parseColor("#64748B"))
                tvStepTitle.text = "Category & details"
                tvStepSubtitle.text = "Tell the community what to look for."
                tvNextStepHint?.visibility = View.GONE
                btnBackBottom.text = "Back"
                btnContinue.text = "Continue"

                // Update Step 2 Summary Card
                if (::badgeStep2Type.isInitialized) {
                    if (isLostSelected) {
                        badgeStep2Type.text = "LOST"
                        badgeStep2Type.setBackgroundResource(R.drawable.bg_badge_lost_pill)
                        badgeStep2Type.setTextColor(android.graphics.Color.parseColor("#EF4444"))
                    } else {
                        badgeStep2Type.text = "FOUND"
                        badgeStep2Type.setBackgroundResource(R.drawable.bg_badge_found_pill)
                        badgeStep2Type.setTextColor(android.graphics.Color.parseColor("#16A34A"))
                    }
                }
                if (::tvStep2SummaryTitle.isInitialized) {
                    val currentTitle = inputTitle.text.toString().trim()
                    tvStep2SummaryTitle.text = if (currentTitle.isNotEmpty()) currentTitle else "Black leather wallet"
                }
                if (::tvStep2SummarySubtitle.isInitialized) {
                    val areaName = if (selectedArea.isNotEmpty()) selectedArea else "Colombo"
                    tvStep2SummarySubtitle.text = "$selectedCategory · $areaName"
                }
            }
            3 -> {
                tvStepNumber.setBackgroundResource(android.R.color.transparent)
                tvStepNumber.setTextColor(android.graphics.Color.parseColor("#64748B"))
                tvStepTitle.text = "Where & when?"
                val itemWord = savedTitle.trim().takeIf { it.isNotEmpty() }?.lowercase() ?: "wallet"
                tvStepSubtitle.text = if (isLostSelected) "Where did you last see your $itemWord?" else "Where did you find this item?"
                lblDate.text = if (isLostSelected) "Date lost" else "Date found"
                lblTime.text = if (isLostSelected) "Time lost" else "Time found"
                tvNextStepHint?.visibility = View.GONE
                btnBackBottom.text = "Back"
                btnContinue.text = "Continue"

                binding.webViewStep3Map.postDelayed({
                    binding.webViewStep3Map.evaluateJavascript("if (typeof map !== 'undefined') { map.invalidateSize(); }", null)
                }, 350)
            }
            4 -> {
                tvStepNumber.setBackgroundResource(android.R.color.transparent)
                tvStepNumber.setTextColor(android.graphics.Color.parseColor("#64748B"))
                tvStepTitle.text = "Add photos"
                tvStepSubtitle.text = "A clear photo makes a better match."
                tvNextStepHint?.visibility = View.GONE
                btnBackBottom.text = "Back"
                btnContinue.text = "Continue"
            }
            5 -> {
                tvStepNumber.setBackgroundResource(android.R.color.transparent)
                tvStepNumber.setTextColor(android.graphics.Color.parseColor("#64748B"))
                tvStepTitle.text = "Review & submit"
                tvStepSubtitle.text = "One last look before we start searching."
                tvNextStepHint?.visibility = View.GONE
                btnBackBottom.text = "Back"
                btnContinue.text = "Submit report"

                // Populate Step 5 Review Details
                val title = inputTitle.text.toString().trim()
                val landmark = if (::inputLocation.isInitialized) inputLocation.text.toString().trim() else savedLandmark
                val desc = inputDescription.text.toString().trim()

                tvReviewTitle.text = if (title.isNotEmpty()) title else if (isLostSelected) "Black leather wallet" else "Found Item"
                badgeReviewStatus.text = if (isLostSelected) "LOST" else "FOUND"
                badgeReviewStatus.setBackgroundResource(
                    if (isLostSelected) R.drawable.bg_badge_lost_pill else R.drawable.bg_badge_found_pill
                )
                badgeReviewStatus.setTextColor(
                    android.graphics.Color.parseColor(if (isLostSelected) "#E11D48" else "#15803D")
                )

                val photoCountText = if (attachedImagesList.isNotEmpty()) {
                    "${attachedImagesList.size} photo${if (attachedImagesList.size > 1) "s" else ""}"
                } else {
                    "1 photo"
                }
                tvReviewCategory.text = "${if (selectedCategory.isNotEmpty()) selectedCategory else "Personal items"} · $photoCountText"
                tvReviewDesc.text = if (desc.isNotEmpty()) desc else "Slim wallet with national ID and two bank cards. Small scratch near the clasp."
                tvReviewLocation.text = if (landmark.isNotEmpty()) landmark else "Near Duplication Road, Colombo 03"
                tvReviewDateTime.text = "$selectedDateFormatted · $selectedTimeFormatted"

                val prefs = requireContext().getSharedPreferences("findit_prefs", android.content.Context.MODE_PRIVATE)
                val userName = prefs.getString("user_name", "")?.takeIf { it.isNotBlank() } ?: "Nadeesha Silva"
                tvPrivacyHiddenNote.text = "Your phone and email stay hidden. Only your display name, $userName, is public."

                if (selectedImageUri != null) {
                    com.group3.lostandfound.util.ImageLoader.load(requireContext(), selectedImageUri.toString(), imgReviewThumbnail)
                } else {
                    imgReviewThumbnail.setImageResource(R.drawable.ic_item_placeholder)
                }
            }
        }
    }

    //	Creates the ItemReport object and asks the ViewModel to submit it.
    private fun submitReport() {
        val title = inputTitle.text.toString().trim()
        val desc = inputDescription.text.toString().trim()
        val landmark = if (::inputLocation.isInitialized) inputLocation.text.toString().trim() else savedLandmark
        val reportTitle = if (title.isNotEmpty()) title else if (isLostSelected) "Lost Item" else "Found Item"

        val prefs = requireContext().getSharedPreferences("findit_prefs", android.content.Context.MODE_PRIVATE)
        val localUid = prefs.getString("user_uid", "") ?: ""
        val uid = FirebaseManager.currentUid.ifEmpty { localUid }.ifEmpty { "user_${System.currentTimeMillis()}" }
        val authorName = prefs.getString("user_name", "")?.takeIf { it.isNotBlank() }
            ?: FirebaseManager.currentUserEmail.substringBefore("@").replaceFirstChar { it.uppercase() }.takeIf { it.isNotBlank() }
            ?: "Community Reporter"

        val fullDescription = buildString {
            if (desc.isNotEmpty()) append(desc)
            if (landmark.isNotEmpty()) {
                if (isNotEmpty()) append("\nLandmark: ")
                append(landmark)
            }
        }

        val reportLocation = if (landmark.isNotEmpty()) landmark else "$selectedArea, Western Province"

        val report = ItemReport(
            userId = uid,
            title = reportTitle,
            description = fullDescription,
            category = selectedCategory,
            status = if (isLostSelected) "LOST" else "FOUND",
            location = reportLocation,
            latitude = selectedLatitude,
            longitude = selectedLongitude,
            timestamp = System.currentTimeMillis(),
            authorName = authorName
        )

        createReportViewModel.submitReport(requireContext(), selectedImageUri, report)
    }

    //Reacts to upload progress, success, and errors.
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            createReportViewModel.uploadState.collect { state ->
                when (state) {
                    is ReportUploadState.Progress -> {
                        cardUploadProgress.visibility = View.VISIBLE
                        layoutUploadError.visibility = View.GONE
                        pbUploadBar.progress = state.percentage
                        tvUploadPercentage.text = "${state.percentage}%"
                        tvUploadStatusText.text = state.statusText

                        btnContinue.isEnabled = false
                        btnContinue.text = if (state.percentage > 0) "Publishing ${state.percentage}%" else "Publishing..."
                    }
                    is ReportUploadState.Success -> {
                        val createdReport = state.report
                        pbUploadBar.progress = 100
                        tvUploadPercentage.text = "100%"
                        tvUploadStatusText.text = "Successfully published"

                        val notifTitle = if (isLostSelected) "Lost Item Broadcasted" else "Found Item Registered"
                        val notifBody = if (isLostSelected) {
                            "Search alert active for '${createdReport.title}'. Nearby community members notified."
                        } else {
                            "Thank you for reporting '${createdReport.title}'. Searching for the verified owner."
                        }

                        // Save user notification to Firebase /notifications/{uid}
                        if (createdReport.userId.isNotEmpty()) {
                            val userNotif = com.group3.lostandfound.NotificationItem(
                                id = "notif_rep_${createdReport.itemId}_${System.currentTimeMillis()}",
                                title = notifTitle,
                                message = notifBody,
                                timestamp = System.currentTimeMillis(),
                                read = false,
                                type = "REPORT_POSTED",
                                itemId = createdReport.itemId,
                                senderUserId = createdReport.userId,
                                senderName = createdReport.authorName
                            )
                            FirebaseManager.database.getReference("notifications")
                                .child(createdReport.userId)
                                .push()
                                .setValue(userNotif)
                        }

                        // Broadcast new report notification to other users in community
                        FirebaseManager.database.getReference("users").addListenerForSingleValueEvent(object : com.google.firebase.database.ValueEventListener {
                            override fun onDataChange(userSnap: com.google.firebase.database.DataSnapshot) {
                                for (child in userSnap.children) {
                                    val otherUid = child.key ?: continue
                                    if (otherUid != createdReport.userId) {
                                        val communityNotif = com.group3.lostandfound.NotificationItem(
                                            id = "alert_${createdReport.itemId}_$otherUid",
                                            title = "New ${if (isLostSelected) "Lost" else "Found"} Report: ${createdReport.title}",
                                            message = "${createdReport.title} reported near ${createdReport.location}. Tap to view.",
                                            timestamp = System.currentTimeMillis(),
                                            read = false,
                                            type = "COMMUNITY_REPORT",
                                            itemId = createdReport.itemId,
                                            senderUserId = createdReport.userId,
                                            senderName = createdReport.authorName
                                        )
                                        FirebaseManager.database.getReference("notifications")
                                            .child(otherUid)
                                            .push()
                                            .setValue(communityNotif)
                                    }
                                }
                            }
                            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
                        })

                        com.group3.lostandfound.service.FCMService.triggerPushNotification(
                            requireContext(),
                            notifTitle,
                            notifBody,
                            createdReport.itemId
                        )

                        com.group3.lostandfound.util.ModernToast.show(
                            requireContext(),
                            "Report published successfully. Redirecting to My Reports.",
                            view
                        )

                        parentFragmentManager.beginTransaction()
                            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                            .replace(R.id.screenBox, ReportsFragment.newInstance())
                            .commit()

                        createReportViewModel.resetState()
                    }
                    is ReportUploadState.Error -> {
                        layoutUploadError.visibility = View.VISIBLE
                        tvErrorMessage.text = "Upload error: ${state.message}. Check connection and retry."
                        tvUploadStatusText.text = "Upload interrupted."

                        btnContinue.isEnabled = true
                        btnContinue.text = "Publish Report"

                        Toast.makeText(
                            requireContext(),
                            "Upload failed: ${state.message}. You can retry.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    is ReportUploadState.Idle -> {}
                }
            }
        }
    }

    //Saves form values when Android recreates the Fragment.
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_SAVED_STEP, currentStep)
        outState.putBoolean(KEY_SAVED_IS_LOST, isLostSelected)
        outState.putString(KEY_SAVED_DATE, selectedDate)
        outState.putString(KEY_SAVED_CATEGORY, selectedCategory)
        outState.putString(KEY_SAVED_AREA, selectedArea)
        outState.putDouble(KEY_SAVED_LAT, selectedLatitude)
        outState.putDouble(KEY_SAVED_LNG, selectedLongitude)
        outState.putString(KEY_SAVED_IMAGE_URI, selectedImageUri?.toString())
        outState.putInt(KEY_SAVED_PHOTOS_COUNT, photosCount)
        if (::inputTitle.isInitialized) outState.putString(KEY_SAVED_TITLE, inputTitle.text.toString())
        if (::inputDescription.isInitialized) outState.putString(KEY_SAVED_DESC, inputDescription.text.toString())
        if (::inputLocation.isInitialized) outState.putString(KEY_SAVED_LANDMARK, inputLocation.text.toString())
    }

    //Caches text inputs before the Fragment's view is destroyed.
    override fun onDestroyView() {
        if (::inputTitle.isInitialized) {
            savedTitle = inputTitle.text.toString()
        }
        if (::inputDescription.isInitialized) {
            savedDescription = inputDescription.text.toString()
        }
        if (::inputLocation.isInitialized) {
            savedLandmark = inputLocation.text.toString()
        }
        _binding?.webViewStep3Map?.destroy()
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_IS_LOST = "arg_is_lost"
        private const val ARG_LAT = "arg_lat"
        private const val ARG_LNG = "arg_lng"
        private const val ARG_LOC_NAME = "arg_loc_name"

        private const val KEY_SAVED_STEP = "key_saved_step"
        private const val KEY_SAVED_IS_LOST = "key_saved_is_lost"
        private const val KEY_SAVED_DATE = "key_saved_date"
        private const val KEY_SAVED_CATEGORY = "key_saved_category"
        private const val KEY_SAVED_AREA = "key_saved_area"
        private const val KEY_SAVED_LAT = "key_saved_lat"
        private const val KEY_SAVED_LNG = "key_saved_lng"
        private const val KEY_SAVED_IMAGE_URI = "key_saved_image_uri"
        private const val KEY_SAVED_PHOTOS_COUNT = "key_saved_photos_count"
        private const val KEY_SAVED_TITLE = "key_saved_title"
        private const val KEY_SAVED_DESC = "key_saved_desc"
        private const val KEY_SAVED_LANDMARK = "key_saved_landmark"

        //Creates a Fragment with optional initial report type and location arguments.
        fun newInstance(
            isLost: Boolean = true,
            latitude: Double = 6.0367,
            longitude: Double = 80.2170,
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
