package com.group3.lostandfound

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import java.io.FileOutputStream
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.group3.lostandfound.model.ItemReport
import com.group3.lostandfound.repository.ChatRepository
import com.group3.lostandfound.repository.ItemRepository
import com.group3.lostandfound.util.FlyerGenerator
import com.group3.lostandfound.util.ImageLoader
import com.group3.lostandfound.viewmodel.DetailsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


class DetailsFragment : Fragment() {

    private lateinit var btnBack: ImageView
    private lateinit var btnBookmark: ImageView
    private lateinit var btnShare: ImageView
    private lateinit var btnLiveChat: TextView
    private lateinit var btnWhatsApp: TextView
    private lateinit var btnMarkResolved: TextView
    private var btnMoreOptions: View? = null
    private var btnEdit: View? = null
    private var btnDeleteReport: View? = null
    private var rowOwnerActions: View? = null
    private var sectionVisitorActions: View? = null

    // Progress Tracker Views
    private var dotStep1: View? = null
    private var lineStep1to2: View? = null
    private var dotStep2: View? = null
    private var lineStep2to3: View? = null
    private var dotStep3: View? = null
    private var lineStep3to4: View? = null
    private var dotStep4: View? = null
    private var lblStep1: TextView? = null
    private var lblStep2: TextView? = null
    private var lblStep3: TextView? = null
    private var lblStep4: TextView? = null

    // Matching Suggestions
    private var cardMatchingSuggestions: View? = null
    private var tvMatchScore: TextView? = null
    private var tvMatchTitle: TextView? = null
    private var tvMatchSubtitle: TextView? = null

    private var reporterPhone: String = ""
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
    private var reporterUserId: String = ""
    private var reporterName: String = "Community Reporter"

    // Embedded Map Properties
    private var webViewItemMap: WebView? = null
    private var btnOpenFullMap: View? = null
    private var mapClickOverlay: View? = null
    private var tvMapCoordinates: TextView? = null
    private var itemLatitude: Double = 6.0367
    private var itemLongitude: Double = 80.2170
    private var currentItemReport: ItemReport? = null

    private lateinit var detailsViewModel: DetailsViewModel
    private var itemId: String? = null
    private var detailsJob: Job? = null
    private var matchingJob: Job? = null

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
        detailsViewModel = ViewModelProvider(this)[DetailsViewModel::class.java]
        bindData()
        setupListeners()
        NavigationHelper.setup(this, view, NavigationHelper.Tab.REPORTS)
    }

    private fun initViews(view: View) {
        btnBack = view.findViewById(R.id.btnBack)
        btnBookmark = view.findViewById(R.id.btnBookmark)
        btnShare = view.findViewById(R.id.btnShare)
        btnLiveChat = view.findViewById(R.id.btnLiveChat)
        btnWhatsApp = view.findViewById(R.id.btnWhatsApp)
        btnMarkResolved = view.findViewById(R.id.btnMarkResolved)

        btnMoreOptions = view.findViewById(R.id.btnMoreOptions)
        btnEdit = view.findViewById(R.id.btnEdit)
        btnDeleteReport = view.findViewById(R.id.btnDeleteReport)
        rowOwnerActions = view.findViewById(R.id.rowOwnerActions)
        sectionVisitorActions = view.findViewById(R.id.sectionVisitorActions)

        dotStep1 = view.findViewById(R.id.dotStep1)
        lineStep1to2 = view.findViewById(R.id.lineStep1to2)
        dotStep2 = view.findViewById(R.id.dotStep2)
        lineStep2to3 = view.findViewById(R.id.lineStep2to3)
        dotStep3 = view.findViewById(R.id.dotStep3)
        lineStep3to4 = view.findViewById(R.id.lineStep3to4)
        dotStep4 = view.findViewById(R.id.dotStep4)
        lblStep1 = view.findViewById(R.id.lblStep1)
        lblStep2 = view.findViewById(R.id.lblStep2)
        lblStep3 = view.findViewById(R.id.lblStep3)
        lblStep4 = view.findViewById(R.id.lblStep4)

        cardMatchingSuggestions = view.findViewById(R.id.cardMatchingSuggestions)
        tvMatchScore = view.findViewById(R.id.tvMatchScore)
        tvMatchTitle = view.findViewById(R.id.tvMatchTitle)
        tvMatchSubtitle = view.findViewById(R.id.tvMatchSubtitle)

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

        webViewItemMap = view.findViewById(R.id.webViewItemMap)
        btnOpenFullMap = view.findViewById(R.id.btnOpenFullMap)
        mapClickOverlay = view.findViewById(R.id.mapClickOverlay)
        tvMapCoordinates = view.findViewById(R.id.tvMapCoordinates)
        setupEmbeddedMap()
    }

private fun bindData() {

    // Read the item details passed from the previous screen.
    // Use default values if any arguments are missing.
    val initialTitle = arguments?.getString(ARG_TITLE) ?: "Black leather wallet"
    val initialDesc = arguments?.getString(ARG_DESC)
        ?: "Slim wallet with national ID and two bank cards. Small scratch near the clasp."
    val initialStatus = arguments?.getString(ARG_STATUS) ?: "LOST"
    val initialLocation = arguments?.getString(ARG_LOCATION) ?: "Colombo 03"

    // Retrieve the item's unique ID.
    itemId = arguments?.getString(ARG_ITEM_ID)

    // Get the reporter's user ID and display name, using empty strings if missing.
    val initialReporter = arguments?.getString(ARG_REPORTER_UID).orEmpty()
    val initialAuthorName = arguments?.getString(ARG_AUTHOR_NAME).orEmpty()

    // Use the supplied author name or display a default name.
    reporterName = if (initialAuthorName.isNotEmpty()) {
        initialAuthorName
    } else {
        "Tharindu Perera"
    }

    // Display the reporter's name if the TextView exists.
    view?.findViewById<TextView>(R.id.tvReporterName)?.text = reporterName

    // Store the reporter's actual user ID when it is available.
    if (initialReporter.isNotEmpty()) {
        reporterUserId = initialReporter
    }

    // Generate a fallback identifier if the reporter ID is missing
    // but the item ID is available.
    if (reporterUserId.isEmpty() && !itemId.isNullOrEmpty()) {
        reporterUserId = "user_${itemId.orEmpty().replace("-", "").takeLast(6)}"
    }

    // Ask the ViewModel to load the item's details.
    detailsViewModel.loadItem(itemId.orEmpty())

    // Display the initial item information on the screen.
    tvItemTitle.text = initialTitle
    tvDescription.text = initialDesc
    updateStatusTag(initialStatus)
    tvLocationText.text = initialLocation

    // Resolve initial coordinates immediately (defaulting to Galle: 6.0367° N, 80.2170° E)
    val argLat = arguments?.getDouble("arg_latitude", 0.0) ?: 0.0
    val argLng = arguments?.getDouble("arg_longitude", 0.0) ?: 0.0
    if (argLat != 0.0 && argLng != 0.0) {
        itemLatitude = argLat
        itemLongitude = argLng
    } else {
        val resolved = resolveFallbackCoordinates(initialLocation)
        itemLatitude = resolved.first
        itemLongitude = resolved.second
    }
    loadEmbeddedMap(itemLatitude, itemLongitude, initialStatus, initialTitle)

    // Retrieve the initial photo URL, or use an empty string if missing.
    val initialImageUrl = arguments?.getString("arg_image_url").orEmpty()

    // Display the supplied photo when a valid URL is available.
    if (initialImageUrl.isNotEmpty()) {
        activeImageUrl = initialImageUrl

        // Store the image URL in the photo carousel's data list.
        itemPhotoList = listOf(initialImageUrl)

        // Configure the photo carousel and open an enlarged photo when clicked.
        pagerPhotos.adapter = PhotoSliderAdapter(
            itemPhotoList,
            viewLifecycleOwner.lifecycleScope
        ) { url ->
            showEnlargedPhotoDialog(url, tvItemTitle.text.toString())
        }

    } else {
        // Provide an empty image entry when no photo URL is supplied.
        val fallbackPhotos = listOf("")
        itemPhotoList = fallbackPhotos

        // Configure the carousel with the fallback entry.
        pagerPhotos.adapter = PhotoSliderAdapter(
            itemPhotoList,
            viewLifecycleOwner.lifecycleScope
        ) { url ->
            // Open the selected photo in an enlarged dialog.
            showEnlargedPhotoDialog(url, tvItemTitle.text.toString())
        }
    }

    // Retrieve the item's category, or use an empty string if unavailable.
    val initialCategory = arguments?.getString("arg_category").orEmpty()

    // Create a fallback report using the item details already available.
    val fallbackReport = ItemReport(
        itemId = itemId.orEmpty(),
        title = initialTitle,
        description = initialDesc,
        category = initialCategory,
        status = initialStatus,
        location = initialLocation,
        imageUrl = initialImageUrl,
        authorName = reporterName
    )

    // Observe suggestions for items that may match this report.
    observeMatchingSuggestions(fallbackReport)



        // Live status updates from DetailsViewModel
     
        // Load live report details only when a valid item ID is available.
        if (!itemId.isNullOrEmpty()) {

            // Cancel any previous collection job to avoid duplicate observers.
            detailsJob?.cancel()

            // Launch a coroutine tied to the Fragment's view lifecycle.
            detailsJob = viewLifecycleOwner.lifecycleScope.launch {

                // Observe report updates from the ViewModel.
                detailsViewModel.itemState.collectLatest { report ->

                    // Update the UI only when report data is available.
                    if (report != null) {

                        // Display the report title, description and current status.
                        tvItemTitle.text = report.title.ifEmpty { "Report Details" }
                        tvDescription.text = report.description.ifEmpty { "No description provided." }
                        updateStatusTag(report.status)

                        // Replace a generic reporter name with the actual report author's name.
                        if (report.authorName.isNotEmpty() &&
                            (reporterName == "Community Reporter" || reporterName.startsWith("User #"))
                        ) {
                            reporterName = report.authorName
                            view?.findViewById<TextView>(R.id.tvReporterName)?.text = reporterName
                        }

                        // Configure ownership-related actions when the report has a user ID.
                        if (report.userId.isNotEmpty()) {
                            reporterUserId = report.userId

                            // Compare the signed-in user's ID with the report owner's ID.
                            val currentUid = FirebaseManager.currentUid
                            val isOwner = currentUid.isNotEmpty() &&
                                (reporterUserId == currentUid || report.userId == currentUid)

                            // Show or hide actions according to report ownership.
                            btnBookmark.visibility = View.VISIBLE
                            rowOwnerActions?.visibility = if (isOwner) View.VISIBLE else View.GONE
                            btnDeleteReport?.visibility = if (isOwner) View.VISIBLE else View.GONE
                            sectionVisitorActions?.visibility = if (isOwner) View.GONE else View.VISIBLE

                            // Allow the owner to resolve a report only if it is not already resolved.
                            btnMarkResolved.visibility =
                                if (isOwner && !report.status.equals("RESOLVED", ignoreCase = true)) {
                                    View.VISIBLE
                                } else {
                                    View.GONE
                                }

                            // Fetch the report owner's profile from Firebase.
                            FirebaseManager.usersRef.child(report.userId)
                                .addListenerForSingleValueEvent(
                                    object : com.google.firebase.database.ValueEventListener {

                                        override fun onDataChange(
                                            userSnapshot: com.google.firebase.database.DataSnapshot
                                        ) {
                                            // Select the best available name from the user's profile.
                                            val name =
                                                userSnapshot.child("fullName")
                                                    .getValue(String::class.java)
                                                    ?.takeIf { it.isNotBlank() }
                                                    ?: userSnapshot.child("displayName")
                                                        .getValue(String::class.java)
                                                        ?.takeIf { it.isNotBlank() }
                                                    ?: userSnapshot.child("email")
                                                        .getValue(String::class.java)
                                                        ?.substringBefore("@")
                                                        ?.takeIf { it.isNotBlank() }
                                                    ?: report.authorName.takeIf { it.isNotBlank() }
                                                    ?: "User #${report.userId.takeLast(4)}"

                                            // Update the reporter's displayed name.
                                            reporterName = name
                                            view?.findViewById<TextView>(R.id.tvReporterName)?.text = reporterName

                                            // Retrieve the phone number using either supported field.
                                            val phone =
                                                userSnapshot.child("phone")
                                                    .getValue(String::class.java)
                                                    ?: userSnapshot.child("phoneNumber")
                                                        .getValue(String::class.java)
                                                    ?: ""

                                            // Show the WhatsApp button only when a phone number exists.
                                            reporterPhone = phone.trim()
                                            btnWhatsApp.visibility =
                                                if (reporterPhone.isNotBlank()) View.VISIBLE else View.GONE
                                        }

                                        // Handle Firebase profile-read failures.
                                        override fun onCancelled(
                                            error: com.google.firebase.database.DatabaseError
                                        ) {
                                            // No action is performed when the request fails.
                                        }
                                    }
                                )
                        }

                        // Store the currently displayed report and refresh matching suggestions.
                        currentItemReport = report
                        observeMatchingSuggestions(report)

                        // Use the report's coordinates when available; otherwise resolve its location.
                        if (report.latitude != 0.0 || report.longitude != 0.0) {
                            itemLatitude = report.latitude
                            itemLongitude = report.longitude
                        } else {
                            val resolved = resolveFallbackCoordinates(report.location)
                            itemLatitude = resolved.first
                            itemLongitude = resolved.second
                        }

                        // Prepare the location text shown to the user.
                        val loc =
                            if (report.latitude != 0.0 || report.longitude != 0.0) {
                                "${report.latitude}, ${report.longitude}"
                            } else {
                                report.location.ifEmpty { "Colombo 03" }
                            }

                        // Display the coordinates or fallback location and the street/location name.
                        tvLocationText.text = loc
                        view?.findViewById<TextView>(R.id.tvStreetName)?.text =
                            if (report.location.isNotBlank()) {
                                report.location
                            } else {
                                "Colombo, Western Province"
                            }

                        // Load the embedded map using the report's location and details.
                        loadEmbeddedMap(
                            itemLatitude,
                            itemLongitude,
                            report.status,
                            report.title
                        )

                        // Update the photo carousel if the report contains an image URL.
                        if (report.imageUrl.isNotEmpty()) {
                            activeImageUrl = report.imageUrl
                            itemPhotoList = listOf(report.imageUrl)

                            // Configure the carousel to enlarge the selected photo when tapped.
                            pagerPhotos.adapter = PhotoSliderAdapter(
                                itemPhotoList,
                                viewLifecycleOwner.lifecycleScope
                            ) { url ->
                                showEnlargedPhotoDialog(url, report.title)
                            }
                        }
                    }
                }
            }
        }

        // Listen for photo carousel page changes.
        pagerPhotos.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {

                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)

                    // Update the carousel indicator dots for the selected photo.
                    updateDots(position)
                }
            }
        )
    }

/**
 * Updates the status badge based on the report's current status.
 * Each status gets its own label, text color, and background drawable.
 */
private fun updateStatusTag(status: String) {
    // Remove surrounding spaces and convert the status to uppercase.
    // Example: " lost " becomes "LOST".
    val norm = status.trim().uppercase()

    // Select the badge appearance based on the normalized status.
    when (norm) {
        "LOST" -> {
            tagStatus.text = "Lost"
            tagStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            tagStatus.setBackgroundResource(R.drawable.bg_badge_lost_hero)
        }
        "FOUND" -> {
            tagStatus.text = "Found"
            tagStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            tagStatus.setBackgroundResource(R.drawable.bg_badge_found_hero)
        }
        "RESOLVED" -> {
            tagStatus.text = "Resolved"
            tagStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            tagStatus.setBackgroundResource(R.drawable.bg_badge_resolved_green)
        }
        "MATCHED" -> {
            tagStatus.text = "Matched"
            tagStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            tagStatus.setBackgroundResource(R.drawable.bg_badge_matched_pill)
        }
        else -> {
            // Display unknown statuses with a formatted label.
            // If the status is empty, display "Active" instead.
            tagStatus.text = if (norm.isNotEmpty()) {
                norm.lowercase().replaceFirstChar { it.uppercase() }
            } else {
                "Active"
            }

            tagStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            tagStatus.setBackgroundResource(R.drawable.bg_badge_matched_pill)
        }
    }

    // Update the progress indicator to reflect the report's status.
    updateReportProgressUI(status)
}

/**
 * Updates the report progress indicator.
 * Changes the step dots, connecting lines, and labels according to status.
 */
private fun updateReportProgressUI(status: String) {
    // Normalize the status so comparisons are case-insensitive.
    val norm = status.trim().uppercase()

    // Define colors used by the progress indicator.
    val navyColor = Color.parseColor("#002840")
    val grayColor = Color.parseColor("#94A3B8")
    val lineInactiveColor = Color.parseColor("#E2E8F0")

    // Select the progress display based on the report status.
    when (norm) {
        "RESOLVED" -> {
            // Mark all four progress steps as completed.
            dotStep1?.setBackgroundResource(R.drawable.bg_dot_progress_completed)
            lineStep1to2?.setBackgroundColor(navyColor)
            dotStep2?.setBackgroundResource(R.drawable.bg_dot_progress_completed)
            lineStep2to3?.setBackgroundColor(navyColor)
            dotStep3?.setBackgroundResource(R.drawable.bg_dot_progress_completed)
            lineStep3to4?.setBackgroundColor(navyColor)
            dotStep4?.setBackgroundResource(R.drawable.bg_dot_progress_completed)

            // Highlight all labels to indicate completion.
            lblStep1?.setTextColor(navyColor)
            lblStep2?.setTextColor(navyColor)
            lblStep3?.setTextColor(navyColor)
            lblStep4?.setTextColor(navyColor)
            lblStep4?.setTypeface(null, android.graphics.Typeface.BOLD)
        }

        "UNDER REVIEW", "REVIEW" -> {
            // Mark step 1 as completed and step 2 as active.
            dotStep1?.setBackgroundResource(R.drawable.bg_dot_progress_completed)
            lineStep1to2?.setBackgroundColor(navyColor)
            dotStep2?.setBackgroundResource(R.drawable.bg_dot_progress_active)

            // Keep the remaining steps inactive.
            lineStep2to3?.setBackgroundColor(lineInactiveColor)
            dotStep3?.setBackgroundResource(R.drawable.bg_dot_progress_inactive)
            lineStep3to4?.setBackgroundColor(lineInactiveColor)
            dotStep4?.setBackgroundResource(R.drawable.bg_dot_progress_inactive)

            // Highlight the completed and active step labels.
            lblStep1?.setTextColor(navyColor)
            lblStep2?.setTextColor(navyColor)
            lblStep2?.setTypeface(null, android.graphics.Typeface.BOLD)

            // Show future steps in gray.
            lblStep3?.setTextColor(grayColor)
            lblStep4?.setTextColor(grayColor)
        }

        else -> {
            // Default progress display for statuses such as MATCHED,
            // PENDING, and LOST: steps 1 and 2 are completed,
            // step 3 is active, and step 4 is inactive.
            dotStep1?.setBackgroundResource(R.drawable.bg_dot_progress_completed)
            lineStep1to2?.setBackgroundColor(navyColor)
            dotStep2?.setBackgroundResource(R.drawable.bg_dot_progress_completed)
            lineStep2to3?.setBackgroundColor(navyColor)
            dotStep3?.setBackgroundResource(R.drawable.bg_dot_progress_active)
            lineStep3to4?.setBackgroundColor(lineInactiveColor)
            dotStep4?.setBackgroundResource(R.drawable.bg_dot_progress_inactive)

            // Highlight the first three labels.
            lblStep1?.setTextColor(navyColor)
            lblStep2?.setTextColor(navyColor)
            lblStep3?.setTextColor(navyColor)
            lblStep3?.setTypeface(null, android.graphics.Typeface.BOLD)

            // Keep the final step inactive and not bold.
            lblStep4?.setTextColor(grayColor)
            lblStep4?.setTypeface(null, android.graphics.Typeface.NORMAL)
        }
    }
}

/**
 * Shows a confirmation dialog before deleting a report.
 * Deletes the report from Firebase only when the user confirms.
 */
private fun promptDeleteReport() {
    // Stop if there is no current report ID.
    val currentItemId = itemId ?: return

    // Get the report title to display in the confirmation dialog.
    val title = tvItemTitle.text.toString()

    // Build the delete confirmation dialog.
    androidx.appcompat.app.AlertDialog.Builder(requireContext())
        .setTitle("Delete Report")
        .setMessage("Are you sure you want to delete '$title'? This cannot be undone.")

        // Execute this block when the user presses Delete.
        .setPositiveButton("Delete") { _, _ ->

            // Remove the report from Firebase using its ID.
            FirebaseManager.itemsRef.child(currentItemId)
                .removeValue()
                .addOnCompleteListener { task ->

                    // Check whether the Firebase deletion succeeded.
                    if (task.isSuccessful) {
                        Toast.makeText(
                            requireContext(),
                            "'$title' deleted successfully.",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Return to the previous screen.
                        parentFragmentManager.popBackStack()
                    } else {
                        // Display the error if deletion failed.
                        Toast.makeText(
                            requireContext(),
                            "Failed to delete: ${task.exception?.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }

        // Close the dialog without deleting anything.
        .setNegativeButton("Cancel", null)
        .show()
}

/**
 * Opens an edit dialog containing the report's current details.
 * Saves the updated title, description, and location to Firebase.
 */
private fun promptEditReport() {
    // Stop if there is no current report ID.
    val currentItemId = itemId ?: return

    // Read the existing report details from the UI.
    val currentTitle = tvItemTitle.text.toString()
    val currentDesc = tvDescription.text.toString()

    // Remove any extra date text after the location separator.
    val currentLocation = tvLocationText.text.toString().substringBefore(" · ")

    // Create a vertical layout to hold the three input fields.
    val layout = android.widget.LinearLayout(requireContext()).apply {
        orientation = android.widget.LinearLayout.VERTICAL

        // Set the layout's internal spacing in pixels.
        setPadding(48, 24, 48, 12)
    }

    // Create the title input and prefill it with the current title.
    val inputTitle = android.widget.EditText(requireContext()).apply {
        hint = "Item Title"
        setText(currentTitle)
    }

    // Create the description input and prefill it with the current description.
    val inputDesc = android.widget.EditText(requireContext()).apply {
        hint = "Description"
        setText(currentDesc)
    }

    // Create the location input and prefill it with the current location.
    val inputLocation = android.widget.EditText(requireContext()).apply {
        hint = "Location"
        setText(currentLocation)
    }

    // Add the input fields to the dialog's layout.
    layout.addView(inputTitle)
    layout.addView(inputDesc)
    layout.addView(inputLocation)

    // Build the edit dialog and display the custom input layout.
    androidx.appcompat.app.AlertDialog.Builder(requireContext())
        .setTitle("Edit Report")
        .setView(layout)

        // Read and save the edited values when Save Changes is pressed.
        .setPositiveButton("Save Changes") { _, _ ->
            val newTitle = inputTitle.text.toString().trim()
            val newDesc = inputDesc.text.toString().trim()
            val newLoc = inputLocation.text.toString().trim()

            // Require a non-empty title before saving.
            if (newTitle.isNotEmpty()) {

                // Create a map containing only the fields to update.
                val updates = mapOf<String, Any>(
                    "title" to newTitle,
                    "description" to newDesc,
                    "location" to newLoc
                )

                // Update the report fields in Firebase.
                FirebaseManager.itemsRef.child(currentItemId)
                    .updateChildren(updates)
                    .addOnCompleteListener { task ->

                        // Update the visible UI after a successful save.
                        if (task.isSuccessful) {
                            tvItemTitle.text = newTitle
                            tvDescription.text = newDesc

                            // Display the new location while preserving the
                            // existing location text if the field is empty.
                            tvLocationText.text = if (newLoc.isNotEmpty()) {
                                "$newLoc · Today"
                            } else {
                                tvLocationText.text
                            }

                            // Notify the user that the update succeeded.
                            Toast.makeText(
                                requireContext(),
                                "Report updated successfully",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
            }
        }

        // Close the dialog without saving changes.
        .setNegativeButton("Cancel", null)
        .show()
}

    private fun showOptionsMenu() {
        val options = arrayOf("Share Flyer & QR Code", "Edit Report", "Mark as Resolved", "Delete Report")
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(tvItemTitle.text.toString())
            .setItems(options) { _, which ->
                when (which) {
                    0 -> btnShare.performClick()
                    1 -> promptEditReport()
                    2 -> markItemAsResolved()
                    3 -> promptDeleteReport()
                }
            }
            .show()
    }

    private fun updateDots(selectedIndex: Int) {
        val activeRes = R.drawable.bg_photo_dot_active
        val inactiveRes = R.drawable.bg_photo_dot_inactive

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
        view?.findViewById<View>(R.id.cardPhotoContainer)?.setOnClickListener {
            showEnlargedPhotoDialog(activeImageUrl, tvItemTitle.text.toString())
        }

        // Bookmarks persistence in findit_prefs & Firebase
        val prefs = requireContext().getSharedPreferences("findit_prefs", android.content.Context.MODE_PRIVATE)
        val currentSet = prefs.getStringSet("bookmarked_ids", emptySet()) ?: emptySet()
        isBookmarked = itemId != null && currentSet.contains(itemId)
        btnBookmark.setImageResource(if (isBookmarked) R.drawable.ic_nav_my_reports else R.drawable.ic_bookmark_outline)

        val currentUid = FirebaseManager.currentUid
        btnBookmark.visibility = View.VISIBLE

        btnBookmark.setOnClickListener {
            val curId = itemId ?: return@setOnClickListener
            if (reporterUserId.isNotEmpty() && reporterUserId == currentUid) {
                Toast.makeText(requireContext(), "You cannot save your own posts. They are in My Reports.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val bookmarkedSet = prefs.getStringSet("bookmarked_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
            isBookmarked = !isBookmarked
            if (isBookmarked) {
                bookmarkedSet.add(curId)
                btnBookmark.setImageResource(R.drawable.ic_nav_my_reports)
                Toast.makeText(requireContext(), "Item saved to bookmarks", Toast.LENGTH_SHORT).show()
                if (currentUid.isNotEmpty()) {
                    com.google.firebase.database.FirebaseDatabase.getInstance(FirebaseManager.DATABASE_URL)
                        .getReference("users")
                        .child(currentUid)
                        .child("saved_items")
                        .child(curId)
                        .setValue(true)
                }
            } else {
                bookmarkedSet.remove(curId)
                btnBookmark.setImageResource(R.drawable.ic_bookmark_outline)
                Toast.makeText(requireContext(), "Removed from bookmarks", Toast.LENGTH_SHORT).show()
                if (currentUid.isNotEmpty()) {
                    com.google.firebase.database.FirebaseDatabase.getInstance(FirebaseManager.DATABASE_URL)
                        .getReference("users")
                        .child(currentUid)
                        .child("saved_items")
                        .child(curId)
                        .removeValue()
                }
            }
            prefs.edit().putStringSet("bookmarked_ids", bookmarkedSet).apply()
        }

        // Social Media & Messaging Sharing with Modern QR Flyer
        btnShare.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.screenBox, ShareFlyerFragment.newInstance())
                .addToBackStack("share_flyer")
                .commit()
        }

        btnMoreOptions?.setOnClickListener { showOptionsMenu() }
        btnEdit?.setOnClickListener { promptEditReport() }
        btnDeleteReport?.setOnClickListener { promptDeleteReport() }

        // Instant Live Messaging & WhatsApp actions
        btnLiveChat.setOnClickListener {
            openLiveChat()
        }
        btnWhatsApp.setOnClickListener {
            openWhatsApp()
        }
        btnMarkResolved.setOnClickListener {
            markItemAsResolved()
        }
        view?.findViewById<View>(R.id.rowReporter)?.setOnClickListener {
            showReporterProfileDialog()
        }
        view?.findViewById<View>(R.id.imgAvatar)?.setOnClickListener {
            showReporterProfileDialog()
        }

        // Live status resolution & management
        tagStatus.setOnClickListener {
            val currentItemId = itemId ?: return@setOnClickListener
            val itemTitle = tvItemTitle.text.toString()
            val options = arrayOf("Mark as RESOLVED / RETURNED", "Mark as LOST", "Mark as FOUND")
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Update Status: $itemTitle")
                .setItems(options) { _, which ->
                    val newStatus = when (which) {
                        0 -> "RESOLVED"
                        1 -> "LOST"
                        else -> "FOUND"
                    }
                    FirebaseManager.updateItemStatus(
                        itemId = currentItemId,
                        newStatus = newStatus,
                        onSuccess = {
                            updateStatusTag(newStatus)
                            if (newStatus == "RESOLVED") {
                                val resolverUid = FirebaseManager.currentUid
                                if (resolverUid.isNotEmpty()) {
                                    val userDbRef = com.google.firebase.database.FirebaseDatabase.getInstance(FirebaseManager.DATABASE_URL).getReference("users").child(resolverUid)
                                    userDbRef.child("itemsReturned").setValue(com.google.firebase.database.ServerValue.increment(1))
                                    userDbRef.child("trustScore").setValue(com.google.firebase.database.ServerValue.increment(15))
                                    val achievement = mapOf(
                                        "id" to "honest_finder_${System.currentTimeMillis()}",
                                        "title" to "Honest Finder Badge",
                                        "description" to "Successfully resolved '$itemTitle' and reunited with owner!",
                                        "timestamp" to System.currentTimeMillis()
                                    )
                                    userDbRef.child("achievements").push().setValue(achievement)
                                }
                                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                                    .setTitle("Congratulations!")
                                    .setMessage("'$itemTitle' has been marked as RESOLVED and reunited with its owner!\n\nYou earned +15 Trust Score and unlocked the 'Honest Finder' achievement badge!")
                                    .setPositiveButton("Awesome!", null)
                                    .show()
                            }
                            com.group3.lostandfound.service.FCMService.triggerPushNotification(
                                requireContext(),
                                "Status Updated: $newStatus",
                                "'$itemTitle' is now marked as $newStatus in Foundly community.",
                                currentItemId
                            )
                            Toast.makeText(requireContext(), "Status updated to $newStatus in Firebase!", Toast.LENGTH_SHORT).show()
                        },
                        onError = { error ->
                            Toast.makeText(requireContext(), "Failed to update: $error", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
                .show()
        }


        val openMapAction = View.OnClickListener {
            openFullMapPage()
        }
        bannerLocation.setOnClickListener(openMapAction)
        btnOpenFullMap?.setOnClickListener(openMapAction)
        mapClickOverlay?.setOnClickListener(openMapAction)
        view?.findViewById<View>(R.id.cardItemMapContainer)?.setOnClickListener(openMapAction)
        view?.findViewById<View>(R.id.badgeOpenMapPill)?.setOnClickListener(openMapAction)
        view?.findViewById<View>(R.id.boxMapCoordinatesBadge)?.setOnClickListener(openMapAction)
        tvMapCoordinates?.setOnClickListener(openMapAction)
        tvLocationText.setOnClickListener(openMapAction)

        val openSeeAllAction = View.OnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.screenBox, ExploreFragment.newInstance())
                .addToBackStack("main_flow")
                .commit()
        }
        view?.findViewById<View>(R.id.btnSeeAllSimilar)?.setOnClickListener(openSeeAllAction)
        cardSimilarItem.setOnClickListener(openSeeAllAction)
    }

    private fun markItemAsResolved() {
        val currentItemId = itemId ?: return
        val itemTitle = tvItemTitle.text.toString()
        val currentUid = FirebaseManager.currentUid

        // Check if there are chatted users for this item
        val convRef = com.google.firebase.database.FirebaseDatabase.getInstance(FirebaseManager.DATABASE_URL)
            .getReference("user_conversations")
            .child(currentUid)

        convRef.addListenerForSingleValueEvent(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val chattedUsers = mutableListOf<Pair<String, String>>()
                for (child in snapshot.children) {
                    val relatedId = child.child("relatedItemId").getValue(String::class.java) ?: ""
                    val otherUid = child.child("otherUserId").getValue(String::class.java) ?: ""
                    val otherName = child.child("otherUserName").getValue(String::class.java) ?: "Chat Contact"
                    if (otherUid.isNotEmpty() && otherUid != currentUid) {
                        if (relatedId == currentItemId || relatedId.isEmpty()) {
                            if (!chattedUsers.any { it.first == otherUid }) {
                                chattedUsers.add(Pair(otherUid, otherName))
                            }
                        }
                    }
                }

                if (chattedUsers.isNotEmpty() && isAdded) {
                    showChattedUserSelectionDialog(currentItemId, itemTitle, chattedUsers)
                } else if (isAdded) {
                    promptDirectResolution(currentItemId, itemTitle, null, null)
                }
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                if (isAdded) promptDirectResolution(currentItemId, itemTitle, null, null)
            }
        })
    }

    private fun showChattedUserSelectionDialog(
        currentItemId: String,
        itemTitle: String,
        chattedUsers: List<Pair<String, String>>
    ) {
        val options = mutableListOf<String>()
        chattedUsers.forEach { user ->
            options.add("${user.second} (Award Reunited Hero Achievement)")
        }
        options.add("Resolved offline without chat contact")

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Select Who Helped Reunited '$itemTitle'")
            .setItems(options.toTypedArray()) { _, which ->
                if (which < chattedUsers.size) {
                    val selectedUser = chattedUsers[which]
                    promptDirectResolution(currentItemId, itemTitle, selectedUser.first, selectedUser.second)
                } else {
                    promptDirectResolution(currentItemId, itemTitle, null, null)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun promptDirectResolution(
        currentItemId: String,
        itemTitle: String,
        targetUserId: String?,
        targetUserName: String?
    ) {
        val currentUid = FirebaseManager.currentUid
        val message = if (targetUserName != null) {
            "Confirm resolution with $targetUserName?\n\n" +
            "$targetUserName will receive the 'Reunited Hero' achievement badge and +20 Trust Score points.\n" +
            "You will receive +15 Trust Score points."
        } else {
            "Has '$itemTitle' been successfully reunited with its rightful owner?"
        }

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Confirm Resolution")
            .setMessage(message)
            .setPositiveButton("Yes, Reunited!") { _, _ ->
                val updateData = mutableMapOf<String, Any>(
                    "status" to "RESOLVED",
                    "resolvedTimestamp" to System.currentTimeMillis()
                )
                if (targetUserId != null && targetUserName != null) {
                    updateData["resolvedWithUserId"] = targetUserId
                    updateData["resolvedWithUserName"] = targetUserName
                    updateData["resolvedByUserId"] = currentUid
                }

                FirebaseManager.itemsRef.child(currentItemId).updateChildren(updateData).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        updateStatusTag("RESOLVED")
                        btnMarkResolved.visibility = View.GONE

                        // 1. Award Chatted User if selected
                        if (targetUserId != null && targetUserName != null) {
                            val userDbRef = FirebaseManager.usersRef.child(targetUserId)
                            userDbRef.child("itemsReturned").setValue(com.google.firebase.database.ServerValue.increment(1))
                            userDbRef.child("trustScore").setValue(com.google.firebase.database.ServerValue.increment(20))

                            val achievementKey = "reunited_hero_${System.currentTimeMillis()}"
                            val achievement = mapOf(
                                "id" to achievementKey,
                                "title" to "Reunited Hero",
                                "description" to "Helped resolve '$itemTitle' and safely returned to owner!",
                                "timestamp" to System.currentTimeMillis(),
                                "badge" to "HERO_REUNITED",
                                "xpAwarded" to 20
                            )
                            userDbRef.child("achievements").child(achievementKey).setValue(achievement)

                            val notif = mapOf(
                                "id" to "notif_${System.currentTimeMillis()}",
                                "title" to "Achievement Unlocked: Reunited Hero!",
                                "message" to "You were selected as the hero who helped reunite '$itemTitle'. +20 Trust Points awarded!",
                                "timestamp" to System.currentTimeMillis(),
                                "type" to "ACHIEVEMENT",
                                "itemId" to currentItemId,
                                "senderUserId" to currentUid,
                                "read" to false
                            )
                            FirebaseManager.database.getReference("notifications").child(targetUserId).push().setValue(notif)
                        }

                        // 2. Award Reporting User
                        if (currentUid.isNotEmpty()) {
                            val reporterRef = FirebaseManager.usersRef.child(currentUid)
                            reporterRef.child("itemsReturned").setValue(com.google.firebase.database.ServerValue.increment(1))
                            reporterRef.child("trustScore").setValue(com.google.firebase.database.ServerValue.increment(15))
                            val achievement = mapOf(
                                "id" to "case_resolved_${System.currentTimeMillis()}",
                                "title" to "Honest Finder & Case Closed",
                                "description" to "Successfully resolved '$itemTitle' and reunited with owner!",
                                "timestamp" to System.currentTimeMillis()
                            )
                            reporterRef.child("achievements").push().setValue(achievement)
                        }

                        val congratsMessage = if (targetUserName != null) {
                            "'$itemTitle' has been marked as RESOLVED and reunited!\n\n$targetUserName earned the 'Reunited Hero' achievement and +20 Trust Score!\nYou earned +15 Trust Score!"
                        } else {
                            "'$itemTitle' has been marked as RESOLVED and reunited!\n\nYou earned +15 Trust Score and unlocked an achievement badge!"
                        }

                        if (isAdded) {
                            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                                .setTitle("Congratulations!")
                                .setMessage(congratsMessage)
                                .setPositiveButton("Awesome!", null)
                                .show()

                            com.group3.lostandfound.service.FCMService.triggerPushNotification(
                                requireContext(),
                                "Item Reunited & Resolved!",
                                "'$itemTitle' has been returned and marked RESOLVED in Foundly community.",
                                currentItemId
                            )
                        }
                    } else if (isAdded) {
                        Toast.makeText(requireContext(), "Failed to update: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEnlargedPhotoDialog(imageUrl: String, title: String) {
        val dialog = Dialog(requireContext(), R.style.Theme_LostAndFound_FullScreenDialog)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_enlarged_photo)
        dialog.setCancelable(true)

        dialog.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.BLACK))
            clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            statusBarColor = android.graphics.Color.BLACK
            navigationBarColor = android.graphics.Color.BLACK
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                attributes.layoutInDisplayCutoutMode = android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        val topBarEnlarged = dialog.findViewById<View>(R.id.topBarEnlarged)
        topBarEnlarged?.bringToFront()
        if (topBarEnlarged != null) {
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(topBarEnlarged) { v, insets ->
                val statusBarHeight = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars()).top
                val safeTop = if (statusBarHeight > 0) statusBarHeight + 12 else (v.resources.displayMetrics.density * 36).toInt()
                v.setPadding(v.paddingLeft, safeTop, v.paddingRight, v.paddingBottom)
                insets
            }
        }

        val imgEnlargedView = dialog.findViewById<ImageView>(R.id.imgEnlargedView)
        val btnCloseLargePhoto = dialog.findViewById<View>(R.id.btnCloseLargePhoto)
        val btnZoomPhoto = dialog.findViewById<View>(R.id.btnZoomPhoto)
        val tvPhotoCounter = dialog.findViewById<TextView>(R.id.tvPhotoCounter)
        val tvEnlargedTitle = dialog.findViewById<TextView>(R.id.tvEnlargedTitle)
        val tvEnlargedReporter = dialog.findViewById<TextView>(R.id.tvEnlargedReporter)
        val btnSharePhoto = dialog.findViewById<View>(R.id.btnSharePhoto)
        val btnSavePhoto = dialog.findViewById<View>(R.id.btnSavePhoto)
        val btnReportPhoto = dialog.findViewById<View>(R.id.btnReportPhoto)

        tvEnlargedTitle.text = if (title.isNotBlank()) title else "Report Details"
        val reporter = if (reporterName.isNotBlank() && reporterName != "Community Reporter") {
            reporterName
        } else {
            "Community Reporter"
        }
        tvEnlargedReporter.text = "Photo from $reporter's report"

        val photoIndex = itemPhotoList.indexOf(imageUrl).let { if (it >= 0) it + 1 else 1 }
        val totalCount = itemPhotoList.size.coerceAtLeast(1)
        tvPhotoCounter.text = "$photoIndex/$totalCount"

        if (imageUrl.isNotBlank()) {
            ImageLoader.load(imgEnlargedView, imageUrl, R.drawable.ic_item_placeholder, viewLifecycleOwner.lifecycleScope)
        } else {
            imgEnlargedView.setImageResource(R.drawable.ic_item_placeholder)
        }

        setupScrollAndZoom(imgEnlargedView, btnZoomPhoto)

        btnCloseLargePhoto.setOnClickListener {
            dialog.dismiss()
        }

        btnSharePhoto.setOnClickListener {
            shareEnlargedPhoto(tvEnlargedTitle.text.toString(), imageUrl, imgEnlargedView)
        }

        btnSavePhoto.setOnClickListener {
            saveEnlargedPhotoToGallery(imgEnlargedView)
        }

        btnReportPhoto.setOnClickListener {
            showReportPhotoDialog(tvEnlargedTitle.text.toString())
        }

        dialog.setOnShowListener {
            dialog.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            topBarEnlarged?.bringToFront()
        }
        dialog.show()
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }
/**
 * Enables image dragging, pinch-to-zoom, double-tap zoom,
 * and button-based zoom for an ImageView.
 */
private fun setupScrollAndZoom(imageView: ImageView, btnZoom: View) {

    // Stores the image's original fitted position and scale.
    val baseMatrix = Matrix()

    // Stores the image's current transformation.
    val currentMatrix = Matrix()

    // Saves the transformation at the beginning of a gesture.
    val savedMatrix = Matrix()

    // Stores the starting touch position when dragging.
    val startPoint = PointF()

    // Stores the midpoint between two fingers during pinch zoom.
    val midPoint = PointF()

    // Distance between the two fingers at the beginning of a pinch.
    var oldDist = 1f

    // Tracks the current gesture: 0 = None, 1 = Drag, 2 = Pinch Zoom.
    var mode = 0

    // Tracks whether the image is currently considered zoomed.
    var isZoomed = false

    // Fits the image inside the ImageView and centers it.
    fun resetToCenter() {
        // Get the image drawable; stop if no image is available.
        val drawable = imageView.drawable ?: return

        // Get the original image dimensions.
        val dw = drawable.intrinsicWidth.toFloat()
        val dh = drawable.intrinsicHeight.toFloat()

        // Get the available ImageView dimensions.
        val vw = imageView.width.toFloat()
        val vh = imageView.height.toFloat()

        // Avoid invalid calculations when dimensions are zero or negative.
        if (dw <= 0f || dh <= 0f || vw <= 0f || vh <= 0f) return

        // Calculate a scale that fits the entire image inside the view.
        val scale = minOf(vw / dw, vh / dh)

        // Calculate offsets to center the scaled image horizontally and vertically.
        val dx = (vw - dw * scale) / 2f
        val dy = (vh - dh * scale) / 2f

        // Reset the base matrix and apply the initial scale and position.
        baseMatrix.reset()
        baseMatrix.postScale(scale, scale)
        baseMatrix.postTranslate(dx, dy)

        // Apply the fitted transformation to the image.
        currentMatrix.set(baseMatrix)
        imageView.imageMatrix = currentMatrix

        // Mark the image as not zoomed.
        isZoomed = false
    }

    // Toggles between the fitted image and an enlarged image.
    fun toggleZoomAt(focusX: Float, focusY: Float) {
        if (isZoomed) {
            // Return the image to its original fitted position.
            resetToCenter()
        } else {
            // Start from the fitted position before applying zoom.
            resetToCenter()

            // Enlarge the image by 2.2 times around the specified focus point.
            currentMatrix.postScale(2.2f, 2.2f, focusX, focusY)

            // Display the enlarged image.
            imageView.imageMatrix = currentMatrix
            isZoomed = true
        }
    }

    // Wait until the ImageView has been laid out before centering the image.
    imageView.post { resetToCenter() }

    // Detect gestures such as double-tapping the image.
    val gestureDetector = GestureDetector(
        imageView.context,
        object : GestureDetector.SimpleOnGestureListener() {

            // Called when the user double-taps the image.
            override fun onDoubleTap(e: MotionEvent): Boolean {
                // Toggle zoom around the tapped position.
                toggleZoomAt(e.x, e.y)
                return true
            }
        }
    )

    // Handle touch events on the image.
    imageView.setOnTouchListener { _, event ->

        // Pass the touch event to the gesture detector.
        gestureDetector.onTouchEvent(event)

        // Determine which touch action occurred.
        when (event.action and MotionEvent.ACTION_MASK) {

            MotionEvent.ACTION_DOWN -> {
                // Save the current image transformation before dragging.
                savedMatrix.set(currentMatrix)

                // Record the initial finger position.
                startPoint.set(event.x, event.y)

                // Enter drag mode.
                mode = 1
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                // Calculate the distance between the two fingers.
                oldDist = getSpacing(event)

                // Continue only if the fingers are far enough apart.
                if (oldDist > 10f) {
                    // Save the current transformation before zooming.
                    savedMatrix.set(currentMatrix)

                    // Calculate the midpoint between the two fingers.
                    getMidPoint(midPoint, event)

                    // Enter pinch-zoom mode.
                    mode = 2
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (mode == 1) {
                    // Restore the transformation saved at the start of the drag.
                    currentMatrix.set(savedMatrix)

                    // Calculate the horizontal and vertical finger movement.
                    val dx = event.x - startPoint.x
                    val dy = event.y - startPoint.y

                    // Move the image according to the finger movement.
                    currentMatrix.postTranslate(dx, dy)

                    // Apply the updated image position.
                    imageView.imageMatrix = currentMatrix

                } else if (mode == 2) {
                    // Measure the current distance between the two fingers.
                    val newDist = getSpacing(event)

                    if (newDist > 10f) {
                        // Restore the transformation saved at the start of the pinch.
                        currentMatrix.set(savedMatrix)

                        // Calculate the scale factor from the change in finger distance.
                        val scale = newDist / oldDist

                        // Scale the image around the fingers' midpoint.
                        currentMatrix.postScale(
                            scale,
                            scale,
                            midPoint.x,
                            midPoint.y
                        )

                        // Display the updated image transformation.
                        imageView.imageMatrix = currentMatrix

                        // Update the zoom flag based on the current gesture scale.
                        isZoomed = scale > 1.15f
                    }
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                // Reset the gesture mode when a finger is released.
                mode = 0
            }
        }

        // Consume the touch event so this listener handles the gesture.
        true
    }

    // Zoom around the center of the image when the zoom button is clicked.
    btnZoom.setOnClickListener {
        val cx = imageView.width / 2f
        val cy = imageView.height / 2f

        // Toggle between zoomed and fitted views.
        toggleZoomAt(cx, cy)
    }
}

/**
 * Calculates the distance between the first two fingers.
 * Used to determine the amount of pinch zoom.
 */
private fun getSpacing(event: MotionEvent): Float {

    // Calculate the horizontal distance between the fingers.
    val x = event.getX(0) - event.getX(1)

    // Calculate the vertical distance between the fingers.
    val y = event.getY(0) - event.getY(1)

    // Use the Pythagorean theorem to calculate the total distance.
    return kotlin.math.sqrt(x * x + y * y)
}

/**
 * Calculates the midpoint between the first two fingers.
 * This point is used as the focus of the pinch-zoom gesture.
 */
private fun getMidPoint(point: PointF, event: MotionEvent) {

    // Add the horizontal coordinates of both fingers.
    val x = event.getX(0) + event.getX(1)

    // Add the vertical coordinates of both fingers.
    val y = event.getY(0) + event.getY(1)

    // Divide the totals by two to obtain the midpoint.
    point.set(x / 2f, y / 2f)
}

/**
 * Shares the displayed photo using Android's share chooser.
 * If image sharing fails, falls back to sharing the photo URL as text.
 */
private fun shareEnlargedPhoto(
    title: String,
    imageUrl: String,
    imageView: ImageView
) {
    try {
        // Check whether the displayed drawable is a BitmapDrawable.
        val drawable = imageView.drawable as? BitmapDrawable

        if (drawable != null) {
            // Extract the bitmap currently displayed in the ImageView.
            val bitmap = drawable.bitmap

            // Create a temporary images directory inside the app's cache.
            val cacheDir = File(requireContext().cacheDir, "images").apply {
                mkdirs()
            }

            // Create a temporary file for the image being shared.
            val tempFile = File(cacheDir, "shared_item.png")

            // Write the bitmap to the temporary PNG file.
            FileOutputStream(tempFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
            }

            // Generate a content URI that other apps can access through FileProvider.
            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                tempFile
            )

            // Build an intent for sharing the image.
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                // Specify the shared content type.
                type = "image/png"

                // Attach the image URI.
                putExtra(Intent.EXTRA_STREAM, uri)

                // Include the title and descriptive text.
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, "Lost & Found Item: $title")

                // Grant the receiving app temporary permission to read the URI.
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            // Display Android's app chooser for sharing the photo.
            startActivity(
                Intent.createChooser(shareIntent, "Share Item Photo")
            )

            // Stop because the image-sharing flow has been started.
            return
        }
    } catch (_: Exception) {
        // Continue to the text-sharing fallback if image sharing fails.
    }

    // Build a plain-text sharing intent as a fallback.
    val textIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, title)

        // Include the item title and image URL in the shared text.
        putExtra(
            Intent.EXTRA_TEXT,
            "Lost & Found Item: $title\n$imageUrl"
        )
    }

    // Display the chooser for sharing the text link.
    startActivity(
        Intent.createChooser(textIntent, "Share Item Photo")
    )
}

/**
 * Saves the displayed photo to the device gallery.
 * Uses MediaStore for Android 10+ and a legacy API for older versions.
 */
@Suppress("DEPRECATION")
private fun saveEnlargedPhotoToGallery(imageView: ImageView) {

    // Get the displayed image as a bitmap.
    // If the drawable is unavailable or has another type, stop the operation.
    val drawable = imageView.drawable as? BitmapDrawable ?: run {
        Toast.makeText(
            requireContext(),
            "Photo not ready to save",
            Toast.LENGTH_SHORT
        ).show()
        return
    }

    // Extract the bitmap to save.
    val bitmap = drawable.bitmap

    try {
        // Generate a unique filename using the current timestamp.
        val filename = "LostFound_${System.currentTimeMillis()}.jpg"

        // Obtain the ContentResolver for accessing Android media storage.
        val resolver = requireContext().contentResolver

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10 and later: define the image's metadata.
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")

                // Save the image under Pictures/LostAndFound.
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/LostAndFound"
                )
            }

            // Create a new image entry in the device's media collection.
            val uri = resolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )

            if (uri != null) {
                // Open the destination and write the bitmap as a JPEG.
                resolver.openOutputStream(uri)?.use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
                }

                // Notify the user that the image was saved.
                Toast.makeText(
                    requireContext(),
                    "Photo saved to Gallery",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                // Notify the user if creating the media entry failed.
                Toast.makeText(
                    requireContext(),
                    "Failed to save photo",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            // Older Android versions: use the deprecated insertImage method.
            val savedUri = MediaStore.Images.Media.insertImage(
                resolver,
                bitmap,
                "Lost & Found Photo",
                "Saved from Lost & Found app"
            )

            // Check whether the image was inserted successfully.
            if (savedUri != null) {
                Toast.makeText(
                    requireContext(),
                    "Photo saved to Gallery",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Failed to save photo",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    } catch (e: Exception) {
        // Display the exception message if saving fails.
        Toast.makeText(
            requireContext(),
            "Failed to save photo: ${e.message}",
            Toast.LENGTH_SHORT
        ).show()
    }
}

/**
 * Displays a dialog allowing the user to select a reason for reporting a photo.
 * Currently, the selection is confirmed with a toast; it is not saved to Firebase.
 */
private fun showReportPhotoDialog(title: String) {

    // Define the available reasons for reporting a photo.
    val reasons = arrayOf(
        "Inappropriate or offensive content",
        "False or misleading photo",
        "Violates privacy / personal info",
        "Wrong item photo",
        "Other"
    )

    // Set the first reason as the default selection.
    var selectedReason = reasons[0]

    // Build a Material Design alert dialog.
    MaterialAlertDialogBuilder(requireContext())
        .setTitle("Report Photo")

        // Display the available reasons as a single-choice list.
        .setSingleChoiceItems(reasons, 0) { _, which ->
            // Store the reason selected by the user.
            selectedReason = reasons[which]
        }

        // Handle the user's submission.
        .setPositiveButton("Submit Report") { _, _ ->

            // Display a confirmation message with the selected reason.
            Toast.makeText(
                requireContext(),
                "Thank you. Photo reported for: $selectedReason",
                Toast.LENGTH_LONG
            ).show()
        }

        // Close the dialog without submitting.
        .setNegativeButton("Cancel", null)
        .show()
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
                adjustViewBounds = false
            }
            return PhotoViewHolder(img)
        }

        override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
            val url = photos[position]
            holder.job?.cancel()
            if (url.isBlank()) {
                holder.imageView.setImageResource(R.drawable.ic_item_placeholder)
            } else {
                holder.job = ImageLoader.load(holder.imageView, url, R.drawable.ic_item_placeholder, scope)
            }

            holder.imageView.setOnClickListener {
                onPhotoClicked(url)
            }
        }

        override fun getItemCount(): Int = photos.size
    }

    private fun openLiveChat() {
        val itemTitle = tvItemTitle.text.toString()
        resolveReporterAndProceed { targetUid, targetName ->
            val isOwnReport = targetUid == FirebaseManager.currentUid && FirebaseManager.currentUid.isNotEmpty()
            val effectiveUid = if (isOwnReport) "support_inquiry" else targetUid
            val effectiveName = if (isOwnReport) "$targetName (Self Preview)" else targetName

            if (isOwnReport) {
                com.group3.lostandfound.util.ModernToast.show(
                    requireContext(),
                    "Opening chat in test mode. You can test live messaging with support.",
                    view
                )
            }

            parentFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(
                    R.id.screenBox,
                    ChatFragment.newInstance(
                        recipientUserId = effectiveUid,
                        recipientName = effectiveName,
                        itemId = itemId ?: "",
                        itemTitle = itemTitle
                    )
                )
                .addToBackStack(null)
                .commit()
        }
    }

    private fun openWhatsApp() {
        val phone = reporterPhone.replace("+", "").replace(" ", "").replace("-", "").trim()
        if (phone.isBlank()) {
            Toast.makeText(requireContext(), "No WhatsApp number provided by this reporter", Toast.LENGTH_SHORT).show()
            return
        }
        val cleanPhone = if (phone.startsWith("0")) "94" + phone.substring(1) else phone
        val itemTitle = tvItemTitle.text.toString()
        val text = "Hello! I am contacting you via Foundly regarding your item: $itemTitle"
        val url = "https://api.whatsapp.com/send?phone=$cleanPhone&text=${android.net.Uri.encode(text)}"
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
            startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(requireContext(), "WhatsApp is not installed or cannot open link", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showReporterProfileDialog() {
        resolveReporterAndProceed { _, targetName ->
            val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            val phoneText = if (reporterPhone.isNotBlank()) reporterPhone else "Contact via Live Chat"
            val message = "Reporter: $targetName\n$phoneText"

            val dialogBuilder = builder.setTitle("Reporter Profile")
                .setMessage(message)
                .setPositiveButton("Live Chat") { _, _ -> openLiveChat() }

            if (reporterPhone.isNotBlank()) {
                dialogBuilder.setNeutralButton("WhatsApp") { _, _ -> openWhatsApp() }
            }
            dialogBuilder.setNegativeButton("Close", null).show()
        }
    }

    private fun showContactOptions() {
        val itemTitle = tvItemTitle.text.toString()
        val contactOptions = arrayOf(
            "Open Live Chat (Instant Messaging)",
            "Send Direct Note to Inbox",
            "Offer Return Reward (LKR)",
            "Call Reporter Verified Line"
        )
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Contact Reporter · $itemTitle")
            .setItems(contactOptions) { _, which ->
                resolveReporterAndProceed { targetUid, targetName ->
                    val isOwnReport = targetUid == FirebaseManager.currentUid && FirebaseManager.currentUid.isNotEmpty()
                    val effectiveUid = if (isOwnReport) "support_inquiry" else targetUid
                    val effectiveName = if (isOwnReport) "$targetName (Self Preview)" else targetName

                    if (isOwnReport && which == 0) {
                        com.group3.lostandfound.util.ModernToast.show(
                            requireContext(),
                            "Opening chat in test mode. You can test live messaging with support.",
                            view
                        )
                    }

                    when (which) {
                        0 -> {
                            // Direct Real-Time Chat screen with actual reporter
                            parentFragmentManager.beginTransaction()
                                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                                .replace(
                                    R.id.screenBox,
                                    ChatFragment.newInstance(
                                        recipientUserId = effectiveUid,
                                        recipientName = effectiveName,
                                        itemId = itemId ?: "",
                                        itemTitle = itemTitle
                                    )
                                )
                                .addToBackStack(null)
                                .commit()
                        }
                        1 -> {
                            val msgInput = android.widget.EditText(requireContext()).apply {
                                hint = "Type message regarding '$itemTitle'..."
                                setPadding(40, 30, 40, 30)
                            }
                            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                                .setTitle("Message $effectiveName")
                                .setView(msgInput)
                                .setPositiveButton("Send") { _, _ ->
                                    val text = msgInput.text.toString().trim()
                                    if (text.isNotEmpty()) {
                                        val chatRepo = ChatRepository()
                                        val convId = chatRepo.generateChatConversationId(FirebaseManager.currentUid, effectiveUid, itemId ?: "")
                                        viewLifecycleOwner.lifecycleScope.launch {
                                            val res = chatRepo.sendChatMessage(
                                                chatConversationId = convId,
                                                senderUserId = FirebaseManager.currentUid,
                                                recipientUserId = effectiveUid,
                                                chatMessageTextContent = text,
                                                relatedItemId = itemId ?: "",
                                                otherUserName = effectiveName,
                                                relatedItemTitle = itemTitle
                                            )
                                            if (res is ResultState.Success) {
                                                com.group3.lostandfound.util.ModernToast.show(requireContext(), "Message sent securely to $effectiveName.", view)
                                            } else if (res is ResultState.Error) {
                                                Toast.makeText(requireContext(), "Failed to send message: ${res.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }
                                .setNegativeButton("Cancel", null)
                                .show()
                        }
                        2 -> {
                            val rewardInput = android.widget.EditText(requireContext()).apply {
                                hint = "Enter reward amount in LKR (e.g. 5000)"
                                inputType = android.text.InputType.TYPE_CLASS_NUMBER
                                setPadding(40, 30, 40, 30)
                            }
                            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                                .setTitle("Offer Reward for $itemTitle")
                                .setView(rewardInput)
                                .setPositiveButton("Attach Reward") { _, _ ->
                                    val amount = rewardInput.text.toString().trim()
                                    if (amount.isNotEmpty()) {
                                        val rewardData = mapOf(
                                            "itemId" to (itemId ?: "item_reward"),
                                            "amount" to (amount.toDoubleOrNull() ?: 0.0),
                                            "currency" to "LKR",
                                            "timestamp" to System.currentTimeMillis(),
                                            "offeredBy" to FirebaseManager.currentUid
                                        )
                                        FirebaseManager.database.getReference("rewards")
                                            .child(itemId ?: "item_reward")
                                            .setValue(rewardData)

                                        val rewardNotif = NotificationItem(
                                            id = System.currentTimeMillis().toString(),
                                            title = "Reward Offer: LKR $amount",
                                            message = "Someone offered a reward of LKR $amount for your item: $itemTitle",
                                            timestamp = System.currentTimeMillis(),
                                            read = false,
                                            type = "REWARD_OFFER",
                                            itemId = itemId ?: "",
                                            senderUserId = FirebaseManager.currentUid
                                        )
                                        FirebaseManager.database.getReference("notifications")
                                            .child(effectiveUid)
                                            .push()
                                            .setValue(rewardNotif)

                                        com.group3.lostandfound.util.ModernToast.show(requireContext(), "Reward of LKR $amount offered to $effectiveName.", view)
                                    }
                                }
                                .setNegativeButton("Cancel", null)
                                .show()
                        }
                        3 -> {
                            com.group3.lostandfound.util.ModernToast.show(requireContext(), "Connecting to $effectiveName's verified phone line.", view)
                        }
                    }
                }
            }
            .show()
    }

    private fun resolveReporterAndProceed(onResolved: (targetUid: String, targetName: String) -> Unit) {
        if (reporterUserId.isNotEmpty() && !reporterUserId.startsWith("reporter_")) {
            onResolved(reporterUserId, reporterName)
            return
        }
        val curItemId = itemId.orEmpty()
        if (curItemId.isEmpty()) {
            val fallbackUid = "community_support"
            val fallbackName = "Community Support"
            reporterUserId = fallbackUid
            reporterName = fallbackName
            onResolved(fallbackUid, fallbackName)
            return
        }

        FirebaseManager.itemsRef.child(curItemId).addListenerForSingleValueEvent(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val snapshotUid = snapshot.child("userId").getValue(String::class.java)?.takeIf { it.isNotBlank() }
                    ?: snapshot.child("user_id").getValue(String::class.java)?.takeIf { it.isNotBlank() }
                    ?: snapshot.child("reporterId").getValue(String::class.java)?.takeIf { it.isNotBlank() }
                    ?: snapshot.child("authorId").getValue(String::class.java)?.takeIf { it.isNotBlank() }
                    ?: ""

                val authorName = snapshot.child("authorName").getValue(String::class.java)?.takeIf { it.isNotBlank() }
                    ?: snapshot.child("reporterName").getValue(String::class.java)?.takeIf { it.isNotBlank() }
                    ?: snapshot.child("userName").getValue(String::class.java)?.takeIf { it.isNotBlank() }
                    ?: "Community Reporter"

                val targetUid = if (snapshotUid.isNotEmpty()) snapshotUid else "user_${curItemId.replace("-", "").takeLast(6)}"
                reporterUserId = targetUid

                if (snapshotUid.isNotEmpty()) {
                    FirebaseManager.usersRef.child(snapshotUid).addListenerForSingleValueEvent(object : com.google.firebase.database.ValueEventListener {
                        override fun onDataChange(userSnap: com.google.firebase.database.DataSnapshot) {
                            val name = userSnap.child("fullName").getValue(String::class.java)?.takeIf { it.isNotBlank() }
                                ?: userSnap.child("displayName").getValue(String::class.java)?.takeIf { it.isNotBlank() }
                                ?: userSnap.child("email").getValue(String::class.java)?.substringBefore("@")?.takeIf { it.isNotBlank() }
                                ?: authorName
                            reporterName = name
                            onResolved(targetUid, name)
                        }
                        override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                            reporterName = authorName
                            onResolved(targetUid, authorName)
                        }
                    })
                } else {
                    reporterName = authorName
                    onResolved(targetUid, authorName)
                }
            }
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                val fallbackUid = "user_${curItemId.replace("-", "").takeLast(6)}"
                val fallbackName = "Community Reporter"
                reporterUserId = fallbackUid
                reporterName = fallbackName
                onResolved(fallbackUid, fallbackName)
            }
        })
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupEmbeddedMap() {
        val webView = webViewItemMap ?: return
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true
        settings.databaseEnabled = true
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }
        webView.setBackgroundColor(Color.parseColor("#EBF2F7"))
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                webView.evaluateJavascript("if (typeof map !== 'undefined') { map.invalidateSize(); }", null)
            }
        }
        webView.addJavascriptInterface(object : Any() {
            @JavascriptInterface
            fun openFullMap() {
                activity?.runOnUiThread {
                    openFullMapPage()
                }
            }
        }, "AndroidBridge")
        loadEmbeddedMap(itemLatitude, itemLongitude, tagStatus.text.toString(), tvItemTitle.text.toString())
    }

    private fun loadEmbeddedMap(lat: Double, lng: Double, status: String, title: String) {
        val effectiveLat = if (lat != 0.0) lat else 6.8974
        val effectiveLng = if (lng != 0.0) lng else 79.8562
        val distKm = calculateDistanceKm(6.0367, 80.2170, effectiveLat, effectiveLng)
        val distText = if (distKm < 0.2) "At your location in Galle" else String.format(java.util.Locale.US, "%.1f km from your location (Galle)", distKm)
        val html = buildEmbeddedMapHtml(effectiveLat, effectiveLng, status, title, distText)
        webViewItemMap?.loadDataWithBaseURL("https://leafletjs.com/", html, "text/html", "UTF-8", null)
        tvMapCoordinates?.text = String.format(java.util.Locale.US, "%.4f° N, %.4f° E · %s · Tap for full map", effectiveLat, effectiveLng, distText)
    }

    private fun openFullMapPage() {
        if (!isAdded) return
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(
                R.id.screenBox,
                MapFragment.newInstance(
                    isPickerMode = false,
                    targetLat = if (itemLatitude != 0.0) itemLatitude else 6.8974,
                    targetLng = if (itemLongitude != 0.0) itemLongitude else 79.8562,
                    targetItemId = itemId.orEmpty()
                )
            )
            .addToBackStack("details_map")
            .commit()
    }

    private fun resolveFallbackCoordinates(location: String): Pair<Double, Double> {
        val lower = location.lowercase(java.util.Locale.US).trim()
        val coordMatch = Regex("""(-?\d+\.\d+)[,\s]+(-?\d+\.\d+)""").find(lower)
        if (coordMatch != null) {
            val lat = coordMatch.groupValues[1].toDoubleOrNull()
            val lng = coordMatch.groupValues[2].toDoubleOrNull()
            if (lat != null && lng != null && lat != 0.0 && lng != 0.0) {
                return Pair(lat, lng)
            }
        }
        return when {
            lower.contains("liberty") || lower.contains("kollupitiya") || lower.contains("colombo 03") -> Pair(6.8974, 79.8562)
            lower.contains("bambalapitiya") || lower.contains("colombo 04") || lower.contains("majestic") -> Pair(6.8884, 79.8587)
            lower.contains("vihara") || lower.contains("victoria park") -> Pair(6.9140, 79.8630)
            lower.contains("independence") || lower.contains("colombo 07") || lower.contains("cinnamon") -> Pair(6.9044, 79.8679)
            lower.contains("fort") || lower.contains("colombo 01") || lower.contains("pettah") -> Pair(6.9344, 79.8428)
            lower.contains("galle face") -> Pair(6.9271, 79.8433)
            lower.contains("borella") || lower.contains("colombo 08") -> Pair(6.9147, 79.8776)
            lower.contains("dehiwala") || lower.contains("mount lavinia") -> Pair(6.8402, 79.8712)
            lower.contains("nugegoda") || lower.contains("kohuwala") -> Pair(6.8724, 79.8997)
            lower.contains("rajagiriya") || lower.contains("kotte") -> Pair(6.9090, 79.8970)
            lower.contains("negombo") || lower.contains("katunayake") -> Pair(7.2008, 79.8736)
            lower.contains("kandy") || lower.contains("peradeniya") -> Pair(7.2906, 80.6337)
            lower.contains("unawatuna") || lower.contains("galle fort") || lower.contains("karapitiya") || lower.contains("galle") -> Pair(6.0367, 80.2170)
            lower.contains("hikkaduwa") -> Pair(6.1406, 80.1011)
            lower.contains("matara") || lower.contains("mirissa") -> Pair(5.9549, 80.5550)
            lower.contains("jaffna") -> Pair(9.6615, 80.0255)
            lower.contains("kurunegala") -> Pair(7.4863, 80.3623)
            lower.contains("anuradhapura") -> Pair(8.3114, 80.4037)
            lower.contains("badulla") || lower.contains("ella") -> Pair(6.9934, 81.0550)
            lower.contains("batticaloa") -> Pair(7.7102, 81.6924)
            lower.contains("trincomalee") -> Pair(8.5874, 81.2152)
            else -> Pair(6.8974, 79.8562)
        }
    }

    private fun buildEmbeddedMapHtml(lat: Double, lng: Double, status: String, title: String, distText: String = ""): String {
        val pinColor = if (status.equals("LOST", ignoreCase = true)) "#DC2626" else "#059669"
        val cleanTitle = title.replace("'", "\\'").replace("\"", "\\\"").replace("\n", " ").take(40)
        return """
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
    <style>
        * {
            box-sizing: border-box;
            user-select: none;
            -webkit-user-select: none;
        }
        html, body {
            width: 100%;
            height: 100%;
            margin: 0;
            padding: 0;
            background: #EBF2F7;
            overflow: hidden;
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
            cursor: pointer;
        }
        #map {
            width: 100%;
            height: 100%;
            position: absolute;
            top: 0;
            left: 0;
            background-color: #EBF2F7;
            background-image: 
                linear-gradient(rgba(180, 205, 225, 0.35) 1px, transparent 1px),
                linear-gradient(90deg, rgba(180, 205, 225, 0.35) 1px, transparent 1px);
            background-size: 20px 20px;
        }
        .leaflet-control-attribution {
            display: none !important;
        }
        @keyframes user-pulse {
            0% { transform: scale(1); opacity: 0.8; }
            50% { transform: scale(1.6); opacity: 0.3; }
            100% { transform: scale(1); opacity: 0.8; }
        }
        .fallback-pin {
            position: absolute;
            top: 50%;
            left: 50%;
            transform: translate(-50%, -100%);
            display: flex;
            flex-direction: column;
            align-items: center;
            z-index: 100;
            pointer-events: none;
        }
        .fallback-bubble {
            background: #002840;
            color: #ffffff;
            font-size: 11px;
            font-weight: bold;
            padding: 4px 8px;
            border-radius: 6px;
            margin-bottom: 4px;
            white-space: nowrap;
            box-shadow: 0 2px 6px rgba(0,0,0,0.3);
        }
    </style>
</head>
<body onclick="triggerOpenMap()">
    <div id="map"></div>
    <div id="fallbackVisual" class="fallback-pin" style="display: none;">
        <div class="fallback-bubble">$cleanTitle</div>
        <svg width="28" height="38" viewBox="0 0 28 38" fill="none" xmlns="http://www.w3.org/2000/svg" style="filter: drop-shadow(0 3px 6px rgba(0,0,0,0.35));">
            <path d="M14 0C6.268 0 0 6.268 0 14C0 24.5 14 38 14 38C14 38 28 24.5 28 14C28 6.268 21.732 0 14 0Z" fill="$pinColor"/>
            <circle cx="14" cy="14" r="5" fill="#FFFFFF"/>
        </svg>
    </div>

    <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
    <script>
        function triggerOpenMap() {
            if (window.AndroidBridge && window.AndroidBridge.openFullMap) {
                window.AndroidBridge.openFullMap();
            }
        }

        if (typeof L === 'undefined') {
            document.getElementById('fallbackVisual').style.display = 'flex';
        } else {
            try {
                var userLat = 6.0367;
                var userLng = 80.2170;
                var itemLat = $lat;
                var itemLng = $lng;

                // 1. Initialize map view centered at user location in Galle
                var map = L.map('map', {
                    zoomControl: false,
                    dragging: false,
                    touchZoom: false,
                    doubleClickZoom: false,
                    scrollWheelZoom: false,
                    boxZoom: false,
                    attributionControl: false
                }).setView([userLat, userLng], 12);

                L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
                    maxZoom: 19
                }).addTo(map);

                // 2. Add Current User Location Pin (Galle)
                var userIcon = L.divIcon({
                    className: '',
                    html: '<div style="position:relative;width:24px;height:24px;"><div style="position:absolute;width:24px;height:24px;border-radius:50%;background:rgba(2,132,199,0.35);animation:user-pulse 1.8s infinite;"></div><div style="position:absolute;top:4px;left:4px;width:16px;height:16px;border-radius:50%;background:#0284C7;border:2.5px solid #FFFFFF;box-shadow:0 2px 8px rgba(0,0,0,0.4);display:flex;align-items:center;justify-content:center;"><div style="width:5px;height:5px;border-radius:50%;background:#FFFFFF;"></div></div></div>',
                    iconSize: [24, 24],
                    iconAnchor: [12, 12]
                });
                var userMarker = L.marker([userLat, userLng], { icon: userIcon, zIndexOffset: 2000 }).addTo(map);
                userMarker.bindPopup("<b>📍 Your Location</b><br/>Galle (6.0367° N, 80.2170° E)");

                // 3. Add Item Pinned Location Pin
                var pinSvg = '<svg width="28" height="38" viewBox="0 0 28 38" fill="none" xmlns="http://www.w3.org/2000/svg" style="filter: drop-shadow(0 3px 6px rgba(0,0,0,0.35));">' +
                    '<path d="M14 0C6.268 0 0 6.268 0 14C0 24.5 14 38 14 38C14 38 28 24.5 28 14C28 6.268 21.732 0 14 0Z" fill="$pinColor"/>' +
                    '<circle cx="14" cy="14" r="5" fill="#FFFFFF"/>' +
                    '</svg>';

                var pinIcon = L.divIcon({
                    className: '',
                    html: pinSvg,
                    iconSize: [28, 38],
                    iconAnchor: [14, 38],
                    popupAnchor: [0, -34]
                });

                var itemMarker = L.marker([itemLat, itemLng], { icon: pinIcon, zIndexOffset: 1500 }).addTo(map);
                itemMarker.bindPopup("<b>$cleanTitle</b><br/>$status Location<br/><span style='color:#64748B;font-size:11px;'>📍 $distText</span>");

                map.on('click', triggerOpenMap);
                itemMarker.on('click', triggerOpenMap);
                userMarker.on('click', triggerOpenMap);

                // 4. Smoothly animate and zoom into item's pinned location from Galle
                setTimeout(function() {
                    map.invalidateSize();
                    map.flyTo([itemLat, itemLng], 15, { duration: 1.8 });
                    setTimeout(function() {
                        itemMarker.openPopup();
                    }, 1900);
                }, 450);
            } catch(e) {
                document.getElementById('fallbackVisual').style.display = 'flex';
            }
        }
    </script>
</body>
</html>
        """.trimIndent()
    }

    private data class MatchResult(
        val item: ItemReport,
        val score: Int,
        val distanceKm: Double?
    )

   
    // Observes community reports and updates the matching suggestion for the current item.
    private fun observeMatchingSuggestions(current: ItemReport) {
        // Cancel the previous matching job before starting a new one.
        matchingJob?.cancel()
        // Launch a coroutine that follows the Fragment view lifecycle.
        matchingJob = viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Collect updated item reports from the repository.
                ItemRepository().streamAllItems().collectLatest { allItems ->
                    // Find the best matching report from the available items.
                    val candidate = findBestMatch(current, allItems)
                    // Display the candidate only when its score is at least 20.
                    if (candidate != null && candidate.score >= 20) {
                        // Retrieve the matched item from the result.
                        val matchedItem = candidate.item
                        // Display the matching score.
                        tvMatchScore?.text = "${candidate.score}% match"
                        tvMatchScore?.visibility = View.VISIBLE
                        // Display the matched item's title or a fallback message.
                        tvMatchTitle?.text = matchedItem.title.ifEmpty { "Similar item reported" }

                        // Determine the distance or location text to display.
                        val distText = if (candidate.distanceKm != null) {
                            // Convert distances below 1 kilometre into metres.
                            if (candidate.distanceKm < 1.0) {
                                "${(candidate.distanceKm * 1000).toInt()} m away"
                            } else {
                                // Display distances of 1 kilometre or more to one decimal place.
                                "%.1f km away".format(java.util.Locale.US, candidate.distanceKm)
                            }
                        } else if (matchedItem.location.isNotBlank()) {
                            // Use the location text when the distance is unavailable.
                            matchedItem.location
                        } else {
                            // Use a fallback when no location information is available.
                            "Nearby"
                        }

                        // Use a default name if the reporter's name is empty.
                        val author = matchedItem.authorName.ifEmpty { "Community Member" }
                        // Display the reporter's name and the item's location or distance.
                        tvMatchSubtitle?.text = "Reported by $author · $distText"

                        // Open the matched report when the suggestion card is clicked.
                        cardMatchingSuggestions?.setOnClickListener {
                            // Start a Fragment transaction with fade animations.
                            parentFragmentManager.beginTransaction()
                                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                                // Replace the current Fragment with the matched item's details.
                                .replace(
                                    R.id.screenBox,
                                    // Pass the selected report's information to the new Fragment.
                                    newInstance(
                                        itemId = matchedItem.itemId,
                                        title = matchedItem.title,
                                        desc = matchedItem.description,
                                        status = matchedItem.status,
                                        location = matchedItem.location,
                                        imageUrl = matchedItem.imageUrl,
                                        reporterUserId = matchedItem.userId,
                                        authorName = matchedItem.authorName
                                    )
                                )
                                // Add the current Fragment to the back stack for back navigation.
                                .addToBackStack(null)
                                // Commit the Fragment transaction.
                                .commit()
                        }
                    } else {
                        // Show the active-search message when no suitable match is found.
                        tvMatchScore?.text = "Active Search"
                        tvMatchTitle?.text = "No direct match found yet"
                        // Display a search message based on the current item's category.
                        tvMatchSubtitle?.text = if (current.category.isNotBlank()) {
                            "Actively searching community reports for ${current.category}..."
                        } else {
                            "Actively searching community reports for matching items..."
                        }
                        // Show a message when the user taps the suggestion card.
                        cardMatchingSuggestions?.setOnClickListener {
                            Toast.makeText(
                                requireContext(),
                                "No close match found yet for \"${current.title}\". We'll notify you when one is posted!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            // Catch exceptions without interrupting the coroutine with an uncaught error.
            } catch (_: Exception) {}
        }
    }

    // Finds and returns the highest-scoring match for the current report.
    private fun findBestMatch(current: ItemReport, allItems: List<ItemReport>): MatchResult? {
        // Return null when there are no reports to compare.
        if (allItems.isEmpty()) return null

        // Exclude reports without IDs and the current report itself.
        val otherItems = allItems.filter { it.itemId.isNotEmpty() && it.itemId != current.itemId }
        // Return null when no eligible reports remain.
        if (otherItems.isEmpty()) return null

        // Check whether the current report has a LOST or FOUND status.
        val currentIsLost = current.status.equals("LOST", ignoreCase = true)
        val currentIsFound = current.status.equals("FOUND", ignoreCase = true)

        // Extract meaningful keywords from the supplied text.
        fun extractKeywords(text: String): Set<String> {
            // Words that should be ignored during keyword matching.
            val stopWords = setOf("the", "and", "with", "for", "near", "from", "item", "lost", "found", "small", "large", "this", "that", "have", "please")
            // Convert the text to lowercase, split it into words, remove short and common words, and eliminate duplicates.
            return text.lowercase(java.util.Locale.US)
                .split(Regex("[^a-zA-Z0-9]+"))
                .filter { it.length > 2 && it !in stopWords }
                .toSet()
        }

        // Extract keywords from the current report's title and description.
        val currentKeywords = extractKeywords("${current.title} ${current.description}")
        // Store the highest-scoring match found so far.
        var bestResult: MatchResult? = null

        // Evaluate each eligible report.
        for (item in otherItems) {
            // Start the candidate's score at zero.
            var score = 0

            // Check whether the candidate report is lost or found.
            val itemIsLost = item.status.equals("LOST", ignoreCase = true)
            val itemIsFound = item.status.equals("FOUND", ignoreCase = true)

            // Status complementarity boost: reward reports where one is lost and the other is found.
            if ((currentIsLost && itemIsFound) || (currentIsFound && itemIsLost)) {
                score += 25
            } else if (currentIsLost == itemIsLost) {
                // Give a smaller bonus when the LOST/FOUND flags match.
                score += 5
            }

            // Category match: reward reports with the same nonempty category.
            if (current.category.isNotBlank() && item.category.isNotBlank() &&
                current.category.equals(item.category, ignoreCase = true)) {
                score += 35
            }

            // Keyword overlap: identify words shared by both reports.
            val itemKeywords = extractKeywords("${item.title} ${item.description}")
            val commonKeywords = currentKeywords.intersect(itemKeywords)
            // Add points for shared keywords, up to a maximum bonus of 35.
            if (commonKeywords.isNotEmpty()) {
                val keywordBoost = (commonKeywords.size * 15).coerceAtMost(35)
                score += keywordBoost
            }

            // Substring title match: normalize both titles before comparing them.
            val currentTitleLower = current.title.trim().lowercase(java.util.Locale.US)
            val itemTitleLower = item.title.trim().lowercase(java.util.Locale.US)
            // Reward titles when one nonempty title contains the other.
            if (currentTitleLower.isNotEmpty() && itemTitleLower.isNotEmpty()) {
                if (currentTitleLower.contains(itemTitleLower) || itemTitleLower.contains(currentTitleLower)) {
                    score += 15
                }
            }

            // Distance calculation: store the distance when coordinates are available.
            var distanceKm: Double? = null
            // Check that all coordinates are nonzero before calculating distance.
            if (current.latitude != 0.0 && current.longitude != 0.0 && item.latitude != 0.0 && item.longitude != 0.0) {
                // Calculate the geographical distance between the two reports.
                val dist = calculateDistanceKm(current.latitude, current.longitude, item.latitude, item.longitude)
                distanceKm = dist
                // Give a higher score to reports that are geographically closer.
                when {
                    dist < 1.0 -> score += 20
                    dist < 3.0 -> score += 15
                    dist < 10.0 -> score += 10
                    dist < 25.0 -> score += 5
                }
            // Use the location text as a fallback when coordinates are unavailable.
            } else if (current.location.isNotBlank() && item.location.isNotBlank()) {
                // Normalize both location strings for case-insensitive comparison.
                val loc1 = current.location.lowercase(java.util.Locale.US)
                val loc2 = item.location.lowercase(java.util.Locale.US)
                // Add points when one location string contains the other.
                if (loc1.contains(loc2) || loc2.contains(loc1)) {
                    score += 10
                }
            }

            // Restrict the final score to the range 0–98.
            val finalScore = score.coerceIn(0, 98)
            // Keep this candidate if it scores higher than the current best result.
            if (bestResult == null || finalScore > bestResult.score) {
                bestResult = MatchResult(item, finalScore, distanceKm)
            }
        }

        // Return the highest-scoring candidate, or null if none was evaluated.
        return bestResult
    }

    // Calculates the distance between two geographical coordinates in kilometres.
    private fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        // Approximate radius of the Earth in kilometres.
        val r = 6371.0
        // Convert the latitude and longitude differences from degrees to radians.
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        // Apply the Haversine formula to calculate the distance between the coordinates.
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        // Calculate the central angle between the two points.
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        // Multiply the angle by Earth's radius to obtain the distance.
        return r * c
    }

    // Called when the Fragment's view is destroyed.
    override fun onDestroyView() {
        // Execute the parent Fragment's cleanup implementation.
        super.onDestroyView()
        // Cancel ongoing jobs to prevent work from continuing unnecessarily.
        detailsJob?.cancel()
        matchingJob?.cancel()
        // Release the WebView's resources.
        webViewItemMap?.destroy()
    }

    // Holds constants and factory functions associated with DetailsFragment.
    companion object {
        // Bundle keys used to pass item details between Fragment instances.
        private const val ARG_ITEM_ID = "arg_item_id"
        private const val ARG_TITLE = "arg_title"
        private const val ARG_DESC = "arg_desc"
        private const val ARG_STATUS = "arg_status"
        private const val ARG_LOCATION = "arg_location"
        private const val ARG_IMAGE_RES = "arg_image_res"
        private const val ARG_REPORTER_UID = "arg_reporter_uid"
        private const val ARG_AUTHOR_NAME = "arg_author_name"
        private const val ARG_LATITUDE = "arg_latitude"
        private const val ARG_LONGITUDE = "arg_longitude"

        // Creates a DetailsFragment using a drawable resource for the item image.
        fun newInstance(
            title: String = "Item Report",
            desc: String = "No description provided.",
            status: String = "LOST",
            location: String = "Colombo 03",
            imageResId: Int = R.drawable.ic_item_placeholder,
            itemId: String? = null,
            reporterUserId: String? = null,
            authorName: String? = null,
            latitude: Double = 0.0,
            longitude: Double = 0.0
        ): DetailsFragment {
            // Create a new Fragment instance.
            val fragment = DetailsFragment()
            // Store the supplied report information in a Bundle.
            val args = Bundle().apply {
                putString(ARG_ITEM_ID, itemId)
                putString(ARG_TITLE, title)
                putString(ARG_DESC, desc)
                putString(ARG_STATUS, status)
                putString(ARG_LOCATION, location)
                putInt(ARG_IMAGE_RES, imageResId)
                putString(ARG_REPORTER_UID, reporterUserId)
                putString(ARG_AUTHOR_NAME, authorName)
                putDouble(ARG_LATITUDE, latitude)
                putDouble(ARG_LONGITUDE, longitude)
            }
            // Attach the arguments to the Fragment.
            fragment.arguments = args
            // Return the configured Fragment.
            return fragment
        }

        // Creates a DetailsFragment using a remote image URL.
        fun newInstance(
            itemId: String,
            title: String,
            desc: String,
            status: String,
            location: String,
            imageUrl: String = "",
            reporterUserId: String? = null,
            authorName: String? = null,
            latitude: Double = 0.0,
            longitude: Double = 0.0
        ): DetailsFragment {
            // Create a new Fragment instance.
            val fragment = DetailsFragment()
            // Store the report information and remote image URL in a Bundle.
            val args = Bundle().apply {
                putString(ARG_ITEM_ID, itemId)
                putString(ARG_TITLE, title)
                putString(ARG_DESC, desc)
                putString(ARG_STATUS, status)
                putString(ARG_LOCATION, location)
                putString("arg_image_url", imageUrl)
                // Set the placeholder drawable as the default image resource.
                putInt(ARG_IMAGE_RES, R.drawable.ic_item_placeholder)
                putString(ARG_REPORTER_UID, reporterUserId)
                putString(ARG_AUTHOR_NAME, authorName)
                putDouble(ARG_LATITUDE, latitude)
                putDouble(ARG_LONGITUDE, longitude)
            }
            // Attach the arguments to the Fragment.
            fragment.arguments = args
            // Return the configured Fragment.
            return fragment
        }
    }
}
