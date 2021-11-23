package com.insa.iss.safecityforcyclists

import android.graphics.PointF
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.geojson.Feature
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.plugins.annotation.Symbol
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions

class PinSelector(private val activity: AppCompatActivity, private val map: MapboxMap, private val routing: Routing?, private val symbolManager: SymbolManager?) {

    private var onBackPressedCallback: OnBackPressedCallback? = null

    init {
        onBackPressedCallback = object : OnBackPressedCallback(
            false
        ) {
            override fun handleOnBackPressed() {
                removeAllRouteWaypoints()
            }
        }
        // Init callbacks
        activity.onBackPressedDispatcher.addCallback(activity, onBackPressedCallback!!)
        symbolManager?.addClickListener { it ->
            removeRouteWaypoint(it)
            true
        }
        map.addOnMapClickListener { point: LatLng ->
            onMapClick(map, point)
            return@addOnMapClickListener true
        }
        map.addOnMapLongClickListener { point: LatLng ->
            onMapLongClick(point)
            return@addOnMapLongClickListener true
        }
    }

    private fun onMapClick(map: MapboxMap, point: LatLng) {
        // Get the clicked point coordinates
        val screenPoint: PointF = map.projection.toScreenLocation(point)
        // Query the source layer in that location
        val reportsFeatures: List<Feature> =
            map.queryRenderedFeatures(screenPoint, "unclustered-points")
        if (reportsFeatures.isNotEmpty()) {
            val feature: Feature = reportsFeatures[0]
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
            println(features)
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

    private fun showReportModal(feature: Feature) {
        val bottomSheet = BottomSheetDialog(feature)
        bottomSheet.show(
            activity.supportFragmentManager,
            "ModalBottomSheet"
        )
    }

    private fun showWaypointModal(feature: Feature) {
        val bottomSheet = WaypointBottomSheetDialog(feature, { f ->
            val p = f.geometry() as Point
            addRouteWaypoint(LatLng(p.latitude(), p.longitude()), true)
        }, { f ->
            val p = f.geometry() as Point
            addRouteWaypoint(LatLng(p.latitude(), p.longitude()), false)
        })
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