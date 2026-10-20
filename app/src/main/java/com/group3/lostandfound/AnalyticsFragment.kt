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
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * AnalyticsFragment displays application metrics:
 * - Timeframe dropdown selection & Export action
 * - Real-time statistics aggregation by observing /items and /users nodes
 * - Coroutines Flow UI state updates
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

    // Metric cards & values
    private lateinit var cardMedianResponse: LinearLayout
    private lateinit var cardTimeToResolve: LinearLayout
    private lateinit var cardMonthlyGrowth: LinearLayout
    private lateinit var textMedianResponseValue: TextView
    private lateinit var textTimeToResolveValue: TextView
    private lateinit var textMonthlyGrowthValue: TextView

    // Category Legend TextViews
    private lateinit var tvCategoryElectronics: TextView
    private lateinit var tvCategoryDocuments: TextView
    private lateinit var tvCategoryPets: TextView
    private lateinit var tvCategoryOther: TextView

    // City Resolution TextViews
    private lateinit var tvPercentColombo: TextView
    private lateinit var tvPercentKandy: TextView
    private lateinit var tvPercentGalle: TextView

    // Leaflet Heat Map WebView
    private lateinit var webHeatMap: WebView

    private var currentSummary = AnalyticsSummary()

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
        setupBottomNav(view, "admin")
        observeRealtimeAnalyticsFlow()
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

        textMedianResponseValue = view.findViewById(R.id.textMedianResponseValue)
        textTimeToResolveValue = view.findViewById(R.id.textTimeToResolveValue)
        textMonthlyGrowthValue = view.findViewById(R.id.textMonthlyGrowthValue)

        tvCategoryElectronics = view.findViewById(R.id.tvCategoryElectronics)
        tvCategoryDocuments = view.findViewById(R.id.tvCategoryDocuments)
        tvCategoryPets = view.findViewById(R.id.tvCategoryPets)
        tvCategoryOther = view.findViewById(R.id.tvCategoryOther)

        tvPercentColombo = view.findViewById(R.id.tvPercentColombo)
        tvPercentKandy = view.findViewById(R.id.tvPercentKandy)
        tvPercentGalle = view.findViewById(R.id.tvPercentGalle)

        webHeatMap = view.findViewById(R.id.webHeatMap)
    }

    /**
     * Compute and observe real-time statistics by observing /items and /users nodes
     * via Kotlin Coroutines Flow UI state.
     */
    private fun observeRealtimeAnalyticsFlow() {
        viewLifecycleOwner.lifecycleScope.launch {
            FirebaseManager.analyticsFlow.collectLatest { summary ->
                currentSummary = summary
                val resolutionPercent = if (summary.totalReports > 0) {
                    (summary.resolvedReports * 100) / summary.totalReports
                } else {
                    0
                }

                // Update dynamic values
                textTimeToResolveValue.text = "${resolutionPercent}%"
                textMedianResponseValue.text = "${summary.totalReports} Items"
                textMonthlyGrowthValue.text = "${summary.userCount} Users"

                // Dynamic Category Distribution
                val total = summary.totalReports
                if (total > 0) {
                    val elec = summary.categoryDistribution["Electronics"] ?: 0
                    val doc = summary.categoryDistribution["Documents"] ?: 0
                    val pets = summary.categoryDistribution["Pets"] ?: 0

                    val elecPct = (elec * 100) / total
                    val docPct = (doc * 100) / total
                    val petsPct = (pets * 100) / total
                    val otherPct = (100 - elecPct - docPct - petsPct).coerceAtLeast(0)

                    tvCategoryElectronics.text = "Electronics $elecPct%"
                    tvCategoryDocuments.text = "Documents $docPct%"
                    tvCategoryPets.text = "Pets $petsPct%"
                    tvCategoryOther.text = "Other $otherPct%"
                } else {
                    tvCategoryElectronics.text = "Electronics 0%"
                    tvCategoryDocuments.text = "Documents 0%"
                    tvCategoryPets.text = "Pets 0%"
                    tvCategoryOther.text = "Other 0%"
                }

                // Dynamic City Resolution Rates
                val colTotal = summary.colomboStats.first
                val colRes = summary.colomboStats.second
                tvPercentColombo.text = if (colTotal > 0) "${(colRes * 100) / colTotal}%" else "100%"

                val kanTotal = summary.kandyStats.first
                val kanRes = summary.kandyStats.second
                tvPercentKandy.text = if (kanTotal > 0) "${(kanRes * 100) / kanTotal}%" else "100%"

                val galTotal = summary.galleStats.first
                val galRes = summary.galleStats.second
                tvPercentGalle.text = if (galTotal > 0) "${(galRes * 100) / galTotal}%" else "100%"

                // Push real points to Leaflet Heat Map
                loadRealPointsToMap(summary.heatMapPoints)
            }
        }
    }

    private fun loadRealPointsToMap(points: List<MapMarkerItem>) {
        if (!::webHeatMap.isInitialized) return
        val jsPoints = points.joinToString(",") { point ->
            val safeTitle = point.title.replace("'", "\\'").ifEmpty { "Reported Item" }
            val color = if (point.status.equals("LOST", true)) "#C25946" else "#4A7C59"
            """{ lat: ${point.latitude}, lng: ${point.longitude}, title: '$safeTitle (${point.status})', color: '$color' }"""
        }
        val script = """
            if (typeof updateMapPoints === 'function') {
                updateMapPoints([$jsPoints]);
            }
        """.trimIndent()
        webHeatMap.post {
            webHeatMap.evaluateJavascript(script, null)
        }
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

                    var dynamicLayerGroup = L.layerGroup().addTo(map);

                    function updateMapPoints(items) {
                        dynamicLayerGroup.clearLayers();
                        if (items && items.length > 0) {
                            items.forEach(function(item) {
                                L.circle([item.lat, item.lng], {
                                    color: item.color,
                                    fillColor: item.color,
                                    fillOpacity: 0.6,
                                    radius: 12000
                                }).addTo(dynamicLayerGroup).bindPopup(item.title);
                            });
                        } else {
                            var defaults = [
                                { lat: 6.9271, lng: 79.8612, city: 'Colombo Region', color: '#3A2D28', r: 18000 },
                                { lat: 7.2906, lng: 80.6337, city: 'Kandy Region', color: '#A48374', r: 14000 },
                                { lat: 6.0535, lng: 80.2210, city: 'Galle Region', color: '#CBAD8D', r: 12000 }
                            ];
                            defaults.forEach(function(s) {
                                L.circle([s.lat, s.lng], {
                                    color: s.color,
                                    fillColor: s.color,
                                    fillOpacity: 0.5,
                                    radius: s.r
                                }).addTo(dynamicLayerGroup).bindPopup(s.city);
                            });
                        }
                    }
                    updateMapPoints([]);
                </script>
            </body>
            </html>
        """.trimIndent()
        webHeatMap.loadDataWithBaseURL("https://openstreetmap.org", html, "text/html", "UTF-8", null)
    }

    private fun setupClickListeners() {
        buttonBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        buttonDownloadReport.setOnClickListener {
            exportAnalyticsReport()
        }

        buttonTimeframeSelector.setOnClickListener {
            val options = arrayOf("Last 7 days", "Last 30 days", "Last 90 days", "All Time")
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Select Timeframe")
                .setItems(options) { _, which ->
                    textSelectedTimeframe.text = options[which]
                    com.group3.lostandfound.util.ModernToast.show(requireContext(), "Filtering for ${options[which]}", view)
                }
                .show()
        }

        buttonExport.setOnClickListener {
            exportAnalyticsReport()
        }

        cardMedianResponse.setOnClickListener {
            com.group3.lostandfound.util.ModernToast.show(requireContext(), "Total Reports in Database: ${currentSummary.totalReports}", view)
        }

        cardTimeToResolve.setOnClickListener {
            com.group3.lostandfound.util.ModernToast.show(requireContext(), "Resolved Cases in Database: ${currentSummary.resolvedReports}", view)
        }

        cardMonthlyGrowth.setOnClickListener {
            com.group3.lostandfound.util.ModernToast.show(requireContext(), "Registered Users in Database: ${currentSummary.userCount}", view)
        }
    }

    private fun exportAnalyticsReport() {
        val resolutionPercent = if (currentSummary.totalReports > 0) {
            (currentSummary.resolvedReports * 100) / currentSummary.totalReports
        } else {
            0
        }

        val exportText = buildString {
            appendLine("=== FOUNDLY ANALYTICS SUMMARY REPORT ===")
            appendLine("Generated: ${java.util.Date()}")
            appendLine("Timeframe: ${textSelectedTimeframe.text}")
            appendLine("----------------------------------------")
            appendLine("Total Community Reports: ${currentSummary.totalReports}")
            appendLine("Resolved Cases: ${currentSummary.resolvedReports} ($resolutionPercent% Resolution Rate)")
            appendLine("Active Searches: ${currentSummary.totalReports - currentSummary.resolvedReports}")
            appendLine("Flagged Moderation Items: ${currentSummary.flaggedItemsCount}")
            appendLine("Registered Community Users: ${currentSummary.userCount}")
            appendLine("----------------------------------------")
            appendLine("Reports by Category:")
            currentSummary.categoryDistribution.forEach { (cat, count) ->
                appendLine("- $cat: $count")
            }
            appendLine("========================================")
        }

        val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_SUBJECT, "Foundly Analytics Report")
            putExtra(android.content.Intent.EXTRA_TEXT, exportText)
        }
        startActivity(android.content.Intent.createChooser(sendIntent, "Share Analytics Summary via"))
    }

    companion object {
        fun newInstance(): AnalyticsFragment {
            return AnalyticsFragment()
        }
    }
}
