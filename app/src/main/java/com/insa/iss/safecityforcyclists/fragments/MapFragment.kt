package com.insa.iss.safecityforcyclists.fragments

import android.os.Bundle
import android.view.Gravity
import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.add
import androidx.fragment.app.commit
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.insa.iss.safecityforcyclists.MainActivity
import com.insa.iss.safecityforcyclists.R
import com.insa.iss.safecityforcyclists.location.Location
import com.insa.iss.safecityforcyclists.reports.DangerReports
import com.insa.iss.safecityforcyclists.reports.LocalDangerReportsViewModel
import com.insa.iss.safecityforcyclists.reports.RemoteDangerReportsViewModel
import com.insa.iss.safecityforcyclists.routing.RouteViewModel
import com.insa.iss.safecityforcyclists.routing.Routing
import com.insa.iss.safecityforcyclists.search.SearchResultsViewModel
import com.insa.iss.safecityforcyclists.selection.PinSelector
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager
import com.mapbox.mapboxsdk.utils.BitmapUtils
import java.util.*


class MapFragment : Fragment(R.layout.map_fragment) {
    companion object {
        const val REMOTE_REPORTS_ID = "REMOTE_REPORTS"
        const val LOCAL_REPORTS_ID = "LOCAL_REPORTS"
    }

    private var mapView: MapView? = null
    private var mapboxMap: MapboxMap? = null
    private var symbolManager: SymbolManager? = null
    private var routing: Routing? = null
    private var remoteDangerReports: DangerReports? = null
    private var localDangerReports: DangerReports? = null
    private var location: Location? = null
    private var pinSelector: PinSelector? = null
    private val searchResultsViewModel: SearchResultsViewModel by activityViewModels()
    private val routeViewModel: RouteViewModel by activityViewModels()
    private val remoteDangerReportsViewModel: RemoteDangerReportsViewModel by activityViewModels()
    private val localDangerReportsViewModel: LocalDangerReportsViewModel by activityViewModels()
    private var uploadFAB: FloatingActionButton? = null

    private fun makeCustomGeoapifyStyle(): Style.Builder {
        val builder = Style.Builder()
        val inputStream = resources.openRawResource(R.raw.osm_bright_cycleways)
        val jsonString: String = Scanner(inputStream).useDelimiter("\\A").next()
        return builder.fromJson(jsonString)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(requireActivity())
        remoteDangerReportsViewModel.initData()
        localDangerReportsViewModel.initData()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState) // Get the MapBox context
        setupMap(view, savedInstanceState)
        uploadFAB = view.findViewById(R.id.uploadFAB)
        uploadFAB?.setOnClickListener {
            val bottomSheet = UploadSummaryBottomSheetDialog { item, position ->
                println("Pressed $item at position $position")
            }
            bottomSheet.show(
                requireActivity().supportFragmentManager,
                "ModalBottomSheet"
            )
        }

        // Debug database
        if (resources.getBoolean(R.bool.debug)) {
            val databaseDebugView: View =
                view.findViewById(R.id.database_debug_fragment)
            databaseDebugView.visibility = View.VISIBLE
        }

        searchResultsViewModel.selected.observe(viewLifecycleOwner, { selected ->
            println("selection changed to $selected")
            println("selected item: ${selected?.let { searchResultsViewModel.dataSet.value?.get(it) }}")
            if (selected != null) {
                val item = searchResultsViewModel.dataSet.value?.get(selected)
                if (item != null) {
                    pinSelector?.showWaypointModal(item)
                    val p = (item.geometry() as Point)
                    val position = CameraPosition.Builder()
                        .target(LatLng(p.latitude(), p.longitude()))
                        .zoom(17.0)
                        .build()
                    mapboxMap?.animateCamera(
                        CameraUpdateFactory
                            .newCameraPosition(position), 7000
                    )
                }
            }
        })
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
                map.uiSettings.attributionGravity = Gravity.TOP
                map.uiSettings.setAttributionMargins(15, 250, 0, 0)
                map.uiSettings.setCompassMargins(0, 250, 50, 0)

                addSymbolImages(style)

                // Init classes
                symbolManager = SymbolManager(mapView!!, map, style)

                remoteDangerReports =
                    DangerReports(style, this, remoteDangerReportsViewModel, REMOTE_REPORTS_ID, true)
                localDangerReports =
                    DangerReports(style, this, localDangerReportsViewModel, LOCAL_REPORTS_ID)

                location = Location(style, map, requireActivity(), view.findViewById(R.id.gpsFAB))
                location?.onResume()
                location?.enableLocationComponent()

                routing = Routing(
                    style,
                    map,
                    requireActivity(),
                    symbolManager?.layerId!!,
                    remoteDangerReportsViewModel,
                    location!!,
                    routeViewModel
                )

                pinSelector =
                    PinSelector(requireActivity(), mapView!!, map, style, routing, symbolManager)
            }
        }
        routeViewModel.routeGeoJson.observe(viewLifecycleOwner, { routeGeoJson ->
            if (routeGeoJson != null) {
                requireActivity().supportFragmentManager.commit {
                    setReorderingAllowed(true)
                    add<RouteSummaryFragment>(R.id.summary_fragment_container_view)
                }
            } else {
                requireActivity().supportFragmentManager.findFragmentById(R.id.summary_fragment_container_view)
                    ?.let {
                        requireActivity().supportFragmentManager.commit {
                            remove(it)
                        }
                    }
            }
        })
    }

    private fun addSymbolImages(style: Style) {
        val markerIconDrawable =
            ResourcesCompat.getDrawable(this.resources, R.drawable.ic_marker_icon, null)
        val localMarkerIconDrawable =
            ResourcesCompat.getDrawable(this.resources, R.drawable.ic_marker_icon_local, null)
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
            MainActivity.LOCAL_MARKER_ICON,
            BitmapUtils.getBitmapFromDrawable(localMarkerIconDrawable)!!,
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
        location?.onResume()
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
        location?.onDestroy()
    }
}