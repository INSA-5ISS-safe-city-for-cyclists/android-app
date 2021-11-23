package com.insa.iss.safecityforcyclists

import android.os.Bundle
import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager
import com.mapbox.mapboxsdk.utils.BitmapUtils
import java.util.*

class MapFragment: Fragment(R.layout.map_fragment) {

    private var mapView: MapView? = null
    private var mapboxMap: MapboxMap? = null
    private var symbolManager: SymbolManager? = null
    private var routing: Routing? = null
    private var dangerReports: DangerReports? = null
    private var location: Location? = null
    private var pinSelector: PinSelector? = null

    private fun makeCustomGeoapifyStyle(): Style.Builder {
        val builder = Style.Builder()
        val inputStream = resources.openRawResource(R.raw.osm_bright_cycleways)
        val jsonString: String = Scanner(inputStream).useDelimiter("\\A").next()
        println(jsonString)
        return builder.fromJson(jsonString)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(requireActivity())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState) // Get the MapBox context
        setupMap(view, savedInstanceState)
    }

    private fun setupMap(view: View, savedInstanceState: Bundle?) {
        // Create map view
        mapView = view.findViewById(R.id.mapView)
        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync { map ->
            mapboxMap = map
            map.setStyle(makeCustomGeoapifyStyle()) { style ->
                // Map fully loaded in this scope.
                // Update attributions position
                map.uiSettings.setAttributionMargins(15, 0, 0, 15)
                map.uiSettings.setCompassMargins(0, 250, 50, 0)

                addSymbolImages(style)

                // Init classes
                symbolManager = SymbolManager(mapView!!, map, style)

                dangerReports = DangerReports(style, requireActivity())
                dangerReports?.getGeoJsonData()

                routing = Routing(style, requireActivity(), symbolManager?.layerId!!, dangerReports!!)

                pinSelector = PinSelector(requireActivity(), mapView!!, map, style, routing, symbolManager)

                location = Location(style, map, requireActivity(), view.findViewById(R.id.gpsFAB))
                location?.enableLocationComponent()
            }
        }
    }

    private fun addSymbolImages(style: Style) {
        val markerIconDrawable =
            ResourcesCompat.getDrawable(this.resources, R.drawable.ic_marker_icon, null)
        val waypointIconDrawable =
            ResourcesCompat.getDrawable(this.resources, R.drawable.ic_waypoint_icon, null)
        val destinationIconDrawable =
            ResourcesCompat.getDrawable(
                this.resources,
                R.drawable.ic_destination_icon,
                null
            )
        val warningIconDrawable =
            ResourcesCompat.getDrawable(this.resources, R.drawable.ic_warning_icon, null)

        style.addImage(
            MainActivity.MARKER_ICON,
            BitmapUtils.getBitmapFromDrawable(markerIconDrawable)!!,
        )
        style.addImage(
            MainActivity.WAYPOINT_ICON,
            BitmapUtils.getBitmapFromDrawable(waypointIconDrawable)!!
        )
        style.addImage(
            MainActivity.DESTINATION_ICON,
            BitmapUtils.getBitmapFromDrawable(destinationIconDrawable)!!
        )
        style.addImage(
            MainActivity.WARNING_ICON,
            BitmapUtils.getBitmapFromDrawable(warningIconDrawable)!!
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        location?.onRequestPermissionsResult(requestCode, permissions, grantResults)
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