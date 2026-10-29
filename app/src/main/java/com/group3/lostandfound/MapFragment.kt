package com.group3.lostandfound

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

/**
 * MapFragment displays an interactive Leaflet map of Sri Lanka / Colombo:
 * - Powered by Leaflet.js and OpenStreetMap
 * - Interactive pins for Lost and Found items
 * - JavascriptInterface bridging marker taps to native Android UI
 * - Dynamic card preview updating with the selected marker
 * - Filter chips (All, Lost, Found, Today)
 * - GPS centering button
 * - Persistent bottom navigation bar
 */
class MapFragment : Fragment() {

    private lateinit var webViewLeafletMap: WebView
    private lateinit var btnClose: FrameLayout
    private lateinit var btnListView: ImageView
    private lateinit var btnFilterSliders: ImageView
    private lateinit var inputSearchArea: EditText

    private lateinit var chipAllMap: TextView
    private lateinit var chipLost: TextView
    private lateinit var chipFound: TextView
    private lateinit var chipToday: TextView
    private lateinit var btnGpsTarget: FrameLayout

    private lateinit var cardLaptopItem: MaterialCardView
    private lateinit var imgLaptopThumb: ImageView
    private lateinit var textItemTitle: TextView
    private lateinit var textItemLocation: TextView
    private lateinit var tagFoundStatus: TextView

    // Selected item tracking
    private var currentId = ""
    private var currentTitle = ""
    private var currentLocation = ""
    private var currentStatus = ""
    private var currentDesc = ""
    private var currentUserId = ""

    // Pinned location tracking
    private var isLocationPinned = false
    private var pinnedLat: Double = 6.0367
    private var pinnedLng: Double = 80.2170
    private var isPickerMode = false

    private var targetLat: Double = 0.0
    private var targetLng: Double = 0.0
    private var targetItemId: String = ""

    private val markerReportsMap = java.util.concurrent.ConcurrentHashMap<String, com.group3.lostandfound.model.ItemReport>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        isPickerMode = arguments?.getBoolean(ARG_IS_PICKER_MODE, false) ?: false
        targetLat = arguments?.getDouble("arg_target_lat", 0.0) ?: 0.0
        targetLng = arguments?.getDouble("arg_target_lng", 0.0) ?: 0.0
        targetItemId = arguments?.getString("arg_target_item_id", "") ?: ""

        initViews(view)
        setupLeafletWebView()
        setupListeners()

        if (isPickerMode) {
            view.findViewById<View>(R.id.bottomNav)?.visibility = View.GONE
            cardLaptopItem.visibility = View.VISIBLE
            textItemTitle.text = "Tap on map to select location"
            textItemLocation.text = "Tap anywhere on map to pin location"
            tagFoundStatus.text = "SELECT"
            tagFoundStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
            imgLaptopThumb.setImageResource(R.drawable.ic_location)
        } else {
            cardLaptopItem.visibility = View.GONE
            setupBottomNav(view, "explore")
        }
    }

    private fun initViews(view: View) {
        webViewLeafletMap = view.findViewById(R.id.webViewLeafletMap)
        btnClose = view.findViewById(R.id.btnClose)
        btnListView = view.findViewById(R.id.btnListView)
        btnFilterSliders = view.findViewById(R.id.btnFilterSliders)
        inputSearchArea = view.findViewById(R.id.inputSearchArea)

        chipAllMap = view.findViewById(R.id.chipAllMap)
        chipLost = view.findViewById(R.id.chipLost)
        chipFound = view.findViewById(R.id.chipFound)
        chipToday = view.findViewById(R.id.chipToday)
        btnGpsTarget = view.findViewById(R.id.btnGpsTarget)

        cardLaptopItem = view.findViewById(R.id.cardLaptopItem)
        imgLaptopThumb = view.findViewById(R.id.imgLaptopThumb)
        textItemTitle = view.findViewById(R.id.textItemTitle)
        textItemLocation = view.findViewById(R.id.textItemLocation)
        tagFoundStatus = view.findViewById(R.id.tagFoundStatus)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupLeafletWebView() {
        val settings = webViewLeafletMap.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.allowFileAccess = true
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true

        webViewLeafletMap.webChromeClient = WebChromeClient()
        webViewLeafletMap.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                loadLiveFirebaseMarkers()
            }
        }

        webViewLeafletMap.addJavascriptInterface(LeafletBridge(), "AndroidInterface")

        val htmlContent = buildLeafletHtml()
        webViewLeafletMap.loadDataWithBaseURL("https://leafletjs.com/", htmlContent, "text/html", "UTF-8", null)
    }

    private fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }

    private fun resolveLocationCoordinates(location: String, id: String): Pair<Double, Double> {
        val lower = location.lowercase(java.util.Locale.US).trim()
        val coordMatch = Regex("""(-?\d+\.\d+)[,\s]+(-?\d+\.\d+)""").find(lower)
        if (coordMatch != null) {
            val lat = coordMatch.groupValues[1].toDoubleOrNull()
            val lng = coordMatch.groupValues[2].toDoubleOrNull()
            if (lat != null && lng != null && lat != 0.0 && lng != 0.0) {
                return Pair(lat, lng)
            }
        }
        val offset = (id.hashCode() % 15) * 0.0015
        return when {
            lower.contains("liberty") || lower.contains("kollupitiya") || lower.contains("colombo 03") -> Pair(6.8974 + offset, 79.8562 + offset)
            lower.contains("bambalapitiya") || lower.contains("colombo 04") || lower.contains("majestic") -> Pair(6.8884 + offset, 79.8587 + offset)
            lower.contains("vihara") || lower.contains("victoria park") -> Pair(6.9140 + offset, 79.8630 + offset)
            lower.contains("independence") || lower.contains("colombo 07") || lower.contains("cinnamon") -> Pair(6.9044 + offset, 79.8679 + offset)
            lower.contains("fort") || lower.contains("colombo 01") || lower.contains("pettah") -> Pair(6.9344 + offset, 79.8428 + offset)
            lower.contains("galle face") -> Pair(6.9271 + offset, 79.8433 + offset)
            lower.contains("borella") || lower.contains("colombo 08") -> Pair(6.9147 + offset, 79.8776 + offset)
            lower.contains("dehiwala") || lower.contains("mount lavinia") -> Pair(6.8402 + offset, 79.8712 + offset)
            lower.contains("nugegoda") || lower.contains("kohuwala") -> Pair(6.8724 + offset, 79.8997 + offset)
            lower.contains("rajagiriya") || lower.contains("kotte") -> Pair(6.9090 + offset, 79.8970 + offset)
            lower.contains("negombo") || lower.contains("katunayake") -> Pair(7.2008 + offset, 79.8736 + offset)
            lower.contains("kandy") || lower.contains("peradeniya") -> Pair(7.2906 + offset, 80.6337 + offset)
            lower.contains("unawatuna") || lower.contains("galle fort") || lower.contains("karapitiya") || lower.contains("galle") -> Pair(6.0367 + offset, 80.2170 + offset)
            lower.contains("hikkaduwa") -> Pair(6.1406 + offset, 80.1011 + offset)
            lower.contains("matara") || lower.contains("mirissa") -> Pair(5.9549 + offset, 80.5550 + offset)
            lower.contains("jaffna") -> Pair(9.6615 + offset, 80.0255 + offset)
            lower.contains("kurunegala") -> Pair(7.4863 + offset, 80.3623 + offset)
            lower.contains("anuradhapura") -> Pair(8.3114 + offset, 80.4037 + offset)
            lower.contains("badulla") || lower.contains("ella") -> Pair(6.9934 + offset, 81.0550 + offset)
            lower.contains("batticaloa") -> Pair(7.7102 + offset, 81.6924 + offset)
            lower.contains("trincomalee") -> Pair(8.5874 + offset, 81.2152 + offset)
            else -> Pair(6.8974 + offset, 79.8562 + offset)
        }
    }

    private fun loadLiveFirebaseMarkers() {
        val dbRef = FirebaseDatabase.getInstance(FirebaseManager.DATABASE_URL).getReference("items")
        dbRef.limitToLast(50).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded || view == null) return
                markerReportsMap.clear()
                val jsBuilder = StringBuilder("items = [];\n")

                for (child in snapshot.children) {
                    val id = child.key ?: continue
                    val title = child.child("title").getValue(String::class.java) ?: continue
                    val status = child.child("status").getValue(String::class.java) ?: "LOST"
                    val desc = child.child("description").getValue(String::class.java) ?: ""
                    val category = child.child("category").getValue(String::class.java) ?: "General"
                    val location = child.child("location").getValue(String::class.java) ?: "$category Area"
                    val lat = child.child("latitude").getValue(Double::class.java) ?: 0.0
                    val lng = child.child("longitude").getValue(Double::class.java) ?: 0.0
                    val imageUrl = child.child("imageUrl").getValue(String::class.java) ?: ""

                    // Read the permanent item type (LOST/FOUND). Falls back to deriving it
                    // from status so that admin approval (status -> "APPROVED") never changes
                    // the pin color — Lost items stay red, Found items stay blue.
                    val storedItemType = child.child("itemType").getValue(String::class.java)
                    val itemType = when {
                        storedItemType.equals("LOST", ignoreCase = true) -> "LOST"
                        storedItemType.equals("FOUND", ignoreCase = true) -> "FOUND"
                        status.equals("LOST", ignoreCase = true) -> "LOST"
                        status.equals("FOUND", ignoreCase = true) -> "FOUND"
                        else -> "LOST" // safe default
                    }

                    val resolved = if (lat != 0.0 && lng != 0.0) Pair(lat, lng) else resolveLocationCoordinates(location, id)
                    val effectiveLat = resolved.first
                    val effectiveLng = resolved.second

                    val report = com.group3.lostandfound.model.ItemReport(
                        itemId = id,
                        title = title,
                        description = desc,
                        category = category,
                        status = status,
                        location = location,
                        latitude = effectiveLat,
                        longitude = effectiveLng,
                        imageUrl = imageUrl
                    )
                    markerReportsMap[id] = report

                    val distKm = calculateDistanceKm(6.0367, 80.2170, effectiveLat, effectiveLng)
                    val distStr = if (distKm < 0.2) "Near you in Galle" else String.format(java.util.Locale.US, "%.1f km from Galle", distKm)

                    val safeTitle = title.replace("'", "\\'").replace("\n", " ").take(40)
                    val safeLoc = location.replace("'", "\\'").replace("\n", " ").take(30)
                    jsBuilder.append("items.push({id:'$id',title:'$safeTitle',location:'$safeLoc',status:'$status',itemType:'$itemType',lat:$effectiveLat,lng:$effectiveLng,dist:'$distStr'});\n")
                }

                jsBuilder.append("renderMarkers('all');\n")
                if (targetLat != 0.0 && targetLng != 0.0) {
                    jsBuilder.append("animateToTargetItem($targetLat, $targetLng, '$targetItemId');\n")
                } else {
                    jsBuilder.append("map.setView([6.0367, 80.2170], 13);\n")
                }
                webViewLeafletMap.evaluateJavascript(jsBuilder.toString(), null)

                // If targetItemId is specified and was not in snapshot, fetch it directly
                if (targetItemId.isNotEmpty() && !markerReportsMap.containsKey(targetItemId)) {
                    dbRef.child(targetItemId).addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(targetSnap: DataSnapshot) {
                            if (!isAdded || view == null) return
                            val tTitle = targetSnap.child("title").getValue(String::class.java) ?: "Selected Item"
                            val tStatus = targetSnap.child("status").getValue(String::class.java) ?: "LOST"
                            val tDesc = targetSnap.child("description").getValue(String::class.java) ?: ""
                            val tCat = targetSnap.child("category").getValue(String::class.java) ?: "General"
                            val tLoc = targetSnap.child("location").getValue(String::class.java) ?: "Location"
                            val tImg = targetSnap.child("imageUrl").getValue(String::class.java) ?: ""
                            val tLatVal = targetSnap.child("latitude").getValue(Double::class.java) ?: 0.0
                            val tLngVal = targetSnap.child("longitude").getValue(Double::class.java) ?: 0.0
                            val tResolved = if (targetLat != 0.0 && targetLng != 0.0) Pair(targetLat, targetLng)
                                            else if (tLatVal != 0.0 && tLngVal != 0.0) Pair(tLatVal, tLngVal)
                                            else resolveLocationCoordinates(tLoc, targetItemId)
                            val tLat = tResolved.first
                            val tLng = tResolved.second

                            val tReport = com.group3.lostandfound.model.ItemReport(
                                itemId = targetItemId,
                                title = tTitle,
                                description = tDesc,
                                category = tCat,
                                status = tStatus,
                                location = tLoc,
                                latitude = tLat,
                                longitude = tLng,
                                imageUrl = tImg
                            )
                            markerReportsMap[targetItemId] = tReport
                            val distKm = calculateDistanceKm(6.0367, 80.2170, tLat, tLng)
                            val distStr = if (distKm < 0.2) "Near you in Galle" else String.format(java.util.Locale.US, "%.1f km from Galle", distKm)

                            val safeTitle = tTitle.replace("'", "\\'").replace("\n", " ").take(40)
                            val safeLoc = tLoc.replace("'", "\\'").replace("\n", " ").take(30)
                            val singleJs = """
                                items.push({id:'$targetItemId',title:'$safeTitle',location:'$safeLoc',status:'$tStatus',itemType:'$tStatus',lat:$tLat,lng:$tLng,dist:'$distStr'});
                                renderMarkers('all');
                                animateToTargetItem($tLat, $tLng, '$targetItemId');
                            """.trimIndent()
                            webViewLeafletMap.evaluateJavascript(singleJs, null)
                        }

                        override fun onCancelled(err: DatabaseError) {}
                    })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Ignore silent network failure
            }
        })
    }

    private fun setupListeners() {
        btnClose.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnListView.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.screenBox, ExploreFragment.newInstance())
                .addToBackStack(null)
                .commit()
        }

        btnFilterSliders.setOnClickListener {
            Toast.makeText(requireContext(), "Filter by distance & categories", Toast.LENGTH_SHORT).show()
        }

        chipAllMap.setOnClickListener {
            highlightChip(chipAllMap)
            filterLeafletMarkers("all")
        }

        chipLost.setOnClickListener {
            highlightChip(chipLost)
            filterLeafletMarkers("LOST")
        }

        chipFound.setOnClickListener {
            highlightChip(chipFound)
            filterLeafletMarkers("FOUND")
        }

        chipToday.setOnClickListener {
            highlightChip(chipToday)
            filterLeafletMarkers("all")
            Toast.makeText(requireContext(), "Showing items reported today", Toast.LENGTH_SHORT).show()
        }

        btnGpsTarget.setOnClickListener {
            webViewLeafletMap.evaluateJavascript("recenterMap();", null)
            Toast.makeText(requireContext(), "Centered to Galle, Sri Lanka (6.0367° N, 80.2170° E)", Toast.LENGTH_SHORT).show()
        }

        cardLaptopItem.setOnClickListener {
            if (isPickerMode) {
                if (isLocationPinned) {
                    confirmPickedLocation(pinnedLat, pinnedLng)
                } else {
                    Toast.makeText(requireContext(), "Tap on the map first to pin your location", Toast.LENGTH_SHORT).show()
                }
            } else {
                if (isLocationPinned) {
                    navigateToCreateReport(pinnedLat, pinnedLng)
                } else if (currentId.isNotEmpty() || currentTitle.isNotEmpty()) {
                    val currentReport = markerReportsMap[currentId]
                    parentFragmentManager.beginTransaction()
                        .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                        .replace(
                            R.id.screenBox,
                            DetailsFragment.newInstance(
                                itemId = currentId,
                                title = currentTitle,
                                desc = currentDesc,
                                status = currentStatus,
                                location = currentLocation,
                                imageUrl = currentReport?.imageUrl.orEmpty(),
                                reporterUserId = currentUserId,
                                authorName = currentReport?.authorName,
                                latitude = currentReport?.latitude ?: targetLat,
                                longitude = currentReport?.longitude ?: targetLng
                            )
                        )
                        .addToBackStack(null)
                        .commit()
                }
            }
        }
    }

    private fun highlightChip(activeChip: TextView) {
        val allChips = listOf(chipAllMap, chipLost, chipFound, chipToday)
        for (chip in allChips) {
            if (chip == activeChip) {
                chip.setBackgroundResource(R.drawable.bg_chip_selected)
                chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            } else {
                chip.setBackgroundResource(R.drawable.bg_chip_unselected)
                chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
            }
        }
    }

    private fun filterLeafletMarkers(type: String) {
        webViewLeafletMap.evaluateJavascript("filterItems('$type');", null)
    }

    inner class LeafletBridge {
        @JavascriptInterface
        fun onMarkerClicked(id: String) {
            activity?.runOnUiThread {
                val report = markerReportsMap[id]
                isLocationPinned = false
                currentId = id
                currentTitle = report?.title ?: "Reported Item"
                currentLocation = report?.location ?: "Location"
                currentStatus = report?.status ?: "LOST"
                currentDesc = report?.description ?: ""
                currentUserId = report?.userId ?: ""

                cardLaptopItem.visibility = View.VISIBLE
                textItemTitle.text = currentTitle
                
                val lat = report?.latitude ?: 0.0
                val lng = report?.longitude ?: 0.0
                if (lat != 0.0 && lng != 0.0) {
                    val distKm = calculateDistanceKm(6.0367, 80.2170, lat, lng)
                    val distStr = if (distKm < 0.2) "Near you in Galle" else String.format(java.util.Locale.US, "%.1f km from Galle", distKm)
                    textItemLocation.text = "$currentLocation · $distStr"
                } else {
                    textItemLocation.text = currentLocation
                }
                tagFoundStatus.text = currentStatus

                if (currentStatus.equals("LOST", ignoreCase = true)) {
                    tagFoundStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.tag_lost_color))
                    tagFoundStatus.setBackgroundResource(R.drawable.bg_tag_lost_outlined)
                } else {
                    tagFoundStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.tag_found_color))
                    tagFoundStatus.setBackgroundResource(R.drawable.bg_tag_found_outlined)
                }

                val imgSource = report?.imageUrl ?: ""
                if (imgSource.isNotEmpty()) {
                    com.group3.lostandfound.util.ImageLoader.load(
                        imgLaptopThumb,
                        imgSource,
                        R.drawable.ic_item_placeholder,
                        viewLifecycleOwner.lifecycleScope
                    )
                } else {
                    imgLaptopThumb.setImageResource(R.drawable.ic_item_placeholder)
                }
            }
        }

        @JavascriptInterface
        fun onMarkerClicked(id: String, title: String, location: String, status: String, desc: String, imageUrl: String) {
            onMarkerClicked(id)
        }

        @JavascriptInterface
        fun onLocationPinned(lat: Double, lng: Double) {
            activity?.runOnUiThread {
                isLocationPinned = true
                pinnedLat = lat
                pinnedLng = lng
                currentTitle = if (isPickerMode) "Location Selected" else "Pinned Location"
                currentLocation = String.format(java.util.Locale.US, "%.4f, %.4f", lat, lng)
                currentStatus = if (isPickerMode) "SELECTED" else "PINNED"
                currentDesc = "Location pinned at coordinates ($pinnedLat, $pinnedLng)."

                textItemTitle.text = if (isPickerMode) "Location Selected" else "Pinned Location"
                textItemLocation.text = String.format(java.util.Locale.US, "%.4f° N, %.4f° E · Tap to confirm", lat, lng)
                tagFoundStatus.text = if (isPickerMode) "USE THIS" else "PINNED"
                tagFoundStatus.setTextColor(ContextCompat.getColor(requireContext(), if (isPickerMode) R.color.tag_found_color else R.color.tag_lost_color))
                tagFoundStatus.setBackgroundResource(if (isPickerMode) R.drawable.bg_tag_found_outlined else R.drawable.bg_tag_lost_outlined)
                imgLaptopThumb.setImageResource(R.drawable.ic_location)

                com.group3.lostandfound.util.ModernToast.show(
                    requireContext(),
                    if (isPickerMode) "Location pinned! Tap card or popup to confirm." else "Location pinned. Tap preview card below to report item here.",
                    view
                )
            }
        }

        @JavascriptInterface
        fun onUsePinnedLocation(lat: Double, lng: Double) {
            activity?.runOnUiThread {
                if (isPickerMode) {
                    confirmPickedLocation(lat, lng)
                } else {
                    navigateToCreateReport(lat, lng)
                }
            }
        }
    }

    private fun confirmPickedLocation(lat: Double, lng: Double) {
        val locationLabel = String.format(java.util.Locale.US, "Pinned Location (%.4f, %.4f)", lat, lng)
        parentFragmentManager.setFragmentResult(
            REQUEST_KEY_LOCATION,
            Bundle().apply {
                putDouble(KEY_LATITUDE, lat)
                putDouble(KEY_LONGITUDE, lng)
                putString(KEY_LOCATION_NAME, locationLabel)
            }
        )
        parentFragmentManager.popBackStack()
    }

    private fun navigateToCreateReport(lat: Double, lng: Double) {
        val locationLabel = String.format(java.util.Locale.US, "Pinned (%.4f, %.4f)", lat, lng)
        parentFragmentManager.beginTransaction()
            .replace(
                R.id.screenBox,
                CreateReportFragment.newInstance(
                    isLost = true,
                    latitude = lat,
                    longitude = lng,
                    locationName = locationLabel
                )
            )
            .addToBackStack(null)
            .commit()
    }

    private fun buildLeafletHtml(): String {
        return """
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
    <style>
        html, body, #map {
            width: 100%;
            height: 100%;
            margin: 0;
            padding: 0;
            background: #F7F5F0;
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
        }
        .custom-pin {
            display: flex;
            align-items: center;
            justify-content: center;
            width: 36px;
            height: 36px;
            border-radius: 50%;
            color: #ffffff;
            font-weight: bold;
            box-shadow: 0 4px 10px rgba(0,0,0,0.3);
            border: 2px solid #ffffff;
            cursor: pointer;
            transition: transform 0.2s;
        }
        .custom-pin:active {
            transform: scale(1.15);
        }
        .pin-lost {
            background: #C1121F;
        }
        .pin-found {
            background: #1565C0;
        }
        .pin-pinned {
            background: #C1121F;
            border: 3px solid #ffffff;
            box-shadow: 0 4px 14px rgba(193,18,31,0.7);
            animation: pulse-pin 1.2s infinite;
        }
        @keyframes user-pulse {
            0% { transform: scale(1); opacity: 0.8; }
            50% { transform: scale(1.6); opacity: 0.3; }
            100% { transform: scale(1); opacity: 0.8; }
        }
        @keyframes pulse-pin {
            0% { transform: scale(1); }
            50% { transform: scale(1.18); }
            100% { transform: scale(1); }
        }
        .leaflet-popup-content-wrapper {
            border-radius: 12px;
            padding: 2px;
            box-shadow: 0 4px 14px rgba(0,0,0,0.2);
        }
        .popup-box {
            font-size: 13px;
            line-height: 1.4;
            color: #003049;
            padding: 4px 6px;
        }
        .popup-badge {
            display: inline-block;
            font-size: 10px;
            font-weight: 800;
            padding: 2px 6px;
            border-radius: 4px;
            margin-bottom: 4px;
        }
        .badge-lost {
            background: #FFF1F2;
            color: #C1121F;
        }
        .badge-found {
            background: #E3F0FC;
            color: #1565C0;
        }
    </style>
</head>
<body>
    <div id="map"></div>
    <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
    <script>
        // Current User Location: Galle, Sri Lanka (6.0367° N, 80.2170° E)
        var userLat = 6.0367;
        var userLng = 80.2170;

        // Initialize Leaflet map centered at user's current location in Galle
        var map = L.map('map', {
            zoomControl: false,
            attributionControl: false
        }).setView([userLat, userLng], 13);

        // OpenStreetMap tile layer
        L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
            maxZoom: 19
        }).addTo(map);

        // Current User Location Marker (Galle)
        var userIconHtml = '<div style="position:relative;width:28px;height:28px;">' +
            '<div style="position:absolute;width:28px;height:28px;border-radius:50%;background:rgba(2,132,199,0.35);animation:user-pulse 1.8s infinite;"></div>' +
            '<div style="position:absolute;top:5px;left:5px;width:18px;height:18px;border-radius:50%;background:#0284C7;border:3px solid #FFFFFF;box-shadow:0 3px 10px rgba(0,0,0,0.45);display:flex;align-items:center;justify-content:center;">' +
            '<div style="width:6px;height:6px;border-radius:50%;background:#FFFFFF;"></div>' +
            '</div>' +
            '</div>';

        var userIcon = L.divIcon({
            className: '',
            html: userIconHtml,
            iconSize: [28, 28],
            iconAnchor: [14, 14],
            popupAnchor: [0, -16]
        });

        var userMarker = L.marker([userLat, userLng], { icon: userIcon, zIndexOffset: 2000 }).addTo(map);
        userMarker.bindPopup('<div class="popup-box"><div class="popup-badge" style="background:#E0F2FE;color:#0284C7;font-weight:bold;">YOUR CURRENT LOCATION</div><div><strong>Galle, Sri Lanka</strong></div><div style="font-size:11px;color:#64748B;">6.0367° N, 80.2170° E</div></div>');

        // Item markers dataset
        var items = [];

        var markersGroup = L.layerGroup().addTo(map);

        function renderMarkers(filter) {
            markersGroup.clearLayers();
            items.forEach(function(item) {
                // Use itemType (LOST/FOUND) for filtering so chips work correctly even
                // after admin approval changes status to "APPROVED"
                var type = item.itemType || item.status;
                if (filter === 'all' || type === filter) {
                    // Pin color is always driven by itemType — never by status —
                    // so admin approval cannot change the pin color
                    var pinColor = type === 'LOST' ? '#C1121F' : '#1565C0';
                    var iconSvg = '<svg width="30" height="42" viewBox="0 0 30 42" fill="none" xmlns="http://www.w3.org/2000/svg" style="filter: drop-shadow(0 3px 6px rgba(0,0,0,0.35)); cursor: pointer;">' +
                        '<path d="M15 0C6.71573 0 0 6.71573 0 15C0 26.25 15 42 15 42C15 42 30 26.25 30 15C30 6.71573 23.2843 0 15 0Z" fill="' + pinColor + '"/>' +
                        '<circle cx="15" cy="15" r="5.5" fill="#FFFFFF"/>' +
                        '</svg>';

                    var customIcon = L.divIcon({
                        className: '',
                        html: iconSvg,
                        iconSize: [30, 42],
                        iconAnchor: [15, 42],
                        popupAnchor: [0, -38]
                    });

                    var marker = L.marker([item.lat, item.lng], { icon: customIcon });

                    var badgeClass = item.status === 'LOST' ? 'badge-lost' : 'badge-found';
                    var distInfo = item.dist ? '<div style="color: #0284C7; font-size: 11px; font-weight: 600; margin-top: 3px;">📍 ' + item.dist + '</div>' : '';
                    var popupHtml = '<div class="popup-box">' +
                        '<div class="popup-badge ' + badgeClass + '">' + item.status + '</div>' +
                        '<div><strong>' + item.title + '</strong></div>' +
                        '<div style="color: #64748B; font-size: 11px;">' + item.location + '</div>' +
                        distInfo +
                        '</div>';
                    marker.bindPopup(popupHtml);

                    marker.on('click', function() {
                        if (window.AndroidInterface) {
                            window.AndroidInterface.onMarkerClicked(item.id);
                        }
                    });

                    markersGroup.addLayer(marker);
                }
            });
        }

        renderMarkers('all');

        var pinnedMarker = null;

        // Interactive Map Pinning: Tapping anywhere drops a pin and notifies Android Interface
        map.on('click', function(e) {
            var lat = e.latlng.lat;
            var lng = e.latlng.lng;

            if (pinnedMarker) {
                map.removeLayer(pinnedMarker);
            }

            var pinnedSvg = '<svg width="32" height="44" viewBox="0 0 30 42" fill="none" xmlns="http://www.w3.org/2000/svg" style="filter: drop-shadow(0 4px 8px rgba(0,0,0,0.45)); cursor: pointer;">' +
                '<path d="M15 0C6.71573 0 0 6.71573 0 15C0 26.25 15 42 15 42C15 42 30 26.25 30 15C30 6.71573 23.2843 0 15 0Z" fill="#C1121F"/>' +
                '<circle cx="15" cy="15" r="5.5" fill="#FFFFFF"/>' +
                '</svg>';

            var pinIcon = L.divIcon({
                className: '',
                html: pinnedSvg,
                iconSize: [32, 44],
                iconAnchor: [16, 44],
                popupAnchor: [0, -40]
            });

            pinnedMarker = L.marker([lat, lng], { icon: pinIcon, draggable: true }).addTo(map);

            var popupBtnText = "${if (isPickerMode) "✓ Use This Location" else "+ Report Item Here"}";
            var popupSubtext = "${if (isPickerMode) "Location pinned. Tap below to confirm location." else "Location pinned. Tap below to create report."}";
            var popupBadge = "${if (isPickerMode) "SELECTED LOCATION" else "PINNED LOCATION"}";

            var popupHtml = '<div class="popup-box" style="text-align:center;">' +
                '<div class="popup-badge" style="background:#FFF1F2;color:#C1121F;font-weight:bold;">' + popupBadge + '</div>' +
                '<div style="font-weight:bold;margin:4px 0;font-size:13px;">' + lat.toFixed(4) + '° N, ' + lng.toFixed(4) + '° E</div>' +
                '<div style="font-size:11px;color:#64748B;margin-bottom:8px;">' + popupSubtext + '</div>' +
                '<button onclick="window.AndroidInterface.onUsePinnedLocation(' + lat + ', ' + lng + ')" ' +
                'style="background:#003049;color:#FFFFFF;border:none;border-radius:8px;padding:8px 12px;font-size:12px;font-weight:bold;cursor:pointer;width:100%;">' +
                popupBtnText + '</button>' +
                '</div>';

            pinnedMarker.bindPopup(popupHtml).openPopup();

            pinnedMarker.on('dragend', function(evt) {
                var pos = evt.target.getLatLng();
                if (window.AndroidInterface) {
                    window.AndroidInterface.onLocationPinned(pos.lat, pos.lng);
                }
            });

            if (window.AndroidInterface) {
                window.AndroidInterface.onLocationPinned(lat, lng);
            }
        });

        function addMapItem(id, title, location, status, lat, lng, desc, imageUrl) {
            items.push({
                id: id,
                title: title,
                location: location,
                status: status,
                lat: lat,
                lng: lng,
                desc: desc,
                imageUrl: imageUrl
            });
            renderMarkers('all');
        }

        function filterItems(type) {
            renderMarkers(type);
        }

        function animateToTargetItem(tLat, tLng, itemId) {
            setTimeout(function() {
                map.flyTo([tLat, tLng], 16, { duration: 2.2 });
                setTimeout(function() {
                    markersGroup.eachLayer(function(layer) {
                        var pos = layer.getLatLng();
                        if (Math.abs(pos.lat - tLat) < 0.001 && Math.abs(pos.lng - tLng) < 0.001) {
                            layer.openPopup();
                        }
                    });
                    if (window.AndroidInterface && itemId) {
                        window.AndroidInterface.onMarkerClicked(itemId);
                    }
                }, 2300);
            }, 500);
        }

        function recenterMap() {
            map.flyTo([6.0367, 80.2170], 14, { duration: 1.2 });
            setTimeout(function() {
                if (typeof userMarker !== 'undefined') {
                    userMarker.openPopup();
                }
            }, 1300);
        }
    </script>
</body>
</html>
        """.trimIndent()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        webViewLeafletMap.destroy()
    }

    companion object {
        const val ARG_IS_PICKER_MODE = "arg_is_picker_mode"
        const val REQUEST_KEY_LOCATION = "request_key_location"
        const val KEY_LATITUDE = "key_latitude"
        const val KEY_LONGITUDE = "key_longitude"
        const val KEY_LOCATION_NAME = "key_location_name"

        fun newInstance(
            isPickerMode: Boolean = false,
            targetLat: Double = 0.0,
            targetLng: Double = 0.0,
            targetItemId: String = ""
        ): MapFragment {
            val fragment = MapFragment()
            val args = Bundle().apply {
                putBoolean(ARG_IS_PICKER_MODE, isPickerMode)
                putDouble("arg_target_lat", targetLat)
                putDouble("arg_target_lng", targetLng)
                putString("arg_target_item_id", targetItemId)
            }
            fragment.arguments = args
            return fragment
        }
    }
}
