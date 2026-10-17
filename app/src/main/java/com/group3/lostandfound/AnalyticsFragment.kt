package com.group3.lostandfound

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment

/**
 * AnalyticsFragment displays application metrics:
 * - Timeframe dropdown selection & Export action
 * - Category distribution donut chart
 * - Resolution rates by major cities (Colombo, Kandy, Galle)
 * - Key performance metric cards (Median response, Time to resolve, Monthly growth)
 * - Embedded Leaflet JS geographic heat map
 */
class AnalyticsFragment : Fragment() {

    // Header views
    private lateinit var buttonBack: ImageView
    private lateinit var buttonDownloadReport: ImageView

    // Controls
    private lateinit var buttonTimeframeSelector: LinearLayout
    private lateinit var textSelectedTimeframe: TextView
    private lateinit var buttonExport: TextView

    // Metric cards
    private lateinit var cardMedianResponse: LinearLayout
    private lateinit var cardTimeToResolve: LinearLayout
    private lateinit var cardMonthlyGrowth: LinearLayout

    // Leaflet Heat Map WebView
    private lateinit var webHeatMap: WebView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_analytics, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupMap()
        setupClickListeners()
        setupBottomNav(view, "messages")
    }

    private fun initViews(view: View) {
        buttonBack = view.findViewById(R.id.buttonBack)
        buttonDownloadReport = view.findViewById(R.id.buttonDownloadReport)

        buttonTimeframeSelector = view.findViewById(R.id.buttonTimeframeSelector)
        textSelectedTimeframe = view.findViewById(R.id.textSelectedTimeframe)
        buttonExport = view.findViewById(R.id.buttonExport)

        cardMedianResponse = view.findViewById(R.id.cardMedianResponse)
        cardTimeToResolve = view.findViewById(R.id.cardTimeToResolve)
        cardMonthlyGrowth = view.findViewById(R.id.cardMonthlyGrowth)

        webHeatMap = view.findViewById(R.id.webHeatMap)
    }

    private fun setupMap() {
        webHeatMap.settings.javaScriptEnabled = true
        webHeatMap.settings.domStorageEnabled = true

        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
                <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"/>
                <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
                <style>
                    html, body, #map { margin: 0; padding: 0; width: 100%; height: 100%; background: #F1ECE6; }
                    .leaflet-control-attribution { display: none !important; }
                </style>
            </head>
            <body>
                <div id="map"></div>
                <script>
                    var map = L.map('map', { zoomControl: false, attributionControl: false }).setView([7.8731, 80.7718], 7);
                    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png').addTo(map);

                    var spots = [
                        { lat: 6.9271, lng: 79.8612, city: 'Colombo: 84% resolved', color: '#3A2D28', r: 24000 },
                        { lat: 7.2906, lng: 80.6337, city: 'Kandy: 76% resolved', color: '#A48374', r: 18000 },
                        { lat: 6.0535, lng: 80.2210, city: 'Galle: 68% resolved', color: '#CBAD8D', r: 15000 }
                    ];

                    spots.forEach(function(s) {
                        L.circle([s.lat, s.lng], {
                            color: s.color,
                            fillColor: s.color,
                            fillOpacity: 0.5,
                            radius: s.r
                        }).addTo(map).bindPopup(s.city);
                    });
                </script>
            </body>
            </html>
        """.trimIndent()
        webHeatMap.loadDataWithBaseURL("https://openstreetmap.org", html, "text/html", "UTF-8", null)
    }

    private fun setupClickListeners() {
        buttonBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        buttonDownloadReport.setOnClickListener {
            Toast.makeText(requireContext(), "Downloading analytics report...", Toast.LENGTH_SHORT).show()
        }

        buttonTimeframeSelector.setOnClickListener {
            Toast.makeText(requireContext(), "Select timeframe: Last 30 days", Toast.LENGTH_SHORT).show()
        }

        buttonExport.setOnClickListener {
            Toast.makeText(requireContext(), "Analytics exported successfully", Toast.LENGTH_SHORT).show()
        }

        cardMedianResponse.setOnClickListener {
            Toast.makeText(requireContext(), "Median response: 2.8 hours", Toast.LENGTH_SHORT).show()
        }

        cardTimeToResolve.setOnClickListener {
            Toast.makeText(requireContext(), "Time to resolve: 1.6 days", Toast.LENGTH_SHORT).show()
        }

        cardMonthlyGrowth.setOnClickListener {
            Toast.makeText(requireContext(), "Growth: +12% vs last month", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        fun newInstance(): AnalyticsFragment {
            return AnalyticsFragment()
        }
    }
}
