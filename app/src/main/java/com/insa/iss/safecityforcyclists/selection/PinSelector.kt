package com.insa.iss.safecityforcyclists.selection

import android.graphics.PointF
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.FragmentActivity
import com.insa.iss.safecityforcyclists.MainActivity
import com.insa.iss.safecityforcyclists.fragments.DangerReportBottomSheetDialog
import com.insa.iss.safecityforcyclists.fragments.MapFragment
import com.insa.iss.safecityforcyclists.fragments.WaypointBottomSheetDialog
import com.insa.iss.safecityforcyclists.routing.Routing
import com.mapbox.geojson.Feature
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.*

class PinSelector(
    private val activity: FragmentActivity,
    mapView: MapView,
    private val mapboxMap: MapboxMap,
    style: Style,
    private val routing: Routing?,
    private val symbolManager: SymbolManager?
) {

    private var onBackPressedCallback: OnBackPressedCallback? = null
    private var circleManager: CircleManager? = null

    init {
        onBackPressedCallback = object : OnBackPressedCallback(
            false
        ) {
            override fun handleOnBackPressed() {
                removeAllRouteWaypoints()
            }
        }
        activity.supportFragmentManager.addOnBackStackChangedListener {
            // Ony enable custom back behavior if the map is displayed
            if (activity.supportFragmentManager.backStackEntryCount == 0) {
                updateOnBackPressedCallback()
            } else {
                onBackPressedCallback?.isEnabled = false
            }
        }
        circleManager = CircleManager(mapView, mapboxMap, style)
        // Init callbacks
        activity.onBackPressedDispatcher.addCallback(activity, onBackPressedCallback!!)
        symbolManager?.addClickListener { it ->
            removeRouteWaypoint(it)
            true
        }
        mapboxMap.addOnMapClickListener { point: LatLng ->
            onMapClick(mapboxMap, point)
            return@addOnMapClickListener true
        }
        mapboxMap.addOnMapLongClickListener { point: LatLng ->
            onMapLongClick(point)
            return@addOnMapLongClickListener true
        }
    }

    private fun centerMapOnSelection(selection: LatLng) {
        mapboxMap.animateCamera(CameraUpdateFactory.newLatLngZoom(selection, 15.0))
    }

    private fun onMapClick(map: MapboxMap, point: LatLng) {
        // Get the clicked point coordinates
        val screenPoint: PointF = map.projection.toScreenLocation(point)
        // Query the source layer in that location
        val remoteReportsFeatures: List<Feature> =
            map.queryRenderedFeatures(
                screenPoint,
                MapFragment.REMOTE_REPORTS_ID,
                MapFragment.LOCAL_REPORTS_ID,
                MapFragment.LOCAL_REPORTS_UNSYNC_ID
            )
        if (remoteReportsFeatures.isNotEmpty()) {
            val feature: Feature = remoteReportsFeatures[0]
            showReportModal(feature)
        } else {
            val features: List<Feature> =
                map.queryRenderedFeatures(
                    screenPoint,
                    "poi-level-3",
                    "poi-level-2",
                    "poi-level-1",
                    "poi-railway"
                )
            if (features.isNotEmpty()) {
                val feature: Feature = features[0]
                showWaypointModal(feature)
            }
        }
    }

    private fun onMapLongClick(point: LatLng) {
        val feature = Feature.fromGeometry(Point.fromLngLat(point.longitude, point.latitude))
        showWaypointModal(feature)
    }

    fun showReportModal(feature: Feature, onDismiss: (() -> Unit)? = null, centerMapOnFeature: Boolean = false) {
        val p = feature.geometry() as Point
        val point = LatLng(p.latitude(), p.longitude())
        showSelectionCircle(point)
        val bottomSheet = DangerReportBottomSheetDialog(feature) {
            hideSelectionCircle()
            onDismiss?.let {
                it()
            }
        }
        if (centerMapOnFeature) {
            centerMapOnSelection(point)
        }
        bottomSheet.show(
            activity.supportFragmentManager,
            "ModalBottomSheet"
        )
    }

    private fun showSelectionCircle(point: LatLng) {
        circleManager?.deleteAll()
        val newCircle = circleManager?.create(
            CircleOptions()
                .withLatLng(point)
                .withCircleColor("#0000ff")
                .withCircleOpacity(0.3f)
                .withCircleRadius(15f)
        )
        circleManager?.update(newCircle)
    }

    private fun hideSelectionCircle() {
        circleManager?.deleteAll()
    }

    fun showWaypointModal(feature: Feature) {
        val p = feature.geometry() as Point
        val point = LatLng(p.latitude(), p.longitude())
        val bottomSheet = WaypointBottomSheetDialog(feature, {
            addRouteWaypoint(point, true)
        }, {
            addRouteWaypoint(point, false)
        }, {
            hideSelectionCircle()
        })
        showSelectionCircle(point)

        bottomSheet.show(
            activity.supportFragmentManager,
            "WaypointBottomSheet"
        )
    }

    private fun addRouteWaypoint(point: LatLng, isStart: Boolean) {
        val icon = if (isStart) {
            MainActivity.WAYPOINT_ICON
        } else {
            MainActivity.DESTINATION_ICON
        }
        val newSymbol = symbolManager?.create(
            SymbolOptions()
                .withLatLng(point)
                .withIconImage(icon)
                .withIconSize(0.8f)
                .withIconOffset(arrayOf(0f, -20f))
        )

        // Clear previous waypoint if any
        if (isStart) {
            removeRouteWaypoint(routing?.startSymbol)
            routing?.startSymbol = newSymbol
        } else {
            removeRouteWaypoint(routing?.endSymbol)
            routing?.endSymbol = newSymbol
        }
        symbolManager?.update(newSymbol)
        updateOnBackPressedCallback()
    }

    private fun removeRouteWaypoint(symbol: Symbol?) {
        if (symbol == null) {
            return
        }
        if (routing?.endSymbol == symbol) {
            routing.endSymbol = null
        } else if (routing?.startSymbol == symbol) {
            routing.startSymbol = null
        }
        symbolManager?.delete(symbol)
        updateOnBackPressedCallback()
    }

    private fun removeAllRouteWaypoints() {
        removeRouteWaypoint(routing?.endSymbol)
        removeRouteWaypoint(routing?.startSymbol)
    }

    private fun updateOnBackPressedCallback() {
        onBackPressedCallback?.isEnabled =
            routing?.endSymbol != null || routing?.startSymbol != null
    }

}