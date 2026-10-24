package com.group3.lostandfound

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.group3.lostandfound.model.ItemReport
import com.group3.lostandfound.repository.ChatRepository
import com.group3.lostandfound.util.FlyerGenerator
import com.group3.lostandfound.util.ImageLoader
import com.group3.lostandfound.viewmodel.DetailsViewModel
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
    private lateinit var btnShare: ImageView
    private lateinit var btnLiveChat: TextView
    private lateinit var btnWhatsApp: TextView
    private lateinit var btnMarkResolved: TextView
    // btnContactMore removed — only Live Chat and WhatsApp are shown
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
    private var itemLatitude: Double = 6.9140
    private var itemLongitude: Double = 79.8630
    private var currentItemReport: ItemReport? = null

    private lateinit var detailsViewModel: DetailsViewModel
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
        detailsViewModel = ViewModelProvider(this)[DetailsViewModel::class.java]
        bindData()
        setupListeners()
        NavigationHelper.setup(this, view, NavigationHelper.Tab.HOME)
    }

    private fun initViews(view: View) {
        btnBack = view.findViewById(R.id.btnBack)
        btnBookmark = view.findViewById(R.id.btnBookmark)
        btnShare = view.findViewById(R.id.btnShare)
        btnLiveChat = view.findViewById(R.id.btnLiveChat)
        btnWhatsApp = view.findViewById(R.id.btnWhatsApp)
        btnMarkResolved = view.findViewById(R.id.btnMarkResolved)
        // btnContactMore removed from layout
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
        val initialTitle = arguments?.getString(ARG_TITLE) ?: "Item Report"
        val initialDesc = arguments?.getString(ARG_DESC) ?: "No description provided."
        val initialStatus = arguments?.getString(ARG_STATUS) ?: "LOST"
        val initialLocation = arguments?.getString(ARG_LOCATION) ?: "Colombo 03"
        itemId = arguments?.getString(ARG_ITEM_ID)
        val initialReporter = arguments?.getString(ARG_REPORTER_UID).orEmpty()
        val initialAuthorName = arguments?.getString(ARG_AUTHOR_NAME).orEmpty()
        if (initialAuthorName.isNotEmpty()) {
            reporterName = initialAuthorName
            view?.findViewById<TextView>(R.id.tvReporterName)?.text = reporterName
        }
        if (initialReporter.isNotEmpty()) {
            reporterUserId = initialReporter
            if (initialAuthorName.isEmpty()) {
                reporterName = "User #${initialReporter.takeLast(4)}"
                view?.findViewById<TextView>(R.id.tvReporterName)?.text = reporterName
            }
        }
        if (reporterUserId.isEmpty() && !itemId.isNullOrEmpty()) {
            reporterUserId = "user_${itemId.orEmpty().replace("-", "").takeLast(6)}"
        }
        detailsViewModel.loadItem(itemId.orEmpty())

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

        // Live status updates from DetailsViewModel
        if (!itemId.isNullOrEmpty()) {
            detailsJob?.cancel()
            detailsJob = viewLifecycleOwner.lifecycleScope.launch {
                detailsViewModel.itemState.collectLatest { report ->
                    if (report != null) {
                        tvItemTitle.text = report.title.ifEmpty { "Report Details" }
                        tvDescription.text = report.description.ifEmpty { "No description provided." }
                        updateStatusTag(report.status)

                        if (report.authorName.isNotEmpty() && (reporterName == "Community Reporter" || reporterName.startsWith("User #"))) {
                            reporterName = report.authorName
                            view?.findViewById<TextView>(R.id.tvReporterName)?.text = reporterName
                        }

                        if (report.userId.isNotEmpty()) {
                            reporterUserId = report.userId
                            val currentUid = FirebaseManager.currentUid
                            val isOwner = currentUid.isNotEmpty() && (reporterUserId == currentUid || report.userId == currentUid)
                            btnBookmark.visibility = if (isOwner) View.GONE else View.VISIBLE
                            btnMarkResolved.visibility = if (isOwner && !report.status.equals("RESOLVED", ignoreCase = true)) View.VISIBLE else View.GONE
                            FirebaseManager.usersRef.child(report.userId).addListenerForSingleValueEvent(object : com.google.firebase.database.ValueEventListener {
                                override fun onDataChange(userSnapshot: com.google.firebase.database.DataSnapshot) {
                                    val name = userSnapshot.child("fullName").getValue(String::class.java)?.takeIf { it.isNotBlank() }
                                        ?: userSnapshot.child("displayName").getValue(String::class.java)?.takeIf { it.isNotBlank() }
                                        ?: userSnapshot.child("email").getValue(String::class.java)?.substringBefore("@")?.takeIf { it.isNotBlank() }
                                        ?: report.authorName.takeIf { it.isNotBlank() }
                                        ?: "User #${report.userId.takeLast(4)}"
                                    reporterName = name
                                    view?.findViewById<TextView>(R.id.tvReporterName)?.text = reporterName

                                    val phone = userSnapshot.child("phone").getValue(String::class.java)
                                        ?: userSnapshot.child("phoneNumber").getValue(String::class.java)
                                        ?: ""
                                    reporterPhone = phone.trim()
                                    if (reporterPhone.isNotBlank()) {
                                        btnWhatsApp.visibility = View.VISIBLE
                                    } else {
                                        btnWhatsApp.visibility = View.GONE
                                    }
                                }
                                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
                            })
                        }

                        currentItemReport = report
                        if (report.latitude != 0.0 || report.longitude != 0.0) {
                            itemLatitude = report.latitude
                            itemLongitude = report.longitude
                        } else {
                            val resolved = resolveFallbackCoordinates(report.location)
                            itemLatitude = resolved.first
                            itemLongitude = resolved.second
                        }

                        val loc = if (report.latitude != 0.0 || report.longitude != 0.0) {
                            "${report.latitude}, ${report.longitude}"
                        } else {
                            report.location.ifEmpty { "Colombo 03" }
                        }
                        tvLocationText.text = loc
                        view?.findViewById<TextView>(R.id.tvStreetName)?.text = if (report.location.isNotBlank()) report.location else "Colombo, Western Province"

                        loadEmbeddedMap(itemLatitude, itemLongitude, report.status, report.title)

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
                btnLiveChat.text = "Chat Owner"
            }
            "FOUND" -> {
                tagStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.tag_found_color))
                tagStatus.setBackgroundResource(R.drawable.bg_tag_found_outlined)
                btnLiveChat.text = "Chat Finder"
            }
            "RESOLVED" -> {
                tagStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.tag_found_color))
                tagStatus.setBackgroundResource(R.drawable.bg_tag_found_outlined)
                btnLiveChat.text = "Item Claimed"
            }
            else -> {
                tagStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
                tagStatus.setBackgroundResource(R.drawable.bg_tag_found_outlined)
                btnLiveChat.text = "Live Chat"
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

        // Bookmarks persistence in findit_prefs & Firebase
        val prefs = requireContext().getSharedPreferences("findit_prefs", android.content.Context.MODE_PRIVATE)
        val currentSet = prefs.getStringSet("bookmarked_ids", emptySet()) ?: emptySet()
        isBookmarked = itemId != null && currentSet.contains(itemId)
        btnBookmark.setImageResource(if (isBookmarked) R.drawable.ic_nav_my_reports else R.drawable.ic_bookmark_outline)

        val currentUid = FirebaseManager.currentUid
        if (currentUid.isNotEmpty() && reporterUserId == currentUid) {
            btnBookmark.visibility = View.GONE
        }

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
            val reportToShare = currentItemReport ?: ItemReport(
                itemId = itemId.orEmpty(),
                userId = reporterUserId,
                title = tvItemTitle.text.toString(),
                description = tvDescription.text.toString(),
                status = tagStatus.text.toString(),
                location = tvLocationText.text.toString(),
                latitude = itemLatitude,
                longitude = itemLongitude,
                authorName = reporterName
            )
            FlyerGenerator.showFlyerShareDialog(
                requireActivity(),
                reportToShare
            )
        }

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
                                    .setTitle("🎉 Congratulations!")
                                    .setMessage("'$itemTitle' has been marked as RESOLVED and reunited with its owner!\n\n🏆 You earned +15 Trust Score and unlocked the 'Honest Finder' achievement badge!")
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
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(
                    R.id.screenBox,
                    MapFragment.newInstance(
                        isPickerMode = false,
                        targetLat = itemLatitude,
                        targetLng = itemLongitude,
                        targetItemId = itemId.orEmpty()
                    )
                )
                .addToBackStack(null)
                .commit()
        }
        bannerLocation.setOnClickListener(openMapAction)
        btnOpenFullMap?.setOnClickListener(openMapAction)
        mapClickOverlay?.setOnClickListener(openMapAction)

        // Hide cardSimilarItem since there is no dummy item
        cardSimilarItem.visibility = View.GONE
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
            options.add("👤 ${user.second} (Award Reunited Hero Achievement)")
        }
        options.add("🤝 Resolved offline without chat contact")

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
            "🏆 $targetUserName will receive the 'Reunited Hero' achievement badge and +20 Trust Score points.\n" +
            "🌟 You will receive +15 Trust Score points."
        } else {
            "Has '$itemTitle' been successfully reunited with its rightful owner?"
        }

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("🎉 Confirm Resolution")
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
                                "title" to "🤝 Reunited Hero",
                                "description" to "Helped resolve '$itemTitle' and safely returned to owner!",
                                "timestamp" to System.currentTimeMillis(),
                                "badge" to "HERO_REUNITED",
                                "xpAwarded" to 20
                            )
                            userDbRef.child("achievements").child(achievementKey).setValue(achievement)

                            val notif = mapOf(
                                "id" to "notif_${System.currentTimeMillis()}",
                                "title" to "🏆 Achievement Unlocked: Reunited Hero!",
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
                            "'$itemTitle' has been marked as RESOLVED and reunited!\n\n🏆 $targetUserName earned the 'Reunited Hero' achievement and +20 Trust Score!\n🌟 You earned +15 Trust Score!"
                        } else {
                            "'$itemTitle' has been marked as RESOLVED and reunited!\n\n🏆 You earned +15 Trust Score and unlocked an achievement badge!"
                        }

                        if (isAdded) {
                            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                                .setTitle("🎉 Congratulations!")
                                .setMessage(congratsMessage)
                                .setPositiveButton("Awesome!", null)
                                .show()

                            com.group3.lostandfound.service.FCMService.triggerPushNotification(
                                requireContext(),
                                "🎉 Item Reunited & Resolved!",
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
        val dialog = Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.setContentView(R.layout.dialog_enlarged_photo)
        dialog.setCancelable(true)

        val imgEnlargedView = dialog.findViewById<ImageView>(R.id.imgEnlargedView)
        val btnCloseLargePhoto = dialog.findViewById<View>(R.id.btnCloseLargePhoto)
        val tvEnlargedTitle = dialog.findViewById<TextView>(R.id.tvEnlargedTitle)

        tvEnlargedTitle.text = title
        ImageLoader.load(imgEnlargedView, imageUrl, R.drawable.ic_item_placeholder, viewLifecycleOwner.lifecycleScope)

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
            holder.job = ImageLoader.load(holder.imageView, url, R.drawable.ic_item_placeholder, scope)

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
            val phoneText = if (reporterPhone.isNotBlank()) "📞 ${reporterPhone}" else "🔒 Contact via Live Chat"
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
            "💬 Open Live Chat (Instant Messaging)",
            "📝 Send Direct Note to Inbox",
            "💰 Offer Return Reward (LKR)",
            "📞 Call Reporter Verified Line"
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
                                            title = "💰 Reward Offer: LKR $amount",
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
        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = WebViewClient()
        loadEmbeddedMap(itemLatitude, itemLongitude, tagStatus.text.toString(), tvItemTitle.text.toString())
    }

    private fun loadEmbeddedMap(lat: Double, lng: Double, status: String, title: String) {
        val html = buildEmbeddedMapHtml(lat, lng, status, title)
        webViewItemMap?.loadDataWithBaseURL("https://leafletjs.com/", html, "text/html", "UTF-8", null)
        tvMapCoordinates?.text = String.format(java.util.Locale.US, "%.4f° N, %.4f° E · Tap to explore", lat, lng)
    }

    private fun resolveFallbackCoordinates(location: String): Pair<Double, Double> {
        val lower = location.lowercase(java.util.Locale.US)
        return when {
            lower.contains("vihara") || lower.contains("park") -> Pair(6.9140, 79.8630)
            lower.contains("independence") || lower.contains("colombo 07") -> Pair(6.9044, 79.8679)
            lower.contains("duplication") || lower.contains("colombo 03") || lower.contains("kollupitiya") -> Pair(6.8974, 79.8562)
            lower.contains("bambalapitiya") || lower.contains("colombo 04") -> Pair(6.8884, 79.8587)
            lower.contains("fort") || lower.contains("colombo 01") || lower.contains("galle face") -> Pair(6.9344, 79.8428)
            lower.contains("kandy") -> Pair(7.2906, 80.6337)
            lower.contains("galle") -> Pair(6.0535, 80.2210)
            else -> Pair(6.9271, 79.8612)
        }
    }

    private fun buildEmbeddedMapHtml(lat: Double, lng: Double, status: String, title: String): String {
        val pinColor = if (status.equals("LOST", ignoreCase = true)) "#E53935" else "#2E7D32"
        val cleanTitle = title.replace("'", "\\'").replace("\"", "\\\"").replace("\n", " ").take(40)
        return """
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
    <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
    <style>
        html, body, #map {
            width: 100%;
            height: 100%;
            margin: 0;
            padding: 0;
            background: #F7F5F0;
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
            zoomControl: false,
            dragging: false,
            touchZoom: false,
            doubleClickZoom: false,
            scrollWheelZoom: false,
            boxZoom: false
        }).setView([$lat, $lng], 15);

        L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
            maxZoom: 19
        }).addTo(map);

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

        L.marker([$lat, $lng], { icon: pinIcon }).addTo(map)
            .bindPopup("<b>$cleanTitle</b><br/>$status Location").openPopup();
    </script>
</body>
</html>
        """.trimIndent()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        detailsJob?.cancel()
        webViewItemMap?.destroy()
    }

    companion object {
        private const val ARG_ITEM_ID = "arg_item_id"
        private const val ARG_TITLE = "arg_title"
        private const val ARG_DESC = "arg_desc"
        private const val ARG_STATUS = "arg_status"
        private const val ARG_LOCATION = "arg_location"
        private const val ARG_IMAGE_RES = "arg_image_res"
        private const val ARG_REPORTER_UID = "arg_reporter_uid"
        private const val ARG_AUTHOR_NAME = "arg_author_name"

        fun newInstance(
            title: String = "Item Report",
            desc: String = "No description provided.",
            status: String = "LOST",
            location: String = "Colombo 03",
            imageResId: Int = R.drawable.ic_item_placeholder,
            itemId: String? = null,
            reporterUserId: String? = null,
            authorName: String? = null
        ): DetailsFragment {
            val fragment = DetailsFragment()
            val args = Bundle().apply {
                putString(ARG_ITEM_ID, itemId)
                putString(ARG_TITLE, title)
                putString(ARG_DESC, desc)
                putString(ARG_STATUS, status)
                putString(ARG_LOCATION, location)
                putInt(ARG_IMAGE_RES, imageResId)
                putString(ARG_REPORTER_UID, reporterUserId)
                putString(ARG_AUTHOR_NAME, authorName)
            }
            fragment.arguments = args
            return fragment
        }

        fun newInstance(
            itemId: String,
            title: String,
            desc: String,
            status: String,
            location: String,
            imageUrl: String = "",
            reporterUserId: String? = null,
            authorName: String? = null
        ): DetailsFragment {
            val fragment = DetailsFragment()
            val args = Bundle().apply {
                putString(ARG_ITEM_ID, itemId)
                putString(ARG_TITLE, title)
                putString(ARG_DESC, desc)
                putString(ARG_STATUS, status)
                putString(ARG_LOCATION, location)
                putString("arg_image_url", imageUrl)
                putInt(ARG_IMAGE_RES, R.drawable.ic_item_placeholder)
                putString(ARG_REPORTER_UID, reporterUserId)
                putString(ARG_AUTHOR_NAME, authorName)
            }
            fragment.arguments = args
            return fragment
        }
    }
}
