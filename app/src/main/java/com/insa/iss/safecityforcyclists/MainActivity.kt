package com.insa.iss.safecityforcyclists

import android.annotation.SuppressLint
import android.graphics.PointF
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import com.mapbox.geojson.Feature
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponent
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.location.permissions.PermissionsListener
import com.mapbox.mapboxsdk.location.permissions.PermissionsManager
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.maps.Style.OnStyleLoaded
import com.mapbox.mapboxsdk.plugins.annotation.Symbol
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions
import com.mapbox.mapboxsdk.style.expressions.Expression.*
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.*
import com.mapbox.mapboxsdk.utils.BitmapUtils
import java.util.*


class MainActivity : AppCompatActivity(), PermissionsListener {
    companion object {
        const val MARKER_ICON = "MARKER_ICON"
        private const val WAYPOINT_ICON = "WAYPOINT_ICON"
    }

    private var mapView: MapView? = null
    private var mapboxMap: MapboxMap? = null
    private var symbolManager: SymbolManager? = null
    private var routing: Routing? = null
    private var onBackPressedCallback: OnBackPressedCallback? = null
    private var permissionsManager: PermissionsManager? = null
    private var dangerReports: DangerReports? = null

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
            mapboxMap = map
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
                dangerReports = DangerReports(style);
                dangerReports?.addClusteredGeoJsonSource()
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
                enableLocationComponent(style)
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



    @SuppressLint("MissingPermission")
    private fun enableLocationComponent(loadedMapStyle: Style) {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            // Get an instance of the component
            val locationComponent: LocationComponent = mapboxMap?.locationComponent!!
            // Activate with options
            locationComponent.activateLocationComponent(
                LocationComponentActivationOptions.builder(this, loadedMapStyle).build()
            )
            // Enable to make component visible
            locationComponent.isLocationComponentEnabled = true
            // Set the component's camera mode
            locationComponent.cameraMode = CameraMode.TRACKING
            // Set the component's render mode
            locationComponent.renderMode = RenderMode.COMPASS
        } else {
            permissionsManager = PermissionsManager(this)
            permissionsManager?.requestLocationPermissions(this)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionsManager?.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onExplanationNeeded(permissionsToExplain: List<String?>?) {
        Toast.makeText(this, R.string.user_location_permission_explanation, Toast.LENGTH_LONG)
            .show()
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            mapboxMap?.getStyle(OnStyleLoaded { style -> enableLocationComponent(style) })
        } else {
            Toast.makeText(this, R.string.user_location_permission_not_granted, Toast.LENGTH_LONG)
                .show()
            finish()
        }
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