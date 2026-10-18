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
import com.google.android.material.card.MaterialCardView

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
    private var currentTitle = "Red laptop sleeve"
    private var currentLocation = "Independence Square"
    private var currentStatus = "FOUND"
    private var currentDesc = "Padded red neoprene laptop sleeve found on park bench near walking path."
    private var currentImageRes = R.drawable.img_wallet_details

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupLeafletWebView()
        setupListeners()
        setupBottomNav(view, "explore")
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
            }
        }

        webViewLeafletMap.addJavascriptInterface(LeafletBridge(), "AndroidInterface")

        val htmlContent = buildLeafletHtml()
        webViewLeafletMap.loadDataWithBaseURL("https://leafletjs.com/", htmlContent, "text/html", "UTF-8", null)
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
            parentFragmentManager.beginTransaction()
                .replace(
                    R.id.screenBox,
                    DetailsFragment.newInstance(
                        title = currentTitle,
                        desc = currentDesc,
                        status = currentStatus,
                        location = currentLocation,
                        imageResId = currentImageRes
                    )
                )
                .addToBackStack(null)
                .commit()
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
        fun onMarkerClicked(id: String, title: String, location: String, status: String, desc: String, imageType: String) {
            activity?.runOnUiThread {
                currentTitle = title
                currentLocation = location
                currentStatus = status
                currentDesc = desc

                when (imageType) {
                    "wallet" -> {
                        currentImageRes = R.drawable.img_wallet
                        imgLaptopThumb.setImageResource(R.drawable.img_wallet)
                    }
                    "passport" -> {
                        currentImageRes = R.drawable.img_passport
                        imgLaptopThumb.setImageResource(R.drawable.img_passport)
                    }
                    "cat" -> {
                        currentImageRes = R.drawable.img_tabby_cat
                        imgLaptopThumb.setImageResource(R.drawable.img_tabby_cat)
                    }
                    "airpods" -> {
                        currentImageRes = R.drawable.img_airpods_bench
                        imgLaptopThumb.setImageResource(R.drawable.img_airpods_bench)
                    }
                    else -> {
                        currentImageRes = R.drawable.img_wallet_details
                        imgLaptopThumb.setImageResource(R.drawable.img_wallet_details)
                    }
                }

                textItemTitle.text = title
                textItemLocation.text = location
                tagFoundStatus.text = status

                if (status == "LOST") {
                    tagFoundStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.tag_lost_color))
                } else {
                    tagFoundStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.tag_found_color))
                }
            }
        }
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
        var items = [
            {
                id: '1',
                title: 'Red laptop sleeve',
                location: 'Independence Square',
                status: 'FOUND',
                lat: 6.9042,
                lng: 69.8679, // fixed to 79.8679 below
                lng: 79.8679,
                desc: 'Padded red neoprene laptop sleeve found on park bench near walking path.',
                imageType: 'laptop'
            },
            {
                id: '2',
                title: 'Black leather wallet',
                location: 'Kollupitiya',
                status: 'LOST',
                lat: 6.8997,
                lng: 79.8530,
                desc: 'Slim bifold wallet with national ID and driving license.',
                imageType: 'wallet'
            },
            {
                id: '3',
                title: 'Sri Lankan passport',
                location: 'Fort Station',
                status: 'FOUND',
                lat: 6.9344,
                lng: 79.8504,
                desc: 'Sri Lankan passport found on waiting hall chair at Fort Railway Station.',
                imageType: 'passport'
            },
            {
                id: '4',
                title: 'AirPods Pro case',
                location: 'Bambalapitiya',
                status: 'LOST',
                lat: 6.8905,
                lng: 79.8570,
                desc: 'White AirPods Pro case left behind at cafe counter.',
                imageType: 'airpods'
            },
            {
                id: '5',
                title: 'Brown tabby cat',
                location: 'Viharamahadevi Park',
                status: 'LOST',
                lat: 6.9128,
                lng: 79.8647,
                desc: 'Friendly brown striped cat with pink collar last seen near lotus pond.',
                imageType: 'cat'
            }
        ];

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
                            window.AndroidInterface.onMarkerClicked(
                                item.id,
                                item.title,
                                item.location,
                                item.status,
                                item.desc,
                                item.imageType
                            );
                        }
                    });

                    markersGroup.addLayer(marker);
                }
            });
        }

        renderMarkers('all');

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
        fun newInstance(): MapFragment = MapFragment()
    }
}
