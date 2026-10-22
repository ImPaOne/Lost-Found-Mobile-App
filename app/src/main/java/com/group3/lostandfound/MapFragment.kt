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
    private var pinnedLat: Double = 6.9140
    private var pinnedLng: Double = 79.8630
    private var isPickerMode = false

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

                    if (lat != 0.0 && lng != 0.0) {
                        val report = com.group3.lostandfound.model.ItemReport(
                            itemId = id,
                            title = title,
                            description = desc,
                            category = category,
                            status = status,
                            location = location,
                            latitude = lat,
                            longitude = lng,
                            imageUrl = imageUrl
                        )
                        markerReportsMap[id] = report

                        val safeTitle = title.replace("'", "\\'").replace("\n", " ").take(40)
                        val safeLoc = location.replace("'", "\\'").replace("\n", " ").take(30)
                        jsBuilder.append("items.push({id:'$id',title:'$safeTitle',location:'$safeLoc',status:'$status',lat:$lat,lng:$lng});\n")
                    }
                }

                jsBuilder.append("renderMarkers('all');")
                webViewLeafletMap.evaluateJavascript(jsBuilder.toString(), null)
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
            Toast.makeText(requireContext(), "Centered to Colombo, Sri Lanka", Toast.LENGTH_SHORT).show()
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
                    parentFragmentManager.beginTransaction()
                        .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                        .replace(
                            R.id.screenBox,
                            DetailsFragment.newInstance(
                                title = currentTitle,
                                desc = currentDesc,
                                status = currentStatus,
                                location = currentLocation,
                                imageResId = R.drawable.ic_item_placeholder,
                                itemId = currentId,
                                reporterUserId = currentUserId
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
                textItemLocation.text = currentLocation
                tagFoundStatus.text = currentStatus

                if (currentStatus.equals("LOST", ignoreCase = true)) {
                    tagFoundStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.tag_lost_color))
                } else {
                    tagFoundStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.tag_found_color))
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
            background: #D43838;
        }
        .pin-found {
            background: #3B7A57;
        }
        .pin-pinned {
            background: #E65100;
            border: 3px solid #ffffff;
            box-shadow: 0 4px 14px rgba(230,81,0,0.7);
            animation: pulse-pin 1.2s infinite;
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
            color: #3A2D28;
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
            background: #FFE8E8;
            color: #D43838;
        }
        .badge-found {
            background: #E8F5E9;
            color: #2E7D32;
        }
    </style>
</head>
<body>
    <div id="map"></div>
    <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
    <script>
        // Initialize Leaflet map centered on Colombo, Sri Lanka
        var map = L.map('map', {
            zoomControl: false,
            attributionControl: false
        }).setView([6.9140, 79.8630], 13);

        // OpenStreetMap tile layer
        L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
            maxZoom: 19
        }).addTo(map);

        // Item markers dataset
        var items = [];

        var markersGroup = L.layerGroup().addTo(map);

        function renderMarkers(filter) {
            markersGroup.clearLayers();
            items.forEach(function(item) {
                if (filter === 'all' || item.status === filter) {
                    var pinClass = item.status === 'LOST' ? 'pin-lost' : 'pin-found';
                    var iconHtml = '<div class="custom-pin ' + pinClass + '">' +
                        (item.status === 'LOST' ? '!' : '✓') +
                        '</div>';
                    var customIcon = L.divIcon({
                        className: '',
                        html: iconHtml,
                        iconSize: [36, 36],
                        iconAnchor: [18, 18]
                    });

                    var marker = L.marker([item.lat, item.lng], { icon: customIcon });

                    var badgeClass = item.status === 'LOST' ? 'badge-lost' : 'badge-found';
                    var popupHtml = '<div class="popup-box">' +
                        '<div class="popup-badge ' + badgeClass + '">' + item.status + '</div>' +
                        '<div><strong>' + item.title + '</strong></div>' +
                        '<div style="color: #7D6F67; font-size: 11px;">' + item.location + '</div>' +
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

            var pinIcon = L.divIcon({
                className: '',
                html: '<div class="custom-pin pin-pinned">&bull;</div>',
                iconSize: [40, 40],
                iconAnchor: [20, 20]
            });

            pinnedMarker = L.marker([lat, lng], { icon: pinIcon, draggable: true }).addTo(map);

            var popupBtnText = "${if (isPickerMode) "✓ Use This Location" else "+ Report Item Here"}";
            var popupSubtext = "${if (isPickerMode) "Location pinned. Tap below to confirm location." else "Location pinned. Tap below to create report."}";
            var popupBadge = "${if (isPickerMode) "SELECTED LOCATION" else "PINNED LOCATION"}";

            var popupHtml = '<div class="popup-box" style="text-align:center;">' +
                '<div class="popup-badge" style="background:#FFF3E0;color:#E65100;font-weight:bold;">' + popupBadge + '</div>' +
                '<div style="font-weight:bold;margin:4px 0;font-size:13px;">' + lat.toFixed(4) + '° N, ' + lng.toFixed(4) + '° E</div>' +
                '<div style="font-size:11px;color:#7D6F67;margin-bottom:8px;">' + popupSubtext + '</div>' +
                '<button onclick="window.AndroidInterface.onUsePinnedLocation(' + lat + ', ' + lng + ')" ' +
                'style="background:#2B2625;color:#FFFFFF;border:none;border-radius:8px;padding:6px 12px;font-size:12px;font-weight:bold;cursor:pointer;width:100%;">' +
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

        function recenterMap() {
            map.flyTo([6.9140, 79.8630], 14, { duration: 1.2 });
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

        fun newInstance(isPickerMode: Boolean = false): MapFragment {
            val fragment = MapFragment()
            val args = Bundle().apply {
                putBoolean(ARG_IS_PICKER_MODE, isPickerMode)
            }
            fragment.arguments = args
            return fragment
        }
    }
}
