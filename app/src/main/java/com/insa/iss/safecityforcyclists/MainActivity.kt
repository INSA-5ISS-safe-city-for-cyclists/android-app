package com.insa.iss.safecityforcyclists

import android.graphics.Color
import android.graphics.PointF
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import com.mapbox.geojson.Feature
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.Symbol
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions
import com.mapbox.mapboxsdk.style.expressions.Expression
import com.mapbox.mapboxsdk.style.expressions.Expression.*
import com.mapbox.mapboxsdk.style.layers.CircleLayer
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.*
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonOptions
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.mapboxsdk.utils.BitmapUtils
import java.net.URI
import java.net.URISyntaxException
import java.util.*




class MainActivity : AppCompatActivity() {
    companion object {
        private const val MARKER_ICON = "MARKER_ICON"
        private const val WAYPOINT_ICON = "WAYPOINT_ICON"
    }

    private var mapView: MapView? = null
    private var symbolManager: SymbolManager? = null
    private var routing: Routing? = null
    private var onBackPressedCallback: OnBackPressedCallback? = null

    private fun makeGeoapifyStyleUrl(style: String = "osm-carto"): String {
        return "${getString(R.string.geoapify_styles_url) + style}/style.json?apiKey=${getString(R.string.geoapify_access_token)}";
    }

    private fun makeCustomGeoapifyStyle(): Style.Builder {
        val builder = Style.Builder()
        val inputStream = resources.openRawResource(R.raw.osm_bright_cycleways)
        val jsonString: String = Scanner(inputStream).useDelimiter("\\A").next()
        println(jsonString)
        return builder.fromJson(jsonString)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Get the MapBox context
        Mapbox.getInstance(this)
        setContentView(R.layout.activity_main)
        setupMap(savedInstanceState)
    }

    private fun setupMap(savedInstanceState: Bundle?) {
        // Create map view
        mapView = findViewById(R.id.mapView)
        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync { map ->
            map.setStyle(makeCustomGeoapifyStyle()) { style ->
                // Map fully loaded in this scope.
                // Update attributions position
                map.uiSettings.setAttributionMargins(15, 0, 0, 15)

                symbolManager = SymbolManager(mapView!!, map, style)
                // Delete marker on click
                symbolManager?.addClickListener { it ->
                    removeRouteWaypoint(it)
                    true
                }
                val markerIconDrawable =
                    ResourcesCompat.getDrawable(this.resources, R.drawable.ic_marker_icon, null)
                val waypointIconDrawable =
                    ResourcesCompat.getDrawable(this.resources, R.drawable.ic_waypoint_icon, null)

                style.addImage(
                    MARKER_ICON,
                    BitmapUtils.getBitmapFromDrawable(markerIconDrawable)!!,
                )
                style.addImage(
                        WAYPOINT_ICON,
                BitmapUtils.getBitmapFromDrawable(waypointIconDrawable)!!
                )
                addClusteredGeoJsonSource(style)
                map.addOnMapClickListener { point: LatLng ->
                    onMapClick(map, point)
                    return@addOnMapClickListener true
                }
                routing = Routing(style, this, symbolManager?.layerId!!)

                onBackPressedCallback = object : OnBackPressedCallback(
                    false
                ) {
                    override fun handleOnBackPressed() {
                        removeAllRouteWaypoints()
                    }
                }
                onBackPressedDispatcher.addCallback(this, onBackPressedCallback!!)

            }
        }
    }

    private fun onMapClick(map: MapboxMap, point: LatLng) {
        // Get the clicked point coordinates
        val screenPoint: PointF = map.projection.toScreenLocation(point)
        // Query the source layer in that location
        val features: List<Feature> =
            map.queryRenderedFeatures(screenPoint, "unclustered-points")
        if (features.isNotEmpty()) {
            // get the first feature in the list
            val feature: Feature = features[0]
            showReportModal(feature)
        } else {
            addRouteWaypoint(point)
        }
    }

    private fun showReportModal(feature: Feature) {
        val bottomSheet = BottomSheetDialog(feature)
        bottomSheet.show(
            supportFragmentManager,
            "ModalBottomSheet"
        )
    }

    private fun addRouteWaypoint(point: LatLng) {
        // Add marker at specified lat/lon.
        val newSymbol = symbolManager?.create(
            SymbolOptions()
                .withLatLng(point)
                .withIconImage(WAYPOINT_ICON)
                .withIconSize(0.8f)
                .withIconOffset(arrayOf(0f, -20f))
        )
        when {
            routing?.startSymbol == null -> {
                routing?.startSymbol = newSymbol
            }
            routing?.endSymbol == null -> {
                routing?.endSymbol = newSymbol
            }
            else -> {
                // If both markers are already present, delete and replace the end
                removeRouteWaypoint(routing?.endSymbol)
                routing?.endSymbol = newSymbol
            }
        }
        symbolManager?.update(newSymbol)
        updateOnBackPressedCallback()
    }

    private fun removeRouteWaypoint(it: Symbol?) {
        if (it == null) {
            return
        }
        if (routing?.endSymbol == it) {
            routing?.endSymbol = null
        } else if (routing?.startSymbol == it) {
            routing?.startSymbol = null
        }
        symbolManager?.delete(it)
        updateOnBackPressedCallback()
    }

    private fun removeAllRouteWaypoints() {
        removeRouteWaypoint(routing?.endSymbol)
        removeRouteWaypoint(routing?.startSymbol)
    }

    private fun updateOnBackPressedCallback() {
        onBackPressedCallback?.isEnabled =  routing?.endSymbol != null || routing?.startSymbol != null
    }

    private fun addClusteredGeoJsonSource(loadedMapStyle: Style) {
        // Add a new source from the GeoJSON data and set the 'cluster' option to true.
        try {
            loadedMapStyle.addSource(
                // Point to GeoJSON data. This example visualizes all M1.0+ earthquakes from
                // 12/22/15 to 1/21/16 as logged by USGS' Earthquake hazards program.
                GeoJsonSource(
                    "clusters",
                    URI("https://maplibre.org/maplibre-gl-js-docs/assets/earthquakes.geojson"),
                    GeoJsonOptions()
                        .withCluster(true)
                        .withClusterMaxZoom(14)
                        .withClusterRadius(50)
                )
            )
        } catch (uriSyntaxException: URISyntaxException) {
            println("Check the URL %s" + uriSyntaxException.message)
        }

        //Creating a marker layer for single data points
        val unclustered = SymbolLayer("unclustered-points", "clusters")
        unclustered.setProperties(
            iconImage(MARKER_ICON),
            iconSize(
                division(
                    get("mag"), literal(4.0f)
                )
            )
        )
        unclustered.setFilter(has("mag"))
        loadedMapStyle.addLayer(unclustered)

        // Use the earthquakes GeoJSON source to create three layers: One layer for each cluster category.
        // Each point range gets a different fill color.
        val layers = arrayOf(
            intArrayOf(150, 30, Color.parseColor("#51bbd6")),
            intArrayOf(20, 25, Color.parseColor("#f1f075")),
            intArrayOf(0, 20, Color.parseColor("#f28cb1"))
        )
        for (i in layers.indices) {
            //Add clusters' circles
            val circles = CircleLayer("cluster-$i", "clusters")
            circles.setProperties(
                circleColor(layers[i][2]),
                circleRadius(layers[i][1].toFloat())
            )
            val pointCount: Expression = toNumber(get("point_count"))

            // Add a filter to the cluster layer that hides the circles based on "point_count"
            circles.setFilter(
                if (i == 0) all(
                    has("point_count"),
                    gte(pointCount, literal(layers[i][0]))
                ) else all(
                    has("point_count"),
                    gte(pointCount, literal(layers[i][0])),
                    lt(pointCount, literal(layers[i - 1][0]))
                )
            )
            loadedMapStyle.addLayer(circles)
        }

        //Add the count labels
        val count = SymbolLayer("count", "clusters")
        count.setProperties(
            textField(toString(get("point_count"))),
            textSize(12f),
            textColor(Color.WHITE),
            textIgnorePlacement(true),
            textAllowOverlap(true)
        )
        loadedMapStyle.addLayer(count)
    }


    override fun onStart() {
        super.onStart()
        mapView?.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView?.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView?.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView?.onDestroy()
    }

}